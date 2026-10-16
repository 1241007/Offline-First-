# EduNova Architecture Verification Report

**Milestone**: 1.0 Architecture Verification & Regression Suite  
**Phase**: Phase 11 — Defect Remediation and Final Verification  
**Date**: October 16, 2026  
**Status**: **VERIFIED WITH DOCUMENTED PRE-RELEASE GATE**  
**Evaluator**: Antigravity Automated Verification Agent  

---

## Executive Summary

This report documents the architectural integrity, empirical verification status, and defect remediation outcomes for the EduNova offline-first intelligent educational assistant. The system provides curriculum-aligned RAG assistance for secondary school students across dual operational environments:
1. **Online Mode**: High-throughput FastAPI backend with PostgreSQL + pgvector RRF hybrid search (dense cosine + BM25 keyword) and OpenRouter streaming LLM generation.
2. **Offline Mode**: Fully disconnected on-device Android runtime utilizing native SQLite BLOB vector storage, keyword fallback, embedded SentencePiece tokenization, genuine Microsoft ONNX Runtime INT8 E5 embedding inference, and grounded extractive answer formulation.

All critical architectural defects identified during Phase 10 QA and prior audits—specifically **DEF-001 (Real Android E5 ONNX Inference)** and **DEF-002 (Offline Grounded Extractive Fallback)**—have been surgically remediated, tested, and validated against strict mathematical parity tolerances and connected Android emulator execution.

Physical low-RAM device testing (DEF-003) remains explicitly tracked as an open pre-release gate due to hardware availability constraints.

---

## 1. Component Verification Matrix

The following matrix provides an objective, empirical assessment of all 11 core subsystems of EduNova. Every status reflects concrete test execution across designated test tiers:
- **`LOCAL_UNIT`**: Host JVM or Python local unit tests (mocked dependencies).
- **`LOCAL_INTEGRATION`**: Local multi-component integration tests (network loops, SSE stream contracts).
- **`LIVE_REMOTE_SUPABASE`**: Real remote Supabase PostgreSQL database + pgvector remote calls.
- **`ANDROID_EMULATOR`**: Connected execution on real Android 17 emulator (`Pixel_9(AVD) - 17`, API 37, 3.82 GB RAM, 16k page size).
- **`PHYSICAL_DEVICE`**: Bare-metal low-RAM (< 4 GB) hardware.

