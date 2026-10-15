# Implementation Tasks — EduNova End-to-End Authentication

## Task Status Legend
- `[ ]` Not started
- `[-]` In progress
- `[x]` Completed
- `[~]` Queued

---

## Phase 1: Backend Foundation

> Goal: Harden existing backend auth infrastructure without breaking anything.
> Verify: `pytest -v` passes after each sub-task.

- [x] 1.1 Fix `config.py` — remove hardcoded JWT secret default and add startup validation
  - File: `backend/app/core/config.py`
  - Change `jwt_secret_key` field: remove the default value `"edunova-dev-secret-key-32bytes-long-change-in-production!"` so the field has no default (requires `JWT_SECRET_KEY` env var)
  - Add a `@model_validator(mode='after')` that checks: if `environment != "development"` and `jwt_secret_key` matches the known placeholder value, raise `ValueError` with message "JWT_SECRET_KEY must be changed from the development placeholder before running in production"
  - Add `cors_origins: list[str]` setting loaded from env (default: `["http://10.0.2.2:8000", "http://localhost:8000", "http://127.0.0.1:8000"]`)
  - Acceptance criteria: Requirement 3.1, 3.2

- [x] 1.2 Write Alembic migration for auth tables — with safe orphan inspection
  - File: `backend/migrations/versions/<timestamp>_add_auth_tables.py`
  - Creates: `users`, `refresh_tokens` (with `session_family_id` column), `password_reset_tokens`
  - BEFORE adding the FK constraint on `user_profiles.user_id`, the migration MUST:
    1. Query all distinct `user_id` values in `user_profiles`
    2. Query all `id` values currently in `users`
    3. Compute the set of orphaned `user_profiles.user_id` values (in profiles but not in users)
    4. For each orphaned `user_id`, also query related rows in: `conversations` (currently no user_id column — note as N/A), `user_course_progress`, `user_lesson_progress`, `roadmaps` (user-owned rows where `user_id = orphan_id`), `roadmap_items` (via roadmap), and `user_profiles` itself
    5. Log a WARNING for each orphaned `user_id` listing the counts of related rows found
    6. If ANY orphaned `user_id` has NO safe mapping (cannot determine email from `user_profiles.email`), STOP the migration and raise an error listing the exact `user_id` values that require a human decision
    7. For orphaned profiles WHERE `user_profiles.email` is present and non-empty: INSERT a `users` row with `id = user_profiles.user_id`, `email = user_profiles.email`, `mobile = user_profiles.mobile` (if set), `hashed_password = '$MIGRATED_PLACEHOLDER$'` (a string that bcrypt will never match — not a valid hash prefix), `is_active = TRUE`, `is_verified = FALSE`. Do NOT invent email values. Do NOT generate a usable password hash.
    8. If `user_profiles.email` is NULL or empty for an orphaned profile, STOP and report those exact records
  - After all orphans are safely resolved, add FK constraint `user_profiles.user_id → users.id` ON DELETE CASCADE
  - Adds index `ix_refresh_tokens_session_family` on `refresh_tokens.session_family_id`
  - Must include a working `downgrade()` function that drops FK constraint before dropping `users` table
  - Run and verify: `cd backend && .venv\Scripts\alembic upgrade head`
  - Acceptance criteria: Requirements 28, 29, 30, 31

- [x] 1.3 Add `session_family_id` to `RefreshToken` model and update `AuthService`
  - File: `backend/app/models/user.py`
  - Add `session_family_id: Mapped[str] = mapped_column(String(36), nullable=False)` to `RefreshToken`
  - Add `Index("ix_refresh_tokens_session_family", "session_family_id")` to `__table_args__`
  - File: `backend/app/core/security.py`
  - Update `create_refresh_token(user_id, session_family_id=None)`: if `session_family_id` is None, generate a new UUID; otherwise use the provided value; return `(raw_token, token_hash, expires_at, session_family_id)`
  - File: `backend/app/services/auth_service.py`
  - Update `login`: generate `session_family_id = str(uuid.uuid4())` and pass to `create_refresh_token`; store in `RefreshToken`
  - Update `refresh_tokens`: pass existing `token_entry.session_family_id` to `create_refresh_token` so new token inherits the family
  - Update `refresh_tokens` reuse detection: when presenting a revoked token, revoke ALL tokens `WHERE session_family_id = token_entry.session_family_id` (not all user tokens)
  - Update `logout` method signature: `async def logout(self, user_id: str, refresh_token: str | None = None)`. If `refresh_token` provided, hash it and revoke only that specific token; otherwise revoke all tokens for the user. Note: current `logout()` only accepts `user_id` — this update adds the optional `refresh_token` parameter.
  - Update `register`: pass new `session_family_id` when creating initial `RefreshToken`
  - Acceptance criteria: Requirements 2.6–2.8, 13.1–13.2, 14.2–14.4

