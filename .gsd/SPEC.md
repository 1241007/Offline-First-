# SPEC.md — EduNova End-to-End Chatbot Verification & Evaluation Suite

> **Status**: `FINALIZED`
>
> ⚠️ **Planning Lock**: Requirements and verification scope locked for implementation.

## Vision

EduNova is an offline-first educational platform and AI tutor designed for Android mobile devices with a FastAPI cloud backend. The mission of this specification is to rigorously prove or disprove whether the actual application genuinely adheres to its intended architecture—from the moment a student submits a query, through routing, retrieval, evidence evaluation, answer generation, SSE streaming, and client-side rendering.

## Core Architectural Guarantees to Verify

1. **Routing and Request Orchestration**:
   - Queries must be intelligently routed: deterministic fast-path for conversational/command inputs, RAG route for curriculum and student queries, DIRECT route for general knowledge/coding when policy permits.
   - Fallback on router timeout or malformed output must deterministically default to RAG for student mode to prevent silent ungrounded halluncination.
   - Profile context (board, class, subject) must propagate securely and prevent cross-curriculum leakage.
   - Empty RAG evidence must NEVER silently fall back to ungrounded generation.

2. **Durable RAG Evaluation Framework**:
   - Version-controlled evaluation suite under `backend/evals/rag/`.
   - Source-grounded golden dataset (`golden_set.jsonl`) derived strictly from verified official NCERT/CBSE curriculum materials.
   - Deterministic retrieval metrics (Recall@5, Recall@10, MRR@10, nDCG@10, curriculum isolation, cross-curriculum leakage, latency).
   - Answer quality & groundedness evaluation (factual correctness, citation accuracy, citation coverage, hallucination rate, abstention correctness).

3. **Empirical Retrieval Threshold Validation**:
   - Empirical evaluation of RRF formula `raw_rrf = sum(1 / (k + rank))` and score normalization.
   - Verification that thresholds (`SUFFICIENT >= 0.70`, `UNCERTAIN >= 0.35`, `MISSING < 0.35`) are mathematically reachable and sound for single-retriever (dense-only or sparse-only) matches.

4. **Online End-to-End Pipeline**:
   - Authenticated student requests reach FastAPI, generate 384-dimensional ONNX/E5 embeddings, query pgvector with curriculum filtering, assess evidence, inject grounded context into LLM, and stream SSE events (`metadata`, `route`, `citations`, `token`, `done`).

5. **Offline RAG & Edge Ingestion**:
   - Android client embeds queries locally using 384-dimensional quantized E5 Small ONNX artifact.
   - Local SQLite hybrid search (LIKE keyword + stored embedding cosine similarity).
   - Local evidence evaluation and on-device LLM context injection without network connectivity.
   - Parity verification between Python and Android ONNX inference runtimes.

6. **Curriculum-Aware Synchronization**:
   - Manifest endpoints enforce JWT and user profile restrictions.
   - Android downloads, stores, and incrementally synchronizes curriculum packs safely without data corruption.

7. **Resource & Low-RAM Constraints**:
   - Quantized model (~113 MB ONNX + tokenizer) validated against SHA-256.
   - Memory footprint, cold-start initialization latency, and inference execution measured on Android thread pool.

## Non-Goals (Out of Scope)

- Fabricating synthetic textbook content or mock syllabus records in production database.
- Modifying production evidence thresholds arbitrarily without case-level evidence.
- Relying purely on mock tests or green APK builds as proof of architectural compliance.
- Unrequested git commits.

## Success Criteria

- [ ] Complete system state inventory recorded (Git, branch, uncommitted diff, schema, real database corpus audit).
- [ ] Comprehensive routing contract test suite passing across all 12 ROUTE test cases.
- [ ] Durable RAG evaluation suite implemented with verified NCERT source-grounded golden set.
- [ ] Retrieval threshold analysis completed and documented with empirical trade-offs.
- [ ] Online end-to-end integration test suite verifying the live FastAPI + pgvector + LLM streaming pipeline.
- [ ] Offline Android evaluation suite covering local embedding, search, sync, and parity.
- [ ] Unified evaluation CLI runner (`evals.rag.run_eval`) operational.
- [ ] Manual E2E checklist (`docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md`) completed with 15 test scenarios.
- [ ] Confirmed architectural defects surgically fixed with regression tests.
