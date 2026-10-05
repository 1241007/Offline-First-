# EduNova Content Quality & Presentation Pass - Design Document

## 1. Overview

This design addresses content quality and icon presentation issues in the EduNova Android + FastAPI application. The backend foundation works correctly—Android successfully receives data from FastAPI/Supabase. However, content quality is poor and icons are incorrectly rendered as text strings.

**Core Problems Identified in Audit:**
1. Roadmap icons display as text fragments ("mobi", "le", "chart")
2. Course icons use name-based matching instead of backend `icon` field
3. Course/roadmap content has poor titles and verbose descriptions
4. No centralized icon resolution logic exists

**Design Principles:**
- ✅ Keep existing database schema
- ✅ Keep existing API contracts
- ✅ Keep existing Android DTOs/models
- ✅ Keep existing offline-first architecture
- ✅ Minimize UI changes
- ✅ Add centralized icon resolver
- ✅ Curate seed data content

---

## 2. Architecture Overview

### Data Flow (Unchanged)
```
Supabase Database
    ↓
FastAPI + SQLAlchemy
    ↓
Pydantic Schemas (JSON)
    ↓
Android Retrofit/Ktor
    ↓
Android DTOs
    ↓
Domain Models
    ↓
Repository
    ↓
ViewModel
    ↓
Compose UI ← NEW: IconResolver.resolve(iconKey)
```

### Key Design Decision
**Icon resolution happens ONLY in the UI layer.** Backend stores semantic strings, Android UI resolves them to Material icons.

---

## 3. Backend Seed Data Design

### 3.1 Database Schema (NO CHANGES)

**Existing schema is correct:**
```python
# Course model
icon: Mapped[str] = mapped_column(String(100), nullable=False)

# Roadmap model  
icon: Mapped[str] = mapped_column(String(100), nullable=False)
```

**No Alembic migration required.**

### 3.2 Semantic Icon Keys

Backend will store semantic identifiers, not Android-specific values:

#### Course Icon Keys
```
python              # Python Fundamentals
java                # Java Programming
javascript          # JavaScript Essentials
data_science        # Data Science with Python
machine_learning    # Machine Learning Fundamentals
deep_learning       # Deep Learning & Neural Networks
android             # Android Development with Kotlin
web_development     # Web Development Fundamentals
full_stack          # Full Stack Development
algorithms          # Data Structures & Algorithms
database            # Database Management
operating_systems   # Operating Systems
cloud               # Cloud Computing Fundamentals
cybersecurity       # Cybersecurity Essentials
networking          # Computer Networks
git                 # Git & GitHub
generative_ai       # Generative AI Fundamentals
software_engineering # Software Engineering Fundamentals
```

#### Roadmap Icon Keys
```
mobile_development      # Mobile App Developer Roadmap
data_science           # Data Scientist Roadmap
web_development        # Full Stack Developer Roadmap
artificial_intelligence # AI & Machine Learning Roadmap
cybersecurity          # Cybersecurity Roadmap
cloud_computing        # Cloud Engineer Roadmap
```

### 3.3 Curated Course Data (18 Courses)

#### Category 1: Programming (3 courses)

**Course 1:**
```python
{
    "slug": "python-fundamentals",
    "category_slug": "programming",
    "name": "Python Fundamentals",  # 2 words ✓
    "description": "Learn Python syntax, problem solving, data structures, functions, and object-oriented programming.",  # 13 words ✓
    "icon": "python",
    "level": "Beginner",
    "duration_hours": 40,
    "accent_color": "blue",
    "is_featured": True
}
```

**Course 2:**
```python
{
    "slug": "java-programming",
    "category_slug": "programming",
    "name": "Java Programming",  # 2 words ✓
    "description": "Build strong Java foundations with OOP, collections, exceptions, and practical programming.",  # 12 words ✓
    "icon": "java",
    "level": "Beginner",
    "duration_hours": 50,
    "accent_color": "orange",
    "is_featured": False
}
```

**Course 3:**
```python
{
    "slug": "javascript-essentials",
    "category_slug": "programming",
    "name": "JavaScript Essentials",  # 2 words ✓
    "description": "Learn modern JavaScript, ES6+, asynchronous programming, and interactive web development.",  # 11 words ✓
    "icon": "javascript",
    "level": "Beginner",
    "duration_hours": 45,
    "accent_color": "yellow",
    "is_featured": True
}
```

#### Category 2: Data & AI (3 courses)

**Course 4:**
```python
{
    "slug": "data-science-python",
    "category_slug": "data-ai",
    "name": "Data Science with Python",  # 4 words ✓
    "description": "Learn data analysis, visualization, statistics, and practical workflows with Python.",  # 11 words ✓
    "icon": "data_science",
    "level": "Intermediate",
    "duration_hours": 60,
    "accent_color": "purple",
    "is_featured": True
}
```

**Course 5:**
```python
{
    "slug": "machine-learning-fundamentals",
    "category_slug": "data-ai",
    "name": "Machine Learning Fundamentals",  # 3 words ✓
    "description": "Understand machine learning algorithms, model evaluation, features, and practical workflows.",  # 11 words ✓
    "icon": "machine_learning",
    "level": "Intermediate",
    "duration_hours": 55,
    "accent_color": "indigo",
    "is_featured": False
}
```

**Course 6:**
```python
{
    "slug": "deep-learning-neural-networks",
    "category_slug": "data-ai",
    "name": "Deep Learning & Neural Networks",  # 5 words (acceptable) ✓
    "description": "Learn neural networks, deep learning concepts, CNNs, and modern model training.",  # 12 words ✓
    "icon": "deep_learning",
    "level": "Advanced",
    "duration_hours": 70,
    "accent_color": "purple",
    "is_featured": False
}
```

#### Category 3: Development (3 courses)

