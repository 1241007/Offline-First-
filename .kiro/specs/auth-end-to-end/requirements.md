# Requirements Document

## Introduction

EduNova requires a complete, production-hardened end-to-end authentication system connecting its
Android (Kotlin/Compose) client to its FastAPI/PostgreSQL backend. Significant backend
infrastructure already exists: JWT security utilities, User/RefreshToken/PasswordResetToken models,
all auth endpoints (`/register`, `/login`, `/refresh`, `/logout`, `/me`, `/forgot-password`,
`/reset-password`), and an `AuthService`. The main gaps are:

1. Android-side implementation (token storage, session management, authenticated HTTP client, real `AuthRepository`)
2. Migration of protected backend routes away from the insecure `?user_id=` dev-bypass pattern to real JWT auth
3. Securing and hardening existing backend code (CORS, structured errors, rate limiting, env validation)
4. Comprehensive testing of both sides
5. Database migration for any schema changes
6. Observability and correlation ID infrastructure

This spec covers all gaps from end to end while preserving existing working features (AI chat,
offline AI, course catalog, roadmap, profile UI).

---

## Glossary

- **AuthService**: FastAPI service class (`backend/app/services/auth_service.py`) implementing all auth business logic
- **AuthRepository**: Android interface (`AuthRepository.kt`) for auth operations; currently stubbed
- **SessionManager**: Android component that owns the canonical auth state machine and token lifecycle
- **TokenStorage**: Android interface for encrypted persistence of tokens; backed by Android Keystore-backed secure encrypted storage (e.g., `EncryptedSharedPreferences` where supported)
- **AuthInterceptor**: OkHttp interceptor that injects `Authorization: Bearer` headers on authenticated requests
- **TokenAuthenticator**: OkHttp `Authenticator` that handles 401 responses by refreshing tokens and retrying
- **PublicApiClient**: OkHttp client for unauthenticated endpoints (auth, health, public catalog)
- **AuthenticatedApiClient**: OkHttp client with `AuthInterceptor` and `TokenAuthenticator` for protected endpoints
- **AccessToken**: Short-lived JWT (15–30 min) encoding `sub` (user_id), `email`, `role`, `type=access`
- **RefreshToken**: Long-lived cryptographically random token (30 days), stored hashed in PostgreSQL, sent raw to client
- **DevBypass**: The `?user_id=` query-parameter mechanism, permitted only when `ENVIRONMENT==development` AND `allow_dev_user_id==True`
- **CorrelationId**: A UUID generated per-request to link Android logs with backend logs
- **PasswordResetDeliveryService**: Abstraction over password-reset token delivery (currently dev-only in-response; production = email/SMS)
- **Alembic**: Python database migration tool used for all schema changes
- **EARS**: Easy Approach to Requirements Syntax (Ubiquitous / Event-driven / State-driven / Unwanted-event / Optional / Complex)

---

## Requirements

### Requirement 1: System Architecture

**User Story:** As a developer, I want a clearly layered auth architecture, so that each component
has a single responsibility and can be tested and replaced independently.

#### Acceptance Criteria

1. THE System SHALL implement auth as a layered stack: Android UI → AuthViewModel → AuthRepository → SessionManager → (PublicApiClient | AuthenticatedApiClient) → FastAPI AuthService → PostgreSQL
2. THE Android auth layer SHALL be independent from the existing `ChatApiClient`; no auth logic shall be added to `ChatApiClient`
3. THE System SHALL provide a `PublicApiClient` (no auth headers) for unauthenticated endpoints and an `AuthenticatedApiClient` (with interceptor and authenticator) for protected endpoints
4. WHEN a new protected API endpoint is added in the future, THE AuthenticatedApiClient SHALL automatically authenticate it without requiring changes to the calling repository

---

### Requirement 2: JWT Token Design

**User Story:** As a security engineer, I want short-lived access tokens paired with stateful
refresh tokens, so that token compromise has limited blast radius and stolen refresh tokens can be
revoked.

#### Acceptance Criteria

1. THE System SHALL issue access tokens that expire within 15–30 minutes of issuance
2. THE System SHALL issue refresh tokens that are cryptographically random (minimum 48 bytes of entropy), stored only as a SHA-256 hash in PostgreSQL, and expire after 30 days
3. THE RefreshToken model SHALL associate each token with a specific user via a foreign key and SHALL support per-device identification for future multi-device token management
4. WHEN a refresh token is presented, THE AuthService SHALL look up its SHA-256 hash in the database, never the raw value
5. THE System SHALL never store or log the raw refresh token value after it has been sent to the client
6. THE `refresh_tokens` table SHALL include a `session_family_id` column (String(36), UUID) that links all tokens rotated from the same original login into a single token family
7. WHEN a new login occurs, THE AuthService SHALL generate a new `session_family_id` UUID and assign it to the initial refresh token; all subsequent rotations from that token SHALL inherit the same `session_family_id`
8. WHEN a revoked refresh token is presented (token reuse detected), THE AuthService SHALL revoke ALL tokens sharing the same `session_family_id`, minimizing impact on other active devices/sessions belonging to the same user

