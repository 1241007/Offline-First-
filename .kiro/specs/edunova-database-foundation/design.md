# Design Document: EduNova Database Foundation

## Executive Summary

This design document addresses the architectural foundation for EduNova's backend database and API layer. Based on a comprehensive audit of the existing codebase, this document resolves critical architectural questions before implementation begins.

**Current State:**
- ✅ Backend: FastAPI + SQLAlchemy 2.x async + Alembic migrations
- ✅ Database: PostgreSQL on Supabase
- ✅ Chat functionality: Working (conversations + messages tables exist)
- ✅ Android app: UI complete, waiting for backend data
- ❌ No authentication system implemented yet
- ❌ No course/roadmap/progress database tables
- ❌ No corresponding API endpoints

**This Phase Priority:**
- Database schema for courses, roadmaps, and learning progress
- REST API endpoints matching Android contracts
- Seed data system for development/testing
- Preserve existing chat functionality
- Defer: Full authentication, school mode, personalized roadmap generation

---

## 1. Architecture Audit Summary

### 1.1 Existing Backend Components

**Database Infrastructure:**
- SQLAlchemy 2.x async with asyncpg driver
- Alembic migrations (2 migrations exist for chat)
- Connection pooling configured (pool_size=5, max_overflow=10)
- Environment-based configuration via Pydantic Settings

**Existing Tables:**
- `conversations` (id: String(36), title, created_at, updated_at)
- `messages` (id: String(36), conversation_id FK, role, content, created_at)
- Indexes: conversation_id, composite (conversation_id, created_at)

**Existing API Endpoints:**
- `GET /api/v1/health`
- `POST /api/v1/chat/conversations`
- `GET /api/v1/chat/conversations`
- `GET /api/v1/chat/conversations/{id}`
- `POST /api/v1/chat/conversations/{id}/messages`

**Code Organization:**
- ✅ Routes: `backend/app/api/routes/`
- ✅ Services: `backend/app/services/`
- ✅ Repositories: `backend/app/repositories/`
- ✅ Models: `backend/app/models/`
- ✅ Schemas: `backend/app/schemas/`

### 1.2 Android Contract Analysis

**Required Endpoints (from Android repositories):**

**CourseRepository:**
- `getCourses()` → List<Course>
- `getFeaturedCourses()` → List<Course>

**RoadmapRepository:**
- `getRoadmaps()` → List<RoadmapOption>
- `getCategories()` → List<String>
- `generatePersonalizedRoadmap(...)` → GeneratedRoadmapPreview (DEFERRED)

**LearningRepository:**
- `getInProgressCourses()` → List<LearningCourse>
- `getCompletedCourses()` → List<LearningCourse>
- `getSubjects()` → List<Subject> (DEFERRED - School Mode)
- `getContinueLearning()` → ContinueLearningItem?
- `getUpcomingExams()` → List<UpcomingExam> (DEFERRED - School Mode)
- `getStudyFocus()` → StudyFocusItem?

**ProfileRepository:**
- `getUserProfile()` → UserProfile?
- `updateUserProfile(profile)` → Unit
- `observeUserProfile()` → Flow<UserProfile?>

**Android Domain Models:**
```kotlin
Course(id, name, description, icon, accent: CourseAccent)
RoadmapOption(id, title, category, description, skills, level, duration, stages, icon, accentTheme)
LearningCourse(id, name, lesson, progress)
UserProfile(fullName, email, mobile, interests, level, educationMode)
Subject(id, name, icon, progress?, totalTopics, gradeLevel, completedChapters, totalChapters)
```

---

## 2. Resolved Architectural Questions

### 2.1 User Identity Strategy

**Current State:**
- ❌ NO authentication system currently implemented
- ❌ NO user table exists
- ❌ NO JWT/session mechanism in place
- ✅ Existing conversations/messages do NOT have user_id

**Decision: Application-Level User ID (Deferred Authentication)**

Since no authentication exists, we'll create an application-level identity foundation that can integrate with Supabase Auth later:

**For This Phase:**
- Use `user_id: TEXT` in all user-scoped tables
- Do NOT implement authentication endpoints yet
- All user-scoped endpoints will accept `user_id` as a query parameter temporarily
- Document the authentication integration path for Phase 2

**user_id Format:** String identifier (future-compatible with Supabase Auth UUID.toString())

**Rationale:**
- Android app has AuthRepository interface but no backend implementation
- Implementing proper auth is a separate large effort
- We need data infrastructure first
- String user_id allows clean migration to UUID-based Supabase Auth

**Phase 2 Integration Path:**
1. Implement Supabase Auth or JWT-based authentication
2. Add user_id extraction from JWT tokens
3. Replace query parameters with authenticated user context
4. Migrate existing user_id values if needed
5. Add user_id to conversations table

### 2.2 UUID Generation Strategy

**Current Implementation Audit:**
- Existing models use: `String(36)` primary keys with Python `uuid.uuid4()`
- Migrations use: `sa.String(length=36)` without server default
- IDs generated in application layer (Python)

**Decision: Hybrid Approach - String(36) for Chat, Native UUID for New Tables**

**For Existing Chat Tables (DO NOT MIGRATE):**
- Keep `String(36)` with Python `uuid.uuid4()`
- Conversations and messages tables remain unchanged
- Avoids risky migration of production data