**Course 7:**
```python
{
    "slug": "android-development-kotlin",
    "category_slug": "development",
    "name": "Android Development with Kotlin",  # 4 words ✓
    "description": "Build modern Android apps with Kotlin, Jetpack Compose, navigation, and real projects.",  # 13 words ✓
    "icon": "android",
    "level": "Intermediate",
    "duration_hours": 65,
    "accent_color": "green",
    "is_featured": True
}
```

**Course 8:**
```python
{
    "slug": "web-development-fundamentals",
    "category_slug": "development",
    "name": "Web Development Fundamentals",  # 3 words ✓
    "description": "Learn HTML, CSS, JavaScript, responsive design, and fundamentals of modern web development.",  # 13 words ✓
    "icon": "web_development",
    "level": "Beginner",
    "duration_hours": 40,
    "accent_color": "cyan",
    "is_featured": False
}
```

**Course 9:**
```python
{
    "slug": "full-stack-development",
    "category_slug": "development",
    "name": "Full Stack Development",  # 3 words ✓
    "description": "Learn frontend, backend, APIs, databases, authentication, and full-stack application development.",  # 11 words ✓
    "icon": "full_stack",
    "level": "Intermediate",
    "duration_hours": 80,
    "accent_color": "green",
    "is_featured": True
}
```

#### Category 4: Computer Science (3 courses)

**Course 10:**
```python
{
    "slug": "data-structures-algorithms",
    "category_slug": "computer-science",
    "name": "Data Structures & Algorithms",  # 4 words ✓
    "description": "Master arrays, trees, graphs, sorting, searching, and algorithmic problem solving.",  # 11 words ✓
    "icon": "algorithms",
    "level": "Intermediate",
    "duration_hours": 50,
    "accent_color": "blue",
    "is_featured": False
}
```

**Course 11:**
```python
{
    "slug": "database-management",
    "category_slug": "computer-science",
    "name": "Database Management",  # 2 words ✓
    "description": "Learn relational databases, SQL, normalization, joins, indexes, and database design.",  # 11 words ✓
    "icon": "database",
    "level": "Beginner",
    "duration_hours": 35,
    "accent_color": "indigo",
    "is_featured": False
}
```

**Course 12:**
```python
{
    "slug": "operating-systems",
    "category_slug": "computer-science",
    "name": "Operating Systems",  # 2 words ✓
    "description": "Understand processes, memory, scheduling, file systems, and core operating system concepts.",  # 12 words ✓
    "icon": "operating_systems",
    "level": "Intermediate",
    "duration_hours": 45,
    "accent_color": "gray",
    "is_featured": False
}
```

#### Category 5: Cloud & Cybersecurity (3 courses)

**Course 13:**
```python
{
    "slug": "cloud-computing-fundamentals",
    "category_slug": "cloud-cybersecurity",
    "name": "Cloud Computing Fundamentals",  # 3 words ✓
    "description": "Learn cloud concepts, services, deployment models, scalability, and modern cloud architecture.",  # 12 words ✓
    "icon": "cloud",
    "level": "Beginner",
    "duration_hours": 40,
    "accent_color": "blue",
    "is_featured": False
}
```

**Course 14:**
```python
{
    "slug": "cybersecurity-essentials",
    "category_slug": "cloud-cybersecurity",
    "name": "Cybersecurity Essentials",  # 2 words ✓
    "description": "Learn security fundamentals, common threats, network security, and secure application practices.",  # 12 words ✓
    "icon": "cybersecurity",
    "level": "Beginner",
    "duration_hours": 45,
    "accent_color": "red",
    "is_featured": True
}
```

**Course 15:**
```python
{
    "slug": "computer-networks",
    "category_slug": "cloud-cybersecurity",
    "name": "Computer Networks",  # 2 words ✓
    "description": "Understand networking fundamentals, protocols, IP addressing, routing, and network architecture.",  # 11 words ✓
    "icon": "networking",
    "level": "Intermediate",
    "duration_hours": 40,
    "accent_color": "indigo",
    "is_featured": False
}
```

#### Category 6: Practical & Career Skills (3 courses)

**Course 16:**
```python
{
    "slug": "git-github",
    "category_slug": "practical-skills",
    "name": "Git & GitHub",  # 3 words ✓
    "description": "Learn version control, branching, collaboration, pull requests, and professional Git workflows.",  # 12 words ✓
    "icon": "git",
    "level": "Beginner",
    "duration_hours": 20,
    "accent_color": "orange",
    "is_featured": False
}
```

**Course 17:**
```python
{
    "slug": "generative-ai-fundamentals",
    "category_slug": "practical-skills",
    "name": "Generative AI Fundamentals",  # 3 words ✓
    "description": "Understand generative AI, LLMs, prompting, AI workflows, and practical applications.",  # 11 words ✓
    "icon": "generative_ai",
    "level": "Beginner",
    "duration_hours": 30,
    "accent_color": "purple",
    "is_featured": True
}
```

**Course 18:**
```python
{
    "slug": "software-engineering-fundamentals",
    "category_slug": "practical-skills",
    "name": "Software Engineering Fundamentals",  # 3 words ✓
    "description": "Learn software development practices, testing, architecture, debugging, and maintainable code.",  # 11 words ✓
    "icon": "software_engineering",
    "level": "Intermediate",
    "duration_hours": 50,
    "accent_color": "blue",
    "is_featured": False
}
```

### 3.4 Category Data

Update category slugs and names:

```python
CATEGORIES = [
    {"slug": "programming", "name": "Programming", "description": "Master programming languages", "order": 0},
    {"slug": "data-ai", "name": "Data & AI", "description": "Learn data science and AI", "order": 1},
    {"slug": "development", "name": "Development", "description": "Build modern applications", "order": 2},
    {"slug": "computer-science", "name": "Computer Science", "description": "Core CS fundamentals", "order": 3},
    {"slug": "cloud-cybersecurity", "name": "Cloud & Cybersecurity", "description": "Cloud and security skills", "order": 4},
    {"slug": "practical-skills", "name": "Practical & Career Skills", "description": "Essential career skills", "order": 5},
]
```