---

### Requirement 3: JWT Secret and Environment Configuration

**User Story:** As an operator, I want all secrets loaded exclusively from environment variables, so
that no secret is ever committed to the repository or present in code defaults.

#### Acceptance Criteria

1. THE System SHALL load `JWT_SECRET_KEY` from the environment exclusively; it SHALL NOT have a valid production default in code
2. WHEN `ENVIRONMENT` is not `"development"` AND `JWT_SECRET_KEY` matches the known development placeholder value, THE Application SHALL fail fast at startup with a descriptive error
3. THE System SHALL load all secret values (`DATABASE_URL`, `JWT_SECRET_KEY`, `GEMINI_API_KEY`) from environment variables via `pydantic-settings`
4. THE `.env` file SHALL be listed in `.gitignore` and SHALL NOT be committed to the repository

---

### Requirement 4: Dev-Only User ID Bypass

**User Story:** As a backend developer, I want a safe development-mode shortcut for testing without
a full Android client, so that I can iterate quickly without introducing a production security hole.

#### Acceptance Criteria

1. THE `get_current_user_id` dependency SHALL accept the `?user_id=` query parameter ONLY WHEN `settings.environment == "development"` AND `settings.allow_dev_user_id == True`; BOTH conditions are required
2. IF `settings.environment != "development"` OR `settings.allow_dev_user_id == False`, THEN THE System SHALL reject any request using `?user_id=` with HTTP 401
3. THE `get_dev_user_id` dependency SHALL also enforce both conditions and raise HTTP 400 when either is not met
4. THE automated test suite SHALL include at least one test that confirms the `?user_id=` bypass is rejected when `allow_dev_user_id=False`
5. THE automated test suite SHALL include at least one test that confirms the `?user_id=` bypass is rejected when `environment="production"`

---

### Requirement 5: Protected Route Identity Derivation

**User Story:** As a security engineer, I want all protected routes to derive user identity from the
JWT exclusively, so that a client cannot impersonate another user by supplying a query parameter.

#### Acceptance Criteria

1. THE routes `/api/v1/profile`, `/api/v1/learning/*`, `/api/v1/chat/conversations*`, and `/api/v1/chat/conversations/*/messages` SHALL use `get_current_user_id` (JWT-based) as their auth dependency
2. IF a request to a protected route omits the `Authorization: Bearer` header AND the dev bypass is disabled, THEN THE System SHALL return HTTP 401
3. THE `get_current_user` and `get_current_user_id` dependencies SHALL derive user identity exclusively from the JWT `sub` claim; no client-supplied user identifier SHALL be trusted for identity

---

### Requirement 6: Endpoint Access Control Classification

**User Story:** As a security engineer, I want all API endpoints explicitly classified as public or
authenticated, so that there is no ambiguity about which routes require a valid JWT.

#### Acceptance Criteria

1. THE following endpoints SHALL be PUBLIC (no authentication required): `/api/v1/health`, `/api/v1/courses`, `/api/v1/courses/{id}`, `/api/v1/roadmaps`, `/api/v1/roadmaps/{id}`, `/api/v1/roadmaps/categories`, `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`
2. THE following endpoints SHALL be AUTHENTICATED (require a valid JWT via `Authorization: Bearer`): `/api/v1/auth/me`, `/api/v1/auth/logout`, `/api/v1/profile` (GET and PUT), `/api/v1/learning/courses/in-progress`, `/api/v1/learning/courses/completed`, `/api/v1/learning/courses/{id}/enroll`, `/api/v1/learning/lessons/{id}/complete`, `/api/v1/chat/conversations` (POST and GET), `/api/v1/chat/conversations/{id}` (GET), `/api/v1/chat/conversations/{id}/messages` (POST)
3. THE conversation endpoints SHALL be migrated to require authentication; conversations SHALL be scoped to the authenticated user via a `user_id` foreign key in a future migration (conversations currently have no `user_id` — this transition SHALL be documented in the design and handled safely to preserve existing anonymous conversations)
4. No endpoints SHALL be classified as "admin-only" in this iteration of the spec

