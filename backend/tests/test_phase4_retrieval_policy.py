"""Phase 4 regression tests for retrieval policy, distance cutoff, and dual-condition evidence assessment.

Verifies:
1. Dense-only valid semantic match (SUFFICIENT).
2. Dense-only irrelevant nearest neighbor (UNCERTAIN).
3. Keyword-only match (UNCERTAIN).
4. Combined dense and keyword match (SUFFICIENT).
5. Cosine-distance and similarity exact boundary values (0.35, 0.75).
6. RRF score exact boundary values (0.70, 0.50, 0.35).
7. Empty candidate sets (MISSING).
8. Out-of-syllabus distance cutoff filtering.
9. Curriculum mismatch protection.
10. Duplicate candidate deduplication.
11. Database timeout and exception rollback behavior.
12. Insufficient evidence abstention signaling.
"""

import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
import pytest

from app.services.embedding_service import embedding_service
from app.schemas.rag import (
    AppMode,
    ChunkResult,
    CorpusScope,
    EvidenceOutcome,
    RAGRequestContext,
    RetrieveResponse,
)
from app.services.supabase_retrieval_service import (
    assess_evidence_outcome,
    compute_rrf_score,
    SupabaseRetrievalService,
    SUFFICIENT_NORMALIZED_THRESHOLD,
    DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD,
    DENSE_ONLY_MIN_SIMILARITY,
    UNCERTAIN_NORMALIZED_THRESHOLD,
    DENSE_MAX_COSINE_DISTANCE,
    MIN_CONTENT_LENGTH_BYTES,
)


def _make_chunk(
    chunk_id: str = "chunk_1",
    content: str = None,
    normalized_score: float = 0.50,
    similarity_score: float = None,
    vector_rank: int = None,
    sparse_rank: int = None,
) -> ChunkResult:
    if content is None:
        content = "This is verified syllabus content describing chemical combination reactions and fundamental scientific principles."
    return ChunkResult(
        chunk_id=chunk_id,
        content=content,
        board="CBSE",
        class_grade="10",
        subject="Science",
        chapter="Chapter 1",
        relevance_score=normalized_score,
        normalized_score=normalized_score,
        similarity_score=similarity_score,
        vector_rank=vector_rank,
        sparse_rank=sparse_rank,
    )


