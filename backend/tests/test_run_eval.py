"""
Unit tests for the EduNova RAG evaluation runner (backend/evals/rag/run_eval.py).

Verifies:
- Golden dataset schema and minimum required case counts.
- Mock execution runner determinism and report output generation.
- Correct handling of answerable vs unanswerable cases.
- Markdown summary generation.
"""

import json
from pathlib import Path
import pytest

from evals.rag.run_eval import (
    load_golden_dataset,
    run_evaluation,
    format_markdown_report,
    get_git_revision,
)

_GOLDEN_PATH = Path(__file__).resolve().parents[1] / "evals" / "rag" / "golden_set.jsonl"


def test_golden_dataset_schema():
    """Verify golden dataset file exists and contains valid required schema fields."""
    assert _GOLDEN_PATH.exists(), f"Golden dataset missing at {_GOLDEN_PATH}"
    cases = load_golden_dataset(_GOLDEN_PATH)
    assert len(cases) >= 20, f"Expected at least 20 cases, found {len(cases)}"

    required_fields = {
        "case_id",
        "question",
        "curriculum",
        "expected_behavior",
        "category",
        "expected_chunk_ids",
        "expected_facts",
    }

    for case in cases:
        missing = required_fields - set(case.keys())
        assert not missing, f"Case {case.get('case_id')} missing fields: {missing}"
        assert case["case_id"].startswith("EVAL-SCI10-")
        assert case["expected_behavior"] in ("SUFFICIENT", "UNCERTAIN", "MISSING", "CORPUS_NOT_CONFIGURED")


@pytest.mark.asyncio
async def test_mock_evaluation_run(tmp_path):
    """Verify evaluation runner in deterministic mock mode produces valid reports."""
    report = await run_evaluation(
        dataset_path=_GOLDEN_PATH,
        mode="mock",
        top_k=10,
        output_dir=tmp_path,
        verbose=False,
    )

    meta = report["metadata"]
    assert meta["execution_mode"] == "mock"
    assert meta["total_cases"] == 25
    assert meta["corpus_version"] == "1.0"
    assert meta["embedding_dimension"] == 384

    agg = report["aggregate_metrics"]
    assert agg["total_cases_evaluated"] == 25
    assert agg["answerable_cases"] == 20
    assert agg["unanswerable_cases"] == 5
    assert agg["mean_recall_at_5"] == 1.0
    assert agg["mean_recall_at_10"] == 1.0
    assert agg["mean_mrr_at_10"] == 1.0
    assert agg["mean_ndcg_at_10"] == 1.0
    assert agg["mean_cross_curriculum_leakage"] == 0.0
    assert agg["empty_result_correctness_rate"] == 1.0

    # Verify generated report files
    json_files = list(tmp_path.glob("eval_report_*.json"))
    md_files = list(tmp_path.glob("eval_summary_*.md"))
    assert len(json_files) >= 1
    assert len(md_files) >= 1
    assert (tmp_path / "latest_eval_report.json").exists()
    assert (tmp_path / "latest_eval_summary.md").exists()


def test_markdown_report_formatting():
    """Verify Markdown report formatter produces expected tables and sections."""
    sample_report = {
        "metadata": {
            "eval_run_id": "test-run",
            "timestamp": "2026-10-15T00:00:00Z",
            "execution_mode": "mock",
            "code_revision": "abcdef123456",
            "corpus_version": "1.0",
            "embedding_model": "intfloat/multilingual-e5-small",
            "embedding_dimension": 384,
            "dataset_file": "golden_set.jsonl",
            "total_cases": 2,
        },
        "aggregate_metrics": {
            "total_cases_evaluated": 2,
            "answerable_cases": 1,
            "unanswerable_cases": 1,
            "mean_recall_at_5": 1.0,
            "mean_recall_at_10": 1.0,
            "mean_mrr_at_10": 1.0,
            "mean_ndcg_at_10": 1.0,
            "mean_cross_curriculum_leakage": 0.0,
            "empty_result_correctness_rate": 1.0,
            "mean_latency_ms": 12.5,
            "p95_latency_ms": 15.0,
        },
        "case_diagnostics": [
            {
                "case_id": "EVAL-SCI10-001",
                "category": "direct_fact",
                "expected_behavior": "SUFFICIENT",
                "actual_outcome": "SUFFICIENT",
                "outcome_matched": True,
                "recall_5": 1.0,
                "mrr_10": 1.0,
                "retrieved_scores": [0.80],
                "failure_reasons": [],
            },
            {
                "case_id": "EVAL-SCI10-019",
                "category": "unanswerable_missing_evidence",
                "expected_behavior": "MISSING",
                "actual_outcome": "MISSING",
                "outcome_matched": True,
                "recall_5": None,
                "mrr_10": None,
                "retrieved_scores": [],
                "failure_reasons": [],
            },
        ],
    }

    md = format_markdown_report(sample_report)
    assert "# EduNova RAG Retrieval Evaluation Report" in md
    assert "EVAL-SCI10-001" in md
    assert "EVAL-SCI10-019" in md
    assert "Mean Recall@5" in md
    assert "Finding 1: RRF Single-Retriever Score Ceiling" in md


