# ROADMAP.md — EduNova Verification & Layered Evaluation Suite

> **Milestone 1.0**: Architecture Verification & Retrieval Suite (`COMPLETE / FROZEN` — Candidate Release RC-1 Approved)  
> **Milestone 2.0**: Complete Layered Evaluation & Regression Framework (`COMPLETE & VERIFIED`)  
> **Target Scope**: Currently Implemented Chatbot Feature Only  

## Milestone 1.0 Phases Overview (Archive)

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
| **Phase 9** | Unified Reusable Evaluation Command | 5 | CLI runner `evals.rag.run_eval` with JSON/Markdown reporting | Complete |
| **Phase 10** | Manual End-to-End Application Checklist | 5 | Produce 15-scenario manual verification checklist | Complete |
| **Phase 11** | Defect Remediation & Verification Reports | 5 | Surgical bug fixes, regression checks, and final verification report | Complete |

---

## Milestone 2.0 Phases Overview (Layered Evaluation Framework)

| Phase | Name | Wave | Primary Goal | Status |
|:---|:---|:---:|:---|:---:|
| **Phase 0** | Repository Audit & Baseline Evaluation Inventory | 1 | Complete audit of chatbot components, existing test executions, gap analysis, and baseline inventory | **Complete** |
| **Phase 1** | Component-Wise Evaluation Expansion | 1 | Dedicated evaluators for online generation quality, offline generation quality, and prompt assembly isolation | **Next** |
| **Phase 2** | Pipeline-Wise Evaluation Suites | 2 | End-to-end integration suites for Pipelines A through G (Online/Offline Curriculum, General, Abstention, Recovery, Citations, Student Context) | **Not Started** |
| **Phase 3** | Application-Wise Evaluation & Benchmarks | 3 | Android Chatbot smoke & functional workflows (1-12), latency (TTFT), memory overhead, and offline transitions | **Not Started** |
| **Phase 4** | Golden Dataset Expansion & Semantic Metrics | 3 | Versioned dataset (`generation_eval_set.jsonl`), rubric scoring, unsupported-claim detection, and instruction adherence | **Not Started** |
| **Phase 5** | Unified Evaluation CLI Orchestrator | 4 | Multi-suite runner supporting `eval component`, `pipeline`, `application`, `all --profile [ci\|integration\|full]`, and baseline diffs | **Not Started** |
| **Phase 6** | Baselines, Thresholds, Reports & CI Automation | 4 | Versioned baseline reports, pass/fail regression gates, and GitHub Actions CI workflow | **Not Started** |
| **Phase 7** | Full Matrix Execution & Final Verification | 5 | Run complete feasible test matrix, remediate in-scope framework/product defects, generate final verification deliverables | **Not Started** |

---

## Detailed Milestone 2.0 Phase Specifications

### Phase 0: Repository Audit & Baseline Evaluation Inventory
- **Objective**: Establish complete empirical audit of chatbot components across backend and Android, recording actual test executions, gap analysis, and planned actions.
- **Deliverables**:
  - `docs/evaluation/EVALUATION_INVENTORY.md` (Delivered).
  - Empirical verification of existing test suites (Pytest: 36 passed; Parity: 7 passed, 1 skipped; Android: 180 unit passed, 6 connected AVD passed).
  - Component gap analysis distinguishing unit correctness from real integration correctness.

### Phase 1: Component-Wise Evaluation Expansion
- **Objective**: Build independently runnable evaluation suites for previously uncovered generation and prompt components without duplicating existing RAG retrieval evaluators.
- **Deliverables**:
  - `backend/evals/generation/test_online_generation_eval.py`: Online generation evaluator measuring factual correctness, rubric adherence, hallucination/unsupported-claim rate, and live provider error/retry handling.
  - `backend/evals/generation/test_offline_generation_eval.py`: Offline generation evaluator verifying GGUF artifact loading, tokenization limits, system instruction following, and strict refusal on missing evidence.
  - `backend/tests/test_prompt_construction.py`: Prompt assembly test verifying student profile propagation, explanation modes, context truncation limits, and zero cross-curriculum leakage.
  - Persistence failure injection tests: Corrupted SQLite records, transaction rollback, and disconnected recovery.

