# Android Unit Testing Suite (`app/src/test/`)

This directory contains the unit, integration, and mock networking test suites for the EduNova Android client application.

---

## Directory Overview

```
app/src/test/java/com/offline_First/
├── AppDnsTest.kt                               # Custom DNS fallback testing (IPv4/IPv6 resilience)
├── DownloadFixTest.kt                          # Hugging Face URL formatting and error categorization
├── data/
│   ├── AIRepositoryTest.kt                     # ModeAwareAIRepository routing and fallback logic
│   ├── OfflineModeBugConditionTest.kt          # Edge cases and offline mode transitions
│   ├── RepositoryTest.kt                       # Base repository operation tests
│   ├── SessionManagerTest.kt                   # AuthState StateFlow transitions and expiration
│   ├── local/
│   │   ├── E5EmbeddingServiceTest.kt           # On-device ONNX E5 embedding generation tests
│   │   ├── OfflineModelLifecycleTest.kt        # 3-minute idle watchdog and resource freeing
│   │   ├── OfflineModelStateTest.kt            # Model download, ready, and uninitialized states
│   │   ├── SyllabusDatabaseTest.kt             # Local SQLite syllabus chunk storage & search
│   │   └── TokenStorageTest.kt                 # Encrypted token storage and key rotation
│   ├── remote/
│   │   ├── AuthInterceptorTest.kt              # Bearer header injection on authenticated calls
│   │   ├── ChatApiClientAuthTest.kt            # SSE chat streaming authorization headers
│   │   ├── OnlineChatIntegrationTest.kt        # MockWebServer SSE response parsing
│   │   ├── OnlineRoadmapRepositoryTest.kt      # Roadmap remote fetch and JSON deserialization
│   │   ├── RAGContextPropagationTest.kt        # Syllabus context and citation payload handling
│   │   ├── RemoteProfileAndLearningRepositoryTest.kt # Course and profile network calls
│   │   ├── RouteHeaderParserTest.kt            # X-Correlation-ID and route header parsing
│   │   └── TokenAuthenticatorTest.kt           # 401 Unauthorized mutex-guarded refresh retry
│   └── repository/
│       ├── RemoteAuthRepositoryTest.kt         # Login, register, logout, and token caching
│       └── SyllabusSyncRepositoryTest.kt       # Syllabus delta sync and version verification
└── ui/screens/
    ├── ai/AIViewModelTest.kt                   # Real-time token streaming state emission
    ├── auth/
    │   ├── AuthValidationTest.kt               # Password, email, and mobile format validation
    │   └── AuthViewModelTest.kt                # UI event handling and error state propagation
    ├── landing/LandingViewModelTest.kt         # Student Mode vs General Mode data loading
    └── roadmap/PersonalizedRoadmapViewModelTest.kt # Roadmap generation and assessment flows
```

---

## Testing Technologies & Patterns

- **Framework**: JUnit 4 with `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`).
- **Network Simulation**: `okhttp3.mockwebserver.MockWebServer` for simulating backend HTTP/SSE responses, latency spikes, and 401 Unauthorized token expirations.
- **Concurrency Testing**: Explicitly tests mutex concurrency in `TokenAuthenticatorTest` to guarantee that concurrent 401 failures trigger **exactly one** token refresh call.

---

## Running the Unit Tests

Execute from the root directory:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

To run a specific test class:
```powershell
.\gradlew.bat testDebugUnitTest --tests "com.offline_First.data.remote.TokenAuthenticatorTest"
```