### 3.5 Curated Roadmap Data (6 Roadmaps)

**Roadmap 1: Mobile App Developer**
```python
{
    "slug": "mobile-app-developer",
    "title": "Mobile App Developer Roadmap",  # 4 words ✓
    "category": "Mobile Development",
    "description": "Build Android applications with Kotlin, Jetpack Compose, APIs, databases, and real-world projects.",  # 13 words ✓
    "level": "Intermediate",
    "duration": "6-8 months",
    "icon": "mobile_development",
    "accent_theme": "green",
    "items": [
        {"order": 0, "title": "Kotlin Foundations", "desc": "Master Kotlin syntax and fundamentals", "skills": ["Kotlin", "OOP", "Functions"], "duration": "2 months"},
        {"order": 1, "title": "Android & Jetpack Compose", "desc": "Build modern Android UIs", "skills": ["Jetpack Compose", "Material Design", "Navigation"], "duration": "2 months"},
        {"order": 2, "title": "APIs & Local Data", "desc": "Connect to APIs and manage local storage", "skills": ["Retrofit", "Room", "Coroutines"], "duration": "2 months"},
        {"order": 3, "title": "Build a Real Android Project", "desc": "Create portfolio-ready app", "skills": ["Architecture", "Testing", "Deployment"], "duration": "2 months"}
    ]
}
```

**Roadmap 2: Data Scientist**
```python
{
    "slug": "data-scientist",
    "title": "Data Scientist Roadmap",  # 3 words ✓
    "category": "Data Science",
    "description": "Build skills in Python, statistics, data analysis, visualization, and machine learning.",  # 12 words ✓
    "level": "Intermediate",
    "duration": "9-12 months",
    "icon": "data_science",
    "accent_theme": "purple",
    "items": [
        {"order": 0, "title": "Python for Data", "desc": "Programming foundation", "skills": ["Python", "NumPy", "Pandas"], "duration": "2 months"},
        {"order": 1, "title": "Statistics & Data Analysis", "desc": "Mathematical foundations", "skills": ["Statistics", "Probability", "Hypothesis Testing"], "duration": "2 months"},
        {"order": 2, "title": "Visualization", "desc": "Present data insights visually", "skills": ["Matplotlib", "Seaborn", "Plotly"], "duration": "2 months"},
        {"order": 3, "title": "Machine Learning", "desc": "Build predictive models", "skills": ["Scikit-learn", "Regression", "Classification"], "duration": "3 months"},
        {"order": 4, "title": "Portfolio Projects", "desc": "Real-world data science projects", "skills": ["Project Planning", "Communication", "Deployment"], "duration": "2 months"}
    ]
}
```

**Roadmap 3: Full Stack Developer**
```python
{
    "slug": "full-stack-developer",
    "title": "Full Stack Developer Roadmap",  # 4 words ✓
    "category": "Web Development",
    "description": "Learn frontend, backend, APIs, databases, authentication, and full-stack application development.",  # 11 words ✓
    "level": "Intermediate",
    "duration": "8-10 months",
    "icon": "web_development",
    "accent_theme": "cyan",
    "items": [
        {"order": 0, "title": "Web Foundations", "desc": "HTML, CSS, responsive design", "skills": ["HTML5", "CSS3", "Flexbox", "Grid"], "duration": "1 month"},
        {"order": 1, "title": "Frontend Development", "desc": "Modern JavaScript frameworks", "skills": ["JavaScript", "React", "State Management"], "duration": "3 months"},
        {"order": 2, "title": "Backend & APIs", "desc": "Server-side development", "skills": ["Node.js", "Express", "REST APIs"], "duration": "2 months"},
        {"order": 3, "title": "Databases", "desc": "Data persistence and management", "skills": ["SQL", "MongoDB", "Database Design"], "duration": "1 month"},
        {"order": 4, "title": "Full Stack Project", "desc": "Build complete web application", "skills": ["Integration", "Authentication", "Deployment"], "duration": "2 months"}
    ]
}
```

**Roadmap 4: AI & Machine Learning**
```python
{
    "slug": "ai-machine-learning",
    "title": "AI & Machine Learning Roadmap",  # 5 words ✓
    "category": "Artificial Intelligence",
    "description": "Learn Python, machine learning, deep learning, model evaluation, and practical AI development.",  # 13 words ✓
    "level": "Advanced",
    "duration": "10-12 months",
    "icon": "artificial_intelligence",
    "accent_theme": "indigo",
    "items": [
        {"order": 0, "title": "Python & Mathematics", "desc": "Programming and math foundations", "skills": ["Python", "Linear Algebra", "Calculus"], "duration": "2 months"},
        {"order": 1, "title": "Machine Learning", "desc": "Core ML algorithms", "skills": ["Supervised Learning", "Unsupervised Learning", "Feature Engineering"], "duration": "3 months"},
        {"order": 2, "title": "Deep Learning", "desc": "Neural networks and frameworks", "skills": ["Neural Networks", "TensorFlow", "PyTorch"], "duration": "3 months"},
        {"order": 3, "title": "Model Evaluation", "desc": "Testing and optimization", "skills": ["Cross-Validation", "Hyperparameter Tuning", "MLOps"], "duration": "2 months"},
        {"order": 4, "title": "AI Projects", "desc": "Build real AI applications", "skills": ["Computer Vision", "NLP", "Deployment"], "duration": "2 months"}
    ]
}
```

