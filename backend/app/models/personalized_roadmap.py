"""Personalized Roadmap and Assessment domain models"""

import uuid
from datetime import datetime, timezone
from typing import Optional, List, Dict, Any
from sqlalchemy import String, Text, Integer, Boolean, DateTime, ForeignKey, Index, JSON
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class AssessmentSessionModel(Base):
    __tablename__ = "assessment_sessions"
    __table_args__ = (
        Index("ix_assessment_sessions_domain", "domain"),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    domain: Mapped[str] = mapped_column(String(100), nullable=False, default="Python")
    phase: Mapped[str] = mapped_column(String(50), nullable=False, default="profile_collection")
    profile_goal: Mapped[str] = mapped_column(String(255), nullable=False, default="")
    profile_level: Mapped[str] = mapped_column(String(100), nullable=False, default="")
    profile_study_hours_per_week: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    profile_timeline_weeks: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    
    question_number: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    total_questions: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    
    # Store questions history, current question, and answers as JSON
    current_question_data: Mapped[Optional[Dict[str, Any]]] = mapped_column(JSON, nullable=True)
    questions_history: Mapped[List[Dict[str, Any]]] = mapped_column(JSON, nullable=False, default=list)
    mastery_map: Mapped[Dict[str, Any]] = mapped_column(JSON, nullable=False, default=dict)
    
    is_complete: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    generated_roadmap_id: Mapped[Optional[str]] = mapped_column(String(36), nullable=True)
    last_answer_correct: Mapped[Optional[bool]] = mapped_column(Boolean, nullable=True)
    last_answer_explanation: Mapped[str] = mapped_column(Text, nullable=False, default="")
    correction_prompt: Mapped[Optional[Dict[str, Any]]] = mapped_column(JSON, nullable=True)

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


class PersonalizedRoadmapModel(Base):
    __tablename__ = "personalized_roadmaps"
    __table_args__ = (
        Index("ix_personalized_roadmaps_user_id", "user_id"),
        Index("ix_personalized_roadmaps_domain", "domain"),
    )

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    domain: Mapped[str] = mapped_column(String(100), nullable=False, default="Python")
    goal: Mapped[str] = mapped_column(String(255), nullable=False, default="")
    level: Mapped[str] = mapped_column(String(100), nullable=False, default="Beginner")
    study_hours_per_week: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    estimated_weeks: Mapped[int] = mapped_column(Integer, nullable=False, default=8)
    
    topics: Mapped[List[Dict[str, Any]]] = mapped_column(JSON, nullable=False, default=list)
    edges: Mapped[List[Dict[str, Any]]] = mapped_column(JSON, nullable=False, default=list)
    assessment_session_id: Mapped[str] = mapped_column(String(36), nullable=False, default="")
    
    completed_topic_ids: Mapped[List[str]] = mapped_column(JSON, nullable=False, default=list)
    in_progress_topic_ids: Mapped[List[str]] = mapped_column(JSON, nullable=False, default=list)

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
