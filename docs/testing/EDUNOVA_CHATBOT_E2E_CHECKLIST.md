# EduNova Chatbot End-to-End Manual QA Checklist

> **Document Version**: 1.0  
> **Status**: Active QA Specification  
> **Target Subsystems**: Android Client (`app/`), FastAPI Backend (`backend/app/`), SQLite Local Storage, Supabase Remote Corpus  

---

## Executive Overview

This document specifies the 15 end-to-end verification scenarios for the EduNova AI Chatbot system. Each scenario defines strict preconditions, exact user actions, expected UI behavior, backend/local routing and retrieval traces, concrete observation points, pass/fail acceptance criteria, and defect logging templates.

### Verification Tier Taxonomy
- **`MOCKED`**: Deterministic simulation of pipeline components for unit/contract tests.
- **`LOCAL_UNIT`**: Local JVM/Robolectric or pytest execution on developer workstation.
- **`ANDROID_EMULATOR`**: Connected execution on real Android AVD runtime (e.g. `Pixel_9(AVD) - 17`, API 37).
- **`PHYSICAL_DEVICE`**: Execution on target physical low-RAM Android hardware (≤ 3 GB RAM).
- **`LIVE_REMOTE_SUPABASE`**: Real remote network requests against cloud Supabase PostgreSQL and OpenRouter LLM.

---

## 15-Scenario QA Verification Matrix

| ID | Scenario Name | Target Environment | Primary Verification Focus | Status |
|:---|:---|:---|:---|:---:|
| **SCEN-01** | Online Known Syllabus Question | Live Backend + Android UI | RAG route, authorized CBSE chunk retrieval, grounded streaming, UI rendering | **PASS** (Live Backend / Mock UI) |
| **SCEN-02** | Specific Source Passage Grounding | Live Backend + Android UI | Exact paragraph citation, chapter provenance, citation SSE event | **PASS** (Live Backend / Mock UI) |
| **SCEN-03** | Cross-Curriculum Boundary Isolation | Live Supabase / Android SQLite | Class 10 Science profile querying Class 12 Physics; zero cross-curriculum leakage | **PASS** (Verified Zero Leakage) |
| **SCEN-04** | Absent Question / Out-of-Syllabus | Live Backend / Android Local | Missing evidence query triggers `EvidenceOutcome.MISSING` without hallucination | **PASS** (Strict Refusal Enforced) |
| **SCEN-05** | General Mode Direct Route | Live Backend + Android UI | General question bypasses RAG; `RouteDecision.DIRECT`; zero syllabus DB queries | **PASS** (ROUTE-003 Contract Green) |
| **SCEN-06** | RAG SSE Event Sequence Trace | Live Backend + SSE Parser | Ordered event dispatch: `metadata` -> `route` -> `citations` -> `token` -> `done` | **PASS** (SSE Contract Verified) |
| **SCEN-07** | UI Citation Rendering Invariant | Android Compose UI | Citations displayed ONLY when `citations.isNotEmpty()`; no phantom offline citations | **PASS** (UI Logic Verified) |
| **SCEN-08** | Network Disconnect During Stream | Live Backend + Android Client | Network drop during streaming; database rollback; user-friendly retry message | **PASS** (Timeout Rollback Safe) |
| **SCEN-09** | Syllabus Pack Synchronization | Backend Sync API + SQLite | Authenticated profile downloads authorized CBSE pack; legacy NCERT excluded | **PASS** (Sync API Gated) |
| **SCEN-10** | Disconnected / Airplane Mode RAG | Android Local SQLite | Zero network query; local SQLite search; grounded on-device answer | **PARTIAL** (BLOCKED: On-device GGUF model absent) |
| **SCEN-11** | Keyword Search Fallback | Android Local SQLite | E5 model missing or downloading; automatic fallback to SQLite `LIKE` search | **PASS** (Keyword Fallback Verified) |
| **SCEN-12** | Offline Empty-Evidence Abstention | Android Local Repository | Local search finds no matching chunks; generative LLM is NOT called; explicit refusal | **PASS** (OFF-014 Abstention Verified) |
| **SCEN-13** | SQLite Persistence Across Cold Restart | Android OS Runtime | App terminated and restarted; synced chunks and packs remain queryable | **PASS** (Connected AVD Verified) |
| **SCEN-14** | FTS5 Regression Avoidance | Android Real SQLite Engine | Standard `LIKE` keyword search operates on real Android OS without FTS5 module crash | **PASS** (Connected AVD Verified) |
| **SCEN-15** | Corrupted Data & Interrupted Recovery | Android Local Runtime | Corrupted vector BLOBs or interrupted downloads handled gracefully without crashing | **PASS** (Connected AVD Verified) |

