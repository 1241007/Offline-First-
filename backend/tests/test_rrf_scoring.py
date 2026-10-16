"""Tests for Supabase RRF evidence scoring and threshold calibration."""

import json
from pathlib import Path
import pytest

from app.schemas.rag import ChunkResult, EvidenceOutcome
from app.services.supabase_retrieval_service import (
    compute_rrf_score,
    assess_evidence_outcome,
    RRF_K,
    RRF_MAX_SCORE,
    SUFFICIENT_NORMALIZED_THRESHOLD,
    UNCERTAIN_NORMALIZED_THRESHOLD,
)

FIXTURES_PATH = Path(__file__).parent / "fixtures" / "rrf_retrieval_fixtures.json"


def test_rrf_max_score_constant():
    """Verify RRF_MAX_SCORE for k=60 with 2 retrievers equals 2/61."""
    expected = 2.0 / (RRF_K + 1)
    assert abs(RRF_MAX_SCORE - expected) < 1e-9
    # Maximum possible score is approximately ~0.032787
    assert 0.0327 < RRF_MAX_SCORE < 0.0328


def test_unrealistic_raw_threshold_rejection():
    """Demonstrate why a raw score threshold of 0.3 is mathematically impossible."""
    # Even with rank 1 in both vector and keyword search, raw score is only ~0.0328
    raw_rrf, normalized = compute_rrf_score(vector_rank=1, sparse_rank=1)
    assert raw_rrf < 0.035
    assert raw_rrf < 0.3  # Any raw threshold of 0.3 would reject every single chunk in existence!


def test_rrf_scoring_and_evidence_fixtures():
    """Verify all calibrated fixture cases pass correctly with normalized thresholds."""
    with open(FIXTURES_PATH, "r", encoding="utf-8") as f:
        fixtures = json.load(f)

    for item in fixtures:
        name = item["name"]
        v_rank = item["vector_rank"]
        s_rank = item["sparse_rank"]
        content_len = item["content_length"]
        expected_raw = item["expected_raw_rrf"]
        expected_norm = item["expected_normalized_score"]
        expected_outcome = item["expected_outcome"]

        raw_rrf, norm_score = compute_rrf_score(v_rank, s_rank)

        assert abs(raw_rrf - expected_raw) < 1e-4, f"[{name}] Raw RRF mismatch"
        assert abs(norm_score - expected_norm) < 1e-3, f"[{name}] Normalized score mismatch"

        if content_len > 0:
            dummy_content = "X" * content_len
            chunk = ChunkResult(
                chunk_id="test_chunk",
                content=dummy_content,
                relevance_score=norm_score,
                raw_rrf_score=raw_rrf,
                normalized_score=norm_score,
            )
            chunks = [chunk]
        else:
            chunks = []

        outcome = assess_evidence_outcome(chunks)
        assert outcome.value == expected_outcome, (
            f"[{name}] Outcome mismatch: got {outcome.value}, expected {expected_outcome}"
        )
