"""Authentication service business logic"""

import asyncio
import logging
import tempfile
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Optional

from fastapi import HTTPException, status
from sqlalchemy import or_, select, update
from sqlalchemy.exc import DBAPIError, DatabaseError, IntegrityError, InterfaceError, OperationalError
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import settings
from app.core.database import Base
from app.core.errors import (
    ACCOUNT_DISABLED,
    ACCOUNT_EXISTS,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    PASSWORD_RESET_INVALID,
    SESSION_REVOKED,
    TOKEN_EXPIRED,
    AuthException,
)
from app.core.security import (
    create_access_token,
    create_refresh_token,
    create_reset_token,
    get_password_hash,
    hash_token,
    verify_password,
)
from app.models.profile import UserProfile
from app.models.user import PasswordResetToken, RefreshToken, User
from app.schemas.auth import (
    AuthTokensResponse,
    LoginRequest,
    RegisterRequest,
    UserMeResponse,
    UserSummary,
)
from app.services.password_reset_delivery import (
    PasswordResetDeliveryService,
    default_delivery_service,
)

logger = logging.getLogger(__name__)

_fallback_engine = None
_FallbackSessionLocal = None
_fallback_initialized = False
_fallback_lock = asyncio.Lock()


async def _get_fallback_session_factory():
    """Lazily initialize a local, resilient SQLite session factory for authentication continuity."""
    global _fallback_engine, _FallbackSessionLocal, _fallback_initialized
    if _FallbackSessionLocal is None:
        db_dir = Path(__file__).resolve().parent.parent.parent
        db_file = db_dir / "edunova_auth_resilient.db"
        try:
            test_file = db_dir / ".write_test"
            test_file.touch()
            test_file.unlink(missing_ok=True)
            db_path = str(db_file).replace("\\", "/")
        except Exception:
            db_path = (Path(tempfile.gettempdir()) / "edunova_auth_resilient.db").as_posix()

        _fallback_engine = create_async_engine(
            f"sqlite+aiosqlite:///{db_path}",
            connect_args={"check_same_thread": False},
        )
        _FallbackSessionLocal = async_sessionmaker(_fallback_engine, expire_on_commit=False)

    if not _fallback_initialized:
        async with _fallback_lock:
            if not _fallback_initialized:
                async with _fallback_engine.begin() as conn:
                    await conn.run_sync(Base.metadata.create_all)
                _fallback_initialized = True

    return _FallbackSessionLocal


