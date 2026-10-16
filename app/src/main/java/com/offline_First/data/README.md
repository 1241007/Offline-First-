# Android Data Layer (`data/`)

This directory contains the implementations for local databases, native inference managers, encrypted credential vaults, OkHttp network clients, model providers, and domain repositories.

---

## Directory Architecture

```
data/
├── AppContainer.kt                 # Centralized manual dependency injection graph
├── SessionManager.kt               # Centralized reactive AuthState state machine
├── local/                          # Local on-device storage & native runtime
│   ├── ChatCacheDatabase.kt        # SQLiteOpenHelper for conversations, messages, memories
│   ├── SyllabusDatabase.kt         # SQLite database storing offline syllabus chunks
│   ├── E5EmbeddingService.kt       # On-device quantized E5 ONNX embedding service
│   ├── EncryptedTokenStorage.kt    # Android Keystore AES-256 GCM encrypted token storage
│   ├── InMemoryTokenStorage.kt     # In-memory implementation used for deterministic testing
│   ├── OfflineInferenceEngine.kt   # JNI bridge declaration wrapping llama_jni.cpp
│   ├── OfflineModelManager.kt      # Model lifecycle: 3-min watchdog & KV cache reuse
│   ├── OfflineSyllabusRepository.kt# Offline syllabus retrieval repository
│   └── MonotonicClock.kt           # Nanosecond monotonic timing for telemetry
├── provider/                       # LLM Provider abstractions
│   ├── LLMProvider.kt              # Common contract for streaming model providers
│   ├── OfflineLlamaProvider.kt     # llama.cpp edge provider (Qwen GGUF)
│   └── OnlineGeminiProvider.kt     # OpenRouter cloud provider (Google Gemini)
├── remote/                         # Network communication, DTOs & interceptors
│   ├── AuthApiClient.kt            # Unauthenticated OkHttp client (login/register/refresh)
│   ├── AuthenticatedApiClient.kt   # Authenticated OkHttp client (Bearer token injection)
│   ├── AuthInterceptor.kt          # Attaches Bearer JWT access tokens to requests
│   ├── TokenAuthenticator.kt       # Mutex-guarded 401 response interceptor for token rotation
│   ├── ChatApiClient.kt            # HTTP SSE streaming client for cloud chat
│   ├── SyllabusSyncClient.kt       # Client for syncing syllabus chunks from cloud
│   └── *Dtos.kt                    # Kotlinx serialization data transfer objects
└── repository/                     # Domain repository interfaces & implementations
    ├── ModeAwareAIRepository.kt    # Intelligent router dispatching between offline and online
    ├── RemoteAuthRepository.kt     # Auth orchestration (login, register, logout, session)
    ├── SyllabusSyncRepository.kt   # Manages syllabus chunk syncing and database upserts
    └── ...                         # Course, Learning, Roadmap, Profile repositories
```

---

## Core Subsystems

### 1. Dual-Engine Model Routing (`ModeAwareAIRepository`)
Acts as the central router for chat requests:
- If current mode is `AIExecutionMode.OFFLINE`, it directs the prompt to `LocalAIRepository` / `OfflineModelManager`.
- If current mode is `AIExecutionMode.ONLINE`, it directs the prompt to `OnlineAIRepository` / `ChatApiClient`.
- Automatically emits telemetry events tracking time-to-first-token and total generation latency.

### 2. Edge AI Lifecycle & KV Cache (`OfflineModelManager`)
- **Singleton Model Lifecycle**: Keeps model weights memory-mapped in native heap across turns to avoid expensive reload latency.
- **Idle Watchdog**: Runs a coroutine watchdog that automatically calls `nativeFree()` if no prompt is received for 3 minutes (`OFFLINE_MODEL_IDLE_TIMEOUT_MS = 180_000L`).
- **Resumable Downloads**: Downloads GGUF models directly from HuggingFace to app-private cache storage, verifying disk space and temporary file integrity before renaming.

### 3. Local SQLite Cache (`ChatCacheDatabase`)
- Implements tables for:
  - `local_conversations`: ID, user ID, title, archive state, pinned state, draft text, sync state.
  - `local_messages`: ID, parent ID (tree branching), role (`user`, `assistant`), content, timestamps.
  - `local_memories`: Learner personalization traits extracted from conversations.

### 4. Resilient Networking & 401 Mutex (`TokenAuthenticator`)
- When a 401 Unauthorized occurs on an authenticated endpoint:
  1. A thread lock (`Mutex`) is acquired so only one thread executes the refresh call.
  2. The refresh token is dispatched to `POST /api/v1/auth/refresh`.
  3. Upon success, new tokens are saved to `EncryptedTokenStorage` and pending requests retry with the new Bearer token.
  4. Upon failure or token revocation, the mutex notifies `SessionManager` to transition to `AuthState.SessionExpired`.
