import uuid
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy import String, Text, Float, Boolean, DateTime, Index
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class UserMemory(Base):
    __tablename__ = "user_memories"
    __table_args__ = (
        Index("ix_user_memories_user_id", "user_id"),
        Index("ix_user_memories_user_active", "user_id", "active"),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(String(36), nullable=False)
    category: Mapped[str] = mapped_column(String(50), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    importance: Mapped[float] = mapped_column(Float, default=1.0, nullable=False)
    confidence: Mapped[float] = mapped_column(Float, default=1.0, nullable=False)
    source_conversation_id: Mapped[Optional[str]] = mapped_column(String(36), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc)
    )
    last_used_at: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True),
        nullable=True
    )
    active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
