# EduNova Android Application (`app/`)

This directory contains the complete source code, build configuration, native C++ runtime, and unit test suites for the **EduNova Android Client Application**.

---

## Directory Overview

```
app/
├── build.gradle.kts                # Android application Gradle build script
├── release/                        # Release signing & build outputs
└── src/
    ├── androidTest/                # Android instrumentation test suite
    ├── main/
    │   ├── AndroidManifest.xml     # Application manifest, permissions, activities
    │   ├── cpp/                    # Native C++ JNI bridge (llama.cpp integration)
    │   ├── java/com/offline_First/ # Kotlin source code (data, domain, ui)
    │   ├── jniLibs/                # Precompiled native shared libraries (.so)
    │   ├── keepRules/              # ProGuard / R8 keep rules
    │   └── res/                    # Android XML resources, icons, themes
    └── test/                       # Unit test suites (JUnit 4, MockWebServer)
```

---

## Architecture & Technology Stack

EduNova Android is built following **Clean Architecture** and **Unidirectional Data Flow (MVI/MVVM)**:

| Layer / Technology | Specification | Responsibilities |
|---|---|---|
| **Language** | Kotlin 2.0+ & Coroutines | Asynchronous pipelines, cold flows for reactive streaming, structured concurrency. |
| **UI Toolkit** | Jetpack Compose (Material 3) | Declarative UI, state hoisting, dark/light theme tokens, fluid animations. |
| **Native Runtime** | Android NDK 27.2+ & CMake 3.31+ | Compiles `llama_jni.cpp` and links prebuilt `llama.cpp` shared libraries. |
| **Edge AI Engine** | `llama.cpp` (GGUF Quantized) | On-device Qwen 2.5 1.5B/3B execution on CPU/RAM with KV cache reuse and watchdog. |
| **Local Persistence** | SQLite OpenHelper (`ChatCacheDatabase`) | Zero-dependency relational cache for messages, conversations, and long-term memory. |
| **Security** | `androidx.security.crypto:1.0.0` | Hardware-backed keystore encryption (`MasterKey` AES-256 GCM) for authentication tokens. |
| **Networking** | OkHttp 4.12+ & Kotlinx Serialization | Resilient HTTP/HTTPS calls, custom interceptors, automated 401 token refresh mutex. |
| **Dependency Injection** | Manual Container (`AppContainer.kt`) | Predictable, lightweight, reflection-free dependency injection container. |

---

## Subdirectories and Modules

- **[`src/main/cpp/`](file:///e:/Offline%20First/app/src/main/cpp/README.md)**: Native C++ JNI bridge source code connecting Android Kotlin runtime with `llama.cpp` inference engine.
- **[`src/main/jniLibs/`](file:///e:/Offline%20First/app/src/main/jniLibs/README.md)**: Precompiled 64-bit native libraries (`arm64-v8a` for modern phones, `x86_64` for Android Studio emulators).
- **[`src/main/java/com/offline_First/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/README.md)**: Kotlin codebase organized into `data/`, `domain/`, and `ui/` packages.
- **[`src/test/`](file:///e:/Offline%20First/app/src/test/README.md)**: Unit tests covering state machines, auth mutexes, offline model lifecycle, and RAG pipelines.

---

## Key Permissions & Security Configuration

Defined in [`src/main/AndroidManifest.xml`](file:///e:/Offline%20First/app/src/main/AndroidManifest.xml):

- `android.permission.INTERNET`: Required for communicating with the FastAPI cloud backend and downloading GGUF models from HuggingFace.
- `android.permission.ACCESS_NETWORK_STATE`: Monitors online/offline connectivity to drive seamless UI switching.
- `android.permission.WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`: Scoped storage access for cached models and curriculum documents.
- `android:usesCleartextTraffic="true"` (debug-only): Permitted for local development emulator loopback (`10.0.2.2:8000`).

---

## Build and Execution Commands

Run commands from the repository root:

### 1. Run Android Unit Tests
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

### 2. Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
Output location: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Clean and Rebuild Native Targets
```powershell
.\gradlew.bat clean assembleDebug --rerun-tasks
```