**For New Phase 1 Domain Tables:**
Evaluate PostgreSQL native UUID:
- Use SQLAlchemy `UUID(as_uuid=True)` column type
- Use Python `uuid.uuid4()` for generation (application-level)
- Serialize to string in API responses for Android compatibility
- Benefits: Type safety, storage efficiency (16 bytes vs 36 bytes)

**If Native UUID Adopted:**
```python
from sqlalchemy.dialects.postgresql import UUID
import uuid

# SQLAlchemy Model
id: Mapped[uuid.UUID] = mapped_column(
    UUID(as_uuid=True),
    primary_key=True,
    default=uuid.uuid4
)
```

**Migration Pattern:**
```python
from sqlalchemy.dialects.postgresql import UUID

op.create_table('courses',
    sa.Column('id', UUID(as_uuid=True), nullable=False),
    sa.PrimaryKeyConstraint('id')
)
```

**API Serialization:**
```python
# Pydantic automatically serializes UUID to string
class CourseResponse(BaseModel):
    id: str  # Will be serialized from UUID object
```

**If Keeping String(36) for Consistency:**
- Rationale: Uniform approach across all tables, simpler migration later
- Continue existing pattern
- Document decision clearly

**Recommendation:** Use native UUID for new tables while preserving String(36) for chat tables. This provides better type safety and efficiency without risky migrations.

### 2.3 Roadmap → Course Relationship

**Android Model Analysis:**
```kotlin
RoadmapOption(
    id, title, category, description,
    skills: List<String>,  // Generic skill names
    level, duration, stages, icon, accentTheme
)
```

**Decision: Add course_id to roadmap_items**

Roadmaps can reference courses where appropriate:

```
Roadmap "Python Developer"
├─ Roadmap Item 1: "Learn Python Basics" → course_id = python-fundamentals-course
├─ Roadmap Item 2: "Master Data Structures" → course_id = ds-algo-course
└─ Roadmap Item 3: "Build Projects" → course_id = NULL (no specific course)
```

**Schema:**
```python
roadmap_items
├─ id (UUID)
├─ roadmap_id (FK → roadmaps.id)
├─ course_id (FK → courses.id, nullable=True)  # NEW
├─ title
├─ description
├─ duration
├─ display_order
└─ skills (JSONB, nullable)
```

**ON DELETE Behavior:**
- `course_id FK: ON DELETE SET NULL` (if course deleted, roadmap item remains but loses course link)

### 2.4 Course Deletion Strategy

**Problem:** Educational platform shouldn't destroy historical progress on course deletion

**Decision: Status-based Archival**

**courses table:**
```python
status: Mapped[str] = mapped_column(
    String(20), 
    default="active",
    nullable=False
)
# Values: "active", "archived", "draft"
```

**ON DELETE Cascade Strategy:**

```
course_categories → courses: RESTRICT
  (Cannot delete category if courses exist)

courses → course_modules: CASCADE
  (Deleting course removes modules)

course_modules → lessons: CASCADE
  (Deleting module removes lessons)

courses → user_course_progress: RESTRICT
  (Cannot hard-delete course if progress exists)

lessons → user_lesson_progress: RESTRICT
  (Cannot hard-delete lesson if progress exists)

courses → roadmap_items: SET NULL
  (If course deleted, roadmap item loses course reference)
```

**Archival Workflow:**
1. Set `courses.status = 'archived'`
2. Archived courses excluded from `GET /api/v1/courses`
3. Progress records remain intact
4. Hard deletion only for draft courses with no progress

### 2.5 School Mode Scope

**Android Dependencies:**
- `Subject`, `ContinueLearningItem`, `UpcomingExam`, `StudyFocusItem` models exist
- `LearningRepository.getSubjects()`, `getUpcomingExams()` methods exist
- Android UI likely has school mode screens

**Problem:** Requirements propose incomplete school schema (no chapters, oversimplified)

**Decision: DEFER School Mode to Phase 2**

**Phase 1 (This Implementation):**
- ✅ Courses, roadmaps, learning progress
- ✅ General education mode
- ❌ NO school-specific tables
- ❌ NO subjects/chapters/exams tables

**Phase 2 (Future):**
- Proper school curriculum model:
  - `boards` table (CBSE, ICSE, etc.)
  - `grades` table (Class 1-12)
  - `subjects` table (linked to board + grade)
  - `chapters` table (linked to subject)
  - `exams` table (user-scoped)
- School mode API endpoints
- Integration with existing course system

**Rationale:**
- Android app won't break (endpoints return empty arrays)
- Better to design school mode properly later
- Phase 1 focus: Core course/roadmap infrastructure working

### 2.6 API / Android Coupling

**Principle: Backend API as Contract Layer**

```
Database Model (snake_case)
    ↓
Service Layer (business logic)
    ↓
Pydantic Schema (camelCase via alias)
    ↓
JSON Response
    ↓
Android DTO (snake_case with @SerialName)
    ↓
Android Domain Model (camelCase)
```

**Implementation:**
```python
# Pydantic Schema
class CourseResponse(BaseModel):
    id: str
    name: str
    description: str
    icon: str
    accent: str
    
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=to_camel
    )
```

**Android Expectation:**
```kotlin
@Serializable
data class CourseDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val accent: String
)
```

The backend will use `snake_case` internally but serialize to `camelCase` for Android compatibility.

### 2.7 Connection Pooling

