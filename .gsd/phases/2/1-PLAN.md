---
phase: 2
plan: 1
wave: 1
---

# Plan 2.1: Verify Routing Contract and Request Orchestration

## Objective
Author a comprehensive, deterministic routing test suite (`backend/tests/test_routing_contract.py`) to verify that user queries, profile metadata, timeouts, and fallback policies execute strictly according to architectural specifications without leaking credentials or ungrounded hallucinations.

## Context
- .gsd/SPEC.md
- backend/app/services/routing_service.py
- backend/app/services/chat_service.py
- backend/app/schemas/rag.py
- backend/app/api/routes/chat.py

## Tasks

<task type="auto">
  <name>Implement Routing Contract Test Suite (ROUTE-001 to ROUTE-012)</name>
  <files>backend/tests/test_routing_contract.py</files>
  <action>
    Create `backend/tests/test_routing_contract.py` covering:
    - ROUTE-001: Clear curriculum question selects RAG route.
    - ROUTE-002: General coding / arithmetic query selects DIRECT route.
    - ROUTE-003: Student mode propagates board, class, subject context.
    - ROUTE-004: Missing required curriculum profile prevents unconstrained broad search.
    - ROUTE-005: Chapter-specific query retains selected chapter in retrieval context.
    - ROUTE-006: Routing classifier timeout triggers deterministic fallback to RAG in student mode.
    - ROUTE-007: Malformed or invalid route output handled safely.
    - ROUTE-008: RAG route invokes retrieval service with correct parameters.
    - ROUTE-009: Empty RAG evidence produces explicit insufficiency message rather than silent ungrounded generation.
    - ROUTE-010: General mode returns CORPUS_NOT_CONFIGURED with clear guidance.
    - ROUTE-011: Unauthenticated request to protected chat endpoints returns HTTP 401.
    - ROUTE-012: Request context cannot override trusted curriculum restrictions from verified user profile.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_routing_contract.py -v</verify>
  <done>All 12 routing contract tests execute and pass with zero failures.</done>
</task>

<task type="auto">
  <name>Implement Structured Secret-Safe Routing Diagnostics</name>
  <files>backend/app/services/routing_service.py</files>
  <action>
    Enhance `routing_service.py` to record structured diagnostics for each route decision:
    - Route chosen (DIRECT vs RAG)
    - Decision rationale (deterministic rule, LLM classification, timeout fallback)
    - Standalone query extracted
    - Context parameters passed
    Ensure strict redaction so no auth tokens, passwords, or PII are logged.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_routing_contract.py -k "test_route_diagnostics" -v</verify>
  <done>Routing diagnostics verifiable in test assertions without exposing sensitive values.</done>
</task>

## Success Criteria
- [ ] All ROUTE-001 to ROUTE-012 test cases implemented in `backend/tests/test_routing_contract.py`.
- [ ] No test mocks the database and then merely asserts the mock was called (actual behavior tested).
- [ ] Diagnostic logging is structured and secret-safe.