@pytest.mark.asyncio
async def test_answer_evaluation_suite(tmp_path):
    """Verify answer grounding, citation correctness, and abstention suite in mock mode."""
    report = await run_evaluation(
        dataset_path=_GOLDEN_PATH,
        mode="mock",
        suite="answer",
        output_dir=tmp_path,
        verbose=False,
    )

    suites = report.get("suites", {})
    assert "answer" in suites
    ans = suites["answer"]
    assert ans["verification_tier"] == "MOCKED"
    assert ans["passed"] is True

    m = ans["metrics"]
    assert m["abstention_correctness_rate"] == 1.0
    assert m["mean_citation_validity"] == 1.0
    assert m["mean_citation_precision"] == 1.0
    assert m["mean_fact_recall"] == 1.0
    assert m["total_cases_evaluated"] == 25


@pytest.mark.asyncio
async def test_cases_filter(tmp_path):
    """Verify filtering specific cases evaluates only the requested case IDs."""
    selected = ["EVAL-SCI10-001", "EVAL-SCI10-019"]
    report = await run_evaluation(
        dataset_path=_GOLDEN_PATH,
        mode="mock",
        suite="retrieval",
        cases_filter=selected,
        output_dir=tmp_path,
        verbose=False,
    )

    ret = report["suites"]["retrieval"]
    assert ret["total_cases"] == 2
    case_ids = [c["case_id"] for c in ret["case_diagnostics"]]
    assert case_ids == selected


def test_baseline_comparison_detection(tmp_path):
    """Verify baseline comparison detects drops in key metrics as regressions."""
    from evals.rag.run_eval import evaluate_baseline_comparison

    # Create baseline file
    baseline_file = tmp_path / "baseline.json"
    baseline_data = {
        "aggregate_metrics": {
            "mean_recall_at_5": 1.0,
            "mean_mrr_at_10": 1.0,
            "mean_cross_curriculum_leakage": 0.0,
            "empty_result_correctness_rate": 1.0,
        }
    }
    with open(baseline_file, "w", encoding="utf-8") as f:
        json.dump(baseline_data, f)

    # Current report with regression (recall dropped to 0.70)
    current_regressed = {
        "aggregate_metrics": {
            "mean_recall_at_5": 0.70,
            "mean_mrr_at_10": 1.0,
            "mean_cross_curriculum_leakage": 0.0,
            "empty_result_correctness_rate": 1.0,
        },
        "suites": {},
    }
    comp_fail = evaluate_baseline_comparison(current_regressed, baseline_file)
    assert comp_fail["regressions_detected"] is True
    assert any("mean_recall_at_5 dropped" in r for r in comp_fail["regressions"])

    # Current report matching baseline
    current_passing = {
        "aggregate_metrics": {
            "mean_recall_at_5": 0.98,  # within 0.05 tolerance
            "mean_mrr_at_10": 1.0,
            "mean_cross_curriculum_leakage": 0.0,
            "empty_result_correctness_rate": 1.0,
        },
        "suites": {},
    }
    comp_pass = evaluate_baseline_comparison(current_passing, baseline_file)
    assert comp_pass["regressions_detected"] is False

