# Feature Specifications (`.kiro/`)

This directory contains functional specifications, architectural requirements, and task checklists for key engineering initiatives.

---

## Directory Overview

```
.kiro/
├── README.md                   # Specifications directory overview
└── specs/
    ├── auth-end-to-end/        # End-to-end authentication specification
    │   ├── spec.md             # JWT rotation, replay detection, Android Keystore requirements
    │   └── tasks.md            # Actionable task list and status tracking
    └── backend-connectivity-and-model-download-fix/
        ├── spec.md             # Render cold start, DNS resolution, and HF download resilience
        └── tasks.md            # Detailed implementation tasks
```

---

## Usage

When designing or implementing major features, create a dedicated folder under `.kiro/specs/` containing `spec.md` (architecture, acceptance criteria) and `tasks.md` (granular implementation checklist).
