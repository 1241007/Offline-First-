"""Supabase hybrid retrieval service with calibrated RRF scoring and evidence assessment."""

import asyncio
import logging
import time
from typing import Dict, List, Optional, Tuple, Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.core.config import settings
from app.schemas.rag import (
    AppMode,
    ChunkResult,
    CorpusScope,
    EvidenceOutcome,
    RAGRequestContext,
    RetrieveResponse,
)
from app.services.embedding_service import embedding_service, EmbeddingConfigurationError

logger = logging.getLogger(__name__)

# Constants for RRF and evidence assessment
RRF_K = 60
# Theoretical maximum for k=60 with 2 retrievers (vector + keyword): (1/61) * 2 = 2/61 ≈ 0.032786885
RRF_MAX_SCORE = (1.0 / (RRF_K + 1)) * 2

# Calibrated evidence thresholds against labeled retrieval fixtures
SUFFICIENT_NORMALIZED_THRESHOLD = 0.70
UNCERTAIN_NORMALIZED_THRESHOLD = 0.35
MIN_CONTENT_LENGTH_BYTES = 80


def compute_rrf_score(
    vector_rank: Optional[int],
    sparse_rank: Optional[int],
    k: int = RRF_K
) -> Tuple[float, float]:
    """
    Computes (raw_rrf, normalized_score) using conventional RRF formula.
    
    Formula:
        raw_rrf = sum(1 / (k + rank)) for each rank present
        normalized_score = raw_rrf / RRF_MAX_SCORE (bounded in [0.0, 1.0])
    """
    raw_rrf = 0.0
    if vector_rank is not None and vector_rank > 0:
        raw_rrf += 1.0 / (k + vector_rank)
    if sparse_rank is not None and sparse_rank > 0:
        raw_rrf += 1.0 / (k + sparse_rank)

    normalized = min(1.0, raw_rrf / RRF_MAX_SCORE)
    return raw_rrf, normalized


def assess_evidence_outcome(chunks: List[ChunkResult]) -> EvidenceOutcome:
    """
    Assesses whether the retrieved chunks provide SUFFICIENT, UNCERTAIN, or MISSING evidence.
    
    Uses calibrated thresholds validated against rrf_retrieval_fixtures.json.
    """
    if not chunks:
        return EvidenceOutcome.MISSING

    top_chunk = chunks[0]
    score = top_chunk.normalized_score if top_chunk.normalized_score is not None else 0.0
    content_len = len(top_chunk.content.strip())

    if score >= SUFFICIENT_NORMALIZED_THRESHOLD and content_len >= MIN_CONTENT_LENGTH_BYTES:
        return EvidenceOutcome.SUFFICIENT
    elif score >= UNCERTAIN_NORMALIZED_THRESHOLD:
        return EvidenceOutcome.UNCERTAIN
    else:
        return EvidenceOutcome.MISSING


