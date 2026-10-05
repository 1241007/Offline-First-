#!/usr/bin/env python3
"""
EduNova Database Seed Script
Generates realistic, deterministic seed data for development and testing.
Idempotent: Can be run multiple times safely.
"""

import sys
from pathlib import Path

# Add backend directory to Python path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

import argparse
import asyncio
import os
import uuid
from datetime import date, datetime, timezone
from sqlalchemy import select, text
from app.core.database import get_session_factory
from app.core.config import settings
from app.models import (
    CourseCategory, Course, CourseModule, Lesson,
    Roadmap, RoadmapItem
)

# Deterministic UUID generation using namespace
SEED_NAMESPACE = uuid.UUID('12345678-1234-5678-1234-567812345678')


def seed_id(entity_type: str, slug: str) -> str:
    """Generate deterministic UUID for seed data"""
    return str(uuid.uuid5(SEED_NAMESPACE, f"{entity_type}:{slug}"))


def is_development_environment():
    """
    Verify this is a development environment using ONLY the ENVIRONMENT variable.

    Safety rule:
        ALLOW reset only if ENVIRONMENT is explicitly one of: development, dev, local
        DENY if ENVIRONMENT is missing, blank, or any non-development value.

    We do NOT inspect DATABASE_URL to determine safety.
    A development Supabase database legitimately has a remote URL;
    a localhost URL does not automatically mean it is safe to wipe.
    """
    # Use the settings object which loads from .env via pydantic-settings
    environment = settings.environment.strip().lower()

    # Must be an explicit known development value — fail closed for everything else
    return environment in {"development", "dev", "local"}


async def reset_seed_data(session):
    """
    Delete existing seed data while preserving:
    - alembic_version  (migration state — never touched)
    - conversations    (chat history)
    - messages         (chat messages)
    - user_profiles

    Safety rule: ENVIRONMENT must be explicitly 'development', 'dev', or 'local'.
    DATABASE_URL is never inspected or printed.
    """
    # Use the settings object (loaded from .env) — never inspect DATABASE_URL
    environment = settings.environment.strip().lower()

    if not is_development_environment():
        raise RuntimeError(
            "[SAFETY] Reset blocked.\n"
            f"  ENVIRONMENT is set to: '{environment or '(not set)'}'\n"
            "  Reset is only allowed when ENVIRONMENT is one of: development, dev, local\n"
            "  Set ENVIRONMENT=development in your .env file to use --reset.\n"
            "  DATABASE_URL has not been inspected or printed."
        )

    print("[RESET] Reset mode activated.")
    print("[RESET] Tables that will be cleared:")
    print("         user_lesson_progress, user_course_progress")
    print("         lessons, course_modules, courses, course_categories")
    print("         roadmap_items, roadmaps (is_system=true only)")
    print("[RESET] Tables that will be PRESERVED:")
    print("         alembic_version, conversations, messages, user_profiles")
    print(f"[RESET] ENVIRONMENT = {environment}")
    print()

    confirmation = input("Type 'YES' to confirm reset: ").strip()
    if confirmation != "YES":
        print("[ERROR] Reset cancelled.")
        return

    print("\n[RESET] Deleting seed data...")

    # Delete in foreign-key-safe order
    await session.execute(text("DELETE FROM user_lesson_progress"))
    await session.execute(text("DELETE FROM user_course_progress"))
    await session.execute(text("DELETE FROM lessons"))
    await session.execute(text("DELETE FROM course_modules"))
    await session.execute(text("DELETE FROM courses"))
    await session.execute(text("DELETE FROM course_categories"))
    await session.execute(text("DELETE FROM roadmap_items"))
    await session.execute(text("DELETE FROM roadmaps WHERE is_system = true"))

    await session.commit()

    print("[RESET] Seed data deleted successfully.")
    print("[RESET] Preserved: alembic_version, conversations, messages, user_profiles")