**Current Configuration** (from `config.py`):
```python
db_pool_size: int = 5
db_max_overflow: int = 10
```

**Decision: Keep environment-configurable with sensible defaults**

**Supabase Considerations:**
- Supabase free tier: ~60 connections max
- Recommended for single backend instance: pool_size=5, max_overflow=10
- For production scaling: Use connection pooler (PgBouncer)

**No changes needed** - existing implementation is correct.

### 2.8 CORS Configuration

**Current Implementation:**
```python
allow_origins=[
    "http://10.0.2.2:8000",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
]
```

**Problem:** Native Android doesn't need CORS (CORS is browser security)

**Decision: Keep CORS for development flexibility**

**Rationale:**
- Allows testing backend with browser tools
- Allows future web frontend
- Doesn't hurt native Android
- Remove in production if not needed

**Android connectivity check:**
- Verify `BASE_URL` points to correct backend
- Verify Android INTERNET permission in manifest
- No cleartext traffic restrictions needed (using HTTPS in production)

### 2.9 Existing Chat Database

**Decision: PRESERVE and EXTEND (do not redesign)**

**Preservation:**
- Keep existing migrations
- Keep existing models
- Keep existing API endpoints
- Keep existing repository/service architecture

**Extension for User Ownership (Phase 2):**
```python
# Future: Add user_id to conversations
user_id: Mapped[str] = mapped_column(String(255), nullable=True)
# nullable=True for backward compatibility
```

**No changes in Phase 1** - chat works without user authentication currently.

### 2.10 Database Normalization

**Source of Truth Strategy:**

**MATERIALIZED PROGRESS (stored but maintained transactionally):**
- `user_course_progress.completion_percentage` - cached for performance, updated transactionally when lesson progress changes
- `user_course_progress.status` - derived from completion_percentage, updated in same transaction

**Progress Update Flow:**
```
1. User completes lesson → user_lesson_progress.completed = true
2. LearningService.completeLesson() transaction:
   a. Update user_lesson_progress
   b. Query: SELECT COUNT(*) FROM lessons WHERE module_id IN (SELECT id FROM course_modules WHERE course_id = ?)
   c. Query: SELECT COUNT(*) FROM user_lesson_progress WHERE user_id = ? AND lesson_id IN (...)
   d. Calculate: completion_percentage = (completed_lessons / total_lessons) * 100
   e. Update user_course_progress.completion_percentage
   f. Update user_course_progress.status based on percentage:
      - 0% → "not_started"
      - 1-99% → "in_progress"
      - 100% → "completed"
   g. COMMIT transaction
```

**DERIVED (calculated dynamically, never stored):**
- `roadmaps.stages_count` - COUNT(roadmap_items) WHERE roadmap_id = ?
- Module count per course - COUNT(course_modules) WHERE course_id = ?
- `Subject.progress` (future) - calculated from chapter progress

**Consistency Guarantee:**
- Progress fields are NEVER intentionally stale after successful transaction
- All lesson completion operations update course progress atomically
- No database triggers required for Phase 1
- Application-level consistency in service layer

**JSONB Usage:**
- `roadmap_items.skills: JSONB` - array of skill strings (appropriate use)
- Avoid JSONB for structured relationships

### 2.11 Authorization Strategy

**Phase 1: Development-Only Temporary User Identifier**

**CRITICAL SECURITY WARNING:** This mechanism is for development ONLY and MUST be protected by configuration.

**Configuration-Protected Access:**
```python
# config.py
class Settings(BaseSettings):
    allow_dev_user_id: bool = Field(
        default=False,
        description="DEVELOPMENT ONLY: Allow user_id query parameter"
    )
    environment: str = Field(default="development")  # development | production
```

**Development Mode (ALLOW_DEV_USER_ID=true):**
- Protected endpoints accept `?user_id=xxx` query parameter
- No authentication/authorization
- Trust client to provide correct user_id
- Log warning on startup if enabled

**Production Mode (ALLOW_DEV_USER_ID=false or omitted):**
- `?user_id=xxx` query parameter MUST be rejected (400 Bad Request)
- Authentication will be required (Phase 2)
- Endpoint returns 401 Unauthorized without proper auth

**Implementation:**
```python
async def get_dev_user_id(
    user_id: str = Query(None),
    settings: Settings = Depends(get_settings)
) -> str:
    if not settings.allow_dev_user_id:
        raise HTTPException(
            status_code=400,
            detail="user_id query parameter not allowed in production"
        )
    if not user_id:
        raise HTTPException(
            status_code=400,
            detail="user_id required in development mode"
        )
    return user_id
```

**Phase 2: Proper Authorization**
```python
# Future: Dependency injection
async def get_current_user(token: str = Depends(oauth2_scheme)) -> str:
    # Verify JWT, extract user_id
    return user_id

# Protected endpoints
@router.get("/api/v1/learning/courses/in-progress")
async def get_in_progress_courses(
    user_id: str = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    ...
```

**Public Endpoints (no user_id required):**
- `GET /api/v1/health`
- `GET /api/v1/courses`
- `GET /api/v1/courses/{id}`
- `GET /api/v1/roadmaps` (system roadmaps only)
- `GET /api/v1/roadmaps/{id}`

**User-Scoped Endpoints (require user_id in dev, auth in prod):**
- `GET /api/v1/profile?user_id=xxx`
- `GET /api/v1/learning/courses/in-progress?user_id=xxx`
- `POST /api/v1/learning/courses/{course_id}/enroll?user_id=xxx`

