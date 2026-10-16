# Android Domain Layer (`domain/`)

The `domain/` directory contains pure Kotlin domain entities, business models, and reactive state wrappers for the EduNova Android application.

---

## Architectural Principles

- **Zero Framework Coupling**: Domain models are independent of Android SDK classes, SQLite, or OkHttp.
- **Immutability**: All models are defined as Kotlin `data class` structures or sealed interfaces to guarantee state consistency across reactive flows.
- **Type Safety**: Enums and sealed classes represent discrete domain states (such as AI modes, sync statuses, and learner levels).

---

## Model Inventory

| File | Purpose & Entities Defined |
|---|---|
| [`AIModels.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/AIModels.kt) | `ChatMessage`, `Conversation`, `AIExecutionMode` (`ONLINE`, `OFFLINE`), `AIPersona` (`Teacher`, `General`, `Explainable`), `ModelDownloadState`, and `TelemetryEvent`. |
| [`RAGModels.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/RAGModels.kt) | `SyllabusChunk`, `EvidenceAssessment` (`SUFFICIENT`, `UNCERTAIN`, `MISSING`), `CitationItem`, and `RAGRetrievalResult`. |
| [`Course.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/Course.kt) | Catalog entities: `Course`, `CourseModule`, `Lesson`, `CourseCategory`, and difficulty levels (`Beginner`, `Intermediate`, `Advanced`). |
| [`LearningCourse.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/LearningCourse.kt) | Enrolled learner models: `UserCourseProgress`, `UserLessonProgress`, study streaks, and completion percentages. |
| [`Roadmap.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/Roadmap.kt) | Career and academic roadmaps: `Roadmap`, `RoadmapStage`, `RoadmapMilestone`, and custom roadmap creation payloads. |
| [`SchoolModels.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/SchoolModels.kt) | Student mode entities: `SchoolSubject`, `ChapterProgress`, `ExamCountdown`, and study focus recommendations for CBSE Classes 9-12. |
| [`UserProfile.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/UserProfile.kt) | User account details: `UserProfile`, `EducationMode` (`STUDENT`, `GENERAL`), and personalization interests. |
| [`UiState.kt`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/model/UiState.kt) | Standard sealed representation for reactive screen states: `Loading`, `Success<T>`, `Error`, and `Empty`. |