# Course Categories Data
CATEGORIES = [
    {"slug": "programming", "name": "Programming", "description": "Master programming languages", "order": 0},
    {"slug": "data-ai", "name": "Data & AI", "description": "Learn data science and AI", "order": 1},
    {"slug": "development", "name": "Development", "description": "Build modern applications", "order": 2},
    {"slug": "computer-science", "name": "Computer Science", "description": "Core CS fundamentals", "order": 3},
    {"slug": "cloud-cybersecurity", "name": "Cloud & Cybersecurity", "description": "Cloud and security skills", "order": 4},
    {"slug": "practical-skills", "name": "Practical & Career Skills", "description": "Essential career skills", "order": 5},
]

# Courses Data - 18 curated courses (3 per category)
COURSES = [
    # Programming (3 courses)
    {
        "slug": "python-fundamentals",
        "category_slug": "programming",
        "name": "Python Fundamentals",
        "description": "Python basics and OOP.",
        "icon": "python",
        "level": "Beginner",
        "duration_hours": 40,
        "accent_color": "blue",
        "is_featured": True,
    },
    {
        "slug": "java-programming",
        "category_slug": "programming",
        "name": "Java Programming",
        "description": "Java OOP and collections.",
        "icon": "java",
        "level": "Beginner",
        "duration_hours": 50,
        "accent_color": "orange",
        "is_featured": False,
    },
    {
        "slug": "javascript-essentials",
        "category_slug": "programming",
        "name": "JavaScript Essentials",
        "description": "Modern JS and ES6+.",
        "icon": "javascript",
        "level": "Beginner",
        "duration_hours": 45,
        "accent_color": "yellow",
        "is_featured": True,
    },
    # Data & AI (3 courses)
    {
        "slug": "data-science-python",
        "category_slug": "data-ai",
        "name": "Data Science with Python",
        "description": "Data analysis and ML.",
        "icon": "data_science",
        "level": "Intermediate",
        "duration_hours": 60,
        "accent_color": "purple",
        "is_featured": True,
    },
    {
        "slug": "machine-learning-fundamentals",
        "category_slug": "data-ai",
        "name": "Machine Learning Fundamentals",
        "description": "ML algorithms and models.",
        "icon": "machine_learning",
        "level": "Intermediate",
        "duration_hours": 55,
        "accent_color": "indigo",
        "is_featured": False,
    },
    {
        "slug": "deep-learning-neural-networks",
        "category_slug": "data-ai",
        "name": "Deep Learning & Neural Networks",
        "description": "Neural networks and CNNs.",
        "icon": "deep_learning",
        "level": "Advanced",
        "duration_hours": 70,
        "accent_color": "purple",
        "is_featured": False,
    },
    # Development (3 courses)
    {
        "slug": "android-development-kotlin",
        "category_slug": "development",
        "name": "Android Development with Kotlin",
        "description": "Android apps with Kotlin.",
        "icon": "android",
        "level": "Intermediate",
        "duration_hours": 65,
        "accent_color": "green",
        "is_featured": True,
    },
    {
        "slug": "web-development-fundamentals",
        "category_slug": "development",
        "name": "Web Development Fundamentals",
        "description": "HTML, CSS and JavaScript.",
        "icon": "web_development",
        "level": "Beginner",
        "duration_hours": 40,
        "accent_color": "cyan",
        "is_featured": False,
    },
    {
        "slug": "full-stack-development",
        "category_slug": "development",
        "name": "Full Stack Development",
        "description": "Frontend and backend dev.",
        "icon": "full_stack",
        "level": "Intermediate",
        "duration_hours": 80,
        "accent_color": "green",
        "is_featured": True,
    },
    # Computer Science (3 courses)
    {
        "slug": "data-structures-algorithms",
        "category_slug": "computer-science",
        "name": "Data Structures & Algorithms",
        "description": "Arrays, trees and graphs.",
        "icon": "algorithms",
        "level": "Intermediate",
        "duration_hours": 50,
        "accent_color": "blue",
        "is_featured": False,
    },
    {
        "slug": "database-management",
        "category_slug": "computer-science",
        "name": "Database Management",
        "description": "SQL and database design.",
        "icon": "database",
        "level": "Beginner",
        "duration_hours": 35,
        "accent_color": "indigo",
        "is_featured": False,
    },
    {
        "slug": "operating-systems",
        "category_slug": "computer-science",
        "name": "Operating Systems",
        "description": "Processes, memory and scheduling.",
        "icon": "operating_systems",
        "level": "Intermediate",
        "duration_hours": 45,
        "accent_color": "gray",
        "is_featured": False,
    },
    # Cloud & Cybersecurity (3 courses)
    {
        "slug": "cloud-computing-fundamentals",
        "category_slug": "cloud-cybersecurity",
        "name": "Cloud Computing Fundamentals",
        "description": "Cloud concepts and services.",
        "icon": "cloud",
        "level": "Beginner",
        "duration_hours": 40,
        "accent_color": "blue",
        "is_featured": False,
    },
    {
        "slug": "cybersecurity-essentials",
        "category_slug": "cloud-cybersecurity",
        "name": "Cybersecurity Essentials",
        "description": "Security and threat defence.",
        "icon": "cybersecurity",
        "level": "Beginner",
        "duration_hours": 45,
        "accent_color": "red",
        "is_featured": True,
    },
    {
        "slug": "computer-networks",
        "category_slug": "cloud-cybersecurity",
        "name": "Computer Networks",
        "description": "Protocols, routing and networking.",
        "icon": "networking",
        "level": "Intermediate",
        "duration_hours": 40,
        "accent_color": "indigo",
        "is_featured": False,
    },
    # Practical & Career Skills (3 courses)
    {
        "slug": "git-github",
        "category_slug": "practical-skills",
        "name": "Git & GitHub",
        "description": "Version control and branching.",
        "icon": "git",
        "level": "Beginner",
        "duration_hours": 20,
        "accent_color": "orange",
        "is_featured": False,
    },
    {
        "slug": "generative-ai-fundamentals",
        "category_slug": "practical-skills",
        "name": "Generative AI Fundamentals",
        "description": "LLMs, prompting and AI.",
        "icon": "generative_ai",
        "level": "Beginner",
        "duration_hours": 30,
        "accent_color": "purple",
        "is_featured": True,
    },
    {
        "slug": "software-engineering-fundamentals",
        "category_slug": "practical-skills",
        "name": "Software Engineering Fundamentals",
        "description": "Testing and clean architecture.",
        "icon": "software_engineering",
        "level": "Intermediate",
        "duration_hours": 50,
        "accent_color": "blue",
        "is_featured": False,
    },
]

