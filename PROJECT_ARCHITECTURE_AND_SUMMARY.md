# EduNova: System Architecture, Current Progress, and Project Summary

---

## 1. Executive Summary & Project Overview

**EduNova** is an advanced, production-grade, **offline-first educational platform and AI-powered tutor** designed for mobile devices (Android) with a supporting cloud backend (FastAPI). The project bridges the gap between cloud-connected learning and completely disconnected edge computing by enabling students and lifelong learners to access curriculum courses, personalized roadmaps, progress tracking, and **real-time AI tutoring with zero internet connectivity**.

### Core Value Propositions

1. **Dual-Engine AI Tutoring (Edge + Cloud)**:
   - **Offline Edge AI**: On-device native large language model inference using `llama.cpp` and GGUF quantized models (Qwen 2.5 1.5B and 3B Instruct) running fully on local device CPU/RAM with real-time token streaming and multi-turn KV cache reuse.
   - **Online Cloud AI**: Low-latency, high-capacity cloud inference powered by OpenRouter and Google Gemini (`google/gemini-3.8-flash` / `gemini-2.5-flash`) with Server-Sent Events (SSE) streaming and automated fallback handling.
2. **Seamless Offline-First Continuity**:
   - Single unified chat and learning interface: conversations, drafts, pinned chats, message history, and user memories persist locally in an encrypted SQLite cache and synchronize bidirectionally with the backend when internet connectivity is restored.
3. **Adaptive Student & General Modes**:
   - Dedicated persona and prompt engines tailored for school curricula (CBSE Class 10/12 subjects, chapter mastery, exam countdowns) and general engineering/career skills (programming, systems architecture, tech roadmaps).
4. **Long-Term Memory & Personalization**:
   - Automated memory extraction that captures user goals (e.g., "preparing for JEE/NEET/GATE"), language preferences (e.g., "code examples in Kotlin/Python"), and preferred explanation styles, injecting context dynamically into both local and cloud prompts.
5. **Production-Grade Security & Reliability**:
   - Dual-token JWT authentication (access + refresh tokens with session family tracking and cryptographic reuse detection).
   - Android hardware-backed encryption (`androidx.security.crypto` via `MasterKey` AES-256 GCM/SIV).
   - Correlation IDs (`X-Correlation-ID`) across the entire network boundary, rate limiting via `slowapi`, and defensive connection timeouts designed for cloud cold-start resilience (Render/Supabase).

---

## 2. High-Level System Architecture

EduNova is built on clean architectural separation across the Android client, the native C++ inference bridge, and the asynchronous Python backend.