---

### Requirement 7: Data Ownership Enforcement

**User Story:** As a user, I want to be certain that another logged-in user cannot access or modify
my data, so that my profile, conversations, and learning progress remain private.

#### Acceptance Criteria

1. WHEN a user requests their profile, THE ProfileService SHALL scope the database query to the authenticated user's ID from the JWT
2. WHEN a user requests their learning progress, THE LearningService SHALL scope the query to the authenticated user's ID
3. WHEN a user creates or reads conversations and messages, THE ChatService SHALL scope all conversation lookups to the authenticated user's ID
4. THE automated test suite SHALL include a cross-user data access test confirming that User A cannot retrieve User B's profile by substituting a different user ID

---

### Requirement 8: User and Profile Data Models

**User Story:** As a developer, I want a clean separation between authentication data and profile
data, so that the auth layer can be updated without touching profile business logic.

#### Acceptance Criteria

1. THE `users` table SHALL store authentication fields: `id` (UUID), `email`, `mobile`, `hashed_password`, `is_active`, `is_verified`, `created_at`, `updated_at`
2. THE `user_profiles` table SHALL store profile fields linked to `users.id` via a foreign key with `ON DELETE CASCADE`
3. THE `user_profiles.user_id` column SHALL reference `users.id` (not a separate identity) and SHALL have a database-level `UNIQUE` constraint and a `NOT NULL` foreign key constraint
4. THE System SHALL generate all user IDs on the backend; the client SHALL NOT supply or influence the user ID

---

### Requirement 9: Email and Mobile Normalization

**User Story:** As a user, I want to log in with my email regardless of case, so that
`User@Example.com` and `user@example.com` are treated as the same account.

#### Acceptance Criteria

1. WHEN a user registers or logs in with an email address, THE AuthService SHALL normalize the email by trimming whitespace and converting to lowercase before any database operation
2. WHEN a user registers or logs in with a mobile number, THE AuthService SHALL normalize the mobile by trimming whitespace before any database operation
3. THE same normalization logic SHALL be applied consistently across `/register`, `/login`, and `/forgot-password`
4. THE database SHALL enforce email uniqueness at the column level via a `UNIQUE` constraint (already present); mobile uniqueness SHALL also be enforced at the column level

---

### Requirement 10: Password Security

**User Story:** As a user, I want my password to be stored securely and never exposed, so that a
database breach does not directly compromise my account.

#### Acceptance Criteria

1. THE AuthService SHALL hash passwords using bcrypt before storing them
2. THE System SHALL enforce a minimum password length of 8 characters at the API schema level (Pydantic)
3. THE System SHALL NEVER include plaintext passwords in log output, HTTP responses, error messages, or analytics
4. THE Android client SHALL NEVER store the user's password after a successful authentication; only tokens SHALL be persisted
5. THE System SHALL NEVER transmit passwords in URL query parameters

---

### Requirement 11: Rate Limiting

**User Story:** As an operator, I want sensitive auth endpoints protected from brute-force and
enumeration attacks, so that the system remains secure under automated attack.

#### Acceptance Criteria

1. THE System SHALL apply rate limiting to `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`, and `/api/v1/auth/refresh`
2. WHEN a client exceeds the rate limit, THE System SHALL return HTTP 429 with a structured error body containing `code: "RATE_LIMITED"`
3. THE design document SHALL specify the chosen rate-limiting strategy and library (e.g., `slowapi`)

---

### Requirement 12: Login Security — No Account Enumeration

**User Story:** As a security engineer, I want the login endpoint to return identical error messages
for all failure modes, so that attackers cannot determine whether an email address is registered.

#### Acceptance Criteria

1. WHEN a login attempt fails for any reason (wrong password, non-existent account, disabled account), THE AuthService SHALL return HTTP 401 with the generic message "Invalid email/mobile number or password"
2. THE login error message SHALL be identical whether the account does not exist or the password is incorrect
3. THE `/forgot-password` endpoint SHALL return an identical success message regardless of whether the provided contact matches a registered account

---

### Requirement 13: Refresh Token Rotation

**User Story:** As a security engineer, I want each refresh operation to issue a new refresh token
and revoke the old one, so that refresh token theft has limited persistence.

#### Acceptance Criteria

