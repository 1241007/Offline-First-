"""Authentication API route handlers"""

from fastapi import APIRouter, Depends, Request, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.rate_limit import limiter
from app.models.user import User
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    RefreshTokenRequest,
    LogoutRequest,
    AuthTokensResponse,
    UserMeResponse,
    ForgotPasswordRequest,
    ForgotPasswordResponse,
    ResetPasswordRequest,
    AuthMessageResponse,
)
from app.services.auth_service import AuthService

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/register", response_model=AuthTokensResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit("3/minute")
async def register(
    request: Request,
    data: RegisterRequest,
    db: AsyncSession = Depends(get_db),
):
    """Register a new user account with full name, email, mobile, and password."""
    service = AuthService(db)
    return await service.register(data)


@router.post("/login", response_model=AuthTokensResponse, status_code=status.HTTP_200_OK)
@limiter.limit("5/minute")
async def login(
    request: Request,
    data: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """Authenticate with email or mobile number and password."""
    service = AuthService(db)
    return await service.login(data)


@router.post("/refresh", response_model=AuthTokensResponse, status_code=status.HTTP_200_OK)
@limiter.limit("10/minute")
async def refresh_tokens(
    request: Request,
    data: RefreshTokenRequest,
    db: AsyncSession = Depends(get_db),
):
    """Exchange a valid refresh token for a new access token and rotated refresh token."""
    service = AuthService(db)
    return await service.refresh_tokens(data.refresh_token)


@router.get("/me", response_model=UserMeResponse, status_code=status.HTTP_200_OK)
async def get_me(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Retrieve profile and authentication details for the currently logged-in user."""
    service = AuthService(db)
    return await service.get_me(current_user)


@router.post("/logout", response_model=AuthMessageResponse, status_code=status.HTTP_200_OK)
async def logout(
    data: LogoutRequest = LogoutRequest(),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Revoke refresh tokens for the current user (single session or all sessions)."""
    service = AuthService(db)
    await service.logout(current_user.id, data.refresh_token)
    return AuthMessageResponse(message="Successfully logged out")


@router.post("/forgot-password", response_model=ForgotPasswordResponse, status_code=status.HTTP_200_OK)
@limiter.limit("3/minute")
async def forgot_password(
    request: Request,
    data: ForgotPasswordRequest,
    db: AsyncSession = Depends(get_db),
):
    """Request a password reset link/token."""
    service = AuthService(db)
    message, dev_token = await service.forgot_password(data.contact)
    return ForgotPasswordResponse(message=message, devResetToken=dev_token)


@router.post("/reset-password", response_model=AuthMessageResponse, status_code=status.HTTP_200_OK)
@limiter.limit("5/minute")
async def reset_password(
    request: Request,
    data: ResetPasswordRequest,
    db: AsyncSession = Depends(get_db),
):
    """Reset account password using a valid reset token."""
    service = AuthService(db)
    await service.reset_password(data.token, data.new_password)
    return AuthMessageResponse(message="Password reset successfully")