```mermaid
flowchart TB
    subgraph AndroidClient ["Android Application (Kotlin + Jetpack Compose)"]
        UI["UI Layer\n- AI Workspace Screen\n- Landing & School Screen\n- My Learning Screen\n- Roadmap Screen\n- Profile & Auth Screens"]
        VM["ViewModel & State Layer\n- AIViewModel\n- LandingViewModel\n- RoadmapViewModel\n- AuthViewModel\n- SessionManager (StateFlow)"]
        REPO["Repository Orchestration\n- ModeAwareAIRepository\n- RemoteAuthRepository\n- Course/Roadmap/Learning Repos"]
        
        subgraph LocalData ["Local Data & Edge Engine"]
            CACHE_DB[("Local SQLite Database\n(edunova_chat_cache.db)\nConversations, Messages, Memories")]
            TOKEN_STORE["EncryptedTokenStorage\n(MasterKey AES-256 GCM/SIV)"]
            MODEL_MGR["OfflineModelManager\n(Idle watchdog, KV cache lifecycle)"]
            JNI["JNI Bridge (llama_jni.cpp)"]
            LLAMA_NATIVE["llama.cpp Shared Libraries\n(libllama, libggml-cpu, libggml)"]
            GGUF_FILE[("Local GGUF Models\nQwen2.5-1.5B / 3B")]
        end

        subgraph RemoteData ["Network & Remote Layer"]
            OKHTTP_PUB["Public AuthApiClient\n(Login, Register, Refresh)"]
            OKHTTP_AUTH["AuthenticatedApiClient\n(AuthInterceptor + TokenAuthenticator)"]
        end
    end

    subgraph CloudBackend ["Cloud Backend (FastAPI + Async Python 3.11)"]
        ROUTERS["API Routers (/api/v1)\n- /auth (JWT, refresh, reset)\n- /chat & /stream (SSE)\n- /courses & /learning\n- /roadmaps & /profile\n- /memory (long-term facts)"]
        MIDDLEWARE["Middleware & Security\n- SlowAPI Rate Limiter\n- CorrelationIdMiddleware\n- Structured AuthException Handler"]
        SERVICES["Business Services\n- AuthService & Token Rotation\n- ChatService\n- OpenRouterService (Gemini)\n- MemoryService (Extraction rules)"]
        SQLA["SQLAlchemy 2.0 Async ORM"]
        DB[("PostgreSQL Database (Supabase)\nUsers, Progress, Roadmaps, Messages")]
    end

    subgraph ExternalServices ["External Cloud Services"]
        OPENROUTER["OpenRouter / Gemini API\n(google/gemini-3.8-flash)"]
        HF["Hugging Face Model Hub\n(PatilKrish/Qwen_Models2)"]
    end

    %% Interactions
    UI --> VM
    VM --> REPO
    REPO --> MODEL_MGR
    REPO --> CACHE_DB
    REPO --> OKHTTP_AUTH
    REPO --> OKHTTP_PUB

    MODEL_MGR --> JNI
    JNI --> LLAMA_NATIVE
    LLAMA_NATIVE --> GGUF_FILE
    MODEL_MGR -.->|Download Model| HF

    OKHTTP_PUB -->|HTTPS / REST| ROUTERS
    OKHTTP_AUTH -->|HTTPS + Bearer JWT| ROUTERS

    ROUTERS --> MIDDLEWARE
    MIDDLEWARE --> SERVICES
    SERVICES --> SQLA
    SQLA --> DB
    SERVICES -->|Prompt + Context| OPENROUTER
```

---

## 3. Android Client Architecture & Implementation

### 3.1 Tech Stack & Tooling

| Component | Technology | Rationale |
|---|---|---|
| **Language** | Kotlin 2.0+ (Kotlin Coroutines & Flow) | Type-safe asynchronous pipelines, cold flows for reactive streaming |
| **UI Toolkit** | Jetpack Compose (Material 3) | Declarative UI, state hoisting, responsive design system |
| **Native Build** | CMake 3.31+, Android NDK 27.2+ | Cross-compiling C++ native JNI bridges for `arm64-v8a` and `x86_64` |
| **Inference Runtime** | `llama.cpp` prebuilt binaries | High-efficiency GGML CPU matrix multiplication with NEON/AVX optimizations |
| **Local Persistence** | Android SQLite OpenHelper (`ChatCacheDatabase`) | Zero-dependency, lightweight, migration-safe local relational storage |
| **Credentials Security** | `androidx.security:security-crypto:1.0.0` | Hardware-backed keystore encryption (`EncryptedSharedPreferences`) |
| **Networking** | OkHttp 4.12+ & Kotlinx Serialization | Resilient connection pools, custom interceptors, automated 401 re-authentication |
| **Unit Testing** | JUnit 4, MockWebServer, Kotlinx Coroutines Test | Fast deterministic testing of repositories, viewmodels, and network interceptors |

---

