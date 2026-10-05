# EduNova Content Quality & Presentation Pass - Requirements

## 1. Overview

The EduNova backend foundation is working correctly - Android successfully receives data from FastAPI/Supabase. However, the seeded content quality and visual presentation need improvement to make the app production-ready.

**Current Problems:**
- Course titles are unnecessarily long
- Course descriptions are too verbose for card layouts
- Roadmap icons display as text ("mobi", "le", "chart") instead of actual icons
- Course/roadmap content feels generic rather than intentional
- Icon mapping from backend to Android UI is broken

**Goal:** Improve database content and presentation mapping so the existing UI displays polished, student-friendly, production-ready content.

## 2. Scope Constraints

### In Scope
- Curating high-quality course and roadmap seed data
- Fixing icon/image mapping from backend to Android
- Ensuring content fits existing UI layouts
- Implementing semantic icon resolution
- Creating deterministic, idempotent seed process
- Verifying end-to-end data flow

### Out of Scope
- UI/UX redesign
- Navigation architecture changes
- Database schema restructuring
- Authentication implementation
- School mode (Phase 2)
- Personalized roadmap generation
- Chat/Gemini modifications
- New features or dependencies

## 3. User Stories

### US-1: Student Views Polished Course Content
**As a** student using EduNova  
**I want to** see well-written, concise course titles and descriptions  
**So that** I can quickly understand what each course offers

**Acceptance Criteria:**
- AC-1.1: Course titles are 2-4 words maximum
- AC-1.2: Course descriptions are 15-22 words for card display
- AC-1.3: All 18 courses have meaningful, education-focused content
- AC-1.4: Course descriptions accurately reflect learning outcomes
- AC-1.5: No generic placeholder text like "Learn programming with this course"

### US-2: Student Views Proper Course Icons
**As a** student browsing courses  
**I want to** see meaningful icons for each course  
**So that** I can quickly identify course topics visually

**Acceptance Criteria:**
- AC-2.1: Course cards display Material icons, not text strings
- AC-2.2: Icon selection is semantically appropriate (Python → code icon, Data Science → analytics icon)
- AC-2.3: Unknown icon keys fall back to a generic academic icon
- AC-2.4: No raw database values like "python" or "data_science" appear as visible text

### US-3: Student Views Polished Roadmap Content
**As a** student exploring learning paths  
**I want to** see well-structured roadmaps with clear stages  
**So that** I can plan my learning journey

**Acceptance Criteria:**
- AC-3.1: All 6 roadmaps have clear, concise titles
- AC-3.2: Roadmap descriptions are 15-25 words
- AC-3.3: Each roadmap has 4-5 meaningful stages
- AC-3.4: Stage names clearly indicate learning progression
- AC-3.5: Roadmap items reference valid course IDs where appropriate

### US-4: Student Views Proper Roadmap Icons
**As a** student viewing roadmaps  
**I want to** see appropriate icons for each roadmap  
**So that** I can visually distinguish different career paths

**Acceptance Criteria:**
- AC-4.1: Roadmap cards display Material icons, not text fragments
- AC-4.2: No text like "mobi", "le", or "chart" appears in icon areas
- AC-4.3: Icon selection matches roadmap category (Mobile → device icon, Cloud → cloud icon)
- AC-4.4: Unknown icon keys fall back to a generic roadmap icon

### US-5: Developer Seeds Curated Content
**As a** developer setting up the environment  
**I want to** run a deterministic seed script  
**So that** the app displays consistent, high-quality demo content

**Acceptance Criteria:**
- AC-5.1: Seed script creates exactly 18 courses across 6 categories
- AC-5.2: Seed script creates exactly 6 system roadmaps
- AC-5.3: Running seed multiple times doesn't create duplicates
- AC-5.4: Seed preserves chat data and Alembic version table
- AC-5.5: Seed script has optional `--reset` flag for development

### US-6: Developer Verifies Icon Resolution
**As a** developer  
**I want to** have centralized icon resolution logic  
**So that** backend semantic keys map correctly to Android icons

**Acceptance Criteria:**
- AC-6.1: Android has centralized icon resolver function
- AC-6.2: Icon resolver maps semantic keys to Material icons
- AC-6.3: Icon resolver provides safe fallback for unknown keys
- AC-6.4: Icon resolver is used consistently across all UI components
- AC-6.5: No UI component directly displays icon key strings

## 4. Data Specifications

### Course Data Structure
**18 Courses across 6 Categories:**

1. **Programming** (3 courses)
   - Python Fundamentals
   - Java Programming
   - JavaScript Essentials

2. **Data & AI** (3 courses)
   - Data Science with Python
   - Machine Learning Fundamentals
   - Deep Learning & Neural Networks

3. **Development** (3 courses)
   - Android Development with Kotlin
   - Web Development Fundamentals
   - Full Stack Development

4. **Computer Science** (3 courses)
   - Data Structures & Algorithms
   - Database Management
   - Operating Systems