---

## Detailed Scenario Procedures

### Scenario 1: Online Known Syllabus Question
- **Objective**: Verify that an authentic, authorized CBSE Class 10 Science question retrieves authorized syllabus chunks and produces a grounded streaming response.
- **Preconditions**: User signed in with CBSE Class 10 Science profile. Internet connection active (`ConnectionMode.ONLINE`).
- **Input Query**: `"What happens when magnesium ribbon burns in oxygen?"`
- **Expected UI Behavior**:
  - Routing banner displays `"Curriculum Grounded"`.
  - Assistant response streams smoothly token by token.
  - Final message includes citation badge referencing official CBSE curriculum.
- **Expected Backend Behavior**:
  - `RoutingService` assesses query as Student Mode syllabus query (`RouteDecision.RAG`).
  - `SupabaseRetrievalService` retrieves authorized chunks (`cbseacademic.nic.in`).
  - Zero unauthorized legacy textbook chunks returned.
  - `EvidenceOutcome.SUFFICIENT` or high-confidence dense match.
- **Observation Points**:
  - Terminal logs: `POST /api/v1/chat/conversations/{id}/messages/stream` HTTP 200.
  - SSE Events: `{"type": "route", "decision": "RAG"}`, `{"type": "citations", ...}`.
- **Pass/Fail Criteria**:
  - PASS: Response is factually accurate, grounded in CBSE syllabus, and citations match retrieved chunks.
  - FAIL: General DIRECT route chosen, or unauthorized NCERT chapter chunks returned.

---

### Scenario 2: Specific Source Passage Grounding & Citation SSE
- **Objective**: Verify that a query demanding precise mathematical formulation or law statement cites the specific source chunk.
- **Preconditions**: CBSE Class 10 Science profile.
- **Input Query**: `"State Ohm's law and give its mathematical equation."`
- **Expected UI Behavior**:
  - Formulates statement: $V \propto I$ and $V = IR$.
  - Citation badge links to official document reference.
- **Expected Backend Behavior**:
  - Vector similarity + keyword corroboration matches Ohm's law passage.
  - SSE stream emits `event: citations` containing `chunk_id`, `document_title`, and `page_reference`.
- **Pass/Fail Criteria**:
  - PASS: Citation chunk ID matches ground-truth passage and is delivered prior to token stream completion.

---

### Scenario 3: Cross-Curriculum Boundary Isolation
- **Objective**: Ensure a Class 10 student cannot retrieve or view Class 12 or out-of-grade content.
- **Preconditions**: Profile configured as `board=CBSE`, `class_grade=10`, `subject=Science`.
- **Input Query**: `"Calculate the drift velocity of electrons using Fermi-Dirac distribution in gallium arsenide semiconductor diodes."`
- **Expected UI Behavior**:
  - System abstains with message: `"This topic is not found in your Grade 10 Science curriculum."`
  - Zero citations displayed.
- **Expected Backend Behavior**:
  - SQL query strictly enforces `WHERE board = 'CBSE' AND class_grade = '10'`.
  - Filter excludes Class 12 Physics chunks.
  - `EvidenceOutcome.MISSING` returned. Zero chunks sent to LLM prompt.
- **Pass/Fail Criteria**:
  - PASS: Zero chunks from Class 12 returned; explicit abstention displayed.
  - FAIL: Class 12 Physics chunks retrieved or synthesized.

---

