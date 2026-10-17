---
phase: 11
plan: 1
wave: 5
---

# Plan 11.1: Defect Remediation, Regression Testing, and Final Deliverables

## Objective
Apply surgical fixes only to confirmed defects discovered during the audit (e.g., wiring offline retrieval and evidence evaluation in `OnlineAIRepository.kt`, connecting citation events, resolving single-retriever RRF scoring normalization), run regression suites, and produce the comprehensive final verification reports.

## Context
- .gsd/SPEC.md
- app/src/main/java/com/offline_First/data/remote/OnlineAIRepository.kt
- app/src/main/java/com/offline_First/data/provider/OnlineGeminiProvider.kt
- backend/app/services/supabase_retrieval_service.py
- docs/reports/

## Tasks

<task type="auto">
  <name>Surgically Fix Confirmed Offline and Citation Gaps</name>
  <files>app/src/main/java/com/offline_First/data/remote/OnlineAIRepository.kt,backend/app/services/supabase_retrieval_service.py</files>
  <action>
    Address confirmed defects:
    1. In `OnlineAIRepository.kt`: Wire `OfflineSyllabusRepository.retrieve(...)` instead of uncalibrated LIKE search, and enforce proper evidence abstention ("This topic was not found...") when outcome is `MISSING` to prevent silent ungrounded generation.
    2. In `backend/app/services/supabase_retrieval_service.py`: Fix RRF normalization so that single-retriever (dense-only) top-rank matches are not artificially capped at 0.500 when semantic similarity is exceptionally strong.
    Preserve all existing working tree edits and uncommitted files.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_rrf_scoring.py backend/tests/test_rag_pipeline.py -v</verify>
  <done>Surgical fixes verified with passing regression tests; no thresholds artificially weakened.</done>
</task>

<task type="auto">
  <name>Generate Final Architecture and RAG Verification Reports</name>
  <files>docs/reports/EDUNOVA_ARCHITECTURE_VERIFICATION_REPORT.md,docs/reports/EDUNOVA_RAG_EVALUATION_REPORT.md</files>
  <action>
    Produce the 5 required final deliverables:
    1. Architecture Verification Report: Step-by-step table comparing intended vs actual implementation, test evidence, status, and remediations.
    2. RAG Evaluation Report: Full empirical metrics (Recall@5, Recall@10, MRR@10, nDCG@10, cross-curriculum leakage rate, citation coverage, groundedness score, latency breakdown).
    3. Comprehensive Test Inventory: Table of all test IDs (ROUTE-*, OFF-*, EVAL-*), files, execution commands, and verified results.
    4. Copy-paste commands for continuous evaluation and regression testing.
    5. Final Component Status Matrix: PASS, FAIL, PARTIAL, or UNVERIFIED across all 11 core components, concluding with release recommendation.
  </action>
  <verify>Test-Path docs/reports/EDUNOVA_ARCHITECTURE_VERIFICATION_REPORT.md</verify>
  <done>All final reports written, thoroughly populated with empirical evidence, and verified.</done>
</task>

## Success Criteria
- [ ] Confirmed defects fixed with minimal, surgical changes and accompanied by regression tests.
- [ ] Final component status matrix accurately reflects actual empirical test results (no false claims).
- [ ] Uncommitted changes preserved without unauthorized commits.
