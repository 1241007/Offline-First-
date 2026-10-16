import asyncio
import logging
import re
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

# Calibrated evidence thresholds against labeled retrieval fixtures and empirical evaluation
SUFFICIENT_NORMALIZED_THRESHOLD = 0.70
DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD = 0.50
DENSE_ONLY_MIN_SIMILARITY = 0.75
UNCERTAIN_NORMALIZED_THRESHOLD = 0.35
MIN_CONTENT_LENGTH_BYTES = 80
DENSE_MAX_COSINE_DISTANCE = 0.35

# Stopwords for lexical corroboration of single-retriever matches
CORROBORATION_STOPWORDS = {
    "a", "an", "the", "in", "on", "of", "for", "with", "at", "by", "from",
    "up", "about", "into", "over", "after", "is", "are", "was", "were", "be",
    "been", "being", "have", "has", "had", "do", "does", "did", "and", "but",
    "if", "or", "because", "as", "what", "which", "who", "whom", "this",
    "that", "these", "those", "am", "it", "its", "you", "your", "he", "she",
    "they", "their", "we", "us", "our", "how", "why", "when", "where", "can",
    "could", "should", "would", "will", "give", "explain", "state", "calculate",
    "write", "find", "formula", "example", "examples", "definition", "define",
    "between", "using", "points", "point", "textbook", "answer", "question",
    "diagram", "determine", "identify", "describe", "discuss"
}


def check_lexical_corroboration(query: Optional[str], content: str) -> bool:
    """
    Verifies lexical and domain corroboration between the query and retrieved content.
    
    Prevents dense-vector nearest-neighbor distractors (such as out-of-syllabus college/research
    topics that cluster close to high-school science in vector space) from falsely becoming SUFFICIENT.

    Rules:
    - Extracts domain terms (>= 3 chars) excluding general stopwords.
    - If 1 or 2 domain terms exist: requires at least 1 match.
    - If >= 3 domain terms exist: requires at least 2 distinct matches OR >= 30% overlap.
    """
    if not query or not content:
        return False
    words = re.findall(r"\b[a-zA-Z]{3,}\b", query.lower())
    content_lower = content.lower()
    domain_words = [w for w in words if w not in CORROBORATION_STOPWORDS]
    if not domain_words:
        return True  # If query has no domain words, cannot disqualify

    matches = [w for w in domain_words if w in content_lower or (len(w) >= 6 and w[:5] in content_lower)]

    if len(domain_words) <= 2:
        return len(matches) >= 1
    return len(matches) >= 2 or (len(matches) / len(domain_words)) >= 0.30


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


def assess_evidence_outcome(
    chunks: List[ChunkResult],
    query: Optional[str] = None
) -> EvidenceOutcome:
    """
    Assesses whether the retrieved chunks provide SUFFICIENT, UNCERTAIN, or MISSING evidence.
    
    Evidence Assessment Policy:
    1. Content length guard:
       Top chunk must have at least MIN_CONTENT_LENGTH_BYTES (80 bytes) of trimmed text.
       Shorter content is downgraded to UNCERTAIN (if normalized score >= 0.35) or MISSING.
    2. Hybrid match (both dense and keyword support):
       Requires normalized RRF score >= SUFFICIENT_NORMALIZED_THRESHOLD (0.70).
    3. Dense-only match:
       When only dense vector retrieval matches (no keyword support):
       - Requires normalized score >= DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD (0.50).
       - AND cosine similarity >= DENSE_ONLY_MIN_SIMILARITY (0.75).
       - If query is provided, requires lexical/domain corroboration (at least one domain term in content)
         to prevent nearest-neighbor distractors (e.g. quantum entanglement, CRISPR) from becoming SUFFICIENT.
       - Dense-only matches lacking corroboration are downgraded to MISSING to prevent ungrounded generation.
    4. Keyword-only match:
       Capped at 0.5000 in 2-retriever RRF; cannot reach SUFFICIENT (0.70); evaluated against UNCERTAIN.
    5. Fallthrough:
       Top chunk with normalized RRF score >= UNCERTAIN_NORMALIZED_THRESHOLD (0.35) -> UNCERTAIN.
       Otherwise -> MISSING.
    """
    if not chunks:
        return EvidenceOutcome.MISSING

    top_chunk = chunks[0]
    score = top_chunk.normalized_score if top_chunk.normalized_score is not None else 0.0
    content_len = len(top_chunk.content.strip())

    if content_len < MIN_CONTENT_LENGTH_BYTES:
        if score >= UNCERTAIN_NORMALIZED_THRESHOLD:
            return EvidenceOutcome.UNCERTAIN
        return EvidenceOutcome.MISSING

    has_dense = top_chunk.vector_rank is not None and top_chunk.vector_rank > 0
    has_keyword = top_chunk.sparse_rank is not None and top_chunk.sparse_rank > 0
    sim_score = top_chunk.similarity_score

    # Case 1: Dual retriever support (or mathematically proven dual score >= 0.70)
    if score >= SUFFICIENT_NORMALIZED_THRESHOLD:
        return EvidenceOutcome.SUFFICIENT

    # Case 2: Dense-only match requiring dual criteria (score >= 0.50 AND similarity >= 0.75)
    if (has_dense or top_chunk.vector_rank is None) and not has_keyword:
        if (
            score >= DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD
            and sim_score is not None
            and sim_score >= DENSE_ONLY_MIN_SIMILARITY
        ):
            # If query is provided, enforce domain corroboration to prevent out-of-syllabus distractors
            if query is not None:
                if check_lexical_corroboration(query, top_chunk.content):
                    return EvidenceOutcome.SUFFICIENT
                else:
                    return EvidenceOutcome.MISSING
            return EvidenceOutcome.SUFFICIENT

    # Case 3: Uncertain evidence
    if score >= UNCERTAIN_NORMALIZED_THRESHOLD:
        return EvidenceOutcome.UNCERTAIN

    return EvidenceOutcome.MISSING