1. WHEN a valid refresh token is presented to `/api/v1/auth/refresh`, THE AuthService SHALL atomically revoke the old token and issue a new access token and refresh token pair; the new refresh token SHALL inherit the same `session_family_id` as the revoked token
2. WHEN a refresh token that has already been revoked is presented (token reuse detected), THE AuthService SHALL return HTTP 401 with `code: "SESSION_REVOKED"` and SHALL revoke ALL remaining active refresh tokens sharing the same `session_family_id` (not all tokens for the user, to preserve other concurrent device sessions)
3. WHEN an expired refresh token is presented, THE AuthService SHALL return HTTP 401 with `code: "TOKEN_EXPIRED"`

---

### Requirement 14: Multi-Device Session Support

**User Story:** As a user, I want to log in from multiple devices simultaneously, so that logging in
on my phone does not invalidate my session on my tablet.

#### Acceptance Criteria

1. THE System SHALL support multiple concurrent active refresh tokens per user (one per device/session)
2. THE `POST /api/v1/auth/logout` endpoint SHALL accept an optional `refresh_token` body field
3. WHEN the `refresh_token` body field is provided, THE AuthService SHALL revoke ONLY that specific token (current-session logout), leaving all other sessions active
4. WHEN no `refresh_token` body field is provided, THE AuthService SHALL revoke ALL refresh tokens for the user (logout-all-sessions behaviour)
5. THE future `POST /api/v1/auth/logout-all` endpoint SHALL be documented in the design as a planned extension for explicit all-sessions logout
6. THE `refresh_tokens` table SHALL store sufficient information to support future per-device token management (e.g., device identifier column can be added via migration)

---

### Requirement 15: Android Token Storage

**User Story:** As an Android developer, I want tokens stored in encrypted storage, so that tokens
cannot be extracted from a non-rooted device.

#### Acceptance Criteria

1. THE Android app SHALL define a `TokenStorage` interface with methods: `saveTokens(accessToken, refreshToken)`, `getAccessToken(): String?`, `getRefreshToken(): String?`, `clearTokens()`
2. THE production implementation SHALL use Android Keystore-backed secure encrypted storage; the specific implementation SHOULD use `EncryptedSharedPreferences` (Jetpack Security Crypto `androidx.security:security-crypto`) on devices where it is supported
3. THE implementation SHALL gracefully handle devices or test environments where hardware-backed Keystore is unavailable (e.g., Robolectric unit tests) by falling back to a safe alternative (e.g., `InMemoryTokenStorage`); this fallback SHALL NOT be used on real devices
4. THE Android app SHALL NEVER store tokens in plaintext `SharedPreferences`, files, or databases
5. WHEN tokens are cleared (logout or session expiration), THE TokenStorage SHALL remove both the access token and refresh token atomically

---

### Requirement 16: Android SessionManager

**User Story:** As an Android developer, I want a centralized session manager, so that all parts of
the app react consistently to auth state changes without duplicating logic.

#### Acceptance Criteria

1. THE `SessionManager` SHALL expose an observable auth state with the following states: `Initializing`, `Unauthenticated`, `Authenticated(userId, accessToken)`, `Refreshing`, `SessionExpired`
2. WHEN the app starts, THE `SessionManager` SHALL read stored tokens, validate the access token, and transition to `Authenticated` or `Unauthenticated` without showing a loading flicker beyond an `Initializing` state
3. WHEN a session expires and cannot be refreshed, THE `SessionManager` SHALL transition to `SessionExpired` and clear all stored tokens
4. WHEN logout is requested, THE `SessionManager` SHALL transition to `Unauthenticated` and clear all stored tokens regardless of whether the backend logout call succeeds

---

### Requirement 17: App Start Session Restoration

**User Story:** As a user, I want the app to automatically restore my session after I close and
reopen it, so that I am not forced to log in every time.

#### Acceptance Criteria

1. WHEN the Android app launches, THE SessionManager SHALL check for a stored access token and attempt session restoration before displaying any authenticated content
2. IF a stored access token is found but expired, THEN THE SessionManager SHALL attempt a token refresh before transitioning to `Unauthenticated`
3. IF no stored tokens are found, THEN THE SessionManager SHALL transition directly to `Unauthenticated`
4. THE `MainActivity` SHALL observe the `SessionManager` state and navigate to `LoginScreen` only after `Initializing` resolves to `Unauthenticated` or `SessionExpired`, preventing premature navigation

---

### Requirement 18: HTTP Client Architecture

**User Story:** As an Android developer, I want two separate HTTP clients (public and
authenticated), so that auth concerns are isolated and the refresh flow cannot cause infinite loops.

#### Acceptance Criteria