- [x] 1.4 Register auth router and update CORS in `main.py`
  - File: `backend/app/main.py`
  - Import and register `auth_router` with `app.include_router(auth_router)`
  - Update CORS `allow_headers` to include `"Authorization"` and `"X-Correlation-ID"`
  - Update CORS `allow_origins` to use `settings.cors_origins` list
  - Acceptance criteria: Requirements 35.2, 35.3

- [x] 1.5 Add structured error response model and exception handlers
  - File: `backend/app/core/errors.py` (new)
  - Define `AuthError(code: str, message: str)` dataclass and `AuthException(HTTPException)` subclass
  - Define all error codes as constants: `INVALID_CREDENTIALS`, `ACCOUNT_EXISTS`, etc.
  - File: `backend/app/main.py`
  - Register exception handler for `AuthException` returning `{"code": ..., "message": ...}`
  - Register handler for `RequestValidationError` to return `{"code": "VALIDATION_ERROR", "message": ...}` (no raw Pydantic internals)
  - Update `auth_service.py` to raise `AuthException` with appropriate codes instead of raw `HTTPException`
  - Acceptance criteria: Requirement 27.1–27.3

- [x] 1.6 Add rate limiting via `slowapi` with isolated storage configuration
  - File: `backend/requirements.txt` — add `slowapi==0.1.9`
  - File: `backend/app/core/rate_limit.py` (new) — isolate rate limit configuration:
    - Create a `get_limiter()` factory function that returns a `Limiter` configured with `key_func=get_remote_address`
    - Accept an optional `storage_uri: str | None` parameter: if `None`, use in-memory store (default for development); if provided (e.g., `REDIS_URL` env var), configure with Redis backend
    - Expose a module-level `limiter` instance created via `get_limiter(storage_uri=settings.redis_url if hasattr(settings, 'redis_url') else None)`
    - This isolates all storage/backend configuration so auth route logic imports only `from app.core.rate_limit import limiter` — no auth route changes needed when switching to Redis
  - File: `backend/app/main.py` — import `limiter` from `app.core.rate_limit`, attach to app state, add `SlowAPIMiddleware`
  - File: `backend/app/api/routes/auth.py` — add `@limiter.limit(...)` decorators: login 5/min, register 3/min, forgot-password 3/min, reset-password 5/min, refresh 10/min
  - Register `RateLimitExceeded` exception handler returning `{"code": "RATE_LIMITED", "message": "Too many requests."}`
  - Add `REDIS_URL` as an optional env var in `config.py` (default `None`) — no Redis connection is made unless the env var is set
  - Acceptance criteria: Requirements 11.1–11.2

- [x] 1.7 Add correlation ID middleware
  - File: `backend/app/middleware/correlation.py` (new)
  - Implement `CorrelationIdMiddleware(BaseHTTPMiddleware)`: reads `X-Correlation-ID` request header; generates UUID if absent; attaches to request state; adds to response headers; stores in `contextvars.ContextVar` for log use
  - File: `backend/app/main.py` — register the middleware
  - Update `auth_service.py` log calls to include `correlation_id` from context var
  - Acceptance criteria: Requirements 36.4, 37.1–37.2

- [x] 1.8 Add `PasswordResetDeliveryService` abstraction
  - File: `backend/app/services/password_reset_delivery.py` (new)
  - Define abstract base class `PasswordResetDeliveryService` with `async def deliver(self, user: User, raw_token: str) -> None`
  - Implement `DevPasswordResetDeliveryService`: logs token at DEBUG level in development; no-op in production
  - Inject into `AuthService.forgot_password` (pass delivery service as parameter or use dependency injection)
  - Remove inline `if settings.environment == "development"` token-return logic from `auth_service.py`; move it to `DevPasswordResetDeliveryService`
  - Acceptance criteria: Requirements 24.1–24.3

---

## Phase 2: Backend Route Protection

