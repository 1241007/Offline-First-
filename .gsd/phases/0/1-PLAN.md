---
phase: 0
plan: 1
wave: 1
---

# Plan 0.1: Repository Audit and Evaluation Inventory

## Objective
Establish a complete empirical audit of chatbot components across backend and Android, recording real test executions, gap analysis, and prioritized implementation plan for the layered evaluation framework.

## Context
- .gsd/SPEC.md
- .gsd/ROADMAP.md
- docs/evaluation/EVALUATION_INVENTORY.md
- backend/app/services/
- app/src/main/java/com/offline_First/data/

## Tasks

<task type="auto">
  <name>Audit System State and Active Tests</name>
  <files>docs/evaluation/EVALUATION_INVENTORY.md</files>
  <action>
    Inspect all existing chatbot components across backend and Android:
    - RAG hybrid retrieval and evidence evaluation
    - Online LLM generation and streaming
    - Offline on-device generation and llama.cpp runtime
    - Router and request orchestration
    - Prompt construction and context assembly
    - Streaming, citations, and response handling
    - Persistence and offline data access
    - Model parity (Python ONNX vs Android ONNX)
    Execute existing test suites and record empirical results.
    Document real source files, test locations, current metrics, verified evidence, coverage gaps, and planned actions.
  </action>
  <verify>Test-Path docs/evaluation/EVALUATION_INVENTORY.md</verify>
  <done>EVALUATION_INVENTORY.md created with complete component matrix, verified test outputs, gap analysis, and prioritized roadmap.</done>
</task>

## Success Criteria
- [x] Evaluation inventory authored with all required fields.
- [x] Pytest suites executed and verified (36 passed).
- [x] Parity suite executed and verified (7 passed, 1 skipped).
- [x] Android unit tests executed and verified (180 passed).
- [x] Gap analysis clearly separating unit correctness from real integration correctness.