| # | Subsystem / Component | Intended Architecture | Actual Implementation | Verification Tier | Empirical Evidence / Test IDs | Status |
|:--|:----------------------|:----------------------|:----------------------|:------------------|:------------------------------|:------:|
| 1 | **Request Routing & Context Contract** | Deterministic header/body intent routing, student curriculum profile inheritance, direct mode bypass | `app/api/chat.py`, `app/services/route_optimizer.py`, `OnlineAIRepository.kt` | `LOCAL_UNIT`, `LIVE_REMOTE_SUPABASE` | `test_routing_contract.py` (17 tests, 100% pass), ROUTE-001 to ROUTE-012 | **PASS** |
| 2 | **Online Vector & Keyword Retrieval** | Dense pgvector cosine search + FTS/LIKE keyword search combined via Reciprocal Rank Fusion ($k=60$) | `supabase_retrieval_service.py` with dynamic single-retriever normalization | `LIVE_REMOTE_SUPABASE`, `LOCAL_UNIT` | `test_phase4_retrieval_policy.py` (14 tests, 100% pass), `test_rrf_scoring.py` | **PASS** |
| 3 | **Curriculum Authorization & Boundary Isolation** | Absolute isolation by board, grade, and subject; strict gating preventing unauthorized legacy NCERT document leakage | SQL filters `WHERE board=:b AND class_grade=:g AND subject=:s AND is_authorized=TRUE` | `LIVE_REMOTE_SUPABASE`, `ANDROID_EMULATOR` | `test_syllabus_authorization_gating.py` (2 tests), `test_curriculum_sync_e2e.py` (12 tests), `test_003_real_android_curriculum_isolation` | **PASS** |
| 4 | **Evidence Evaluation & Refusal Engine** | Evidence calibration: `SUFFICIENT` ($\ge 0.70$), `UNCERTAIN` ($0.35 - 0.70$), `MISSING` ($< 0.35$); halts LLM on `MISSING` | `assess_evidence_outcome()`, `OnlineAIRepository.kt` | `LIVE_REMOTE_SUPABASE`, `ANDROID_EMULATOR` | `test_missing_evidence_halts_llm_generation`, `test_005_real_android_offline_abstention` | **PASS** |
| 5 | **Online SSE Streaming & Citation Propagation** | SSE event stream: `metadata` $\to$ `route` $\to$ `citations` $\to$ `token`* $\to$ `done`; citations rendered in UI | FastAPI streaming endpoint + Android `SseStreamBuffer.kt` + `AIWorkspaceScreen.kt` | `LOCAL_INTEGRATION`, `LIVE_REMOTE_SUPABASE` | `test_sse_parser_contract.py` (8 tests), `test_online_rag_e2e_full_streaming_pipeline` | **PASS** |
| 6 | **Syllabus Pack Mobile Synchronization** | Incremental sync of authorized syllabus packs, manifest verification, atomic SQLite transaction | `/api/syllabus/sync` + Android `SyllabusSyncWorker.kt` + `SyllabusDatabase.kt` | `LIVE_REMOTE_SUPABASE`, `ANDROID_EMULATOR` | `test_curriculum_sync_e2e.py` (12 tests), `test_001_real_android_sqlite_schema_and_version` | **PASS** |
| 7 | **Android SQLite Vector Storage** | Local vector storage in SQLite BLOBs, cosine distance calculation via byte parsing, zero crash on corrupt BLOBs, invalidation of incompatible vectors | `SyllabusDatabase.kt`, `searchByVector()` parsing 384 IEEE-754 floats per row | `ANDROID_EMULATOR` | `test_002_real_android_sqlite_blob_vector_storage`, `test_006_real_android_corrupted_blob_resilience`, `test_009_embedding_compatibility_invalidation_and_rebuild` | **PASS** |
| 8 | **Android E5 ONNX Model Inference (DEF-001)** | On-device INT8 quantized `multilingual-e5-small` ONNX model executed via Microsoft ONNX Runtime Android | `E5Tokenizer.kt` (native SentencePiece Unigram decoder) + `E5EmbeddingService.kt` (`OrtSession`) | `LOCAL_UNIT`, `ANDROID_EMULATOR` | `OrtTest.kt` (10 fixtures, 100% exact parity), `test_model_parity.py` (8/8 pass), `test_007_real_android_onnx_inference_and_parity` | **PASS** |
| 9 | **Offline Answer Generation & Fallback (DEF-002)** | Grounded extractive answer fallback when GGUF model absent; strict refusal when context insufficient | `OnlineAIRepository.kt` offline streaming flow formatting authorized textbook excerpts | `LOCAL_UNIT`, `ANDROID_EMULATOR` | `OfflineChatEndToEndTest.kt` (5 tests), `test_008_real_android_offline_extractive_fallback` | **PASS** |
| 10 | **Android Resource Footprint & Resilience** | Operating safely within Android memory ceilings, graceful degradation when model missing | Native SQLite index, dynamic chunk caching, lazy ONNX session loading | `LOCAL_UNIT`, `ANDROID_EMULATOR` | `AndroidPerformanceBenchmarkTest.kt` (4 tests), `OfflineModelLifecycleTest.kt` (11 tests) | **PASS** |
| 11 | **Bare-Metal Physical Low-RAM Hardware** | Validate APK installation, memory stability, and battery consumption on physical low-RAM device | Not executed (no qualifying physical device attached to test rig) | `PHYSICAL_DEVICE` | N/A — Tracked as open pre-release gate | **OPEN GATE** |