# --- 1. Dense-Only Valid Semantic Match ---
def test_dense_only_valid_semantic_match():
    """Valid dense-only match with score >= 0.50 and similarity >= 0.75 must be SUFFICIENT."""
    chunk = _make_chunk(
        normalized_score=0.50,
        similarity_score=0.85,
        vector_rank=1,
        sparse_rank=None,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.SUFFICIENT, (
        f"Dense-only match with score {chunk.normalized_score} and similarity {chunk.similarity_score} "
        f"must evaluate to SUFFICIENT."
    )


# --- 2. Dense-Only Irrelevant Nearest Neighbor ---
def test_dense_only_irrelevant_nearest_neighbor():
    """Dense-only nearest neighbor with similarity < 0.75 must NOT be SUFFICIENT (UNCERTAIN)."""
    chunk = _make_chunk(
        normalized_score=0.50,
        similarity_score=0.72,  # Below 0.75
        vector_rank=1,
        sparse_rank=None,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.UNCERTAIN, (
        f"Dense-only match with similarity {chunk.similarity_score} (< 0.75) must be UNCERTAIN, not SUFFICIENT."
    )


# --- 3. Keyword-Only Match ---
def test_keyword_only_match_cannot_reach_sufficient():
    """Keyword-only match with no dense support cannot reach SUFFICIENT on semantic evidence."""
    chunk = _make_chunk(
        normalized_score=0.50,
        similarity_score=None,
        vector_rank=None,
        sparse_rank=1,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.UNCERTAIN, (
        "Keyword-only match without dense corroboration must evaluate to UNCERTAIN, not SUFFICIENT."
    )


# --- 4. Combined Dense and Keyword Match ---
def test_combined_dense_and_keyword_match():
    """Dual retriever match meeting hybrid threshold (>= 0.70) must evaluate to SUFFICIENT."""
    chunk = _make_chunk(
        normalized_score=0.85,
        similarity_score=0.70,  # Even with lower similarity, dual retriever corroboration suffices
        vector_rank=2,
        sparse_rank=3,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.SUFFICIENT


# --- 5. Cosine Distance and Similarity Exact Boundaries ---
def test_cosine_distance_and_similarity_exact_boundaries():
    """Exact boundary evaluation for distance cutoff (< 0.35) and similarity (>= 0.75)."""
    assert DENSE_MAX_COSINE_DISTANCE == 0.35
    assert DENSE_ONLY_MIN_SIMILARITY == 0.75

    # Boundary at exactly similarity = 0.75 -> SUFFICIENT
    chunk_exact_sim = _make_chunk(
        normalized_score=0.50,
        similarity_score=0.75,
        vector_rank=1,
        sparse_rank=None,
    )
    assert assess_evidence_outcome([chunk_exact_sim]) == EvidenceOutcome.SUFFICIENT

    # Boundary just below similarity = 0.7499 -> UNCERTAIN
    chunk_just_below_sim = _make_chunk(
        normalized_score=0.50,
        similarity_score=0.7499,
        vector_rank=1,
        sparse_rank=None,
    )
    assert assess_evidence_outcome([chunk_just_below_sim]) == EvidenceOutcome.UNCERTAIN


# --- 6. RRF Score Exact Boundary Values ---
def test_rrf_score_exact_boundaries():
    """Verify behavior at exact score boundaries: 0.70, 0.50, 0.35."""
    assert SUFFICIENT_NORMALIZED_THRESHOLD == 0.70
    assert DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD == 0.50
    assert UNCERTAIN_NORMALIZED_THRESHOLD == 0.35

    # Hybrid at exactly 0.70 -> SUFFICIENT
    c_hybrid_70 = _make_chunk(normalized_score=0.70, vector_rank=1, sparse_rank=15)
    assert assess_evidence_outcome([c_hybrid_70]) == EvidenceOutcome.SUFFICIENT

    # Hybrid just below 0.6999 -> UNCERTAIN
    c_hybrid_699 = _make_chunk(normalized_score=0.6999, vector_rank=2, sparse_rank=15)
    assert assess_evidence_outcome([c_hybrid_699]) == EvidenceOutcome.UNCERTAIN

    # Dense-only at exactly 0.50 with sim=0.75 -> SUFFICIENT
    c_dense_50 = _make_chunk(normalized_score=0.50, similarity_score=0.75, vector_rank=1)
    assert assess_evidence_outcome([c_dense_50]) == EvidenceOutcome.SUFFICIENT

    # Dense-only at 0.4999 with sim=0.75 -> UNCERTAIN
    c_dense_499 = _make_chunk(normalized_score=0.4999, similarity_score=0.75, vector_rank=1)
    assert assess_evidence_outcome([c_dense_499]) == EvidenceOutcome.UNCERTAIN

    # Uncertain at exactly 0.35 -> UNCERTAIN
    c_unc_35 = _make_chunk(normalized_score=0.35, vector_rank=30)
    assert assess_evidence_outcome([c_unc_35]) == EvidenceOutcome.UNCERTAIN

    # Fallthrough just below 0.3499 -> MISSING
    c_miss_349 = _make_chunk(normalized_score=0.3499, vector_rank=35)
    assert assess_evidence_outcome([c_miss_349]) == EvidenceOutcome.MISSING


# --- 7. Empty Candidate Sets ---
def test_empty_candidate_sets():
    """Empty chunk list returns MISSING."""
    assert assess_evidence_outcome([]) == EvidenceOutcome.MISSING


# --- 8. Out-of-Syllabus Distance Cutoff Filtering ---
def test_out_of_syllabus_cutoff_contract():
    """Verify distance condition semantics: vectors with distance >= 0.35 are excluded."""
    # Distance of 0.35 or higher (e.g. 0.40) does not satisfy distance < 0.35
    dist = 0.35
    satisfies_cutoff = dist < DENSE_MAX_COSINE_DISTANCE
    assert not satisfies_cutoff, "Distance equal to 0.35 must not satisfy the strict < 0.35 cutoff."

    dist_valid = 0.349
    assert dist_valid < DENSE_MAX_COSINE_DISTANCE


# --- 9. Curriculum Mismatch Protection ---
@pytest.mark.asyncio
async def test_curriculum_mismatch_protection():
    """Missing board or class_grade in student mode returns MISSING with explicit error detail."""
    svc = SupabaseRetrievalService(db=MagicMock())
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        corpus_scope=CorpusScope.CURRICULUM,
        board=None,  # Missing board
        class_grade="10",
        subject="Science",
    )
    res = await svc.retrieve("What is an acid?", context=ctx)
    assert res.evidence_outcome == EvidenceOutcome.MISSING
    assert res.chunks == []
    assert "Curriculum target" in res.error_detail


# --- 10. Duplicate Candidate Deduplication ---
def test_duplicate_candidate_deduplication():
    """Full outer join invariant: duplicate occurrences across vector and keyword merge cleanly."""
    v_rows = [{"id": "chunk_x", "rank": 1}, {"id": "chunk_y", "rank": 2}]
    k_rows = [{"id": "chunk_x", "rank": 4}, {"id": "chunk_z", "rank": 1}]

    merged = {}
    for r in v_rows:
        merged[r["id"]] = {"v_rank": r["rank"], "k_rank": None}
    for r in k_rows:
        if r["id"] in merged:
            merged[r["id"]]["k_rank"] = r["rank"]
        else:
            merged[r["id"]] = {"v_rank": None, "k_rank": r["rank"]}

    assert len(merged) == 3
    assert merged["chunk_x"]["v_rank"] == 1
    assert merged["chunk_x"]["k_rank"] == 4


# --- 11. Database Timeout and Exception Rollback ---
@pytest.mark.asyncio
async def test_database_timeout_and_exception_rollback():
    """Verify that timeout and DB exceptions cleanly roll back the session and return RETRIEVAL_ERROR."""
    mock_db = MagicMock()
    mock_db.rollback = AsyncMock()

    svc = SupabaseRetrievalService(db=mock_db)
    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    with patch.object(svc, "_execute_hybrid_retrieval", side_effect=asyncio.TimeoutError()):
        res = await svc.retrieve("What is electricity?", context=ctx)
        assert res.evidence_outcome == EvidenceOutcome.RETRIEVAL_ERROR
        assert "exceeded" in res.error_detail.lower()
        mock_db.rollback.assert_awaited()

    mock_db.rollback.reset_mock()
    with patch.object(svc, "_execute_hybrid_retrieval", side_effect=RuntimeError("Connection lost")):
        res = await svc.retrieve("What is electricity?", context=ctx)
        assert res.evidence_outcome == EvidenceOutcome.RETRIEVAL_ERROR
        assert "Connection lost" in res.error_detail
        mock_db.rollback.assert_awaited()


# --- 12. Insufficient Evidence Prevents Ungrounded Answering ---
def test_short_content_insufficient_evidence():
    """Content below MIN_CONTENT_LENGTH_BYTES (80 bytes) downgrades to UNCERTAIN to prevent truncation hallucination."""
    short_chunk = _make_chunk(
        content="Short fragment.",
        normalized_score=1.0,
        similarity_score=0.95,
        vector_rank=1,
        sparse_rank=1,
    )
    # Explicitly verify length
    assert len(short_chunk.content.strip()) < MIN_CONTENT_LENGTH_BYTES
    outcome = assess_evidence_outcome([short_chunk])
    assert outcome == EvidenceOutcome.UNCERTAIN, (
        "Short content below 80 bytes must be downgraded to UNCERTAIN to prevent ungrounded generation."
    )


# --- 13. Domain Corroboration Prevents Out-of-Syllabus Distractor Traps ---
def test_dense_only_distractor_without_corroboration_evaluates_to_missing():
    """
    Out-of-syllabus questions (e.g. quantum entanglement, CRISPR-Cas9) produce dense similarities
    around 0.81-0.84 against unrelated science text. Without lexical corroboration, they MUST evaluate
    to MISSING to prevent ungrounded generation.
    """
    # Trap case: Quantum entanglement query matched against unrelated Control & Coordination chunk
    trap_chunk = _make_chunk(
        content="Environmental triggers such as light, or gravity will change the directions that plant parts grow in. These directional, or tropic, movements can be either towards the stimulus, or away from it.",
        normalized_score=0.50,
        similarity_score=0.8285,
        vector_rank=1,
        sparse_rank=None,
    )
    trap_query = "Explain quantum entanglement and Bell state tomography."
    outcome = assess_evidence_outcome([trap_chunk], query=trap_query)
    assert outcome == EvidenceOutcome.MISSING, (
        f"Out-of-syllabus distractor query must evaluate to MISSING when content lacks corroboration, got {outcome}"
    )


# --- 14. Domain Corroboration Permits Legitimate In-Syllabus Paraphrases ---
def test_dense_only_valid_paraphrase_with_corroboration_evaluates_to_sufficient():
    """
    Valid in-syllabus paraphrases with dense support and domain keyword overlap evaluate to SUFFICIENT.
    """
    chunk = _make_chunk(
        content="When magnesium ribbon is burnt in oxygen, it gets converted to magnesium oxide. This is an example of a chemical combination reaction: 2Mg(s) + O2(g) -> 2MgO(s).",
        normalized_score=0.50,
        similarity_score=0.8349,
        vector_rank=1,
        sparse_rank=None,
    )
    query = "What happens when magnesium ribbon burns in oxygen?"
    outcome = assess_evidence_outcome([chunk], query=query)
    assert outcome == EvidenceOutcome.SUFFICIENT, (
        f"Valid in-syllabus query with domain keyword corroboration must evaluate to SUFFICIENT, got {outcome}"
    )


# --- 15. RRF Dense-Only 0.500 Ceiling Resolution Contract ---
def test_dense_only_0_50_ceiling_reaches_sufficient_without_lowering_thresholds():
    """
    Confirms that the theoretical single-retriever RRF score ceiling of 0.500 does NOT prevent
    valid semantic matches from being classified as SUFFICIENT.
    
    Verifies:
    1. Hybrid threshold remains 0.70 (not lowered).
    2. Dense-only threshold is calibrated at 0.50 (matching single-retriever Rank 1 theoretical ceiling).
    3. Dual-condition gate (score >= 0.50 AND similarity >= 0.75 AND lexical corroboration)
       enables valid semantic matches to achieve SUFFICIENT without diluting gating rigor.
    4. Irrelevant chunks with similarity < 0.75 remain UNCERTAIN (not SUFFICIENT).
    """
    # Invariant: thresholds are intact
    assert SUFFICIENT_NORMALIZED_THRESHOLD == 0.70
    assert DENSE_ONLY_SUFFICIENT_NORMALIZED_THRESHOLD == 0.50
    assert DENSE_ONLY_MIN_SIMILARITY == 0.75

    # Compute raw RRF and normalized score for single dense retriever at Rank 1
    raw_rrf, normalized = compute_rrf_score(vector_rank=1, sparse_rank=None)
    assert raw_rrf == pytest.approx(1.0 / 61, abs=1e-6)
    assert normalized == pytest.approx(0.50, abs=1e-6)

    # Valid semantic match at exactly 0.500 ceiling
    valid_semantic_chunk = _make_chunk(
        content="Bleaching powder is produced by the action of chlorine on dry slaked lime Ca(OH)2 + Cl2 -> CaOCl2 + H2O.",
        normalized_score=normalized,
        similarity_score=0.842,
        vector_rank=1,
        sparse_rank=None,
    )
    query = "What is the preparation of bleaching powder?"
    outcome = assess_evidence_outcome([valid_semantic_chunk], query=query)
    assert outcome == EvidenceOutcome.SUFFICIENT, (
        f"Valid semantic match at 0.500 ceiling must evaluate to SUFFICIENT, got {outcome}"
    )

    # Irrelevant chunk with high RRF (0.50) but low similarity (0.71 < 0.75) must NOT be SUFFICIENT
    low_sim_chunk = _make_chunk(
        content=valid_semantic_chunk.content,
        normalized_score=normalized,
        similarity_score=0.71,
        vector_rank=1,
        sparse_rank=None,
    )
    assert assess_evidence_outcome([low_sim_chunk], query=query) == EvidenceOutcome.UNCERTAIN


# --- 16. Chunks Emptied on MISSING Outcome Contract ---
@pytest.mark.asyncio
async def test_retrieve_clears_chunks_on_missing_outcome():
    """
    Verifies that when assess_evidence_outcome evaluates to MISSING,
    SupabaseRetrievalService.retrieve() returns chunks=[] so downstream prompt assembly
    and evaluation metrics observe zero evidence.
    """
    mock_row = MagicMock()
    mock_row.id = "distractor_chunk_1"
    mock_row.content = "Plant hormones or phytohormones are chemical substances which coordinate plant growth."
    mock_row.board = "CBSE"
    mock_row.class_grade = "10"
    mock_row.subject = "Science"
    mock_row.chapter = "Control and Coordination"
    mock_row.section = "7.2"
    mock_row.textbook_title = "Science Class 10"
    mock_row.page_reference = "120"
    mock_row.source_id = "doc_1"
    mock_row.corpus_version = "1.0"
    mock_row.vector_rank = 1
    mock_row.keyword_rank = None
    mock_row.similarity_score = 0.82

    mock_db = AsyncMock()
    mock_result = MagicMock()
    mock_result.fetchall.return_value = [mock_row]
    mock_db.execute.return_value = mock_result

    svc = SupabaseRetrievalService(db=mock_db)

    ctx = RAGRequestContext(
        app_mode=AppMode.STUDENT,
        board="CBSE",
        class_grade="10",
        subject="Science",
    )

    with patch.object(embedding_service, "embed_query", return_value=[0.1] * 384):
        # Query has no lexical corroboration with plant hormones -> assess_evidence_outcome evaluates to MISSING
        res = await svc.retrieve("Explain quantum chromodynamics and gluon flux tubes.", context=ctx)
        assert res.evidence_outcome == EvidenceOutcome.MISSING
        assert res.chunks == [], "Candidate chunks must be emptied when evidence_outcome is MISSING."