> Goal: Migrate profile, learning, and chat routes from dev bypass to real JWT auth.
> Verify: Existing public endpoints still work; protected endpoints return 401 without token.

- [x] 2.1 Update `profile.py` — replace `get_dev_user_id` with `get_current_user_id`
  - File: `backend/app/api/routes/profile.py`
  - Replace `from app.core.deps import get_dev_user_id` with `get_current_user_id`
  - Replace `user_id: str = Depends(get_dev_user_id)` with `Depends(get_current_user_id)` on both GET and PUT handlers
  - Acceptance criteria: Requirement 5.1, 7.1

- [x] 2.2 Update `learning.py` — replace `get_dev_user_id` with `get_current_user_id`
  - File: `backend/app/api/routes/learning.py`
  - Replace all 4 route handlers: `get_dev_user_id` → `get_current_user_id`
  - Acceptance criteria: Requirement 5.1, 7.2

- [x] 2.3 Add auth to `chat.py` routes and add conversations ownership migration
  - File: `backend/app/api/routes/chat.py`
  - Add `user_id: str = Depends(get_current_user_id)` to all 4 conversation route handlers and the send_message handler
  - **Ownership migration required in this iteration** — conversations currently have no `user_id` column (confirmed by inspecting migration `041d77f52987`). This means User A can currently access User B's conversations by guessing an ID. This MUST be fixed:
    1. Create a new Alembic migration that adds `user_id VARCHAR(36) NULL` to `conversations` (nullable initially for existing anonymous rows)
    2. Existing anonymous conversations: set `user_id = NULL` (they remain accessible only as "unowned" — do NOT delete them or reassign them to an invented user)
    3. Add index `ix_conversations_user_id` on `conversations.user_id`
    4. Update `ChatService.create_conversation(user_id: str)` to store the `user_id`
    5. Update `ChatService.list_conversations(user_id: str)` to filter `WHERE user_id = :user_id`
    6. Update `ChatService.get_conversation(conversation_id: str, user_id: str)` to verify ownership: if `conv.user_id != user_id` return 403 or 404
    7. Update `ChatService.send_message(conversation_id: str, user_id: str, ...)` to verify ownership before accepting message
  - User A MUST NOT be able to read or write to User B's conversation by changing the ID in the URL
  - If the existing `conversations` table has rows that would conflict with adding a NOT NULL FK immediately, use the nullable + phased approach above — do NOT silently drop existing conversation data
  - Acceptance criteria: Requirement 5.1, 6.3, 7.3

- [x] 2.4 Harden `get_current_user_id` — enforce ENVIRONMENT double-check
  - File: `backend/app/core/deps.py`
  - Update `get_current_user_id`: the dev bypass fallback MUST check `settings.environment == "development"` explicitly (in addition to `allow_dev_user_id`); reject with 401 if environment is not "development" regardless of `allow_dev_user_id` value
  - Same hardening for `get_dev_user_id`
  - Acceptance criteria: Requirements 4.1–4.3

---

## Phase 3: Backend Tests

> Goal: Comprehensive test coverage for all auth flows.
> Verify: `pytest -v` passes with zero failures.

- [x] 3.1 Write backend auth test suite
  - File: `backend/tests/test_auth.py`
  - Fixtures: reuse/extend existing `conftest.py` (in-memory SQLite session, TestClient)
  - Test groups:
    - **Registration**: valid, duplicate email, duplicate mobile, invalid email format, password < 8 chars
    - **Login**: valid email, valid mobile, wrong password, non-existent account, inactive account
    - **JWT validation**: valid token on /me, expired token, tampered signature, refresh token used as access token
    - **Refresh rotation**: valid rotation (new token pair returned, old token revoked), expired refresh, revoked refresh reuse (all same-family tokens revoked, other family tokens intact)
    - **Logout**: with refresh_token body (only that token revoked), without refresh_token body (all tokens revoked)
    - **Auth bypass security**: `?user_id=` rejected when `allow_dev_user_id=False`, rejected when `environment="production"`
    - **Cross-user access**: User A JWT cannot access User B's profile (returns 404 or 403)
    - **Forgot password**: generic message returned regardless of account existence, `devResetToken` present in dev, absent in production
    - **Reset password**: valid flow, expired token, already-used token, invalid token
    - **Rate limiting**: login returns 429 after 5 attempts (mock the limiter or use actual slowapi)
  - Acceptance criteria: Requirements 32, 33, 34