1. THE Android app SHALL create a `PublicApiClient` (plain OkHttp, no auth interceptor) for endpoints: `/api/v1/auth/*`, `/api/v1/health`, `/api/v1/courses`, `/api/v1/roadmaps`
2. THE Android app SHALL create an `AuthenticatedApiClient` with an `AuthInterceptor` and a `TokenAuthenticator` for protected endpoints: `/api/v1/profile`, `/api/v1/learning/*`, `/api/v1/chat/*`
3. THE `/api/v1/auth/refresh` call WITHIN the `TokenAuthenticator` SHALL use the `PublicApiClient`, never the `AuthenticatedApiClient`, to prevent infinite refresh loops

---

### Requirement 19: Auth Interceptor

**User Story:** As an Android developer, I want token injection to happen automatically in the HTTP
layer, so that repository code never manually manages tokens.

#### Acceptance Criteria

1. THE `AuthInterceptor` SHALL read the current access token from `TokenStorage` and add `Authorization: Bearer <token>` to every request passing through the `AuthenticatedApiClient`
2. IF no access token is stored when the interceptor runs, THEN THE interceptor SHALL proceed without adding an Authorization header (the server will return 401, triggering the authenticator)
3. WHEN a request is intercepted, THE AuthInterceptor SHALL NOT call the backend to validate the token; it SHALL add the stored token unconditionally

---

### Requirement 20: Token Refresh Concurrency

**User Story:** As an Android developer, I want concurrent 401 responses to trigger only one refresh
call, so that the refresh token is not rotated multiple times causing subsequent requests to fail.

#### Acceptance Criteria

1. WHEN multiple authenticated requests receive HTTP 401 simultaneously, THE `TokenAuthenticator` SHALL ensure only one refresh API call is made; remaining requests SHALL wait and reuse the new token
2. THE refresh coordination SHALL be implemented using a thread-safe mechanism (e.g., `Mutex` or `synchronized`)
3. WHEN the refresh completes successfully, ALL waiting requests SHALL automatically retry with the new access token

---

### Requirement 21: Retry Policy

**User Story:** As a user, I want the app to transparently handle expired access tokens, so that I
am not interrupted by token expiration during normal use.

#### Acceptance Criteria

1. WHEN a protected request receives HTTP 401, THE `TokenAuthenticator` SHALL attempt a token refresh and retry the original request exactly once
2. IF the token refresh fails, THEN THE `TokenAuthenticator` SHALL clear the session, transition `SessionManager` to `SessionExpired`, and NOT retry the original request
3. THE `TokenAuthenticator` SHALL NEVER retry a request more than once per token refresh cycle to prevent infinite retry loops

---

### Requirement 22: Logout

**User Story:** As a user, I want logout to be complete and reliable, so that my session is
terminated even if the network is temporarily unavailable.

#### Acceptance Criteria

1. WHEN a user initiates logout, THE Android app SHALL call `POST /api/v1/auth/logout` and SHALL include the current refresh token in the request body to revoke only the current session
2. WHEN the backend logout call completes (success or failure), THE SessionManager SHALL clear local tokens and transition to `Unauthenticated`; backend failure SHALL NOT leave local credentials in place
3. WHEN logout completes, THE Android app SHALL clear any in-memory caches related to the user's data and navigate to the Login screen

---

### Requirement 23: Forgot Password Flow

**User Story:** As a user, I want to reset my password without revealing whether my account exists,
so that the reset flow does not expose my account registration status.

#### Acceptance Criteria

1. THE `/api/v1/auth/forgot-password` endpoint SHALL always return HTTP 200 with the same generic message regardless of whether the contact matches a registered account
2. WHEN `settings.environment == "development"`, THE response MAY include a `devResetToken` field containing the raw reset token to facilitate testing without an email/SMS provider
3. WHEN `settings.environment != "development"`, THE response SHALL NOT include the `devResetToken` field
4. THE reset token SHALL expire within 15 minutes of issuance

---

### Requirement 24: Password Reset Delivery Abstraction

**User Story:** As an operator, I want a clean abstraction for password-reset delivery, so that
switching from dev-only to production email/SMS delivery requires minimal changes.

#### Acceptance Criteria

1. THE backend SHALL define a `PasswordResetDeliveryService` interface with a single method `deliver(user, raw_token)`
2. THE current implementation SHALL be `DevPasswordResetDeliveryService`, which is a no-op in production and returns the token in the response only in development
3. THE design document SHALL document the steps required to replace `DevPasswordResetDeliveryService` with a production implementation (e.g., SendGrid, AWS SES)

---

### Requirement 25: Email Verification — Future Ready

**User Story:** As an operator, I want the data model to support email verification in the future,
so that we can enforce verification without a schema migration.

