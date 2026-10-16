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


from fastapi.responses import HTMLResponse
from app.core.config import settings

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
    if settings.environment != "development":
        dev_token = None
    return ForgotPasswordResponse(message=message, devResetToken=dev_token)


@router.get("/reset-password", response_class=HTMLResponse)
async def reset_password_page(token: str = ""):
    """Serve a secure, responsive HTML password reset form for web/mobile browsers."""
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Reset Password — EduNova</title>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }}
    body {{ background: #f8fafc; color: #1e293b; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }}
    .card {{ background: #ffffff; border-radius: 16px; padding: 36px; max-width: 440px; width: 100%; box-shadow: 0 10px 25px rgba(0,0,0,0.06); }}
    .brand {{ text-align: center; margin-bottom: 24px; }}
    .brand h1 {{ color: #4f46e5; font-size: 28px; font-weight: 800; }}
    .brand p {{ color: #64748b; font-size: 14px; margin-top: 4px; }}
    .form-group {{ margin-bottom: 18px; }}
    label {{ display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; }}
    input {{ width: 100%; padding: 12px 14px; border: 1.5px solid #cbd5e1; border-radius: 10px; font-size: 15px; outline: none; transition: border-color 0.2s; }}
    input:focus {{ border-color: #4f46e5; }}
    button {{ width: 100%; background: #4f46e5; color: #ffffff; border: none; padding: 14px; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.2s; }}
    button:hover {{ background: #4338ca; }}
    button:disabled {{ background: #94a3b8; cursor: not-allowed; }}
    .alert {{ padding: 12px; border-radius: 8px; font-size: 14px; margin-bottom: 16px; display: none; }}
    .alert-error {{ background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }}
    .alert-success {{ background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }}
  </style>
</head>
<body>
  <div class="card">
    <div class="brand">
      <h1>EduNova</h1>
      <p>Set a new password for your account</p>
    </div>
    <div id="error-box" class="alert alert-error"></div>
    <div id="success-box" class="alert alert-success"></div>
    <form id="reset-form" onsubmit="submitReset(event)">
      <input type="hidden" id="token" value="{token}">
      <div class="form-group">
        <label for="password">New Password</label>
        <input type="password" id="password" minlength="8" required placeholder="Minimum 8 characters">
      </div>
      <div class="form-group">
        <label for="confirm-password">Confirm New Password</label>
        <input type="password" id="confirm-password" minlength="8" required placeholder="Re-enter password">
      </div>
      <button type="submit" id="submit-btn">Reset Password</button>
    </form>
  </div>
  <script>
    async function submitReset(e) {{
      e.preventDefault();
      const token = document.getElementById('token').value.trim();
      const password = document.getElementById('password').value;
      const confirm = document.getElementById('confirm-password').value;
      const errBox = document.getElementById('error-box');
      const succBox = document.getElementById('success-box');
      const btn = document.getElementById('submit-btn');

      errBox.style.display = 'none';
      succBox.style.display = 'none';

      if (!token) {{
        errBox.innerText = 'Reset token is missing from the link.';
        errBox.style.display = 'block';
        return;
      }}
      if (password !== confirm) {{
        errBox.innerText = 'Passwords do not match.';
        errBox.style.display = 'block';
        return;
      }}
      if (password.length < 8) {{
        errBox.innerText = 'Password must be at least 8 characters.';
        errBox.style.display = 'block';
        return;
      }}

      btn.disabled = true;
      btn.innerText = 'Updating password...';

      try {{
        const res = await fetch('/api/v1/auth/reset-password', {{
          method: 'POST',
          headers: {{ 'Content-Type': 'application/json' }},
          body: JSON.stringify({{ token: token, newPassword: password }})
        }});
        const data = await res.json();
        if (res.ok) {{
          document.getElementById('reset-form').style.display = 'none';
          succBox.innerText = 'Your password has been reset successfully! You can now log in using your new password in the EduNova app.';
          succBox.style.display = 'block';
        }} else {{
          errBox.innerText = data.message || 'Failed to reset password. The link may have expired.';
          errBox.style.display = 'block';
          btn.disabled = false;
          btn.innerText = 'Reset Password';
        }}
      }} catch (err) {{
        errBox.innerText = 'Network error. Please try again.';
        errBox.style.display = 'block';
        btn.disabled = false;
        btn.innerText = 'Reset Password';
      }}
    }}
  </script>
</body>
</html>"""
    return HTMLResponse(content=html_content)


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