- [x] 3.2 Write property-based tests using `hypothesis`
  - File: `backend/tests/test_auth_properties.py`
  - Implement the correctness properties from `design.md`:
    - Property 1: Email normalization idempotency
    - Property 2: Case-insensitive login consistency
    - Property 3: Refresh token rotation — rotated token always returns 401
    - Property 4: JWT expiry rejection for any past timestamp
  - Use `hypothesis` strategies for email strings and timestamps
  - Acceptance criteria: Design document correctness properties

- [x] 3.3 Run full pytest suite and fix all failures
  - Command: `cd backend && .\.venv\Scripts\pytest -v`
  - Fix any issues found: SQLite dialect compatibility, fixture teardown, import errors
  - Ensure all existing tests (health, chat) still pass
  - Acceptance criteria: Requirement 43.1

---

## Phase 4: Android Token Storage and Session Manager

> Goal: Encrypted token persistence and centralized auth state machine.
> Verify: `./gradlew testDebugUnitTest --tests "com.offline_First.data.local.*"` passes.

- [x] 4.1 Add `security-crypto` dependency to `app/build.gradle.kts`
  - File: `app/build.gradle.kts`
  - Add to `dependencies`: use the current stable AndroidX Security Crypto release. As of this project's compile SDK 37 / min SDK 24, use `implementation("androidx.security:security-crypto:1.0.0")` which is the stable non-alpha release. Do NOT pin to `1.1.0-alpha06` or any alpha/beta version unless the existing project already uses alpha dependencies for other androidx libraries.
  - Verify the chosen version resolves without conflict against the project's existing BOM and Compose dependencies
  - Sync Gradle and verify no compilation errors
  - Acceptance criteria: Requirement 15.2

- [x] 4.2 Create `TokenStorage` interface
  - File: `app/src/main/java/com/offline_First/data/local/TokenStorage.kt`
  - Interface with: `saveTokens(accessToken: String, refreshToken: String)`, `getAccessToken(): String?`, `getRefreshToken(): String?`, `clearTokens()`, `hasTokens(): Boolean`
  - Acceptance criteria: Requirement 15.1

- [x] 4.3 Create `EncryptedTokenStorage` implementation
  - File: `app/src/main/java/com/offline_First/data/local/EncryptedTokenStorage.kt`
  - Uses `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`
  - Uses `EncryptedSharedPreferences.create(...)` with AES256_SIV key encryption and AES256_GCM value encryption
  - Stores keys: `KEY_ACCESS_TOKEN`, `KEY_REFRESH_TOKEN`
  - `clearTokens()` removes both keys atomically
  - Acceptance criteria: Requirements 15.2–15.5

- [x] 4.4 Create `InMemoryTokenStorage` for tests
  - File: `app/src/main/java/com/offline_First/data/local/InMemoryTokenStorage.kt`
  - Simple in-memory `MutableMap` implementation of `TokenStorage`
  - Thread-safe using `@Synchronized`
  - Acceptance criteria: Requirement 15.3

- [x] 4.5 Create `SessionManager`
  - File: `app/src/main/java/com/offline_First/data/SessionManager.kt`
  - Sealed class `AuthState`: `Initializing`, `Unauthenticated`, `Authenticating`, `Authenticated(userId: String, email: String, fullName: String)`, `Refreshing`, `SessionExpired`
  - `SessionManager` exposes `val authState: StateFlow<AuthState>`
  - `suspend fun initialize(authRepository: AuthRepository)`: reads tokens from storage; if no tokens → `Unauthenticated`; if tokens present → attempt refresh if access token is expired → then call `authRepository.fetchMe()` (GET /api/v1/auth/me) to obtain authoritative `UserDto` including `fullName` → transition to `Authenticated(userId, email, fullName)` from the `/me` response. Do NOT transition to `Authenticated` solely because the stored JWT has a valid `exp` claim — `fullName` is not in the JWT and must come from the `/me` endpoint.
  - If the `/me` call fails after a successful refresh: transition to `Unauthenticated` and clear tokens
  - `fun onLoginSuccess(accessToken: String, refreshToken: String, userId: String, email: String, fullName: String)`: saves tokens, transitions to `Authenticated` — fullName comes from the login/register response (already authoritative)
  - `suspend fun logout(refreshToken: String?)`: clears tokens, transitions to `Unauthenticated` (backend call is responsibility of `RemoteAuthRepository`)
  - `fun onSessionExpired()`: clears tokens, transitions to `SessionExpired`
  - `fun onTokensRefreshed(newAccessToken: String, newRefreshToken: String)`: updates storage, remains `Authenticated`
  - `fun currentRefreshToken(): String?`: reads from `tokenStorage.getRefreshToken()`
  - Note: networking (refresh HTTP call, /me HTTP call) remains in the repository/network layer — `SessionManager` receives results via callbacks/parameters, does not call HTTP directly. Avoid circular dependency: SessionManager does not construct or hold a reference to AuthRepository.
  - Acceptance criteria: Requirements 16.1–16.4, 17.1–17.3

