# Gradle Configuration & Build System (`gradle/`)

This directory contains the Gradle build configuration, dependency Version Catalogs, daemon JVM parameters, and the official Gradle Wrapper distribution for the Android client application.

---

## Directory Overview

```
gradle/
├── README.md                   # Build system documentation
├── gradle-daemon-jvm.properties# Daemon JVM configuration flags
├── libs.versions.toml          # Centralized Gradle Version Catalog
└── wrapper/
    ├── gradle-wrapper.jar      # Gradle Wrapper bootstrap binary
    └── gradle-wrapper.properties# Pinned Gradle distribution URL & checksum
```

---

## Key Files

### 1. Version Catalog (`libs.versions.toml`)
EduNova uses Gradle's official Version Catalog to declare all dependencies and plugins in a single centralized file:
- **Plugins**: Android Application (`agp`), Kotlin Android, Kotlin Serialization, Compose Compiler.
- **AndroidX & Compose**: `lifecycle-runtime-compose`, `activity-compose`, `navigation-compose`, Material 3, `core-ktx`.
- **Security**: `androidx.security:security-crypto` for hardware-backed keystore integration.
- **Networking**: `okhttp` and `logging-interceptor`.
- **Coroutines & Testing**: `kotlinx-coroutines-android`, `kotlinx-coroutines-test`, `mockwebserver`, `junit`.

### 2. Wrapper Properties (`wrapper/gradle-wrapper.properties`)
Pins the exact Gradle distribution used across all developer machines and continuous integration environments:
- **Gradle Version**: `8.9`
- **Distribution URL**: `https\://services.gradle.org/distributions/gradle-8.9-bin.zip`

---

## Build Commands

From the repository root:

- **Run all Android unit tests**:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- **Assemble Debug APK**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- **Refresh Dependencies**:
  ```powershell
  .\gradlew.bat --refresh-dependencies
  ```