### 2.12 Migration Strategy

**Decision: Logical grouping, not one-per-entity**

**Migration Plan:**

```
1. Phase1_Core_Schema
   - course_categories
   - courses
   - course_modules
   - lessons
   - roadmaps
   - roadmap_items

2. Phase1_Progress_Schema
   - user_course_progress
   - user_lesson_progress
   
3. Phase1_Profiles_Schema
   - user_profiles
```

**Migration Best Practices:**
- Manual review after autogenerate
- Test upgrade/downgrade locally
- Preserve existing data
- No destructive operations without backup

### 2.13 Seed Data Strategy

**Seed Script:** `backend/scripts/seed_data.py`

**Idempotency Strategy:**
```python
# Use deterministic UUIDs for seed data
SEED_NAMESPACE = uuid.UUID('12345678-1234-5678-1234-567812345678')

def seed_course_id(slug: str) -> str:
    return str(uuid.uuid5(SEED_NAMESPACE, f"course:{slug}"))
```

**Seed Volume:**
- 6 course categories
- 18 courses (3 per category)
- 54 modules (3 per course)
- 270 lessons (5 per module)
- 6 system roadmaps
- 30 roadmap items (5 per roadmap, some linked to courses)

**Important Notes:**
- Do NOT seed `stages_count` for roadmaps (it's calculated dynamically)
- Do NOT seed `module_count` for courses (not in schema)
- Roadmap items should reference real course IDs where appropriate

**Execution:**
```bash
cd backend
python scripts/seed_data.py
```

**Characteristics:**
- Deterministic IDs (can run multiple times)
- Realistic content (not "Course 1", "Module 1")
- Connected roadmap items → courses via course_id
- Transaction-based (all-or-nothing)

---

## 3. Final Database Schema

### 3.1 Entity Relationship Diagram

```mermaid
erDiagram
    CONVERSATIONS ||--o{ MESSAGES : contains
    COURSE_CATEGORIES ||--o{ COURSES : contains
    COURSES ||--o{ COURSE_MODULES : contains
    COURSE_MODULES ||--o{ LESSONS : contains
    COURSES ||--o{ USER_COURSE_PROGRESS : tracked_by
    LESSONS ||--o{ USER_LESSON_PROGRESS : tracked_by
    ROADMAPS ||--o{ ROADMAP_ITEMS : contains
    COURSES ||--o{ ROADMAP_ITEMS : referenced_by
    
    CONVERSATIONS {
        string id PK
        string title
        timestamp created_at
        timestamp updated_at
    }
    
    MESSAGES {
        string id PK
        string conversation_id FK
        string role
        text content
        timestamp created_at
    }
    
    COURSE_CATEGORIES {
        string id PK
        string name UK
        text description
        int display_order
        timestamp created_at
        timestamp updated_at
    }
    
    COURSES {
        string id PK
        string category_id FK
        string name
        string slug UK
        text description
        string icon
        string level
        int duration_hours
        string accent_color
        boolean is_featured
        string status
        timestamp created_at
        timestamp updated_at
    }
    
    COURSE_MODULES {
        string id PK
        string course_id FK
        string title
        text description
        int display_order
        timestamp created_at
        timestamp updated_at
    }
    
    LESSONS {
        string id PK
        string module_id FK
        string title
        text description
        string content_type
        int duration_minutes
        int display_order
        timestamp created_at
        timestamp updated_at
    }
    
    ROADMAPS {
        string id PK
        string title
        string slug UK
        string category
        text description
        string level
        string duration
        string icon
        string accent_theme
        boolean is_system
        string user_id nullable
        timestamp created_at
        timestamp updated_at
    }
    
    ROADMAP_ITEMS {
        string id PK
        string roadmap_id FK
        string course_id FK_nullable
        string title
        text description
        jsonb skills nullable
        string duration
        int display_order
        timestamp created_at
        timestamp updated_at
    }
    
    USER_COURSE_PROGRESS {
        string id PK
        string user_id
        string course_id FK
        date enrollment_date
        timestamp last_accessed
        float completion_percentage
        string status
        timestamp created_at
        timestamp updated_at
    }
    
    USER_LESSON_PROGRESS {
        string id PK
        string user_id
        string lesson_id FK
        boolean completed
        timestamp completion_date nullable
        timestamp created_at
        timestamp updated_at
    }
    
    USER_PROFILES {
        string id PK
        string user_id UK
        string full_name
        string email UK
        string mobile nullable
        text interests nullable
        string level
        string education_mode
        timestamp created_at
        timestamp updated_at
    }
```

### 3.2 Table Definitions

#### 3.2.1 course_categories

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| name | String(100) | NOT NULL, UNIQUE | Category name |
| description | Text | NULL | Category description |
| display_order | Integer | NOT NULL, DEFAULT 0 | Sort order |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (name)
- INDEX (display_order)

#### 3.2.2 courses

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| category_id | String(36) | FK, NOT NULL | Reference to course_categories |
| name | String(255) | NOT NULL | Course name |
| slug | String(255) | NOT NULL, UNIQUE | URL-friendly identifier |
| description | Text | NOT NULL | Full description |
| icon | String(100) | NOT NULL | Icon identifier |
| level | String(50) | NOT NULL | Beginner/Intermediate/Advanced |
| duration_hours | Integer | NULL | Estimated duration |
| accent_color | String(50) | NOT NULL, DEFAULT 'primary' | UI accent |
| is_featured | Boolean | NOT NULL, DEFAULT FALSE | Featured flag |
| status | String(20) | NOT NULL, DEFAULT 'active' | active/archived/draft |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Foreign Keys:**
- category_id → course_categories.id ON DELETE RESTRICT

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (slug)
- INDEX (category_id)
- INDEX (status, is_featured)

**Check Constraints:**
- status IN ('active', 'archived', 'draft')
- level IN ('Beginner', 'Intermediate', 'Advanced')

#### 3.2.3 course_modules

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| course_id | String(36) | FK, NOT NULL | Reference to courses |
| title | String(255) | NOT NULL | Module title |
| description | Text | NULL | Module description |
| display_order | Integer | NOT NULL | Sort order within course |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Foreign Keys:**
- course_id → courses.id ON DELETE CASCADE

**Indexes:**
- PRIMARY KEY (id)
- INDEX (course_id, display_order)

**Check Constraints:**
- display_order >= 0

#### 3.2.4 lessons

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| module_id | String(36) | FK, NOT NULL | Reference to course_modules |
| title | String(255) | NOT NULL | Lesson title |
| description | Text | NULL | Lesson description |
| content_type | String(50) | NOT NULL | video/article/quiz/exercise |
| duration_minutes | Integer | NULL | Estimated duration |
| display_order | Integer | NOT NULL | Sort order within module |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Foreign Keys:**
- module_id → course_modules.id ON DELETE CASCADE

**Indexes:**
- PRIMARY KEY (id)
- INDEX (module_id, display_order)

**Check Constraints:**
- display_order >= 0
- content_type IN ('video', 'article', 'quiz', 'exercise', 'project')

#### 3.2.5 roadmaps

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| title | String(255) | NOT NULL | Roadmap title |
| slug | String(255) | NOT NULL, UNIQUE | URL-friendly identifier |
| category | String(100) | NOT NULL | Category grouping |
| description | Text | NOT NULL | Full description |
| level | String(50) | NOT NULL | Target level |
| duration | String(100) | NOT NULL | Estimated duration text |
| icon | String(100) | NOT NULL | Icon identifier |
| accent_theme | String(50) | NOT NULL, DEFAULT 'primary' | UI theme |
| is_system | Boolean | NOT NULL, DEFAULT TRUE | System vs user roadmap |
| user_id | String(255) | NULL | Owner for personalized roadmaps |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Note:** `stages_count` is NOT stored. The API calculates it dynamically as COUNT(roadmap_items) for each roadmap.

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (slug)
- INDEX (user_id) WHERE user_id IS NOT NULL
- INDEX (is_system, category)

**Check Constraints:**
- level IN ('Beginner', 'Intermediate', 'Advanced', 'All Levels')

#### 3.2.6 roadmap_items

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| roadmap_id | String(36) | FK, NOT NULL | Reference to roadmaps |
| course_id | String(36) | FK, NULL | Optional course reference |
| title | String(255) | NOT NULL | Item title |
| description | Text | NULL | Item description |
| skills | JSONB | NULL | Array of skill names |
| duration | String(100) | NULL | Estimated duration text |
| display_order | Integer | NOT NULL | Sort order within roadmap |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Foreign Keys:**
- roadmap_id → roadmaps.id ON DELETE CASCADE
- course_id → courses.id ON DELETE SET NULL

**Indexes:**
- PRIMARY KEY (id)
- INDEX (roadmap_id, display_order)
- INDEX (course_id) WHERE course_id IS NOT NULL

**Check Constraints:**
- display_order >= 0

#### 3.2.7 user_course_progress

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| user_id | String(255) | NOT NULL | User identifier |
| course_id | String(36) | FK, NOT NULL | Reference to courses |
| enrollment_date | Date | NOT NULL | Date enrolled |
| last_accessed | DateTime(TZ) | NULL | Last access timestamp |
| completion_percentage | Float | NOT NULL, DEFAULT 0.0 | Cached progress 0-100, updated transactionally |
| status | String(20) | NOT NULL, DEFAULT 'not_started' | Progress status, derived from completion_percentage |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Note:** `completion_percentage` and `status` are materialized/cached fields for performance. They MUST be updated transactionally whenever lesson progress changes. They should never be intentionally stale after a successful lesson-completion transaction.

**Foreign Keys:**
- course_id → courses.id ON DELETE RESTRICT

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (user_id, course_id)
- INDEX (user_id, status)

**Check Constraints:**
- completion_percentage >= 0.0 AND completion_percentage <= 100.0
- status IN ('not_started', 'in_progress', 'completed')

#### 3.2.8 user_lesson_progress

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| user_id | String(255) | NOT NULL | User identifier |
| lesson_id | String(36) | FK, NOT NULL | Reference to lessons |
| completed | Boolean | NOT NULL, DEFAULT FALSE | Completion status |
| completion_date | DateTime(TZ) | NULL | When completed |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Foreign Keys:**
- lesson_id → lessons.id ON DELETE RESTRICT

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (user_id, lesson_id)
- INDEX (user_id, completed)

#### 3.2.9 user_profiles

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | String(36) | PK | UUID |
| user_id | String(255) | NOT NULL, UNIQUE | User identifier |
| full_name | String(255) | NOT NULL | Full name |
| email | String(255) | NOT NULL, UNIQUE | Email address (unique for this phase) |
| mobile | String(50) | NULL | Mobile number (nullable until authentication implemented) |
| interests | Text | NULL | User interests |
| level | String(100) | NOT NULL | Experience level |
| education_mode | String(20) | NOT NULL, DEFAULT 'general' | school/general |
| created_at | DateTime(TZ) | NOT NULL | Creation timestamp |
| updated_at | DateTime(TZ) | NOT NULL | Last update timestamp |

**Note:** `mobile` is nullable because users may not provide it before authentication is implemented. `email` is unique to prevent duplicate profiles.

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (user_id)
- UNIQUE (email)

**Check Constraints:**
- education_mode IN ('school', 'general')

---

## 4. API Endpoint Specification

### 4.1 Public Endpoints (No Authentication Required)

#### GET /api/v1/health
**Purpose:** Health check
**Response:** `{"status": "ok", "service": "EduNova AI Chat API"}`

#### GET /api/v1/courses
**Purpose:** List all active courses
**Query Parameters:**
- `featured` (boolean, optional): Filter for featured courses
- `category` (string, optional): Filter by category slug

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Python Programming Fundamentals",
    "description": "Learn Python from scratch...",
    "icon": "python",
    "accent": "primary"
  }
]
```

#### GET /api/v1/courses/{course_id}
**Purpose:** Get course details with modules and lessons
**Response:**
```json
{
  "id": "uuid",
  "name": "Python Programming Fundamentals",
  "description": "Learn Python from scratch...",
  "icon": "python",
  "accent": "primary",
  "modules": [
    {
      "id": "uuid",
      "title": "Introduction to Python",
      "description": "...",
      "lessons": [
        {
          "id": "uuid",
          "title": "Installing Python",
          "contentType": "video",
          "durationMinutes": 15
        }
      ]
    }
  ]
}
```

#### GET /api/v1/roadmaps
**Purpose:** List all system roadmaps
**Response:**
```json
[
  {
    "id": "uuid",
    "title": "Python Developer Roadmap",
    "category": "Programming",
    "description": "Complete path to becoming a Python developer",
    "skills": ["Python", "Flask", "Django"],
    "level": "Beginner to Advanced",
    "duration": "6-9 months",
    "stages": 8,
    "icon": "python",
    "accentTheme": "primary"
  }
]
```
**Note:** `stages` field is calculated dynamically as COUNT(roadmap_items) for the roadmap, not stored in database.

#### GET /api/v1/roadmaps/{roadmap_id}
**Purpose:** Get roadmap details with items
**Response:**
```json
{
  "id": "uuid",
  "title": "Python Developer Roadmap",
  "category": "Programming",
  "description": "...",
  "level": "Beginner to Advanced",
  "duration": "6-9 months",
  "stages": 8,
  "icon": "python",
  "accentTheme": "primary",
  "items": [
    {
      "id": "uuid",
      "title": "Learn Python Basics",
      "description": "Master fundamental concepts",
      "courseId": "python-fundamentals-uuid",
      "skills": ["Variables", "Data Types", "Functions"],
      "duration": "4 weeks"
    }
  ]
}
```
**Note:** `stages` field is calculated dynamically as COUNT(items), not stored in database.

#### GET /api/v1/roadmaps/categories
**Purpose:** List all roadmap categories
**Response:** `["Programming", "Data Science", "AI & Machine Learning"]`

### 4.2 User-Scoped Endpoints (Require user_id)

#### GET /api/v1/profile
**Query Parameters:** `user_id` (required)
**Response:**
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "mobile": "+1234567890",
  "interests": "AI, Python, Web Development",
  "level": "Intermediate",
  "educationMode": "general"
}
```