class SupabaseRetrievalService:
    """Hybrid vector + full-text retrieval against Supabase / PostgreSQL."""

    def __init__(self, db: Optional[AsyncSession] = None):
        self.db = db

    async def retrieve(
        self,
        query: str,
        context: RAGRequestContext,
        top_k: int = 5
    ) -> RetrieveResponse:
        """
        Executes hybrid retrieval enforcing:
        - General Mode disabled with CORPUS_NOT_CONFIGURED
        - End-to-end deadline enforcement
        - Evidence assessment with calibrated thresholds
        - Zero silent fallthrough
        """
        start_time = time.perf_counter()

        # Mandatory Rule 8: General Mode RAG is disabled until a real corpus exists
        if context.app_mode == AppMode.GENERAL or context.corpus_scope == CorpusScope.GENERAL_DOCUMENTS:
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.CORPUS_NOT_CONFIGURED,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail="General Mode RAG is disabled because no document corpus is configured for general queries in this release."
            )

        # Enforce curriculum target to prevent silent cross-curriculum broadening
        if context.app_mode == AppMode.STUDENT or context.corpus_scope == CorpusScope.CURRICULUM:
            if not context.board or not context.class_grade:
                return RetrieveResponse(
                    chunks=[],
                    evidence_outcome=EvidenceOutcome.MISSING,
                    query_used=query,
                    search_time_ms=(time.perf_counter() - start_time) * 1000,
                    error_detail="Curriculum target (board and class grade) is required for syllabus retrieval to prevent cross-curriculum broadening."
                )

        # Check embedding service credentials
        if not embedding_service.is_configured:
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.RETRIEVAL_ERROR,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail="Embedding service is not configured with authorized credentials. Online retrieval requires EMBEDDING_API_KEY."
            )

        try:
            # Enforce overall retrieval deadline
            return await asyncio.wait_for(
                self._execute_hybrid_retrieval(query, context, top_k, start_time),
                timeout=settings.retrieval_timeout_seconds
            )
        except asyncio.TimeoutError:
            logger.error(f"Retrieval timed out after {settings.retrieval_timeout_seconds}s for query: {query}")
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.RETRIEVAL_ERROR,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail=f"Online retrieval deadline ({settings.retrieval_timeout_seconds}s) exceeded."
            )
        except Exception as exc:
            logger.error(f"Retrieval error: {exc}", exc_info=True)
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.RETRIEVAL_ERROR,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail=str(exc)
            )

    async def _execute_hybrid_retrieval(
        self,
        query: str,
        context: RAGRequestContext,
        top_k: int,
        start_time: float
    ) -> RetrieveResponse:
        """Internal retrieval worker querying embedding and database."""
        # 1. Generate query embedding with consistent provider
        query_embedding = await embedding_service.embed_query(query)

        if not self.db:
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.MISSING,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail="Database session unavailable for online retrieval."
            )

        # Check if syllabus_chunks table exists in DB
        table_check = await self.db.execute(
            text("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'syllabus_chunks');")
        )
        has_table = table_check.scalar()
        if not has_table:
            # Knowledge base has not been provisioned with authorized curriculum data
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.MISSING,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail="No authorized syllabus curriculum chunks table found in database."
            )

        # Build parameterized hybrid query with curriculum metadata filters
        sql = """
            WITH vector_matches AS (
                SELECT id, content, board, class_grade, subject, chapter, section,
                       textbook_title, page_reference, source_id, corpus_version,
                       ROW_NUMBER() OVER (ORDER BY embedding <=> CAST(:embedding AS vector)) AS rank
                FROM syllabus_chunks
                WHERE (:board IS NULL OR board = :board)
                  AND (:class_grade IS NULL OR class_grade = :class_grade)
                  AND (:subject IS NULL OR subject ILIKE :subject)
                LIMIT 20
            ),
            keyword_matches AS (
                SELECT id, content, board, class_grade, subject, chapter, section,
                       textbook_title, page_reference, source_id, corpus_version,
                       ROW_NUMBER() OVER (ORDER BY ts_rank_cd(to_tsvector('english', content), plainto_tsquery('english', :query)) DESC) AS rank
                FROM syllabus_chunks
                WHERE to_tsvector('english', content) @@ plainto_tsquery('english', :query)
                  AND (:board IS NULL OR board = :board)
                  AND (:class_grade IS NULL OR class_grade = :class_grade)
                  AND (:subject IS NULL OR subject ILIKE :subject)
                LIMIT 20
            )
            SELECT 
                COALESCE(v.id, k.id) as id,
                COALESCE(v.content, k.content) as content,
                COALESCE(v.board, k.board) as board,
                COALESCE(v.class_grade, k.class_grade) as class_grade,
                COALESCE(v.subject, k.subject) as subject,
                COALESCE(v.chapter, k.chapter) as chapter,
                COALESCE(v.section, k.section) as section,
                COALESCE(v.textbook_title, k.textbook_title) as textbook_title,
                COALESCE(v.page_reference, k.page_reference) as page_reference,
                COALESCE(v.source_id, k.source_id) as source_id,
                COALESCE(v.corpus_version, k.corpus_version) as corpus_version,
                v.rank as vector_rank,
                k.rank as keyword_rank
            FROM vector_matches v
            FULL OUTER JOIN keyword_matches k ON v.id = k.id;
        """

        params = {
            "embedding": str(query_embedding),
            "query": query,
            "board": context.board,
            "class_grade": context.class_grade,
            "subject": f"%{context.subject}%" if context.subject else None,
        }

        result = await self.db.execute(text(sql), params)
        rows = result.fetchall()

        if not rows:
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.MISSING,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
            )

        scored_chunks: List[ChunkResult] = []
        for row in rows:
            v_rank = row.vector_rank
            k_rank = row.keyword_rank
            raw_rrf, normalized = compute_rrf_score(v_rank, k_rank)

            scored_chunks.append(
                ChunkResult(
                    chunk_id=str(row.id),
                    content=row.content,
                    board=row.board,
                    class_grade=row.class_grade,
                    subject=row.subject,
                    chapter=row.chapter,
                    section=row.section,
                    textbook_title=row.textbook_title,
                    page_reference=row.page_reference,
                    source_id=row.source_id,
                    corpus_version=row.corpus_version,
                    relevance_score=normalized,
                    raw_rrf_score=raw_rrf,
                    normalized_score=normalized,
                )
            )

        scored_chunks.sort(key=lambda c: c.normalized_score or 0.0, reverse=True)
        top_chunks = scored_chunks[:top_k]
        outcome = assess_evidence_outcome(top_chunks)

        return RetrieveResponse(
            chunks=top_chunks,
            evidence_outcome=outcome,
            query_used=query,
            search_time_ms=(time.perf_counter() - start_time) * 1000,
        )