#### Acceptance Criteria

1. THE `users` table SHALL retain the `is_verified` boolean column (already present)
2. THE System SHALL NOT block login based on `is_verified` status until a future requirement explicitly enables enforcement
3. THE design document SHALL describe how email verification enforcement would be added in a future iteration

---

### Requirement 26: API Versioning

**User Story:** As an API consumer, I want versioned API endpoints, so that I can upgrade to new
API versions without breaking existing clients.

#### Acceptance Criteria

1. THE System SHALL serve all auth endpoints under `/api/v1/auth/*`
2. THE design document SHALL describe how a future `/api/v2/` can be introduced without removing `/api/v1/`

---

### Requirement 27: Structured Error Contracts

**User Story:** As an Android developer, I want consistent, machine-readable error responses from
the backend, so that the app can display appropriate messages and handle errors programmatically.

#### Acceptance Criteria

1. THE backend SHALL return all error responses in the form `{"code": "<CODE>", "message": "<human-readable>"}`
2. THE backend SHALL use the following error codes: `INVALID_CREDENTIALS`, `ACCOUNT_EXISTS`, `INVALID_TOKEN`, `TOKEN_EXPIRED`, `SESSION_REVOKED`, `VALIDATION_ERROR`, `RATE_LIMITED`, `ACCOUNT_DISABLED`, `PASSWORD_RESET_INVALID`, `PASSWORD_RESET_EXPIRED`
3. THE backend SHALL NEVER include SQL errors, stack traces, or internal exception messages in error responses returned to clients
4. THE Kotlin DTOs for auth responses SHALL use field names that match the FastAPI camelCase aliases

---

### Requirement 28: Database Integrity

**User Story:** As a database administrator, I want strong schema constraints, so that corrupt or
orphaned data cannot enter the system.

#### Acceptance Criteria

1. THE `users` table SHALL have primary key, unique constraints on `email` and `mobile`, and indexes on both
2. THE `refresh_tokens` table SHALL have a foreign key to `users.id` with `ON DELETE CASCADE`, a unique index on `token_hash`, and an index on `user_id`
3. THE `password_reset_tokens` table SHALL have a foreign key to `users.id` with `ON DELETE CASCADE`, a unique index on `token_hash`, and an index on `user_id`
4. THE `user_profiles` table SHALL have a foreign key to `users.id` (currently missing — must be added via migration) with `ON DELETE CASCADE`, and a unique constraint on `user_id`
5. ALL `created_at` and `updated_at` timestamps SHALL be stored as UTC timezone-aware datetimes

---

### Requirement 29: Atomic Registration Transaction

**User Story:** As a developer, I want user registration to be an atomic operation, so that a
partial failure never creates an orphaned user or profile record.

#### Acceptance Criteria

1. WHEN a new user registers, THE AuthService SHALL create both the `User` and the `UserProfile` records within a single database transaction
2. IF either the `User` or `UserProfile` insertion fails, THEN THE transaction SHALL be rolled back and no partial records SHALL be persisted
3. THE registration transaction SHALL also include the initial `RefreshToken` record

---

### Requirement 30: Database Migrations

**User Story:** As a developer, I want all schema changes managed by Alembic, so that the database
schema is version-controlled and reproducible.

#### Acceptance Criteria

1. THE System SHALL use Alembic for all schema changes; no manual SQL changes SHALL be applied to production
2. EVERY migration SHALL be reversible (have a `downgrade` function)
3. THE migration adding the foreign key from `user_profiles.user_id` to `users.id` SHALL be a separate, idempotent migration that safely handles existing data
4. THE CI/CD pipeline SHALL verify that `alembic upgrade head` runs without error against a clean database

---

### Requirement 31: Existing Data Migration

**User Story:** As an operator, I want a safe migration path for existing `user_profiles` data, so
that no existing user conversations, roadmaps, or progress records are orphaned.

#### Acceptance Criteria

1. BEFORE adding the foreign key constraint on `user_profiles.user_id`, THE migration SHALL inspect existing records and document any `user_profiles.user_id` values that do not reference a row in `users`
2. THE migration strategy SHALL be documented in the design document with options for handling orphaned profiles (e.g., delete orphans, create placeholder users)
3. THE migration SHALL be idempotent and safe to run against a database with existing data

---

### Requirement 32: Backend Authentication Tests

**User Story:** As a developer, I want comprehensive automated tests for all backend auth
scenarios, so that regressions are caught before deployment.

#### Acceptance Criteria