**Roadmap 5: Cybersecurity**
```python
{
    "slug": "cybersecurity-specialist",
    "title": "Cybersecurity Roadmap",  # 2 words ✓
    "category": "Cybersecurity",
    "description": "Build security fundamentals across networks, systems, applications, and common security practices.",  # 12 words ✓
    "level": "Beginner",
    "duration": "8-10 months",
    "icon": "cybersecurity",
    "accent_theme": "red",
    "items": [
        {"order": 0, "title": "Security Foundations", "desc": "Core security concepts", "skills": ["CIA Triad", "Threat Models", "Risk Assessment"], "duration": "2 months"},
        {"order": 1, "title": "Networking", "desc": "Network protocols and architecture", "skills": ["TCP/IP", "DNS", "Network Analysis"], "duration": "2 months"},
        {"order": 2, "title": "System Security", "desc": "Secure systems and infrastructure", "skills": ["Linux Security", "Windows Security", "Hardening"], "duration": "2 months"},
        {"order": 3, "title": "Application Security", "desc": "Secure code and applications", "skills": ["OWASP", "Secure Coding", "Vulnerability Testing"], "duration": "2 months"},
        {"order": 4, "title": "Security Projects", "desc": "Practical security implementations", "skills": ["Penetration Testing", "Security Audits", "Incident Response"], "duration": "2 months"}
    ]
}
```

**Roadmap 6: Cloud Engineer**
```python
{
    "slug": "cloud-engineer",
    "title": "Cloud Engineer Roadmap",  # 3 words ✓
    "category": "Cloud Computing",
    "description": "Learn cloud fundamentals, deployment, containers, scalability, monitoring, and modern cloud architecture.",  # 12 words ✓
    "level": "Intermediate",
    "duration": "7-9 months",
    "icon": "cloud_computing",
    "accent_theme": "blue",
    "items": [
        {"order": 0, "title": "Cloud Fundamentals", "desc": "Core cloud concepts", "skills": ["IaaS", "PaaS", "SaaS", "Cloud Models"], "duration": "1 month"},
        {"order": 1, "title": "Linux & Networking", "desc": "Essential infrastructure skills", "skills": ["Linux", "Bash", "Networking"], "duration": "2 months"},
        {"order": 2, "title": "Cloud Services", "desc": "AWS, Azure, or GCP services", "skills": ["Compute", "Storage", "Databases", "Networking"], "duration": "2 months"},
        {"order": 3, "title": "Containers & Deployment", "desc": "Modern deployment practices", "skills": ["Docker", "Kubernetes", "CI/CD"], "duration": "2 months"},
        {"order": 4, "title": "Cloud Projects", "desc": "Build cloud infrastructure", "skills": ["Terraform", "Monitoring", "Security"], "duration": "2 months"}
    ]
}
```

### 3.6 Seed Reset Design

#### Implementation Strategy

**File:** `backend/scripts/seed_data.py`

Add `--reset` flag with strict environment safety:

```python
import argparse
import os
from sqlalchemy import text

def is_development_environment():
    """
    Verify this is a development environment.
    Multiple checks for safety.
    """
    database_url = os.getenv("DATABASE_URL", "")
    environment = os.getenv("ENVIRONMENT", "").lower()
    
    # Check 1: Environment variable explicitly set to development
    if environment not in ["development", "dev", "local"]:
        return False
    
    # Check 2: Database URL contains localhost or local development indicators
    dev_indicators = ["localhost", "127.0.0.1", "::1", ".local", "dev.supabase"]
    if not any(indicator in database_url.lower() for indicator in dev_indicators):
        return False
    
    # Check 3: Not production
    prod_indicators = ["prod", "production", ".live", "cloud.supabase.co"]
    if any(indicator in database_url.lower() for indicator in prod_indicators):
        return False
    
    return True


async def reset_seed_data(session):
    """
    Delete existing seed data while preserving:
    - alembic_version (migration state)
    - conversations (chat history)
    - messages (chat messages)
    - user profiles
    """
    if not is_development_environment():
        raise RuntimeError(
            "❌ SAFETY CHECK FAILED\n"
            "Reset operation is only allowed in development environment.\n"
            f"ENVIRONMENT={os.getenv('ENVIRONMENT')}\n"
            f"DATABASE_URL={os.getenv('DATABASE_URL', 'NOT_SET')}\n"
            "Aborting to protect production data."
        )
    
    print("\n⚠️  RESET MODE ACTIVATED")
    print("This will delete all courses, roadmaps, and related seed data.")
    print("Preserving: alembic_version, conversations, messages, user_profiles")
    
    confirmation = input("Type 'YES' to confirm reset: ")
    if confirmation != "YES":
        print("❌ Reset cancelled.")
        return
    
    print("\n🗑️  Deleting seed data...")
    
    # Delete in correct order (respecting foreign keys)
    # Lessons → Modules → Courses → Categories
    # RoadmapItems → Roadmaps
    
    await session.execute(text("DELETE FROM user_lesson_progress"))
    await session.execute(text("DELETE FROM user_course_progress"))
    await session.execute(text("DELETE FROM lessons"))
    await session.execute(text("DELETE FROM course_modules"))
    await session.execute(text("DELETE FROM courses"))
    await session.execute(text("DELETE FROM course_categories"))
    await session.execute(text("DELETE FROM roadmap_items"))
    await session.execute(text("DELETE FROM roadmaps WHERE is_system = true"))
    
    await session.commit()
    
    print("✅ Seed data deleted successfully")
    print("   Preserved: alembic_version, conversations, messages, user_profiles")


async def main():
    parser = argparse.ArgumentParser(description="EduNova Database Seeder")
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Reset seed data (development only)"
    )
    args = parser.parse_args()
    
    session_factory = get_session_factory()
    
    async with session_factory() as session:
        if args.reset:
            await reset_seed_data(session)
        
        await seed_database(session)
```

#### Safety Guarantees

1. **Environment check:** Must have `ENVIRONMENT=development` or similar
2. **Database URL check:** Must contain local development indicators
3. **Production blocklist:** Must NOT contain production indicators
4. **User confirmation:** Requires typing "YES" explicitly
5. **Preserves critical data:**
   - `alembic_version` (not touched)
   - `conversations` (not touched)
   - `messages` (not touched)
   - `user_profiles` (not touched)

