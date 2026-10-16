"""Schemas for syllabus pack synchronization between Supabase and Android client."""

from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class SyllabusPackSummary(BaseModel):
    pack_id: str = Field(..., description="Unique pack identifier e.g. cbse-10-science")
    board: str
    class_grade: str
    subject: str
    academic_year: Optional[str] = "2026-27"
    language: str = "en"
    chunk_count: int = 0
    corpus_version: str = "1.0"
    embedding_model: str = "intfloat/multilingual-e5-small"
    embedding_version: str = "1.0"
    last_updated: Optional[datetime] = None


class SyllabusPackListResponse(BaseModel):
    packs: List[SyllabusPackSummary]
    total_count: int


class SyllabusManifestResponse(BaseModel):
    pack_id: str
    board: str
    class_grade: str
    subject: str
    academic_year: Optional[str] = "2026-27"
    language: str = "en"
    chunk_count: int
    corpus_version: str
    embedding_model: str = "intfloat/multilingual-e5-small"
    embedding_version: str = "1.0"
    embedding_dimension: int = 384
    content_hash: Optional[str] = None
    last_updated: Optional[datetime] = None


class SyllabusChunkDTO(BaseModel):
    chunk_id: str
    content: str
    board: str
    class_grade: str
    subject: str
    chapter: Optional[str] = None
    section: Optional[str] = None
    language: str = "en"
    textbook_title: Optional[str] = None
    page_reference: Optional[str] = None
    source_id: Optional[str] = None
    corpus_version: Optional[str] = None
    content_hash: Optional[str] = None
    embedding_model: str = "intfloat/multilingual-e5-small"
    embedding_version: str = "1.0"
    embedding: Optional[List[float]] = Field(
        None, description="Precomputed 384-dimensional float vector for on-device vector search"
    )


class SyllabusChunksResponse(BaseModel):
    pack_id: str
    chunks: List[SyllabusChunkDTO]
    next_cursor: Optional[str] = None
    has_more: bool = False
    total_chunks: int = 0
    embedding_model: str = "intfloat/multilingual-e5-small"
    embedding_version: str = "1.0"
    embedding_dimension: int = 384
