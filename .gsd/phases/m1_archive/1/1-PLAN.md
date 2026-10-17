---
phase: 1
plan: 1
wave: 1
---

# Plan 1.1: Establish Actual System State & Real Request Path Inventory

## Objective
Establish an empirical, secret-safe inventory of the entire system (Git, configuration, database schema, real Supabase pgvector corpus contents, and request paths) to prove or disprove architectural assumptions before writing test or remediation code.

## Context
- .gsd/SPEC.md
- PROJECT_RULES.md
- backend/app/core/config.py
- backend/app/services/supabase_retrieval_service.py
- backend/scripts/ingest_syllabus.py
- app/src/main/java/com/offline_First/data/remote/OnlineAIRepository.kt
- app/src/main/java/com/offline_First/data/local/OfflineSyllabusRepository.kt

## Tasks

<task type="auto">
  <name>Audit Database Corpus and Model Artifact Metadata</name>
  <files>backend/scripts/audit_corpus_inventory.py</files>
  <action>
    Create a standalone, read-only audit script `backend/scripts/audit_corpus_inventory.py` that queries Supabase PostgreSQL directly to measure:
    - Exact document count in `syllabus_documents`
    - Exact chunk count in `syllabus_chunks`
    - Null or invalid embeddings count
    - Distinct vector dimensions via `vector_dims(embedding)`
    - Model and version distribution
    - Duplicate content hashes
    - Official source provenance URLs and metadata completeness
    Verify that output contains no `.env` credentials, API keys, or raw textbook dumps.
  </action>
  <verify>.\backend\.venv\Scripts\python.exe backend/scripts/audit_corpus_inventory.py</verify>
  <done>Script executes cleanly and returns structured JSON proving exact database corpus counts and 384-dimensional pgvector health.</done>
</task>

<task type="auto">
  <name>Trace and Document Online and Offline Request Pathways</name>
  <files>docs/architecture/EDUNOVA_REQUEST_FLOW_AUDIT.md</files>
  <action>
    Document the step-by-step runtime request path for both Online and Offline modes based on the active codebase.
    Identify and flag all architectural disconnects, including:
    - Android `OnlineGeminiProvider` dropping `citations` SSE events
    - Android `OnlineAIRepository` bypassing `OfflineSyllabusRepository` and `E5EmbeddingService`
    - Silent ungrounded generation when offline evidence is empty
  </action>
  <verify>Test-Path docs/architecture/EDUNOVA_REQUEST_FLOW_AUDIT.md</verify>
  <done>Comprehensive request flow audit document created with source code references and confirmed gaps.</done>
</task>

## Success Criteria
- [ ] Database corpus proven with live SQL results (14 docs, 219 chunks, 0 nulls, 384-dim).
- [ ] Working-tree changes preserved intact without resetting or reverting.
- [ ] Disconnected runtime components documented with file and line references.
