# Implementation Plan: Backend Connectivity and Model Download Fix

## Overview

Four files need changes across three tasks. Tasks are ordered so each change is independently buildable before the next is applied.

---

## Tasks

- [x] 1. Fix HuggingFace model download URLs in build.gradle.kts
  - In `app/build.gradle.kts`, replace the default value for `offlineModelSmallUrl` with `"https://huggingface.co/PatilKrish/Qwen_Models2/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf"`
  - Replace the default value for `offlineModelLargeUrl` with `"https://huggingface.co/PatilKrish/Qwen_Models2/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf"`
  - Both corrected URLs must not contain `/buckets/` or `?download=true`
  - References: Requirements 2.1, 2.2, 2.3, 2.4

- [x] 2. Add configurable backend base URL to build configuration
  - In `app/build.gradle.kts`, add a `backendBaseUrl` variable that reads from `providers.gradleProperty("backendBaseUrl")`, falls back to `providers.environmentVariable("BACKEND_BASE_URL")`, and defaults to `"htdtps://edunova-backend-9waj.onrender.com"`
  - Add `buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")` inside `defaultConfig`
  - In `gradle.properties`, add the active entry `backendBaseUrl=https://edunova-backend-9waj.onrender.com` with commented-out alternatives for emulator (`http://10.0.2.2:8000`) and LAN (`http://192.168.1.x:8000`)
  - References: Requirements 1.1, 1.2, 1.4, 1.5, 1.6

- [~] 3. Update ChatApiConfig to read BASE_URL from BuildConfig
  - In `ChatApiConfig.kt`, replace `const val BASE_URL = "http://10.0.2.2:8000"` with `val BASE_URL: String = BuildConfig.BACKEND_BASE_URL`
  - Add the `import com.offline_First.BuildConfig` import
  - References: Requirements 1.3

- [x] 4. Improve download error messages in LocalAIRepository
  - In the `catch (error: Throwable)` block of `startOfflineAIDownload`, replace the generic `"Download failed"` stage string with a `when` expression that maps:
    - `java.net.UnknownHostException` → `"No internet connection. Connect to the internet and retry."`
    - `java.net.SocketTimeoutException` → `"Connection timed out. Check your internet connection and retry."`
    - `java.net.ConnectException` → `"Could not reach the download server. Check your internet connection and retry."`
    - Any other `Throwable` → `"Download failed: ${error.message ?: "Unknown error"}"`
  - Verify the existing `partial.delete()` call remains in the catch block
  - References: Requirements 3.1, 3.2, 3.3, 3.4

- [x] 5. Write unit tests for error classification and URL correctness
  - Create `app/src/test/java/com/offline_First/DownloadFixTest.kt`
  - Test that `BuildConfig.OFFLINE_MODEL_SMALL_URL` and `BuildConfig.OFFLINE_MODEL_LARGE_URL` do not contain `"/buckets/"` or `"?download=true"`
  - Test that each known network exception type (`UnknownHostException`, `SocketTimeoutException`, `ConnectException`) maps to the expected non-generic error message
  - Use JUnit 4 (already on classpath via `testImplementation(libs.junit)`)
  - References: Requirements 2.3, 2.4, 3.1