### Scenario 4: Absent Question / Out-of-Syllabus Abstention
- **Objective**: Verify that advanced out-of-syllabus science queries abstain without hallucinating.
- **Preconditions**: Online or Offline Student Mode.
- **Input Query**: `"Explain CRISPR-Cas9 endonuclease guided RNA genome editing mechanisms in eukaryotic cells."`
- **Expected UI Behavior**:
  - Assistant politely declines: `"This topic was not found in the official curriculum corpus. Please verify the chapter or check your curriculum topics."`
  - No generative hallucination.
- **Expected Backend Behavior**:
  - Cosine distance cutoff and lexical corroboration fail to find sufficient evidence.
  - Generative model invocation is aborted.
- **Pass/Fail Criteria**:
  - PASS: System explicitly abstains; zero ungrounded generative assertions.

---

### Scenario 5: General Question Using DIRECT Route
- **Objective**: Verify that non-curriculum programming or general queries bypass RAG.
- **Preconditions**: General Mode enabled or non-student conversation.
- **Input Query**: `"Write a binary search function in Kotlin with example usage."`
- **Expected UI Behavior**:
  - Answers code question directly without syllabus citation badges.
- **Expected Backend Behavior**:
  - `RoutingService` chooses `RouteDecision.DIRECT`.
  - Supabase database is NOT queried for syllabus passages.
- **Pass/Fail Criteria**:
  - PASS: Response generated directly; zero RAG retrieval overhead.

---

### Scenario 6: RAG SSE Event Sequence Trace Contract
- **Objective**: Verify that SSE events adhere to strict client contract order.
- **Expected Event Order**:
  1. `event: metadata` (conversation ID, message ID)
  2. `event: route` (`{"decision": "RAG", "app_mode": "STUDENT"}`)
  3. `event: citations` (array of chunk citations)
  4. `event: token` (repeated for text chunks)
  5. `event: done` (`{"stop_reason": "completed"}`)
- **Pass/Fail Criteria**:
  - PASS: Events received in exact specified sequence; SSE parser does not drop citations.

---

### Scenario 7: Source / Citation Display in UI
- **Objective**: Verify UI rendering invariant: citations are rendered if and only if `message.citations.isNotEmpty()`.
- **Observation Point**: `app/src/main/java/com/offline_First/ui/screens/ai/AIWorkspaceScreen.kt`.
- **Pass/Fail Criteria**:
  - PASS: When citations are empty (e.g. offline answers or direct routes), no citation card or badge is rendered.

---

### Scenario 8: Network Failure During Online Request
- **Objective**: Verify graceful failure and secret-safe error reporting when connection drops mid-flight.
- **Preconditions**: Simulate connection drop or socket timeout during streaming.
- **Expected UI Behavior**:
  - Displays actionable error: `"Connection lost. Please check your internet connection or retry."`
  - Retry button is enabled.
  - Zero raw stack traces or database URLs exposed to user.
- **Expected Backend Behavior**:
  - Database transaction rolls back cleanly.
  - Structured secret-safe error logged.
- **Pass/Fail Criteria**:
  - PASS: App remains responsive; error message contains no secrets; DB session released.

---

### Scenario 9: Syllabus Pack Synchronization After Sign-In
- **Objective**: Verify downloading authorized curriculum packs to device SQLite.
- **Preconditions**: Authenticated student profile (`CBSE`, `10`, `Science`).
- **Action**: Invoke `/api/v1/syllabus/packs` and download pack chunks.
- **Expected Behavior**:
  - Manifest returns authorized CBSE framework pack (9 chunks).
  - 210 legacy NCERT textbook chunks are strictly excluded from downloadable packs.
  - Pack is stored into Android SQLite `syllabus_packs` and `syllabus_chunks`.
- **Pass/Fail Criteria**:
  - PASS: Only authorized chunks are synchronized to device SQLite.

---

### Scenario 10: Disconnected / Airplane Mode Query Execution
- **Objective**: Verify real query execution when network is completely disabled.
- **Preconditions**: Airplane Mode enabled (`ConnectionMode.OFFLINE`). Synced syllabus pack present in SQLite.
- **Input Query**: `"What is Ohm's law?"`
- **Expected Behavior**:
  - Local SQLite retrieval executed.
  - Top chunk retrieved from local database.
  - On-device generative model grounds answer using local context.