1. THE test suite SHALL include registration tests: valid registration, duplicate email, duplicate mobile, invalid email format, weak password (< 8 chars)
2. THE test suite SHALL include login tests: valid email login, valid mobile login, wrong password, non-existent account, disabled account
3. THE test suite SHALL include JWT validation tests: valid token, expired token, wrong type (refresh used as access), tampered signature
4. THE test suite SHALL include refresh token tests: valid rotation, expired token, revoked token, reuse detection (revoked token presented again → all tokens revoked)
5. THE test suite SHALL include authorization tests: no token → 401, invalid token → 401, cross-user data access → 403/404, `?user_id=` bypass rejected in production
6. THE test suite SHALL include logout, password reset (valid flow, expired token, already-used token), and forgot-password tests

---

### Requirement 33: Android Authentication Tests

**User Story:** As an Android developer, I want unit tests for all Android auth components, so that
auth logic is correct and regressions are caught before release.

#### Acceptance Criteria

1. THE test suite SHALL include `TokenStorage` tests: save and retrieve, clear removes all tokens, encrypted storage is used
2. THE test suite SHALL include `SessionManager` tests: initial state is `Initializing`, restoration with valid token, restoration with expired token triggers refresh, restoration with no tokens → `Unauthenticated`
3. THE test suite SHALL include `AuthViewModel` tests: login success → `LoginSucceeded` event, login failure → error state, register success
4. THE test suite SHALL include `AuthRepository` tests (with mocked HTTP): login maps response to session, register sends correct body, network failure returns `Result.failure`
5. THE test suite SHALL include `AuthInterceptor` tests: token is added to header, missing token does not add header
6. THE test suite SHALL include `TokenAuthenticator` tests: 401 triggers refresh, successful refresh retries request, failed refresh clears session, concurrent 401s trigger only one refresh

---

### Requirement 34: Security Tests

**User Story:** As a security engineer, I want automated security checks in the test suite, so that
common security mistakes are caught before deployment.

#### Acceptance Criteria

1. THE test suite SHALL verify that no auth response body contains a plaintext password field
2. THE test suite SHALL verify that no log output during tests contains a raw token value
3. THE test suite SHALL verify that the `?user_id=` bypass is blocked when `ENVIRONMENT="production"`
4. THE test suite SHALL verify that refresh token reuse is detected and all tokens with the same `session_family_id` are revoked (while tokens from other sessions/families remain active)
5. THE test suite SHALL verify that `JWT_SECRET_KEY` is not the known development placeholder in a simulated production environment check

---

### Requirement 35: CORS Configuration

**User Story:** As a security engineer, I want CORS configured per environment, so that the backend
does not expose itself to arbitrary origins in production.

#### Acceptance Criteria

1. THE backend SHALL NOT use `allow_origins=["*"]` in any environment
2. THE backend CORS configuration SHALL include `"Authorization"` in `allow_headers`
3. THE production CORS `allow_origins` SHALL be loaded from an environment variable (e.g., `CORS_ORIGINS`) and SHALL only include known production origins
4. THE development CORS configuration MAY include `http://10.0.2.2:*`, `http://localhost:*` for local testing

---

### Requirement 36: Observability and Structured Logging

**User Story:** As an operator, I want auth events logged in a structured, queryable format, so
that I can audit and debug authentication issues in production.

#### Acceptance Criteria

1. THE backend SHALL emit structured log entries for: successful login, failed login (without revealing why), user registration, logout, token refresh failure, token reuse detection, password reset initiated
2. EACH log entry SHALL include: `timestamp` (UTC ISO 8601), `event`, `user_id` (where available), `correlation_id`
3. THE backend SHALL NEVER log: plaintext passwords, raw tokens, JWT payloads, or any PII beyond `user_id` and `email` (masked)
4. THE backend SHALL include a middleware that generates a `correlation_id` (UUID) per request and attaches it to all log entries for that request

---

### Requirement 37: Correlation IDs

**User Story:** As a developer, I want each request tagged with a correlation ID that the Android
client can reference, so that client-side errors can be linked to backend log entries.

#### Acceptance Criteria

1. THE backend SHALL generate a UUID `X-Correlation-ID` header per request and return it in the response
2. IF the Android client sends an `X-Correlation-ID` request header, THE backend SHALL use that value instead of generating a new one
3. THE Android app SHALL implement a `CorrelationIdInterceptor` that generates a new UUID `X-Correlation-ID` and adds it to every outbound request; this interceptor SHALL be shared by BOTH `PublicApiClient` and `AuthenticatedApiClient`
4. THE `AuthInterceptor` SHALL be responsible ONLY for injecting the `Authorization: Bearer` header; it SHALL NOT also handle correlation IDs or any other cross-cutting concerns
5. THE `CorrelationIdInterceptor` SHALL generate a new UUID for each request if an `X-Correlation-ID` header is not already present in the outbound request

