# EVALUATION_INVENTORY.md — EduNova Chatbot Evaluation & Regression Inventory

> **Document Version**: 1.0.0  
> **Repository**: EduNova (Offline First)  
> **Scope**: Currently Implemented Chatbot Feature Only  
> **Date**: October 17, 2026  
> **Audit Status**: Complete Baseline Audit & Live Verification  

---

## 1. Executive Summary & Inventory Overview

This inventory documents all functional chatbot components across the **FastAPI Cloud Backend** and the **Android Native Client**, mapping existing source implementations to their test coverage, verified empirical results, identified gaps, and planned actions.

All tests referenced in this inventory were directly executed and validated against real environments (Python 3.11 backend venv, Gradle Android unit test runner, and Android AVD connected emulators).

---

## 2. Component-by-Component Evaluation Inventory

| Component | Location (Source Files & Classes) | Existing Tests | Current Metrics & Thresholds | Execution Command | Verified Evidence | Coverage Gaps | Planned Action |
|:---|:---|:---|:---|:---|:---|:---|:---|
| **1. RAG Hybrid Retrieval & Evidence Evaluation (Backend)** | `backend/app/services/supabase_retrieval_service.py` (`SupabaseRetrievalService`)<br>`backend/app/schemas/rag.py` | `backend/evals/rag/run_eval.py`<br>`backend/tests/test_eval_metrics.py`<br>`backend/tests/test_phase4_retrieval_policy.py` | Recall@5, Recall@10, MRR@10, nDCG@10, Cross-Curriculum Leakage, Empty-Result Correctness.<br>Thresholds: Dual match `SUFFICIENT >= 0.70`, Dense-only `SUFFICIENT >= 0.50` with `sim >= 0.75` & lexical corroboration, `UNCERTAIN >= 0.35`, `MISSING < 0.35`. | `$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m evals.rag.run_eval --mode mock`<br>`python -m pytest backend/tests/test_eval_metrics.py backend/tests/test_phase4_retrieval_policy.py` | **25/25 golden cases passed** in mock mode (Recall@5=1.0, MRR@10=1.0, Leakage=0.0%, Empty=100%, avg latency 14.3ms).<br>9 unit tests in `test_eval_metrics.py` passed.<br>Live authorization verified with 0 leaks across 210 legacy chunks. | Generation quality on retrieved context is evaluated separately from retrieval; golden dataset has 25 cases (primarily Class 10 Science Chapter 1). Needs expansion across broader subject distribution and Hindi queries. | **Reuse & Extend**: Keep existing retrieval evaluators unchanged; link into unified orchestrator and generation evaluation harness. |
| **2. Online LLM Generation (Backend)** | `backend/app/services/openrouter_service.py` (`OpenRouterService`)<br>`backend/app/services/gemini_service.py`<br>`backend/app/core/config.py` | `backend/tests/test_deterministic_generation.py`<br>`backend/tests/test_openrouter_service.py`<br>`backend/tests/integration/test_sse_parser_contract.py` | Deterministic decoding parameters (`temperature=0.0`, `seed=42`, `max_tokens=1024`).<br>Byte-for-byte identical output contract without caching.<br>Fallback model chain (`gemini-3.8-flash` -> `gemini-2.5-flash` -> `gemini-3.5-flash`). | `python -m pytest backend/tests/test_deterministic_generation.py -v` | **7/7 tests passed** in 0.55s.<br>Confirmed payload decoding parameters and lack of cache.<br>Confirmed distinct prompt templates per explanation mode. | Lacks an automated generation quality benchmark: no factual correctness rubric, hallucination/unsupported-claim rate, reference answer comparison, or live OpenRouter latency/token tracking. | **Add**: Build dedicated online generation evaluation suite (`evals/generation/test_online_generation_eval.py`) with calibrated rubric and reference facts. |
| **3. Offline On-Device Generation (Android)** | `app/src/main/java/com/offline_First/data/local/LocalAIRepository.kt`<br>`app/src/main/java/com/offline_First/data/local/OfflineInferenceEngine.kt`<br>`app/src/main/java/com/offline_First/data/local/OfflineModelManager.kt`<br>`app/src/main/java/com/offline_First/data/provider/OfflineLlamaProvider.kt` | `app/src/test/.../DeterministicInferenceTest.kt`<br>`app/src/test/.../OfflineModelLifecycleTest.kt`<br>`app/src/test/.../OfflineChatEndToEndTest.kt`<br>`app/src/androidTest/.../OfflineModelAndParityInstrumentedTest.kt` | Model integrity (GGUF magic `0x47475546`, header version 2-3).<br>Asset footprint (< 150 MB budget; Qwen GGUF 1.5B/3B profiles).<br>Cold start latency & heap allocation (< 2 MB overhead). | `.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.DeterministicInferenceTest"`<br>`.\gradlew.bat connectedDebugAndroidTest` | **180/180 Android unit tests passed**.<br>6/6 connected instrumented tests passed on real AVDs (`Medium_Phone(AVD) - 16` and `Pixel_9(AVD) - 17`).<br>Integrity verification confirmed. | Real on-device answer generation quality, instruction adherence (Teacher mode formatting), and refusal to hallucinate missing evidence under real GGUF execution need automated evaluation fixtures with clear mock vs emulator vs physical separation. | **Add**: Build dedicated offline generation evaluator (`evals/generation/offline_generation_eval.py` / Android offline benchmark test) with host-side mock and connected test runners. |
| **4. Router & Execution Selection** | `backend/app/services/routing_service.py` (`RoutingService`)<br>`backend/app/services/route_header_parser.py`<br>`app/src/main/java/com/offline_First/data/repository/ModeAwareAIRepository.kt` | `backend/tests/test_routing_contract.py`<br>`backend/tests/test_route_header_parser.py`<br>`app/src/test/.../RouteHeaderParserTest.kt`<br>`app/src/test/.../OfflineModeBugConditionTest.kt` | ROUTE-001 to ROUTE-012 contracts:<br>- Conversational fast-path -> DIRECT<br>- Student mode curriculum query -> RAG<br>- General mode coding/math -> DIRECT<br>- Timeout/malformed fallback -> RAG<br>- No-evidence -> deterministic refusal | `python -m pytest backend/tests/test_routing_contract.py -v` | **17/17 tests passed** in 0.82s.<br>Zero leaks of untrusted client profile overrides.<br>Deterministic abstention verified. | Router contract tests exist at unit level, but need end-to-end integration assertions across network drop / online-offline transition scenarios. | **Reuse & Extend**: Reuse 17 routing unit tests; integrate router decision matrix into the end-to-end pipeline test suite. |
| **5. Prompt Construction & Context Assembly** | `backend/app/services/chat_service.py` (`ChatService._build_payload`)<br>`backend/app/services/openrouter_service.py` (`_format_messages_for_openrouter`)<br>`app/src/main/java/com/offline_First/data/local/LocalAIRepository.kt` (`systemPromptFor`) | `backend/tests/test_context_propagation.py`<br>`backend/tests/test_deterministic_generation.py`<br>`app/src/test/.../RAGContextPropagationTest.kt` | System instruction adherence per mode (`teacher`, `general`, `explainable`).<br>Context window truncation (`chat_context_messages=10`).<br>Zero cross-curriculum leakage in injected references. | `python -m pytest backend/tests/test_context_propagation.py backend/tests/test_deterministic_generation.py -v` | **All tests passed**.<br>Verified distinct system prompts per explanation mode and memory injection format. | Structural snapshot testing for prompt boundary conditions (very long query, max context token overflow, multi-chunk formatting, missing evidence template). | **Add**: Build focused prompt assembly evaluator (`backend/tests/test_prompt_construction.py`) testing token limits, formatting invariants, and isolation. |
| **6. Streaming, Citations, & Response Assembly** | `backend/app/api/v1/endpoints/chat.py`<br>`backend/app/services/chat_service.py`<br>`app/src/main/java/com/offline_First/data/provider/OnlineGeminiProvider.kt`<br>`app/src/main/java/com/offline_First/data/remote/ChatApiClient.kt`<br>`app/src/main/java/com/offline_First/ui/screens/AIWorkspaceScreen.kt` | `backend/tests/integration/test_sse_parser_contract.py`<br>`backend/tests/test_chat_stream.py`<br>`app/src/test/.../OnlineChatIntegrationTest.kt` | SSE event schema: `metadata` -> `route` -> `citations` (optional) -> `token`* -> `done`.<br>Citation model: `textbook_title`, `chapter`, `section`, `page_reference`, `score`, `chunk_id`.<br>UI citation rendering: citations only rendered when `citations.isNotEmpty()`. | `python -m pytest backend/tests/integration/test_sse_parser_contract.py -v`<br>`.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.remote.OnlineChatIntegrationTest"` | **4/4 backend tests passed** in 0.40s.<br>Android unit tests passed.<br>Verified token ordering, metadata reservation, and buffered citation emission. | Full end-to-end streaming integrity test asserting zero lost tokens, zero duplicate tokens, and cancellation mid-stream during client lifecycle event. | **Extend**: Integrate full event validation into Pipeline F and Application Test Suite. |
| **7. Persistence & Offline Data Access** | `app/src/main/java/com/offline_First/data/local/ChatCacheDatabase.kt`<br>`app/src/main/java/com/offline_First/data/local/SyllabusDatabase.kt`<br>`backend/app/repositories/chat_repository.py` | `app/src/test/.../SyllabusDatabaseTest.kt`<br>`app/src/test/.../AndroidPerformanceBenchmarkTest.kt`<br>`backend/tests/test_syllabus_authorization_gating.py`<br>`backend/tests/test_curriculum_sync_e2e.py` | Schema v2 SQLite BLOB vector storage (384-d float32 = 1,536 bytes/chunk).<br>Transaction safety on sync rollback.<br>Zero leaks of unauthorized textbook chunks. | `.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.SyllabusDatabaseTest"`<br>`python -m pytest backend/tests/test_syllabus_authorization_gating.py -v` | **6/6 SQLite tests passed**.<br>Authorization gating test verified against live Supabase: 0 leaks across 210 unauthorized legacy textbook chunks. | Corrupted record recovery, SQLite disk-full / constraint error resilience during offline chat persistence. | **Extend**: Add persistence failure-injection test cases in component suite. |
| **8. Model Parity (Embedding & Preprocessing)** | `backend/app/services/embedding_service.py`<br>`app/src/main/java/com/offline_First/data/local/E5EmbeddingService.kt`<br>`backend/models/model_quantized.onnx` | `backend/evals/parity/test_model_parity.py`<br>`app/src/test/.../LocalEmbeddingCompatibilityTest.kt` | Cosine similarity >= 0.999 across Python ONNX and Android ONNX.<br>Mean Absolute Error (MAE) < 1e-4.<br>Identical ranking on top-K curriculum index.<br>Asymmetric prefix separation (`query: ` vs `passage: `). | `python -m pytest backend/evals/parity/test_model_parity.py -v` | **7 passed, 1 skipped** in 13.54s.<br>Confirmed artifact SHA-256 (`model_quantized.onnx` = 112.83 MB, `tokenizer.json` = 16.29 MB).<br>Ranking agreement verified on Hindi and English queries. | Android cross-runtime test was skipped in Python test runner because Android Kotlin runtime output needs automated loading from fixture file. | **Reuse & Maintain**: Keep test_model_parity.py active; wire into CI regression gate. |
| **9. Evaluation CLI & Orchestrator** | `backend/evals/rag/run_eval.py` | `backend/tests/test_run_eval.py` | CLI execution with `--mode [mock|live]`, `--top-k`, `--output-dir`.<br>Produces `eval_report_<timestamp>.json` and `eval_summary_<timestamp>.md`. | `python -m pytest backend/tests/test_run_eval.py -v`<br>`python -m evals.rag.run_eval --mode mock` | **3/3 unit tests passed**.<br>CLI executed in 0.8s on 25 golden cases. | Currently only executes RAG retrieval suite; does not orchestrate online generation, offline generation, pipeline suites, application smoke tests, or multi-suite CI/full profiles. | **Extend & Transform**: Upgrade `run_eval.py` into unified evaluator CLI supporting `eval component`, `eval pipeline`, `eval application`, `eval all --profile [ci|full]`, and `eval compare --baseline`. |

