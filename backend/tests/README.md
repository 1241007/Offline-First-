# Backend Pytest Test Suite (`backend/tests/`)

This directory houses the complete automated test suite for the EduNova FastAPI backend, encompassing unit tests, database integration tests, property-based fuzz tests, and RAG evaluation logic.

---

## Directory Overview

```
backend/tests/
├── conftest.py                             # Pytest fixtures, async database setup, mock clients
├── fixtures/
│   └── rrf_retrieval_fixtures.json         # Deterministic mock chunks for RRF calibration tests
├── test_auth.py                            # Registration, login, JWT issuance, token rotation, rate limits
├── test_auth_properties.py                 # Property-based testing with Hypothesis (fuzzing & invariants)
├── test_chat.py                            # Synchronous chat creation & permission validation
├── test_chat_stream.py                     # Server-Sent Events (SSE) token streaming tests
├── test_chat_pagination.py                 # Message and conversation pagination & sorting
├── test_conversations.py                   # Multi-conversation management (pin, archive, drafts)
├── test_context_propagation.py             # User profile, student grade, and memory prompt injection
├── test_courses_roadmaps_pagination.py     # Catalog course and roadmap pagination
├── test_embedding_service.py               # E5 ONNX runtime embedding generation tests
├── test_eval_metrics.py                    # Unit tests for Recall@K, MRR@K, nDCG@10, and Leakage
├── test_health.py                          # Service health check and AI provider status endpoint
├── test_memory.py                          # Memory extraction rules, CRUD, and active filtering
├── test_openrouter_service.py              # OpenRouter payload normalization and model fallbacks
├── test_personalized_roadmap.py            # Personalized roadmap generation tests
├── test_personalized_roadmap_pipeline.py   # Diagnostic quiz and roadmap adaptation pipeline
├── test_profile_and_learning.py            # Course enrollment, lesson checkoffs, and streaks
├── test_rag_pipeline.py                    # End-to-end RAG pipeline integration tests
├── test_roadmap_quality_fixtures.py        # Roadmap quality and milestone ordering tests
├── test_route_header_parser.py             # X-Correlation-ID and client routing header parsing
├── test_routing_contract.py                # Online vs. Offline routing API contract tests
├── test_rrf_scoring.py                     # Reciprocal Rank Fusion mathematical verification
├── test_rrf_threshold_analysis.py          # Evidence threshold sensitivity and bound checks
├── test_run_eval.py                        # CLI evaluation runner execution tests
├── test_syllabus_sync.py                   # Syllabus delta sync payloads for Android client
└── test_audit_e2e_scenarios.py             # End-to-end user journeys (auth -> chat -> progress)
```

---

## Testing Tools & Frameworks

- **Pytest**: Core test runner.
- **Pytest-AsyncIO**: Executes asynchronous FastAPI route and SQLAlchemy repository tests natively with `asyncio`.
- **Hypothesis**: Property-based fuzz testing validating invariants (e.g., token normalization idempotency, arbitrary input sanitization).
- **SQLite / In-Memory Mocking**: SQLite async engine is leveraged during fast unit test execution to provide zero-dependency database testing.

---

## Running the Tests

Execute tests from the `backend/` directory:

### Run All Tests
```powershell
cd backend
.\.venv\Scripts\python.exe -m pytest tests/ -v
```

### Run a Specific Test Module
```powershell
.\.venv\Scripts\python.exe -m pytest tests/test_auth.py -v
```

### Run Property-Based Fuzz Tests
```powershell
.\.venv\Scripts\python.exe -m pytest tests/test_auth_properties.py -v
```

### Run with Coverage Report
```powershell
.\.venv\Scripts\python.exe -m pytest --cov=app tests/
```
