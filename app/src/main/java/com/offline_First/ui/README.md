# Android UI & Presentation Layer (`ui/`)

This directory contains the user interface components, Jetpack Compose screens, ViewModels, navigation routing, and design system tokens for the EduNova Android application.

---

## Architecture & Design System

The presentation layer adheres to **Unidirectional Data Flow (UDF)**:
- **ViewModels** expose state via Kotlin `StateFlow<UiState<T>>` or dedicated UI state data classes.
- **Compose Screens** are pure rendering functions that accept immutable state and hoist user events to the ViewModel.
- **Material 3 Design Tokens** in `theme/` provide consistent colors, typography, surface elevations, and adaptive dark/light themes.

---

## Directory Overview

```
ui/
├── components/                 # Reusable Compose UI components & design system widgets
│   ├── AuthComponents.kt       # Styled text fields, password visibility toggles, action buttons
│   ├── ChipSelector.kt         # Filter chips for categories, difficulty, and persona selection
│   ├── EduNovaBottomNavigation # Persistent bottom navigation bar with badging
│   ├── EduNovaDesignSystem.kt  # Design system card wrappers, banners, and empty states
│   └── LazyListPrefetch.kt     # Optimized list prefetching helper
├── navigation/
│   └── AppDestination.kt       # Sealed navigation route definitions & argument extractors
├── screens/                    # Feature screens and associated ViewModels
│   ├── ai/                     # AI Workspace Screen & AIViewModel
│   ├── auth/                   # Login, Register, Forgot Password screens & AuthViewModel
│   ├── landing/                # Student & General Mode landing screen & LandingViewModel
│   ├── roadmap/                # Roadmap explorer, builder & RoadmapViewModel
│   ├── MyLearningScreen.kt     # Enrolled courses, lessons, progress, and streaks
│   ├── ProfileScreen.kt        # User profile, learning preferences, and stats
│   └── SettingsScreen.kt       # Offline model manager (downloads, disk usage, cleanup)
├── theme/                      # Styling tokens
│   ├── Color.kt                # Vibrant, accessible color palette (Primary, Secondary, Surface)
│   ├── Dimens.kt               # Spacing tokens, padding standards, corner radiuses
│   ├── Theme.kt                # EduNovaTheme Compose entry point (Dark/Light mode support)
│   └── Type.kt                 # Material 3 typography with custom font scale
└── utils/
    └── IconResolver.kt         # Dynamic category-to-vector icon resolver
```

---

## Key Screens & Features

1. **AI Workspace Screen (`AIWorkspaceScreen.kt`)**:
   - Live mode switcher: toggle between **Online Cloud (Gemini)** and **Offline Edge (llama.cpp)**.
   - Real-time token streaming markdown bubbles with syntax highlighting.
   - Persona selector (`Teacher`, `General`, `Explainable`).
   - Conversation management: message retry, edit previous prompt, and branching.

2. **Landing Screen (`LandingScreen.kt`)**:
   - **Student Mode**: CBSE Class 9-12 curriculum dashboard, subject cards, chapter completion rings, and upcoming board exam countdowns.
   - **General Mode**: Engineering tracks, trending technologies, and career roadmap access.

3. **My Learning Screen (`MyLearningScreen.kt`)**:
   - In-progress course dashboard, module completion, interactive checkmarks for lessons, and study streak counters.

4. **Roadmap Explorer & Builder (`RoadmapScreen.kt`, `RoadmapBuilderScreen.kt`)**:
   - Interactive milestone maps with stage gates, completed steps, and custom roadmap creation.

5. **Authentication Flow (`LoginScreen.kt`, `RegisterScreen.kt`, `ForgotPasswordScreen.kt`)**:
   - Strict client-side validation (`AuthValidation.kt`), loading spinners, session expired banners, and password reset flows.
