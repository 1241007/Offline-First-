# Design Document — EduNova End-to-End Authentication

## Overview

This document describes the technical design for the EduNova authentication system, connecting
the Android Compose client to the FastAPI/PostgreSQL backend via a production-grade, layered auth
architecture.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Android                              │
│                                                             │
│  LoginScreen / RegisterScreen / ForgotPasswordScreen        │
│                        │                                    │
│                   AuthViewModel                             │
│                        │                                    │
│                  AuthRepository ◄── RemoteAuthRepository    │
│                        │                                    │
│                  SessionManager ──► TokenStorage            │
│                   (state machine)   (Keystore-backed)       │
│                        │                                    │
│         ┌──────────────┴──────────────────┐                 │
│  PublicApiClient               AuthenticatedApiClient       │
│  + CorrelationIdInterceptor    + CorrelationIdInterceptor   │
│  + AppDns + retry              + AuthInterceptor            │
│                                + TokenAuthenticator         │
│                                + AppDns + retry             │
└─────────────────────────────────────────────────────────────┘
                         │ HTTPS
┌─────────────────────────────────────────────────────────────┐
│                        FastAPI                               │
│                                                             │
│  CorrelationIdMiddleware                                    │
│  RateLimiting (slowapi)                                     │
│  StructuredErrorHandlers                                    │
│                                                             │
│  /api/v1/auth/*  (public)    /api/v1/profile  (JWT)        │
│  /api/v1/health  (public)    /api/v1/learning/* (JWT)      │
│  /api/v1/courses (public)    /api/v1/chat/*    (JWT)       │
│  /api/v1/roadmaps (public)                                  │
│                                                             │
│  AuthService  ProfileService  LearningService  ChatService  │
│                        │                                    │
│                   AsyncSession (SQLAlchemy)                 │
└─────────────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────────────┐
│            PostgreSQL / Supabase                             │
│                                                             │
│  users   refresh_tokens   password_reset_tokens             │
│  user_profiles   conversations   messages                   │
│  courses   roadmaps   user_course_progress                  │
└─────────────────────────────────────────────────────────────┘
```

---

## What Already Exists vs What Needs Building

### Backend — Already Built

| Component | File | Status |
|---|---|---|
| User / RefreshToken / PasswordResetToken models | `models/user.py` | ✅ Complete |
| Security utils (bcrypt, JWT, token generation) | `core/security.py` | ✅ Complete |
| AuthService (register, login, refresh, logout, forgot-pw, reset-pw, me) | `services/auth_service.py` | ⚠️ Needs updates (logout scoping, session_family_id) |
| Auth route handlers | `api/routes/auth.py` | ⚠️ Not registered in main.py |
| JWT dependencies (get_current_user, get_current_user_id) | `core/deps.py` | ⚠️ Needs ENVIRONMENT double-check hardening |
| Auth request/response schemas | `schemas/auth.py` | ✅ Complete |
| All auth endpoints | all 7 exist | ⚠️ Auth router not yet registered in main.py |

### Backend — Needs to be Built / Modified

1. **Alembic migration** — create `users`, `refresh_tokens` (with `session_family_id`), `password_reset_tokens` tables + FK from `user_profiles.user_id → users.id`
2. **`config.py`** — remove hardcoded JWT secret default; fail fast on startup if JWT_SECRET_KEY is placeholder in non-development env
3. **`auth_service.py`** — add `session_family_id` logic; update `logout` for single-session vs all-sessions
4. **`main.py`** — register auth router; add `Authorization` to CORS `allow_headers`
5. **`profile.py` routes** — replace `get_dev_user_id` → `get_current_user_id`
6. **`learning.py` routes** — replace `get_dev_user_id` → `get_current_user_id`
7. **`chat.py` routes** — add auth dependency; scope conversations to authenticated user
8. **Rate limiting** — add `slowapi` to auth endpoints
9. **Structured error handlers** — consistent `{"code": "...", "message": "..."}` responses
10. **Correlation ID middleware** — per-request UUID, log attachment, response header
11. **`PasswordResetDeliveryService`** — abstraction for future email/SMS delivery
12. **Backend test suite** — `backend/tests/test_auth.py`

### Android — Already Built

| Component | File | Status |
|---|---|---|
| AuthViewModel | `ui/screens/auth/AuthViewModel.kt` | ⚠️ Wired to stub; needs SessionManager |
| LoginScreen / RegisterScreen / ForgotPasswordScreen | `ui/screens/auth/*.kt` | ⚠️ UI exists; needs loading state + session wiring |
| MainActivity | `MainActivity.kt` | ⚠️ Needs Initializing state handling |
| ChatApiClient | `data/remote/ChatApiClient.kt` | ✅ Do NOT modify |
| AppDns | `data/remote/AppDns.kt` | ✅ Reuse |

### Android — Needs to be Built / Modified

1. `TokenStorage` interface + `EncryptedTokenStorage` + `InMemoryTokenStorage` (tests)
2. `SessionManager` — auth state machine, token lifecycle
3. `CorrelationIdInterceptor` — shared by PublicApiClient + AuthenticatedApiClient
4. `AuthInterceptor` — Authorization header only
5. `TokenAuthenticator` — 401 handling, mutex-synchronized refresh
6. `AuthDtos.kt` — serialization DTOs
7. `AuthApiClient` — public OkHttp client (no auth)
8. `AuthenticatedApiClient` — OkHttp client with interceptors
9. `RemoteAuthRepository` — implements `AuthRepository`
10. Update `AuthRepository` interface — add `logout`, `restoreSession`, `observeAuthState`
11. Update `BackendNotConfiguredRepositories` — match new interface
12. Update `AppContainer` — wire real implementations
13. Update `AuthViewModel` — wire `SessionManager` state
14. Update `MainActivity` — observe `SessionManager.authState`, handle `Initializing`
15. Add `security-crypto` dependency to `app/build.gradle.kts`
16. Android test suite

---

## Database Migration Strategy

### Current Schema State (confirmed from existing migrations)

The latest migration (`fd5a2e32997d`) created:
- `user_profiles`: `user_id` String(255) UNIQUE — **no FK to `users`**
- `conversations`, `messages`: no `user_id` column
- `user_course_progress`, `user_lesson_progress`: `user_id` String(255) — no FK

The auth tables (`users`, `refresh_tokens`, `password_reset_tokens`) exist in SQLAlchemy models
but have **no Alembic migration** — they do not exist in the database yet.

### Migration Plan

**Migration: `add_auth_tables_and_session_family`**

Step-by-step, in a single migration file:

1. Create `users` table (safe — does not exist):
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile VARCHAR(50) UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX ix_users_email ON users(email);
CREATE UNIQUE INDEX ix_users_mobile ON users(mobile) WHERE mobile IS NOT NULL;
```

2. Create `refresh_tokens` table with `session_family_id`:
```sql
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    session_family_id VARCHAR(36) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE UNIQUE INDEX ix_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX ix_refresh_tokens_session_family ON refresh_tokens(session_family_id);
```

3. Create `password_reset_tokens` table:
```sql
CREATE TABLE password_reset_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_prt_user_id ON password_reset_tokens(user_id);
CREATE UNIQUE INDEX ix_prt_token_hash ON password_reset_tokens(token_hash);
```

4. Handle existing `user_profiles` data (CRITICAL):

Before adding FK constraint, the migration must handle the fact that existing `user_profiles.user_id` values are arbitrary strings (dev UUIDs) that have no corresponding rows in `users`.

**Strategy**: The migration will:
- Query all distinct `user_id` values in `user_profiles` not yet in `users`
- For each orphaned profile, also count related rows in `user_course_progress`, `user_lesson_progress`, and user-owned `roadmaps`
- Log a WARNING for each orphaned ID with related row counts
- If any orphaned `user_profiles` row has a NULL or empty `email` column, STOP and raise an error naming the exact records — do not proceed
- For orphaned profiles where `email` is present: INSERT a `users` row using `id = user_profiles.user_id`, `email = user_profiles.email`, `mobile = user_profiles.mobile` (nullable), `hashed_password = '$MIGRATED_PLACEHOLDER$'` (not a valid bcrypt hash — can never be used to authenticate), `is_active = TRUE`, `is_verified = FALSE`
- Do NOT invent email values; do NOT generate a usable password hash; do NOT create any account with a predictable password
- Migrated users will need to go through "forgot password" to set a real password

5. Add FK constraint from `user_profiles.user_id → users.id`:
```sql
ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
```

### Existing Conversation Ownership

`conversations` currently has no `user_id`. The design decision for this iteration:
- Chat routes will require authentication (JWT)
- The `user_id` will be extracted from the JWT in the route handler
- A **future migration** will add `user_id` to `conversations` and backfill/scope existing records
- In this iteration, `ChatService` will track conversations per authenticated user at the query level once the column is added
- Document this as a known gap: existing anonymous conversations are not deleted, but new conversations will be user-scoped

---

## Token Family / Session Design

```
POST /login
  → generate new session_family_id (UUID)
  → create RefreshToken(session_family_id=X, revoked=False)
  → return raw_token

POST /refresh (valid token)
  → find token by hash → revoke old token
  → create new RefreshToken(session_family_id=X, same family, revoked=False)
  → return new raw_token

POST /refresh (reused/revoked token — THEFT DETECTED)
  → find token by hash → already revoked
  → revoke ALL tokens WHERE session_family_id=X (that device's session chain only)
  → return 401 SESSION_REVOKED
  → log: token_reuse_detected, user_id, session_family_id

POST /logout (with refresh_token in body)
  → revoke ONLY that specific token
  → other sessions on other devices remain active

POST /logout (no refresh_token in body)
  → revoke ALL tokens for user (logout-all-sessions)
```

This ensures that if one device's token is stolen and reused, only that device's session is
terminated — not the user's phone, tablet, and laptop simultaneously.

---

## HTTP Client Architecture (Android)

```
AppDns (shared DNS resolver)
   │
   ├── PublicApiClient
   │     OkHttpClient.Builder()
   │       .dns(AppDns)
   │       .connectTimeout(60s)  ← Render cold-start
   │       .readTimeout(90s)
   │       .addInterceptor(CorrelationIdInterceptor)
   │       .retryOnConnectionFailure(true)
   │     Endpoints: /auth/*, /health, /courses, /roadmaps
   │
   └── AuthenticatedApiClient
         OkHttpClient.Builder()
           .dns(AppDns)
           .connectTimeout(60s)
           .readTimeout(90s)
           .addInterceptor(CorrelationIdInterceptor)
           .addInterceptor(AuthInterceptor)
           .authenticator(TokenAuthenticator)
           .retryOnConnectionFailure(true)
         Endpoints: /profile, /learning/*, /chat/*
```

**ChatApiClient is not modified.** It remains the dedicated client for AI chat, with its own
DNS retry and timeout configuration.

---

## Interceptor Responsibilities

| Interceptor | Responsibility | Used By |
|---|---|---|
| `CorrelationIdInterceptor` | Generate + attach `X-Correlation-ID` UUID header | Both clients |
| `AuthInterceptor` | Attach `Authorization: Bearer <token>` header ONLY | AuthenticatedApiClient |
| `TokenAuthenticator` | Handle 401: refresh token, retry once, or clear session | AuthenticatedApiClient |

---

## Token Refresh Concurrency (Android)

The `TokenAuthenticator` uses a `kotlinx.coroutines.sync.Mutex` (or `@Synchronized` on the JVM
OkHttp thread) to serialize refresh calls:

```
Request A → 401
Request B → 401  (arrives while A is refreshing)
Request C → 401  (arrives while A is refreshing)

TokenAuthenticator:
  mutex.lock()
    if (tokenStorage.getAccessToken() != staleToken):
        // Another thread already refreshed — reuse new token
        mutex.unlock(); return request with new token
    // First to lock — perform refresh
    newTokens = publicClient.POST /auth/refresh
    tokenStorage.saveTokens(newTokens)
  mutex.unlock()
  Requests B and C retry with new token (no additional refresh call)
```

---

## Rate Limiting Strategy

**Library**: `slowapi` (wraps `limits` library, integrates with FastAPI)

**Development**: In-memory store (default)
**Production**: Redis-backed store via `REDIS_URL` environment variable (future; document steps)

| Endpoint | Limit |
|---|---|
| POST /login | 5 requests / minute / IP |
| POST /register | 3 requests / minute / IP |
| POST /forgot-password | 3 requests / minute / IP |
| POST /reset-password | 5 requests / minute / IP |
| POST /refresh | 10 requests / minute / IP |

On limit exceeded → HTTP 429 `{"code": "RATE_LIMITED", "message": "Too many requests. Please try again later."}`

---

## Structured Error Response Contract

All backend errors follow this structure:

```json
{"code": "ERROR_CODE", "message": "Human-readable description"}
```

| Code | HTTP Status | Trigger |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Wrong password, non-existent account |
| `ACCOUNT_EXISTS` | 409 | Duplicate email or mobile on register |
| `INVALID_TOKEN` | 401 | Malformed or tampered JWT |
| `TOKEN_EXPIRED` | 401 | JWT or refresh token past expiry |
| `SESSION_REVOKED` | 401 | Refresh token revoked (logout or reuse) |
| `VALIDATION_ERROR` | 422 | Pydantic validation failure |
| `RATE_LIMITED` | 429 | Rate limit exceeded |
| `ACCOUNT_DISABLED` | 403 | `is_active = False` |
| `PASSWORD_RESET_INVALID` | 400 | Invalid reset token |
| `PASSWORD_RESET_EXPIRED` | 400 | Expired reset token |

A custom `AuthException` class and FastAPI exception handler will ensure all auth errors produce
this structure. Pydantic 422 errors will be re-formatted to match.

---

## Endpoint Access Control Table

| Endpoint | Method | Access | Notes |
|---|---|---|---|
| `/api/v1/health` | GET | Public | Always accessible |
| `/api/v1/auth/register` | POST | Public | Creates user + profile |
| `/api/v1/auth/login` | POST | Public | Returns JWT pair |
| `/api/v1/auth/refresh` | POST | Public | Uses refresh token |
| `/api/v1/auth/forgot-password` | POST | Public | No account enumeration |
| `/api/v1/auth/reset-password` | POST | Public | Validated by token hash |
| `/api/v1/auth/me` | GET | Authenticated | Returns user + profile |
| `/api/v1/auth/logout` | POST | Authenticated | Revokes session token(s) |
| `/api/v1/courses` | GET | Public | Course catalog |
| `/api/v1/courses/{id}` | GET | Public | Course detail |
| `/api/v1/roadmaps` | GET | Public | System roadmaps |
| `/api/v1/roadmaps/{id}` | GET | Public | Roadmap detail |
| `/api/v1/roadmaps/categories` | GET | Public | Roadmap categories |
| `/api/v1/profile` | GET | Authenticated | Scoped to JWT user |
| `/api/v1/profile` | PUT | Authenticated | Scoped to JWT user |
| `/api/v1/learning/courses/in-progress` | GET | Authenticated | Scoped to JWT user |
| `/api/v1/learning/courses/completed` | GET | Authenticated | Scoped to JWT user |
| `/api/v1/learning/courses/{id}/enroll` | POST | Authenticated | Scoped to JWT user |
| `/api/v1/learning/lessons/{id}/complete` | POST | Authenticated | Scoped to JWT user |
| `/api/v1/chat/conversations` | POST | Authenticated | Future: user-scoped |
| `/api/v1/chat/conversations` | GET | Authenticated | Future: user-scoped |
| `/api/v1/chat/conversations/{id}` | GET | Authenticated | Future: user-scoped |
| `/api/v1/chat/conversations/{id}/messages` | POST | Authenticated | Future: user-scoped |

---

## SessionManager State Machine (Android)

```
         ┌─────────────────┐
         │   Initializing  │  ← App start
         └────────┬────────┘
                  │
         stored tokens?
         /               \
       Yes                No
        │                  │
   token valid?       ┌────▼────────────┐
   /         \        │ Unauthenticated │◄─── logout / session cleared
 Yes          No      └─────────────────┘
  │            │
  │     try refresh
  │      /        \
  │   success    fail
  │     │          │
  ▼     ▼          ▼
┌──────────┐  ┌────────────────┐
│Authenticated│  │ SessionExpired │
│(userId,    │  │ (tokens cleared)│
│accessToken)│  └────────────────┘
└──────────┘
     │
  Refreshing ← TokenAuthenticator 401 handling
     │
  Returns to Authenticated or SessionExpired
```

---

## Password Reset Delivery Abstraction

```python
class PasswordResetDeliveryService(ABC):
    @abstractmethod
    async def deliver(self, user: User, raw_token: str) -> None: ...

class DevPasswordResetDeliveryService(PasswordResetDeliveryService):
    # Returns token in response body only when ENVIRONMENT == "development"
    # No-op in production (token is NOT returned)

# Future production implementations:
class EmailPasswordResetDeliveryService(PasswordResetDeliveryService):
    # Send via SendGrid / AWS SES
    # Requires: SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD env vars

class SmsPasswordResetDeliveryService(PasswordResetDeliveryService):
    # Send via Twilio / AWS SNS
    # Requires: SMS_PROVIDER_KEY env var
```

---

## Email Verification — Future Design

`is_verified` is retained in the `users` table. Login is NOT blocked on `is_verified` in this
iteration. To enable verification enforcement in a future iteration:

1. Add `POST /api/v1/auth/send-verification` endpoint
2. Add `GET /api/v1/auth/verify-email?token=...` endpoint
3. Store verification token hash in a new `email_verification_tokens` table
4. Update login to check `is_verified` and return `ACCOUNT_UNVERIFIED` error
5. The schema already supports this without a migration

---

## API Versioning

All endpoints are under `/api/v1/`. Future version introduction:

1. Create `backend/app/api/v2/` directory with new route handlers
2. Register the new router with prefix `/api/v2/` in `main.py` alongside `/api/v1/`
3. `/api/v1/` remains active until explicitly sunset
4. Android clients target a specific version; version negotiation is handled at the client level

---

## Real-Time Auth Readiness (WebSocket / SSE)

Future WebSocket connections can authenticate using the existing JWT:

```
# Option 1: Token in upgrade URL query parameter
wss://api.edunova.com/ws/chat?token=<access_token>

# Option 2: Token in first WebSocket message (handshake message)
{ "type": "auth", "token": "<access_token>" }
```

`decode_access_token()` in `core/security.py` is a stateless pure function — it can be called
from any WebSocket connection handler without modification. No database round-trip is needed for
access token validation.

---

## CORS Configuration

```python
# Development
allow_origins = [
    "http://10.0.2.2:8000",  # Android emulator
    "http://localhost:8000",
    "http://127.0.0.1:8000",
]

# Production (from env var CORS_ORIGINS, comma-separated)
allow_origins = settings.cors_origins  # e.g., ["https://app.edunova.com"]

allow_headers = ["Content-Type", "Accept", "Authorization", "X-Correlation-ID"]
allow_credentials = False  # tokens in Authorization header, not cookies
```

---

## Correctness Properties

### Property 1: Email Normalization is Idempotent
**Validates: Requirement 9.1**
```
For any email string e:
    normalize(normalize(e)) == normalize(e)
```
Any email normalized twice produces the same result as normalizing once.

### Property 2: Case-Insensitive Login Consistency
**Validates: Requirement 9.1, 9.3**
```
For any valid registered email:
    login(email.upper()) succeeds iff login(email.lower()) succeeds
```

### Property 3: Refresh Token Rotation — No Reuse
**Validates: Requirement 13.1**
```
For any refresh token T that has been successfully rotated:
    POST /refresh with T → always returns 401 SESSION_REVOKED
```
A rotated token is permanently invalid.

### Property 4: JWT Expiry Rejection
**Validates: Requirement 2.1**
```
For any JWT with exp <= now():
    decode_access_token(jwt) → None
```
Any token with a past expiry is always rejected.

### Property 5: Token Hash Uniqueness
**Validates: Requirement 2.2**
```
For any two distinct raw tokens t1 != t2:
    hash_token(t1) != hash_token(t2)
```
SHA-256 collision resistance guarantees this in practice.

### Property 6: Session Family Scoping
**Validates: Requirement 2.8, 13.2**
```
For a user with active sessions S1 (family F1) and S2 (family F2):
    Reuse token from S1 → revokes all tokens in F1, S2 remains active
```
Token theft on one device does not log out other devices.

### Property 7: Rate Limiting Monotonicity
**Validates: Requirement 11.1**
```
For n requests within 1 minute from the same IP (n > limit):
    first (limit) requests → 200 or 4xx (valid responses)
    remaining requests → 429
```