# Module templates (3 per course)
MODULE_TEMPLATES = [
    {"order": 0, "title": "Introduction & Setup", "desc": "Get started with the fundamentals"},
    {"order": 1, "title": "Core Concepts", "desc": "Master the essential building blocks"},
    {"order": 2, "title": "Advanced Topics & Projects", "desc": "Apply knowledge in real-world scenarios"},
]

# Lesson templates (5 per module)
LESSON_TEMPLATES = [
    {"order": 0, "title_suffix": "Overview", "type": "video", "duration": 15},
    {"order": 1, "title_suffix": "Key Concepts", "type": "article", "duration": 20},
    {"order": 2, "title_suffix": "Practical Examples", "type": "video", "duration": 25},
    {"order": 3, "title_suffix": "Hands-on Exercise", "type": "exercise", "duration": 30},
    {"order": 4, "title_suffix": "Quiz", "type": "quiz", "duration": 10},
]

# Roadmap Data - 6 curated system roadmaps
ROADMAPS = [
    {
        "slug": "mobile-app-developer",
        "title": "Mobile App Developer Roadmap",
        "category": "Mobile Development",
        "description": "Build Android applications with Kotlin, Jetpack Compose, REST APIs, local databases, and real-world portfolio projects.",
        "level": "Intermediate",
        "duration": "6-8 months",
        "icon": "mobile_development",
        "accent_theme": "green",
        "items": [
            {"order": 0, "title": "Kotlin Foundations", "desc": "Master Kotlin syntax and fundamentals", "course_slug": None, "skills": ["Kotlin", "OOP", "Functions"], "duration": "2 months"},
            {"order": 1, "title": "Android & Jetpack Compose", "desc": "Build modern Android UIs", "course_slug": "android-development-kotlin", "skills": ["Jetpack Compose", "Material Design", "Navigation"], "duration": "2 months"},
            {"order": 2, "title": "APIs & Local Data", "desc": "Connect to APIs and manage local storage", "course_slug": None, "skills": ["Retrofit", "Room", "Coroutines"], "duration": "2 months"},
            {"order": 3, "title": "Build a Real Android Project", "desc": "Create portfolio-ready app", "course_slug": None, "skills": ["Architecture", "Testing", "Deployment"], "duration": "2 months"}
        ],
    },
    {
        "slug": "data-scientist",
        "title": "Data Scientist Roadmap",
        "category": "Data Science",
        "description": "Build skills in Python, statistics, data analysis, visualization, machine learning, and real-world portfolio projects.",
        "level": "Intermediate",
        "duration": "9-12 months",
        "icon": "data_science",
        "accent_theme": "purple",
        "items": [
            {"order": 0, "title": "Python for Data", "desc": "Programming foundation", "course_slug": "python-fundamentals", "skills": ["Python", "NumPy", "Pandas"], "duration": "2 months"},
            {"order": 1, "title": "Statistics & Data Analysis", "desc": "Mathematical foundations", "course_slug": None, "skills": ["Statistics", "Probability", "Hypothesis Testing"], "duration": "2 months"},
            {"order": 2, "title": "Visualization", "desc": "Present data insights visually", "course_slug": "data-science-python", "skills": ["Matplotlib", "Seaborn", "Plotly"], "duration": "2 months"},
            {"order": 3, "title": "Machine Learning", "desc": "Build predictive models", "course_slug": "machine-learning-fundamentals", "skills": ["Scikit-learn", "Regression", "Classification"], "duration": "3 months"},
            {"order": 4, "title": "Portfolio Projects", "desc": "Real-world data science projects", "course_slug": None, "skills": ["Project Planning", "Communication", "Deployment"], "duration": "2 months"}
        ],
    },
    {
        "slug": "full-stack-developer",
        "title": "Full Stack Developer Roadmap",
        "category": "Web Development",
        "description": "Learn frontend, backend, REST APIs, databases, authentication, and full-stack application development from end to end.",
        "level": "Intermediate",
        "duration": "8-10 months",
        "icon": "web_development",
        "accent_theme": "cyan",
        "items": [
            {"order": 0, "title": "Web Foundations", "desc": "HTML, CSS, responsive design", "course_slug": "web-development-fundamentals", "skills": ["HTML5", "CSS3", "Flexbox", "Grid"], "duration": "1 month"},
            {"order": 1, "title": "Frontend Development", "desc": "Modern JavaScript frameworks", "course_slug": "javascript-essentials", "skills": ["JavaScript", "React", "State Management"], "duration": "3 months"},
            {"order": 2, "title": "Backend & APIs", "desc": "Server-side development", "course_slug": None, "skills": ["Node.js", "Express", "REST APIs"], "duration": "2 months"},
            {"order": 3, "title": "Databases", "desc": "Data persistence and management", "course_slug": "database-management", "skills": ["SQL", "MongoDB", "Database Design"], "duration": "1 month"},
            {"order": 4, "title": "Full Stack Project", "desc": "Build complete web application", "course_slug": "full-stack-development", "skills": ["Integration", "Authentication", "Deployment"], "duration": "2 months"}
        ],
    },
    {
        "slug": "ai-machine-learning",
        "title": "AI & Machine Learning Roadmap",
        "category": "Artificial Intelligence",
        "description": "Learn Python, machine learning algorithms, deep learning, model evaluation, and practical AI application development.",
        "level": "Advanced",
        "duration": "10-12 months",
        "icon": "artificial_intelligence",
        "accent_theme": "indigo",
        "items": [
            {"order": 0, "title": "Python & Mathematics", "desc": "Programming and math foundations", "course_slug": "python-fundamentals", "skills": ["Python", "Linear Algebra", "Calculus"], "duration": "2 months"},
            {"order": 1, "title": "Machine Learning", "desc": "Core ML algorithms", "course_slug": "machine-learning-fundamentals", "skills": ["Supervised Learning", "Unsupervised Learning", "Feature Engineering"], "duration": "3 months"},
            {"order": 2, "title": "Deep Learning", "desc": "Neural networks and frameworks", "course_slug": "deep-learning-neural-networks", "skills": ["Neural Networks", "TensorFlow", "PyTorch"], "duration": "3 months"},
            {"order": 3, "title": "Model Evaluation", "desc": "Testing and optimization", "course_slug": None, "skills": ["Cross-Validation", "Hyperparameter Tuning", "MLOps"], "duration": "2 months"},
            {"order": 4, "title": "AI Projects", "desc": "Build real AI applications", "course_slug": None, "skills": ["Computer Vision", "NLP", "Deployment"], "duration": "2 months"}
        ],
    },
    {
        "slug": "cybersecurity-specialist",
        "title": "Cybersecurity Roadmap",
        "category": "Cybersecurity",
        "description": "Build security fundamentals across networking, systems, application security, and common industry security practices.",
        "level": "Beginner",
        "duration": "8-10 months",
        "icon": "cybersecurity",
        "accent_theme": "red",
        "items": [
            {"order": 0, "title": "Security Foundations", "desc": "Core security concepts", "course_slug": "cybersecurity-essentials", "skills": ["CIA Triad", "Threat Models", "Risk Assessment"], "duration": "2 months"},
            {"order": 1, "title": "Networking", "desc": "Network protocols and architecture", "course_slug": "computer-networks", "skills": ["TCP/IP", "DNS", "Network Analysis"], "duration": "2 months"},
            {"order": 2, "title": "System Security", "desc": "Secure systems and infrastructure", "course_slug": None, "skills": ["Linux Security", "Windows Security", "Hardening"], "duration": "2 months"},
            {"order": 3, "title": "Application Security", "desc": "Secure code and applications", "course_slug": None, "skills": ["OWASP", "Secure Coding", "Vulnerability Testing"], "duration": "2 months"},
            {"order": 4, "title": "Security Projects", "desc": "Practical security implementations", "course_slug": None, "skills": ["Penetration Testing", "Security Audits", "Incident Response"], "duration": "2 months"}
        ],
    },
    {
        "slug": "cloud-engineer",
        "title": "Cloud Engineer Roadmap",
        "category": "Cloud Computing",
        "description": "Learn cloud fundamentals, Linux, deployment, containers, scalability, monitoring, and modern cloud architecture practices.",
        "level": "Intermediate",
        "duration": "7-9 months",
        "icon": "cloud_computing",
        "accent_theme": "blue",
        "items": [
            {"order": 0, "title": "Cloud Fundamentals", "desc": "Core cloud concepts", "course_slug": "cloud-computing-fundamentals", "skills": ["IaaS", "PaaS", "SaaS", "Cloud Models"], "duration": "1 month"},
            {"order": 1, "title": "Linux & Networking", "desc": "Essential infrastructure skills", "course_slug": "computer-networks", "skills": ["Linux", "Bash", "Networking"], "duration": "2 months"},
            {"order": 2, "title": "Cloud Services", "desc": "AWS, Azure, or GCP services", "course_slug": None, "skills": ["Compute", "Storage", "Databases", "Networking"], "duration": "2 months"},
            {"order": 3, "title": "Containers & Deployment", "desc": "Modern deployment practices", "course_slug": None, "skills": ["Docker", "Kubernetes", "CI/CD"], "duration": "2 months"},
            {"order": 4, "title": "Cloud Projects", "desc": "Build cloud infrastructure", "course_slug": None, "skills": ["Terraform", "Monitoring", "Security"], "duration": "2 months"}
        ],
    },
]


