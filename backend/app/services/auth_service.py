"""Authentication service business logic"""

import logging
import uuid
from datetime import datetime, timezone

logger = logging.getLogger(__name__)
from typing import Optional
from fastapi import status
from sqlalchemy import select, or_, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.errors import (
    AuthException,
    INVALID_CREDENTIALS,
    ACCOUNT_EXISTS,
    ACCOUNT_DISABLED,
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    SESSION_REVOKED,
    PASSWORD_RESET_INVALID,
)
from app.core.security import (
    get_password_hash,
    verify_password,
    create_access_token,
    create_refresh_token,
    create_reset_token,
    hash_token,
)
from app.models.user import User, RefreshToken, PasswordResetToken
from app.models.profile import UserProfile
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    AuthTokensResponse,
    UserSummary,
    UserMeResponse,
)

from app.services.password_reset_delivery import (
    PasswordResetDeliveryService,
    default_delivery_service,
)


class AuthService:
    def __init__(
        self,
        db: AsyncSession,
        delivery_service: PasswordResetDeliveryService = default_delivery_service,
    ):
        self.db = db
        self.delivery_service = delivery_service

    async def register(self, data: RegisterRequest) -> AuthTokensResponse:
        """Register a new user and create their initial user profile."""
        email = data.email.strip().lower()
        mobile = data.mobile.strip() if data.mobile else None

        # Check existing email
        existing_email = await self.db.scalar(
            select(User).where(User.email == email)
        )
        if existing_email:
            raise AuthException(
                status_code=status.HTTP_409_CONFLICT,
                code=ACCOUNT_EXISTS,
                message="An account with this email already exists",
            )

        # Check existing mobile if provided
        if mobile:
            existing_mobile = await self.db.scalar(
                select(User).where(User.mobile == mobile)
            )
            if existing_mobile:
                raise AuthException(
                    status_code=status.HTTP_409_CONFLICT,
                    code=ACCOUNT_EXISTS,
                    message="An account with this mobile number already exists",
                )

        hashed_password = get_password_hash(data.password)
        new_user = User(
            email=email,
            mobile=mobile,
            hashed_password=hashed_password,
            is_active=True,
            is_verified=False,
        )
        self.db.add(new_user)
        await self.db.flush()

        # Create linked profile
        new_profile = UserProfile(
            user_id=new_user.id,
            full_name=data.full_name.strip(),
            email=email,
            mobile=mobile,
            level="Beginner",
            education_mode="general",
        )
        self.db.add(new_profile)

        # Issue access & refresh tokens with a new session family
        access_token = create_access_token(user_id=new_user.id, email=email)
        session_family_id = str(uuid.uuid4())
        raw_refresh, token_hash_val, expires_at, session_family_id = create_refresh_token(
            user_id=new_user.id, session_family_id=session_family_id
        )

        refresh_entry = RefreshToken(
            user_id=new_user.id,
            token_hash=token_hash_val,
            session_family_id=session_family_id,
            expires_at=expires_at,
            revoked=False,
        )
        self.db.add(refresh_entry)
        await self.db.commit()

        logger.info(f"User registered: id={new_user.id} email={email}")

        return AuthTokensResponse(
            accessToken=access_token,
            refreshToken=raw_refresh,
            tokenType="Bearer",
            expiresIn=settings.access_token_expire_minutes * 60,
            user=UserSummary(
                id=new_user.id,
                email=new_user.email,
                mobile=new_user.mobile,
                fullName=new_profile.full_name,
            ),
        )

    async def login(self, data: LoginRequest) -> AuthTokensResponse:
        """Authenticate user by email or mobile and issue tokens."""
        contact = data.contact.strip()
        contact_lower = contact.lower()

        stmt = select(User).where(
            or_(User.email == contact_lower, User.mobile == contact)
        )
        user = await self.db.scalar(stmt)

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

        profile = await self.db.scalar(
            select(UserProfile).where(UserProfile.user_id == user.id)
        )
        full_name = profile.full_name if profile else user.email.split("@")[0]

        access_token = create_access_token(user_id=user.id, email=user.email)
        session_family_id = str(uuid.uuid4())
        raw_refresh, token_hash_val, expires_at, session_family_id = create_refresh_token(
            user_id=user.id, session_family_id=session_family_id
        )

        refresh_entry = RefreshToken(
            user_id=user.id,
            token_hash=token_hash_val,
            session_family_id=session_family_id,
            expires_at=expires_at,
            revoked=False,
        )
        self.db.add(refresh_entry)
        await self.db.commit()

        logger.info(f"User logged in: id={user.id}")

        return AuthTokensResponse(
            accessToken=access_token,
            refreshToken=raw_refresh,
            tokenType="Bearer",
            expiresIn=settings.access_token_expire_minutes * 60,
            user=UserSummary(
                id=user.id,
                email=user.email,
                mobile=user.mobile,
                fullName=full_name,
            ),
        )

    async def refresh_tokens(self, raw_refresh_token: str) -> AuthTokensResponse:
        """Validate refresh token and issue new token pair (refresh token rotation)."""
        hashed = hash_token(raw_refresh_token)
        now = datetime.now(timezone.utc)

        # Look up by hash regardless of revoked status (to detect reuse)
        token_entry = await self.db.scalar(
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
            await self.db.execute(
                update(RefreshToken)
                .where(
                    RefreshToken.session_family_id == token_entry.session_family_id,
                    RefreshToken.revoked.is_(False),
                )
                .values(revoked=True)
            )
            await self.db.commit()
            raise AuthException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code=SESSION_REVOKED,
                message="Session revoked due to token reuse detected",
            )

        # Check expiry
        if token_entry.expires_at.replace(tzinfo=timezone.utc) <= now:
            raise AuthException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code=TOKEN_EXPIRED,
                message="Refresh token has expired",
            )

        user = await self.db.scalar(
            select(User).where(User.id == token_entry.user_id)
        )
        if not user or not user.is_active:
            raise AuthException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code=ACCOUNT_DISABLED,
                message="User account not found or disabled",
            )

        # Revoke old token
        token_entry.revoked = True

        # Issue new token pair inheriting the same session family
        access_token = create_access_token(user_id=user.id, email=user.email)
        raw_refresh, new_token_hash, new_expires, _ = create_refresh_token(
            user_id=user.id, session_family_id=token_entry.session_family_id
        )

        new_entry = RefreshToken(
            user_id=user.id,
            token_hash=new_token_hash,
            session_family_id=token_entry.session_family_id,
            expires_at=new_expires,
            revoked=False,
        )
        self.db.add(new_entry)
        await self.db.commit()

        profile = await self.db.scalar(
            select(UserProfile).where(UserProfile.user_id == user.id)
        )
        full_name = profile.full_name if profile else user.email.split("@")[0]

        return AuthTokensResponse(
            accessToken=access_token,
            refreshToken=raw_refresh,
            tokenType="Bearer",
            expiresIn=settings.access_token_expire_minutes * 60,
            user=UserSummary(
                id=user.id,
                email=user.email,
                mobile=user.mobile,
                fullName=full_name,
            ),
        )

    async def logout(self, user_id: str, refresh_token: str | None = None) -> None:
        """
        Revoke refresh tokens for the user.
        - If refresh_token is provided: revoke only that specific token (current-session logout).
        - If not provided: revoke ALL tokens for the user (all-sessions logout).
        """
        if refresh_token:
            hashed = hash_token(refresh_token)
            await self.db.execute(
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
            await self.db.execute(
                update(RefreshToken)
                .where(
                    RefreshToken.user_id == user_id,
                    RefreshToken.revoked.is_(False),
                )
                .values(revoked=True)
            )
            logger.info(f"User logged out (all sessions): user_id={user_id}")
        await self.db.commit()

    async def forgot_password(self, contact: str) -> tuple[str, Optional[str]]:
        """
        Initiate password reset.
        Returns: (generic_message, dev_reset_token)
        """
        contact_clean = contact.strip()
        stmt = select(User).where(
            or_(User.email == contact_clean.lower(), User.mobile == contact_clean)
        )
        user = await self.db.scalar(stmt)

        dev_token: Optional[str] = None
        if user:
            raw_token, token_hash_val, expires_at = create_reset_token(user.id)
            reset_entry = PasswordResetToken(
                user_id=user.id,
                token_hash=token_hash_val,
                expires_at=expires_at,
                used=False,
            )
            self.db.add(reset_entry)
            await self.db.commit()

            logger.info(f"Password reset token generated for user_id={user.id}")
            await self.delivery_service.deliver(user, raw_token)
            dev_token = self.delivery_service.get_dev_token(raw_token)

        return (
            "If an account exists with these details, password reset instructions will be sent.",
            dev_token,
        )

    async def reset_password(self, token_str: str, new_password: str) -> None:
        """Reset password with a valid reset token."""
        hashed = hash_token(token_str)
        now = datetime.now(timezone.utc)

        stmt = select(PasswordResetToken).where(
            PasswordResetToken.token_hash == hashed,
            PasswordResetToken.used.is_(False),
            PasswordResetToken.expires_at > now,
        )
        reset_entry = await self.db.scalar(stmt)

        if not reset_entry:
            raise AuthException(
                status_code=status.HTTP_400_BAD_REQUEST,
                code=PASSWORD_RESET_INVALID,
                message="Invalid or expired password reset token",
            )

        user = await self.db.scalar(
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
        await self.db.execute(
            update(RefreshToken)
            .where(RefreshToken.user_id == user.id, RefreshToken.revoked.is_(False))
            .values(revoked=True)
        )

        await self.db.commit()
        logger.info(f"Password reset successful for user_id={user.id}")

    async def get_me(self, user: User) -> UserMeResponse:
        """Get profile and account details for the authenticated user."""
        profile = await self.db.scalar(
            select(UserProfile).where(UserProfile.user_id == user.id)
        )

        return UserMeResponse(
            id=user.id,
            email=user.email,
            mobile=user.mobile,
            fullName=profile.full_name if profile else user.email.split("@")[0],
            interests=profile.interests if profile else None,
            level=profile.level if profile else "Beginner",
            educationMode=profile.education_mode if profile else "general",
            isActive=user.is_active,
        )