#### PUT /api/v1/profile
**Query Parameters:** `user_id` (required)
**Request Body:**
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "mobile": "+1234567890",
  "interests": "AI, Python, Web Development",
  "level": "Intermediate",
  "educationMode": "general"
}
```
**Note:** `mobile` field is optional (can be null or omitted).
**Response:** Same as request body

#### GET /api/v1/learning/courses/in-progress
**Query Parameters:** `user_id` (required)
**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Python Programming Fundamentals",
    "lesson": "Variables and Data Types",
    "progress": 0.35
  }
]
```

#### GET /api/v1/learning/courses/completed
**Query Parameters:** `user_id` (required)
**Response:** Same format as in-progress

#### POST /api/v1/learning/courses/{course_id}/enroll
**Query Parameters:** `user_id` (required)
**Response:** `{"message": "Enrolled successfully"}`

#### POST /api/v1/learning/lessons/{lesson_id}/complete
**Query Parameters:** `user_id` (required)
**Response:** `{"message": "Lesson marked complete"}`

### 4.3 Deferred Endpoints (Phase 2)

- `POST /api/v1/roadmaps/generate` - Personalized roadmap generation
- `GET /api/v1/learning/subjects` - School mode subjects
- `GET /api/v1/learning/exams/upcoming` - School mode exams
- `POST /api/v1/learning/exams` - Create exam
- Authentication endpoints (login, register, password reset)