- [x] 4.6 Write `TokenStorageTest` and `SessionManagerTest`
  - File: `app/src/test/java/com/offline_First/data/local/TokenStorageTest.kt`
  - Tests: save and retrieve tokens, clear removes both, `hasTokens()` returns correct value
  - Uses `InMemoryTokenStorage`
  - File: `app/src/test/java/com/offline_First/data/SessionManagerTest.kt`
  - Tests: initial state is `Initializing`, valid token → `Authenticated`, expired token triggers refresh callback, no tokens → `Unauthenticated`, `logout()` clears and transitions
  - Acceptance criteria: Requirements 32.1–32.2, 33.1

---

## Phase 5: Android Network Layer

> Goal: Public and authenticated OkHttp clients with correct interceptor separation.
> Verify: `./gradlew testDebugUnitTest --tests "com.offline_First.data.remote.*"` passes.

- [x] 5.1 Create `CorrelationIdInterceptor`
  - File: `app/src/main/java/com/offline_First/data/remote/CorrelationIdInterceptor.kt`
  - Implements `okhttp3.Interceptor`
  - Generates a new `UUID.randomUUID().toString()` per request
  - Adds `X-Correlation-ID` header to the request
  - Does NOT handle Authorization
  - Acceptance criteria: Requirements 37.3–37.5

- [x] 5.2 Create `AuthInterceptor`
  - File: `app/src/main/java/com/offline_First/data/remote/AuthInterceptor.kt`
  - Implements `okhttp3.Interceptor`
  - Constructor takes `tokenStorage: TokenStorage`
  - Reads `tokenStorage.getAccessToken()`; if non-null, adds `Authorization: Bearer <token>` header
  - If null, proceeds without header
  - Does NOT handle correlation IDs or any other concerns
  - Acceptance criteria: Requirements 19.1–19.3

- [x] 5.3 Create `TokenAuthenticator` with mutex-synchronized refresh
  - File: `app/src/main/java/com/offline_First/data/remote/TokenAuthenticator.kt`
  - Implements `okhttp3.Authenticator`
  - Constructor: `tokenStorage: TokenStorage`, `sessionManager: SessionManager`, `publicClient: () -> OkHttpClient` (lazy to avoid circular reference)
  - Uses `@Synchronized` or `ReentrantLock` to serialize concurrent 401 handling
  - Logic:
    1. If `response.priorResponse?.code == 401`, return null (do not retry again — prevents infinite loop)
    2. Lock mutex
    3. Check if `tokenStorage.getAccessToken()` has already changed (another thread may have refreshed) — if so, retry with new token without refreshing
    4. Perform `POST /api/v1/auth/refresh` using `publicClient` (not the authenticated client)
    5. On success: save new tokens via `tokenStorage`, call `sessionManager.onTokensRefreshed(...)`, retry request
    6. On failure: call `sessionManager.onSessionExpired()`, return null
    7. Unlock mutex
  - Acceptance criteria: Requirements 19.1–21.3

- [x] 5.4 Create `AuthDtos.kt`
  - File: `app/src/main/java/com/offline_First/data/remote/AuthDtos.kt`
  - `@Serializable` data classes matching backend camelCase aliases:
    - `LoginRequestDto(contact: String, password: String)`
    - `RegisterRequestDto(fullName: String, email: String, mobile: String?, password: String)`
    - `RefreshTokenRequestDto(refreshToken: String)`
    - `UserDto(id: String, email: String, mobile: String?, fullName: String)`
    - `AuthTokensResponseDto(accessToken: String, refreshToken: String, tokenType: String, expiresIn: Int, user: UserDto)`
    - `ForgotPasswordRequestDto(contact: String)`
    - `ResetPasswordRequestDto(token: String, newPassword: String)`
    - `AuthMessageResponseDto(message: String)`
    - `ForgotPasswordResponseDto(message: String, devResetToken: String? = null)`
    - `LogoutRequestDto(refreshToken: String? = null)` (used in request body for `POST /logout`)
    - `ApiErrorDto(code: String, message: String)`
  - Acceptance criteria: Requirements 27.4, 28.4

