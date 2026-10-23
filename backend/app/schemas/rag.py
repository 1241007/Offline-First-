"""RAG schemas for EduNova."""

from datetime import datetime
from enum import Enum
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class RouteDecision(str, Enum):
    DIRECT = "DIRECT"
    RAG = "RAG"


class CorpusScope(str, Enum):
    CURRICULUM = "curriculum"
    GENERAL_DOCUMENTS = "general_documents"


class EvidenceOutcome(str, Enum):
    SUFFICIENT = "SUFFICIENT"
    UNCERTAIN = "UNCERTAIN"
    MISSING = "MISSING"
    RETRIEVAL_ERROR = "RETRIEVAL_ERROR"
    CORPUS_NOT_CONFIGURED = "CORPUS_NOT_CONFIGURED"


class AppMode(str, Enum):
    STUDENT = "student"
    GENERAL = "general"


class RAGRequestContext(BaseModel):
    """Carries all routing and retrieval context through the pipeline."""
    execution_mode: str = "ONLINE"  # "ONLINE" or "OFFLINE"
    app_mode: AppMode = AppMode.GENERAL
    board: Optional[str] = None
    class_grade: Optional[str] = None
    subject: Optional[str] = None
    language: Optional[str] = "en"
    explanation_mode: str = "general"
    conversation_history: List[Dict[str, Any]] = Field(default_factory=list)
    standalone_query: Optional[str] = None
    selected_chapter: Optional[str] = None
    corpus_scope: CorpusScope = CorpusScope.CURRICULUM
    corpus_version: Optional[str] = None
    domain_classification: Optional[Dict[str, Any]] = None


class RetrieveRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)
    context: RAGRequestContext = Field(default_factory=RAGRequestContext)
    top_k: int = Field(default=5, ge=1, le=20)


class ChunkResult(BaseModel):
    chunk_id: str
    content: str
    board: Optional[str] = None
    class_grade: Optional[str] = None
    subject: Optional[str] = None
    chapter: Optional[str] = None
    section: Optional[str] = None
    textbook_title: Optional[str] = None
    page_reference: Optional[str] = None
    source_id: Optional[str] = None
    corpus_version: Optional[str] = None
    relevance_score: Optional[float] = None
    raw_rrf_score: Optional[float] = None
    normalized_score: Optional[float] = None
    similarity_score: Optional[float] = None
    vector_rank: Optional[int] = None
    sparse_rank: Optional[int] = None


class RetrieveResponse(BaseModel):
    chunks: List[ChunkResult] = Field(default_factory=list)
    evidence_outcome: EvidenceOutcome = EvidenceOutcome.MISSING
    query_used: str = ""
    search_time_ms: Optional[float] = None
    error_detail: Optional[str] = None


class RetrievalMissRecord(BaseModel):
    query: str
    board: Optional[str] = None
    class_grade: Optional[str] = None
    subject: Optional[str] = None
    corpus_scope: str = "curriculum"
    corpus_version: Optional[str] = None
    search_backend: str = "unknown"
    top_score: Optional[float] = None
    timestamp: datetime = Field(default_factory=lambda: datetime.now())
