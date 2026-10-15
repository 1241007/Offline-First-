"""Pydantic schemas for authentication and authorization"""

from typing import Optional
from pydantic import BaseModel, ConfigDict, EmailStr, Field


class RegisterRequest(BaseModel):
    full_name: str = Field(..., alias="fullName", min_length=1)
    email: EmailStr
    mobile: Optional[str] = None
    password: str = Field(..., min_length=8)

    model_config = ConfigDict(populate_by_name=True)


class LoginRequest(BaseModel):
    contact: str = Field(..., min_length=1)
    password: str = Field(..., min_length=1)

    model_config = ConfigDict(populate_by_name=True)


class RefreshTokenRequest(BaseModel):
    refresh_token: str = Field(..., alias="refreshToken")

    model_config = ConfigDict(populate_by_name=True)


class ForgotPasswordRequest(BaseModel):
    contact: str = Field(..., min_length=1)

    model_config = ConfigDict(populate_by_name=True)


class ResetPasswordRequest(BaseModel):
    token: str = Field(..., min_length=1)
    new_password: str = Field(..., alias="newPassword", min_length=8)

    model_config = ConfigDict(populate_by_name=True)


class UserSummary(BaseModel):
    id: str
    email: str
    mobile: Optional[str] = None
    full_name: str = Field(..., alias="fullName")

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)


class AuthTokensResponse(BaseModel):
    access_token: str = Field(..., alias="accessToken")
    refresh_token: str = Field(..., alias="refreshToken")
    token_type: str = Field("Bearer", alias="tokenType")
    expires_in: int = Field(..., alias="expiresIn")
    user: UserSummary

    model_config = ConfigDict(populate_by_name=True)


class UserMeResponse(BaseModel):
    id: str
    email: str
    mobile: Optional[str] = None
    full_name: str = Field(..., alias="fullName")
    interests: Optional[str] = None
    level: str = "Beginner"
    education_mode: str = Field("general", alias="educationMode")
    is_active: bool = Field(True, alias="isActive")

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)


class ForgotPasswordResponse(BaseModel):
    message: str
    dev_reset_token: Optional[str] = Field(None, alias="devResetToken")

    model_config = ConfigDict(populate_by_name=True)


class LogoutRequest(BaseModel):
    refresh_token: Optional[str] = Field(None, alias="refreshToken")

    model_config = ConfigDict(populate_by_name=True)


class AuthMessageResponse(BaseModel):
    message: str

    model_config = ConfigDict(populate_by_name=True)