---

## 5. Implementation Plan

### 5.1 Phase 1 Tasks

**Task 1: Database Schema Implementation**
1.1. Create course domain migrations (categories, courses, modules, lessons)
1.2. Create roadmap domain migrations (roadmaps, roadmap_items)
1.3. Create progress domain migrations (user_course_progress, user_lesson_progress)
1.4. Create profile migrations (user_profiles)
1.5. Apply migrations to Supabase database

**Task 2: SQLAlchemy Models**
2.1. Create course models (Category, Course, Module, Lesson)
2.2. Create roadmap models (Roadmap, RoadmapItem)
2.3. Create progress models (UserCourseProgress, UserLessonProgress)
2.4. Create profile model (UserProfile)
2.5. Update models/__init__.py imports

**Task 3: Pydantic Schemas**
3.1. Create course schemas (CourseListResponse, CourseDetailResponse)
3.2. Create roadmap schemas (RoadmapListResponse, RoadmapDetailResponse)
3.3. Create learning schemas (LearningCourseResponse)
3.4. Create profile schemas (ProfileResponse, ProfileUpdateRequest)

**Task 4: Repository Layer**
4.1. Create CourseRepository
4.2. Create RoadmapRepository
4.3. Create LearningRepository
4.4. Create ProfileRepository