---

### Requirement 38: Real-Time Auth Readiness

**User Story:** As a developer, I want the auth design to be forward-compatible with WebSocket
connections, so that future real-time features can reuse the same auth infrastructure.

#### Acceptance Criteria

1. THE design document SHALL describe how a WebSocket or SSE connection can authenticate using the existing JWT access token (e.g., token passed as query parameter or in the initial HTTP upgrade handshake)
2. THE `decode_access_token` utility SHALL remain a pure, stateless function suitable for use in WebSocket connection handlers

---

### Requirement 39: Time Handling

**User Story:** As a developer, I want all time values to use UTC, so that timezone bugs in token
expiry and timestamp comparisons are impossible.

#### Acceptance Criteria

1. ALL datetime values created in the backend SHALL use `datetime.now(timezone.utc)` or equivalent timezone-aware UTC
2. THE backend SHALL NOT mix naive and timezone-aware datetime objects in the same comparison
3. ALL `expires_at` and `created_at` columns SHALL be declared as `DateTime(timezone=True)` in SQLAlchemy

---

### Requirement 40: Performance

**User Story:** As an operator, I want authentication to be fast, so that it does not become a
bottleneck for the application.

#### Acceptance Criteria

1. THE `refresh_tokens.token_hash` column SHALL have a unique index (already present) to ensure O(log n) lookup
2. THE `users.email` and `users.mobile` columns SHALL have indexes (already present) to ensure O(log n) login lookup
3. THE AuthService SHALL NOT make more than one database query per token validation; the access token SHALL be decoded statelessly from the JWT without a DB round trip

---

### Requirement 41: Backward Compatibility

**User Story:** As a user, I want existing features (AI chat, offline AI, courses, roadmaps) to
continue working after the auth integration, so that the migration does not break my current usage.

#### Acceptance Criteria

1. THE existing `ChatApiClient` (used for AI chat) SHALL continue to function without modification to its retry and DNS logic after auth is integrated
2. THE existing public API endpoints (`/api/v1/courses`, `/api/v1/roadmaps`, `/api/v1/health`) SHALL remain unauthenticated and continue to work for unauthenticated clients
3. THE auth migration SHALL be incremental: the app SHALL remain functional at each intermediate step

---

### Requirement 42: Android UI — Auth Screens

**User Story:** As a user, I want the login, register, and forgot-password screens to display
loading states and errors properly, so that I can understand what is happening during auth
operations.

#### Acceptance Criteria

1. THE existing `LoginScreen`, `RegisterScreen`, and `ForgotPasswordScreen` designs SHALL NOT be redesigned; only functional behaviour (loading, error display, session state) SHALL be added or corrected
2. WHEN an auth operation is in progress, THE UI SHALL display a loading indicator and disable the submit button to prevent duplicate submissions
3. WHEN a session is expired or the user is logged out, THE `MainActivity` SHALL navigate to `LoginScreen` automatically
4. WHEN the app is in `Initializing` state, THE `MainActivity` SHALL display a loading/splash state rather than flashing the login screen

---

### Requirement 43: Build and CI Verification

**User Story:** As a developer, I want automated build and test verification, so that auth
regressions are caught before they reach production.

#### Acceptance Criteria

1. THE backend test suite SHALL pass with `pytest` against an in-memory SQLite test database
2. THE Android project SHALL build with `./gradlew assembleDebug` without errors after auth integration
3. THE Android unit tests SHALL pass with `./gradlew testDebugUnitTest`
4. THE database migration SHALL be verified by running `alembic upgrade head` against a clean database in CI

---

### Requirement 44: Production Readiness Checklist

**User Story:** As an operator, I want a verifiable checklist of production-readiness requirements,
so that the system is not deployed with development-only settings.

#### Acceptance Criteria

1. THE Render deployment SHALL have the following environment variables set: `JWT_SECRET_KEY` (non-placeholder), `DATABASE_URL`, `ENVIRONMENT=production`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `REFRESH_TOKEN_EXPIRE_DAYS`
2. THE production deployment SHALL have `ALLOW_DEV_USER_ID=False` (or unset, defaulting to False)
3. THE `.env` file SHALL be in `.gitignore`; no secrets SHALL exist in the git history
4. WHEN the application starts in production with the development placeholder `JWT_SECRET_KEY`, THE application SHALL raise a startup error and refuse to serve requests