_TABLE_EXISTS_CACHE: bool = False


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
            if self.db:
                try:
                    await self.db.rollback()
                except Exception:
                    pass
            logger.error(f"Retrieval timed out after {settings.retrieval_timeout_seconds}s for query: {query}")
            return RetrieveResponse(
                chunks=[],
                evidence_outcome=EvidenceOutcome.RETRIEVAL_ERROR,
                query_used=query,
                search_time_ms=(time.perf_counter() - start_time) * 1000,
                error_detail=f"Online retrieval deadline ({settings.retrieval_timeout_seconds}s) exceeded."
            )
        except Exception as exc:
            if self.db:
                try:
                    await self.db.rollback()
                except Exception:
                    pass
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

        # Check if syllabus_chunks table exists in DB (cached to avoid redundant catalog scans)
        global _TABLE_EXISTS_CACHE
        if not _TABLE_EXISTS_CACHE:
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
            _TABLE_EXISTS_CACHE = True

        # Build parameterized hybrid query strictly gated to authorized curriculum documents (cbseacademic.nic.in)
        # Prevents unverified legacy NCERT textbook documents from being served in online RAG
        sql = """
            WITH vector_matches AS (
                SELECT sc.id, sc.content, sc.board, sc.class_grade, sc.subject, sc.chapter, sc.section,
                       sc.textbook_title, sc.page_reference, sc.source_id, sc.corpus_version,
                       (1.0 - (sc.embedding <=> CAST(:embedding AS vector))) AS similarity_score,
                       ROW_NUMBER() OVER (ORDER BY sc.embedding <=> CAST(:embedding AS vector)) AS rank
                FROM syllabus_chunks sc
                JOIN syllabus_documents sd ON sc.document_id = sd.id
                WHERE (sc.embedding <=> CAST(:embedding AS vector)) < :max_cosine_distance
                  AND sd.source_url LIKE '%cbseacademic.nic.in%'
                  AND (CAST(:board AS text) IS NULL OR sc.board = CAST(:board AS varchar))
                  AND (CAST(:class_grade AS text) IS NULL OR sc.class_grade = CAST(:class_grade AS varchar))
                  AND (CAST(:subject AS text) IS NULL OR sc.subject ILIKE CAST(:subject AS varchar))
                LIMIT 20
            ),
            keyword_matches AS (
                SELECT sc.id, sc.content, sc.board, sc.class_grade, sc.subject, sc.chapter, sc.section,
                       sc.textbook_title, sc.page_reference, sc.source_id, sc.corpus_version,
                       ROW_NUMBER() OVER (ORDER BY ts_rank_cd(to_tsvector('english', sc.content), plainto_tsquery('english', :query)) DESC) AS rank
                FROM syllabus_chunks sc
                JOIN syllabus_documents sd ON sc.document_id = sd.id
                WHERE to_tsvector('english', sc.content) @@ plainto_tsquery('english', :query)
                  AND sd.source_url LIKE '%cbseacademic.nic.in%'
                  AND (CAST(:board AS text) IS NULL OR sc.board = CAST(:board AS varchar))
                  AND (CAST(:class_grade AS text) IS NULL OR sc.class_grade = CAST(:class_grade AS varchar))
                  AND (CAST(:subject AS text) IS NULL OR sc.subject ILIKE CAST(:subject AS varchar))
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
                k.rank as keyword_rank,
                v.similarity_score as similarity_score
            FROM vector_matches v
            FULL OUTER JOIN keyword_matches k ON v.id = k.id;
        """

        params = {
            "embedding": str(query_embedding),
            "query": query,
            "max_cosine_distance": DENSE_MAX_COSINE_DISTANCE,
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
                    similarity_score=float(row.similarity_score) if row.similarity_score is not None else None,
                    vector_rank=row.vector_rank,
                    sparse_rank=row.keyword_rank,
                )
            )

        scored_chunks.sort(key=lambda c: c.normalized_score or 0.0, reverse=True)
        top_chunks = scored_chunks[:top_k]
        outcome = assess_evidence_outcome(top_chunks, query=query)

        return RetrieveResponse(
            chunks=top_chunks,
            evidence_outcome=outcome,
            query_used=query,
            search_time_ms=(time.perf_counter() - start_time) * 1000,
        )
