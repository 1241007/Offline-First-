"""FastAPI dependencies for authentication and database sessions"""

from typing import Optional
from fastapi import Query, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db
from app.core.security import decode_access_token
from app.models.user import User

security_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    """
    Dependency that extracts the JWT Bearer token, decodes it,
    and returns the authenticated User instance.
    """
    if not credentials or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication credentials were not provided",
            headers={"WWW-Authenticate": "Bearer"},
        )

    payload = decode_access_token(credentials.credentials)
    if not payload or not payload.get("sub"):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired access token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user_id = payload["sub"]
    user = await db.scalar(select(User).where(User.id == user_id))

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive",
        )

    return user


async def get_current_user_id(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_scheme),
    user_id: Optional[str] = Query(None, description="User ID (development only)"),
    db: AsyncSession = Depends(get_db),
) -> str:
    """
    Dependency that extracts user_id from JWT Bearer token.
    Falls back to user_id query parameter ONLY if allow_dev_user_id is True and environment == 'development'.
    """
    if credentials and credentials.credentials:
        payload = decode_access_token(credentials.credentials)
        if not payload or not payload.get("sub"):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired access token",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return payload["sub"]

    # Fallback to dev query parameter if enabled and environment is strictly development
    if (
        settings.allow_dev_user_id
        and settings.environment == "development"
        and user_id
    ):
        return user_id

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Authentication required",
        headers={"WWW-Authenticate": "Bearer"},
    )


async def get_dev_user_id(
    user_id: str = Query(..., description="User ID (development only)")
) -> str:
    """Legacy helper kept for backward compatibility."""
    if not (settings.allow_dev_user_id and settings.environment == "development"):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="user_id query parameter not allowed in production. Authentication required."
        )
    if not user_id:
        raise HTTPException(
            status_code=400,
            detail="user_id required in development mode"
        )
    return user_id
