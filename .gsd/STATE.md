# STATE.md — Current Session Memory

## Current Position
- **Milestone 2.0**: Complete Layered Evaluation & Regression Framework (`EXECUTION COMPLETE & VERIFIED`)
- **Milestone 1.0**: Architecture Verification & Retrieval Suite (`COMPLETE / FROZEN` — Candidate Release RC-1 Approved)

---

## Executive Summary of Milestone 2.0 Completions

1. **Phase 0 — Repository & Architecture Audit**:
   - Delivered [EVALUATION_INVENTORY.md](file:///e:/Offline%20First/docs/evaluation/EVALUATION_INVENTORY.md) establishing current source implementations, test tiers, and gap analysis.
2. **Phase 1 — RAG Evaluation Audit & Live Diagnosis**:
   - Audited `evals/rag/run_eval.py`. Proved `mock` mode synthesized tautological 100% scores.
   - Identified root cause in live retrieval: `supabase_retrieval_service.py` filters `sd.source_url LIKE '%cbseacademic.nic.in%'`, deliberately isolating the 9 authorized CBSE framework chunks and rejecting the 210 legacy NCERT textbook chunks.
3. **Phase 2 — Online Generation Quality Evaluator**:
   - Implemented [answer_eval_metrics.py](file:///e:/Offline%20First/backend/evals/generation/answer_eval_metrics.py) and [generation_eval_set.jsonl](file:///e:/Offline%20First/backend/evals/generation/generation_eval_set.jsonl).
   - Verified [test_online_generation_eval.py](file:///e:/Offline%20First/backend/evals/generation/test_online_generation_eval.py): 6 passed.
4. **Phase 3 — Online Provider Resilience**:
   - Verified [test_provider_resilience.py](file:///e:/Offline%20First/backend/evals/generation/test_provider_resilience.py): 6 passed (429 backoff, 404 fallback, 401 fast-fail, secret safety, stream drop).
5. **Phase 4 — Offline Generation & On-Device Model Validation**:
   - Verified [test_offline_generation_eval.py](file:///e:/Offline%20First/backend/evals/generation/test_offline_generation_eval.py): 6 passed (GGUF header validation, ChatML prompt formatting, abstention invariants).
   - Android test [OfflineGenerationQualityTest.kt](file:///e:/Offline%20First/app/src/test/java/com/offline_First/data/local/OfflineGenerationQualityTest.kt): PASSED in Gradle.
6. **Phase 5 — Prompt Assembly, Context Isolation & Persistence**:
   - Verified [test_prompt_construction.py](file:///e:/Offline%20First/backend/tests/test_prompt_construction.py): 7 passed.
   - Verified [test_chat_persistence_invariants.py](file:///e:/Offline%20First/backend/tests/test_chat_persistence_invariants.py): 3 passed.
   - Android test [ChatPersistenceInvariantTest.kt](file:///e:/Offline%20First/app/src/test/java/com/offline_First/data/local/ChatPersistenceInvariantTest.kt): PASSED in Gradle.
7. **Phase 6 — Pipeline Integration Matrix (Pipelines A–G)**:
   - Verified [test_pipeline_e2e_matrix.py](file:///e:/Offline%20First/backend/tests/integration/test_pipeline_e2e_matrix.py): 7 passed.
8. **Phase 7 — Android Application-Level Verification**:
   - Verified [ChatbotApplicationWorkflowsTest.kt](file:///e:/Offline%20First/app/src/test/java/com/offline_First/ui/screens/ai/ChatbotApplicationWorkflowsTest.kt): 8 workflows PASSED in Gradle.
9. **Phase 8 & 10 — Evaluation Runner & Regression Gate**:
   - Implemented [backend/evals/runner.py](file:///e:/Offline%20First/backend/evals/runner.py).
   - Validated gate self-test (`--self-test-gate`) exits with error on simulated regression.
   - Saved baseline [baseline.json](file:///e:/Offline%20First/backend/evals/results/baseline.json).
   - Generated final report [chatbot_evaluation_report.md](file:///e:/Offline%20First/docs/reports/chatbot_evaluation_report.md).

---

## Executive Summary of Milestone 1.0 (Archive)

- **Status**: **COMPLETE (Milestone 1.0 Verified — Candidate Release RC-1 Approved, Physical Low-RAM Hardware Tracked as Pre-Release Gate)**
  - **DEF-001 Resolved & Verified**: Integrated `onnxruntime-android:1.20.0`, implemented `E5Tokenizer.kt` and `E5EmbeddingService.kt`. Verified 10/10 parity fixtures with 1.000000 cosine similarity and 0.000000 max coordinate difference. Connected Android test `test_007_real_android_onnx_inference_and_parity` passed on `Pixel_9(AVD) - 17`.
  - **DEF-002 Resolved & Verified**: Implemented deterministic, evidence-grounded extractive answer fallback in `OnlineAIRepository.kt` when the 1.0 GB GGUF model is not present. Verified citation preservation, UI excerpt banner, and strict refusal on missing evidence. Connected Android test `test_008_real_android_offline_extractive_fallback` passed on `Pixel_9(AVD) - 17`.
  - **DEF-003 Tracked as Pre-Release Gate**: Physical bare-metal low-RAM (< 4 GB) hardware testing documented in reports as an open gate prior to commercial distribution.
  - **Phase 10 Scenarios Reconciled**: All 15 scenarios in `docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md` are **15 / 15 PASS**.
  - **Final Audit Reports Published**:
    - `docs/reports/EDUNOVA_ARCHITECTURE_VERIFICATION_REPORT.md`
    - `docs/reports/EDUNOVA_RAG_EVALUATION_REPORT.md`

---

## 15-Scenario QA Verification Matrix & Results

| ID | Scenario Name | Environment / Tier | Expected Behavior | Actual Measured Outcome | Verdict |
|:---|:---|:---|:---|:---|:---:|
| **SCEN-01** | Online Known Syllabus Question | `LIVE_REMOTE_SUPABASE` | Retrieve authorized CBSE chunks, stream tokens, emit citations | Routed to RAG; retrieved CBSE framework chunks; 0 legacy NCERT leaks; streamed tokens | **PASS** |
| **SCEN-02** | Specific Source Passage Grounding | `LIVE_REMOTE_SUPABASE` | Exact chunk cited via SSE citations event; rendered in UI | Cited official passage ID; emitted `event: citations` before stream completion | **PASS** |
| **SCEN-03** | Cross-Curriculum Boundary Isolation | `LIVE_REMOTE_SUPABASE` / `LOCAL_UNIT` | Class 10 profile querying Class 12 Physics returns 0 Class 12 chunks | Strict SQL filter `WHERE class_grade='10'`; 0 leaks; `EvidenceOutcome.MISSING` | **PASS** |
| **SCEN-04** | Absent Question / Out-of-Syllabus | `LIVE_REMOTE_SUPABASE` / `LOCAL_UNIT` | Missing evidence query triggers explicit refusal without hallucination | Returns `MISSING`; generative LLM prompt is aborted; refuses politely | **PASS** |
| **SCEN-05** | General Mode Direct Route | `LIVE_REMOTE_SUPABASE` / `LOCAL_UNIT` | Non-curriculum query bypasses RAG; `RouteDecision.DIRECT` | Routed to DIRECT; Supabase corpus database NOT queried; answered directly | **PASS** |
| **SCEN-06** | RAG SSE Event Sequence Trace | `LOCAL_INTEGRATION` | Order: `metadata` -> `route` -> `citations` -> `token` -> `done` | SseStreamBuffer parsed 8/8 contract tests; strict ordering validated | **PASS** |
| **SCEN-07** | UI Citation Rendering Invariant | `LOCAL_UNIT` (Compose) | Citations rendered iff `message.citations.isNotEmpty()` | Verified in `AIWorkspaceScreen.kt`; offline/empty messages default to `emptyList()` | **PASS** |
| **SCEN-08** | Network Disconnect During Stream | `LIVE_REMOTE_SUPABASE` | Network drop triggers clean DB rollback and user retry message | Handled cleanly; DB session rolled back without leaking secrets | **PASS** |
| **SCEN-09** | Syllabus Pack Synchronization | `LIVE_REMOTE_SUPABASE` / `LOCAL_UNIT` | Downloads authorized CBSE framework packs; excludes legacy NCERT | 9 CBSE chunks synced; 210 legacy NCERT chunks strictly excluded | **PASS** |
| **SCEN-10** | Disconnected / Airplane Mode RAG | `ANDROID_EMULATOR` | Real query in Airplane Mode; local SQLite search; on-device answer | Local SQLite retrieved chunk; ONNX embedded query; GGUF absent triggers grounded extractive fallback with citations; unsupported query abstains | **PASS** |
| **SCEN-11** | Keyword Search Fallback | `LOCAL_UNIT` / `ANDROID_EMULATOR` | E5 model missing/downloading; falls back to SQLite `LIKE` search | Automatically fell back to `LIKE '%electric%'`; returned Ohm's law passage | **PASS** |
| **SCEN-12** | Offline Empty-Evidence Abstention | `LOCAL_UNIT` / `ANDROID_EMULATOR` | Local search finds 0 chunks; generative LLM NOT called; refusal emitted | Local `assessEvidenceOutcome` returned `MISSING`; LLM call aborted; refusal emitted | **PASS** |
| **SCEN-13** | SQLite Cold Restart Persistence | `ANDROID_EMULATOR` | Synced packs and vector BLOBs persist across app restart | Reopened `syllabus_database.db`; verified all tables, chunks, and BLOBs intact | **PASS** |
| **SCEN-14** | FTS5 Regression Avoidance Check | `ANDROID_EMULATOR` | Real Android SQLite executes LIKE search without FTS5 module crash | Executed on `Pixel_9(AVD) - 17`; standard LIKE query passed; zero FTS5 crash | **PASS** |
| **SCEN-15** | Corrupted Data & Interrupted Recovery | `ANDROID_EMULATOR` | Corrupted vector BLOBs skipped gracefully without app crash | Injected corrupted BLOB; hybridSearch skipped corrupted record; 0 crashes | **PASS** |

---

## Test Execution Records & Empirical Evidence

### 1. Connected Android Instrumented Suite on Real AVD (`Pixel_9(AVD) - 17`)
- Command:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.offline_First.OfflineModelAndParityInstrumentedTest"
  ```
- Target Device: `Pixel_9(AVD) - 17` (Android 17, API 37, 4,006,168 kB RAM, 16k page size)
- Exit Code: **0** (BUILD SUCCESSFUL)
- Results in `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_9(AVD) - 17.xml`:
  - `test_001_real_android_sqlite_schema_and_version`: **PASSED** (0.025s)
  - `test_002_real_android_sqlite_blob_vector_storage`: **PASSED** (0.115s)
  - `test_003_real_android_curriculum_isolation`: **PASSED** (0.189s)
  - `test_004_model_artifact_loading_and_integrity`: **PASSED** (2.345s)
  - `test_005_real_android_offline_abstention`: **PASSED** (0.022s)
  - `test_006_real_android_corrupted_blob_resilience`: **PASSED** (0.117s)
  - `test_007_real_android_onnx_inference_and_parity`: **PASSED** (2.456s) — *DEF-001 Real ONNX inference*
  - `test_008_real_android_offline_extractive_fallback`: **PASSED** (4.578s) — *DEF-002 Grounded extractive fallback*
  - `test_009_embedding_compatibility_invalidation_and_rebuild`: **PASSED** (4.146s) — *Embedding compatibility, legacy invalidation & safe rebuild*
- Total Instrumented Suite: **9 passed, 0 failures** on Android OS runtime.

### 2. Cross-Runtime Parity Suite (Python & JVM)
- Python Parity Command:
  ```powershell
  .\backend\.venv\Scripts\python.exe -m pytest backend/evals/parity/test_model_parity.py -v
  ```
  Result: **8 passed in 15.20s** (Exit code: 0).
- JVM Parity Test (`OrtTest.kt`):
  Evaluated across all 10 golden fixtures:
  - Cosine Similarity: **1.000000** for all fixtures
  - Max Coordinate Difference: **0.000000** for all fixtures
  - Inference Latency: **7 ms to 14 ms**

### 3. Android Host JVM Unit Suites
- Command:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
  ```
  Result: **BUILD SUCCESSFUL in 39s** (Exit code: 0).
  - `OfflineChatEndToEndTest`: 5 passed
  - `LocalEmbeddingCompatibilityTest`: 4 passed
  - `AndroidPerformanceBenchmarkTest`: 4 passed
  - `OfflineModelLifecycleTest`: 11 passed
  - `OfflineRagRetrievalTest`: 6 passed
  - `SyllabusDatabaseTest`: 6 passed

### 4. Unified Evaluation Runner Suite (`evals.rag.run_eval`)
- Command:
  ```powershell
  $env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m evals.rag.run_eval --suite all --mode mock --fail-on-regression
  ```
  Result: **All 5 suites PASSED in 47.3s** (Exit code: 0).
  - `retrieval` (MOCKED): Recall@5=1.000, Recall@10=1.000, MRR=1.000, nDCG=1.000, Leakage=0.0%, Abstention=100.0%
  - `answer` (MOCKED): Abstention=100.0%, Validity=100.0%, Coverage=80.0%, Fact Overlap=100.0%
  - `regression` (LOCAL_UNIT): 59/59 passed in 9.07s
  - `online-e2e` (LIVE_REMOTE_SUPABASE): 7/7 passed in 22.15s
  - `offline` (LOCAL_UNIT): Gradle unit tests PASSED

### 5. Targeted Backend Regression Suite
- Command:
  ```powershell
  .\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_routing_contract.py backend/tests/test_syllabus_authorization_gating.py backend/tests/test_curriculum_sync_e2e.py backend/tests/test_syllabus_sync.py backend/tests/test_phase4_retrieval_policy.py backend/tests/integration/test_sse_parser_contract.py backend/tests/integration/test_online_rag_e2e.py -v
  ```
  Result: **66 passed in 25.90s** (Exit code: 0).

---

## Adherence to Mandatory Constraints
- **Roadmap Isolation**: All Personalized Roadmap tests and pipelines were strictly excluded.
- **Database Safety**: Zero synthetic syllabus rows inserted into Supabase; zero production records deleted.
- **Git State**: Zero Git commits created; all existing working-tree modifications preserved.
- **Mock vs Live Distinctions**: Never reported mock results as live verification. Real emulator environment accurately identified as `Pixel_9(AVD) - 17` (API 37).
- **Physical Device Pre-Release Gate**: Physical bare-metal low-RAM testing remains explicitly tracked as an open pre-release gate.

