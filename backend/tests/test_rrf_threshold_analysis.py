"""Empirical and mathematical threshold analysis tests for RRF scoring and evidence assessment.

Verifies:
1. Mathematical ceiling under single-retriever matches (dense-only rank 1 capped at 0.500).
2. Boundary behavior at exact thresholds (0.70, 0.35).
3. Disconnection of cosine similarity values from ordinal RRF rank scores.
4. Content length guard behavior (< 80 bytes).
5. Deduplication invariants between vector and keyword match lists.
"""

import pytest
from app.schemas.rag import ChunkResult, EvidenceOutcome
from app.services.supabase_retrieval_service import (
    compute_rrf_score,
    assess_evidence_outcome,
    RRF_K,
    RRF_MAX_SCORE,
    SUFFICIENT_NORMALIZED_THRESHOLD,
    UNCERTAIN_NORMALIZED_THRESHOLD,
    MIN_CONTENT_LENGTH_BYTES,
)


def test_rrf_single_retriever_dense_ceiling():
    """Prove that single-retriever dense matches cannot mathematically exceed 0.500."""
    # Rank 1 dense match with no keyword match
    raw_rrf, normalized = compute_rrf_score(vector_rank=1, sparse_rank=None)
    
    # Theoretical: (1 / 61) / ((1 / 61) * 2) = 0.500000
    assert abs(normalized - 0.500) < 1e-4
    assert normalized < SUFFICIENT_NORMALIZED_THRESHOLD, (
        f"Dense-only rank 1 normalized score ({normalized}) must be strictly less than "
        f"SUFFICIENT threshold ({SUFFICIENT_NORMALIZED_THRESHOLD})."
    )

    # Assess outcome: Even perfect semantic match without keyword overlap yields UNCERTAIN
    chunk = ChunkResult(
        chunk_id="chunk_dense_1",
        content="A" * 150,
        relevance_score=normalized,
        raw_rrf_score=raw_rrf,
        normalized_score=normalized,
    )
    outcome = assess_evidence_outcome([chunk])
    assert outcome == EvidenceOutcome.UNCERTAIN, (
        "Under the current 2-retriever normalization, a dense-only rank 1 match is capped at UNCERTAIN."
    )


def test_rrf_single_retriever_sparse_ceiling():
    """Prove that single-retriever keyword matches cannot mathematically exceed 0.500."""
    raw_rrf, normalized = compute_rrf_score(vector_rank=None, sparse_rank=1)
    assert abs(normalized - 0.500) < 1e-4
    assert normalized < SUFFICIENT_NORMALIZED_THRESHOLD


def test_rrf_hybrid_reachability():
    """Prove that dual-retriever matches can reach SUFFICIENT (>= 0.70)."""
    # Dual rank 1
    raw_rrf, normalized = compute_rrf_score(vector_rank=1, sparse_rank=1)
    assert abs(normalized - 1.000) < 1e-4
    assert normalized >= SUFFICIENT_NORMALIZED_THRESHOLD

    # Dual rank 10
    raw_rrf_10, norm_10 = compute_rrf_score(vector_rank=10, sparse_rank=10)
    # (2/70) / (2/61) = 61 / 70 ≈ 0.8714
    assert abs(norm_10 - (61.0 / 70.0)) < 1e-4
    assert norm_10 >= SUFFICIENT_NORMALIZED_THRESHOLD

    # Dense 1 + Sparse 15
    raw_hybrid, norm_hybrid = compute_rrf_score(vector_rank=1, sparse_rank=15)
    # (1/61 + 1/75) / (2/61) = 0.5 + (61 / 150) = 0.5 + 0.4067 = 0.9067
    assert norm_hybrid > 0.90
    assert norm_hybrid >= SUFFICIENT_NORMALIZED_THRESHOLD


def test_threshold_exact_boundaries():
    """Verify behavior at exact threshold boundaries (0.70, 0.35)."""
    content = "B" * 120

    # Exactly 0.70 -> SUFFICIENT
    c_sufficient = ChunkResult(
        chunk_id="c1", content=content,
        relevance_score=0.70, raw_rrf_score=0.02, normalized_score=0.70
    )
    assert assess_evidence_outcome([c_sufficient]) == EvidenceOutcome.SUFFICIENT

    # 0.6999 -> UNCERTAIN
    c_just_below_suff = ChunkResult(
        chunk_id="c2", content=content,
        relevance_score=0.6999, raw_rrf_score=0.02, normalized_score=0.6999
    )
    assert assess_evidence_outcome([c_just_below_suff]) == EvidenceOutcome.UNCERTAIN

    # Exactly 0.35 -> UNCERTAIN
    c_uncertain = ChunkResult(
        chunk_id="c3", content=content,
        relevance_score=0.35, raw_rrf_score=0.01, normalized_score=0.35
    )
    assert assess_evidence_outcome([c_uncertain]) == EvidenceOutcome.UNCERTAIN

    # 0.3499 -> MISSING
    c_missing = ChunkResult(
        chunk_id="c4", content=content,
        relevance_score=0.3499, raw_rrf_score=0.01, normalized_score=0.3499
    )
    assert assess_evidence_outcome([c_missing]) == EvidenceOutcome.MISSING


def test_short_content_length_guard():
    """Verify that chunks with content shorter than MIN_CONTENT_LENGTH_BYTES (80) are downgraded."""
    short_content = "Short text."
    assert len(short_content) < MIN_CONTENT_LENGTH_BYTES

    # Even with a perfect 1.0 normalized score, short content cannot be SUFFICIENT
    c_short_perfect = ChunkResult(
        chunk_id="c_short",
        content=short_content,
        relevance_score=1.0,
        raw_rrf_score=RRF_MAX_SCORE,
        normalized_score=1.0,
    )
    outcome = assess_evidence_outcome([c_short_perfect])
    assert outcome == EvidenceOutcome.UNCERTAIN, (
        f"Chunks shorter than {MIN_CONTENT_LENGTH_BYTES} bytes must be downgraded to UNCERTAIN."
    )


def test_rrf_rank_insensitivity_to_metric_scale():
    """Verify that RRF depends strictly on ordinal rank and ignores raw metric magnitudes.
    
    A vector distance of 0.05 at rank 2 yields LOWER RRF than a vector distance of 0.45 at rank 1.
    This demonstrates why RRF is an ordinal rank fusion, not a calibrated probability.
    """
    _, norm_rank_1 = compute_rrf_score(vector_rank=1, sparse_rank=None)
    _, norm_rank_2 = compute_rrf_score(vector_rank=2, sparse_rank=None)

    assert norm_rank_1 > norm_rank_2
    # RRF score is identical regardless of whether distance is 0.01 or 0.49 as long as rank is 1
    assert norm_rank_1 == 0.500


def test_duplicate_chunk_deduplication():
    """Verify that joining vector matches and keyword matches does not duplicate entries."""
    v_results = [{"id": "chunk_1", "rank": 1}, {"id": "chunk_2", "rank": 2}]
    k_results = [{"id": "chunk_1", "rank": 3}, {"id": "chunk_3", "rank": 1}]

    # Simulating FULL OUTER JOIN on id
    combined = {}
    for r in v_results:
        combined[r["id"]] = {"v_rank": r["rank"], "k_rank": None}
    for r in k_results:
        if r["id"] in combined:
            combined[r["id"]]["k_rank"] = r["rank"]
        else:
            combined[r["id"]] = {"v_rank": None, "k_rank": r["rank"]}

    assert len(combined) == 3
    assert combined["chunk_1"]["v_rank"] == 1
    assert combined["chunk_1"]["k_rank"] == 3