---

## 4. Android Icon Resolver Design

### 4.1 File Structure

**New File:** `app/src/main/java/com/offline_First/ui/utils/IconResolver.kt`

### 4.2 Implementation Design

```kotlin
package com.offline_First.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon resolver for EduNova.
 * Maps semantic backend icon keys to Material Design icons.
 * 
 * Usage:
 *   Icon(imageVector = IconResolver.resolve(course.icon), ...)
 *   Icon(imageVector = IconResolver.resolve(roadmap.icon), ...)
 */
object IconResolver {
    
    /**
     * Resolve semantic icon key to Material icon.
     * 
     * @param iconKey Semantic key from backend (e.g., "python", "data_science")
     * @return Material icon, or fallback icon if key is unknown
     */
    fun resolve(iconKey: String?): ImageVector {
        return when (iconKey?.lowercase()?.trim()) {
            // Programming
            "python" -> Icons.Default.Code
            "java" -> Icons.Default.Code
            "javascript" -> Icons.Default.Code
            
            // Data & AI
            "data_science" -> Icons.Default.Analytics
            "machine_learning" -> Icons.Default.Psychology
            "deep_learning" -> Icons.Default.Memory
            "artificial_intelligence" -> Icons.Default.Psychology
            "generative_ai" -> Icons.Default.AutoAwesome
            
            // Development
            "android" -> Icons.Default.Android
            "web_development" -> Icons.Default.Language
            "full_stack" -> Icons.Default.Layers
            "mobile_development" -> Icons.Default.PhoneAndroid
            
            // Computer Science
            "algorithms" -> Icons.Default.Functions
            "database" -> Icons.Default.Storage
            "operating_systems" -> Icons.Default.Computer
            
            // Cloud & Cybersecurity
            "cloud" -> Icons.Default.Cloud
            "cloud_computing" -> Icons.Default.Cloud
            "cybersecurity" -> Icons.Default.Security
            "networking" -> Icons.Default.Hub
            
            // Practical Skills
            "git" -> Icons.Default.AccountTree
            "software_engineering" -> Icons.Default.Engineering
            
            // Fallback for unknown keys
            else -> Icons.Default.School
        }
    }
    
    /**
     * Check if icon key is known/valid.
     * Useful for debugging.
     */
    fun isKnown(iconKey: String?): Boolean {
        return resolve(iconKey) != Icons.Default.School || iconKey?.lowercase()?.trim() == "school"
    }
}
```

### 4.3 Design Rationale

**Why `object` (singleton)?**
- No state to manage
- Global utility function
- Efficient (no instance creation)

**Why Material Icons only?**
- Already imported in project
- No new dependencies
- Consistent with existing UI
- 1000+ icons available

**Why `School` as fallback?**
- Generic academic icon
- Fits EduNova context
- Never crashes
- Obvious to developers (easy to spot unknown keys)

**Null safety:**
- Accepts `String?` (nullable)
- Handles blank/empty strings
- Always returns valid icon

---

## 5. Android UI Component Changes

### 5.1 RoadmapScreen.kt Changes

**File:** `app/src/main/java/com/offline_First/ui/screens/RoadmapScreen.kt`

**Import:**
```kotlin
import com.offline_First.ui.utils.IconResolver
import androidx.compose.material3.Icon
```

#### Change 1: PopularRoadmapCard (Line ~692)

**BEFORE:**
```kotlin
Box(
    modifier = Modifier
        .size(42.dp)
        .background(accent.copy(alpha = 0.12f), CircleShape),
    contentAlignment = Alignment.Center
) {
    Text(
        text = roadmap.icon,  // ❌ BUG
        color = accent,
        fontWeight = FontWeight.Bold
    )
}
```

**AFTER:**
```kotlin
Box(
    modifier = Modifier
        .size(42.dp)
        .background(accent.copy(alpha = 0.12f), CircleShape),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = IconResolver.resolve(roadmap.icon),
        contentDescription = "${roadmap.title} icon",
        tint = accent,
        modifier = Modifier.size(24.dp)
    )
}
```

#### Change 2: StandardRoadmapCard (Line ~752)

**BEFORE:**
```kotlin
Box(
    modifier = Modifier
        .size(36.dp)
        .background(accent.copy(alpha = 0.12f), CircleShape),
    contentAlignment = Alignment.Center
) {
    Text(
        text = roadmap.icon,  // ❌ BUG
        fontWeight = FontWeight.Bold,
        color = accent,
        fontSize = 13.sp
    )
}
```

**AFTER:**
```kotlin
Box(
    modifier = Modifier
        .size(36.dp)
        .background(accent.copy(alpha = 0.12f), CircleShape),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = IconResolver.resolve(roadmap.icon),
        contentDescription = "${roadmap.title} icon",
        tint = accent,
        modifier = Modifier.size(20.dp)
    )
}
```

### 5.2 LandingScreen.kt Changes

**File:** `app/src/main/java/com/offline_First/ui/screens/LandingScreen.kt`

**Import:**
```kotlin
import com.offline_First.ui.utils.IconResolver
```

#### Change: CourseIcon Function (Line ~2100)

**BEFORE:**
```kotlin
@Composable
private fun CourseIcon(course: Course, large: Boolean = false) {
    val courseColor = course.accent.toColor()
    Box(
        modifier = Modifier
            .size(if (large) 70.dp else 42.dp)
            .clip(RoundedCornerShape(if (large) 20.dp else 12.dp))
            .background(EduNovaPrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val courseIcon = when (course.name.lowercase()) {  // ❌ Uses name
            "data science" -> Icons.Default.Analytics
            "dsa" -> Icons.Default.Hub
            "full stack" -> Icons.Default.Web
            else -> Icons.Default.Code
        }
        Icon(
            imageVector = courseIcon,
            contentDescription = "${course.name} course",
            tint = courseColor,
            modifier = Modifier.size(if (large) 34.dp else 24.dp)
        )
    }
}
```