async def seed_database(session):
    """Main seeding function"""
    try:
        print("[SEED] Starting EduNova database seeding...")
        
        # 1. Seed Course Categories
        print("\n[CATEGORIES] Seeding course categories...")
        category_map = {}
        for cat_data in CATEGORIES:
            cat_id = seed_id("category", cat_data["slug"])
            
            # Check if exists
            result = await session.execute(
                select(CourseCategory).where(CourseCategory.id == cat_id)
            )
            existing = result.scalar_one_or_none()
            
            if not existing:
                category = CourseCategory(
                    id=cat_id,
                    name=cat_data["name"],
                    description=cat_data["description"],
                    display_order=cat_data["order"]
                )
                session.add(category)
                print(f"   + Created: {cat_data['name']}")
            else:
                print(f"   - Exists:  {cat_data['name']}")
            
            category_map[cat_data["slug"]] = cat_id
        
        await session.commit()
        
        # 2. Seed Courses
        print("\n[COURSES] Seeding courses...")
        course_map = {}
        for course_data in COURSES:
            course_id = seed_id("course", course_data["slug"])
            
            result = await session.execute(
                select(Course).where(Course.id == course_id)
            )
            existing = result.scalar_one_or_none()
            
            if not existing:
                course = Course(
                    id=course_id,
                    category_id=category_map[course_data["category_slug"]],
                    name=course_data["name"],
                    slug=course_data["slug"],
                    description=course_data["description"],
                    icon=course_data["icon"],
                    level=course_data["level"],
                    duration_hours=course_data["duration_hours"],
                    accent_color=course_data["accent_color"],
                    is_featured=course_data["is_featured"],
                    status="active"
                )
                session.add(course)
                print(f"   + Created: {course_data['name']}")
            else:
                # Update mutable fields so reruns pick up content changes
                existing.name = course_data["name"]
                existing.description = course_data["description"]
                existing.icon = course_data["icon"]
                existing.level = course_data["level"]
                existing.duration_hours = course_data["duration_hours"]
                existing.accent_color = course_data["accent_color"]
                existing.is_featured = course_data["is_featured"]
                print(f"   ~ Updated: {course_data['name']}")
            
            course_map[course_data["slug"]] = course_id
        
        await session.commit()
        
        # 3. Seed Modules and Lessons - commit per course to avoid Supabase statement timeout
        print("\n[MODULES] Seeding modules and lessons...")
        for course_data in COURSES:
            course_id = course_map[course_data["slug"]]

            # Collect all expected IDs for this course in one query instead of per-row checks
            expected_module_ids = [
                seed_id("module", f"{course_data['slug']}-module-{t['order']}")
                for t in MODULE_TEMPLATES
            ]
            result = await session.execute(
                select(CourseModule.id).where(CourseModule.id.in_(expected_module_ids))
            )
            existing_module_ids = {row[0] for row in result.fetchall()}

            for mod_template in MODULE_TEMPLATES:
                module_slug = f"{course_data['slug']}-module-{mod_template['order']}"
                module_id = seed_id("module", module_slug)

                if module_id not in existing_module_ids:
                    module = CourseModule(
                        id=module_id,
                        course_id=course_id,
                        title=mod_template["title"],
                        description=mod_template["desc"],
                        display_order=mod_template["order"]
                    )
                    session.add(module)

                # Collect all expected lesson IDs for this module in one query
                expected_lesson_ids = [
                    seed_id("lesson", f"{module_slug}-lesson-{lt['order']}")
                    for lt in LESSON_TEMPLATES
                ]
                result = await session.execute(
                    select(Lesson.id).where(Lesson.id.in_(expected_lesson_ids))
                )
                existing_lesson_ids = {row[0] for row in result.fetchall()}

                for lesson_template in LESSON_TEMPLATES:
                    lesson_slug = f"{module_slug}-lesson-{lesson_template['order']}"
                    lesson_id = seed_id("lesson", lesson_slug)

                    if lesson_id not in existing_lesson_ids:
                        lesson = Lesson(
                            id=lesson_id,
                            module_id=module_id,
                            title=f"{mod_template['title']} - {lesson_template['title_suffix']}",
                            description=f"Learn about {lesson_template['title_suffix'].lower()}",
                            content_type=lesson_template["type"],
                            duration_minutes=lesson_template["duration"],
                            display_order=lesson_template["order"]
                        )
                        session.add(lesson)

            # Commit after each course to avoid Supabase statement timeout on large batches
            await session.commit()

        print("   + Created all modules and lessons")
        
        # 4. Seed Roadmaps
        print("\n[ROADMAPS] Seeding roadmaps...")
        for roadmap_data in ROADMAPS:
            roadmap_id = seed_id("roadmap", roadmap_data["slug"])
            
            result = await session.execute(
                select(Roadmap).where(Roadmap.id == roadmap_id)
            )
            existing = result.scalar_one_or_none()
            
            if not existing:
                roadmap = Roadmap(
                    id=roadmap_id,
                    title=roadmap_data["title"],
                    slug=roadmap_data["slug"],
                    category=roadmap_data["category"],
                    description=roadmap_data["description"],
                    level=roadmap_data["level"],
                    duration=roadmap_data["duration"],
                    icon=roadmap_data["icon"],
                    accent_theme=roadmap_data["accent_theme"],
                    is_system=True,
                    user_id=None
                )
                session.add(roadmap)
                print(f"   + Created: {roadmap_data['title']}")
            else:
                # Update mutable fields so reruns pick up content changes
                existing.title = roadmap_data["title"]
                existing.description = roadmap_data["description"]
                existing.level = roadmap_data["level"]
                existing.duration = roadmap_data["duration"]
                existing.icon = roadmap_data["icon"]
                existing.accent_theme = roadmap_data["accent_theme"]
                print(f"   ~ Updated: {roadmap_data['title']}")
            
            # Seed roadmap items
            for item_data in roadmap_data["items"]:
                item_slug = f"{roadmap_data['slug']}-item-{item_data['order']}"
                item_id = seed_id("roadmap-item", item_slug)
                
                result = await session.execute(
                    select(RoadmapItem).where(RoadmapItem.id == item_id)
                )
                existing = result.scalar_one_or_none()
                
                if not existing:
                    course_id = course_map.get(item_data["course_slug"]) if item_data["course_slug"] else None
                    
                    roadmap_item = RoadmapItem(
                        id=item_id,
                        roadmap_id=roadmap_id,
                        course_id=course_id,
                        title=item_data["title"],
                        description=item_data["desc"],
                        skills=item_data["skills"],  # JSONB field
                        duration=item_data["duration"],
                        display_order=item_data["order"]
                    )
                    session.add(roadmap_item)
            
            # Commit after each roadmap to avoid statement timeout
            await session.commit()
        
        print("   + Created all roadmap items")
        
        # 5. Verify seed counts
        print("\n[SUMMARY] Seed data summary:")
        
        result = await session.execute(select(CourseCategory))
        print(f"   - Course Categories: {len(result.scalars().all())}")
        
        result = await session.execute(select(Course))
        print(f"   - Courses: {len(result.scalars().all())}")
        
        result = await session.execute(select(CourseModule))
        print(f"   - Modules: {len(result.scalars().all())}")
        
        result = await session.execute(select(Lesson))
        print(f"   - Lessons: {len(result.scalars().all())}")
        
        result = await session.execute(select(Roadmap))
        print(f"   - Roadmaps: {len(result.scalars().all())}")
        
        result = await session.execute(select(RoadmapItem))
        print(f"   - Roadmap Items: {len(result.scalars().all())}")
        
        print("\n[DONE] Database seeding completed successfully!")
        
    except Exception as e:
        print(f"\n[ERROR] Error during seeding: {e}")
        import traceback
        traceback.print_exc()
        await session.rollback()
        raise


async def main():
    """Main entry point with argument parsing"""
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


if __name__ == "__main__":
    asyncio.run(main())
