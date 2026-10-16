# Workspace Agent Skills & Personas (`.agents/`)

This directory contains workspace customizations, specialized skills, and agent persona definitions that enhance AI agent reasoning and task execution capabilities within this repository.

---

## Directory Overview

```
.agents/
├── README.md                   # Workspace agent definitions overview
├── agents/                     # Persona definitions (Architect, Engineer, Auditor, etc.)
└── skills/                     # Specialized on-demand skill instructions
    ├── codebase-mapper/        # Analyzes codebase architecture and dependencies
    ├── context-compressor/     # Optimizes token usage and context efficiency
    ├── context-fetch/          # Search-first file discovery to reduce token bloat
    ├── debugger/               # Systematic root-cause debugging protocol
    ├── empirical-validation/   # Verifiable evidence standard (no "trust me, it works")
    ├── executor/               # GSD plan execution with atomic checkpoints
    ├── plan-checker/           # Validates execution plans before execution
    ├── planner/                # Breaks down complex requirements into phases
    ├── subagent-delegation/    # Spawns and manages subagents for parallel tasks
    ├── token-budget/           # Token budget tracking and context degradation prevention
    └── verifier/               # Validates completed features against acceptance criteria
```

---

## Loading & Invocation

Skills are automatically discovered and loaded when an agent encounters tasks matching their specialized capabilities or when explicitly invoked. Each skill contains a `SKILL.md` instruction file with YAML frontmatter.