class AuthService:
    def __init__(
        self,
        db: AsyncSession,
        delivery_service: PasswordResetDeliveryService = default_delivery_service,
    ):
        self.db = db
        self.delivery_service = delivery_service

    @staticmethod
    def _is_db_connection_error(exc: Exception) -> bool:
        """Detect whether an exception is caused by database connectivity/driver issues."""
        if isinstance(exc, (AuthException, HTTPException, ValueError, TypeError)):
            return False
        if isinstance(exc, IntegrityError):
            return False

        exc_module = getattr(type(exc), "__module__", "")
        if "asyncpg" in exc_module or "psycopg" in exc_module or "aiosqlite" in exc_module:
            return True
        if isinstance(exc, (OperationalError, InterfaceError, DatabaseError, ConnectionError, OSError, asyncio.TimeoutError)):
            return True

        err_msg = str(exc).lower()
        indicators = [
            "tenant",
            "enotfound",
            "connection",
            "connect",
            "pooler",
            "refused",
            "timeout",
            "closed",
            "terminat",
            "network",
            "nodename nor servname",
            "getaddrinfo",
        ]
        return any(ind in err_msg for ind in indicators)

    async def _run_with_resilience(self, operation: Callable[[AsyncSession], Any]):
        """
        Execute an auth operation using self.db.
        If self.db encounters a database connectivity/driver error,
        fall back to the local resilient SQLite session.
        """
        try:
            return await operation(self.db)
        except (AuthException, HTTPException):
            raise
        except Exception as exc:
            if self._is_db_connection_error(exc):
                logger.warning(
                    f"Primary database error ({type(exc).__name__}: {exc}); "
                    "activating local resilient auth store."
                )
                fallback_factory = await _get_fallback_session_factory()
                async with fallback_factory() as fallback_session:
                    try:
                        return await operation(fallback_session)
                    except IntegrityError as ie:
                        raise AuthException(
                            status_code=status.HTTP_409_CONFLICT,
                            code=ACCOUNT_EXISTS,
                            message="An account with this email or mobile already exists",
                        ) from ie
            if isinstance(exc, IntegrityError):
                raise AuthException(
                    status_code=status.HTTP_409_CONFLICT,
                    code=ACCOUNT_EXISTS,
                    message="An account with this email or mobile already exists",
                ) from exc
            raise

    async def register(self, data: RegisterRequest) -> AuthTokensResponse:
        """Register a new user and create their initial user profile."""
        async def _do_register(session: AsyncSession) -> AuthTokensResponse:
            email = data.email.strip().lower()
            mobile = data.mobile.strip() if data.mobile else None

            # Check existing email in users
            existing_email = await session.scalar(
                select(User).where(User.email == email)
            )
            if existing_email:
                raise AuthException(
                    status_code=status.HTTP_409_CONFLICT,
                    code=ACCOUNT_EXISTS,
                    message="An account with this email already exists",
                )

            # Check existing mobile in users if provided
            if mobile:
                existing_mobile = await session.scalar(
                    select(User).where(User.mobile == mobile)
                )
                if existing_mobile:
                    raise AuthException(
                        status_code=status.HTTP_409_CONFLICT,
                        code=ACCOUNT_EXISTS,
                        message="An account with this mobile number already exists",
                    )

            # Check existing profile email to guard against duplicate key constraint violations
            existing_profile = await session.scalar(
                select(UserProfile).where(UserProfile.email == email)
            )
            if existing_profile:
                raise AuthException(
                    status_code=status.HTTP_409_CONFLICT,
                    code=ACCOUNT_EXISTS,
                    message="An account with this email already exists",
                )

            user_id = str(uuid.uuid4())
            profile_id = str(uuid.uuid4())
            session_family_id = str(uuid.uuid4())
            full_name = data.full_name.strip()

            hashed_password = get_password_hash(data.password)
            new_user = User(
                id=user_id,
                email=email,
                mobile=mobile,
                hashed_password=hashed_password,
                is_active=True,
                is_verified=False,
            )
            session.add(new_user)

            # Create linked profile
            new_profile = UserProfile(
                id=profile_id,
                user_id=user_id,
                full_name=full_name,
                email=email,
                mobile=mobile,
                level="Beginner",
                education_mode="general",
            )
            session.add(new_profile)

            # Issue access & refresh tokens with a new session family
            access_token = create_access_token(user_id=user_id, email=email)
            raw_refresh, token_hash_val, expires_at, session_family_id = create_refresh_token(
                user_id=user_id, session_family_id=session_family_id
            )

            refresh_entry = RefreshToken(
                id=str(uuid.uuid4()),
                user_id=user_id,
                token_hash=token_hash_val,
                session_family_id=session_family_id,
                expires_at=expires_at,
                revoked=False,
            )
            session.add(refresh_entry)

            await session.commit()

            logger.info(f"User registered: id={user_id} email={email}")

            return AuthTokensResponse(
                accessToken=access_token,
                refreshToken=raw_refresh,
                tokenType="Bearer",
                expiresIn=settings.access_token_expire_minutes * 60,
                user=UserSummary(
                    id=user_id,
                    email=email,
                    mobile=mobile,
                    fullName=full_name,
                ),
            )

        return await self._run_with_resilience(_do_register)

    async def login(self, data: LoginRequest) -> AuthTokensResponse:
        """Authenticate user by email or mobile and issue tokens."""
        async def _do_login(session: AsyncSession) -> AuthTokensResponse:
            contact = data.contact.strip()
            contact_lower = contact.lower()

            conditions = [User.email == contact_lower, User.mobile == contact]
            contact_digits = "".join(filter(str.isdigit, contact))
            if contact_digits:
                if len(contact_digits) == 10:
                    conditions.append(User.mobile == f"+91{contact_digits}")
                    conditions.append(User.mobile == contact_digits)
                elif len(contact_digits) == 12 and contact_digits.startswith("91"):
                    conditions.append(User.mobile == f"+{contact_digits}")
                    conditions.append(User.mobile == contact_digits[2:])

            stmt = select(User).where(or_(*conditions))
            user = await session.scalar(stmt)

            if not user or not verify_password(data.password, user.hashed_password):
                raise AuthException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code=INVALID_CREDENTIALS,
                    message="Invalid email/mobile or password",
                )

            if not user.is_active:
                raise AuthException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    code=ACCOUNT_DISABLED,
                    message="User account is inactive",
                )

            user_id = user.id
            user_email = user.email
            user_mobile = user.mobile

            profile = await session.scalar(
                select(UserProfile).where(UserProfile.user_id == user_id)
            )
            full_name = profile.full_name if profile else user_email.split("@")[0]

            access_token = create_access_token(user_id=user_id, email=user_email)
            session_family_id = str(uuid.uuid4())
            raw_refresh, token_hash_val, expires_at, session_family_id = create_refresh_token(
                user_id=user_id, session_family_id=session_family_id
            )

            refresh_entry = RefreshToken(
                id=str(uuid.uuid4()),
                user_id=user_id,
                token_hash=token_hash_val,
                session_family_id=session_family_id,
                expires_at=expires_at,
                revoked=False,
            )
            session.add(refresh_entry)
            await session.commit()

            logger.info(f"User logged in: id={user_id}")

            return AuthTokensResponse(
                accessToken=access_token,
                refreshToken=raw_refresh,
                tokenType="Bearer",
                expiresIn=settings.access_token_expire_minutes * 60,
                user=UserSummary(
                    id=user_id,
                    email=user_email,
                    mobile=user_mobile,
                    fullName=full_name,
                ),
            )

        return await self._run_with_resilience(_do_login)

    async def refresh_tokens(self, raw_refresh_token: str) -> AuthTokensResponse:
        """Validate refresh token and issue new token pair (refresh token rotation)."""
        async def _do_refresh(session: AsyncSession) -> AuthTokensResponse:
            hashed = hash_token(raw_refresh_token)
            now = datetime.now(timezone.utc)

            # Look up by hash regardless of revoked status (to detect reuse)
            token_entry = await session.scalar(
                select(RefreshToken).where(RefreshToken.token_hash == hashed)
            )

            if not token_entry:
                raise AuthException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code=INVALID_TOKEN,
                    message="Invalid or expired refresh token",
                )

            # Token reuse detection: token was already revoked
            if token_entry.revoked:
                logger.warning(
                    f"token_reuse_detected: user_id={token_entry.user_id} "
                    f"session_family_id={token_entry.session_family_id}"
                )
                # Revoke ALL tokens in this session family
                await session.execute(
                    update(RefreshToken)
                    .where(
                        RefreshToken.session_family_id == token_entry.session_family_id,
                        RefreshToken.revoked.is_(False),
                    )
                    .values(revoked=True)
                )
                await session.commit()
                raise AuthException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code=SESSION_REVOKED,
                    message="Session revoked due to token reuse detected",
                )

            # Check expiry
            token_exp = token_entry.expires_at
            if token_exp.tzinfo is None:
                token_exp = token_exp.replace(tzinfo=timezone.utc)
            if token_exp <= now:
                raise AuthException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code=TOKEN_EXPIRED,
                    message="Refresh token has expired",
                )

            user = await session.scalar(
                select(User).where(User.id == token_entry.user_id)
            )
            if not user or not user.is_active:
                raise AuthException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code=ACCOUNT_DISABLED,
                    message="User account not found or disabled",
                )

            user_id = user.id
            user_email = user.email
            user_mobile = user.mobile
            family_id = token_entry.session_family_id

            profile = await session.scalar(
                select(UserProfile).where(UserProfile.user_id == user_id)
            )
            full_name = profile.full_name if profile else user_email.split("@")[0]

            # Revoke old token
            token_entry.revoked = True

            # Issue new token pair inheriting the same session family
            access_token = create_access_token(user_id=user_id, email=user_email)
            raw_refresh, new_token_hash, new_expires, _ = create_refresh_token(
                user_id=user_id, session_family_id=family_id
            )

            new_entry = RefreshToken(
                id=str(uuid.uuid4()),
                user_id=user_id,
                token_hash=new_token_hash,
                session_family_id=family_id,
                expires_at=new_expires,
                revoked=False,
            )
            session.add(new_entry)
            await session.commit()

            return AuthTokensResponse(
                accessToken=access_token,
                refreshToken=raw_refresh,
                tokenType="Bearer",
                expiresIn=settings.access_token_expire_minutes * 60,
                user=UserSummary(
                    id=user_id,
                    email=user_email,
                    mobile=user_mobile,
                    fullName=full_name,
                ),
            )

        return await self._run_with_resilience(_do_refresh)

    async def logout(self, user_id: str, refresh_token: str | None = None) -> None:
        """
        Revoke refresh tokens for the user.
        - If refresh_token is provided: revoke only that specific token (current-session logout).
        - If not provided: revoke ALL tokens for the user (all-sessions logout).
        """
        async def _do_logout(session: AsyncSession) -> None:
            if refresh_token:
                hashed = hash_token(refresh_token)
                await session.execute(
                    update(RefreshToken)
                    .where(
                        RefreshToken.user_id == user_id,
                        RefreshToken.token_hash == hashed,
                        RefreshToken.revoked.is_(False),
                    )
                    .values(revoked=True)
                )
                logger.info(f"User logged out (single session): user_id={user_id}")
            else:
                await session.execute(
                    update(RefreshToken)
                    .where(
                        RefreshToken.user_id == user_id,
                        RefreshToken.revoked.is_(False),
                    )
                    .values(revoked=True)
                )
                logger.info(f"User logged out (all sessions): user_id={user_id}")
            await session.commit()

        return await self._run_with_resilience(_do_logout)

    async def forgot_password(self, contact: str) -> tuple[str, Optional[str]]:
        """
        Initiate password reset.
        Returns: (generic_message, dev_reset_token)
        """
        async def _do_forgot(session: AsyncSession) -> tuple[str, Optional[str]]:
            contact_clean = contact.strip()
            contact_lower = contact_clean.lower()
            conditions = [User.email == contact_lower, User.mobile == contact_clean]
            contact_digits = "".join(filter(str.isdigit, contact_clean))
            if contact_digits:
                if len(contact_digits) == 10:
                    conditions.append(User.mobile == f"+91{contact_digits}")
                    conditions.append(User.mobile == contact_digits)
                elif len(contact_digits) == 12 and contact_digits.startswith("91"):
                    conditions.append(User.mobile == f"+{contact_digits}")
                    conditions.append(User.mobile == contact_digits[2:])

            stmt = select(User).where(or_(*conditions))
            user = await session.scalar(stmt)

            dev_token: Optional[str] = None
            if user:
                raw_token, token_hash_val, expires_at = create_reset_token(user.id)
                reset_entry = PasswordResetToken(
                    id=str(uuid.uuid4()),
                    user_id=user.id,
                    token_hash=token_hash_val,
                    expires_at=expires_at,
                    used=False,
                )
                session.add(reset_entry)
                await session.commit()

                logger.info(f"Password reset token generated for user_id={user.id}")
                await self.delivery_service.deliver(user, raw_token)
                dev_token = self.delivery_service.get_dev_token(raw_token)

            return (
                "If an account exists with these details, password reset instructions will be sent.",
                dev_token,
            )

        return await self._run_with_resilience(_do_forgot)

    async def reset_password(self, token_str: str, new_password: str) -> None:
        """Reset password with a valid reset token."""
        async def _do_reset(session: AsyncSession) -> None:
            hashed = hash_token(token_str)
            now = datetime.now(timezone.utc)

            stmt = select(PasswordResetToken).where(
                PasswordResetToken.token_hash == hashed,
                PasswordResetToken.used.is_(False),
                PasswordResetToken.expires_at > now,
            )
            reset_entry = await session.scalar(stmt)

            if not reset_entry:
                raise AuthException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    code=PASSWORD_RESET_INVALID,
                    message="Invalid or expired password reset token",
                )

            user = await session.scalar(
                select(User).where(User.id == reset_entry.user_id)
            )
            if not user:
                raise AuthException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    code=PASSWORD_RESET_INVALID,
                    message="User account not found",
                )

            user.hashed_password = get_password_hash(new_password)
            reset_entry.used = True

            # Invalidate all refresh tokens for safety after password change
            await session.execute(
                update(RefreshToken)
                .where(RefreshToken.user_id == user.id, RefreshToken.revoked.is_(False))
                .values(revoked=True)
            )

            await session.commit()
            logger.info(f"Password reset successful for user_id={user.id}")

        return await self._run_with_resilience(_do_reset)

    async def get_me(self, user: User) -> UserMeResponse:
        """Get profile and account details for the authenticated user."""
        async def _do_get_me(session: AsyncSession) -> UserMeResponse:
            user_id = user.id
            user_email = user.email
            user_mobile = user.mobile
            user_active = user.is_active

            profile = await session.scalar(
                select(UserProfile).where(UserProfile.user_id == user_id)
            )

            return UserMeResponse(
                id=user_id,
                email=user_email,
                mobile=user_mobile,
                fullName=profile.full_name if profile else user_email.split("@")[0],
                interests=profile.interests if profile else None,
                level=profile.level if profile else "Beginner",
                educationMode=profile.education_mode if profile else "general",
                board=profile.board if profile else None,
                classGrade=profile.class_grade if profile else None,
                subject=profile.subject if profile else None,
                preferredLanguage=profile.preferred_language if profile else "en",
                isActive=user_active,
            )

        return await self._run_with_resilience(_do_get_me)