---

## 3. Gap Analysis

### 3.1 What Is Already Verified and Strong
1. **RAG Retrieval & Thresholds**: Hybrid RRF retrieval, dense-only thresholds (0.50), dual-match thresholds (0.70), and lexical corroboration are mathematically proven and empirically verified with zero distractor leaks.
2. **Routing Contract**: Fast-path, Student mode RAG defaults, untrusted parameter rejection, and timeout fallback are completely verified across 17 test cases.
3. **Model Parity**: 384-dimensional quantized E5 ONNX embeddings match between Python and Android fixtures within 1e-4 MAE.
4. **Android Offline RAG & Low-RAM**: On-device vector similarity, SQLite BLOB storage, and performance bounds (< 150 MB footprint, < 0.5 ms query latency, < 2 MB heap allocation) have been verified on real connected AVDs.
5. **Authorization Gating**: Untrusted textbook chunks (ncert.nic.in) are 100% blocked from leaking into authorized CBSE student queries.

### 3.2 What Is Missing (The Real Gaps)
1. **Online Generation Quality Evaluator**:
   - No benchmark measuring factual correctness against a reference answer set.
   - No automated detection of unsupported claims or hallucinations in generated text.
   - No automated validation of answer style adherence (e.g. did Teacher mode include step-by-step guidance and practice questions?).
