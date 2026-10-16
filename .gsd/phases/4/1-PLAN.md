---
phase: 4
plan: 1
wave: 2
---

# Plan 4.1: Empirical Retrieval Threshold Validation and Calibration Analysis

## Objective
Rigorously test the RRF score formula, normalization bounds, and evidence thresholds (`SUFFICIENT >= 0.70`, `UNCERTAIN >= 0.35`, `MISSING < 0.35`) against empirical data to evaluate single-retriever caps, boundary conditions, and precision-recall trade-offs without making arbitrary ungrounded changes.

## Context
- .gsd/SPEC.md
- backend/app/services/supabase_retrieval_service.py
- backend/tests/test_rrf_scoring.py
- backend/tests/fixtures/rrf_retrieval_fixtures.json
- backend/evals/rag/golden_set.jsonl

## Tasks

<task type="auto">
  <name>Author RRF Mathematical Boundary and Normalization Verification Suite</name>
  <files>backend/tests/test_rrf_threshold_analysis.py</files>
  <action>
    Create `backend/tests/test_rrf_threshold_analysis.py` to test:
    - Mathematical reachability of `SUFFICIENT` (0.70) under single-retriever matches (dense-only rank 1 = 0.500).
    - Boundary behavior at exactly 0.70, 0.35, and below 0.35.
    - Duplicate chunk result handling (ensuring duplicates do not artificially inflate ranks).
    - Disconnection of cosine similarity values from RRF rank reciprocal scores.
    - Behavior when content length is below 80 bytes despite high rank scores.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_rrf_threshold_analysis.py -v</verify>
  <done>All mathematical boundary tests pass and confirm theoretical and empirical score distributions.</done>
</task>

<task type="auto">
  <name>Run Threshold Calibration Sweep on Verified Golden Set</name>
  <files>backend/evals/rag/threshold_analysis.py,docs/research/RETRIEVAL_THRESHOLD_EVALUATION.md</files>
  <action>
    Implement `threshold_analysis.py` to evaluate the golden test set across varying threshold combinations:
    - Current baseline: `SUFFICIENT=0.70`, `UNCERTAIN=0.35`
    - Evaluated sweep: `SUFFICIENT` from 0.45 to 0.75, `UNCERTAIN` from 0.25 to 0.40
    Measure: Precision, Recall, False Abstention rate, and Hallucination Exposure rate.
    Document empirical findings in `docs/research/RETRIEVAL_THRESHOLD_EVALUATION.md` with case-level evidence and actionable recommendations.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe backend/evals/rag/threshold_analysis.py</verify>
  <done>Analysis script executes across the golden set and generates documented threshold trade-off matrix.</done>
</task>

## Success Criteria
- [ ] Single-retriever ceiling (0.500 cap for dense-only rank 1) mathematically proven and documented.
- [ ] Threshold sensitivity sweep evaluated with empirical Precision, Recall, and Abstention curves.
- [ ] No production thresholds modified without supporting case-level evidence.
