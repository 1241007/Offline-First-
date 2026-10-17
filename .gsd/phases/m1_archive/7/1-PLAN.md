---
phase: 7
plan: 1
wave: 4
---

# Plan 7.1: Curriculum-Aware Syllabus Synchronization Tests

## Objective
Author a rigorous synchronization verification suite testing both backend API manifest/pack endpoints and Android client sync logic to verify curriculum isolation, idempotency, incremental updates, and transaction integrity.

## Context
- .gsd/SPEC.md
- backend/app/api/routes/syllabus_sync.py
- backend/app/schemas/syllabus_sync.py
- app/src/main/java/com/offline_First/data/remote/SyllabusSyncClient.kt
- app/src/main/java/com/offline_First/data/repository/SyllabusSyncRepository.kt
- app/src/main/java/com/offline_First/data/local/SyllabusDatabase.kt

## Tasks

<task type="auto">
  <name>Author Backend Curriculum Sync Security & Integrity Suite</name>
  <files>backend/tests/test_curriculum_sync_e2e.py</files>
  <action>
    Create `backend/tests/test_curriculum_sync_e2e.py` verifying:
    - JWT authentication required on `/api/v1/syllabus/packs` and `/api/v1/syllabus/packs/{pack_id}`.
    - User profile education mode and curriculum restrictions strictly enforced (students only receive permitted packs).
    - Manifest metadata integrity: chunk count, content hashes, model ID (`intfloat/multilingual-e5-small`), embedding dimension (384).
    - Keyset / offset pagination works without duplicating or skipping chunks.
    - Stable chunk IDs across sync requests.
    - Missing profile fields default safely to zero packs rather than broadening query to entire corpus.
    - Unexpected pack ID requests return HTTP 403/404 without leaking other curricula.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe -m pytest backend/tests/test_curriculum_sync_e2e.py -v</verify>
  <done>All backend sync security, isolation, and pagination tests pass.</done>
</task>

<task type="auto">
  <name>Author Android Sync Flow and Transaction Integrity Tests</name>
  <files>app/src/test/java/com/offline_First/data/repository/SyllabusSyncIntegrationTest.kt</files>
  <action>
    Create `app/src/test/java/com/offline_First/data/repository/SyllabusSyncIntegrationTest.kt` verifying:
    - Initial cold sync: packs and chunks successfully written into local SQLite database.
    - No-change sync: server returning 304 / identical version avoids redundant inserts.
    - Incremental sync: only modified chunks updated in SQLite.
    - Sync interruption simulation: database transaction rollback ensures existing valid pack remains usable.
    - Network failure handling: offline access to previously synced packs remains intact.
  </action>
  <verify>.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.repository.SyllabusSyncIntegrationTest"</verify>
  <done>Android sync repository tests execute cleanly and confirm transaction safety.</done>
</task>

## Success Criteria
- [ ] Cross-curriculum pack leakage impossible under authenticated profile constraints.
- [ ] Incremental sync updates without duplicating or wiping local SQLite records.
- [ ] No production database rows mutated during test runs.