2. **Offline Generation Quality Evaluator**:
   - No automated test harness evaluating real GGUF output quality on device/host.
   - Missing test for prompt injection of retrieved context into local LLM prompt.
   - Missing validation that offline generation strictly refuses to fabricate missing evidence.
3. **Pipeline Integration Suites**:
   - The repository has separate component tests and a basic `test_online_rag_e2e.py`, but lacks standardized, versioned test suites for the 7 canonical pipelines (Pipelines A through G).
4. **Application-Wise Automated Evaluation**:
   - UI tests currently test individual ViewModels (`AIViewModelTest`), but do not run full user workflows (submit question -> stream -> render tokens -> render citations -> toggle offline -> recover).
5. **Unified Multi-Suite Orchestrator & Regression Gates**:
   - `evals/rag/run_eval.py` only covers RAG retrieval. There is no unified entry point to run component, pipeline, application, and regression comparison suites with standardized exit codes and profiles (`ci`, `integration`, `full`).
6. **Continuous Integration (CI) Workflow**:
   - The repository has no `.github/workflows/` CI automation to run regression gates on pull requests.

---

## 4. Prioritized Implementation Plan

Based on actual findings and following the mandatory 13-step execution order:

```mermaid
flowchart TD
    P0[Phase 0: Inventory & Audit] --> P1[Phase 1: Component Generation & Prompt Evaluators]
    P1 --> P2[Phase 2: Pipeline Integration Suites A-G]
    P2 --> P3[Phase 3: Application-Wise Chatbot Regression Suite]
    P3 --> P4[Phase 4: Golden Dataset Expansion & Evaluation Metrics]
    P4 --> P5[Phase 5: Unified Evaluation CLI & Regression Gates]
    P5 --> P6[Phase 6: Baselines, Reports & CI Automation]
    P6 --> P7[Phase 7: Full Matrix Execution & Verification Report]
```