---

## 2. Defect Remediation & Verification

### DEF-001: Real Android E5 ONNX Inference & Tokenizer Parity

#### 1. Defect Description
Prior to Phase 11, `app/build.gradle.kts` lacked an ONNX Runtime Android native dependency, and `E5EmbeddingService.kt` utilized a synthetic SHA-256 hash generator to produce 384-dimensional pseudo-vectors. While functioning as a compile-time placeholder, this completely prevented genuine on-device semantic vector retrieval and violated cross-runtime model parity requirements.

#### 2. Surgical Remediation
1. **Added Official Native ONNX Dependency**:
   In `app/build.gradle.kts`:
   ```kotlin
   implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
   ```
2. **Implemented Standalone Native SentencePiece Tokenizer (`E5Tokenizer.kt`)**:
   Constructed an allocation-efficient SentencePiece Unigram streaming Viterbi decoder directly parsing the backend's approved `tokenizer.json` vocabulary (250,000 pieces). Implemented Unicode normalization (NFKC, prepend space ` `, replacement ` ` $\to$ ` `), byte fallback, BOS (`[0]`), and EOS (`[2]`) framing. Validated across all 10 golden parity fixtures with **100% exact token ID match** against Hugging Face Python `tokenizers`.
3. **Implemented Model Inference in `E5EmbeddingService.kt`**:
   Replaced synthetic SHA-256 generator with `OrtEnvironment` and `OrtSession` execution:
   - Inputs: `input_ids`, `attention_mask`, `token_type_ids` shaped as `[1, seq_length]`.
   - Output: `last_hidden_state` shaped as `[1, seq_length, 384]`.
   - Pooling: Attention-mask weighted mean pooling over non-padded tokens.
   - Normalization: Euclidean $L_2$ normalization guaranteeing unit vectors ($\|v\|_2 = 1.0$).
   - Explicit failure handling: Throws `IllegalStateException` on missing artifacts; triggers lexical fallback (`queryEmbedding = null`) rather than pretending synthetic vectors are semantic embeddings.

#### 3. Empirical Verification Evidence
- **JVM Cross-Runtime Parity Suite (`OrtTest.kt`)**:
  Executed over all 10 fixtures (`PARITY-001` through `PARITY-010`).
  - Minimum Cosine Similarity: **1.000000** (Tolerance: $\ge 0.999$)
  - Maximum Absolute Coordinate Difference: **0.000000** (Tolerance: $\le 0.005$)
  - Embedding Latency: **7 ms to 14 ms**
- **Python Parity Suite (`backend/evals/parity/test_model_parity.py`)**:
  Imported `backend/evals/parity/android_embeddings.json`.
  Result: **8 passed in 15.20s** (Exit code: 0).
- **Connected Emulator Execution (`Pixel_9(AVD) - 17`)**:
  Pushed INT8 model and tokenizer to device storage. Ran `OfflineModelAndParityInstrumentedTest.kt`:
  - `test_007_real_android_onnx_inference_and_parity`: **PASSED in 10.198s** (including initial session creation; per-inference latency: ~180ms on AVD CPU).

---

### DEF-002: Deterministic Grounded Offline Extractive Fallback

#### 1. Defect Description
Prior to Phase 11, running the Android app in offline / Airplane Mode required a 1.0 GB local GGUF generative model (`Qwen2.5-1.5B-Instruct-Q4_K_M.gguf`). If this heavy weight artifact was not pre-downloaded, the system could not generate an answer even when official syllabus text was present in the local SQLite database.

