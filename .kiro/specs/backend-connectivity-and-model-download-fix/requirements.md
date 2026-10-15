# Requirements Document

## Introduction

EduNova is an Android learning app with a Kotlin/Compose frontend, a FastAPI backend, and offline AI chat powered by llama.cpp with Qwen2.5 GGUF models. Two bugs currently prevent the app from functioning correctly on physical devices:

1. The backend base URL is hardcoded to `http://10.0.2.2:8000`, which only routes correctly on the Android Emulator. Physical devices cannot reach this address, causing all backend-dependent screens (roadmap, courses, chat history) to fail with a connection error.

2. The HuggingFace model download URLs contain an invalid `/buckets/` path segment. The correct HuggingFace URL format omits this segment. The invalid URL causes DNS resolution to succeed but the server to return a 404 or redirect, and the error surfaced to the user does not clearly explain the root cause.

This spec covers the changes needed to fix both bugs with minimal code change: making the backend URL configurable via `gradle.properties`, and correcting the model download URLs.

## Glossary

- **BASE_URL**: The HTTP base URL used by `ChatApiClient` to reach the FastAPI backend.
- **BuildConfig**: The auto-generated Android class that exposes compile-time constants defined in `build.gradle.kts`.
- **ChatApiConfig**: The Kotlin object in `data/remote/ChatApiConfig.kt` that holds `BASE_URL` and HTTP timeout constants.
- **gradle.properties**: The project-level Gradle properties file used to supply environment-specific values without modifying source code.
- **GGUF**: The file format used by llama.cpp for quantized language models.
- **HuggingFace_URL**: A URL pointing to a model file hosted on `huggingface.co`, following the format `https://huggingface.co/{user}/{repo}/resolve/{branch}/{filename}`.
- **LocalAIRepository**: The Kotlin class in `data/local/LocalAIRepository.kt` that manages model download and offline inference.
- **Partial_File**: A temporary `.download`-suffixed file written during model download, renamed to the final model file only after successful verification.

---

## Requirements

### Requirement 1: Configurable Backend Base URL

**User Story:** As a developer, I want to configure the backend base URL in `gradle.properties` without editing source code, so that the app can connect to the FastAPI backend from both the Android Emulator and a physical device.

#### Acceptance Criteria

1. THE `app/build.gradle.kts` SHALL read the backend base URL from the Gradle property `backendBaseUrl`, falling back to the environment variable `BACKEND_BASE_URL`, and finally defaulting to `"http://10.0.2.2:8000"`.
2. THE `app/build.gradle.kts` SHALL expose the resolved URL as a `BuildConfig` string field named `BACKEND_BASE_URL`.
3. THE `ChatApiConfig` SHALL read `BASE_URL` from `BuildConfig.BACKEND_BASE_URL` instead of a hardcoded string literal.
4. WHEN the `backendBaseUrl` property is set in `gradle.properties`, THE Build_System SHALL use that value for `BuildConfig.BACKEND_BASE_URL` in every build variant.
5. WHEN the `backendBaseUrl` property is absent from `gradle.properties` and the `BACKEND_BASE_URL` environment variable is unset, THE Build_System SHALL use `"http://10.0.2.2:8000"` as the default value for `BuildConfig.BACKEND_BASE_URL`.
6. THE `gradle.properties` file SHALL contain a commented-out example entry for `backendBaseUrl` that shows a physical-device LAN IP pattern (e.g. `# backendBaseUrl=http://192.168.1.x:8000`) alongside the emulator default.

---

### Requirement 2: Correct HuggingFace Model Download URLs

**User Story:** As a user, I want the offline AI model to download successfully, so that I can use AI-powered features without an internet connection after the initial download.

#### Acceptance Criteria

1. THE `app/build.gradle.kts` SHALL set the default value for `offlineModelSmallUrl` to `"https://huggingface.co/PatilKrish/Qwen_Models2/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf"`.
2. THE `app/build.gradle.kts` SHALL set the default value for `offlineModelLargeUrl` to `"https://huggingface.co/PatilKrish/Qwen_Models2/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf"`.
3. THE corrected URLs SHALL NOT contain the `/buckets/` path segment.
4. THE corrected URLs SHALL NOT contain the `?download=true` query parameter.
5. WHEN the `offlineModelSmallUrl` or `offlineModelLargeUrl` Gradle property is explicitly set, THE Build_System SHALL use those overrides instead of the defaults, preserving the existing override mechanism.

---

### Requirement 3: Improved Download Error Messaging

**User Story:** As a user, I want to see a clear error message when the model download fails due to a network problem, so that I understand what went wrong and know how to fix it.

#### Acceptance Criteria

1. WHEN the model download fails because the device has no internet connectivity (e.g. `UnknownHostException`, `SocketTimeoutException`, or `ConnectException`), THE `LocalAIRepository` SHALL surface an error message that states the device is not connected to the internet.
2. WHEN the model download fails because the server returns a non-2xx HTTP status code, THE `LocalAIRepository` SHALL surface an error message that includes the HTTP status code.
3. WHEN the model download fails for any reason, THE `LocalAIRepository` SHALL delete the Partial_File before reporting the failure.
4. IF the model download fails, THEN THE `LocalAIRepository` SHALL set `offlineStatus` to `OfflineAIStatus.NOT_DOWNLOADED`.
