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
from typing import Optional
from sqlalchemy import select, text
from app.core.database import get_session_factory
from app.core.config import settings
from app.models import (
    CourseCategory, Course, CourseModule, Lesson,
    Roadmap, RoadmapItem, UserRevisionItem
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
    print("         user_revision_items, user_lesson_progress, user_course_progress")
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
    await session.execute(text("DELETE FROM user_revision_items"))
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

# Courses Data - 19 curated courses (including all 7 canonical EduNova tracks)
COURSES = [
    # Programming (4 courses)
    {
        "slug": "cpp-programming",
        "category_slug": "programming",
        "name": "C++ Programming",
        "description": "High-performance systems and OOP.",
        "icon": "cpp",
        "level": "Intermediate",
        "duration_hours": 40,
        "accent_color": "blue",
        "is_featured": True,
        "prerequisites": ["Basic computer literacy", "Problem-solving fundamentals"],
    },
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
        "prerequisites": ["None - beginner friendly"],
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
        "prerequisites": ["Basic computer literacy"],
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
        "prerequisites": ["Basic HTML/CSS knowledge"],
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
        "prerequisites": ["Basic Python knowledge", "High-school algebra and statistics"],
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
        "prerequisites": ["Python proficiency", "Linear algebra and calculus basics"],
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
        "prerequisites": ["Machine Learning Fundamentals", "Linear Algebra", "Python"],
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
        "prerequisites": ["Basic object-oriented programming", "Kotlin fundamentals"],
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
        "prerequisites": ["Basic computer literacy"],
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
        "prerequisites": ["Basic HTML & web concepts"],
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
        "prerequisites": ["Basic programming experience in any language"],
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
        "prerequisites": ["Basic computer literacy", "Basic data handling"],
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
        "prerequisites": ["Computer architecture basics", "C or C++ basics"],
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
        "prerequisites": ["Basic networking and command-line knowledge"],
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
        "prerequisites": ["Computer networking fundamentals", "Basic Linux knowledge"],
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
        "prerequisites": ["Basic computer concepts"],
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
        "prerequisites": ["Basic command line familiarity"],
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
        "prerequisites": ["None - beginner friendly"],
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
        "prerequisites": ["Solid understanding of DSA", "Backend programming foundation"],
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

# Curated educational resource links matching authentic documentation/tutorials
# Key: (course_slug, module_order, lesson_order) -> (resource_url, resource_provider)
SPECIFIC_LESSON_RESOURCES: dict[tuple[str, int, int], tuple[str, str]] = {
    # 1. C++ Programming
    ("cpp-programming", 0, 0): ("https://en.cppreference.com/w/cpp/language/basic_concepts", "cppreference.com"),
    ("cpp-programming", 0, 1): ("https://www.learncpp.com/cpp-tutorial/introduction-to-functions/", "LearnCpp.com"),
    ("cpp-programming", 0, 2): ("https://www.learncpp.com/cpp-tutorial/introduction-to-pointers/", "LearnCpp.com"),
    ("cpp-programming", 0, 3): ("https://www.learncpp.com/cpp-tutorial/introduction-to-compound-data-types/", "LearnCpp.com"),
    ("cpp-programming", 0, 4): ("https://en.cppreference.com/w/cpp/language/expressions", "cppreference.com"),
    ("cpp-programming", 1, 0): ("https://www.learncpp.com/cpp-tutorial/welcome-to-object-oriented-programming/", "LearnCpp.com"),
    ("cpp-programming", 1, 1): ("https://en.cppreference.com/w/cpp/language/virtual", "cppreference.com"),
    ("cpp-programming", 1, 2): ("https://en.cppreference.com/w/cpp/language/templates", "cppreference.com"),
    ("cpp-programming", 1, 3): ("https://en.cppreference.com/w/cpp/language/raii", "cppreference.com"),
    ("cpp-programming", 1, 4): ("https://en.cppreference.com/w/cpp/language/classes", "cppreference.com"),
    ("cpp-programming", 2, 0): ("https://en.cppreference.com/w/cpp/container", "cppreference.com"),
    ("cpp-programming", 2, 1): ("https://en.cppreference.com/w/cpp/algorithm", "cppreference.com"),
    ("cpp-programming", 2, 2): ("https://en.cppreference.com/w/cpp/memory", "cppreference.com"),
    ("cpp-programming", 2, 3): ("https://www.learncpp.com/cpp-tutorial/introduction-to-standard-library-algorithms/", "LearnCpp.com"),
    ("cpp-programming", 2, 4): ("https://en.cppreference.com/w/cpp/language/exceptions", "cppreference.com"),

    # 2. Data Structures & Algorithms
    ("data-structures-algorithms", 0, 0): ("https://en.wikipedia.org/wiki/Big_O_notation", "Wikipedia"),
    ("data-structures-algorithms", 0, 1): ("https://www.geeksforgeeks.org/data-structures/linked-list/", "GeeksforGeeks"),
    ("data-structures-algorithms", 0, 2): ("https://www.geeksforgeeks.org/hash-table-data-structure/", "GeeksforGeeks"),
    ("data-structures-algorithms", 0, 3): ("https://www.geeksforgeeks.org/stack-data-structure/", "GeeksforGeeks"),
    ("data-structures-algorithms", 0, 4): ("https://www.geeksforgeeks.org/queue-data-structure/", "GeeksforGeeks"),
    ("data-structures-algorithms", 1, 0): ("https://www.geeksforgeeks.org/binary-search-tree-data-structure/", "GeeksforGeeks"),
    ("data-structures-algorithms", 1, 1): ("https://www.geeksforgeeks.org/graph-data-structure-and-algorithms/", "GeeksforGeeks"),
    ("data-structures-algorithms", 1, 2): ("https://en.wikipedia.org/wiki/Dynamic_programming", "Wikipedia"),
    ("data-structures-algorithms", 1, 3): ("https://www.geeksforgeeks.org/sorting-algorithms/", "GeeksforGeeks"),
    ("data-structures-algorithms", 1, 4): ("https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm", "Wikipedia"),
    ("data-structures-algorithms", 2, 0): ("https://www.geeksforgeeks.org/trie-insert-and-search/", "GeeksforGeeks"),
    ("data-structures-algorithms", 2, 1): ("https://www.geeksforgeeks.org/heap-data-structure/", "GeeksforGeeks"),
    ("data-structures-algorithms", 2, 2): ("https://www.geeksforgeeks.org/disjoint-set-data-structures/", "GeeksforGeeks"),
    ("data-structures-algorithms", 2, 3): ("https://leetcode.com/explore/", "LeetCode"),
    ("data-structures-algorithms", 2, 4): ("https://en.wikipedia.org/wiki/A*_search_algorithm", "Wikipedia"),

    # 3. Data Science with Python
    ("data-science-python", 0, 0): ("https://numpy.org/doc/stable/user/quickstart.html", "NumPy Documentation"),
    ("data-science-python", 0, 1): ("https://pandas.pydata.org/docs/user_guide/10min.html", "Pandas Documentation"),
    ("data-science-python", 0, 2): ("https://pandas.pydata.org/docs/user_guide/indexing.html", "Pandas Documentation"),
    ("data-science-python", 0, 3): ("https://pandas.pydata.org/docs/user_guide/reshaping.html", "Pandas Documentation"),
    ("data-science-python", 0, 4): ("https://numpy.org/doc/stable/user/basics.broadcasting.html", "NumPy Documentation"),
    ("data-science-python", 1, 0): ("https://matplotlib.org/stable/tutorials/introductory/quick_start.html", "Matplotlib Documentation"),
    ("data-science-python", 1, 1): ("https://seaborn.pydata.org/tutorial.html", "Seaborn Documentation"),
    ("data-science-python", 1, 2): ("https://en.wikipedia.org/wiki/Exploratory_data_analysis", "Wikipedia"),
    ("data-science-python", 1, 3): ("https://docs.scipy.org/doc/scipy/tutorial/stats.html", "SciPy Documentation"),
    ("data-science-python", 1, 4): ("https://en.wikipedia.org/wiki/Statistical_hypothesis_testing", "Wikipedia"),
    ("data-science-python", 2, 0): ("https://scikit-learn.org/stable/modules/preprocessing.html", "Scikit-Learn Documentation"),
    ("data-science-python", 2, 1): ("https://scikit-learn.org/stable/modules/feature_extraction.html", "Scikit-Learn Documentation"),
    ("data-science-python", 2, 2): ("https://kaggle.com/learn", "Kaggle"),
    ("data-science-python", 2, 3): ("https://pandas.pydata.org/docs/user_guide/timeseries.html", "Pandas Documentation"),
    ("data-science-python", 2, 4): ("https://scikit-learn.org/stable/modules/clustering.html", "Scikit-Learn Documentation"),

    # 4. Python Fundamentals
    ("python-fundamentals", 0, 0): ("https://docs.python.org/3/tutorial/introduction.html", "Python Software Foundation"),
    ("python-fundamentals", 0, 1): ("https://docs.python.org/3/tutorial/controlflow.html", "Python Software Foundation"),
    ("python-fundamentals", 0, 2): ("https://docs.python.org/3/tutorial/controlflow.html#defining-functions", "Python Software Foundation"),
    ("python-fundamentals", 0, 3): ("https://docs.python.org/3/tutorial/datastructures.html", "Python Software Foundation"),
    ("python-fundamentals", 0, 4): ("https://docs.python.org/3/tutorial/datastructures.html#list-comprehensions", "Python Software Foundation"),
    ("python-fundamentals", 1, 0): ("https://docs.python.org/3/tutorial/modules.html", "Python Software Foundation"),
    ("python-fundamentals", 1, 1): ("https://docs.python.org/3/tutorial/inputoutput.html", "Python Software Foundation"),
    ("python-fundamentals", 1, 2): ("https://docs.python.org/3/tutorial/errors.html", "Python Software Foundation"),
    ("python-fundamentals", 1, 3): ("https://docs.python.org/3/tutorial/classes.html", "Python Software Foundation"),
    ("python-fundamentals", 1, 4): ("https://docs.python.org/3/tutorial/classes.html#iterators", "Python Software Foundation"),
    ("python-fundamentals", 2, 0): ("https://docs.python.org/3/library/venv.html", "Python Software Foundation"),
    ("python-fundamentals", 2, 1): ("https://docs.python.org/3/library/asyncio.html", "Python Software Foundation"),
    ("python-fundamentals", 2, 2): ("https://docs.python.org/3/library/unittest.html", "Python Software Foundation"),
    ("python-fundamentals", 2, 3): ("https://realpython.com/", "Real Python"),
    ("python-fundamentals", 2, 4): ("https://peps.python.org/pep-0008/", "Python PEP 8"),

    # 5. Machine Learning Fundamentals
    ("machine-learning-fundamentals", 0, 0): ("https://scikit-learn.org/stable/getting_started.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 0, 1): ("https://scikit-learn.org/stable/modules/linear_model.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 0, 2): ("https://scikit-learn.org/stable/modules/tree.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 0, 3): ("https://scikit-learn.org/stable/modules/ensemble.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 0, 4): ("https://scikit-learn.org/stable/modules/neighbors.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 1, 0): ("https://scikit-learn.org/stable/modules/cross_validation.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 1, 1): ("https://scikit-learn.org/stable/modules/model_evaluation.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 1, 2): ("https://scikit-learn.org/stable/modules/neural_networks_supervised.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 1, 3): ("https://scikit-learn.org/stable/modules/clustering.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 1, 4): ("https://scikit-learn.org/stable/modules/decomposition.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 2, 0): ("https://scikit-learn.org/stable/modules/pipeline.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 2, 1): ("https://scikit-learn.org/stable/modules/grid_search.html", "Scikit-Learn Documentation"),
    ("machine-learning-fundamentals", 2, 2): ("https://developers.google.com/machine-learning/crash-course", "Google Developers"),
    ("machine-learning-fundamentals", 2, 3): ("https://en.wikipedia.org/wiki/Bias%E2%80%93variance_tradeoff", "Wikipedia"),
    ("machine-learning-fundamentals", 2, 4): ("https://scikit-learn.org/stable/auto_examples/index.html", "Scikit-Learn Documentation"),

    # 6. Full Stack Development
    ("full-stack-development", 0, 0): ("https://developer.mozilla.org/en-US/docs/Learn/HTML", "MDN Web Docs"),
    ("full-stack-development", 0, 1): ("https://developer.mozilla.org/en-US/docs/Learn/CSS", "MDN Web Docs"),
    ("full-stack-development", 0, 2): ("https://developer.mozilla.org/en-US/docs/Learn/JavaScript", "MDN Web Docs"),
    ("full-stack-development", 0, 3): ("https://developer.mozilla.org/en-US/docs/Learn/CSS/CSS_layout/Flexbox", "MDN Web Docs"),
    ("full-stack-development", 0, 4): ("https://developer.mozilla.org/en-US/docs/Learn/CSS/CSS_layout/Grids", "MDN Web Docs"),
    ("full-stack-development", 1, 0): ("https://fastapi.tiangolo.com/tutorial/", "FastAPI Documentation"),
    ("full-stack-development", 1, 1): ("https://www.postgresql.org/docs/current/tutorial.html", "PostgreSQL Documentation"),
    ("full-stack-development", 1, 2): ("https://docs.pydantic.dev/latest/", "Pydantic Documentation"),
    ("full-stack-development", 1, 3): ("https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS", "MDN Web Docs"),
    ("full-stack-development", 1, 4): ("https://alembic.sqlalchemy.org/en/latest/tutorial.html", "Alembic Documentation"),
    ("full-stack-development", 2, 0): ("https://react.dev/learn", "React Documentation"),
    ("full-stack-development", 2, 1): ("https://developer.mozilla.org/en-US/docs/Web/Security", "MDN Web Docs"),
    ("full-stack-development", 2, 2): ("https://docs.docker.com/get-started/", "Docker Documentation"),
    ("full-stack-development", 2, 3): ("https://fullstackopen.com/en/", "University of Helsinki Full Stack Open"),
    ("full-stack-development", 2, 4): ("https://12factor.net/", "The Twelve-Factor App"),

    # 7. Software Engineering Fundamentals (SDE)
    ("software-engineering-fundamentals", 0, 0): ("https://en.wikipedia.org/wiki/SOLID", "Wikipedia"),
    ("software-engineering-fundamentals", 0, 1): ("https://refactoring.guru/design-patterns", "Refactoring Guru"),
    ("software-engineering-fundamentals", 0, 2): ("https://refactoring.guru/refactoring", "Refactoring Guru"),
    ("software-engineering-fundamentals", 0, 3): ("https://en.wikipedia.org/wiki/Software_design_pattern", "Wikipedia"),
    ("software-engineering-fundamentals", 0, 4): ("https://en.wikipedia.org/wiki/Clean_Code", "Wikipedia"),
    ("software-engineering-fundamentals", 1, 0): ("https://github.com/donnemartin/system-design-primer", "System Design Primer"),
    ("software-engineering-fundamentals", 1, 1): ("https://docs.github.com/en/actions", "GitHub Docs"),
    ("software-engineering-fundamentals", 1, 2): ("https://en.wikipedia.org/wiki/CAP_theorem", "Wikipedia"),
    ("software-engineering-fundamentals", 1, 3): ("https://martinfowler.com/articles/microservices.html", "Martin Fowler"),
    ("software-engineering-fundamentals", 1, 4): ("https://en.wikipedia.org/wiki/Continuous_integration", "Wikipedia"),
    ("software-engineering-fundamentals", 2, 0): ("https://12factor.net/", "The Twelve-Factor App"),
    ("software-engineering-fundamentals", 2, 1): ("https://sre.google/books/", "Google SRE"),
    ("software-engineering-fundamentals", 2, 2): ("https://kubernetes.io/docs/home/", "Kubernetes Documentation"),
    ("software-engineering-fundamentals", 2, 3): ("https://owasp.org/www-project-top-ten/", "OWASP"),
    ("software-engineering-fundamentals", 2, 4): ("https://martinfowler.com/testing/", "Martin Fowler"),
}

# Fallback default resources by course topic
COURSE_DEFAULT_RESOURCES: dict[str, tuple[str, str]] = {
    "java-programming": ("https://docs.oracle.com/en/java/javase/", "Oracle Java Documentation"),
    "javascript-essentials": ("https://developer.mozilla.org/en-US/docs/Web/JavaScript", "MDN Web Docs"),
    "deep-learning-neural-networks": ("https://pytorch.org/tutorials/", "PyTorch Documentation"),
    "android-development-kotlin": ("https://developer.android.com/get-started/overview", "Android Developers"),
    "web-development-fundamentals": ("https://developer.mozilla.org/en-US/docs/Learn", "MDN Web Docs"),
    "database-management": ("https://www.postgresql.org/docs/", "PostgreSQL Documentation"),
    "operating-systems": ("https://en.wikipedia.org/wiki/Operating_system", "Wikipedia"),
    "cloud-computing-fundamentals": ("https://aws.amazon.com/what-is-cloud-computing/", "AWS Documentation"),
    "cybersecurity-essentials": ("https://www.cisa.gov/cybersecurity-best-practices", "CISA"),
    "computer-networks": ("https://en.wikipedia.org/wiki/Computer_network", "Wikipedia"),
    "git-github": ("https://git-scm.com/doc", "Git Documentation"),
    "generative-ai-fundamentals": ("https://cloud.google.com/ai-learn", "Google Cloud AI"),
}


def get_lesson_resource(course_slug: str, module_order: int, lesson_order: int) -> tuple[str, str]:
    """Retrieve verified authentic learning resource URL and provider for a lesson."""
    specific = SPECIFIC_LESSON_RESOURCES.get((course_slug, module_order, lesson_order))
    if specific:
        return specific
    default_res = COURSE_DEFAULT_RESOURCES.get(course_slug)
    if default_res:
        return default_res
    return ("https://en.wikipedia.org/wiki/Computer_science", "Wikipedia")


# Curated, verified YouTube video learning resources
# Key: (course_slug, module_order, lesson_order) -> (video_url, video_provider)
SPECIFIC_LESSON_VIDEOS: dict[tuple[str, int, int], tuple[str, str]] = {
    # 1. C++ Programming
    ("cpp-programming", 0, 0): ("https://www.youtube.com/watch?v=vLnPwxZdW4Y", "freeCodeCamp.org"),
    ("cpp-programming", 1, 0): ("https://www.youtube.com/watch?v=wN0x9eZLix4", "freeCodeCamp.org"),
    ("cpp-programming", 2, 0): ("https://www.youtube.com/playlist?list=PLlrATfBNZ98dudnM48yfGUldqGD0S4FFb", "The Cherno"),

    # 2. Data Structures & Algorithms
    ("data-structures-algorithms", 0, 0): ("https://www.youtube.com/watch?v=RBSGKlAvoiM", "freeCodeCamp.org"),
    ("data-structures-algorithms", 1, 0): ("https://www.youtube.com/playlist?list=PLDN4rrl48XKpZkf03iYFl-O29szjTrs_O", "Abdul Bari"),
    ("data-structures-algorithms", 2, 0): ("https://www.youtube.com/watch?v=pkYVOmU3MgA", "freeCodeCamp.org"),

    # 3. Data Science with Python
    ("data-science-python", 0, 0): ("https://www.youtube.com/watch?v=LHBE6Q9XlzI", "freeCodeCamp.org"),
    ("data-science-python", 1, 0): ("https://www.youtube.com/watch?v=r-uOLxNrNk8", "freeCodeCamp.org"),
    ("data-science-python", 2, 0): ("https://www.youtube.com/watch?v=xxpc-HPKN28", "freeCodeCamp.org"),

    # 4. Python Fundamentals
    ("python-fundamentals", 0, 0): ("https://www.youtube.com/watch?v=_uQrJ0TkZlc", "Programming with Mosh"),
    ("python-fundamentals", 1, 0): ("https://www.youtube.com/playlist?list=PL-osiE80TeTskrapNbzXhwoFUiLCjGgY7", "Corey Schafer"),
    ("python-fundamentals", 2, 0): ("https://www.youtube.com/watch?v=ZDa-Z5JzLYM", "Corey Schafer"),

    # 5. Machine Learning Fundamentals
    ("machine-learning-fundamentals", 0, 0): ("https://www.youtube.com/playlist?list=PLZHQObOWTQDNU6R1_67000Dx_ZCJB-3pi", "3Blue1Brown"),
    ("machine-learning-fundamentals", 1, 0): ("https://www.youtube.com/watch?v=NWONeJKn6kc", "freeCodeCamp.org"),
    ("machine-learning-fundamentals", 2, 0): ("https://www.youtube.com/playlist?list=PLblh5JKOoLUICTaGLRoHQDuF_7q2GfuJF", "StatQuest"),

    # 6. Full Stack Development
    ("full-stack-development", 0, 0): ("https://www.youtube.com/watch?v=nu_pCVPKzTk", "freeCodeCamp.org"),
    ("full-stack-development", 1, 0): ("https://www.youtube.com/watch?v=bMknfKXIFA8", "freeCodeCamp.org"),
    ("full-stack-development", 2, 0): ("https://www.youtube.com/watch?v=7CqJlxBYj-M", "freeCodeCamp.org"),

    # 7. Software Engineering Fundamentals
    ("software-engineering-fundamentals", 0, 0): ("https://www.youtube.com/watch?v=m8Icp_Cid5o", "freeCodeCamp.org"),
    ("software-engineering-fundamentals", 1, 0): ("https://www.youtube.com/playlist?list=PLWKjhJtqVAbkK24EaPurzMq0-kw5U9pJh", "freeCodeCamp.org"),
    ("software-engineering-fundamentals", 2, 0): ("https://www.youtube.com/watch?v=sO8eGL6QVUw", "Edureka"),
}


def get_lesson_video(course_slug: str, module_order: int, lesson_order: int) -> tuple[Optional[str], Optional[str]]:
    """Retrieve verified authentic YouTube video resource URL and provider for a lesson, or (None, None)."""
    return SPECIFIC_LESSON_VIDEOS.get((course_slug, module_order, lesson_order), (None, None))


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
                    slug=cat_data["slug"],
                    name=cat_data["name"],
                    description=cat_data["description"],
                    display_order=cat_data["order"]
                )
                session.add(category)
                print(f"   + Created: {cat_data['name']}")
            else:
                existing.slug = cat_data["slug"]
                existing.name = cat_data["name"]
                existing.description = cat_data["description"]
                existing.display_order = cat_data["order"]
                print(f"   ~ Updated: {cat_data['name']}")
            
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
                    prerequisites=course_data.get("prerequisites", []),
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
                existing.prerequisites = course_data.get("prerequisites", [])
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
                    select(Lesson).where(Lesson.id.in_(expected_lesson_ids))
                )
                existing_lessons_map = {lesson.id: lesson for lesson in result.scalars().all()}

                for lesson_template in LESSON_TEMPLATES:
                    lesson_slug = f"{module_slug}-lesson-{lesson_template['order']}"
                    lesson_id = seed_id("lesson", lesson_slug)
                    res_url, res_provider = get_lesson_resource(
                        course_data["slug"],
                        mod_template["order"],
                        lesson_template["order"]
                    )
                    vid_url, vid_provider = get_lesson_video(
                        course_data["slug"],
                        mod_template["order"],
                        lesson_template["order"]
                    )

                    if lesson_id not in existing_lessons_map:
                        lesson = Lesson(
                            id=lesson_id,
                            module_id=module_id,
                            title=f"{mod_template['title']} - {lesson_template['title_suffix']}",
                            description=f"Learn about {lesson_template['title_suffix'].lower()}",
                            content_type=lesson_template["type"],
                            duration_minutes=lesson_template["duration"],
                            display_order=lesson_template["order"],
                            resource_url=res_url,
                            resource_provider=res_provider,
                            video_url=vid_url,
                            video_provider=vid_provider
                        )
                        session.add(lesson)
                    else:
                        existing_lesson = existing_lessons_map[lesson_id]
                        existing_lesson.title = f"{mod_template['title']} - {lesson_template['title_suffix']}"
                        existing_lesson.description = f"Learn about {lesson_template['title_suffix'].lower()}"
                        existing_lesson.content_type = lesson_template["type"]
                        existing_lesson.duration_minutes = lesson_template["duration"]
                        existing_lesson.display_order = lesson_template["order"]
                        existing_lesson.resource_url = res_url
                        existing_lesson.resource_provider = res_provider
                        existing_lesson.video_url = vid_url
                        existing_lesson.video_provider = vid_provider

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