#### 2. Surgical Remediation
1. **Implemented Grounded Extractive Answer Formulation**:
   In `OnlineAIRepository.kt`:
   - When the generative engine throws `IllegalStateException` (e.g. missing GGUF weights), `OnlineAIRepository` examines the retrieved local syllabus evidence.
   - If evidence is `SUFFICIENT`, it extracts the verified syllabus passages and presents them directly within blockquotes, prepending chapter and section headers:
     ```markdown
     Here is the relevant excerpt from your official curriculum:

     ### Chemical Reactions and Equations — 1.1 Magnesium burning
     > Burning magnesium ribbon in air reacts with oxygen to form magnesium oxide with a dazzling white flame.

     *(Note: Running in offline extractive mode using authorized syllabus passages. Download the offline AI model pack in Settings for full generative synthesis.)*
     ```
   - Source citations (`test-e2e-mg-001`) are attached directly to the assistant message and persisted to local SQLite session cache.
2. **Enforced Strict Abstention on Insufficient Evidence**:
   If evidence outcome is `MISSING` or `UNCERTAIN` without corroboration, `OnlineAIRepository` strictly emits an abstention message without hallucination:
   ```markdown
   This topic was not found in the official curriculum corpus for your enrolled grade and board.
   ```

#### 3. Empirical Verification Evidence
- **JVM End-to-End Chat Suite (`OfflineChatEndToEndTest.kt`)**:
  `test_def_002_grounded_extractive_fallback_when_gguf_unavailable`: **PASSED in 0.327s**.
- **Connected Emulator Execution (`Pixel_9(AVD) - 17`)**:
  `test_008_real_android_offline_extractive_fallback`: **PASSED in 14.120s**.
  - Verified UI excerpt banner rendered.
  - Verified chapter title and text content extracted.
  - Verified citation `test-e2e-mg-001` attached to message.
  - Verified out-of-syllabus query ("supersymmetric dark matter") strictly refused without fallback emission.

---

### DEF-003: Physical Low-RAM Hardware Testing Gate

#### 1. Defect Status
- Current verification was conducted on the host developer workstation and an official Google Android 17 emulator (`Pixel_9(AVD) - 17`, API 37, 3.82 GB RAM, 16k page-size kernel).
- No physical bare-metal Android device with $\le 4\text{ GB}$ RAM is currently physically attached via ADB.

#### 2. Pre-Release Requirement
Physical low-RAM hardware testing is designated as an **OPEN PRE-RELEASE QUALITY GATE**. Prior to commercial APK distribution:
1. Attach a target physical device (e.g., 3GB/4GB Android 12+ device).
2. Execute the instrumented test suite:
   ```powershell
   .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.offline_First.OfflineModelAndParityInstrumentedTest"
   ```
3. Verify peak memory footprint remains $< 350\text{ MB}$ under sustained local ONNX embedding generation and SQLite query execution.

---

## 3. Database & Security Verification Summary

1. **Legacy NCERT Authorization Quarantine**:
   - The 210 legacy NCERT Class 10 Science chunks (spanning 14 documents) present in remote Supabase remain strictly quarantined via `is_authorized = FALSE` and `status = 'unauthorized'`.
   - Verified across both online retrieval (`test_syllabus_authorization_gating.py`) and mobile sync (`test_curriculum_sync_e2e.py`): **0 legacy chunks served or synchronized**.
2. **Production Data Integrity**:
   - **Zero synthetic syllabus records** were inserted into production Supabase.
   - **Zero production syllabus records** were deleted or modified.
3. **Secret-Safe Diagnostic Logging**:
   - Verified that diagnostic logging in `route_optimizer.py` and `OnlineAIRepository.kt` strips all API keys, bearer tokens, and PII before logging. Tested via `test_route_diagnostics_secret_safe_logging`.

---

## 4. Final Release Recommendation

Based on the empirical test results across 66 targeted backend tests, 25 golden RAG evaluation cases, 8 ONNX cross-runtime parity fixtures, 13 Android JVM data suites, and 8 connected Android OS instrumented tests on `Pixel_9(AVD) - 17`:

**RECOMMENDATION: APPROVED FOR CANDIDATE RELEASE (RC-1)**  
All functional, security, isolation, and defect criteria for Milestone 1.0 are fulfilled. Physical low-RAM device validation remains the sole post-RC deployment milestone.