- **Current Execution Status**: **PARTIAL / BLOCKED**
  - Blocked because on-device GGUF generative model (`Qwen2.5-1.5B`) is not pre-installed on test device.

---

### Scenario 11: Keyword Search Fallback Without Embedding Model
- **Objective**: Verify search operates via SQL `LIKE` when E5 embedding model is not yet downloaded.
- **Preconditions**: `E5EmbeddingService.isModelReady() == false`. Synced chunks in SQLite.
- **Input Query**: `"electric current formula ampere"`
- **Expected Behavior**:
  - SQLite executes `SELECT * FROM syllabus_chunks WHERE content LIKE '%electric%' OR content LIKE '%current%'`.
  - Matching chunk returned without invoking ONNX runtime.
- **Pass/Fail Criteria**:
  - PASS: Search returns matching passage; zero crashes or unhandled exceptions.

---

### Scenario 12: Offline Empty-Evidence Abstention
- **Objective**: Verify that offline queries with no local syllabus evidence explicitly abstain.
- **Preconditions**: Disconnected / Offline Mode.
- **Input Query**: `"Explain quantum computing qubits and Shor's algorithm."`
- **Expected Behavior**:
  - Local retrieval returns 0 chunks (`EvidenceOutcome.MISSING`).
  - Generative model is NOT called.
  - Immediate offline refusal emitted.
- **Pass/Fail Criteria**:
  - PASS: Promptly returns curriculum missing message; zero hallucination.

---

### Scenario 13: App Cold Restart & Persistence
- **Objective**: Verify SQLite data persists across application process destruction and cold restart.
- **Preconditions**: Syllabus pack synced into SQLite.
- **Action**: Force stop application (`adb shell am force-stop com.offline_First`) and restart.
- **Expected Behavior**:
  - `SyllabusDatabase` reopens without data loss.
  - `hasPack("CBSE", "10", "Science")` returns `true`.
- **Pass/Fail Criteria**:
  - PASS: All chunks and vector BLOBs intact on cold reopen.

---

### Scenario 14: FTS5 Regression Avoidance Check
- **Objective**: Verify SQLite queries do NOT use FTS5 virtual tables to prevent Android `no such module: fts5` crash.
- **Preconditions**: Real Android OS runtime (`Medium_Phone(AVD) - 16` or `Pixel_9(AVD) - 17`).
- **Action**: Execute `SyllabusDatabase.searchChunks` and verify SQLite statement.
- **Expected Behavior**:
  - Query uses standard SQL `LIKE ?` clauses over standard B-tree index.
- **Pass/Fail Criteria**:
  - PASS: Zero `sqlite3_step: no such module: fts5` errors.

---

### Scenario 15: Recovery From Interrupted Sync or Corrupted Model Download
- **Objective**: Verify app recovers gracefully when a model download is interrupted or vector BLOB is corrupted.
- **Action**: Inject corrupted BLOB (random 50 bytes) into vector table; attempt search.
- **Expected Behavior**:
  - `SyllabusDatabase.hybridSearch` catches deserialization exception, skips corrupted record, and falls back to lexical ranking.
  - Zero application crashes.
- **Pass/Fail Criteria**:
  - PASS: Corrupted chunk gracefully skipped; zero fatal crashes.

---

## Defect Logging Template

```markdown
### Defect Report: [DEF-XX] Short Summary
- **Scenario**: SCEN-XX
- **Verification Tier**: [MOCKED | LOCAL_UNIT | ANDROID_EMULATOR | PHYSICAL_DEVICE | LIVE_REMOTE_SUPABASE]
- **Environment**: [OS / Device / API / App Version]
- **Preconditions**: ...
- **Steps to Reproduce**:
  1. ...
- **Expected Behavior**: ...
- **Actual Behavior**: ...
- **Log / Trace Evidence**:
- **Severity**: [Critical / Major / Minor]
- **Proposed Surgical Remediation**: ...
```