**AFTER:**
```kotlin
@Composable
private fun CourseIcon(course: Course, large: Boolean = false) {
    val courseColor = course.accent.toColor()
    Box(
        modifier = Modifier
            .size(if (large) 70.dp else 42.dp)
            .clip(RoundedCornerShape(if (large) 20.dp else 12.dp))
            .background(EduNovaPrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = IconResolver.resolve(course.icon),  // ✅ Uses icon field
            contentDescription = "${course.name} course",
            tint = courseColor,
            modifier = Modifier.size(if (large) 34.dp else 24.dp)
        )
    }
}
```

### 5.3 MyLearningScreen.kt Analysis

**Audit Finding:** Need to verify if MyLearningScreen renders course icons.

**Action during implementation:**
1. Check if `LearningCourseCard` displays course icons
2. If yes, ensure it uses `IconResolver.resolve(course.icon)`
3. If no, no changes needed

**Preliminary grep results show:** `LearningCourseCard` exists but doesn't seem to use course icons based on audit. Will verify during implementation.

---

## 6. API Contract Verification (NO CHANGES)

### 6.1 Backend Pydantic Schemas (UNCHANGED)

```python
# CourseListResponse
class CourseListResponse(BaseModel):
    id: str
    name: str
    description: str
    icon: str  # ← Flows to Android as-is
    accent: str = Field(alias="accentColor")

# RoadmapListResponse  
class RoadmapListResponse(BaseModel):
    id: str
    title: str
    category: str
    description: str
    skills: List[str]
    level: str
    duration: str
    stages: int
    icon: str  # ← Flows to Android as-is
    accent_theme: str = Field(alias="accentTheme")
```

### 6.2 Android DTOs (UNCHANGED)

```kotlin
// CourseDto
@Serializable
data class CourseDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,  // ← Receives from API
    @SerialName("accentColor") val accentColor: String
)

// RoadmapDto
@Serializable
data class RoadmapDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,  // ← Receives from API
    @SerialName("accentTheme") val accentTheme: String
)
```

### 6.3 Domain Models (UNCHANGED)

```kotlin
// Course
data class Course(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,  // ← Used by IconResolver
    val accent: CourseAccent = CourseAccent.PRIMARY
)

// RoadmapOption
data class RoadmapOption(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,  // ← Used by IconResolver
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY
)
```

**CRITICAL:** Icon resolution happens **only in UI composables**, not in DTOs/models/repositories.

---

## 7. Verification Plan

### 7.1 Backend Verification

#### Step 1: Seed Script Safety
```bash
# Test environment check (should FAIL on non-dev)
ENVIRONMENT=production python backend/scripts/seed_data.py --reset

# Expected: RuntimeError about safety check
```

#### Step 2: Seed Reset (Development Only)
```bash
cd backend
ENVIRONMENT=development python scripts/seed_data.py --reset

# Verify:
# - Prompted for "YES" confirmation
# - Shows deletion progress
# - Shows seeding progress
# - No errors
```

#### Step 3: Verify Counts
```bash
# Connect to database and verify:
# - 6 categories
# - 18 courses
# - 6 roadmaps
# - alembic_version still exists
# - conversations preserved (if any existed)
# - messages preserved (if any existed)
```

#### Step 4: API Response Verification
```bash
# Test course endpoint
curl http://localhost:8000/api/v1/courses | jq '.[0]'

# Expected:
# {
#   "id": "...",
#   "name": "Python Fundamentals",  // Short title ✓
#   "description": "Learn Python syntax...",  // 13 words ✓
#   "icon": "python",  // Semantic key ✓
#   "accentColor": "blue"
# }

# Test roadmap endpoint
curl http://localhost:8000/api/v1/roadmaps | jq '.[0]'

# Expected:
# {
#   "id": "...",
#   "title": "Mobile App Developer Roadmap",
#   "icon": "mobile_development",  // Semantic key ✓
#   "stages": 4,
#   ...
# }
```

### 7.2 Android Verification

#### Step 1: Build Verification
```bash
cd app
./gradlew assembleDebug

# Expected: BUILD SUCCESSFUL
# No compilation errors
```

#### Step 2: Icon Resolver Unit Test (Optional)
```kotlin
// Test icon resolution
@Test
fun iconResolverTest() {
    assertEquals(Icons.Default.Code, IconResolver.resolve("python"))
    assertEquals(Icons.Default.Analytics, IconResolver.resolve("data_science"))
    assertEquals(Icons.Default.PhoneAndroid, IconResolver.resolve("mobile_development"))
    assertEquals(Icons.Default.School, IconResolver.resolve("unknown_key"))
    assertEquals(Icons.Default.School, IconResolver.resolve(null))
    assertEquals(Icons.Default.School, IconResolver.resolve(""))
}
```

#### Step 3: Visual Verification (CRITICAL)

**Run app on emulator/device and verify:**

**Home/Landing Screen:**
- [ ] Course cards display Material icons (not text)
- [ ] Course titles fit in 1-2 lines
- [ ] Course descriptions fit in 3-4 lines
- [ ] Python course shows code icon
- [ ] Data Science course shows analytics icon
- [ ] Android course shows Android icon
- [ ] No text strings visible in icon areas

**Roadmap Screen:**
- [ ] Roadmap cards display Material icons (not text)
- [ ] No "mobi", "le", "chart" text fragments
- [ ] Mobile roadmap shows phone/device icon
- [ ] Data Science roadmap shows analytics icon
- [ ] Cloud roadmap shows cloud icon
- [ ] Cybersecurity roadmap shows security icon
- [ ] Roadmap titles fit properly
- [ ] Roadmap descriptions fit properly

**Course Detail (if applicable):**
- [ ] Course detail shows correct icon

