"""Password reset delivery abstraction.

Switch from dev-only (log/response) to production (email/SMS) by implementing
a new subclass and injecting it — no auth route logic changes required.
"""
import logging
from abc import ABC, abstractmethod

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
        # Production: intentionally no-op — token delivery via email/SMS not yet configured

    def get_dev_token(self, raw_token: str) -> str | None:
        if settings.environment == "development":
            return raw_token
        return None


# Default singleton used by AuthService
default_delivery_service = DevPasswordResetDeliveryService()
