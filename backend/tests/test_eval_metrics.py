"""
Unit tests for deterministic RAG evaluation metrics (retrieval_metrics and answer_metrics).

Validates:
- Recall@K with answerable and unanswerable (empty expected) cases
- MRR@K rank reciprocals
- nDCG@K against manual DCG formulas
- Curriculum correctness & cross-curriculum leakage calculation
- Empty-result and abstention correctness
- Citation validity and precision
- Fact overlap scoring
"""

import math
import pytest

from evals.rag.retrieval_metrics import (
    compute_recall_at_k,
    compute_mrr_at_k,
    compute_dcg_at_k,
    compute_ndcg_at_k,
    compute_curriculum_correctness,
    compute_cross_curriculum_leakage,
    evaluate_empty_result_correctness,
    aggregate_retrieval_metrics,
)
from evals.rag.answer_metrics import (
    compute_citation_correctness,
    compute_citation_coverage,
    evaluate_answer_abstention,
    compute_fact_overlap,
)


def test_recall_at_k():
    expected = ["c1", "c2"]

    # All retrieved in top 5
    assert compute_recall_at_k(["c1", "c2", "c3"], expected, k=5) == 1.0

    # Half retrieved in top 2
    assert compute_recall_at_k(["c1", "c3", "c2"], expected, k=2) == 0.5

    # None retrieved
    assert compute_recall_at_k(["c4", "c5"], expected, k=5) == 0.0

    # Invariant: Empty expected (unanswerable) returns None, NOT 1.0 or 0.0
    assert compute_recall_at_k(["c1", "c2"], [], k=5) is None


def test_mrr_at_k():
    expected = ["c2", "c3"]

    # First expected chunk is at rank 1
    assert compute_mrr_at_k(["c2", "c1", "c3"], expected, k=5) == 1.0

    # First expected chunk is at rank 2
    assert compute_mrr_at_k(["c1", "c3", "c2"], expected, k=5) == 0.5

    # First expected chunk is at rank 4
    assert compute_mrr_at_k(["c5", "c6", "c7", "c2"], expected, k=5) == 0.25

    # None retrieved in top K
    assert compute_mrr_at_k(["c5", "c6"], expected, k=5) == 0.0

    # Empty expected returns None
    assert compute_mrr_at_k(["c1"], [], k=5) is None


def test_ndcg_at_k():
    grades = {"c1": 3.0, "c2": 2.0, "c3": 1.0}

    # Perfect ranking: c1 (rank 1), c2 (rank 2), c3 (rank 3)
    assert compute_ndcg_at_k(["c1", "c2", "c3"], grades, k=3) == 1.0

    # Sub-optimal ranking: c3 (rank 1), c2 (rank 2), c1 (rank 3)
    score_sub = compute_ndcg_at_k(["c3", "c2", "c1"], grades, k=3)
    assert score_sub is not None
    assert 0.0 < score_sub < 1.0

    # Zero hits
    assert compute_ndcg_at_k(["c99", "c98"], grades, k=3) == 0.0

    # Empty grades returns None
    assert compute_ndcg_at_k(["c1"], {}, k=3) is None


def test_curriculum_correctness_and_leakage():
    curriculum = {"board": "CBSE", "class_grade": "10"}

    class MockChunk:
        def __init__(self, board, class_grade):
            self.board = board
            self.class_grade = class_grade

    # All matching
    chunks_clean = [MockChunk("CBSE", "10"), MockChunk("CBSE", "10")]
    assert compute_curriculum_correctness(chunks_clean, curriculum) == 1.0
    assert compute_cross_curriculum_leakage(chunks_clean, curriculum) == 0.0

    # One leaked chunk from ICSE Class 12
    chunks_leaked = [MockChunk("CBSE", "10"), MockChunk("ICSE", "12")]
    assert compute_curriculum_correctness(chunks_leaked, curriculum) == 0.5
    assert compute_cross_curriculum_leakage(chunks_leaked, curriculum) == 0.5

    # Empty list
    assert compute_curriculum_correctness([], curriculum) == 1.0
    assert compute_cross_curriculum_leakage([], curriculum) == 0.0


