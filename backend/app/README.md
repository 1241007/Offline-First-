# FastAPI Application Core (`backend/app/`)

This directory houses the core application code for the EduNova cloud backend, structured into cleanly separated architectural layers: API routes, core infrastructure, middleware, database models, data repositories, Pydantic schemas, and domain services.

---

## Architecture & Layer Breakdown

```
backend/app/
├── main.py                     # FastAPI application factory, middleware, router mount points
├── api/
│   └── routes/                 # REST & SSE endpoint handlers
│       ├── auth.py             # User registration, login, token rotation, password reset
│       ├── chat.py             # Conversation management, message history, SSE stream
│       ├── courses.py          # Course catalog, categories, module details
│       ├── learning.py         # Enrolled course tracking, lesson completion checkoffs
│       ├── memory.py           # User long-term memory entity CRUD
│       ├── profile.py          # Learner profile, education mode, interests
│       ├── retrieve.py         # Standalone RAG hybrid retrieval endpoint
│       ├── roadmaps.py         # Career & academic roadmap exploration & generation
│       └── syllabus_sync.py    # Syllabus delta synchronization for Android client
├── core/                       # Core system configuration & security primitives
│   ├── config.py               # Pydantic Settings reading .env and environment variables
│   ├── database.py             # Async engine, sessionmaker, and DB connection pooling
│   ├── deps.py                 # FastAPI dependency injection providers (get_db, get_current_user)
│   ├── errors.py               # Structured exception definitions & JSON error handlers
│   ├── rate_limit.py           # SlowAPI rate limiter configuration
│   └── security.py             # BCrypt password hashing & PyJWT token creation/decoding
├── middleware/
│   └── correlation.py          # Attaches/propagates X-Correlation-ID across requests and logs
├── models/                     # SQLAlchemy 2.0 Async ORM domain entities
│   ├── conversation.py         # Conversation entity (title, pinned, archived, draft)
│   ├── message.py              # Message entity (role, content, parent_id, timestamps)
│   ├── memory.py               # User long-term personalization facts
│   ├── user.py                 # User account, credentials, and refresh tokens
│   ├── profile.py              # Profile, education mode, and student grade level
│   ├── course.py               # Courses, modules, and lessons
│   ├── progress.py             # User course & lesson progress tracking
│   ├── roadmap.py              # System and user-created roadmaps
│   ├── roadmap_assessment.py   # Diagnostic quiz and roadmap adaptation sessions
│   └── syllabus.py             # Syllabus documents, chunks, and pgvector embeddings
├── repositories/               # Data access layer (async queries, transactions)
│   ├── chat_repository.py      # Conversation queries, message pagination, parent tree fetch
│   ├── course_repository.py    # Catalog search, filtering, and module lookups
│   ├── learning_repository.py  # User enrollments, progress updates, streak calculations
│   ├── memory_repository.py    # Memory facts filtering by importance/confidence
│   ├── profile_repository.py   # Profile upserts and interest lookups
│   ├── roadmap_repository.py   # Roadmap milestone retrieval
│   └── roadmap_assessment_repository.py # Diagnostic assessment state storage
├── schemas/                    # Pydantic v2 schemas for request validation & response DTOs
│   ├── auth.py, chat.py, course.py, learning.py, memory.py, profile.py, rag.py, ...
└── services/                   # Business logic, third-party integrations, algorithms
    ├── auth_service.py         # Token rotation, replay attack defense, session family
    ├── chat_service.py         # Orchestrates prompt assembly, memory injection, retrieval
    ├── openrouter_service.py   # OpenRouter API client for Gemini streaming & fallback
    ├── supabase_retrieval_service.py # Hybrid search (vector + keyword) with RRF scoring
    ├── memory_service.py       # Rule-based real-time extraction of user preferences
    ├── local_e5_service.py     # Local ONNX runtime for E5 embedding generation
    └── ...                     # Course, learning, profile, and roadmap services
```

---

## Key Subsystems & Workflow

### 1. Hybrid RAG Pipeline (`supabase_retrieval_service.py`)
- Takes student query and grade/subject filter.
- Computes query embedding using `local_e5_service.py` (ONNX quantized E5-small).
- Dispatches concurrent database queries to Supabase:
  - **Dense Search**: `cosine_similarity` using `pgvector` IVFFlat index.
  - **Sparse Search**: `ts_rank_cd` full-text search against `tsvector` column.
- Fuses rankings using Reciprocal Rank Fusion ($k=60$):
  $$S_{RRF}(d) = \sum_{m \in \{dense, sparse\}} \frac{1}{60 + r_m(d)}$$
- Computes normalized score and classifies evidence as `SUFFICIENT`, `UNCERTAIN`, or `MISSING`.

### 2. Token Security & Rotation (`auth_service.py`)
- Distributes paired tokens: Access Token (short-lived) + Refresh Token (long-lived).
- Stored as SHA-256 hash in `refresh_tokens` table.
- Enforces session family continuity. If an attacker attempts to replay an expired or revoked refresh token, the entire session family is revoked instantly.

### 3. Server-Sent Events Chat Streaming (`api/routes/chat.py`)
- Emits chunks via HTTP `text/event-stream`:
  - `event: token`: Assistant message text piece.
  - `event: citations`: JSON list of syllabus citations with chapter, page, and chunk IDs.
  - `event: done`: Signals completion with final metadata.
