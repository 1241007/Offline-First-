import os
from pathlib import Path
from urllib.parse import quote_plus, urlsplit, urlunsplit
from pydantic_settings import BaseSettings, SettingsConfigDict

# Root .env is two directories above the backend/app/ package
_ROOT_ENV = Path(__file__).resolve().parent.parent.parent.parent / ".env"


class Settings(BaseSettings):
    environment: str = "development"
    database_url: str
    gemini_api_key: str
    gemini_model: str = "gemini-3.8-flash"
    chat_context_messages: int = 12
    gemini_max_output_tokens: int = 1024
    db_pool_size: int = 5
    db_max_overflow: int = 10
    allow_dev_user_id: bool = False  # DEVELOPMENT ONLY: Allow user_id query parameter

    model_config = SettingsConfigDict(
        env_file=str(_ROOT_ENV),
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @property
    def async_database_url(self) -> str:
        raw_url = self.database_url
        if raw_url.startswith("postgresql://") and "+asyncpg" not in raw_url:
            raw_url = raw_url.replace("postgresql://", "postgresql+asyncpg://", 1)

        # Robustly parse scheme, user, password, host, port, database, and query
        # Handles passwords with '@' character safely
        parsed = urlsplit(raw_url)
        netloc = parsed.netloc

        if "@" in netloc:
            # The host is after the last '@', credentials before it
            userpass, hostport = netloc.rsplit("@", 1)
            if ":" in userpass:
                user, password = userpass.split(":", 1)
                # URL-encode user and password to prevent parsing ambiguity
                encoded_userpass = f"{quote_plus(user)}:{quote_plus(password)}"
            else:
                encoded_userpass = quote_plus(userpass)
            netloc = f"{encoded_userpass}@{hostport}"

        return urlunsplit((parsed.scheme, netloc, parsed.path, parsed.query, parsed.fragment))


settings = Settings()
