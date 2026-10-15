"""Syllabus document and chunk ORM models for EduNova curriculum ingestion and retrieval."""

import uuid
from datetime import datetime, timezone
from typing import Optional, List
from sqlalchemy import String, Text, Integer, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


class SyllabusDocument(Base):
    __tablename__ = "syllabus_documents"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    source: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    board: Mapped[str] = mapped_column(String(50), nullable=False)
    class_grade: Mapped[str] = mapped_column(String(20), nullable=False)
    subject: Mapped[str] = mapped_column(String(100), nullable=False)
    language: Mapped[str] = mapped_column(String(20), default="en")
    chapter: Mapped[Optional[str]] = mapped_column(String(200), nullable=True)
    source_url: Mapped[Optional[str]] = mapped_column(String(1000), nullable=True)
    content_hash: Mapped[Optional[str]] = mapped_column(String(64), nullable=True)
    corpus_version: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    ingestion_status: Mapped[str] = mapped_column(String(20), default="pending")
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    chunks: Mapped[List["SyllabusChunk"]] = relationship(
        "SyllabusChunk", back_populates="document", cascade="all, delete-orphan"
    )


class SyllabusChunk(Base):
    __tablename__ = "syllabus_chunks"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    document_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("syllabus_documents.id", ondelete="CASCADE"), nullable=True
    )
    content: Mapped[str] = mapped_column(Text, nullable=False)
    chunk_index: Mapped[int] = mapped_column(Integer, default=0)
    board: Mapped[str] = mapped_column(String(50), nullable=False)
    class_grade: Mapped[str] = mapped_column(String(20), nullable=False)
    subject: Mapped[str] = mapped_column(String(100), nullable=False)
    chapter: Mapped[Optional[str]] = mapped_column(String(200), nullable=True)
    section: Mapped[Optional[str]] = mapped_column(String(200), nullable=True)
    language: Mapped[str] = mapped_column(String(20), default="en")
    textbook_title: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    page_reference: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    source_id: Mapped[Optional[str]] = mapped_column(String(36), nullable=True)
    corpus_version: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    content_hash: Mapped[Optional[str]] = mapped_column(String(64), nullable=True)
    embedding_model: Mapped[str] = mapped_column(String(100), default="intfloat/multilingual-e5-small")
    embedding_version: Mapped[str] = mapped_column(String(50), default="1.0")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    document: Mapped[Optional["SyllabusDocument"]] = relationship(
        "SyllabusDocument", back_populates="chunks"
    )