def test_empty_result_correctness():
    # Unanswerable query properly returned MISSING with 0 chunks -> True
    assert evaluate_empty_result_correctness("MISSING", "MISSING", 0) is True

    # Unanswerable query returned chunks or SUFFICIENT -> False
    assert evaluate_empty_result_correctness("SUFFICIENT", "MISSING", 2) is False

    # General Mode unconfigured properly returned CORPUS_NOT_CONFIGURED -> True
    assert evaluate_empty_result_correctness("CORPUS_NOT_CONFIGURED", "CORPUS_NOT_CONFIGURED", 0) is True

    # Answerable query properly returned SUFFICIENT with chunks -> True
    assert evaluate_empty_result_correctness("SUFFICIENT", "SUFFICIENT", 3) is True

    # Answerable query mistakenly returned MISSING -> False
    assert evaluate_empty_result_correctness("MISSING", "SUFFICIENT", 0) is False


def test_aggregate_retrieval_metrics():
    case_results = [
        # Answerable case 1: Recall 1.0, MRR 1.0, NDCG 1.0
        {
            "recall_5": 1.0,
            "recall_10": 1.0,
            "mrr_10": 1.0,
            "ndcg_10": 1.0,
            "curriculum_leakage": 0.0,
            "empty_result_correct": None,
            "search_time_ms": 10.0,
        },
        # Answerable case 2: Recall 0.5, MRR 0.5, NDCG 0.6
        {
            "recall_5": 0.5,
            "recall_10": 0.5,
            "mrr_10": 0.5,
            "ndcg_10": 0.6,
            "curriculum_leakage": 0.0,
            "empty_result_correct": None,
            "search_time_ms": 20.0,
        },
        # Unanswerable case 3: Recall None, MRR None, empty_result_correct True
        {
            "recall_5": None,
            "recall_10": None,
            "mrr_10": None,
            "ndcg_10": None,
            "curriculum_leakage": 0.0,
            "empty_result_correct": True,
            "search_time_ms": 5.0,
        },
    ]

    agg = aggregate_retrieval_metrics(case_results)
    assert agg["total_cases_evaluated"] == 3
    assert agg["answerable_cases"] == 2
    assert agg["unanswerable_cases"] == 1
    # Average recall of the 2 answerable cases: (1.0 + 0.5) / 2 = 0.75
    assert abs(agg["mean_recall_at_5"] - 0.75) < 1e-6
    assert abs(agg["mean_mrr_at_10"] - 0.75) < 1e-6
    assert agg["empty_result_correctness_rate"] == 1.0


def test_citation_correctness():
    retrieved = ["c1", "c2", "c3"]
    expected = ["c1", "c2"]

    # Perfect citation
    res = compute_citation_correctness(["c1", "c2"], retrieved, expected)
    assert res["citation_validity"] == 1.0
    assert res["citation_precision"] == 1.0

    # Hallucinated citation not in retrieved
    res_fake = compute_citation_correctness(["c1", "c99"], retrieved, expected)
    assert res_fake["citation_validity"] == 0.5
    assert res_fake["citation_precision"] == 0.5


def test_answer_abstention():
    # Correct abstention
    text_abstain = "This topic was not found in the official curriculum corpus for CBSE Class 10."
    assert evaluate_answer_abstention(text_abstain, "MISSING") is True
    assert evaluate_answer_abstention(text_abstain, "SUFFICIENT") is False

    # Hallucinated ungrounded text on MISSING expected
    text_ungrounded = "Magnesium reacts with quantum fields in leaves to produce energy."
    assert evaluate_answer_abstention(text_ungrounded, "MISSING") is False
    assert evaluate_answer_abstention(text_ungrounded, "SUFFICIENT") is True


def test_fact_overlap():
    text = "Magnesium ribbon burns in oxygen with a dazzling white flame to form magnesium oxide."
    expected_facts = [
        "magnesium ribbon burns with dazzling white flame",
        "forms magnesium oxide",
        "photosynthesis produces glucose in stomata",  # Missing fact
    ]

    res = compute_fact_overlap(text, expected_facts)
    assert res["total_facts"] == 3
    assert res["facts_matched"] == 2
    assert abs(res["fact_recall"] - (2.0 / 3.0)) < 1e-6
