# EduNova Cloud Backend (`backend/`)

This directory contains the asynchronous Python cloud backend service for EduNova, built on **FastAPI**, **SQLAlchemy 2.0 (AsyncIO)**, and **Supabase PostgreSQL** with `pgvector`.

---

## Directory Overview

```
backend/
├── alembic.ini                 # Database migration configuration
├── pytest.ini                  # Pytest configuration and async test loop settings
├── requirements.txt            # Python dependencies (FastAPI, SQLAlchemy, PyJWT, SlowAPI, etc.)
├── app/                        # FastAPI application source code
│   ├── main.py                 # Application factory, middleware registration, and routers
│   ├── api/routes/             # REST endpoints (auth, chat, courses, learning, memory, etc.)
│   ├── core/                   # Configuration, async DB session, rate limits, security
│   ├── middleware/             # Correlation ID tracking middleware
│   ├── models/                 # SQLAlchemy 2.0 async ORM domain entities
│   ├── repositories/           # Async database repositories
│   ├── schemas/                # Pydantic v2 schemas and validation models
│   └── services/               # Business logic, LLM streaming, hybrid retrieval, memory
├── docs/                       # Research documentation, retrieval threshold studies
├── evals/                      # RAG evaluation harness, golden benchmark sets, and metrics
├── migrations/                 # Alembic database migration revisions
├── models/                     # Quantized ONNX embedding models and tokenizers
├── scripts/                    # Operational CLI scripts (data seeding, syllabus ingestion)
├── syllabus_sources/           # Official CBSE & NCERT curriculum documents (Classes 9-12)
└── tests/                      # Pytest unit, integration, and property-based test suites
```

---

## Technology Stack

| Component | Framework / Library | Rationale |
|---|---|---|
| **Web Framework** | FastAPI 0.115.0 & Uvicorn | High-throughput asynchronous routing, dependency injection, and automatic OpenAPI documentation. |
| **ORM & Database** | SQLAlchemy 2.0.36 + `asyncpg` | Async query execution, type safety, and connection pooling. |
| **Vector Database** | Supabase PostgreSQL (`pgvector`) | Native 384-dimensional vector similarity search with IVFFlat and GIN full-text indexes. |
| **Migrations** | Alembic 1.13.3 | Automated schema versioning and migration rollbacks. |
| **Security & Auth** | PyJWT 2.15.1 + BCrypt 5.0.0 | Cryptographic token creation, session families, and constant-time password hashing. |
| **Rate Limiting** | SlowAPI 0.1.9 | IP and user-based token bucket rate limiting to prevent abuse. |
| **AI LLM Gateway** | HTTPX 0.27.2 + OpenRouter | Server-Sent Events (SSE) streaming to Google Gemini models with automated fallback. |
| **Testing** | Pytest 8.3.3 + Hypothesis 6.112 | Comprehensive unit testing and property-based fuzzing. |

---

## Subdirectories

- **[`app/`](file:///e:/Offline%20First/backend/app/README.md)**: Main FastAPI application architecture, routers, services, repositories, and schemas.
- **[`docs/`](file:///e:/Offline%20First/backend/docs/README.md)**: Architectural research notes and empirical threshold evaluations.
- **[`evals/`](file:///e:/Offline%20First/backend/evals/README.md)**: RAG evaluation suite, golden benchmark sets (`golden_set.jsonl`), and metric algorithms.
- **[`migrations/`](file:///e:/Offline%20First/backend/migrations/README.md)**: Alembic database migration scripts.
- **[`models/`](file:///e:/Offline%20First/backend/models/README.md)**: Local quantized embedding model weights and tokenizers.
- **[`scripts/`](file:///e:/Offline%20First/backend/scripts/README.md)**: Administrative and operational scripts.
- **[`syllabus_sources/`](file:///e:/Offline%20First/backend/syllabus_sources/README.md)**: Official CBSE & NCERT syllabus PDFs and metadata.
- **[`tests/`](file:///e:/Offline%20First/backend/tests/README.md)**: Test suites covering auth, chat, RAG, and property tests.

---

## Environment Setup & Local Server

### 1. Create and Activate Virtual Environment
```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate      # Windows
# source .venv/bin/activate    # Linux / macOS
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Configure Environment Variables
Create `.env` in the repository root or in `backend/`:
```ini
ENVIRONMENT=development
DATABASE_URL=postgresql+asyncpg://postgres:your_password@localhost:5432/edunova
JWT_SECRET_KEY=your-32-byte-hex-secret
ACCESS_TOKEN_EXPIRE_MINUTES=15
REFRESH_TOKEN_EXPIRE_DAYS=30
RESET_TOKEN_EXPIRE_MINUTES=60
OPENROUTER_API_KEY=sk-or-v1-...
OPENROUTER_MODEL=google/gemini-3.8-flash
CORS_ORIGINS=["http://10.0.2.2:8000","http://localhost:8000"]
```

### 4. Run Migrations & Seed Data
```bash
alembic upgrade head
python scripts/seed_data.py
```

### 5. Start the Development Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
- Interactive Swagger UI: [http://localhost:8000/docs](http://localhost:8000/docs)
- Health Check: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

## Running the Test Suite

```powershell
.\.venv\Scripts\python.exe -m pytest tests/ -v
```
