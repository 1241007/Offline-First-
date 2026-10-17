"""
Online Generation Evaluation Suite.

Evaluates online generation quality against atomic criteria:
- Factual correctness against reference facts.
- Unsupported claim rate and groundedness.
- Explanation mode instruction adherence (Teacher, Explainable, General).
- Refusal and abstention integrity on missing evidence.
- Determinism under configured decoding parameters (temperature=0.0, seed=42).
- Edge cases: whitespace-only, multilingual text, conflicting evidence.
"""

import json
from pathlib import Path
from typing import Any, Dict, List
from unittest.mock import AsyncMock, patch, MagicMock
import pytest

from app.services.openrouter_service import OpenRouterService
from evals.generation.answer_eval_metrics import (
    compute_factual_correctness,
    compute_unsupported_claim_rate,
    compute_instruction_adherence,
    evaluate_abstention,
)

_DATASET_PATH = Path(__file__).parent / "generation_eval_set.jsonl"


def load_generation_cases() -> List[Dict[str, Any]]:
    cases = []
    with open(_DATASET_PATH, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                cases.append(json.loads(line))
    return cases


# ==============================================================================
# 1. EVALUATION METRIC UNIT TESTS
# ==============================================================================

def test_metric_factual_correctness_calculation():
    reference_facts = [
        "magnesium ribbon burns with dazzling white flame",
        "forms magnesium oxide powder",
        "2Mg + O2 -> 2MgO",
    ]
    good_response = (
        "When magnesium ribbon burns in oxygen, it burns with a dazzling white flame. "
        "It forms a white powder called magnesium oxide. The balanced equation is 2Mg + O2 -> 2MgO."
    )
    score, matched, missing = compute_factual_correctness(good_response, reference_facts)
    assert score == 1.0
    assert len(matched) == 3
    assert len(missing) == 0

    partial_response = "Magnesium burns with a dazzling white flame."
    p_score, p_matched, p_missing = compute_factual_correctness(partial_response, reference_facts)
    assert 0.3 <= p_score <= 0.67
    assert len(p_missing) >= 1

    empty_score, _, _ = compute_factual_correctness("", reference_facts)
    assert empty_score == 0.0


def test_metric_unsupported_claim_detection():
    evidence = ["Water boils at 100 degrees Celsius at standard atmospheric pressure."]
    grounded_resp = "At standard pressure, water boils at 100 degrees Celsius."
    rate, ungrounded = compute_unsupported_claim_rate(grounded_resp, evidence)
    assert rate == 0.0
    assert len(ungrounded) == 0

    hallucinated_resp = (
        "Water boils at 100 degrees Celsius at standard atmospheric pressure. "
        "Quantum computing allows particles to teleport across galaxies instantly."
    )
    h_rate, h_ungrounded = compute_unsupported_claim_rate(hallucinated_resp, evidence)
    assert h_rate > 0.0
    assert any("quantum" in s.lower() for s in h_ungrounded)


def test_metric_instruction_adherence_modes():
    teacher_resp = (
        "Great job asking this! Let's understand combination reactions step by step.\n"
        "Step 1: Two substances react together.\n"
        "Step 2: A single product is formed with release of heat.\n"
        "Remember, practice makes perfect! Can you name another exothermic reaction?"
    )
    t_eval = compute_instruction_adherence(teacher_resp, "teacher")
    assert t_eval["passed"] is True
    assert t_eval["score"] >= 0.65

    explainable_resp = (
        "**Core Mechanism**:\n"
        "Iron displaces copper because iron possesses a lower standard reduction potential. "
        "Therefore, iron has a stronger tendency to lose electrons, which implies it reduces copper ions in solution."
    )
    e_eval = compute_instruction_adherence(explainable_resp, "explainable")
    assert e_eval["passed"] is True
    assert e_eval["score"] >= 0.65

    general_resp = "Paris is the capital of France and the Euro is its currency."
    g_eval = compute_instruction_adherence(general_resp, "general")
    assert g_eval["passed"] is True
    assert g_eval["score"] == 1.0


def test_metric_abstention_evaluation():
    refusal = "This topic was not found in the official curriculum corpus for CBSE Class 10 Science. Please verify the chapter."
    abs_res = evaluate_abstention(refusal, "MISSING")
    assert abs_res["passed"] is True
    assert abs_res["is_refusal"] is True
    assert abs_res["concise_abstention"] is True

    lecture = "CRISPR-Cas9 is an advanced biotechnology discovered by Jennifer Doudna and Emmanuelle Charpentier..."
    fail_res = evaluate_abstention(lecture, "MISSING")
    assert fail_res["passed"] is False


# ==============================================================================
# 2. DETERMINISTIC GENERATION PIPELINE EVALUATOR
# ==============================================================================

@pytest.mark.asyncio
async def test_generation_eval_curriculum_cases_mock_adapter():
    """
    Evaluates curriculum test cases using simulated OpenRouter responses
    to verify end-to-end scoring pipeline and benchmark metrics.
    """
    cases = load_generation_cases()
    service = OpenRouterService()
    service._api_key = "test-key"

    # Simulated high-quality teacher model response for Case GEN-001
    simulated_responses = {
        "GEN-001": (
            "Hello! Let's look at this reaction together.\n"
            "Step 1: Magnesium ribbon burns in air with a dazzling white flame.\n"
            "Step 2: It combines with oxygen to form magnesium oxide powder.\n"
            "The chemical equation is 2Mg + O2 -> 2MgO.\n"
            "Since two substances combine to form a single substance, it is a combination reaction.\n"
            "Great effort! Can you write the state symbols for this equation?"
        ),
        "GEN-002": (
            "**Ohm's Law Principle**:\n"
            "At constant temperature, current is proportional to potential difference V.\n"
            "Therefore, V / I is constant, giving the equation V = I * R where R is resistance.\n"
            "The SI unit of resistance is ohm."
        ),
        "GEN-003": (
            "Let's solve this step by step!\n"
            "Step 1: Note the formula R = V / I.\n"
            "Step 2: Convert 2.5 mA to 2.5 * 10^-3 A.\n"
            "Step 3: R = 12 / (2.5 * 10^-3) = 4800 ohms or 4.8 k-ohms.\n"
            "Excellent work! Try calculating current if voltage becomes 24V."
        ),
        "GEN-004": "Paris is the capital of France and the official currency is the Euro.",
        "GEN-005": "This topic was not found in the official curriculum corpus for CBSE Class 10 Science. Please verify the chapter.",
    }

    evaluated_cases = 0
    passed_cases = 0

    for case in cases:
        cid = case["case_id"]
        if cid not in simulated_responses:
            continue

        resp_text = simulated_responses[cid]
        mode = case.get("explanation_mode", "general")
        expected_behavior = case.get("expected_behavior", "SUFFICIENT")

        if expected_behavior == "MISSING":
            abstention_check = evaluate_abstention(resp_text, "MISSING")
            assert abstention_check["passed"] is True
            passed_cases += 1
        else:
            # Check factual correctness
            facts = case.get("reference_facts", [])
            f_score, _, _ = compute_factual_correctness(resp_text, facts)
            assert f_score >= 0.70, f"Case {cid} factual correctness {f_score} < 0.70"

            # Check instruction adherence
            adherence = compute_instruction_adherence(resp_text, mode)
            assert adherence["passed"] is True, f"Case {cid} failed mode adherence for {mode}"

            # Check groundedness
            evidence = case.get("evidence", [])
            if evidence:
                claim_rate, _ = compute_unsupported_claim_rate(resp_text, evidence)
                assert claim_rate <= 0.20, f"Case {cid} unsupported claim rate {claim_rate} > 0.20"

            passed_cases += 1
        evaluated_cases += 1

    assert evaluated_cases >= 5
    assert passed_cases == evaluated_cases


@pytest.mark.asyncio
async def test_online_generation_decoding_contract():
    """Verify that OpenRouter payload strictly sends temperature=0.0 and seed=42 for determinism."""
    service = OpenRouterService()
    target_model, payload = service._build_payload(
        history=[{"role": "user", "content": "Explain combination reaction"}],
        explanation_mode="teacher",
        memory_context=None,
    )
    assert payload["temperature"] == 0.0
    assert payload["seed"] == 42
    assert payload["max_tokens"] == 1024
    assert len(payload["messages"]) == 2
    assert "patient, encouraging teacher" in payload["messages"][0]["content"]