### Milestone & Phase Mapping:
- **Phase 0 — Repository Audit & Baseline Recording**: Complete (this inventory, test run records, gap analysis).
- **Phase 1 — Component-Wise Evaluation Expansion**:
  - Implement online generation quality evaluator (`backend/evals/generation/test_online_generation_eval.py`).
  - Implement offline on-device generation evaluator with artifact checks and mock/device harnesses.
  - Implement prompt assembly & context isolation test suite (`backend/tests/test_prompt_construction.py`).
- **Phase 2 — Pipeline Integration Suites**:
  - Implement Pipeline A (Online Curriculum Question end-to-end).
  - Implement Pipeline B (Offline Curriculum Question with local SQLite & inference).
  - Implement Pipeline C (General conversation direct route).
  - Implement Pipeline D (Missing/insufficient evidence strict abstention).
  - Implement Pipeline E (Network drop / timeout fallback & recovery).
  - Implement Pipeline F (Citation event propagation & rendering).
  - Implement Pipeline G (Student profile context & subject isolation).
- **Phase 3 — Application-Wise Evaluation**:
  - Build Android Chatbot Smoke & Regression test suite covering workflows 1-12.
  - Measure end-to-end latency (TTFT, total generation time), heap footprint, and lifecycle recovery.
- **Phase 4 — Evaluation Datasets & Semantic Metrics**:
  - Expand golden evaluation dataset to include reference rubrics, factual answer rubrics, and Hindi/English cases (`backend/evals/generation/generation_eval_set.jsonl`).
  - Implement semantic answer metrics (correctness rubric score, unsupported-claim detection, instruction adherence).
- **Phase 5 — Unified CLI Orchestrator & Regression Gates**:
  - Upgrade `evals/rag/run_eval.py` into a unified `evals/runner.py` / CLI supporting `--profile [ci|integration|full]`, `--suite <suite>`, `--baseline <file>`, and `--fail-on-regression`.
- **Phase 6 — Continuous Integration & Baselines**:
  - Add GitHub Actions CI workflow (`.github/workflows/chatbot-eval.yml`).
  - Generate versioned baseline report (`backend/evals/baselines/baseline_v1.0.json`).
- **Phase 7 — Complete Verification & Final Deliverables**:
  - Execute full CI profile and integration profile.
  - Generate comprehensive verification report with exact results, limitations, and reproducible commands.