- [x] 5.5 Create `AuthApiClient` (public, no auth)
  - File: `app/src/main/java/com/offline_First/data/remote/AuthApiClient.kt`
  - Builds `OkHttpClient` with: `AppDns`, `CorrelationIdInterceptor`, Render cold-start timeouts (60s connect, 90s read), `retryOnConnectionFailure(true)`
  - Does NOT add `AuthInterceptor` or `TokenAuthenticator`
  - Provides suspend functions: `login(dto)`, `register(dto)`, `refresh(dto)`, `forgotPassword(dto)`, `resetPassword(dto)`, `logout(dto: LogoutRequestDto?): Result<Unit>`
  - `logout` sends `POST /api/v1/auth/logout` with the current access token in the `Authorization: Bearer` header (pass the raw access token as a parameter since this client has no AuthInterceptor). If `dto.refreshToken` is provided, include it in the request body.
  - All return `Result<T>` using `runCatching`
  - Parses `ApiErrorDto` from non-2xx responses and wraps in appropriate exception
  - Acceptance criteria: Requirements 18.1, 18.3, 22.1

- [x] 5.6 Create `AuthenticatedApiClient`
  - File: `app/src/main/java/com/offline_First/data/remote/AuthenticatedApiClient.kt`
  - Builds `OkHttpClient` with: `AppDns`, `CorrelationIdInterceptor`, `AuthInterceptor`, `TokenAuthenticator`, Render cold-start timeouts, `retryOnConnectionFailure(true)`
  - Provides the `OkHttpClient` instance for use by profile, learning, and chat repositories
  - Acceptance criteria: Requirements 18.2, 17.3

- [x] 5.7 Write `AuthInterceptorTest` and `TokenAuthenticatorTest`
  - File: `app/src/test/java/com/offline_First/data/remote/AuthInterceptorTest.kt`
  - Tests: token added when present, no header when absent, header is exactly "Bearer <token>"
  - Uses `InMemoryTokenStorage`, `MockWebServer`
  - File: `app/src/test/java/com/offline_First/data/remote/TokenAuthenticatorTest.kt`
  - Tests: 401 triggers refresh, successful refresh retries request with new token, failed refresh clears session, concurrent 401s trigger only one refresh call
  - Acceptance criteria: Requirements 33.5–33.6

---

## Phase 6: Android Repository and ViewModel

> Goal: Wire the real `RemoteAuthRepository` and update `AuthViewModel` to use `SessionManager`.
> Verify: `./gradlew testDebugUnitTest --tests "com.offline_First.ui.screens.auth.*"` passes.

- [x] 6.1 Update `AuthRepository` interface
  - File: `app/src/main/java/com/offline_First/data/repository/AuthRepository.kt`
  - Add methods:
    - `suspend fun signIn(contact: String, password: String): Result<Unit>` (keep existing)
    - `suspend fun register(input: RegistrationInput): Result<Unit>` (keep existing)
    - `suspend fun requestPasswordReset(contact: String): Result<Unit>` (keep existing)
    - `suspend fun logout(refreshToken: String?): Result<Unit>` (new)
    - `suspend fun restoreSession(): Result<Boolean>` (new — returns true if session restored)
    - `suspend fun fetchMe(): Result<UserDto>` (new — calls GET /api/v1/auth/me; used by SessionManager.initialize() to get authoritative user data including fullName)
    - `fun observeAuthState(): Flow<AuthState>` (new)
  - Import `AuthState` from `SessionManager`
  - Acceptance criteria: Requirements 16.1, 17.1, 22.1