### 3.2 Dual-Engine AI & Inference Subsystem

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as AIWorkspaceScreen
    participant VM as AIViewModel
    participant Repo as ModeAwareAIRepository
    participant LocalRepo as LocalAIRepository
    participant Mgr as OfflineModelManager
    participant JNI as llama_jni.cpp
    participant Engine as llama.cpp Native Core
    participant OnlineRepo as OnlineAIRepository
    participant Backend as FastAPI Backend

    User->>UI: Enters message & hits Send
    UI->>VM: sendMessage(prompt)
    VM->>Repo: streamMessage(prompt, parentId, clientMessageId)

    alt Mode is OFFLINE
        Repo->>LocalRepo: streamMessage(...)
        LocalRepo->>Mgr: generateStreaming(prompt, history, ...)
        Mgr->>Mgr: Check instance ID & reset 3-min idle timer
        Mgr->>JNI: nativeGenerateStream(handle, prompt, callback)
        JNI->>Engine: Evaluate prompt tokens (reuses KV cache prefix)
        loop Token Generation Loop
            Engine->>JNI: Sample token
            JNI->>Mgr: onToken(tokenPiece)
            Mgr->>LocalRepo: emit(tokenPiece)
            LocalRepo->>VM: emit(tokenPiece)
            VM->>UI: Update Assistant message bubble in real time
        end
        Mgr->>LocalRepo: Generation complete -> Persist to SQLite
    else Mode is ONLINE
        Repo->>OnlineRepo: streamMessage(...)
        OnlineRepo->>Backend: POST /api/v1/chat/conversations/{id}/messages/stream (Bearer JWT)
        Backend-->>OnlineRepo: Server-Sent Events (SSE chunk stream)
        loop SSE Chunks
            OnlineRepo->>VM: emit(chunk)
            VM->>UI: Update Assistant message bubble in real time
        end
        OnlineRepo->>OnlineRepo: Persist message in local cache & sync state
    end
```

#### Key Engineering Features of Edge AI:
1. **Model Management (`OfflineModelManager`)**:
   - **Singleton Instance Lifecycle**: Keeps the GGUF model mapped in memory across multi-turn interactions without reloading weights from disk.
   - **Idle Shutdown Watchdog**: Unloads model weights and releases native RAM if no inference requests are received within **3 minutes** (`OFFLINE_MODEL_IDLE_TIMEOUT_MS = 180_000L`).
   - **KV Cache Prefix Matching**: C++ bridge compares new prompt token sequences with previously evaluated tokens in the context, evaluating only newly added tokens to drastically cut prompt processing latency.
   - **Latency Telemetry**: Comprehensive monotonic timestamp logging tracking `send_clicked` $\to$ `repository_received` $\to$ `model_manager_called` $\to$ `model_ready` $\to$ `inference_started` $\to$ `prompt_processing` $\to$ `first_token` $\to$ `generation_completed`.
2. **Model Download Manager**:
   - Downloads small (`Qwen2.5-1.5B-Instruct-Q4_K_M.gguf`, ~0.98 GB) or large (`Qwen2.5-3B-Instruct-Q4_K_M.gguf`, ~1.9 GB) models directly from HuggingFace.
   - Resumable streaming downloads with temporary file verification (`.tmp` $\to$ `.gguf`), disk space validation, and detailed network exception classification (`UnknownHostException`, `SocketTimeoutException`, `ConnectException`).

---

### 3.3 Offline Storage & Caching Layer (`ChatCacheDatabase`)

EduNova implements an offline-first SQLite database schema supporting full schema migration safeguards:
- **`local_conversations`**: Stores conversation UUIDs, owning user ID, title, archive status, pinned status, draft text, and sync markers (`synced`, `pending_sync`, `sync_failed`).
- **`local_messages`**: Stores message UUIDs, parent message IDs (for tree branch exploration and edits), sender role (`user`, `assistant`), content, timestamps, and sync states.
- **`local_memories`**: Stores user-extracted personalization facts with confidence and importance scores.

---

### 3.4 Security & Network Layer

```mermaid
flowchart LR
    subgraph OkHttpClients ["OkHttp Client Architecture"]
        direction TB
        PUB["AuthApiClient (Public)\n- No Authorization Header\n- AppDns (IPv4/IPv6 Fallback)\n- CorrelationIdInterceptor\n- Timeouts: 60s Connect, 90s Read"]
        AUTH["AuthenticatedApiClient (Private)\n- AppDns & CorrelationIdInterceptor\n- AuthInterceptor (Bearer Access Token)\n- TokenAuthenticator (401 Mutex Interceptor)"]
    end

    subgraph StateAndStorage ["State & Key Management"]
        SESSION["SessionManager (StateFlow<AuthState>)\nStates: Initializing, Unauthenticated,\nAuthenticating, Authenticated,\nRefreshing, SessionExpired"]
        STORAGE["EncryptedTokenStorage\n- MasterKey (AES-256 GCM)\n- EncryptedSharedPreferences (AES-256 SIV)"]
    end

    PUB -->|Login / Register / Refresh| STORAGE
    AUTH -->|Read Access Token| STORAGE
    AUTH -.->|On 401: Acquire Mutex & Refresh| PUB
    PUB -.->|Update Tokens| SESSION
    SESSION -->|Sync Tokens| STORAGE