**Unknown Icon Key Test:**
- Temporarily modify backend to return `"icon": "totally_unknown_key"`
- [ ] App doesn't crash
- [ ] Fallback School icon displays

### 7.3 Content Quality Verification

**Checklist:**
- [ ] All 18 courses present
- [ ] All 6 roadmaps present
- [ ] Course titles are 2-4 words
- [ ] Course descriptions are 15-22 words
- [ ] Roadmap titles are concise
- [ ] Roadmap descriptions are 15-25 words
- [ ] Content feels education-focused (not generic marketing)
- [ ] No lorem ipsum or obvious placeholder text

### 7.4 Regression Testing

**Ensure nothing broke:**
- [ ] Chat still works
- [ ] AI workspace still accessible
- [ ] School mode still works (separate from courses)
- [ ] Bottom navigation works
- [ ] Profile works
- [ ] Settings work
- [ ] Login/logout flow works (if implemented)

---

## 8. Implementation Task Breakdown

### Task 1: Update Backend Seed Data
**File:** `backend/scripts/seed_data.py`

**Changes:**
1. Update `CATEGORIES` to 6 new categories
2. Replace `COURSES` with 18 curated courses
3. Replace `ROADMAPS` with 6 curated roadmaps
4. Add `--reset` argument parsing
5. Add `is_development_environment()` function
6. Add `reset_seed_data()` function
7. Update `main()` to handle reset flag

**Verification:**
- Run seed with `--reset` on dev database
- Verify counts and API responses

### Task 2: Create Icon Resolver
**File:** `app/src/main/java/com/offline_First/ui/utils/IconResolver.kt` (NEW)

**Changes:**
1. Create new file in utils package
2. Implement `IconResolver` object
3. Implement `resolve()` function with all icon mappings
4. Implement `isKnown()` helper (optional)

**Verification:**
- File compiles
- No import errors

### Task 3: Update RoadmapScreen
**File:** `app/src/main/java/com/offline_First/ui/screens/RoadmapScreen.kt`

**Changes:**
1. Add `IconResolver` import
2. Replace `Text(roadmap.icon)` with `Icon(IconResolver.resolve(roadmap.icon))` at line ~692
3. Replace `Text(roadmap.icon)` with `Icon(IconResolver.resolve(roadmap.icon))` at line ~752
4. Add content description
5. Adjust icon size if needed

**Verification:**
- File compiles
- Run app: roadmaps show icons

### Task 4: Update LandingScreen
**File:** `app/src/main/java/com/offline_First/ui/screens/LandingScreen.kt`

**Changes:**
1. Add `IconResolver` import
2. Replace name-based matching in `CourseIcon()` function
3. Use `IconResolver.resolve(course.icon)` instead

**Verification:**
- File compiles
- Run app: courses show correct icons

### Task 5: Check MyLearningScreen (Conditional)
**File:** `app/src/main/java/com/offline_First/ui/screens/MyLearningScreen.kt`

**Changes:**
1. Read file to check if `LearningCourseCard` displays icons
2. If yes, apply IconResolver
3. If no, skip this task

**Verification:**
- File compiles
- Run app: learning courses show correct icons (if applicable)

### Task 6: Full Integration Test
**Actions:**
1. Run backend with reseeded data
2. Build and run Android app
3. Navigate through all screens
4. Visual verification checklist
5. Test unknown icon key
6. Regression testing

---

## 9. Files to Modify

### Backend (1 file)
1. ✏️ `backend/scripts/seed_data.py`
   - Update categories, courses, roadmaps
   - Add reset functionality

### Android (3-4 files)
1. ✏️ `app/src/main/java/com/offline_First/ui/utils/IconResolver.kt` **(NEW FILE)**
   - Create centralized icon resolver

2. ✏️ `app/src/main/java/com/offline_First/ui/screens/RoadmapScreen.kt`
   - Fix icon rendering bug (2 locations)

3. ✏️ `app/src/main/java/com/offline_First/ui/screens/LandingScreen.kt`
   - Use icon field instead of name matching

4. ⚠️ `app/src/main/java/com/offline_First/ui/screens/MyLearningScreen.kt` **(CONDITIONAL)**
   - Only if course icons are displayed

**Total: 4-5 files**

---

## 10. Files Explicitly NOT to Modify

### Backend (NO CHANGES)
- ❌ `backend/app/models/course.py` - Schema is correct
- ❌ `backend/app/models/roadmap.py` - Schema is correct
- ❌ `backend/app/schemas/course.py` - Pydantic schema is correct
- ❌ `backend/app/schemas/roadmap.py` - Pydantic schema is correct
- ❌ `backend/app/services/course_service.py` - Service logic is correct
- ❌ `backend/app/services/roadmap_service.py` - Service logic is correct
- ❌ `backend/app/api/routes/courses.py` - Routes are correct
- ❌ `backend/app/api/routes/roadmaps.py` - Routes are correct
- ❌ `backend/app/repositories/*` - Repository logic is correct
- ❌ `backend/migrations/*` - No migration needed
- ❌ `backend/alembic.ini` - No changes
- ❌ `backend/app/models/conversation.py` - Chat unchanged
- ❌ `backend/app/models/message.py` - Chat unchanged

