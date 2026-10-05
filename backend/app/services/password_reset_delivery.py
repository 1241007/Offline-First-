import asyncio
import email.message
import logging
import smtplib
from abc import ABC, abstractmethod
from typing import Optional
import httpx

from app.core.config import settings
from app.models.user import User

logger = logging.getLogger(__name__)


class PasswordResetDeliveryService(ABC):
    @abstractmethod
    async def deliver(self, user: User, raw_token: str) -> None:
        """Deliver the raw reset token to the user via the appropriate channel."""
        ...

    def get_dev_token(self, raw_token: str) -> str | None:
        """Return the raw token for response body if in development mode, else None."""
        return None


class DevPasswordResetDeliveryService(PasswordResetDeliveryService):
    """
    Development-only delivery service.

    - In development: logs the token at DEBUG level (visible in server output).
    - In production: no-op (token is NOT returned or logged).
    """

    async def deliver(self, user: User, raw_token: str) -> None:
        if settings.environment == "development":
            logger.debug(
                f"[DEV] Password reset token for user_id={user.id} "
                f"email={user.email}: {raw_token}"
            )

    def get_dev_token(self, raw_token: str) -> str | None:
        if settings.environment == "development":
            return raw_token
        return None


class ProductionEmailPasswordResetDeliveryService(PasswordResetDeliveryService):
    """
    Production transactional email delivery service supporting Resend API and standard SMTP.
    """

    def __init__(self):
        self.provider = settings.email_provider.lower().strip()
        self.api_key = settings.email_api_key
        self.email_from = settings.email_from
        self.public_url = settings.backend_public_url.rstrip("/")

    def _build_email_contents(self, user: User, raw_token: str) -> tuple[str, str, str]:
        subject = "Reset your EduNova password"
        reset_url = f"{self.public_url}/api/v1/auth/reset-password?token={raw_token}"
        expires_min = settings.reset_token_expire_minutes

        text_content = (
            f"Hello,\n\n"
            f"We received a request to reset the password for your EduNova account ({user.email}).\n\n"
            f"To choose a new password, open the link below:\n"
            f"{reset_url}\n\n"
            f"This link will expire in {expires_min} minutes.\n"
            f"If you did not request a password reset, you can safely ignore this email.\n\n"
            f"— The EduNova Team\n"
        )

        html_content = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Reset your EduNova password</title>
</head>
<body style="margin: 0; padding: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f7f9fc; color: #1e293b;">
  <div style="max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 14px; padding: 32px; box-shadow: 0 4px 18px rgba(0,0,0,0.06);">
    <div style="text-align: center; margin-bottom: 24px;">
      <h1 style="color: #4f46e5; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">EduNova</h1>
      <p style="margin: 4px 0 0 0; color: #64748b; font-size: 14px;">Next-Gen Offline-First Learning Platform</p>
    </div>
    <div style="border-top: 1px solid #e2e8f0; padding-top: 20px;">
      <p style="font-size: 15px; line-height: 1.5; margin: 0 0 16px 0;">Hello,</p>
      <p style="font-size: 15px; line-height: 1.5; margin: 0 0 20px 0;">
        We received a request to reset the password for your account (<strong>{user.email}</strong>).
      </p>
      <div style="text-align: center; margin: 28px 0;">
        <a href="{reset_url}" style="background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 14px 30px; font-size: 15px; font-weight: 600; border-radius: 10px; display: inline-block;">
          Reset Password
        </a>
      </div>
      <p style="font-size: 13px; color: #64748b; margin: 20px 0 6px 0;">
        This password reset link will expire in <strong>{expires_min} minutes</strong>.
      </p>
      <p style="font-size: 13px; color: #94a3b8; margin: 0 0 16px 0;">
        If you did not request a password reset, please ignore this email. Your current password remains safe and unchanged.
      </p>
    </div>
    <div style="border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px; text-align: center; font-size: 12px; color: #94a3b8;">
      &copy; EduNova Platform. All rights reserved.
    </div>
  </div>
</body>
</html>"""
        return subject, text_content, html_content

    async def deliver(self, user: User, raw_token: str) -> None:
        subject, text_content, html_content = self._build_email_contents(user, raw_token)

        # 1. Resend REST API provider
        if self.provider == "resend" or (self.api_key and self.provider != "smtp"):
            await self._deliver_resend(user.email, subject, text_content, html_content)
        # 2. Standard SMTP provider
        elif self.provider == "smtp" or settings.smtp_host:
            await self._deliver_smtp(user.email, subject, text_content, html_content)
        else:
            logger.warning(
                f"No production email provider configured for user_id={user.id}. "
                f"Set EMAIL_PROVIDER=resend with EMAIL_API_KEY, or EMAIL_PROVIDER=smtp."
            )

    async def _deliver_resend(self, to_email: str, subject: str, text: str, html: str) -> None:
        if not self.api_key:
            logger.error("Resend delivery failed: EMAIL_API_KEY is not configured.")
            return

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "from": self.email_from,
            "to": [to_email],
            "subject": subject,
            "html": html,
            "text": text,
        }

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(
                    "https://api.resend.com/emails",
                    headers=headers,
                    json=payload,
                )
                if response.status_code in (200, 201):
                    logger.info(f"Password reset email sent via Resend to {to_email}")
                else:
                    logger.error(
                        f"Resend email delivery error: status={response.status_code} "
                        f"response={response.text}"
                    )
        except Exception as e:
            logger.error(f"Failed to send email via Resend: {str(e)}")

    async def _deliver_smtp(self, to_email: str, subject: str, text: str, html: str) -> None:
        if not settings.smtp_host:
            logger.error("SMTP delivery failed: SMTP_HOST is not configured.")
            return

        def _send_sync():
            msg = email.message.EmailMessage()
            msg["Subject"] = subject
            msg["From"] = self.email_from
            msg["To"] = to_email
            msg.set_content(text)
            msg.add_alternative(html, subtype="html")

            server = smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10)
            try:
                if settings.smtp_use_tls:
                    server.starttls()
                if settings.smtp_username and settings.smtp_password:
                    server.login(settings.smtp_username, settings.smtp_password)
                server.send_message(msg)
                logger.info(f"Password reset email sent via SMTP to {to_email}")
            finally:
                server.quit()

        try:
            await asyncio.to_thread(_send_sync)
        except Exception as e:
            logger.error(f"Failed to send email via SMTP: {str(e)}")

    def get_dev_token(self, raw_token: str) -> str | None:
        # In production email delivery, never leak dev token
        if settings.environment == "development":
            return raw_token
        return None


def get_default_delivery_service() -> PasswordResetDeliveryService:
    if settings.email_provider in ("resend", "smtp") or settings.email_api_key or settings.smtp_host:
        return ProductionEmailPasswordResetDeliveryService()
    if settings.environment != "development":
        return ProductionEmailPasswordResetDeliveryService()
    return DevPasswordResetDeliveryService()


default_delivery_service = get_default_delivery_service()
