---
phase: 5
plan: 1
wave: 3
---

# Plan 5.1: Online End-to-End RAG Integration and Streaming Test Suite

## Objective
Author a live integration test suite (`backend/tests/integration/test_online_rag_e2e.py`) to verify the end-to-end cloud pipeline: authenticated request -> routing -> 384-dim E5 embedding -> Supabase pgvector retrieval -> evidence assessment -> prompt grounding -> SSE streaming.

## Context
- .gsd/SPEC.md
- backend/app/main.py
- backend/app/api/routes/chat.py
- backend/app/services/chat_service.py
- backend/app/services/supabase_retrieval_service.py
- backend/app/core/security.py

## Tasks

<task type="auto">
  <name>Implement Online RAG E2E Integration Suite</name>
  <files>backend/tests/integration/test_online_rag_e2e.py</files>
  <action>
    Create `backend/tests/integration/test_online_rag_e2e.py` that verifies:
    1. Authenticated chat streaming request reaches `POST /api/v1/chat/conversations/{id}/messages/stream`.
    2. Route decision event (`type: 'route'`) emits `RAG` for verified CBSE Class 10 Science question.
    3. Query embedding is generated with 384 dimensions and prepended with `"query: "`.
    4. Database query executes against `syllabus_chunks` with `board="CBSE"`, `class_grade="10"`, `subject="Science"`.
    5. Relevant chunks from ingested NCERT chapters are retrieved.
    6. Citations event (`type: 'citations'`) emits correct chunk IDs, titles, chapters, and scores.
    7. Insufficient evidence query (out-of-syllabus topic) returns `type: 'token'` abstention rather than ungrounded hallucination.
    8. Token stream terminates with valid `type: 'done'` event.
    9. Error handling: simulates database timeout and invalid credentials without exposing secrets in SSE responses.
    Keep live LLM calls strictly controlled using targeted fixtures or live provider flags.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/integration/test_online_rag_e2e.py -v</verify>
  <done>All integration scenarios pass against the live local FastAPI test transport and live database.</done>
</task>

<task type="auto">
  <name>Validate SSE Event Parser and Client Compatibility</name>
  <files>backend/tests/integration/test_sse_parser_contract.py</files>
  <action>
    Create `backend/tests/integration/test_sse_parser_contract.py` validating that the SSE payload emitted by FastAPI strictly adheres to the schema expected by Android's `ChatApiClient` and `StreamEventDto`:
    - Handles chunked HTTP transfer encoding.
    - Handles split `data: ` headers across network buffer boundaries.
    - Verifies JSON structure for `metadata`, `route`, `citations`, `token`, `error`, and `done`.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/integration/test_sse_parser_contract.py -v</verify>
  <done>SSE contract validated against buffer fragmentation and schema expectations.</done>
</task>

## Success Criteria
- [ ] End-to-end streaming verified from request initiation to terminal done event.
- [ ] Zero fabricated test chunks written to production database (read-only verification).
- [ ] Error scenarios proved not to leak secret credentials in error payloads.
