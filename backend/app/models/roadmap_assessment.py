"""Roadmap Assessment Session domain model"""

import uuid
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy import String, Float, DateTime, ForeignKey, Index, JSON
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import JSONB
from app.core.database import Base


class RoadmapAssessmentSession(Base):
    __tablename__ = "roadmap_assessment_sessions"
    __table_args__ = (
        Index("ix_roadmap_assessments_user_id", "user_id"),
        Index("ix_roadmap_assessments_user_state", "user_id", "state"),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False
    )
    state: Mapped[str] = mapped_column(
        String(50), nullable=False, default="INITIALIZING"
    )
    goal: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    target_level: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    target_timeline: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    weekly_hours: Mapped[Optional[float]] = mapped_column(Float, nullable=True)

    # Stores gathered profile (verified evidence, self-reported skills, skill gaps, quiz history, unknowns)
    collected_profile: Mapped[Optional[dict]] = mapped_column(
        JSONB().with_variant(JSON, "sqlite"), nullable=True
    )

    # Stores chat turns (mentor messages, learner answers, quiz representations without answers revealed)
    messages: Mapped[Optional[list]] = mapped_column(
        JSONB().with_variant(JSON, "sqlite"), nullable=True
    )

    # Stores active diagnostic quiz securely on server (question, options, correct_index, explanation, skill_tested)
    # The client NEVER receives correct_index or explanation before submitting!
    pending_quiz: Mapped[Optional[dict]] = mapped_column(
        JSONB().with_variant(JSON, "sqlite"), nullable=True
    )

    roadmap_id: Mapped[Optional[str]] = mapped_column(
        ForeignKey("roadmaps.id", ondelete="SET NULL"),
        nullable=True
    )

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
    roadmap: Mapped[Optional["Roadmap"]] = relationship("Roadmap")