### Phase 2: Pipeline-Wise Evaluation Suites
- **Objective**: Construct deterministic, versioned integration tests for all 7 supported chatbot execution pipelines.
- **Deliverables**:
  - `backend/tests/integration/test_pipeline_e2e_matrix.py`:
    - **Pipeline A**: Online Curriculum Question (User -> Router -> Retrieval -> Evidence -> Prompt -> OpenRouter -> SSE -> Citations -> Response).
    - **Pipeline B**: Offline Curriculum Question (User -> Offline Route -> SQLite Vector -> Evidence -> Local Prompt -> Llama.cpp -> Response).
    - **Pipeline C**: General Conversation (User -> Router DIRECT -> Direct Generation -> Streaming Response).
    - **Pipeline D**: Missing / Insufficient Evidence (User -> Retrieval -> MISSING -> Strict Refusal without LLM Hallucination).
    - **Pipeline E**: Supported Fallback & Recovery (Timeout / Model error -> Model Fallback / Retry).
    - **Pipeline F**: Citation & Streaming Integration (Backend token/citation emission -> Android consumer -> UI rendering).
    - **Pipeline G**: Student-Context Integration (Board/Class/Subject isolation, Teacher vs General style).

### Phase 3: Application-Wise Evaluation & Performance Benchmarking
- **Objective**: Evaluate the complete Android chatbot from user input to rendered response across functional workflows and performance metrics.
- **Deliverables**:
  - Android application test suite covering 12 functional workflows:
    1. Submit question & render response
    2. Online stream & token rendering
    3. Offline answer without network
    4. Online/offline toggle transition
    5. Valid citation rendering
    6. Student context & answer-style adherence
    7. Empty/malformed question handling
    8. Loading, timeout, and cancellation
    9. Error recovery
    10. Lifecycle state restoration
    11. Missing evidence abstention rendering
    12. Displayed answer vs assembled response parity
  - Benchmarking metrics: TTFT (Time To First Token), total generation latency, peak heap allocation, low-RAM safety.

### Phase 4: Golden Dataset Expansion & Semantic Evaluation Metrics
- **Objective**: Create versioned evaluation datasets and calibrated semantic evaluation metrics.
- **Deliverables**:
  - `backend/evals/generation/generation_eval_set.jsonl`: Curated cases across direct facts, conceptual explanations, step-by-step problems, paraphrased queries, ambiguous queries, and Hindi/English queries.
  - `backend/evals/generation/answer_eval_metrics.py`: Calibrated metrics for factual correctness, groundedness / unsupported-claim detection, citation validity, and instruction adherence.

### Phase 5: Unified Evaluation CLI Orchestrator
- **Objective**: Provide a single CLI entry point to orchestrate all component, pipeline, and application evaluation suites.
- **Deliverables**:
  - `backend/evals/runner.py` (CLI interface):
    - `eval list`
    - `eval component --suite <name>`
    - `eval pipeline --suite <name>`
    - `eval application --suite <name>`
    - `eval all --profile [ci|integration|full]`
    - `eval compare --baseline <baseline_id>`
  - Automated generation of machine-readable JSON reports and GitHub-flavored Markdown summaries.
  - Distinct exit codes (0 for pass, 1 for regression failure, 2 for blocked/environmental failure).

### Phase 6: Baselines, Thresholds, Reports & CI Automation
- **Objective**: Establish version-controlled baseline snapshots, formal regression gates, and automated CI execution.
- **Deliverables**:
  - Versioned baseline artifact: `backend/evals/baselines/baseline_v1.0.json`.
  - Regression decision engine comparing run deltas against statistical tolerances.
  - GitHub Actions CI workflow: `.github/workflows/chatbot-eval.yml`.

### Phase 7: Full Matrix Execution & Final Verification
- **Objective**: Execute the complete feasible test matrix across CI, integration, and device profiles; fix discovered defects; document final deliverables.
- **Deliverables**:
  - Final Architecture & Evaluation Verification Report (`docs/evaluation/FINAL_VERIFICATION_REPORT.md`).
  - Recorded test outputs across all tiers.
  - Known limitations, environmental boundaries, and maintenance guidelines.

---

## Milestone 1.0 Archive (Completed Phases 1–8)

*(Preserved for historical traceability)*
- **Phase 1**: System State & Corpus Inventory (`Complete`)
- **Phase 2**: Routing Contract & Orchestration Test Suite (`Complete` — ROUTE-001 to ROUTE-012)
- **Phase 3**: Durable RAG Evaluation Framework & Golden Set (`Complete` — `evals/rag/golden_set.jsonl`)
- **Phase 4**: Retrieval Threshold Validation & Analysis (`Complete` — RRF analysis & dense-only 0.50 threshold)
- **Phase 5**: Online End-to-End RAG Integration Suite (`Complete` — `test_online_rag_e2e.py`)
- **Phase 6**: Offline RAG Evaluation & Model Parity (`Complete` — E5 ONNX parity)
- **Phase 7**: Curriculum-Aware Syllabus Synchronization (`Complete` — Auth gating & sync)
- **Phase 8**: Android Low-RAM & Performance Benchmark (`Complete` — Connected AVD execution on API 36/37)