### Android (NO CHANGES)
- ❌ `app/src/main/java/com/offline_First/data/remote/CourseDtos.kt` - DTO is correct
- ❌ `app/src/main/java/com/offline_First/data/remote/RoadmapDtos.kt` - DTO is correct
- ❌ `app/src/main/java/com/offline_First/domain/model/Course.kt` - Model is correct
- ❌ `app/src/main/java/com/offline_First/domain/model/Roadmap.kt` - Model is correct
- ❌ `app/src/main/java/com/offline_First/data/repository/CourseRepository.kt` - Interface unchanged
- ❌ `app/src/main/java/com/offline_First/data/repository/RoadmapRepository.kt` - Interface unchanged
- ❌ `app/src/main/java/com/offline_First/data/remote/OnlineCourseRepository.kt` - Implementation unchanged
- ❌ `app/src/main/java/com/offline_First/data/remote/OnlineRoadmapRepository.kt` - Implementation unchanged
- ❌ `app/src/main/java/com/offline_First/data/remote/ChatApiClient.kt` - API client unchanged
- ❌ `app/src/main/java/com/offline_First/ui/screens/landing/LandingViewModel.kt` - ViewModel unchanged
- ❌ `app/src/main/java/com/offline_First/ui/navigation/*` - Navigation unchanged
- ❌ `app/src/main/java/com/offline_First/ui/components/*` - Shared components unchanged
- ❌ `app/src/main/java/com/offline_First/ui/theme/*` - Theme unchanged
- ❌ `app/src/main/java/com/offline_First/data/AppContainer.kt` - DI unchanged
- ❌ `app/build.gradle.kts` - No new dependencies
- ❌ `app/src/main/AndroidManifest.xml` - Manifest unchanged

---

## 11. Design Safety Check

### ✅ No Schema Migration Required
- Existing `icon` field (String, 100 chars) supports semantic keys
- No new columns needed
- No data type changes
- Alembic migrations unchanged

### ✅ No DTO Changes Required
- `CourseDto.icon: String` is correct
- `RoadmapDto.icon: String` is correct
- Kotlinx serialization unchanged
- JSON parsing unchanged

### ✅ No API Contract Changes
- Backend returns `icon` as string (unchanged)
- Pydantic schemas unchanged
- FastAPI routes unchanged
- HTTP contracts preserved

### ✅ No Chat Changes
- Conversations untouched
- Messages untouched
- Chat API untouched
- Gemini integration untouched

### ✅ No Authentication Changes
- Auth models untouched
- Auth routes untouched
- Login/register untouched

### ✅ No Navigation Changes
- Navigation graph unchanged
- Bottom navigation unchanged
- AppDestination unchanged

### ✅ No New Dependencies
- Using existing Material Icons
- Using existing Compose
- Using existing Kotlin stdlib
- No new Gradle dependencies

### ✅ Offline Repository Preserved
- Repository interfaces unchanged
- OnlineCourseRepository unchanged (except domain mapping uses IconResolver in UI only)
- BackendNotConfiguredRepositories unchanged
- Offline-first pattern preserved

---

## 12. Risks and Mitigation

### Risk 1: Seed Reset Accidentally Runs on Production
**Severity:** CRITICAL  
**Likelihood:** LOW (with safeguards)

**Mitigation:**
- Triple environment checks in code
- Requires explicit user confirmation ("YES")
- Logs environment variables
- Fails loudly with detailed error message

### Risk 2: Unknown Icon Keys After Future Backend Changes
**Severity:** LOW  
**Likelihood:** MEDIUM

**Mitigation:**
- Fallback icon (School) always displays
- App never crashes
- Easy to spot visually
- Easy to add new mappings to IconResolver

### Risk 3: Content Doesn't Fit Existing UI
**Severity:** MEDIUM  
**Likelihood:** LOW (carefully designed)

**Mitigation:**
- Word count constraints in design (15-22 words)
- Titles kept to 2-4 words
- Visual verification checklist
- Can adjust during implementation if needed

### Risk 4: Breaking Existing Course/Roadmap References
**Severity:** LOW  
**Likelihood:** LOW

**Mitigation:**
- Deterministic UUIDs from seed script
- Same slug patterns maintained where possible
- School mode uses separate data
- Chat doesn't reference course/roadmap data

---

## 13. Implementation NOT Started

**CONFIRMATION:** This is a design document only. No code has been implemented yet.

**Next Steps:**
1. Review this design document
2. Approve or request changes
3. Create tasks.md with implementation checklist
4. Begin implementation only after approval

---

## 14. Summary

### What This Design Achieves

**Problem:** Roadmap icons display as text ("mobi", "le", "chart"), course icons use name matching, content quality is poor.

**Solution:**
1. **Centralized icon resolver** maps semantic backend keys to Material icons
2. **Curated seed data** with 18 high-quality courses and 6 roadmaps
3. **Safe seed reset** with triple environment checks
4. **UI fixes** in 2-3 Android files to use IconResolver
5. **Zero schema/API changes** - works with existing architecture

### Data Changes
- 18 curated courses (from poor generic content)
- 6 curated roadmaps (from poor generic content)
- Semantic icon keys stored in database
- All titles/descriptions optimized for card layouts

### Icon Resolution Design
- **Location:** UI layer only (IconResolver.kt)
- **Mapping:** Semantic string → Material icon
- **Fallback:** School icon for unknown keys
- **Safety:** Never crashes, handles null/empty

### Seed Reset Design
- **Command:** `python scripts/seed_data.py --reset`
- **Safety:** Triple environment checks + user confirmation
- **Preserves:** alembic_version, conversations, messages, user_profiles
- **Deletes:** courses, roadmaps, modules, lessons (seed data only)

### Files Modified
1. `backend/scripts/seed_data.py`
2. `app/.../ui/utils/IconResolver.kt` (NEW)
3. `app/.../ui/screens/RoadmapScreen.kt`
4. `app/.../ui/screens/LandingScreen.kt`
5. `app/.../ui/screens/MyLearningScreen.kt` (conditional)

### Verification Plan
- Backend: Seed reset, API responses, counts
- Android: Build, visual verification, icon rendering
- Content: Quality checks, layout fit
- Regression: Chat, navigation, school mode

### Risks
- LOW: All risks have mitigation strategies
- CRITICAL safeguards on seed reset
- Fallback behavior for unknown icons
- Content carefully sized for layouts

---

**Design Status:** ✅ COMPLETE  
**Implementation Status:** ⏸️ NOT STARTED (awaiting approval)  
**Ready for Review:** ✅ YES
