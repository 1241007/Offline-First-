# Get Shit Done (GSD) Execution Framework (`.gsd/`)

This directory contains the milestone tracking, phase specifications, state snapshots, and execution plans for the **Get Shit Done (GSD)** development framework.

---

## Directory Overview

```
.gsd/
├── README.md                   # This overview of the GSD planning & execution framework
├── ROADMAP.md                  # Milestone and phase roadmap breakdown
├── SPEC.md                     # Technical requirements and core engineering specifications
├── STATE.md                    # Live session state, active tasks, blockers, and decisions
├── phases/                     # Granular phase execution folders
│   ├── 1/ to 11/               # Execution steps, tasks, and empirical verification notes
├── templates/                  # Templates for plans, summaries, and verification reports
└── examples/                   # Canonical reference examples of completed phases
```

---

## Core Artifacts

1. **[`ROADMAP.md`](file:///e:/Offline%20First/.gsd/ROADMAP.md)**:
   - High-level project milestones (Authentication, Local Inference, Cloud Hybrid RAG, Syllabus Corpus, Evaluation Suite).
   - Decomposes work into sequential, testable phases.

2. **[`SPEC.md`](file:///e:/Offline%20First/.gsd/SPEC.md)**:
   - Functional requirements, architectural boundaries, and performance benchmarks.

3. **[`STATE.md`](file:///e:/Offline%20First/.gsd/STATE.md)**:
   - Living snapshot of the current session: active phase, completed tasks, open questions, and next steps for zero-friction agent handoffs.

4. **[`phases/`](file:///e:/Offline%20First/.gsd/phases/)**:
   - Each phase directory contains the specific plan, task checklist, and empirical verification log for that phase.

---

## Operating Principles

- **Empirical Validation**: Work is not marked complete based on assumptions; every phase requires verifiable evidence (e.g., test execution outputs, audit logs, or metric reports).
- **Goal-Backward Execution**: Requirements drive test fixtures; test fixtures drive implementation.
- **Context Hygiene**: State is dumped into `STATE.md` to prevent context degradation across sessions.