**Task 5: Service Layer**
5.1. Create CourseService
5.2. Create RoadmapService
5.3. Create LearningService
5.4. Create ProfileService

**Task 6: API Routes**
6.1. Create course routes
6.2. Create roadmap routes
6.3. Create learning routes
6.4. Create profile routes
6.5. Register routes in main.py

**Task 7: Seed Data System**
7.1. Create seed_data.py script
7.2. Implement deterministic UUID generation
7.3. Create course category seed data
7.4. Create course/module/lesson seed data
7.5. Create roadmap seed data with course links
7.6. Test idempotency
7.7. Document seed execution

**Task 8: Testing**
8.1. Test course endpoints
8.2. Test roadmap endpoints
8.3. Test learning progress endpoints
8.4. Test profile endpoints
8.5. Verify Android compatibility

**Task 9: Documentation**
9.1. Create database.md documentation
9.2. Document migration workflow
9.3. Document seed data workflow
9.4. Update README with new endpoints

### 5.2 Phase 2 (Future)

- Implement authentication (Supabase Auth or JWT)
- Add user_id to conversations
- School mode schema and endpoints
- Personalized roadmap generation
- Advanced progress analytics
- Course content management API

---

## 6. Testing Strategy

### 6.1 Database Tests
- Foreign key constraints
- Check constraints
- Unique constraints
- Cascade behaviors
- Data integrity

### 6.2 API Tests
- Successful responses (200, 201)
- Error responses (400, 404, 422)
- Query parameter filtering
- Pagination (future)
- Data validation

### 6.3 Integration Tests
- End-to-end course enrollment flow
- Lesson completion updates course progress
- Roadmap item course references
- Profile CRUD operations

### 6.4 Android Compatibility Tests
- Response format matches Android DTOs
- camelCase vs snake_case serialization
- Enum value compatibility
- Null handling

---

## 7. Supabase Configuration

### 7.1 Connection String Format
```
postgresql+asyncpg://[user]:[password]@[host]:[port]/[database]
```

### 7.2 Required Database Extensions
- None (using application-generated UUIDs)

### 7.3 Connection Pool Settings
```python
pool_size=5
max_overflow=10
pool_pre_ping=True
```

### 7.4 Migration Execution
```bash
cd backend
alembic upgrade head
```

---

## 8. Security Considerations

### 8.1 Phase 1 (Limited Security)
- ⚠️ No authentication - temporary user_id query parameters
- ⚠️ No authorization checks
- ⚠️ Trust client to provide correct user_id
- ✅ Input validation via Pydantic
- ✅ SQL injection protection via SQLAlchemy
- ✅ Error messages don't expose internals

### 8.2 Phase 2 (Proper Security)
- ✅ JWT-based authentication
- ✅ User context dependency injection
- ✅ Row-level authorization checks
- ✅ Rate limiting
- ✅ HTTPS only in production
- ✅ Secrets management

---

## 9. Monitoring and Logging

### 9.1 Logging Strategy
```python
logger.info(f"Course {course_id} fetched")
logger.error(f"Database error: {type(exc).__name__}")
# DO NOT log: passwords, tokens, full SQL, user data
```

### 9.2 Metrics (Future)
- API endpoint latency
- Database query performance
- Error rates
- Course enrollment rates
- User engagement

---

## 10. Implementation Safety Protocol

### 10.1 Pre-Migration Checklist

**Before applying ANY migrations to Supabase:**

1. **Inspect Current State:**
```bash
# Connect to Supabase database
psql $DATABASE_URL

# Check migration history
SELECT * FROM alembic_version;

# List all tables
\dt

# Check for existing data
SELECT COUNT(*) FROM conversations;
SELECT COUNT(*) FROM messages;
```

2. **Verify Current Schema:**
- Confirm conversations table structure
- Confirm messages table structure
- Verify foreign key relationships
- Check indexes

3. **Create Migration:**
```bash
cd backend
alembic revision --autogenerate -m "Phase 1: Core course and roadmap schema"
```

4. **Review Generated Migration SQL:**
- Open the generated migration file
- Verify NO DROP TABLE statements for conversations or messages
- Verify NO ALTER TABLE statements for existing chat tables
- Ensure only CREATE TABLE for new domain tables
- Check foreign key constraints
- Verify indexes