- [x] 6.2 Create `RemoteAuthRepository`
  - File: `app/src/main/java/com/offline_First/data/repository/RemoteAuthRepository.kt`
  - Constructor: `authApiClient: AuthApiClient`, `authenticatedApiClient: AuthenticatedApiClient`, `sessionManager: SessionManager`, `tokenStorage: TokenStorage`
  - `signIn`: calls `authApiClient.login(...)`, on success calls `sessionManager.onLoginSuccess(...)` with fullName from response
  - `register`: calls `authApiClient.register(...)`, on success calls `sessionManager.onLoginSuccess(...)` with fullName from response
  - `logout(refreshToken)`: calls `authApiClient.logout(accessToken = tokenStorage.getAccessToken(), dto = LogoutRequestDto(refreshToken))` best-effort (fire and ignore network failure); always calls `sessionManager.logout(refreshToken)` to clear local state
  - `restoreSession()`: calls refresh if access token is expired (via `authApiClient.refresh(...)`) → then calls `fetchMe()` to get authoritative user → calls `sessionManager.onLoginSuccess(...)` or `sessionManager.onSessionExpired()` based on result. Returns `Result.success(true)` if session restored, `Result.success(false)` if no tokens, `Result.failure(...)` if unrecoverable error.
  - `fetchMe()`: calls GET /api/v1/auth/me using the authenticated client, returns `Result<UserDto>`
  - `observeAuthState()`: returns `sessionManager.authState`
  - `requestPasswordReset`: delegates to `authApiClient.forgotPassword(...)`
  - Acceptance criteria: Requirements 16, 17, 22, 23

- [x] 6.3 Update `BackendNotConfiguredRepositories` to match new `AuthRepository` interface
  - File: `app/src/main/java/com/offline_First/data/repository/BackendNotConfiguredRepositories.kt`
  - Add stub implementations for `logout`, `restoreSession`, `observeAuthState`, `fetchMe`
  - `logout` → `unavailable("Authentication")`
  - `restoreSession` → `Result.success(false)`
  - `observeAuthState` → `flowOf(AuthState.Unauthenticated)`
  - `fetchMe` → `Result.failure(UnsupportedOperationException("Backend not configured"))`
  - Acceptance criteria: Requirement 41.1 (backward compatibility)

- [x] 6.4 Update `AppContainer` to wire real implementations
  - File: `app/src/main/java/com/offline_First/data/AppContainer.kt`
  - In `initialize(context)`:
    - Create `EncryptedTokenStorage(context)`
    - Create `SessionManager(tokenStorage)`
    - Create `AuthApiClient()`
    - Create `RemoteAuthRepository(authApiClient, sessionManager, tokenStorage)`
    - Create `AuthenticatedApiClient(tokenStorage, sessionManager)`
    - Expose `authRepository: AuthRepository`, `sessionManager: SessionManager`, `authenticatedApiClient`
  - Acceptance criteria: Requirements 1.3, 1.4

- [x] 6.5 Update `AuthViewModel` to wire `SessionManager` state
  - File: `app/src/main/java/com/offline_First/ui/screens/auth/AuthViewModel.kt`
  - Update `signIn` and `register` operations: on success they now return `Result<Unit>` (same interface) but `SessionManager` state will update reactively
  - Emit `AuthEvent.LoginSucceeded` when signIn or register returns success (existing behavior preserved)
  - No redesign of UI flow — only fix any broken compilation after interface changes
  - Acceptance criteria: Requirement 42.1

- [x] 6.6 Write `AuthViewModelTest` and `AuthRepositoryTest`
  - File: `app/src/test/java/com/offline_First/ui/screens/auth/AuthViewModelTest.kt`
  - Tests: login success emits `LoginSucceeded` event, login failure sets error state, register success emits message, double-submit prevention (isSubmitting guard)
  - Uses `InMemoryTokenStorage`, fake `AuthRepository`
  - File: `app/src/test/java/com/offline_First/data/repository/RemoteAuthRepositoryTest.kt`
  - Tests: login maps response to session, register sends correct body, network failure returns `Result.failure`, logout clears session even when network fails
  - Uses `MockWebServer`, `InMemoryTokenStorage`
  - Acceptance criteria: Requirements 33.3–33.4

---

## Phase 7: Android App Integration

> Goal: Wire session state into `MainActivity` and auth screens; handle `Initializing` state.
> Verify: `./gradlew assembleDebug` succeeds; manual smoke test on emulator.

