"""Isolated rate-limit configuration.

Importing `limiter` from this module is the only thing auth routes need to do.
Switching from in-memory storage to Redis (or any other backend) only requires
changing this file — no auth route logic changes needed.
"""
from slowapi import Limiter
from slowapi.util import get_remote_address


def get_limiter(storage_uri: str | None = None, enabled: bool = True) -> Limiter:
    """
    Factory that creates a Limiter instance.

    Args:
        storage_uri: If None, uses the default in-memory store (development).
                     If a Redis URL is provided (e.g. "redis://localhost:6379"),
                     uses Redis-backed storage (production).
    """
    if storage_uri:
        return Limiter(key_func=get_remote_address, storage_uri=storage_uri, enabled=enabled)
    return Limiter(key_func=get_remote_address, enabled=enabled)


# Module-level singleton — routes import this directly.
# Reads REDIS_URL from settings if set, otherwise falls back to in-memory.
def _create_limiter() -> Limiter:
    from app.core.config import settings
    return get_limiter(storage_uri=settings.redis_url)


limiter = _create_limiter()