5. **Cloud & Cybersecurity** (3 courses)
   - Cloud Computing Fundamentals
   - Cybersecurity Essentials
   - Computer Networks

6. **Practical & Career Skills** (3 courses)
   - Git & GitHub
   - Generative AI Fundamentals
   - Software Engineering Fundamentals

### Roadmap Data Structure
**6 System Roadmaps:**

1. Mobile App Developer Roadmap (4 stages)
2. Data Scientist Roadmap (5 stages)
3. Full Stack Developer Roadmap (5 stages)
4. AI & Machine Learning Roadmap (5 stages)
5. Cybersecurity Roadmap (5 stages)
6. Cloud Engineer Roadmap (5 stages)

### Icon Semantic Keys
**Backend stores semantic keys, Android resolves to icons:**

- `python` → Code/Terminal icon
- `java` → Code icon
- `javascript` → Code icon
- `data_science` → Analytics/Chart icon
- `machine_learning` → Psychology/Brain icon
- `deep_learning` → Memory/Neural icon
- `android` → Android/Phone icon
- `web_development` → Language/Web icon
- `full_stack` → Layers/Stack icon
- `algorithms` → Functions/Algorithm icon
- `database` → Database icon
- `operating_systems` → Computer/System icon
- `cloud` → Cloud icon
- `cybersecurity` → Security/Shield icon
- `networking` → Hub/Network icon
- `git` → Source/Branch icon
- `generative_ai` → AutoAwesome/AI icon
- `software_engineering` → Engineering/Build icon
- `mobile_development` → Phone/Device icon
- `artificial_intelligence` → Psychology/AI icon
- `cloud_computing` → Cloud icon

## 5. Technical Requirements

### TR-1: Backend Seed Process
- Seed script must be idempotent
- Seed script must preserve Alembic version table
- Seed script must preserve chat conversations and messages
- Seed script must support `--reset` flag for development
- Reset operation must verify development environment
- All course/module/lesson data must remain valid

### TR-2: Android Icon Resolution
- Centralized icon resolver function or object
- Maps semantic strings to Material Icons
- Provides safe fallback icon (School/AutoAwesome)
- Never displays raw icon key as text
- Used by all course and roadmap UI components

### TR-3: Data Flow Integrity
- Supabase → FastAPI → Android API → Repository → ViewModel → Compose
- Offline-first architecture preserved
- API contracts remain intact (camelCase JSON)
- No hardcoded course/roadmap data overriding API
- Local cache/fallback mechanism preserved

### TR-4: Content Quality Standards
- Course titles: 2-4 words, 1-2 lines
- Course descriptions: 15-22 words, 3-4 lines max
- Roadmap titles: 2-4 words, 1-2 lines
- Roadmap descriptions: 15-25 words
- All content education-focused, not marketing fluff
- No lorem ipsum or obvious placeholder content

## 6. Verification Requirements

### Backend Verification
- [ ] Seed script executes without errors
- [ ] 18 courses created
- [ ] 6 roadmaps created
- [ ] No duplicate rows
- [ ] GET /api/v1/courses returns all courses
- [ ] GET /api/v1/courses/{id} returns valid course details
- [ ] GET /api/v1/roadmaps returns all roadmaps
- [ ] GET /api/v1/roadmaps/{id} returns valid roadmap details
- [ ] Icon keys are semantic strings, not UI references

### Android Verification
- [ ] App builds successfully
- [ ] Home screen displays courses from API
- [ ] Course cards show icons, not text strings
- [ ] Course titles fit in 1-2 lines
- [ ] Course descriptions fit in 3-4 lines
- [ ] Roadmap screen displays roadmaps from API
- [ ] Roadmap cards show icons, not "mobi"/"le"/"chart"
- [ ] Roadmap titles and descriptions fit properly
- [ ] Unknown icon keys display fallback icon
- [ ] No crashes when encountering new icon keys

## 7. Non-Functional Requirements

### NFR-1: Offline-First Compliance
- Changes must not break offline functionality
- Local repository pattern preserved
- Cache mechanism remains intact
- Backend is remote source of truth

### NFR-2: Maintainability
- Icon resolution logic is centralized and reusable
- Seed script is well-documented
- Content changes require only seed file modification
- No hardcoded UI strings for course/roadmap data

### NFR-3: Development Experience
- Seed process is simple: `python scripts/seed_data.py --reset`
- Clear error messages if seed fails
- Seed output shows counts (courses, roadmaps, etc.)
- Documentation explains icon key → icon mapping

## 8. Success Metrics

- **Zero** raw icon key strings visible in UI
- **18** high-quality courses with consistent formatting
- **6** high-quality roadmaps with clear progression
- **100%** of course cards display properly within layout
- **100%** of roadmap cards display properly within layout
- **Zero** content overflow or wrapping issues
- **Zero** crashes related to icon resolution