```

- **Render Cold-Start Protection**: Custom DNS provider (`AppDns`) and extended socket timeouts prevent false connection timeouts when the backend wakes up from hibernation.
- **Mutex-Protected Re-Authentication (`TokenAuthenticator`)**: When multiple parallel requests encounter a `401 Unauthorized`, a thread lock guarantees that **exactly one refresh request** is sent to `/api/v1/auth/refresh`. Other pending threads wait and retry with the newly minted access token. If refresh fails or tokens are revoked, the app transitions immediately to `AuthState.SessionExpired`.

---

### 3.5 UI & Feature Modules (Jetpack Compose)

1. **AI Workspace Screen (`AIWorkspaceScreen.kt`)**:
   - Dual-mode toggle (Online vs. Offline) with real-time model status indicators.
   - Streaming markdown bubbles, syntax highlighting, retry/regenerate controls, and message editing.
   - Persona selector (`Teacher`, `General`, `Explainable`).
2. **Landing & Student Mode Screen (`LandingScreen.kt`)**:
   - Mode switcher: **General Mode** (career paths, technology courses) vs. **Student Mode** (school curriculum, CBSE Class 10 subjects, chapter completion progress, upcoming exam countdowns, study focus recommendations).
3. **My Learning Screen (`MyLearningScreen.kt`)**:
   - Real-time tracking of in-progress and completed courses, module breakdowns, interactive lesson completion, and study streaks.
4. **Roadmap Explorer & Builder (`RoadmapScreen.kt`, `RoadmapBuilderScreen.kt`)**:
   - Interactive milestone maps with stage gates, progress indicators, and custom roadmap creation.
5. **Profile & Settings (`ProfileScreen.kt`, `SettingsScreen.kt`)**:
   - User profile customization, offline model management (download progress, storage cleanup, model deletion), and backend connectivity diagnostics.
6. **Authentication Screens (`LoginScreen.kt`, `RegisterScreen.kt`, `ForgotPasswordScreen.kt`)**:
   - Input validation, password visibility toggles, loading states, error toast/dialog feedback, and deep-link friendly password reset flow.

---

## 4. Cloud Backend Architecture & Implementation

### 4.1 Tech Stack & Tooling

| Component | Technology | Rationale |
|---|---|---|
| **Framework** | FastAPI (Python 3.11+) | High-performance asynchronous routing, automatic OpenAPI/Swagger docs |
| **ORM & Database** | SQLAlchemy 2.0 (AsyncIO) | Asynchronous query execution, strong relationship typing, dialect portability |
| **Database Engines** | PostgreSQL (Supabase) / SQLite | Production-grade cloud Postgres with SSL; zero-dependency SQLite for pytest |
| **Migrations** | Alembic | Tracked schema evolution with safe orphan inspection and downgrade scripts |
| **Security & JWT** | PyJWT + Passlib (Bcrypt) | Cryptographic token creation, constant-time hash comparisons |
| **Rate Limiting** | SlowAPI | In-memory token bucket for development; configurable Redis backend for production |
| **LLM Gateway** | HTTPX Async + OpenRouter API | Seamless streaming SSE connection to Google Gemini models with fallback |
| **Testing** | Pytest, Pytest-AsyncIO, Hypothesis | Unit, integration, and property-based correctness testing |

---

### 4.2 Database Relational Schema

```mermaid
erDiagram
    users ||--o{ refresh_tokens : owns
    users ||--o{ password_reset_tokens : owns
    users ||--o| user_profiles : has
    users ||--o{ user_course_progress : tracks
    users ||--o{ user_lesson_progress : tracks
    users ||--o{ conversations : owns
    users ||--o{ user_memories : stores
    users ||--o{ roadmaps : creates

    course_categories ||--o{ courses : categorizes
    courses ||--o{ course_modules : contains
    course_modules ||--o{ lessons : contains
    courses ||--o{ user_course_progress : relates_to
    lessons ||--o{ user_lesson_progress : relates_to

    roadmaps ||--o{ roadmap_items : contains
    conversations ||--o{ messages : contains

    users {
        string id PK
        string email UK
        string mobile UK
        string hashed_password
        boolean is_active
        boolean is_verified
        timestamp created_at
        timestamp updated_at
    }

    refresh_tokens {
        string id PK
        string user_id FK
        string token_hash UK
        string session_family_id
        timestamp expires_at
        boolean revoked
        timestamp created_at
    }

    user_profiles {
        string id PK
        string user_id UK
        string full_name
        string email UK
        string mobile
        string level
        string education_mode
        text interests
    }

    conversations {
        string id PK
        string user_id FK
        string title
        boolean is_archived
        boolean is_pinned
        string draft_text
        timestamp created_at
        timestamp updated_at
    }

    messages {
        string id PK
        string conversation_id FK
        string role
        text content
        string parent_id
        timestamp created_at
    }

    user_memories {
        string id PK
        string user_id FK
        string category
        text content
        float importance
        float confidence
        boolean active
        timestamp created_at
    }

    courses {
        string id PK
        string category_id FK
        string title
        string slug UK
        string level
        string status
        boolean is_featured
    }
```

---

### 4.3 Authentication & Session Security Flow

1. **Dual-Token System**:
   - **Access Token**: Short-lived (default 15 minutes) containing user ID, subject, and expiration.
   - **Refresh Token**: Long-lived (default 30 days) stored as a SHA-256 hash in the database, associated with a unique `session_family_id`.
2. **Cryptographic Token Rotation & Reuse Detection**:
   - Every call to `POST /api/v1/auth/refresh` revokes the old refresh token and issues a new pair inheriting the existing `session_family_id`.
   - **Replay Attack Defense**: If an already revoked refresh token is presented, the backend detects token theft and immediately **revokes all tokens within that session family**, terminating the attacker and compromised session.
3. **Session-Scoped Logout**:
   - Logging out with a specific refresh token revokes only that active session family, preserving logins on the user's other devices. Calling logout without a token revokes all active sessions.
4. **Password Reset Pipeline**:
   - Secure URL token generation with single-use revocation.
   - Configurable delivery abstraction (`PasswordResetDeliveryService`): logs tokens in development and dispatches via production email in live environments.

---

### 4.4 Cloud AI & Memory Subsystem

1. **OpenRouter & Google Gemini Integration (`openrouter_service.py`)**:
   - Primary model: `google/gemini-3.8-flash`.
   - Automatic fallback matrix: `google/gemini-2.5-flash`, `google/gemini-3.5-flash`.
   - History normalization: Sanitizes heterogeneous client history formats (Gemini parts vs OpenAI content structures).
   - Real-time token streaming over HTTP SSE (`/api/v1/chat/conversations/{id}/messages/stream`).
2. **Long-Term Contextual Memory Service (`memory_service.py`)**:
   - Real-time regex pattern scanner extracting high-signal user traits during normal conversation:
     - **Coding Preferences**: Detects languages (e.g., C++, Python, Kotlin, Rust) and adjusts coding outputs.
     - **Exam & Career Goals**: Extracts preparation targets (e.g., JEE, NEET, CBSE Class 10, interviews).
     - **Learning Profile**: Identifies difficulty preferences (Beginner/Advanced) and delivery styles (step-by-step, simple examples, analogies).
   - Extracted memories are scored for confidence/importance and automatically injected as system instructions in subsequent chat turns.

---

### 4.5 Backend API Endpoint Directory

| Route | Method | Description | Auth Required |
|---|---|---|---|
| `/api/v1/health` | GET | Service health check & AI provider status | Public |
| `/api/v1/auth/register` | POST | Register new student/user account | Public (Rate Limited) |
| `/api/v1/auth/login` | POST | Authenticate via email/mobile + password | Public (Rate Limited) |
| `/api/v1/auth/refresh` | POST | Rotate refresh token for new access token | Public (Rate Limited) |
| `/api/v1/auth/me` | GET | Retrieve authenticated user profile | Bearer JWT |
| `/api/v1/auth/logout` | POST | Revoke active session family or all sessions | Bearer JWT |
| `/api/v1/auth/forgot-password`| POST | Request password reset email / token | Public (Rate Limited) |
| `/api/v1/auth/reset-password` | POST / GET | Submit new password / View reset web page | Public |
| `/api/v1/profile` | GET / PUT | Fetch / update learner preferences and info | Bearer JWT |
| `/api/v1/courses` | GET | List catalog courses with category/level filter | Public |
| `/api/v1/courses/{id}` | GET | Course details, modules, and lessons | Public |
| `/api/v1/courses/in-progress` | GET | Active user courses with completion % | Bearer JWT |
| `/api/v1/courses/completed` | GET | Completed courses | Bearer JWT |
| `/api/v1/courses/{id}/enroll`| POST | Enroll in a course | Bearer JWT |
| `/api/v1/lessons/{id}/complete`| POST | Mark lesson complete & recalculate progress| Bearer JWT |
| `/api/v1/roadmaps` | GET | System and user roadmaps | Public |
| `/api/v1/roadmaps/{id}` | GET | Roadmap stages, milestones, and resources | Public |
| `/api/v1/chat/conversations` | GET / POST | List user conversations / Create new chat | Bearer JWT |
| `/api/v1/chat/conversations/{id}` | GET / PATCH / DELETE | Manage conversation (rename, pin, archive) | Bearer JWT |
| `/api/v1/chat/conversations/{id}/messages` | GET / POST | Message history / Send synchronous message | Bearer JWT |
| `/api/v1/chat/conversations/{id}/messages/stream` | POST | Real-time SSE AI message streaming | Bearer JWT |
| `/api/v1/chat/sync` | POST | Synchronize offline messages with cloud | Bearer JWT |
| `/api/v1/memory` | GET / POST / PATCH / DELETE | CRUD operations for user's long-term memory | Bearer JWT |

---

## 5. Current Implementation Progress & Completed Milestones

### 5.1 Completed Features & Enhancements

- [x] **Complete End-to-End Authentication Stack**:
  - Full backend auth pipeline with BCrypt, dual JWT, session family rotation, and replay detection.
  - Alembic migrations for users, refresh tokens, and password reset tokens with safe orphan profile reconciliation.
  - Android `EncryptedTokenStorage` and reactive `SessionManager`.
  - Android `AuthenticatedApiClient` with automatic OkHttp 401 re-authentication mutex.
  - Complete UI login, registration, and password reset screens.
- [x] **On-Device Native Edge AI Engine (`llama.cpp`)**:
  - Native C++ JNI bridge (`llama_jni.cpp`) with real-time token streaming and prompt cancellation.
  - Precompiled 64-bit native libraries (`arm64-v8a`, `x86_64`) packaged in APK.
  - Model lifecycle management (`OfflineModelManager`): 3-minute idle watchdog, persistent instance ID, and multi-turn KV cache reuse.
  - Hugging Face direct model download pipeline with network error diagnostics and disk space checks.
- [x] **Unified Multi-Engine Chat System**:
  - Seamless mode switching between Offline (Llama) and Online (Gemini via OpenRouter).
  - SSE streaming support in backend and Android client.
  - Tree-structured conversation support (parent message linking, branching, and editing).
  - Draft text auto-saving and pinned/archived conversation management.
- [x] **Adaptive Curriculum & Modes**:
  - **Student Mode**: CBSE Class 10/12 syllabus models, chapter completion tracking, and exam countdowns.
  - **General Mode**: Engineering and professional learning courses and roadmaps.
  - Three distinct AI tutoring personas: *Teacher*, *General*, and *Analytical/Explainable*.
- [x] **Long-Term Memory Extraction Engine**:
  - Automated extraction of user coding preferences, exams, and learning levels into persistent memory entities.
  - Dynamic prompt injection for personalized responses.
- [x] **Cloud Backend Hardening**:
  - Rate limiting on sensitive auth endpoints using SlowAPI.
  - Correlation ID middleware (`X-Correlation-ID`) across requests and logs.
  - Clean validation error formatting eliminating raw Pydantic tracebacks.
  - Resilient OpenRouter provider with Gemini fallback matrix.

---

### 5.2 Verification & Test Suite Status

```
============================= Pytest Backend Status =============================
Directory: backend/tests
Status: 49 PASSED (100% Passing)
Test Suites:
  - test_auth.py: Registration, login, JWT validation, refresh rotation, logout, rate limiting
  - test_auth_properties.py: Hypothesis property-based testing (normalization idempotency, rotation)
  - test_chat.py & test_chat_stream.py: Message creation, SSE streaming, ownership checks
  - test_conversations.py & test_chat_pagination.py: Multi-conversation list/detail, pagination
  - test_memory.py: Memory extraction, CRUD, and active filtering
  - test_openrouter_service.py: Provider payload normalization, fallbacks
  - test_profile_and_learning.py: Course enrollment, lesson completion, profile updates
  - test_health.py: Health check validation

============================= Android Gradle Status =============================
Directory: app/src/test
Command: .\gradlew.bat testDebugUnitTest
Status: BUILD SUCCESSFUL (100% Passing)
Test Suites:
  - TokenStorageTest & SessionManagerTest: Encrypted token handling and state machine
  - AuthInterceptorTest & TokenAuthenticatorTest: OkHttp header injection and 401 retry mutex
  - RemoteAuthRepositoryTest & AuthViewModelTest: Auth flow and UI event dispatching
  - OfflineModelLifecycleTest & OfflineModelStateTest: Local model loading and idle timeout
  - AIRepositoryTest & AIViewModelTest: Unified chat and streaming logic
  - DownloadFixTest: HuggingFace URL formatting and error categorization
```

---

## 6. Project Directory Structure

```
Offline First/
├── .kiro/                                # Project specification and task documents
│   └── specs/
│       ├── auth-end-to-end/             # Auth requirements, design, and task checklist
│       └── backend-connectivity-.../    # Connectivity & download fix specifications
├── app/                                  # Android Application (Kotlin + Compose + C++)
│   ├── build.gradle.kts                 # Android Gradle build configuration
│   └── src/
│       ├── main/
│       │   ├── cpp/                     # Native C++ JNI bridge
│       │   │   ├── CMakeLists.txt       # Native build rules
│       │   │   ├── include/             # llama.h, ggml.h headers
│       │   │   └── llama_jni.cpp        # JNI bridge with token callbacks & KV cache
│       │   ├── jniLibs/                 # Prebuilt .so libraries (arm64-v8a, x86_64)
│       │   └── java/com/offline_First/
│       │       ├── MainActivity.kt      # Main entry point & route orchestrator
│       │       ├── data/
│       │       │   ├── AppContainer.kt  # Manual dependency injection container
│       │       │   ├── SessionManager.kt# Centralized AuthState StateFlow machine
│       │       │   ├── local/           # Offline engine, model manager, SQLite cache
│       │       │   ├── provider/        # OfflineLlamaProvider & OnlineGeminiProvider
│       │       │   ├── remote/          # OkHttp clients, DTOs, interceptors, authenticators
│       │       │   └── repository/      # Repository implementations & mode routing
│       │       ├── domain/model/        # Kotlin domain models (Chat, Course, Roadmap, User)
│       │       └── ui/
│       │           ├── components/      # Common Compose UI components
│       │           ├── navigation/      # Screen destinations & routing
│       │           ├── screens/         # AI Workspace, Landing, Learning, Roadmaps, Auth
│       │           └── theme/           # Color, typography, dimens tokens
│       └── test/                        # Comprehensive Android unit test suite
├── backend/                              # Python Cloud Backend (FastAPI + SQLAlchemy)
│   ├── app/
│   │   ├── main.py                      # FastAPI app initialization, middleware, routes
│   │   ├── api/routes/                  # REST endpoints (auth, chat, courses, memory, etc.)
│   │   ├── core/                        # Config, async database, JWT security, rate limiting
│   │   ├── middleware/                  # Correlation ID tracking middleware
│   │   ├── models/                      # SQLAlchemy 2.0 ORM domain entities
│   │   ├── repositories/                # Clean data access repositories
│   │   ├── schemas/                     # Pydantic v2 validation models
│   │   └── services/                    # Business services (Auth, OpenRouter, Memory, Chat)
│   ├── migrations/                      # Alembic database migration scripts
│   ├── tests/                           # Pytest integration, unit, and property tests
│   └── requirements.txt                 # Backend Python dependencies
├── build.gradle.kts                     # Root Gradle build script
├── gradle.properties                    # Android build properties & backend URL configs
└── .env                                 # Environment configurations (local overrides)
```

---

## 7. Configuration & Deployment Guide

### 7.1 Backend Environment Configuration (`.env`)

```ini
# Environment
ENVIRONMENT=development                # development | production
ALLOW_DEV_USER_ID=false                # Must be false in production

# Database (PostgreSQL / Supabase)
DATABASE_URL=postgresql+asyncpg://user:password@host:5432/dbname

# JWT Security
JWT_SECRET_KEY=your-secure-32-byte-hex-secret
ACCESS_TOKEN_EXPIRE_MINUTES=15
REFRESH_TOKEN_EXPIRE_DAYS=30
RESET_TOKEN_EXPIRE_MINUTES=60

# AI Provider (OpenRouter / Gemini)
OPENROUTER_API_KEY=sk-or-v1-...
OPENROUTER_MODEL=google/gemini-3.8-flash

# Networking & Rate Limiting
CORS_ORIGINS=["http://10.0.2.2:8000","http://localhost:8000"]
REDIS_URL=redis://localhost:6379/0      # Optional: In-memory fallback used if unset
```

### 7.2 Running the Backend Locally

```bash
cd backend
# Activate virtual environment
.\.venv\Scripts\activate

# Apply migrations
alembic upgrade head

# Run development server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
- Interactive Swagger UI: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/api/v1/health`

### 7.3 Building & Running the Android App

In `gradle.properties`, configure the target backend:
```properties
# Cloud Production Backend (Render)
backendBaseUrl=https://edunova-backend-9waj.onrender.com

# Or Android Emulator targeting local backend:
# backendBaseUrl=http://10.0.2.2:8000
```

Execute Gradle builds with Android Studio JDK (Java 17/21):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug
```

---

## 8. Next Steps & Future Enhancements

1. **Two-Way Sync Engine & Conflict Resolution**:
   - Implement delta-based sync queue for offline chat messages, lesson completions, and progress states using vector clocks or last-write-wins timestamps.
2. **On-Device Vector Search (Local RAG)**:
   - Package small embedding models (e.g., MiniLM or BGE-Micro) and SQLite-Vec on Android to allow offline semantic search across downloaded textbook chapters and notes.
3. **Offline Course Media & Asset Caching**:
   - Enable background downloading and secure offline playback of course lesson videos, diagrams, and interactive quizzes.
4. **Hardware Acceleration (Vulkan / NPU / OpenCL)**:
   - Extend `llama.cpp` compilation to enable Vulkan GPU offloading on compatible Android chipsets (Snapdragon / Dimensity) to further accelerate tokens-per-second generation.