- [x] 7.1 Update `MainActivity` to observe `SessionManager.authState` and handle `Initializing`
  - File: `app/src/main/java/com/offline_First/MainActivity.kt`
  - In `onCreate`, call `AppContainer.authRepository.restoreSession()` inside `LaunchedEffect(Unit)`
  - Observe `AppContainer.sessionManager.authState.collectAsState()`
  - Add `AppDestination.LOADING` state (or use a boolean `isInitializing`) that shows a blank/splash screen until `Initializing` resolves
  - When `authState` transitions to `SessionExpired`, navigate to `AppDestination.LOGIN`
  - `isLoggedIn` should be derived from `authState is AuthState.Authenticated`
  - Logout now calls `AppContainer.authRepository.logout(AppContainer.sessionManager.currentRefreshToken)`
  - Do NOT redesign existing screen routing
  - Acceptance criteria: Requirements 17.4, 42.3–42.4

- [x] 7.2 Wire `LoginScreen` and `RegisterScreen` to `SessionManager` token storage
  - File: `app/src/main/java/com/offline_First/ui/screens/auth/LoginScreen.kt`
  - `AuthEvent.LoginSucceeded` remains the navigation trigger (existing behavior)
  - `SessionManager` state update happens automatically via `RemoteAuthRepository.signIn`
  - Add proper loading state: disable Login button when `uiState.isSubmitting == true`
  - File: `app/src/main/java/com/offline_First/ui/screens/auth/RegisterScreen.kt`
  - Same: after successful register, emit `LoginSucceeded` event
  - Do NOT redesign screen layout or components
  - Acceptance criteria: Requirements 42.1–42.2

- [x] 7.3 Wire `ForgotPasswordScreen` to `AuthViewModel`
  - File: `app/src/main/java/com/offline_First/ui/screens/auth/ForgotPasswordScreen.kt`
  - Ensure `requestPasswordReset` is wired to `viewModel.requestPasswordReset(contact)`
  - Show loading indicator while submitting
  - Acceptance criteria: Requirement 42.1

- [x] 7.4 Ensure logout passes refresh token for session-scoped revocation
  - File: `app/src/main/java/com/offline_First/data/SessionManager.kt`
  - `fun currentRefreshToken(): String?` is defined here (see Task 4.5) — reads from `tokenStorage.getRefreshToken()`
  - File: `app/src/main/java/com/offline_First/MainActivity.kt`
  - Update logout handler to call `authRepository.logout(sessionManager.currentRefreshToken())`
  - Acceptance criteria: Requirement 22.1, 14.3

---

## Phase 8: End-to-End Verification

> Goal: Confirm the entire stack works against real backend and database.
> Do not claim completion until real devices/emulators communicate with deployed backend.

- [x] 8.1 Run full backend test suite
  - Command: `cd backend && .\.venv\Scripts\pytest -v`
  - Expected: all tests pass including new `test_auth.py` and `test_auth_properties.py`
  - Fix any remaining failures before proceeding

- [x] 8.2 Run Android unit tests
  - Command: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`
  - Expected: `TokenStorageTest`, `SessionManagerTest`, `AuthViewModelTest`, `AuthRepositoryTest`, `AuthInterceptorTest`, `TokenAuthenticatorTest` all pass

- [x] 8.3 Run Android build compilation
  - Command: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
  - Expected: APK builds without errors or warnings related to auth integration

- [x] 8.4 Verify Alembic migration
  - Command: `cd backend && .\.venv\Scripts\alembic upgrade head`
  - Verify against real Supabase DB that all tables exist and FK is correct
  - Verify existing `user_profiles` data was preserved (no orphaned records)

- [x] 8.5 Manual end-to-end verification
  - Start backend: `cd backend && .\.venv\Scripts\uvicorn app.main:app --port 8000`
  - Verify Swagger UI at `http://localhost:8000/docs`: all 7 auth endpoints documented
  - Register a new user → receive access + refresh tokens
  - Login → receive tokens
  - GET /me with Bearer token → returns user info
  - POST /refresh → returns new token pair, old token rejected
  - POST /logout (with refresh_token) → only current session revoked
  - Android build and auth test suite validated (`assembleDebug` and tests passing)

- [x] 8.6 Production readiness checklist
  - Verify Render environment variables are set: `JWT_SECRET_KEY`, `DATABASE_URL`, `ENVIRONMENT`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `REFRESH_TOKEN_EXPIRE_DAYS`, `RESET_TOKEN_EXPIRE_MINUTES`
  - Verify `ALLOW_DEV_USER_ID` is `false` or unset on Render
  - Verify `.env` is in `.gitignore`
  - Verify no secrets exist in git history
  - Confirm production startup fails if JWT_SECRET_KEY is placeholder
  - Acceptance criteria: Requirement 44.1–44.4
