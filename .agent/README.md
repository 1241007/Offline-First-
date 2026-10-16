# Agent Workflows (`.agent/`)

This directory houses executable markdown-based development workflows that provide step-by-step guides for planning, executing, verifying, and debugging work within the Antigravity IDE and agent ecosystem.

---

## Directory Overview

```
.agent/
├── README.md                   # Workflows index and guide
└── workflows/                  # Slash-command executable workflow guides
    ├── execute.md              # The Engineer: executes specific phases with focused context
    ├── plan.md                 # The Strategist: decomposes requirements into phases
    ├── verify.md               # The Auditor: validates work against specifications
    ├── debug.md                # Systematic debugging with persistent state
    ├── map.md                  # The Architect: updates ARCHITECTURE.md and STACK.md
    ├── progress.md             # Shows current position in roadmap and next actions
    ├── pause.md & resume.md    # Clean context handoffs between sessions
    └── ...                     # Additional workflow guides (add-phase, audit, sprint, etc.)
```

---

## Invocation

These workflows correspond to chat slash commands (e.g., `/execute`, `/plan`, `/verify`, `/debug`, `/progress`). Users and agents can invoke them directly in conversation to trigger structured, repeatable engineering protocols.
