---
phase: 1
plan: 1
wave: 1
---

# Plan 1.1: Online Generation Quality & Error Resilience Evaluator

## Objective
Build an independently runnable evaluation suite for cloud-based LLM generation (`OpenRouterService`), validating prompt assembly, grounded answering from supplied evidence, factual correctness against reference rubrics, unsupported-claim detection, and error/fallback resilience.

## Context
- .gsd/SPEC.md
- .gsd/ROADMAP.md
- docs/evaluation/EVALUATION_INVENTORY.md
- backend/app/services/openrouter_service.py
- backend/app/services/chat_service.py
- backend/app/core/config.py
- backend/evals/rag/golden_set.jsonl

## Tasks

<task type="auto">
  <name>Build Online Generation Evaluation Suite</name>
  <files>backend/evals/generation/test_online_generation_eval.py, backend/evals/generation/answer_eval_metrics.py</files>
  <action>
    Create `backend/evals/generation/answer_eval_metrics.py` defining:
    - `compute_factual_correctness(response, reference_facts)`: verifies presence of required facts from rubric.
    - `compute_unsupported_claim_rate(response, evidence_chunks)`: detects claims without grounding in evidence.
    - `compute_instruction_adherence(response, mode)`: checks mode-specific constraints (Teacher: step-by-step + practice question; Explainable: structured rationale; General: concise direct answer).
    
    Create `backend/evals/generation/test_online_generation_eval.py` testing:
    - System instruction integrity across all 3 explanation modes.
    - Grounded generation using real CBSE curriculum evidence.
    - Reproducibility under configured decoding parameters (temperature=0.0, seed=42).
    - Separation of offline-safe deterministic mock tests from optional live provider evaluations.
  </action>
  <verify>$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m pytest backend/evals/generation/test_online_generation_eval.py -v</verify>
  <done>Online generation evaluator executes cleanly in CI-safe mode with explicit metric thresholds.</done>
</task>

<task type="auto">
  <name>Implement Provider Fallback and Error Resilience Tests</name>
  <files>backend/evals/generation/test_provider_resilience.py</files>
  <action>
    Author `backend/evals/generation/test_provider_resilience.py` testing OpenRouter edge and failure paths:
    - HTTP 429 rate limit backoff and retry behavior.
    - HTTP 502/503 server error recovery.
    - Dynamic fallback model chain (`google/gemini-3.8-flash` -> `google/gemini-2.5-flash` -> `google/gemini-3.5-flash`).
    - Fatal authentication errors (HTTP 401/403) cleanly raising `RuntimeError` without infinite loops.
    - Token streaming order preservation and disconnection handling.
  </action>
  <verify>$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m pytest backend/evals/generation/test_provider_resilience.py -v</verify>
  <done>All provider error, timeout, rate-limit, and fallback contracts verified with 100% pass rate.</done>
</task>

## Success Criteria
- [ ] Online generation quality evaluator independently executable without requiring live OpenRouter credentials.
- [ ] Metric algorithms (factual correctness, unsupported claims, instruction adherence) unit-tested with mathematical rigor.
- [ ] Provider fallback chain verified across transient and fatal HTTP statuses.
