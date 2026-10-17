---
phase: 1
plan: 3
wave: 2
depends_on: [1, 2]
---

# Plan 1.3: Prompt Assembly Context Isolation & Persistence Invariant Suite

## Objective
Author rigorous boundary tests for prompt construction and persistent storage, validating student profile isolation, context length enforcement, and SQLite/Postgres transactional integrity.

## Context
- .gsd/SPEC.md
- .gsd/ROADMAP.md
- docs/evaluation/EVALUATION_INVENTORY.md
- backend/app/services/chat_service.py
- backend/app/services/openrouter_service.py
- backend/app/repositories/chat_repository.py
- app/src/main/java/com/offline_First/data/local/ChatCacheDatabase.kt

## Tasks

<task type="auto">
  <name>Build Prompt Construction and Context Isolation Evaluator</name>
  <files>backend/tests/test_prompt_construction.py</files>
  <action>
    Create `backend/tests/test_prompt_construction.py` verifying:
    - Boundary testing: very long user queries, multi-turn conversation context exceeding `chat_context_messages` limit.
    - Strict isolation: asserting zero cross-student, cross-subject, or cross-board leakage in assembled prompt payloads.
    - System instruction integrity: validating that teacher, explainable, and general instruction templates are maintained without degradation.
    - Empty evidence prompt handling: verifying that empty retrieval results never yield a prompt instructing the model to guess or hallucinate.
  </action>
  <verify>$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_prompt_construction.py -v</verify>
  <done>Prompt construction evaluator passing across all boundary, isolation, and formatting cases.</done>
</task>

<task type="auto">
  <name>Build Persistence Invariant and Failure-Injection Test Suite</name>
  <files>backend/tests/test_chat_persistence_invariants.py, app/src/test/java/com/offline_First/data/local/ChatPersistenceInvariantTest.kt</files>
  <action>
    Create `test_chat_persistence_invariants.py` (backend) and `ChatPersistenceInvariantTest.kt` (Android) testing:
    - Transaction safety: message and conversation creation rollbacks cleanly on simulated database errors.
    - Cache consistency: offline messages persist with `pending_sync` status and preserve client message IDs across reconnects.
    - Resilience to corrupted SQLite cache records: graceful recovery without application crash or silent fallback to ungrounded generation.
  </action>
  <verify>$env:PYTHONPATH="backend"; .\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_chat_persistence_invariants.py -v; $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.local.ChatPersistenceInvariantTest"</verify>
  <done>Persistence failure paths and transactional invariants verified on backend and Android.</done>
</task>

## Success Criteria
- [ ] Prompt construction verified for strict curriculum isolation and token limit truncation.
- [ ] Persistence failure injection tests prove transactional rollback and corrupted record recovery.
- [ ] Zero ungrounded fallback on database error.
