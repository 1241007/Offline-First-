"""Roadmap domain models"""

import uuid
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy import String, Text, Integer, Boolean, DateTime, ForeignKey, CheckConstraint, Index, JSON
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import JSONB
from app.core.database import Base


class Roadmap(Base):
    __tablename__ = "roadmaps"
    __table_args__ = (
        Index("ix_roadmaps_user_id", "user_id", postgresql_where="user_id IS NOT NULL"),
        Index("ix_roadmaps_system_category", "is_system", "category"),
        CheckConstraint(
            "level IN ('Beginner', 'Intermediate', 'Advanced', 'All Levels')",
            name="ck_roadmaps_level"
        ),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    slug: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
    category: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    level: Mapped[str] = mapped_column(String(50), nullable=False)
    duration: Mapped[str] = mapped_column(String(100), nullable=False)
    icon: Mapped[str] = mapped_column(String(100), nullable=False)
    accent_theme: Mapped[str] = mapped_column(String(50), nullable=False, default="primary")
    is_system: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    user_id: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False
    )

    # Relationships
    items: Mapped[list["RoadmapItem"]] = relationship(
        "RoadmapItem",
        back_populates="roadmap",
        cascade="all, delete-orphan",
        order_by="RoadmapItem.display_order"
    )


class RoadmapItem(Base):
    __tablename__ = "roadmap_items"
    __table_args__ = (
        Index("ix_roadmap_items_roadmap_display", "roadmap_id", "display_order"),
        Index("ix_roadmap_items_course_id", "course_id", postgresql_where="course_id IS NOT NULL"),
        CheckConstraint("display_order >= 0", name="ck_roadmap_items_display_order"),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    roadmap_id: Mapped[str] = mapped_column(
        ForeignKey("roadmaps.id", ondelete="CASCADE"),
        nullable=False
    )
    course_id: Mapped[Optional[str]] = mapped_column(
        ForeignKey("courses.id", ondelete="SET NULL"),
        nullable=True
    )
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    skills: Mapped[Optional[dict]] = mapped_column(JSONB().with_variant(JSON, "sqlite"), nullable=True)
    duration: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False
    )

    # Relationships
    roadmap: Mapped["Roadmap"] = relationship(
        "Roadmap",
        back_populates="items"
    )
    course: Mapped[Optional["Course"]] = relationship("Course")

