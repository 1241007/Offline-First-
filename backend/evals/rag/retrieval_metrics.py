"""
Deterministic retrieval evaluation metrics for EduNova RAG framework.

Implements:
- Recall@K (K=5, K=10)
- Mean Reciprocal Rank (MRR@K)
- Normalized Discounted Cumulative Gain (nDCG@K)
- Curriculum Correctness & Cross-Curriculum Leakage Rate
- Empty-Result & Abstention Correctness
- Latency tracking

Mathematical Invariant:
When a test case has NO relevant ground-truth passages (e.g. unanswerable / missing evidence),
recall and MRR return None rather than 0.0 or 1.0, so that empty cases do NOT artificially
inflate or distort answerable retrieval benchmarks.
"""

import math
from typing import Any, Dict, List, Optional, Set, Tuple


def compute_recall_at_k(
    retrieved_chunk_ids: List[str],
    expected_chunk_ids: List[str],
    k: int = 5,
) -> Optional[float]:
    """
    Computes Recall@K: fraction of expected chunks retrieved in top K.
    
    Returns:
        float in [0.0, 1.0] if expected_chunk_ids is non-empty.
        None if expected_chunk_ids is empty (unanswerable case).
    """
    if not expected_chunk_ids:
        return None

    top_k_set = set(retrieved_chunk_ids[:k])
    expected_set = set(expected_chunk_ids)
    hits = len(top_k_set.intersection(expected_set))
    return hits / float(len(expected_set))


def compute_mrr_at_k(
    retrieved_chunk_ids: List[str],
    expected_chunk_ids: List[str],
    k: int = 10,
) -> Optional[float]:
    """
    Computes Reciprocal Rank (1/rank) of the FIRST relevant chunk in top K.
    
    Returns:
        float in [0.0, 1.0] if expected_chunk_ids is non-empty.
        None if expected_chunk_ids is empty.
    """
    if not expected_chunk_ids:
        return None

    expected_set = set(expected_chunk_ids)
    for rank, chunk_id in enumerate(retrieved_chunk_ids[:k], start=1):
        if chunk_id in expected_set:
            return 1.0 / float(rank)

    return 0.0


def compute_dcg_at_k(
    retrieved_chunk_ids: List[str],
    graded_relevance: Dict[str, float],
    k: int = 10,
) -> float:
    """Computes Discounted Cumulative Gain at rank K."""
    dcg = 0.0
    for rank, chunk_id in enumerate(retrieved_chunk_ids[:k], start=1):
        rel = graded_relevance.get(chunk_id, 0.0)
        if rel > 0.0:
            # (2^rel - 1) / log2(rank + 1)
            dcg += (math.pow(2.0, rel) - 1.0) / math.log2(rank + 1.0)
    return dcg


def compute_ndcg_at_k(
    retrieved_chunk_ids: List[str],
    graded_relevance: Dict[str, float],
    k: int = 10,
) -> Optional[float]:
    """
    Computes Normalized Discounted Cumulative Gain (nDCG@K).
    
    Returns:
        float in [0.0, 1.0] if graded_relevance is non-empty.
        None if graded_relevance is empty.
    """
    if not graded_relevance:
        return None

    actual_dcg = compute_dcg_at_k(retrieved_chunk_ids, graded_relevance, k=k)

    # Compute Ideal DCG (IDCG) by sorting grades descending
    sorted_grades = sorted(graded_relevance.values(), reverse=True)[:k]
    idcg = 0.0
    for rank, rel in enumerate(sorted_grades, start=1):
        if rel > 0.0:
            idcg += (math.pow(2.0, rel) - 1.0) / math.log2(rank + 1.0)

    if idcg <= 1e-12:
        return 0.0

    return min(1.0, actual_dcg / idcg)


