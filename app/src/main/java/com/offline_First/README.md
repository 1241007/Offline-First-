# EduNova Kotlin Android Core (`app/src/main/java/com/offline_First/`)

This directory is the root package for all Kotlin code in the EduNova Android client application.

---

## Architecture & Package Organization

```
com/offline_First/
├── MainActivity.kt             # Single Activity hosting Jetpack Compose navigation & window insets
├── data/                       # Data layer: databases, network clients, providers, repositories
│   ├── AppContainer.kt         # Manual Dependency Injection container
│   ├── SessionManager.kt       # Centralized authentication state machine (StateFlow<AuthState>)
│   ├── local/                  # On-device storage (SQLite, EncryptedKeystore, ModelManager)
│   ├── provider/               # Model provider implementations (Offline Llama vs Online Gemini)
│   ├── remote/                 # OkHttp networking, interceptors, authenticators, DTOs
│   └── repository/             # High-level domain repositories & mode routing
├── domain/                     # Domain layer: business entities, state representations, models
│   └── model/                  # AI, Chat, Course, Learning, RAG, Roadmap, and Profile models
└── ui/                         # Presentation layer: Jetpack Compose screens, ViewModels, theme
    ├── components/             # Reusable UI widgets, navigation bars, chip selectors
    ├── navigation/             # AppDestination routes and screen transitions
    ├── screens/                # Feature screens (AIWorkspace, Landing, Learning, Roadmap, Auth)
    ├── theme/                  # Material 3 color schemes, typography, dimensions
    └── utils/                  # Icon and UI utility helpers
```

---

## Core Components

### 1. `MainActivity.kt`
The application's single `ComponentActivity`. It:
- Initializes edge-to-edge window insets.
- Acquires dependencies from the global `AppContainer`.
- Renders the root Compose hierarchy: bottom navigation bar, state listeners for `AuthState.SessionExpired`, and top-level navigation graph.

### 2. `data/AppContainer.kt`
The centralized manual dependency injection container:
- Instantiates singleton database instances (`ChatCacheDatabase`, `SyllabusDatabase`).
- Instantiates hardware-encrypted token storage (`EncryptedTokenStorage`).
- Builds OkHttp clients (`AuthApiClient` for unauthenticated endpoints, `AuthenticatedApiClient` with automatic 401 retry mutex).
- Injects dependencies into all domain repositories and factory methods for ViewModels.

### 3. `data/SessionManager.kt`
Centralized reactive session state machine exposing:
```kotlin
val authState: StateFlow<AuthState>
```
With distinct states:
- `AuthState.Initializing`: Loading encrypted tokens from Android Keystore.
- `AuthState.Unauthenticated`: No valid credentials present.
- `AuthState.Authenticating`: Login/register request in flight.
- `AuthState.Authenticated`: Valid access and refresh tokens available.
- `AuthState.Refreshing`: OkHttp authenticator actively renewing access token.
- `AuthState.SessionExpired`: Refresh token revoked or rejected; UI immediately prompts re-login.

---

## Subpackage Guides

- **[`data/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/data/README.md)**: Data Layer documentation.
- **[`domain/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/README.md)**: Domain Layer documentation.
- **[`ui/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/ui/README.md)**: Presentation Layer documentation.
