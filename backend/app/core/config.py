import os
from pathlib import Path
from urllib.parse import quote_plus, urlsplit, urlunsplit
from typing import Optional
from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

# Root .env is three directories above backend/app/core/config.py (i.e. E:\Offline First\.env)
_ROOT_ENV = Path(__file__).resolve().parents[3] / ".env"
_BACKEND_ENV = Path(__file__).resolve().parents[2] / ".env"

_DEV_PLACEHOLDER = "edunova-dev-secret-key-32bytes-long-change-in-production!"


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
    jwt_secret_key: str  # No default — must be set via environment
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    refresh_token_expire_days: int = 30
    reset_token_expire_minutes: int = 15
    redis_url: Optional[str] = None  # Optional: Redis for distributed rate limiting
    cors_origins: list[str] = [
        "http://10.0.2.2:8000",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
    ]

    model_config = SettingsConfigDict(
        env_file=(str(_ROOT_ENV), str(_BACKEND_ENV)),
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @model_validator(mode="after")
    def validate_secrets(self) -> "Settings":
        if self.environment != "development" and self.jwt_secret_key == _DEV_PLACEHOLDER:
            raise ValueError(
                "JWT_SECRET_KEY must be changed from the development placeholder "
                "before running in production"
            )
        return self

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