def compute_curriculum_correctness(
    retrieved_chunks: List[Any],
    expected_curriculum: Dict[str, str],
) -> float:
    """
    Computes fraction of retrieved chunks that match expected board and class_grade.
    
    Returns 1.0 if retrieved_chunks is empty (no invalid leakage).
    """
    if not retrieved_chunks:
        return 1.0

    target_board = (expected_curriculum.get("board") or "").strip().lower()
    target_class = (expected_curriculum.get("class_grade") or "").strip().lower()

    matches = 0
    for chunk in retrieved_chunks:
        c_board = (getattr(chunk, "board", None) or (chunk.get("board") if isinstance(chunk, dict) else "") or "").strip().lower()
        c_class = (getattr(chunk, "class_grade", None) or (chunk.get("class_grade") if isinstance(chunk, dict) else "") or "").strip().lower()

        board_match = not target_board or (c_board == target_board)
        class_match = not target_class or (c_class == target_class)

        if board_match and class_match:
            matches += 1

    return matches / float(len(retrieved_chunks))


def compute_cross_curriculum_leakage(
    retrieved_chunks: List[Any],
    expected_curriculum: Dict[str, str],
) -> float:
    """Computes cross-curriculum leakage rate (1.0 - correctness)."""
    return 1.0 - compute_curriculum_correctness(retrieved_chunks, expected_curriculum)


def evaluate_empty_result_correctness(
    actual_outcome: str,
    expected_behavior: str,
    retrieved_count: int,
) -> bool:
    """
    Evaluates whether an unanswerable or out-of-scope query was handled correctly.
    """
    expected_upper = expected_behavior.upper()
    actual_upper = actual_outcome.upper()

    if expected_upper in ("MISSING", "CORPUS_NOT_CONFIGURED"):
        # Must return the expected non-evidence outcome and zero chunks
        return actual_upper == expected_upper and retrieved_count == 0
    else:
        # Expected an answerable outcome
        return actual_upper in ("SUFFICIENT", "UNCERTAIN") and retrieved_count > 0


def aggregate_retrieval_metrics(case_results: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Computes aggregate metrics across evaluation case evaluations.
    Properly handles cases where metrics return None.
    """
    recalls_5 = [r["recall_5"] for r in case_results if r.get("recall_5") is not None]
    recalls_10 = [r["recall_10"] for r in case_results if r.get("recall_10") is not None]
    mrrs_10 = [r["mrr_10"] for r in case_results if r.get("mrr_10") is not None]
    ndcgs_10 = [r["ndcg_10"] for r in case_results if r.get("ndcg_10") is not None]
    leakages = [r["curriculum_leakage"] for r in case_results if r.get("curriculum_leakage") is not None]
    empty_checks = [r["empty_result_correct"] for r in case_results if r.get("empty_result_correct") is not None]

    latencies = [r["search_time_ms"] for r in case_results if r.get("search_time_ms") is not None]

    total_cases = len(case_results)
    answerable_cases = len(recalls_5)
    unanswerable_cases = total_cases - answerable_cases

    return {
        "total_cases_evaluated": total_cases,
        "answerable_cases": answerable_cases,
        "unanswerable_cases": unanswerable_cases,
        "mean_recall_at_5": sum(recalls_5) / len(recalls_5) if recalls_5 else 0.0,
        "mean_recall_at_10": sum(recalls_10) / len(recalls_10) if recalls_10 else 0.0,
        "mean_mrr_at_10": sum(mrrs_10) / len(mrrs_10) if mrrs_10 else 0.0,
        "mean_ndcg_at_10": sum(ndcgs_10) / len(ndcgs_10) if ndcgs_10 else 0.0,
        "mean_cross_curriculum_leakage": sum(leakages) / len(leakages) if leakages else 0.0,
        "empty_result_correctness_rate": sum(1 for c in empty_checks if c) / len(empty_checks) if empty_checks else 1.0,
        "mean_latency_ms": sum(latencies) / len(latencies) if latencies else 0.0,
        "p95_latency_ms": sorted(latencies)[int(len(latencies) * 0.95)] if len(latencies) >= 20 else (max(latencies) if latencies else 0.0),
    }
