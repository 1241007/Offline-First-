---
phase: 9
plan: 1
wave: 5
---

# Plan 9.1: Design and Implement Unified Reusable Evaluation Command

## Objective
Implement `python -m evals.rag.run_eval`, providing a single CLI runner to execute evaluation suites (`regression`, `retrieval`, `answer`, `online-e2e`, `offline`), generate machine-readable JSON reports, human-readable Markdown summaries, and support baseline comparison.

## Context
- .gsd/SPEC.md
- backend/evals/rag/golden_set.jsonl
- backend/evals/rag/retrieval_metrics.py
- backend/evals/rag/answer_metrics.py
- backend/app/core/config.py

## Tasks

<task type="auto">
  <name>Implement Unified CLI Runner (evals/rag/run_eval.py)</name>
  <files>backend/evals/rag/run_eval.py</files>
  <action>
    Create `backend/evals/rag/run_eval.py` supporting:
    - `--suite [regression|retrieval|answer|online-e2e|offline|all]`
    - `--cases <id1,id2>`: Filter specific evaluation case IDs
    - `--output-dir <path>`: Directory to save reports (default `backend/evals/rag/results/`)
    - `--baseline <path>`: Compare current run against a previous baseline JSON report
    - `--fail-on-regression`: Exit code 1 if metrics drop below baseline or contracts fail
    Collect metadata: git commit, branch, timestamp, model ID, corpus version, threshold settings.
    Generate two artifacts per run:
    1. Machine-readable JSON summary (`eval_run_<timestamp>.json`)
    2. GitHub-flavored Markdown report (`eval_run_<timestamp>.md`)
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m evals.rag.run_eval --suite regression</verify>
  <done>Evaluation CLI executes regression suite cleanly and produces formatted JSON/Markdown reports.</done>
</task>

<task type="auto">
  <name>Document Golden Set Governance and Evaluation Guide</name>
  <files>backend/evals/rag/GOVERNANCE.md</files>
  <action>
    Author `backend/evals/rag/GOVERNANCE.md` detailing:
    - Policy for adding new cases: required source provenance, expected passages, documented outcome, and reviewer sign-off.
    - Baseline update workflow.
    - Separation of deterministic contracts from stochastic LLM evaluations.
    - Security guidelines: zero secret exposure, zero raw textbook passage dumps.
  </action>
  <verify>Test-Path backend/evals/rag/GOVERNANCE.md</verify>
  <done>Governance documentation established for ongoing project maintenance.</done>
</task>

## Success Criteria
- [ ] Documented CLI commands functional and verified with real executions.
- [ ] Generated reports contain commit metadata, latency metrics, and case breakdowns.
- [ ] Baseline comparison detects metric regressions accurately.
