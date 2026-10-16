# EduNova: Offline-First AI Educational Platform & Adaptive Tutor

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110+-009688.svg?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![Android API](https://img.shields.io/badge/Android%20API-26+-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Inference Engine](https://img.shields.io/badge/llama.cpp-Edge%20Native-blue.svg)](https://github.com/ggerganov/llama.cpp)
[![PostgreSQL](https://img.shields.io/badge/Database-Supabase%20pgvector-3ECF8E.svg?logo=supabase&logoColor=white)](https://supabase.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**EduNova** is an enterprise-grade, **offline-first educational platform and AI tutor** designed for Android mobile devices and backed by an asynchronous Python cloud service (FastAPI). 

EduNova bridges the digital divide for students and lifelong learners: when connected to the internet, it leverages low-latency cloud intelligence (Google Gemini models via OpenRouter, Supabase pgvector hybrid search, and persistent cloud sync); when completely disconnected, it continues to provide **full-featured, on-device AI tutoring** using native C++ `llama.cpp` inference running quantized GGUF models (Qwen 2.5 1.5B/3B) and local SQLite storage on CPU and RAM.

---

## Table of Contents

- [Core Value Propositions](#core-value-propositions)
- [System Architecture](#system-architecture)
  - [High-Level Architectural Diagram](#high-level-architectural-diagram)
  - [Edge vs. Cloud Request Routing](#edge-vs-cloud-request-routing)
- [Repository & Folder Structure](#repository--folder-structure)
- [Android Client (`app/`)](#android-client-app)
  - [Tech Stack & Native JNI](#tech-stack--native-jni)
  - [Offline Model Engine & KV Caching](#offline-model-engine--kv-caching)
  - [Local Cache & Storage](#local-cache--storage)
  - [Hardware-Backed Security](#hardware-backed-security)
- [Cloud Backend (`backend/`)](#cloud-backend-backend)
  - [Tech Stack & Frameworks](#tech-stack--frameworks)
  - [API Endpoints Directory](#api-endpoints-directory)
  - [RAG Retrieval & Evidence Calibration](#rag-retrieval--evidence-calibration)
  - [Long-Term Personalization Memory](#long-term-personalization-memory)
  - [Authentication & Replay Attack Defense](#authentication--replay-attack-defense)
- [Official Curriculum & Syllabus Corpus](#official-curriculum--syllabus-corpus)
- [Getting Started & Local Setup](#getting-started--local-setup)
  - [Prerequisites](#prerequisites)
  - [1. Backend Setup & Local Server](#1-backend-setup--local-server)
  - [2. Android Studio Setup & Compilation](#2-android-studio-setup--compilation)
  - [3. Native Libraries (`llama.cpp`) Prebuilt Setup](#3-native-libraries-llamacpp-prebuilt-setup)
- [Testing & Quality Verification](#testing--quality-verification)
- [Contributing & Development Workflows](#contributing--development-workflows)
- [License](#license)

---

## Core Value Propositions

1. **Dual-Engine AI Tutoring (Edge + Cloud)**:
   - **Offline Edge AI**: Native C++ on-device LLM inference using `llama.cpp` with quantized Qwen 2.5 1.5B and 3B Instruct GGUF models. Operates with zero network dependency, real-time token streaming, and automatic multi-turn KV cache reuse.
   - **Online Cloud AI**: High-capacity cloud inference with Google Gemini (`google/gemini-3.8-flash` / `gemini-2.5-flash`) via OpenRouter, featuring Server-Sent Events (SSE) streaming and dynamic fallback.
2. **True Offline-First Continuity**:
   - Single unified chat and learning interface: conversations, drafts, pinned chats, message history, and user memories persist locally in an encrypted SQLite cache and synchronize bidirectionally with the backend when internet connectivity is restored.
3. **Hybrid RAG & Grounded Curriculum Retrieval**:
   - Hybrid retrieval combining dense vector similarity (`intfloat/multilingual-e5-small` 384-dimensional embeddings) and sparse lexical search (`tsvector` with BM25-style ranking) fused via Reciprocal Rank Fusion (RRF).
   - Strict evidence thresholds preventing hallucinations on out-of-syllabus questions.
4. **Adaptive Learning Modes (Student vs. General)**:
   - **Student Mode**: Curated for CBSE Classes 9-12 (Science, Mathematics, Physics, Chemistry, Economics, Computer Science) with chapter breakdowns, progress tracking, and exam countdowns.
   - **General Mode**: Focused on professional software engineering, system architecture, programming languages, and industry roadmaps.
5. **Dynamic Long-Term Personalization**:
   - Real-time rule-based memory extraction that identifies learner goals (e.g., "preparing for JEE/NEET"), programming language preferences (e.g., "prefer Kotlin/Python"), and cognitive delivery styles, injecting context into prompt system instructions.
6. **Hardened Security & Cold-Start Protection**:
   - Dual-token JWT architecture with session family rotation and cryptographic token theft detection.
   - Android hardware-backed encryption (`androidx.security.crypto` with `MasterKey` AES-256 GCM/SIV).
   - Dynamic DNS and extended connection timeouts to gracefully absorb cloud provider cold starts (Render/Supabase).

---

## System Architecture

### High-Level Architectural Diagram

```mermaid
flowchart TB
    subgraph AndroidClient ["Android Application (Kotlin + Jetpack Compose)"]
        UI["UI Layer\n- AI Workspace Screen\n- Landing & School Screen\n- My Learning Screen\n- Roadmap Screen\n- Profile & Auth Screens"]
        VM["ViewModel & State Machine Layer\n- AIViewModel\n- LandingViewModel\n- RoadmapViewModel\n- AuthViewModel\n- SessionManager (StateFlow)"]
        REPO["Repository Orchestration\n- ModeAwareAIRepository\n- RemoteAuthRepository\n- Course & Learning Repositories\n- SyllabusSyncRepository"]
        
        subgraph LocalEdgeEngine ["Local Data & Edge Engine"]
            CACHE_DB[("Local SQLite Database\nedunova_chat_cache.db\nsyllabus_offline.db")]
            TOKEN_STORE["EncryptedTokenStorage\n(MasterKey AES-256 GCM/SIV)"]
            MODEL_MGR["OfflineModelManager\n(Idle watchdog, KV cache lifecycle)"]
            JNI["JNI Bridge (llama_jni.cpp)"]
            LLAMA_NATIVE["llama.cpp Shared Libraries\n(libllama, libggml-cpu, libggml)"]
            GGUF_FILE[("Local GGUF Models\nQwen2.5-1.5B / 3B")]
            E5_EMBED["E5EmbeddingService\n(ONNX Quantized 384-dim)"]
        end

        subgraph RemoteNetworking ["Network & API Layer"]
            OKHTTP_PUB["Public AuthApiClient\n(Login, Register, Refresh)"]
            OKHTTP_AUTH["AuthenticatedApiClient\n(AuthInterceptor + TokenAuthenticator)"]
            CHAT_CLIENT["ChatApiClient\n(SSE Streaming / REST)"]
        end
    end

    subgraph CloudBackend ["Cloud Backend (FastAPI + Async Python 3.11)"]
        ROUTERS["API Routers (/api/v1)\n- /auth (JWT, refresh, reset)\n- /chat & /stream (SSE)\n- /courses & /learning\n- /roadmaps & /profile\n- /memory (long-term facts)\n- /syllabus (sync & search)"]
        MIDDLEWARE["Middleware & Security\n- SlowAPI Rate Limiter\n- CorrelationIdMiddleware\n- Structured AuthException Handler"]
        SERVICES["Business Services\n- AuthService & Token Rotation\n- ChatService\n- SupabaseRetrievalService (RRF)\n- OpenRouterService (Gemini)\n- MemoryService (Rule Extraction)"]
        SQLA["SQLAlchemy 2.0 Async ORM"]
        DB[("PostgreSQL Database (Supabase)\nUsers, Progress, Roadmaps, Messages\nSyllabus pgvector 384-dim (IVFFlat + GIN)")]
    end

    subgraph ExternalServices ["External Cloud Services"]
        OPENROUTER["OpenRouter API\n(google/gemini-3.8-flash)"]
        HF["Hugging Face Model Hub\n(PatilKrish/Qwen_Models2)"]
    end

    %% Wiring
    UI --> VM
    VM --> REPO
    REPO --> MODEL_MGR
    REPO --> CACHE_DB
    REPO --> OKHTTP_AUTH
    REPO --> OKHTTP_PUB
    REPO --> CHAT_CLIENT

    MODEL_MGR --> JNI
    JNI --> LLAMA_NATIVE
    LLAMA_NATIVE --> GGUF_FILE
    MODEL_MGR -.->|Direct Model Download| HF

    OKHTTP_PUB -->|HTTPS / REST| ROUTERS
    OKHTTP_AUTH -->|HTTPS + Bearer JWT| ROUTERS
    CHAT_CLIENT -->|HTTP SSE Streaming| ROUTERS

    ROUTERS --> MIDDLEWARE
    MIDDLEWARE --> SERVICES
    SERVICES --> SQLA
    SQLA --> DB
    SERVICES -->|Prompt + Grounded Context| OPENROUTER
```

### Edge vs. Cloud Request Routing

EduNova provides an intelligent routing layer (`ModeAwareAIRepository`) that directs user interactions based on user preference, connectivity state, and model availability:

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student (User)
    participant UI as AIWorkspaceScreen
    participant VM as AIViewModel
    participant ModeRouter as ModeAwareAIRepository
    participant LocalRepo as LocalAIRepository
    participant ModelMgr as OfflineModelManager
    participant JNI as llama_jni.cpp
    participant OnlineRepo as OnlineAIRepository
    participant CloudAPI as FastAPI Backend
    participant Supabase as Supabase pgvector
    participant LLM as OpenRouter Gemini

    Student->>UI: Enters question & taps Send
    UI->>VM: sendMessage(text)
    VM->>ModeRouter: streamMessage(prompt, conversationId, mode)

    alt Mode is OFFLINE
        ModeRouter->>LocalRepo: streamMessage(...)
        LocalRepo->>ModelMgr: generateStreaming(prompt, history)
        ModelMgr->>ModelMgr: Verify GGUF loaded & reset 3-min idle timer
        ModelMgr->>JNI: nativeGenerateStream(handle, prompt, callback)
        loop Local Token Generation
            JNI-->>ModelMgr: onToken(tokenPiece)
            ModelMgr-->>LocalRepo: emit(tokenPiece)
            LocalRepo-->>VM: emit(tokenPiece)
            VM-->>UI: Real-time UI text update
        end
        LocalRepo->>LocalRepo: Commit prompt & answer to SQLite
    else Mode is ONLINE
        ModeRouter->>OnlineRepo: streamMessage(...)
        OnlineRepo->>CloudAPI: POST /api/v1/chat/.../stream (Bearer JWT)
        CloudAPI->>Supabase: Hybrid Retrieval (E5 384-dim + tsvector)
        Supabase-->>CloudAPI: RRF Scored Chunks
        CloudAPI->>LLM: Stream prompt with syllabus evidence
        loop SSE Event Stream
            LLM-->>CloudAPI: Token piece
            CloudAPI-->>OnlineRepo: SSE "token" / "citations" event
            OnlineRepo-->>VM: emit(tokenPiece)
            VM-->>UI: Real-time UI text update
        end
        OnlineRepo->>OnlineRepo: Cache message in local SQLite database
    end
```

---

## Repository & Folder Structure

Every major directory in this repository contains its own dedicated `README.md` with deep domain explanations, component manifests, and usage instructions:

| Directory | Purpose | Dedicated Guide |
|---|---|---|
| [`adapters/`](file:///e:/Offline%20First/adapters/README.md) | AI agent rule configuration adapters (`CLAUDE.md`, `GEMINI.md`, `GPT_OSS.md`) | [adapters/README.md](file:///e:/Offline%20First/adapters/README.md) |
| [`app/`](file:///e:/Offline%20First/app/README.md) | Android client application (Kotlin, Compose, C++ NDK, SQLite, llama.cpp) | [app/README.md](file:///e:/Offline%20First/app/README.md) |
| ├── [`app/src/main/cpp/`](file:///e:/Offline%20First/app/src/main/cpp/README.md) | Native C++ JNI bridge for `llama.cpp` inference and KV cache control | [app/src/main/cpp/README.md](file:///e:/Offline%20First/app/src/main/cpp/README.md) |
| ├── [`app/src/main/jniLibs/`](file:///e:/Offline%20First/app/src/main/jniLibs/README.md) | Prebuilt 64-bit native libraries (`arm64-v8a`, `x86_64`) | [app/src/main/jniLibs/README.md](file:///e:/Offline%20First/app/src/main/jniLibs/README.md) |
| ├── [`app/src/main/java/com/offline_First/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/README.md) | Kotlin source root, `MainActivity.kt`, and `AppContainer.kt` | [java/.../README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/README.md) |
| │   ├── [`data/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/data/README.md) | Data layer: SQLite, EncryptedSharedPreferences, OkHttp clients, repositories | [data/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/data/README.md) |
| │   ├── [`domain/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/README.md) | Domain entities, business models, and reactive UI states | [domain/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/README.md) |
| │   └── [`ui/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/ui/README.md) | Presentation layer: Jetpack Compose screens, ViewModels, and design system | [ui/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/ui/README.md) |
| └── [`app/src/test/`](file:///e:/Offline%20First/app/src/test/README.md) | Android unit and integration test suite (JUnit 4, MockWebServer, Coroutines) | [app/src/test/README.md](file:///e:/Offline%20First/app/src/test/README.md) |
| [`backend/`](file:///e:/Offline%20First/backend/README.md) | Python cloud backend application (FastAPI, SQLAlchemy 2.0 Async, Supabase) | [backend/README.md](file:///e:/Offline%20First/backend/README.md) |
| ├── [`backend/app/`](file:///e:/Offline%20First/backend/app/README.md) | FastAPI core application: routes, core security, middleware, ORM models, services | [backend/app/README.md](file:///e:/Offline%20First/backend/app/README.md) |
| ├── [`backend/docs/`](file:///e:/Offline%20First/backend/docs/README.md) | Backend research notes, threshold analyses, and architectural reports | [backend/docs/README.md](file:///e:/Offline%20First/backend/docs/README.md) |
| ├── [`backend/evals/`](file:///e:/Offline%20First/backend/evals/README.md) | RAG evaluation framework, golden benchmark cases, and scoring metrics | [backend/evals/README.md](file:///e:/Offline%20First/backend/evals/README.md) |
| ├── [`backend/migrations/`](file:///e:/Offline%20First/backend/migrations/README.md) | Alembic database schema migration history and scripts | [backend/migrations/README.md](file:///e:/Offline%20First/backend/migrations/README.md) |
| ├── [`backend/models/`](file:///e:/Offline%20First/backend/models/README.md) | Local quantized ONNX embedding model (`intfloat/multilingual-e5-small`) | [backend/models/README.md](file:///e:/Offline%20First/backend/models/README.md) |
| ├── [`backend/scripts/`](file:///e:/Offline%20First/backend/scripts/README.md) | Operational scripts (syllabus ingestion, data seeding, inference parity) | [backend/scripts/README.md](file:///e:/Offline%20First/backend/scripts/README.md) |
| ├── [`backend/syllabus_sources/`](file:///e:/Offline%20First/backend/syllabus_sources/README.md) | Official CBSE & NCERT curriculum documents, raw PDFs, and source manifests | [syllabus_sources/README.md](file:///e:/Offline%20First/backend/syllabus_sources/README.md) |
| └── [`backend/tests/`](file:///e:/Offline%20First/backend/tests/README.md) | Backend pytest test suite (unit, integration, property-based tests) | [backend/tests/README.md](file:///e:/Offline%20First/backend/tests/README.md) |
| [`docs/`](file:///e:/Offline%20First/docs/README.md) | Global system audits, request flow traces, and corpus inventories | [docs/README.md](file:///e:/Offline%20First/docs/README.md) |
| [`gradle/`](file:///e:/Offline%20First/gradle/README.md) | Gradle wrapper binaries, JVM daemon configs, and version catalog (`libs.versions.toml`) | [gradle/README.md](file:///e:/Offline%20First/gradle/README.md) |
| [`scripts/`](file:///e:/Offline%20First/scripts/README.md) | Project automation: prebuilt compilation, repository search, and validation | [scripts/README.md](file:///e:/Offline%20First/scripts/README.md) |
| [`.gsd/`](file:///e:/Offline%20First/.gsd/README.md) | Get Shit Done (GSD) roadmap, specification, phase execution plans, and state tracking | [.gsd/README.md](file:///e:/Offline%20First/.gsd/README.md) |

---

## Android Client (`app/`)

The Android application is written in 100% Kotlin with Jetpack Compose for declarative UI and a native C++ JNI bridge for on-device inference.

### Key Capabilities

1. **Native On-Device LLM Inference**:
   - Compiles prebuilt `libllama.so` and `libggml.so` using Android NDK 27.2+ and CMake 3.31+.
   - Streams tokens directly into Kotlin coroutine `Flow` collectors for instant UI rendering.
   - Preserves prompt token sequences across multi-turn chats via KV cache prefix matching.
   - Manages memory through an idle watchdog that unloads native model weights after **3 minutes** of inactivity (`OFFLINE_MODEL_IDLE_TIMEOUT_MS = 180_000L`).
2. **Resilient Local Persistence**:
   - `ChatCacheDatabase`: Native SQLite implementation managing conversations, parent-child message trees (supporting branching and editing), pinned items, draft text, and sync markers.
   - `SyllabusDatabase`: Stores localized syllabus chunks and metadata for offline study.
3. **Enterprise Security**:
   - `EncryptedTokenStorage`: Leverages Android Keystore (`androidx.security.crypto.MasterKey`) with AES-256 GCM to secure access and refresh tokens.
   - `TokenAuthenticator`: Implements a synchronized mutex on OkHttp 401 Unauthorized responses to perform a single token refresh request and retry pending calls without racing.

---

## Cloud Backend (`backend/`)

The backend is built with asynchronous Python 3.11 and FastAPI, adhering to clean architecture principles with separate routers, services, repositories, and ORM models.

### Key Capabilities

1. **Dual-Token JWT Authentication**:
   - Access tokens (15-minute lifespan) and refresh tokens (30-day lifespan).
   - Refresh tokens are hashed using SHA-256 and tracked under a unique `session_family_id`.
   - **Replay Attack Detection**: If an already revoked refresh token is presented, all active sessions belonging to that family are immediately revoked.
2. **Hybrid RAG & Reciprocal Rank Fusion (RRF)**:
   - Queries are embedded using a quantized `intfloat/multilingual-e5-small` ONNX model (384 dimensions).
   - Simultaneous vector similarity search and PostgreSQL `tsvector` keyword search in Supabase.
   - Scores combined via RRF ($k=60$):
     $$\text{RRF}(d) = \sum_{m \in \{dense, sparse\}} \frac{1}{k + r_m(d)}$$
   - Calibrated normalized evidence threshold (`SUFFICIENT = 0.48`, `UNCERTAIN = 0.35`) prevents hallucination while allowing single-retriever semantic matches.
3. **Automated Memory Extraction**:
   - Real-time extraction of user coding preferences (languages, frameworks), career targets (JEE, NEET, GATE), and difficulty preferences from conversational inputs.
   - Stored in `user_memories` and injected into LLM system prompts.

---

## Official Curriculum & Syllabus Corpus

EduNova contains official CBSE & NCERT curriculum documents structured under `backend/syllabus_sources/CBSE/`:

- **Class 9**: English, Mathematics, Science, Social Science
- **Class 10**: English, Mathematics, Science, Social Science
- **Class 11**: Biology, Chemistry, Computer Science, Economics, Mathematics, Physics
- **Class 12**: Biology, Chemistry, Computer Science, Economics, Mathematics, Physics

Each subject folder contains:
1. Official Curriculum PDF (e.g., `Curriculum_2026_27_Mathematics.pdf`).
2. Verification Metadata (`.pdf.meta.json` with file hashes, page counts, and extraction dates).
3. Source Provenance Manifest (`sources.json` with official CBSE portal source URLs).

---

## Getting Started & Local Setup

### Prerequisites

- **Android Development**: Android Studio Jellyfish / Ladybug+ with JDK 17 or 21, Android SDK Platform 35, and NDK 27.2+.
- **Backend Development**: Python 3.11+, PostgreSQL (or Supabase instance with `pgvector` enabled), and Git.
- **PowerShell / Bash**: Standard shell tooling.

---

### 1. Backend Setup & Local Server

1. **Navigate to the backend directory and set up a virtual environment**:
   ```powershell
   cd backend
   python -m venv .venv
   .\.venv\Scripts\activate      # Windows PowerShell
   # source .venv/bin/activate    # Linux / macOS
   ```

2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Configure environment variables**:
   Create or edit `.env` in the repository root (or `backend/.env`):
   ```ini
   ENVIRONMENT=development
   DATABASE_URL=postgresql+asyncpg://postgres:your_password@localhost:5432/edunova
   JWT_SECRET_KEY=your-32-byte-secret-hex-key-here
   OPENROUTER_API_KEY=sk-or-v1-...
   OPENROUTER_MODEL=google/gemini-3.8-flash
   CORS_ORIGINS=["http://10.0.2.2:8000","http://localhost:8000"]
   ```

4. **Run database migrations**:
   ```bash
   alembic upgrade head
   ```

5. **(Optional) Seed baseline courses and roadmaps**:
   ```bash
   python scripts/seed_data.py
   ```

6. **Launch the FastAPI development server**:
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```
   - Interactive OpenAPI Swagger Docs: [http://localhost:8000/docs](http://localhost:8000/docs)
   - Health Check: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

### 2. Android Studio Setup & Compilation

1. **Configure Backend URL**:
   In `gradle.properties`, set `backendBaseUrl`:
   ```properties
   # For Android Emulator pointing to local machine:
   backendBaseUrl=http://10.0.2.2:8000

   # Or Cloud Backend:
   # backendBaseUrl=https://edunova-backend-9waj.onrender.com
   ```

2. **Compile and Run Unit Tests**:
   ```powershell
   # Point to Android Studio JDK
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat testDebugUnitTest
   ```

3. **Build the Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

### 3. Native Libraries (`llama.cpp`) Prebuilt Setup

The project includes precompiled native shared libraries in `app/src/main/jniLibs/` for `arm64-v8a` and `x86_64`. If you ever need to recompile or update the pinned `llama.cpp` version:

```powershell
# Windows PowerShell
.\scripts\build_llama_prebuilt.ps1

# Linux / macOS
chmod +x scripts/build_llama_prebuilt.sh
./scripts/build_llama_prebuilt.sh
```

---

## Testing & Quality Verification

EduNova maintains comprehensive, automated test suites across both the Android application and the backend service:

### Backend Pytest Suite
```powershell
cd backend
.\.venv\Scripts\python.exe -m pytest tests/ -v
```
Includes:
- `test_auth.py`: Full login, registration, token refresh, and session revocation tests.
- `test_auth_properties.py`: Property-based fuzzing with Hypothesis.
- `test_chat.py` & `test_chat_stream.py`: SSE message streaming and ownership checks.
- `test_rrf_scoring.py`: Mathematical verification of Reciprocal Rank Fusion scores.
- `test_syllabus_sync.py`: Verification of curriculum sync payloads.

### RAG Benchmark Evaluation Suite
```powershell
cd backend
.\.venv\Scripts\python.exe evals/rag/run_eval.py --suite retrieval --output-dir evals/rag/results/
```
Calculates **Recall@K**, **MRR@K**, **nDCG@10**, and **Cross-Curriculum Leakage** on the `golden_set.jsonl` benchmark.

### Android Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```
Tests ViewModel state transformations, MockWebServer token refresh mutex, encrypted token storage, and offline model lifecycle watchdogs.

---

## Contributing & Development Workflows

- Follow [PROJECT_RULES.md](file:///e:/Offline%20First/PROJECT_RULES.md) for architectural boundaries, error-handling conventions, and coding guidelines.
- Follow [GSD-STYLE.md](file:///e:/Offline%20First/GSD-STYLE.md) for commit standards and task orchestration.
- To validate repository health before opening a pull request:
  ```powershell
  .\scripts\validate-all.ps1
  ```

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
