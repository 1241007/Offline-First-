---
phase: 6
plan: 1
wave: 3
---

# Plan 6.1: Offline RAG Evaluation and Cross-Runtime Model Parity

## Objective
Implement dedicated Android unit and integration tests covering the offline RAG pathway (OFF-001 through OFF-015) and create a cross-runtime embedding parity fixture to empirically evaluate vector parity between Python and Android.

## Context
- .gsd/SPEC.md
- app/src/main/java/com/offline_First/data/local/E5EmbeddingService.kt
- app/src/main/java/com/offline_First/data/local/OfflineSyllabusRepository.kt
- app/src/main/java/com/offline_First/data/local/SyllabusDatabase.kt
- app/src/main/java/com/offline_First/data/remote/OnlineAIRepository.kt
- backend/scripts/verify_inference_parity.py

## Tasks

<task type="auto">
  <name>Author Android Offline RAG Test Suite (OFF-001 to OFF-015)</name>
  <files>app/src/test/java/com/offline_First/data/local/OfflineRagRetrievalTest.kt,app/src/test/java/com/offline_First/data/local/LocalEmbeddingCompatibilityTest.kt,app/src/test/java/com/offline_First/data/local/OfflineChatEndToEndTest.kt</files>
  <action>
    Create Android unit test files in `app/src/test/java/com/offline_First/data/local/`:
    - `LocalEmbeddingCompatibilityTest.kt`: Tests OFF-001, OFF-002, OFF-006, OFF-011 (dimensions=384, prefix handling, checksum verification, version checks).
    - `OfflineRagRetrievalTest.kt`: Tests OFF-003, OFF-005, OFF-008, OFF-009, OFF-012, OFF-013 (keyword LIKE search, hybrid vector search, curriculum filtering, FTS5 regression avoidance).
    - `OfflineChatEndToEndTest.kt`: Tests OFF-004, OFF-007, OFF-014, OFF-015 (end-to-end disconnected execution, prompt injection, abstention on empty evidence, safe fallback on low RAM).
  </action>
  <verify>.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.*"</verify>
  <done>Android offline test suite compiles and runs via Gradle test runner.</done>
</task>

<task type="auto">
  <name>Implement Cross-Runtime Embedding Parity Fixture</name>
  <files>backend/evals/parity/test_model_parity.py,backend/evals/parity/parity_fixtures.json</files>
  <action>
    Create a fixed evaluation fixture `backend/evals/parity/parity_fixtures.json` with 10 representative educational queries and passages.
    Implement `test_model_parity.py` to:
    - Compare SHA-256 hashes of `model_quantized.onnx` and `tokenizer.json` across backend and Android configurations.
    - Measure vector dimensions (384) and L2 norm (1.0).
    - Compute cosine similarity across corresponding test vectors.
    - Document numerical tolerance bounds and any tokenization variances.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/evals/parity/test_model_parity.py -v</verify>
  <done>Model parity suite validates artifact integrity and numerical stability.</done>
</task>

## Success Criteria
- [ ] OFF-001 to OFF-015 test scenarios represented in Android test suite.
- [ ] Parity suite measures actual numerical vectors without simulated noise.
- [ ] No claim of parity made without empirical comparison against the artifact.
