"""
Domain Adherence & Scope Boundary Evaluation Suite (Phase 2).

Validates:
- Dataset integrity against test_case_schema.json.
- Deterministic classification of Out-of-Domain prohibited queries (target: 0.0 FAR).
- Legitimate in-domain preservation (target: < 0.05 FRR).
- Clarification requests on ambiguous prompts (target: >= 0.90).
- Mixed-intent prompt decomposition (target: >= 0.90).
- Missing evidence distinction (not misclassified as out-of-domain).
"""

import json
import sys
from pathlib import Path
import pytest

BACKEND_DIR = Path(__file__).resolve().parent.parent.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

from app.services.domain_policy import DomainIntent, domain_policy_service
from evals.case_schema import validate_case
from evals.domain.domain_eval_metrics import (
    aggregate_domain_metrics,
    evaluate_domain_test_case,
)

BENCHMARK_PATH = Path(__file__).parent / "domain_scope_benchmark.jsonl"


@pytest.fixture(scope="module")
def benchmark_cases():
    assert BENCHMARK_PATH.exists(), f"Benchmark dataset not found at {BENCHMARK_PATH}"
    cases = []
    with open(BENCHMARK_PATH, "r", encoding="utf-8") as f:
        for line_num, line in enumerate(f, 1):
            line_str = line.strip()
            if not line_str or line_str.startswith("#"):
                continue
            case_data = json.loads(line_str)
            # Validate every case against test_case_schema.json
            valid, err = validate_case(case_data)
            assert valid, f"Line {line_num} failed schema validation: {err}"
            cases.append(case_data)
    assert len(cases) >= 30, f"Expected at least 30 benchmark cases, found {len(cases)}"
    return cases


def test_domain_dataset_schema_and_fingerprint(benchmark_cases):
    """Verify that all benchmark dataset records conform strictly to the standard JSON schema."""
    assert len(benchmark_cases) == 60
    categories = {c["domain_category"] for c in benchmark_cases}
    assert "in_domain_curriculum" in categories
    assert "in_domain_general_educational" in categories
    assert "out_of_domain_prohibited" in categories
    assert "ambiguous_clarification" in categories
    assert "mixed_intent" in categories
    assert "missing_evidence_trap" in categories


def test_holdout_split_contamination_and_isolation(benchmark_cases):
    """Verify that dev and holdout sets are completely isolated with zero prompt overlap."""
    dev_cases = [c for c in benchmark_cases if c.get("split") == "dev"]
    holdout_cases = [c for c in benchmark_cases if c.get("split") == "holdout"]

    assert len(dev_cases) == 30, f"Expected 30 dev cases, got {len(dev_cases)}"
    assert len(holdout_cases) == 30, f"Expected 30 holdout cases, got {len(holdout_cases)}"

    dev_prompts = {c["prompt"].strip().lower() for c in dev_cases}
    holdout_prompts = {c["prompt"].strip().lower() for c in holdout_cases}

    overlap = dev_prompts.intersection(holdout_prompts)
    assert len(overlap) == 0, f"Critical holdout leakage detected! Overlapping prompts: {overlap}"


def test_domain_adherence_and_metrics_evaluation(benchmark_cases):
    """Runs domain evaluation across the full benchmark dataset and computes rigorous metrics."""
    evaluated_cases = []

    for case in benchmark_cases:
        prompt = case["prompt"]
        app_mode = case.get("app_mode", "student")

        classification = domain_policy_service.classify_prompt(prompt, app_mode=app_mode)
        eval_outcome = evaluate_domain_test_case(case, classification)
        evaluated_cases.append(eval_outcome)

    metrics = aggregate_domain_metrics(evaluated_cases)
    metrics_summary = metrics.to_dict()

    print("\n" + "=" * 60)
    print("DOMAIN POLICY EVALUATION SUMMARY:")
    print("=" * 60)
    print(f"Total Evaluated Cases:              {metrics.total_cases}")
    print(f"OOD False Acceptance Rate (FAR):    {metrics.ood_false_acceptance_rate} (raw: {metrics.ood_false_accepted}/{metrics.ood_total})")
    print(f"In-Domain False Rejection Rate:     {metrics.in_domain_false_rejection_rate} (raw: {metrics.in_domain_false_rejected}/{metrics.in_domain_total})")
    print(f"Ambiguous Clarification Rate:       {metrics.clarification_rate} (raw: {metrics.ambiguous_clarification_asked}/{metrics.ambiguous_total})")
    print(f"Mixed-Intent Compliance Rate:       {metrics.mixed_intent_compliance_rate} (raw: {metrics.mixed_intent_compliant}/{metrics.mixed_intent_total})")
    print(f"Missing-Evidence Preservation Rate: {metrics.missing_evidence_compliance_rate} (raw: {metrics.missing_evidence_abstained}/{metrics.missing_evidence_total})")
    print("=" * 60)

    # 1. Prohibited requests must NEVER be falsely accepted
    assert metrics.ood_false_accepted == 0, (
        f"Detected {metrics.ood_false_accepted} falsely accepted prohibited requests!"
    )
    assert metrics.ood_false_acceptance_rate == 0.0

    # 2. Legitimate in-domain requests must NOT be falsely rejected
    assert metrics.in_domain_false_rejected == 0, (
        f"Detected {metrics.in_domain_false_rejected} falsely rejected in-domain queries!"
    )
    assert metrics.in_domain_false_rejection_rate == 0.0

    # 3. Ambiguous queries must trigger clarification
    assert metrics.ambiguous_clarification_asked == metrics.ambiguous_total
    assert metrics.clarification_rate == 1.0

    # 4. Mixed-intent queries must be decomposed
    assert metrics.mixed_intent_compliant == metrics.mixed_intent_total
    assert metrics.mixed_intent_compliance_rate == 1.0

    # 5. Missing evidence queries must NOT be misclassified as out-of-domain
    assert metrics.missing_evidence_misclassified_as_ood == 0
    assert metrics.missing_evidence_compliance_rate == 1.0


def test_empty_denominators_handling():
    """Verify that empty categories do NOT silently report a fake 100% score."""
    from evals.domain.domain_eval_metrics import DomainEvaluationMetrics

    empty_metrics = DomainEvaluationMetrics(total_cases=0)
    assert empty_metrics.ood_false_acceptance_rate is None
    assert empty_metrics.in_domain_false_rejection_rate is None
    assert empty_metrics.clarification_rate is None
    assert empty_metrics.mixed_intent_compliance_rate is None
    assert empty_metrics.missing_evidence_compliance_rate is None