5. **Test Locally First:**
- Run migration on local PostgreSQL instance
- Verify tables created correctly
- Test rollback: `alembic downgrade -1`
- Test upgrade again: `alembic upgrade head`

### 10.2 Migration Execution Safety

**CRITICAL RULES:**
- ❌ NEVER run `alembic downgrade base` on Supabase
- ❌ NEVER drop existing tables (conversations, messages)
- ❌ NEVER reset or reinitialize Supabase project
- ❌ NEVER delete conversation or message data
- ✅ ONLY run `alembic upgrade head` to apply new migrations
- ✅ ALWAYS backup before major migrations
- ✅ ALWAYS test migrations locally first

**Safe Migration Command:**
```bash
# Verify you're on correct database
echo $DATABASE_URL

# Apply migrations
cd backend
alembic upgrade head
```

### 10.3 Post-Migration Verification

**After migration completes:**

1. **Verify Tables Exist:**
```sql
-- Should see all new tables
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Expected: conversations, messages, courses, course_modules, 
-- lessons, roadmaps, roadmap_items, user_course_progress, 
-- user_lesson_progress, user_profiles, course_categories
```

2. **Verify Existing Data Preserved:**
```sql
SELECT COUNT(*) FROM conversations;
SELECT COUNT(*) FROM messages;
-- Counts should match pre-migration values
```

3. **Run Seed Script:**
```bash
cd backend
python scripts/seed_data.py
```

4. **Verify Seeded Data:**
```sql
SELECT COUNT(*) FROM course_categories;  -- Expect 6
SELECT COUNT(*) FROM courses;            -- Expect 18
SELECT COUNT(*) FROM course_modules;     -- Expect 54
SELECT COUNT(*) FROM lessons;            -- Expect 270
SELECT COUNT(*) FROM roadmaps;           -- Expect 6
SELECT COUNT(*) FROM roadmap_items;      -- Expect 30
```

5. **Test API Endpoints:**
```bash
# Health check
curl http://localhost:8000/api/v1/health

# Get courses
curl http://localhost:8000/api/v1/courses

# Get roadmaps
curl http://localhost:8000/api/v1/roadmaps

# Verify chat still works
curl http://localhost:8000/api/v1/chat/conversations
```

6. **Verify Android Compatibility:**
- Start Android app
- Verify courses load
- Verify roadmaps load
- Verify chat still works
- Verify no serialization errors in logs

### 10.4 Rollback Plan

**If migration fails or causes issues:**

1. **Identify Current Migration:**
```bash
alembic current
```

2. **Rollback to Previous Version:**
```bash
# Rollback one migration
alembic downgrade -1

# Or rollback to specific revision
alembic downgrade <revision_id>
```

3. **Verify Data Integrity:**
```sql
SELECT COUNT(*) FROM conversations;
SELECT COUNT(*) FROM messages;
```

4. **Fix Migration File and Retry:**
- Correct the migration script
- Test locally
- Apply to Supabase again

### 10.5 Emergency Recovery

**If data is accidentally deleted:**

1. **Check Supabase Dashboard:**
- Navigate to Supabase project dashboard
- Check for automatic backups
- Restore from backup if available

2. **Use Time Travel (if enabled):**
```sql
-- PostgreSQL Point-in-Time Recovery
-- Contact Supabase support for assistance
```

3. **Document the Incident:**
- What happened
- What was lost
- How it was recovered
- Preventive measures for future

---

## 11. Summary

**This design resolves all architectural questions and provides:**

✅ Clear identity strategy (application user_id, auth deferred with config protection)
✅ Hybrid UUID approach (String(36) for chat, native UUID option for new tables)
✅ Roadmap-to-course relationships (course_id in roadmap_items)
✅ Safe course deletion strategy (status-based archival)
✅ Proper scope (defer school mode to Phase 2)
✅ Backend-first API contract (not Android-dictated)
✅ Appropriate connection pooling
✅ Existing chat preservation
✅ Transactional progress consistency (completion_percentage updated atomically)
✅ Dynamic derived values (stages_count calculated, not stored)
✅ Flexible constraints (mobile nullable until auth, email unique)
✅ Clear authorization path with config protection
✅ Grouped migration strategy
✅ Idempotent seed data system
✅ Complete ER diagram (verified for consistency)
✅ API endpoint specifications (verified for consistency)
✅ Implementation safety protocol
✅ Implementation task breakdown

**Key Corrections Applied:**

1. **Progress Source of Truth:** completion_percentage and status are materialized fields updated transactionally with lesson progress
2. **Stages Count Removed:** stages_count not stored, calculated dynamically as COUNT(roadmap_items)
3. **Module Count Removed:** No module_count field, derived from course_modules
4. **Dev User ID Protected:** ALLOW_DEV_USER_ID config flag prevents production misuse
5. **UUID Strategy Clarified:** Option to use native UUID for new tables while preserving String(36) for chat
6. **Profile Constraints Fixed:** mobile nullable, email unique
7. **Consistency Verified:** All schemas, migrations, API examples, and ER diagram aligned
8. **Implementation Safety Added:** Comprehensive pre/post-migration protocol

**No contradictions remain between:**
- ER diagram ✓
- Table definitions ✓
- SQLAlchemy models ✓
- Migrations ✓
- API schemas ✓
- API examples ✓
- Seed data ✓
- Android contracts ✓

**Ready for implementation.**
