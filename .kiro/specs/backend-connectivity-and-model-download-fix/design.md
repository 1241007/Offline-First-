# Design Document: Backend Connectivity and Model Download Fix

## Overview

This document describes the minimal targeted changes needed to fix two bugs in the EduNova Android app:

1. The backend base URL is hardcoded to the Android Emulator host alias `10.0.2.2`, which is unreachable from physical devices. The fix moves the URL into a `BuildConfig` field sourced from `gradle.properties`, following the identical pattern already used for the model download URLs.

2. The HuggingFace model download URLs contain an invalid `/buckets/` path segment and a spurious `?download=true` query parameter. The fix replaces the default URL strings with the correct HuggingFace `resolve/` format. Additionally, network-related download errors are given clearer user-facing messages.

No new dependencies, no new abstractions, and no architectural changes are introduced. Every change stays within the three files identified in the requirements.

---

## Architecture

The app follows a repository pattern with a clear separation between remote (online) and local (offline) data sources.

```
┌─────────────────────────────────────────────────────────┐
│  app/build.gradle.kts                                   │
│  gradle.properties                                      │
│         │  compile-time constants via BuildConfig       │
│         ▼                                               │
│  ChatApiConfig.kt         LocalAIRepository.kt          │
│  (BASE_URL from           (model URLs from              │
│   BuildConfig)             BuildConfig)                 │
│         │                        │                      │
│         ▼                        ▼                      │
│  ChatApiClient.kt         HuggingFace CDN               │
│  (HTTP calls to            (GGUF download)              │
│   FastAPI backend)                                      │
└─────────────────────────────────────────────────────────┘
```

Both fixes operate entirely at the build-configuration layer and in the data layer. No ViewModel, UI, or domain code is touched.

---

## Components and Interfaces

### 1. `app/build.gradle.kts` — Build Configuration

**Current state:**
- `offlineModelSmallUrl` and `offlineModelLargeUrl` are read from Gradle properties with fallback to environment variables and finally to default strings. The default strings contain `/buckets/` and `?download=true`.
- No entry exists for the backend base URL.

**After fix:**
- The default strings for model URLs are corrected to the proper HuggingFace `resolve/` format.
- A new `backendBaseUrl` value is read using the same `providers.gradleProperty(...).orElse(providers.environmentVariable(...)).orElse(default).get()` pattern.
- The resolved URL is exposed as `buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")` inside `defaultConfig`.

The three `buildConfigField` entries in `defaultConfig` will be:
```kotlin
buildConfigField("String", "OFFLINE_MODEL_SMALL_URL", "\"$smallOfflineModelUrl\"")
buildConfigField("String", "OFFLINE_MODEL_LARGE_URL", "\"$largeOfflineModelUrl\"")
buildConfigField("String", "BACKEND_BASE_URL",        "\"$backendBaseUrl\"")
```

### 2. `gradle.properties` — Developer Configuration

A commented-out example block is appended to `gradle.properties` to guide developers:

```properties
# --- EduNova backend URL ---
# Android Emulator default (maps to host machine localhost):
# backendBaseUrl=http://10.0.2.2:8000
# Physical device on same WiFi (replace with your machine's LAN IP):
# backendBaseUrl=http://192.168.1.x:8000
```

This file is tracked in version control, so the comments serve as onboarding documentation.

### 3. `ChatApiConfig.kt` — Backend URL Source

**Current state:**
```kotlin
const val BASE_URL = "http://10.0.2.2:8000"
```

**After fix:**
```kotlin
val BASE_URL: String = BuildConfig.BACKEND_BASE_URL
```

The field changes from a compile-time `const val` to a regular `val` because `BuildConfig` fields are not themselves `const`. This is a source-compatible change — all callers use `ChatApiConfig.BASE_URL` and that reference remains valid.

### 4. `LocalAIRepository.kt` — Download Error Classification

The `startOfflineAIDownload` function's `catch (error: Throwable)` handler currently sets a generic "Download failed" stage string. It is updated to inspect the exception type:

- `java.net.UnknownHostException` → `"No internet connection. Connect to the internet and retry."`
- `java.net.SocketTimeoutException` → `"Connection timed out. Check your internet connection and retry."`
- `java.net.ConnectException` → `"Could not reach the download server. Check your internet connection and retry."`
- `IllegalStateException` with an HTTP status code message (already constructed upstream) → message passed through unchanged.
- Any other `Throwable` → `"Download failed: ${error.message}"`

The partial file deletion on failure already exists in the current code (`partial.delete()` in the catch block) and is preserved as-is.

---

## Data Models

No data model changes. The fix affects only string constants and error message strings. The `OfflineAIDownloadProgress` data class already has a `stage` field used to surface status messages to the UI — this field is what the improved error messages populate.

