from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase
from typing import AsyncGenerator
from .config import settings
import logging

logger = logging.getLogger(__name__)

class Base(DeclarativeBase):
    pass

_engine = None
_AsyncSessionLocal = None

def get_engine():
    global _engine
    if _engine is None:
        _engine = create_async_engine(
            settings.async_database_url,
            pool_pre_ping=True,
            pool_size=settings.db_pool_size,
            max_overflow=settings.db_max_overflow,
        )
    return _engine

def get_session_factory():
    global _AsyncSessionLocal
    if _AsyncSessionLocal is None:
        _AsyncSessionLocal = async_sessionmaker(get_engine(), expire_on_commit=False)
    return _AsyncSessionLocal

async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with get_session_factory()() as session:
        yield session
