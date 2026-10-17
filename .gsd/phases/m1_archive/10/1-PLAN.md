---
phase: 10
plan: 1
wave: 5
---

# Plan 10.1: Manual End-to-End Application Verification Checklist

## Objective
Author a comprehensive manual verification checklist (`docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md`) containing 15 exact, repeatable QA procedures across Android UI and FastAPI backend to correlate visible results with underlying routing decisions, retrieval traces, and source chunks.

## Context
- .gsd/SPEC.md
- docs/testing/
- app/src/main/java/com/offline_First/ui/screens/ai/
- backend/app/api/routes/chat.py
- backend/app/services/chat_service.py

## Tasks

<task type="auto">
  <name>Author Comprehensive Manual Verification Checklist</name>
  <files>docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md</files>
  <action>
    Create `docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md` detailing exact test protocols for all 15 required scenarios:
    1. Known question present in an official ingested chapter (e.g., "What is a chemical reaction?").
    2. Question requiring a specific source passage (e.g., balancing equations, Law of Conservation of Mass).
    3. Question from a different class/subject (e.g., Class 12 Physics query in Class 10 Science profile) to verify boundary isolation.
    4. Question absent from the ingested corpus (unanswerable syllabus query).
    5. General question using the DIRECT route (e.g., "Write a binary search in Kotlin").
    6. RAG question monitoring backend route/retrieval logs (`type: 'route'`, `type: 'citations'`).
    7. Source/citation display in UI.
    8. Network failure during an online request (verifying graceful error and retry).
    9. Syllabus pack synchronization after signing in with authenticated profile.
    10. Real query execution after enabling Airplane Mode.
    11. Query execution without downloaded embedding model (verifying keyword fallback).
    12. Query with no locally available syllabus evidence (verifying abstention).
    13. App cold restart after synchronization (verifying SQLite persistence).
    14. Local search FTS5 regression avoidance check (verifying LIKE search operates without SQLite crash).
    15. Recovery from interrupted sync or corrupted model download.
    For each scenario, specify: Preconditions, Exact Action, Expected UI Behavior, Expected Backend/Local Behavior, Evidence Observation Point, Pass/Fail Criteria, Defect Template.
  </action>
  <verify>Test-Path docs/testing/EDUNOVA_CHATBOT_E2E_CHECKLIST.md</verify>
  <done>Manual verification checklist written with all 15 scenarios thoroughly specified.</done>
</task>

## Success Criteria
- [ ] All 15 required verification scenarios documented.
- [ ] Every case links visible UI output to concrete log/SQL evidence.
- [ ] Clear pass/fail criteria established for human or automated QA.