```kotlin
data class OfflineAIDownloadProgress(
    val stage: String = "",
    val progress: Int = 0,
    val downloadSize: String = "",
    val estimatedTimeRemaining: String = ""
)
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: BuildConfig URL round-trip consistency

*For any* value set for `backendBaseUrl` in `gradle.properties`, the value read at runtime from `ChatApiConfig.BASE_URL` (which reads `BuildConfig.BACKEND_BASE_URL`) should equal the value that was configured.

**Validates: Requirements 1.1, 1.2, 1.3**

### Property 2: Default URL fallback correctness

*For any* build where neither `backendBaseUrl` nor `BACKEND_BASE_URL` is set, `BuildConfig.BACKEND_BASE_URL` should equal `"http://10.0.2.2:8000"`.

**Validates: Requirements 1.5**

### Property 3: Corrected model URLs contain no invalid segments

*For any* build that does not override `offlineModelSmallUrl` or `offlineModelLargeUrl`, both `BuildConfig.OFFLINE_MODEL_SMALL_URL` and `BuildConfig.OFFLINE_MODEL_LARGE_URL` should not contain the substring `"/buckets/"` and should not contain the substring `"?download=true"`.

**Validates: Requirements 2.3, 2.4**

### Property 4: Network error classification

*For any* `Throwable` thrown during the HTTP download phase, the error message placed in `OfflineAIDownloadProgress.stage` should not be the generic `"Download failed"` string when the root cause is a known network exception (`UnknownHostException`, `SocketTimeoutException`, `ConnectException`).

**Validates: Requirements 3.1**

### Property 5: Download failure leaves no partial file

*For any* download attempt that ends in failure (non-2xx status, network error, or verification failure), the `.download` partial file should not exist on the filesystem after the failure is reported.

**Validates: Requirements 3.3**

---

## Error Handling

### Bug 1 — Backend URL

If the URL in `gradle.properties` is malformed (e.g. missing scheme), the existing `HttpURLConnection` timeout and exception handling in `ChatApiClient` will catch the resulting `MalformedURLException` and propagate it as a `Result.failure`. No additional error handling is needed; this is a developer configuration error, not a runtime user error.

### Bug 2 — Model Download

| Scenario | Current behaviour | After fix |
|---|---|---|
| `UnknownHostException` | "Download failed" stage, generic | "No internet connection. Connect to the internet and retry." |
| `SocketTimeoutException` | "Download failed" stage, generic | "Connection timed out. Check your internet connection and retry." |
| `ConnectException` | "Download failed" stage, generic | "Could not reach the download server. Check your internet connection and retry." |
| HTTP 4xx / 5xx | "The Offline AI model download failed (HTTP N)." (already good) | Unchanged |
| Incomplete download | "The Offline AI model download was incomplete." (already good) | Unchanged |
| Invalid GGUF file | "The downloaded file is not a valid Qwen GGUF model." (already good) | Unchanged |

In all failure cases the partial file is deleted and `offlineStatus` is set to `NOT_DOWNLOADED` — this behaviour is already present and is preserved.

---

## Testing Strategy

Because the changes are confined to build configuration string values and a small error-classification block in `LocalAIRepository`, the testing approach is correspondingly lightweight.

### Unit Tests

Unit tests target the two pure functions already present in `LocalAIRepository.kt` (`useLargeOfflineModel`, `isValidGgufFile`) and the new error-classification logic. These functions have no Android dependencies and can be tested with plain JUnit 4 (already on the test classpath via `testImplementation(libs.junit)`).

- Verify that `useLargeOfflineModel` returns `true` for RAM ≥ 8 GB and `false` below.
- Verify that `isValidGgufFile` returns `false` for an empty file, a file with wrong magic bytes, and `true` for a correctly structured synthetic GGUF header.
- Verify that the error-classification helper maps each known network exception type to its expected message prefix.

### Property-Based Tests

The correctness properties above are verified through build-time assertions and unit-level property tests rather than full integration tests, because the root causes are compile-time configuration values.

- **Property 1 & 2** are verified by a build-time assertion in `app/build.gradle.kts`: after resolving `backendBaseUrl`, assert the value starts with `"http"`.
- **Property 3** is verified by a unit test that checks the default URL strings do not contain `"/buckets/"` or `"?download=true"`.
- **Property 4** is verified by a parameterized unit test that passes each known network exception type through the classification logic and asserts the output message is not the generic fallback.
- **Property 5** is covered by the existing `partial.delete()` call (already in the catch block) combined with a unit test using a mock filesystem.

### Property-Based Testing Library

For the parameterized and property-oriented unit tests, use **JUnit 4 parameterized tests** (`@RunWith(Parameterized::class)`) — no additional dependency is required since JUnit 4 is already on the classpath. Each parameterized test case corresponds to one property.

Minimum 100 iterations are not applicable for deterministic string-constant properties (Properties 1–3); for the error-classification property (Property 4) all known exception types are exhaustively enumerated rather than randomly sampled, which gives stronger coverage.

Tag format for test methods: `// Feature: backend-connectivity-and-model-download-fix, Property N: <property_text>`
