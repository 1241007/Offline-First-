# Database Migrations (`backend/migrations/`)

This directory contains database schema migration scripts managed by **Alembic** for the EduNova backend.

---

## Directory Overview

```
backend/migrations/
├── README.md                   # Migration documentation and execution guide
├── env.py                      # Alembic runtime environment script configuring async SQLAlchemy
├── script.py.mako              # Template for generating new revision scripts
└── versions/                   # Ordered migration revision scripts
    ├── fd5a2e32997d_phase1_core_schema_add_course_.py
    ├── 041d77f52987_create_conversations_and_messages.py
    ├── 0c9897f27723_add_composite_index_on_messages.py
    ├── a1b2c3d4e5f6_add_auth_tables_and_conversation_ownership.py
    ├── b2c3d4e5f6a7_add_chat_metadata_columns.py
    ├── c4d5e6f7a8b9_add_roadmap_assessment_sessions_and_structure.py
    ├── e1f2a3b4c5d6_add_rag_profile_fields.py
    └── f1a2b3c4d5e6_add_syllabus_documents_and_chunks.py
```

---

## Migration History & Key Schema Changes

| Revision ID | Description | Key Tables / Columns Added |
|---|---|---|
| `fd5a2e32997d` | Phase 1 Core Schema | `course_categories`, `courses`, `course_modules`, `lessons`, `user_course_progress`, `user_lesson_progress`, `roadmaps`, `roadmap_items`. |
| `041d77f52987` | Conversations & Messages | `conversations`, `messages`. |
| `0c9897f27723` | Performance Indexing | Composite index on `(conversation_id, created_at)` for message pagination. |
| `a1b2c3d4e5f6` | Authentication & Ownership | `users`, `refresh_tokens`, `password_reset_tokens`, `user_profiles`, user ID foreign keys on conversations. |
| `b2c3d4e5f6a7` | Chat Metadata | Pinned status, archive flag, draft text, and sync markers on conversations. |
| `c4d5e6f7a8b9` | Roadmap Diagnostics | `roadmap_assessment_sessions`, diagnostic question tracks. |
| `e1f2a3b4c5d6` | RAG Profile Preferences | Added `grade_level`, `preferred_board` (`CBSE`), and personalized study goals to `user_profiles`. |
| `f1a2b3c4d5e6` | Syllabus Corpus & Vector Search | `syllabus_documents`, `syllabus_chunks` with `pgvector` (384-dimensional vector column) and `tsvector` full-text search. |

---

## Migration Commands

All commands should be executed from the `backend/` directory with the virtual environment activated:

### Apply All Pending Migrations
```bash
alembic upgrade head
```

### Roll Back Last Migration
```bash
alembic downgrade -1
```

### Generate a New Migration Revision
```bash
alembic revision --autogenerate -m "describe_schema_change"
```

### Check Current Migration Status
```bash
alembic current
```
