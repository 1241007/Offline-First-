# ROADMAP.md — EduNova Verification & RAG Evaluation Suite

> **Milestone**: 1.0 Architecture Verification & Regression Suite
> **Status**: In Progress

## Phases Overview

| Phase | Name | Wave | Primary Goal | Status |
|-------|------|------|--------------|--------|
| **Phase 1** | System State & Corpus Inventory | 1 | Audit Git, schema, config, request path, and real Supabase corpus | Complete |
| **Phase 2** | Routing Contract & Orchestration Test Suite | 1 | Create `test_routing_contract.py` covering ROUTE-001 to ROUTE-012 | Complete |
| **Phase 3** | Durable RAG Evaluation Framework & Golden Set | 2 | Build `evals/rag/` with NCERT golden set and deterministic metrics | Complete |
| **Phase 4** | Retrieval Threshold Validation & Analysis | 2 | Mathematically verify RRF scoring and evaluate threshold calibration | Complete |
| **Phase 5** | Online End-to-End RAG Integration Suite | 3 | Implement `test_online_rag_e2e.py` testing live FastAPI + pgvector + LLM | Complete |
| **Phase 6** | Offline RAG Evaluation & Model Parity | 3 | Implement Android offline tests and cross-runtime E5 parity fixture | Complete |
| **Phase 7** | Curriculum-Aware Syllabus Synchronization | 4 | End-to-end sync verification across backend and Android client | Complete |
| **Phase 8** | Android Low-RAM & Performance Benchmark | 4 | Quantize footprint, latency, and memory safety evaluation | Complete |
| **Phase 9** | Unified Reusable Evaluation Command | 5 | CLI runner `evals.rag.run_eval` with JSON/Markdown reporting | Next |
| **Phase 10** | Manual End-to-End Application Checklist | 5 | Produce 15-scenario manual verification checklist | Not Started |
| **Phase 11** | Defect Remediation & Verification Reports | 5 | Surgical bug fixes, regression checks, and final verification report | Not Started |

---

## Phase Details

### Phase 1: Establish Actual System State & Corpus Inventory
- **Objective**: Establish empirical baseline of git status, backend/Android config, models, and real Supabase corpus content.
- **Deliverables**:
  - Environment inventory report (Git branch, HEAD commit, uncommitted changes preserved).
  - Database schema & Alembic head verification.
  - Read-only audit of `syllabus_documents` and `syllabus_chunks` (counts, dimensions, hashes, source provenance).
  - Traced online and offline request paths identifying connected vs missing components.

### Phase 2: Verify Routing and Request Orchestration
- **Objective**: Author dedicated routing test suite enforcing strict contracts for Student and General modes.
- **Deliverables**:
  - `backend/tests/test_routing_contract.py` covering ROUTE-001 to ROUTE-012.
  - Secret-safe structured route diagnostics.

### Phase 3: Create Durable RAG Evaluation Suite
- **Objective**: Establish version-controlled evaluation framework with verified curriculum source grounding.
- **Deliverables**:
  - `backend/evals/rag/golden_set.jsonl` (20-50 verified NCERT Class 10 Science questions).
  - `backend/evals/rag/retrieval_metrics.py` (Recall@5, Recall@10, MRR@10, nDCG@10, curriculum isolation).
  - `backend/evals/rag/answer_metrics.py` (groundedness, citation accuracy, abstention correctness).

### Phase 4: Test Retrieval Thresholds Instead of Trusting Them
- **Objective**: Empirically evaluate RRF score normalization and evidence thresholds (`SUFFICIENT` 0.70 / `UNCERTAIN` 0.35).
- **Deliverables**:
  - Threshold analysis script and precision/recall trade-off documentation.
  - Identification of single-retriever normalization limitations (dense-only rank 1 capped at 0.50).

### Phase 5: Online Retrieval and Answer Evaluation
- **Objective**: Implement live integration test verifying FastAPI + pgvector + LLM streaming pipeline.
- **Deliverables**:
  - `backend/tests/integration/test_online_rag_e2e.py`.
  - SSE event parsing verification (`metadata`, `route`, `citations`, `token`, `done`).

### Phase 6: Offline RAG Evaluation & Model Parity
- **Objective**: Validate Android local embedding inference, SQLite hybrid search, and cross-runtime parity.
- **Deliverables**:
  - Android test suite: `RouteSelectionOfflineTest.kt`, `OfflineRagRetrievalTest.kt`, `LocalEmbeddingCompatibilityTest.kt`.
  - Cross-runtime model parity evaluation fixture comparing Python ONNX vs Android Kotlin.

### Phase 7: Curriculum-Aware Synchronization Tests
- **Objective**: Test syllabus pack synchronization end-to-end between backend and Android SQLite.
- **Deliverables**:
  - Manifest authorization and pagination tests.
  - Android incremental sync, transaction rollback, and pack persistence tests.

### Phase 8: Android Runtime and Low-RAM Evaluation
- **Objective**: Measure on-device resource utilization for low-end device feasibility.
- **Deliverables**:
  - Asset size, initialization time, embedding latency, and peak RAM benchmarks.
  - Low-memory graceful degradation verification.

### Phase 9: Unified Reusable Evaluation Command
- **Objective**: Provide automated evaluation runner with standardized CLI options.
- **Deliverables**:
  - `python -m evals.rag.run_eval --suite [regression|retrieval|answer|online-e2e|offline]`.
  - JSON and Markdown evaluation artifacts.

### Phase 10: Manual Application Verification
- **Objective**: Document concrete verification checklist for developer QA.
- **Deliverables**:
  - `docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md` with 15 detailed scenarios.

### Phase 11: Defect Remediation & Verification Deliverables
- **Objective**: Surgically fix confirmed defects, verify regression suite, and publish final audit reports.
- **Deliverables**:
  - Architecture Verification Report.
  - RAG Evaluation Report.
  - Test Inventory.
  - Final Component Status Matrix.
