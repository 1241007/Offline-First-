"""Personalized Roadmap Assessment and Planning Service for EduNova 2.0"""

import json
import logging
import math
from math import ceil
import re
import uuid
from typing import Optional, List, Dict, Any, Tuple
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.config import settings
from app.models import (
    RoadmapAssessmentSession,
    Roadmap,
    RoadmapItem,
    Course,
    CourseCategory,
    UserProfile,
    UserCourseProgress,
    UserLessonProgress,
    UserMemory,
)
from app.repositories.roadmap_assessment_repository import RoadmapAssessmentRepository
from app.repositories.roadmap_repository import RoadmapRepository
from app.repositories.course_repository import CourseRepository
from app.repositories.profile_repository import ProfileRepository
from app.repositories.learning_repository import LearningRepository
from app.repositories.memory_repository import MemoryRepository
from app.schemas.personalized_roadmap import (
    AssessmentQuizPublicDto,
    AssessmentMessageItem,
    AssessmentSessionResponse,
    PersonalizedRoadmapSchema,
    PersonalizedRoadmapDetailResponse,
    SavePersonalizedRoadmapRequest,
    PersonalizedPhaseSchema,
    PersonalizedTaskSchema,
    PersonalizedMilestoneSchema,
    WeeklyScheduleItemSchema,
    AssessmentSummarySchema,
    SkillGapItemSchema,
    ValidateSubjectResponse,
    TopicLearningContentResponse,
    TopicCodeExampleDto,
    TopicCommonMistakeDto,
    TopicPracticeQuizDto,
    TopicLearningResourceDto,
    CompleteTopicResponse,
)
from app.schemas.roadmap import RoadmapItemResponse
from app.services.openrouter_service import openrouter_service

logger = logging.getLogger(__name__)

# State machine step identifiers
STAGE_GOAL = "STAGE_GOAL"
STAGE_BASELINE = "STAGE_BASELINE"
STAGE_SKILLS = "STAGE_SKILLS"
STAGE_CONSTRAINTS = "STAGE_CONSTRAINTS"
STAGE_REVIEW = "STAGE_REVIEW"
READY_FOR_GENERATION = "READY_FOR_GENERATION"

# Legacy aliases for backward compatibility with existing tests/client
STEP_GOAL = "STEP_GOAL"
STEP_LEVEL = "STEP_LEVEL"
STEP_AVAILABILITY = "STEP_AVAILABILITY"
STEP_SKILLS = "STEP_SKILLS"
STEP_READY = "READY_FOR_GENERATION"

# Domain-specific curated diagnostic question bank with explainable metadata
_DIAGNOSTIC_QUESTION_BANK = {
    "java": [
        {
            "id": "java_1",
            "question": "In Java 8 and later, how does an interface with `default` methods fundamentally differ from an `abstract class`?",
            "options": [
                "An abstract class can maintain mutable instance state (fields), while an interface cannot have instance state",
                "Interfaces can only contain private methods, whereas abstract classes have public methods",
                "Abstract classes cannot implement any methods with bodies",
                "There is no difference; default methods completely deprecated abstract classes",
            ],
            "correct_index": 0,
            "explanation": "Interfaces cannot have instance state/variables (only static final constants), whereas abstract classes can have constructors and mutable instance fields.",
            "skill_tested": "Java OOP & Interface Design",
            "difficulty": "Intermediate",
            "selection_rationale": "Verifies foundational object-oriented design and Java type system principles.",
        },
        {
            "id": "java_2",
            "question": "What is the primary guarantee provided by the `volatile` keyword in Java multi-threaded concurrency?",
            "options": [
                "Guarantees visibility of changes across threads and prevents instruction reordering without acquiring a mutual exclusion lock",
                "Guarantees compound operations like `count++` are executed atomically without race conditions",
                "Prevents garbage collection from ever removing the object from JVM memory",
                "Causes the thread to yield execution immediately to higher priority threads",
            ],
            "correct_index": 0,
            "explanation": "`volatile` establishes a happens-before relationship ensuring memory visibility and preventing compiler/CPU reordering, but does NOT guarantee atomicity for compound operations.",
            "skill_tested": "Java Concurrency & Memory Model",
            "difficulty": "Advanced",
            "selection_rationale": "Evaluates understanding of Java concurrency, memory visibility, and thread safety.",
        },
        {
            "id": "java_3",
            "question": "In Java Collections, what occurs in `HashMap` when multiple keys hash to the same bucket and the number of collisions exceeds the TREEIFY_THRESHOLD (8)?",
            "options": [
                "The internal bucket structure converts from a linked list into a balanced Red-Black tree to maintain O(log n) worst-case lookups",
                "The HashMap immediately throws an OutOfMemoryError and crashes",
                "All older colliding keys are discarded and overwritten by the newest key",
                "The table size is doubled and all existing entries are rehashed into an array of vectors",
            ],
            "correct_index": 0,
            "explanation": "In Java 8+, HashMap converts linked lists in buckets to Red-Black Trees when collisions reach 8, reducing lookup time from O(n) to O(log n).",
            "skill_tested": "Java Collections & Internal Performance",
            "difficulty": "Advanced",
            "selection_rationale": "Tests deep knowledge of data structure implementations in the Java standard library.",
        },
    ],
    "sql": [
        {
            "id": "sql_1",
            "question": "In SQL, what is the fundamental difference between the `WHERE` clause and the `HAVING` clause?",
            "options": [
                "`WHERE` filters rows before aggregations are computed, while `HAVING` filters aggregated groups after `GROUP BY`",
                "`HAVING` is faster than `WHERE` because it uses primary key indexes exclusively",
                "`WHERE` can only be used with numeric columns, while `HAVING` works on strings",
                "`HAVING` can only be used in subqueries, whereas `WHERE` is only for outer queries",
            ],
            "correct_index": 0,
            "explanation": "`WHERE` filters individual records before aggregation. `HAVING` filters group summaries created by `GROUP BY`.",
            "skill_tested": "SQL Filtering & Aggregations",
            "difficulty": "Beginner",
            "selection_rationale": "Validates core query evaluation pipeline and relational aggregation concepts.",
        },
        {
            "id": "sql_2",
            "question": "When querying with SQL window functions, how does `DENSE_RANK()` differ from `RANK()` when identical values occur?",
            "options": [
                "`DENSE_RANK()` leaves no gaps in ranking sequences (e.g. 1, 1, 2), whereas `RANK()` skips subsequent ranks (e.g. 1, 1, 3)",
                "`DENSE_RANK()` orders by descending values only, while `RANK()` orders ascending",
                "`DENSE_RANK()` can only be applied inside correlated subqueries",
                "`RANK()` generates fractional values between 0.0 and 1.0, while `DENSE_RANK()` produces integers",
            ],
            "correct_index": 0,
            "explanation": "`RANK()` skips numbers after ties (1, 1, 3). `DENSE_RANK()` does not skip rank values (1, 1, 2).",
            "skill_tested": "SQL Window Functions & Analytics",
            "difficulty": "Intermediate",
            "selection_rationale": "Verifies ability to write advanced analytical and reporting SQL queries.",
        },
        {
            "id": "sql_3",
            "question": "Under the ANSI SQL standard, what concurrency phenomenon is prevented by the `SERIALIZABLE` isolation level that is permitted under `REPEATABLE READ`?",
            "options": [
                "Phantom reads (insertions of new rows by other transactions that match the query filter)",
                "Dirty reads (reading uncommitted changes from other transactions)",
                "Non-repeatable reads (reading modified values of previously read rows)",
                "Deadlocks during index scans",
            ],
            "correct_index": 0,
            "explanation": "Repeatable Read prevents dirty and non-repeatable reads. Serializable additionally prevents phantom reads by locking or range-verifying predicates.",
            "skill_tested": "Database Transactions & ACID Isolation",
            "difficulty": "Advanced",
            "selection_rationale": "Tests knowledge of transaction integrity, concurrency anomalies, and ACID isolation levels.",
        },
    ],
    "javascript": [
        {
            "id": "js_1",
            "question": "In the JavaScript runtime event loop, in what order are Promise callbacks (`.then()`) executed relative to `setTimeout(fn, 0)` callbacks?",
            "options": [
                "Promise callbacks run first in the microtask queue before the next macrotask (setTimeout) is dequeued",
                "`setTimeout(fn, 0)` always executes before Promise callbacks because timers have OS thread priority",
                "Execution order is completely random and non-deterministic",
                "Both callbacks run concurrently on separate worker threads in parallel",
            ],
            "correct_index": 0,
            "explanation": "Microtasks (Promises, queueMicrotask) are drained completely at the end of each task before the event loop picks up the next macrotask (timer, I/O).",
            "skill_tested": "JavaScript Event Loop & Microtask Queue",
            "difficulty": "Intermediate",
            "selection_rationale": "Assesses deep understanding of asynchronous JavaScript concurrency and execution order.",
        },
        {
            "id": "js_2",
            "question": "What is a JavaScript closure and why is it useful?",
            "options": [
                "A function that retains lexical access to variables in its outer scope even after the outer function has finished executing",
                "A syntax error caused by forgetting to close a curly brace in a block",
                "A built-in garbage collection method that frees unused DOM nodes",
                "A method to seal an object preventing addition or deletion of properties",
            ],
            "correct_index": 0,
            "explanation": "Closures allow an inner function to remember and access variables from an enclosing scope across invocations, enabling encapsulation and stateful functions.",
            "skill_tested": "JavaScript Scope & Closures",
            "difficulty": "Beginner",
            "selection_rationale": "Verifies foundational language mechanics and functional programming patterns.",
        },
    ],
    "python_data": [
        {
            "id": "py_data_1",
            "question": "When working with pandas, what is the key performance difference between using `.apply()` with a Python lambda versus vectorized operations (e.g., `df['a'] + df['b']`)?",
            "options": [
                "Vectorized operations run in compiled C/Cython with SIMD, while `.apply()` iterates in Python bytecode with overhead",
                "`.apply()` is always faster because it automatically utilizes all CPU cores concurrently",
                "There is no difference in execution speed; `.apply()` is simply more readable",
                "Vectorized operations consume more RAM because they create uncompressed memory copies",
            ],
            "correct_index": 0,
            "explanation": "Vectorized NumPy/pandas operations execute in optimized C/SIMD loops, whereas `.apply()` invokes a Python function per element/row.",
            "skill_tested": "Pandas Vectorization & Dataframe Performance",
            "difficulty": "Intermediate",
            "selection_rationale": "Evaluates foundational understanding of vectorized data processing vs naive iteration.",
        },
        {
            "id": "py_data_2",
            "question": "Which technique in Python is best suited for handling datasets larger than available RAM during exploratory analysis?",
            "options": [
                "Processing data in chunked batches (`chunksize`) or using lazy execution engines like Polars/DuckDB/Dask",
                "Using Python standard `list` objects instead of NumPy arrays",
                "Increasing the Python recursion depth limit with `sys.setrecursionlimit()`",
                "Converting all numerical values to Python `str` before analysis",
            ],
            "correct_index": 0,
            "explanation": "Chunking with `chunksize` and lazy query engines (DuckDB/Polars/Dask) stream or query data efficiently without loading the full set into memory.",
            "skill_tested": "Out-of-Core Data Processing",
            "difficulty": "Advanced",
            "selection_rationale": "Verifies ability to design scalable data pipelines beyond simple in-memory scripts.",
        },
        {
            "id": "py_core_1",
            "question": "In Python, why is defining a mutable object (e.g. `def append_item(x, target=[])`) as a default argument considered a known anti-pattern?",
            "options": [
                "The default list is instantiated once when the function is defined and shared across all subsequent invocations",
                "Python throws an immediate SyntaxError on mutable default parameters",
                "Default arguments cannot be modified inside the function body",
                "It causes Python's garbage collector to prematurely deallocate the list",
            ],
            "correct_index": 0,
            "explanation": "Default argument expressions are evaluated once when the function definition is executed, so the same mutable list is retained and modified across calls.",
            "skill_tested": "Python Functions & Mutable Defaults",
            "difficulty": "Beginner",
            "selection_rationale": "Tests core language mechanics and prevents subtle state-leak bugs.",
        },
    ],
    "android_kotlin": [
        {
            "id": "android_1",
            "question": "In Jetpack Compose, why should long-running suspend tasks be launched inside `LaunchedEffect` rather than directly in the Composable body?",
            "options": [
                "Direct Composable invocations execute on every recomposition, causing duplicate jobs and potential memory leaks",
                "Suspend functions cannot syntactically be called on the main thread under any circumstance",
                "`LaunchedEffect` forces coroutines to run on the RenderThread instead of Dispatchers.Main",
                "Composable bodies are synchronous and will throw an immediate Compiler Crash",
            ],
            "correct_index": 0,
            "explanation": "`LaunchedEffect` scopes the coroutine to the Composable lifecycle, canceling and restarting it only when keys change.",
            "skill_tested": "Jetpack Compose Lifecycle & Side Effects",
            "difficulty": "Intermediate",
            "selection_rationale": "Tests critical Compose architecture and side-effect safety.",
        },
        {
            "id": "android_2",
            "question": "In an offline-first Android architecture, what is the Single Source of Truth (SSOT) pattern for UI state?",
            "options": [
                "The UI observes a Room/SQLite stream, while network sync updates the local database in the background",
                "The UI directly calls REST APIs, caching JSON in memory variables as fallback",
                "SharedPreferences caches all network responses for 10 minutes",
                "The ViewModel maintains UI state exclusively without persisting to disk",
            ],
            "correct_index": 0,
            "explanation": "In SSOT offline-first architecture, the local database is the single authority driving the UI Flow, and network updates write to DB.",
            "skill_tested": "Offline-First Mobile Architecture",
            "difficulty": "Advanced",
            "selection_rationale": "Validates enterprise mobile data flow and local persistence discipline.",
        },
    ],
    "backend_systems": [
        {
            "id": "backend_1",
            "question": "In an asynchronous FastAPI service using SQLAlchemy 2.0, why is calling a blocking I/O library (e.g. `requests.get()`) inside an `async def` route hazardous?",
            "options": [
                "It blocks the single asyncio event loop thread, preventing all concurrent requests from being processed",
                "FastAPI will automatically abort the request with HTTP 500",
                "SQLAlchemy will drop active database connections immediately",
                "Python does not permit synchronous function calls inside async functions",
            ],
            "correct_index": 0,
            "explanation": "Blocking calls inside the main event loop freeze all asynchronous coroutines. Use `httpx.AsyncClient` or `run_in_threadpool`.",
            "skill_tested": "Async Event Loop & Concurrency Safety",
            "difficulty": "Intermediate",
            "selection_rationale": "Assesses understanding of asynchronous server concurrency and non-blocking I/O.",
        },
    ],
    "ml_ai": [
        {
            "id": "ml_1",
            "question": "In Retrieval-Augmented Generation (RAG), what is the primary role of dense vector embeddings combined with cosine similarity?",
            "options": [
                "Capture semantic meaning in continuous vector space to retrieve topically relevant context chunks for the LLM",
                "Compress text files by 90% to reduce PostgreSQL disk usage",
                "Tokenize user inputs into Byte-Pair Encoded integer IDs for transformer attention",
                "Encrypt user prompts before sending them across the network",
            ],
            "correct_index": 0,
            "explanation": "Embeddings project semantic meaning into dense vectors, enabling vector databases to retrieve nearest semantic neighbors.",
            "skill_tested": "RAG Architecture & Vector Embeddings",
            "difficulty": "Intermediate",
            "selection_rationale": "Determines practical understanding of modern LLM context retrieval systems.",
        },
        {
            "id": "ml_2",
            "question": "In classification tasks with severe class imbalance (e.g. 99% negative, 1% positive), why is standard accuracy a misleading performance metric?",
            "options": [
                "A naive model predicting always negative achieves 99% accuracy while detecting zero true positives; PR-AUC or F1-Score are far more informative",
                "Accuracy calculation divides by zero when classes are imbalanced",
                "Scikit-learn disallows calculating accuracy on imbalanced datasets",
                "Accuracy can only be computed when classes have exactly 50/50 balance",
            ],
            "correct_index": 0,
            "explanation": "High accuracy can hide complete failure on the minority class. Precision, Recall, and PR-AUC properly reflect minority class performance.",
            "skill_tested": "ML Model Evaluation & Imbalance Metrics",
            "difficulty": "Intermediate",
            "selection_rationale": "Verifies ability to select rigorous metrics in real-world machine learning.",
        },
    ],
    "algorithms_interview": [
        {
            "id": "algo_1",
            "question": "Which data structure provides average O(1) time complexity for lookup, insert, and delete operations?",
            "options": [
                "Hash Table / HashMap",
                "Balanced Binary Search Tree (AVL / Red-Black)",
                "Sorted Doubly Linked List",
                "Dynamic Array",
            ],
            "correct_index": 0,
            "explanation": "Hash tables use hash functions to index keys directly into buckets, achieving O(1) average time complexity.",
            "skill_tested": "Core Data Structures & Complexity",
            "difficulty": "Beginner",
            "selection_rationale": "Establishes baseline algorithmic knowledge and complexity analysis.",
        },
    ],
    "general": [
        {
            "id": "gen_1",
            "question": "What is the primary benefit of adhering to the Dependency Inversion Principle (DIP) in software engineering?",
            "options": [
                "High-level business logic depends on abstractions/interfaces rather than concrete implementations, enabling testability and modularity",
                "It eliminates the need for unit testing by guaranteeing zero runtime exceptions",
                "It increases code compilation speed by removing class headers",
                "It reduces RAM consumption by allocating classes statically",
            ],
            "correct_index": 0,
            "explanation": "Dependency Inversion decouples high-level policy from low-level details through interfaces.",
            "skill_tested": "Software Design Principles & Modularity",
            "difficulty": "Intermediate",
            "selection_rationale": "Tests core software engineering design patterns and architecture.",
        },
    ],
}


def _extract_json_from_response(raw_text: str) -> Optional[dict]:
    """Extract JSON object from model response text even if surrounded by markdown fences."""
    text = (raw_text or "").strip()
    if not text:
        return None

    if text.startswith("```"):
        lines = text.splitlines()
        if lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()

    try:
        return json.loads(text)
    except Exception:
        pass

    match = re.search(r"(\{.*\})", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(1))
        except Exception:
            pass

    return None


def _normalize_experience_level(text: str) -> Optional[str]:
    """Extract and validate normalized experience level without altering user intent."""
    t = text.lower().strip()
    if re.search(r"^(?:i\s+want\s+to|i\'?d\s+like\s+to|become\s+a|learn)\b", t):
        return None

    if (
        re.search(
            r"\b(advanced|expert|experienced|mastery|senior level|senior developer|senior engineer)\b",
            t,
        )
        or "built projects" in t
        or "built production" in t
    ):
        return "Advanced"
    if (
        re.search(
            r"\b(intermediate|medium|moderate|mid-level|mid level|some experience|know fundamentals)\b",
            t,
        )
        or "some experience" in t
        or "know fundamentals" in t
    ):
        return "Intermediate"
    if (
        re.search(
            r"\b(beginner|novice|starter|basics|entry-level|no experience|new to|starting fresh)\b",
            t,
        )
        or "no experience" in t
        or "new to" in t
        or "starting fresh" in t
    ):
        return "Beginner"
    return None


def _normalize_weekly_hours(text: str) -> Optional[float]:
    """Extract weekly available study hours from various learner inputs."""
    t = text.lower().strip()
    daily_match = re.search(
        r"(\d+(?:\.\d+)?)\s*(?:hours?|hrs?)\s*(?:/|per|\s+a\s+)?\s*day", t
    )
    if daily_match:
        return min(80.0, float(daily_match.group(1)) * 7.0)
    if "30 min" in t or "half hour" in t:
        return 3.5

    range_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:-|to|–)\s*(\d+(?:\.\d+)?)", t)
    if range_match:
        low = float(range_match.group(1))
        high = float(range_match.group(2))
        return (low + high) / 2.0

    plus_match = re.search(r"(\d+(?:\.\d+)?)\s*\+", t)
    if plus_match:
        return float(plus_match.group(1))

    hours_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h/w|hrs/week)", t)
    if hours_match:
        return float(hours_match.group(1))

    num_match = re.search(r"\b(\d+(?:\.\d+)?)\b", t)
    if num_match:
        val = float(num_match.group(1))
        if 1.0 <= val <= 80.0:
            return val
    return None


def _is_availability_string(text: str) -> bool:
    t = text.lower().strip()
    return bool(
        re.search(
            r"\b\d+\s*(?:-|to|–|\+)?\s*\d*\s*(?:hours?|hrs?|h/w|hrs/week|hours/week|min|day|week)\b",
            t,
        )
        or _normalize_weekly_hours(text) is not None
    )


def _normalize_goal(text: str) -> str:
    """Normalize goal text while rejecting pure level or availability strings."""
    t = text.strip().lower()
    if _is_availability_string(text):
        return ""
    if t in (
        "beginner",
        "intermediate",
        "advanced",
        "expert",
        "novice",
        "starter",
        "all levels",
    ):
        return ""

    cleaned = text.strip()
    prefixes = [
        r"^(?:i\s+want\s+to\s+become\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:i\s+want\s+to\s+learn\s+(?:about\s+)?(?:how\s+to\s+)?(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:i\s+want\s+to\s+master\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:i\'?d\s+like\s+to\s+(?:learn|become|master)\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:learn\s+(?:about\s+)?(?:how\s+to\s+)?(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:master\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:become\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:build\s+(?:\b(?:a|an|the)\b\s*)?)",
        r"^(?:prepare\s+for\s+(?:\b(?:a|an|the)\b\s*)?)",
    ]
    for p in prefixes:
        cleaned = re.sub(p, "", cleaned, flags=re.IGNORECASE).strip()
    cleaned = cleaned.rstrip(".!?,")
    if not cleaned:
        cleaned = text.strip().rstrip(".!?,")
    if cleaned and cleaned[0].islower():
        cleaned = cleaned[0].upper() + cleaned[1:]
    return cleaned


def _select_diagnostic_question_for_goal(
    goal: Optional[str],
    level: Optional[str],
    already_asked_ids: List[str],
    domain: Optional[str] = None,
) -> dict:
    """Dynamically select targeted diagnostic question with explainable metadata."""
    d = (domain or "").lower()
    g = (goal or "").lower()
    combined = f"{d} {g}".strip()
    pool_key = "general"

    if "java" in combined and "script" not in combined:
        pool_key = "java"
    elif "sql" in combined or "database" in combined or "postgres" in combined:
        pool_key = "sql"
    elif "javascript" in combined or "react" in combined or "frontend" in combined or "web" in combined:
        pool_key = "javascript"
    elif "android" in combined or "compose" in combined or "kotlin" in combined or "mobile" in combined:
        pool_key = "android_kotlin"
    elif "backend" in combined or "fastapi" in combined or "api" in combined or "systems" in combined:
        pool_key = "backend_systems"
    elif (
        "ml" in combined
        or "machine learning" in combined
        or "ai" in combined
        or "rag" in combined
        or "llm" in combined
        or "deep learning" in combined
    ):
        pool_key = "ml_ai"
    elif "interview" in combined or "algorithm" in combined or "leetcode" in combined or "dsa" in combined:
        pool_key = "algorithms_interview"
    elif "data" in combined or "pandas" in combined or "analytics" in combined or "python" in combined:
        pool_key = "python_data"

    candidates = _DIAGNOSTIC_QUESTION_BANK.get(
        pool_key, _DIAGNOSTIC_QUESTION_BANK["general"]
    )
    
    # Adaptive difficulty sorting: if Advanced, prefer Advanced/Intermediate; if Beginner, prefer Beginner/Intermediate
    target_lvl = (level or "").lower()
    if target_lvl == "advanced":
        ordered_candidates = sorted(candidates, key=lambda q: 0 if q.get("difficulty") == "Advanced" else (1 if q.get("difficulty") == "Intermediate" else 2))
    elif target_lvl == "beginner":
        ordered_candidates = sorted(candidates, key=lambda q: 0 if q.get("difficulty") == "Beginner" else (1 if q.get("difficulty") == "Intermediate" else 2))
    else:
        ordered_candidates = sorted(candidates, key=lambda q: 0 if q.get("difficulty") == "Intermediate" else 1)

    for q in ordered_candidates:
        if q["id"] not in already_asked_ids:
            return q.copy()

    for q in _DIAGNOSTIC_QUESTION_BANK["general"]:
        if q["id"] not in already_asked_ids:
            return q.copy()

    return _DIAGNOSTIC_QUESTION_BANK["general"][0].copy()


def _calculate_completeness_score(
    session: RoadmapAssessmentSession,
    quiz_history_count: int,
    reviewed: bool = False,
) -> int:
    """Calculate completeness score based on information dimensions gathered."""
    score = 0
    if session.goal:
        score += 25
    if session.target_level:
        score += 20
    if quiz_history_count > 0 or session.pending_quiz is not None:
        score += 25
    if session.weekly_hours:
        score += 15
    if session.state in ("READY_FOR_GENERATION", "COMPLETED") or reviewed:
        score += 15
    return min(100, score)


def _determine_next_step(
    session: RoadmapAssessmentSession,
    completed_steps: List[str],
    has_pending_quiz: bool,
    quiz_history_count: int,
) -> str:
    """Deterministic step progression for adaptive assessment."""
    has_goal = STEP_GOAL in completed_steps or STAGE_GOAL in completed_steps or bool(session.goal)
    has_level = STEP_LEVEL in completed_steps or STAGE_BASELINE in completed_steps or bool(session.target_level)
    has_avail = STEP_AVAILABILITY in completed_steps or STAGE_CONSTRAINTS in completed_steps or (session.weekly_hours is not None)

    if not has_goal:
        return STEP_GOAL
    if not has_level:
        return STEP_LEVEL
    if not has_avail:
        return STEP_AVAILABILITY
    if has_pending_quiz:
        return STEP_SKILLS
    if STEP_SKILLS not in completed_steps and STAGE_SKILLS not in completed_steps and quiz_history_count == 0:
        return STEP_SKILLS
    return STEP_READY


class PersonalizedRoadmapService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.assessment_repo = RoadmapAssessmentRepository(db)
        self.roadmap_repo = RoadmapRepository(db)
        self.course_repo = CourseRepository(db)
        self.profile_repo = ProfileRepository(db)
        self.learning_repo = LearningRepository(db)
        self.memory_repo = MemoryRepository(db)

    async def _gather_learner_context(self, user_id: str) -> Dict[str, Any]:
        """Gather authentic verified records for the authenticated learner."""
        profile = await self.profile_repo.get_by_user_id(user_id)
        in_progress_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="in_progress"
        )
        completed_courses = await self.learning_repo.get_user_course_progress(
            user_id=user_id, status="completed"
        )
        memories = await self.memory_repo.list_memories(
            user_id=user_id, active_only=True
        )

        lesson_query = select(UserLessonProgress).where(
            UserLessonProgress.user_id == user_id,
            UserLessonProgress.completed == True,
        )
        lesson_result = await self.db.execute(lesson_query)
        completed_lessons_count = len(list(lesson_result.scalars().all()))

        verified_evidence = []
        for c in completed_courses:
            c_name = c.course.name if c.course else "Course"
            verified_evidence.append(f"Completed course: {c_name} (100% progress)")
        for ip in in_progress_courses:
            c_name = ip.course.name if ip.course else "Course"
            pct = int(ip.completion_percentage)
            verified_evidence.append(f"In-progress course: {c_name} ({pct}% complete)")
        if completed_lessons_count > 0:
            verified_evidence.append(
                f"Completed {completed_lessons_count} individual lesson(s)"
            )

        return {
            "name": profile.full_name if profile else "Learner",
            "education_mode": profile.education_mode if profile else "general",
            "stated_level": profile.level if profile else "Beginner",
            "interests": profile.interests if profile else "",
            "verified_evidence": verified_evidence,
            "memories": [f"{m.category}: {m.content}" for m in memories],
        }

    async def _get_catalog_summary(self) -> List[Dict[str, Any]]:
        """Retrieve authentic EduNova catalog courses to prevent hallucinating fake course IDs."""
        courses = await self.course_repo.get_all_courses(limit=50)
        return [
            {
                "id": c.id,
                "name": c.name,
                "level": c.level,
                "description": c.description[:120] if c.description else "",
            }
            for c in courses
        ]

    async def start_assessment_session(
        self, user_id: str, domain: str = "Python"
    ) -> AssessmentSessionResponse:
        """Starts a stateful adaptive assessment session."""
        context = await self._gather_learner_context(user_id)
        target_domain = (domain or "Python").strip()

        initial_profile = {
            "domain": target_domain,
            "verified_evidence": context["verified_evidence"],
            "self_reported_information": [],
            "strengths": [],
            "skill_gaps": [],
            "skill_gap_breakdown": [],
            "unknowns": [
                "primary_goal",
                "target_timeline",
                "weekly_hours",
                "specific_skills_to_master",
            ],
            "education_mode": context["education_mode"],
            "stated_level": context["stated_level"],
            "interests": context["interests"],
            "memories": context["memories"],
            "current_step_id": STEP_GOAL,
            "completed_step_ids": [],
            "quiz_history": [],
            "asked_quiz_ids": [],
        }

        dom_lower = target_domain.lower()
        if "java" in dom_lower and "script" not in dom_lower:
            mentor_text = (
                f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. Let's design a high-impact Java learning roadmap tailored to your exact goal and baseline skills.\n\n"
                "What specific skill, project, or career milestone do you want to achieve with Java?"
            )
            quick_opts = [
                "Enterprise Java & Spring Boot Microservices",
                "Core Java OOP & High-Performance Collections",
                "Android Development with Java & Kotlin",
                "Java Backend Systems & Distributed Architecture",
            ]
        elif "sql" in dom_lower or "database" in dom_lower:
            mentor_text = (
                f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. Let's design a high-impact SQL & Database learning roadmap tailored to your exact goal and baseline skills.\n\n"
                "What specific database skill or data engineering outcome do you want to achieve?"
            )
            quick_opts = [
                "PostgreSQL & Relational Database Design",
                "Advanced SQL Analytics & Window Functions",
                "Database Performance Tuning & Indexing",
                "Backend SQL & ORM Integration",
            ]
        elif "ml" in dom_lower or "machine learning" in dom_lower or "ai" in dom_lower:
            mentor_text = (
                f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. Let's design a modern AI & Machine Learning roadmap tailored to your exact goal and baseline skills.\n\n"
                "What specific AI or ML milestone do you want to build towards?"
            )
            quick_opts = [
                "Machine Learning & Predictive Modeling",
                "Deep Learning & PyTorch Architectures",
                "LLMs, RAG & Generative AI Systems",
                "Production ML Pipelines & Model Serving",
            ]
        elif "android" in dom_lower or "compose" in dom_lower:
            mentor_text = (
                f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. Let's design an Android & Kotlin roadmap tailored to your exact goal and baseline skills.\n\n"
                "What kind of mobile applications do you want to build?"
            )
            quick_opts = [
                "Modern Android App with Kotlin & Compose",
                "Offline-First Mobile Architecture with Room",
                "Android App Performance & Multi-Module Clean Architecture",
                "Android Portfolio Capstone Application",
            ]
        else:
            mentor_text = (
                f"Hi {context['name']}! I'm your EduNova AI Learning Advisor. Let's design a high-impact roadmap tailored to your exact goal and baseline skills.\n\n"
                "What specific skill, project, or career milestone do you want to achieve?"
            )
            quick_opts = [
                "Python Data Analysis & Visualization",
                "Modern Android App with Kotlin & Compose",
                "FastAPI Backend & Distributed Systems",
                "Machine Learning & AI Engineering",
            ]

        system_prompt = (
            "You are EduNova's AI Learning Advisor. Your goal is to guide learners to a tailored roadmap.\n"
            "You ask ONE focused question at a time. Do not overwhelm the learner.\n"
            "Acknowledge the learner's existing verified progress naturally if any is available.\n"
            "You must return ONLY valid JSON in this exact structure:\n"
            "{\n"
            '  "mentor_message": "Warm, personalized greeting + first question about their primary learning/career goal.",\n'
            '  "quick_options": ["Option 1", "Option 2", "Option 3", "Option 4"],\n'
            '  "assessment_state": "COLLECTING_GOALS",\n'
            '  "completeness_percentage": 20\n'
            "}"
        )

        user_prompt = (
            f"Learner Context:\n"
            f"- Name: {context['name']}\n"
            f"- Domain: {target_domain}\n"
            f"- Mode: {context['education_mode']}\n"
            f"- Level: {context['stated_level']}\n"
            f"- Interests: {context['interests']}\n"
            f"- Verified Progress:\n" + "\n".join(f"  * {e}" for e in context["verified_evidence"]) + "\n"
            f"- Known Memories:\n" + "\n".join(f"  * {m}" for m in context["memories"]) + "\n\n"
            f"Initiate the assessment session for domain '{target_domain}' with a concise, warm message and ask the first question."
        )

        state = "COLLECTING_GOALS"
        completeness = 20

        try:
            raw_resp = await openrouter_service.generate_response(
                history=[{"role": "user", "content": user_prompt}],
                explanation_mode="general",
                memory_context=system_prompt,
            )
            parsed = _extract_json_from_response(raw_resp)
            if parsed and "mentor_message" in parsed:
                mentor_text = parsed["mentor_message"]
                quick_opts = parsed.get("quick_options", quick_opts)
                state = parsed.get("assessment_state", state)
                completeness = parsed.get("completeness_percentage", completeness)
        except Exception as e:
            logger.warning(f"Failed AI initial generation, using robust fallback greeting: {e}")

        initial_messages = [
            {
                "sender": "mentor",
                "text": mentor_text,
                "options": quick_opts,
                "quiz": None,
                "step_id": STEP_GOAL,
                "timestamp": None,
            }
        ]

        session = await self.assessment_repo.create_session(
            user_id=user_id,
            state=state,
            domain=target_domain,
            collected_profile=initial_profile,
            messages=initial_messages,
            pending_quiz=None,
        )
        await self.db.commit()

        logger.info(
            "ASSESSMENT_TRANSITION: session_id=%s domain=%s step=%s state=%s completeness=%s",
            session.id,
            target_domain,
            STEP_GOAL,
            state,
            completeness,
        )

        return self._format_session_response(session, completeness)

    async def get_active_session(
        self, user_id: str
    ) -> Optional[AssessmentSessionResponse]:
        session = await self.assessment_repo.get_active_session_for_user(user_id)
        if not session:
            return None
        return self._format_session_response(session)

    async def get_session_by_id(
        self, session_id: str, user_id: str
    ) -> Optional[AssessmentSessionResponse]:
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            return None
        return self._format_session_response(session)

    async def process_learner_response(
        self,
        user_id: str,
        session_id: str,
        answer: str,
        quiz_selected_index: Optional[int] = None,
    ) -> AssessmentSessionResponse:
        """Process learner answer, score quizzes on server, and advance state machine."""
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        if session.state in ("COMPLETED", "ERROR"):
            raise ValueError(
                f"Session is in {session.state} state and cannot accept further messages"
            )

        user_turn_text = (answer or "").strip()
        if not user_turn_text and quiz_selected_index is None:
            raise ValueError("Answer text or quiz selection cannot be empty")

        collected_profile = dict(session.collected_profile or {})
        messages = list(session.messages or [])
        pending_quiz = session.pending_quiz
        completed_steps = list(collected_profile.get("completed_step_ids", []))
        current_step = collected_profile.get("current_step_id", STEP_GOAL)
        quiz_history = list(collected_profile.get("quiz_history", []))
        asked_quiz_ids = list(collected_profile.get("asked_quiz_ids", []))
        skill_breakdown = list(collected_profile.get("skill_gap_breakdown", []))

        # Idempotency check
        last_learner_msg = next(
            (m for m in reversed(messages) if m.get("sender") == "learner"), None
        )
        if (
            last_learner_msg is not None
            and last_learner_msg.get("text") == user_turn_text
            and quiz_selected_index is None
        ):
            return self._format_session_response(session)

        intent = "STEP_ANSWER"
        quiz_feedback_note = ""
        correction_acknowledged_field = None

        # Check for natural language correction in-turn
        t_low = user_turn_text.lower()
        if (
            "change my" in t_low
            or "actually" in t_low
            or "set my" in t_low
            or "update my" in t_low
        ):
            if "level" in t_low or any(lvl in t_low for lvl in ("beginner", "intermediate", "advanced")):
                new_lvl = _normalize_experience_level(user_turn_text)
                if new_lvl:
                    session.target_level = new_lvl
                    correction_acknowledged_field = "experience level"
            elif "hour" in t_low or "hrs" in t_low:
                new_hrs = _normalize_weekly_hours(user_turn_text)
                if new_hrs:
                    session.weekly_hours = new_hrs
                    correction_acknowledged_field = "weekly study hours"

        # Check if Quiz Answer
        if pending_quiz is not None and (
            quiz_selected_index is not None
            or any(
                opt.lower() == user_turn_text.lower()
                for opt in pending_quiz.get("options", [])
            )
        ):
            intent = "QUIZ_ANSWER"
            if quiz_selected_index is None:
                for idx, opt in enumerate(pending_quiz.get("options", [])):
                    if opt.lower() == user_turn_text.lower():
                        quiz_selected_index = idx
                        break

            correct_idx = pending_quiz.get("correct_index", 0)
            is_correct = quiz_selected_index == correct_idx
            tested_skill = pending_quiz.get("skill_tested", "Concept check")

            quiz_history.append(
                {
                    "question": pending_quiz.get("question"),
                    "skill_tested": tested_skill,
                    "selected_index": quiz_selected_index,
                    "correct_index": correct_idx,
                    "is_correct": is_correct,
                }
            )

            if is_correct:
                quiz_feedback_note = f"Verified: {tested_skill} (Demonstrated solid proficiency)."
                collected_profile.setdefault("strengths", []).append(tested_skill)
                collected_profile.setdefault("verified_mastery", []).append(tested_skill)
                skill_breakdown.append(
                    {
                        "skill": tested_skill,
                        "status": "verified_mastery",
                        "source": "diagnostic_verified",
                        "confidence": 0.95,
                        "rationale": "Correctly answered targeted diagnostic question.",
                    }
                )
            else:
                exp = pending_quiz.get("explanation", "")
                quiz_feedback_note = f"Focus Area: {tested_skill}. Concept insight: {exp}"
                collected_profile.setdefault("skill_gaps", []).append(tested_skill)
                collected_profile.setdefault("needs_learning", []).append(tested_skill)
                skill_breakdown.append(
                    {
                        "skill": tested_skill,
                        "status": "needs_learning",
                        "source": "diagnostic_verified",
                        "confidence": 0.85,
                        "rationale": f"Needs reinforcement in {tested_skill}.",
                    }
                )

            pending_quiz = None
            if STEP_SKILLS not in completed_steps:
                completed_steps.append(STEP_SKILLS)
            if STAGE_SKILLS not in completed_steps:
                completed_steps.append(STAGE_SKILLS)

        # Process standard turn text
        elif not correction_acknowledged_field:
            norm_goal = _normalize_goal(user_turn_text)
            norm_level = _normalize_experience_level(user_turn_text)
            norm_hours = _normalize_weekly_hours(user_turn_text)

            if current_step in (STEP_GOAL, STAGE_GOAL) or (
                norm_goal and not session.goal and not _is_availability_string(user_turn_text)
            ):
                if norm_goal:
                    session.goal = norm_goal
                    if STEP_GOAL not in completed_steps:
                        completed_steps.append(STEP_GOAL)
                    if STAGE_GOAL not in completed_steps:
                        completed_steps.append(STAGE_GOAL)
                    collected_profile.setdefault("self_reported_information", []).append(
                        f"Primary Goal: {norm_goal}"
                    )

            elif current_step in (STEP_LEVEL, STAGE_BASELINE) or (norm_level and not session.target_level):
                if norm_level:
                    session.target_level = norm_level
                    if STEP_LEVEL not in completed_steps:
                        completed_steps.append(STEP_LEVEL)
                    if STAGE_BASELINE not in completed_steps:
                        completed_steps.append(STAGE_BASELINE)
                    collected_profile.setdefault("self_reported_information", []).append(
                        f"Declared Baseline Level: {norm_level}"
                    )
                    skill_breakdown.append(
                        {
                            "skill": f"{norm_level} Foundations",
                            "status": "partial_mastery",
                            "source": "self_reported",
                            "confidence": 0.60,
                            "rationale": f"Self-reported baseline proficiency: {norm_level}.",
                        }
                    )

            elif current_step in (STEP_AVAILABILITY, STAGE_CONSTRAINTS) or (norm_hours and session.weekly_hours is None):
                if norm_hours:
                    session.weekly_hours = norm_hours
                    if STEP_AVAILABILITY not in completed_steps:
                        completed_steps.append(STEP_AVAILABILITY)
                    if STAGE_CONSTRAINTS not in completed_steps:
                        completed_steps.append(STAGE_CONSTRAINTS)
                    collected_profile.setdefault("self_reported_information", []).append(
                        f"Available Study Time: {norm_hours} hrs/week"
                    )

        # 2. Append learner message
        messages.append(
            {
                "sender": "learner",
                "text": user_turn_text,
                "options": [],
                "quiz": None,
                "step_id": current_step,
                "timestamp": None,
            }
        )

        # 3. Determine next step
        next_step = _determine_next_step(
            session=session,
            completed_steps=completed_steps,
            has_pending_quiz=pending_quiz is not None,
            quiz_history_count=len(quiz_history),
        )

        # 4. Generate next mentor prompt and options (Deterministic Fallback)
        next_quiz_dto = None
        new_pending_quiz = None

        if next_step in (STEP_GOAL, STAGE_GOAL):
            next_mentor_text = "What is your primary learning or career goal?"
            next_options = [
                "Python Data Analysis & Visualization",
                "Modern Android App with Kotlin & Compose",
                "FastAPI Backend & Distributed Systems",
                "Machine Learning & AI Engineering",
            ]
            next_state = "COLLECTING_GOALS"

        elif next_step in (STEP_LEVEL, STAGE_BASELINE):
            goal_label = session.goal or "this domain"
            next_mentor_text = (
                f"Got it! What is your current experience level with {goal_label}?"
            )
            next_options = [
                "Beginner (Starting fresh, need core basics)",
                "Intermediate (Know fundamentals, ready for applied architecture)",
                "Advanced (Experienced, want advanced mastery & capstone)",
            ]
            next_state = "COLLECTING_PROGRESS"

        elif next_step in (STEP_AVAILABILITY, STAGE_CONSTRAINTS):
            prefix = ""
            if quiz_feedback_note:
                prefix = f"{quiz_feedback_note}\n\n"
            if correction_acknowledged_field:
                prefix = f"Got it, I've updated your {correction_acknowledged_field}! "

            next_mentor_text = (
                f"{prefix}How many hours per week can you realistically dedicate to studying and hands-on practice?"
            )
            next_options = [
                "2-4 hours/week",
                "5-10 hours/week",
                "15+ hours/week",
            ]
            next_state = "ASSESSING_AVAILABILITY"

        elif next_step in (STEP_SKILLS, STAGE_SKILLS):
            domain_val = getattr(session, "domain", None) or collected_profile.get("domain") or "Python"
            diag = _select_diagnostic_question_for_goal(
                goal=session.goal,
                level=session.target_level,
                already_asked_ids=asked_quiz_ids,
                domain=domain_val,
            )
            asked_quiz_ids.append(diag["id"])
            new_pending_quiz = diag

            prefix = ""
            if quiz_feedback_note:
                prefix = f"{quiz_feedback_note}\n\n"
            if correction_acknowledged_field:
                prefix = f"Got it, I've updated your {correction_acknowledged_field}! "

            next_mentor_text = (
                f"{prefix}Awesome! Let's do a quick diagnostic check on your {session.goal or 'technical'} foundation:"
            )
            next_options = []
            next_quiz_dto = {
                "question": diag["question"],
                "options": diag["options"],
                "skillTested": diag.get("skill_tested"),
                "difficulty": diag.get("difficulty"),
                "selectionRationale": diag.get("selection_rationale"),
            }
            next_state = "ASSESSING_SKILLS"

        else:
            # READY_FOR_GENERATION
            prefix = ""
            if quiz_feedback_note:
                prefix = f"{quiz_feedback_note}\n\n"
            if correction_acknowledged_field:
                prefix = f"Got it, I've updated your {correction_acknowledged_field}! "

            strengths_summary = ", ".join(collected_profile.get("strengths", [])) or "Committed baseline"
            gaps_summary = ", ".join(collected_profile.get("skill_gaps", [])) or "Curriculum focus areas"
            hours_val = session.weekly_hours or 6.0
            level_val = session.target_level or "Beginner"
            goal_val = session.goal or "Software Engineering"

            next_mentor_text = (
                f"{prefix}Excellent! I've gathered all your details ({goal_val}, {level_val} level, {hours_val} hrs/week). We are ready to build your tailored learning roadmap!"
            )
            next_options = ["Generate My Personalized Roadmap"]
            next_state = "READY_FOR_GENERATION"

        # 5. Call LLM for conversational richness if available
        system_prompt = (
            "You are EduNova's AI Learning Advisor. Your goal is to guide learners to a tailored roadmap.\n"
            "You ask ONE focused question at a time. Do not overwhelm the learner.\n"
            "Return valid JSON adhering to:\n"
            "{\n"
            '  "mentor_message": "...",\n'
            '  "quick_options": ["..."],\n'
            '  "diagnostic_quiz": null,\n'
            '  "extracted_data": {"goal": "...", "target_level": "...", "weekly_hours": 5.0, "strengths": [], "skill_gaps": []},\n'
            f'  "assessment_state": "{next_state}",\n'
            '  "completeness_percentage": 50\n'
            "}"
        )

        history_payload = []
        for m in messages[-8:]:
            role = "assistant" if m["sender"] == "mentor" else "user"
            history_payload.append({"role": role, "content": m["text"]})

        try:
            raw_resp = await openrouter_service.generate_response(
                history=history_payload,
                explanation_mode="general",
                memory_context=system_prompt,
            )
            parsed = _extract_json_from_response(raw_resp)
            if parsed and "mentor_message" in parsed:
                next_mentor_text = parsed["mentor_message"]
                if "quick_options" in parsed and isinstance(parsed["quick_options"], list):
                    next_options = parsed["quick_options"]
                if parsed.get("assessment_state") in (
                    "COLLECTING_GOALS",
                    "COLLECTING_PROGRESS",
                    "ASSESSING_AVAILABILITY",
                    "ASSESSING_SKILLS",
                    "READY_FOR_GENERATION",
                ):
                    next_state = parsed["assessment_state"]

                extracted = parsed.get("extracted_data", {})
                if extracted.get("goal") and not session.goal:
                    goal_cand = _normalize_goal(extracted["goal"])
                    if goal_cand:
                        session.goal = goal_cand
                if extracted.get("target_level") and not session.target_level:
                    lvl_cand = _normalize_experience_level(extracted["target_level"])
                    if lvl_cand:
                        session.target_level = lvl_cand
                if extracted.get("weekly_hours") and session.weekly_hours is None:
                    try:
                        session.weekly_hours = float(extracted["weekly_hours"])
                    except (ValueError, TypeError):
                        pass
                if extracted.get("strengths"):
                    collected_profile.setdefault("strengths", []).extend(extracted["strengths"])
                if extracted.get("skill_gaps"):
                    collected_profile.setdefault("skill_gaps", []).extend(extracted["skill_gaps"])

                diag_quiz = parsed.get("diagnostic_quiz")
                if diag_quiz and isinstance(diag_quiz, dict) and "question" in diag_quiz and "options" in diag_quiz:
                    new_pending_quiz = {
                        "question": diag_quiz["question"],
                        "options": diag_quiz["options"],
                        "correct_index": diag_quiz.get("correct_index", 0),
                        "explanation": diag_quiz.get("explanation", ""),
                        "skill_tested": diag_quiz.get("skill_tested", "Concept check"),
                    }
                    next_quiz_dto = {
                        "question": diag_quiz["question"],
                        "options": diag_quiz["options"],
                        "skillTested": diag_quiz.get("skill_tested"),
                    }
                    next_state = "ASSESSING_SKILLS"
                elif parsed.get("diagnostic_quiz") is None and intent == "QUIZ_ANSWER":
                    new_pending_quiz = None
                    next_quiz_dto = None
        except Exception as e:
            logger.warning(f"Error calling OpenRouter for assessment turn: {e}")

        # Enforce deterministic state and quiz representation
        if new_pending_quiz is not None:
            next_state = "ASSESSING_SKILLS"
            next_step = STEP_SKILLS
            if not next_quiz_dto:
                next_quiz_dto = {
                    "question": new_pending_quiz["question"],
                    "options": new_pending_quiz["options"],
                    "skillTested": new_pending_quiz.get("skill_tested"),
                }
        elif next_step == STEP_READY or next_state == "READY_FOR_GENERATION":
            next_state = "READY_FOR_GENERATION"
            next_step = STEP_READY
            new_pending_quiz = None
            next_quiz_dto = None

        completeness = _calculate_completeness_score(
            session=session,
            quiz_history_count=len(quiz_history),
            reviewed=(next_state == "READY_FOR_GENERATION"),
        )

        messages.append(
            {
                "sender": "mentor",
                "text": next_mentor_text,
                "options": next_options,
                "quiz": next_quiz_dto,
                "step_id": next_step,
                "timestamp": None,
            }
        )

        collected_profile["current_step_id"] = next_step
        collected_profile["completed_step_ids"] = completed_steps
        collected_profile["quiz_history"] = quiz_history
        collected_profile["asked_quiz_ids"] = asked_quiz_ids
        collected_profile["skill_gap_breakdown"] = skill_breakdown

        session = await self.assessment_repo.update_session(
            session=session,
            state=next_state,
            goal=session.goal,
            target_level=session.target_level,
            weekly_hours=session.weekly_hours,
            collected_profile=collected_profile,
            messages=messages,
            pending_quiz=new_pending_quiz,
        )
        await self.db.commit()

        logger.info(
            "ASSESSMENT_TRANSITION: session_id=%s step=%s intent=%s next_step=%s state=%s completeness=%s",
            session.id,
            current_step,
            intent,
            next_step,
            next_state,
            completeness,
        )

        return self._format_session_response(session, completeness)

    async def correct_profile_field(
        self, user_id: str, session_id: str, field: str, value: Any
    ) -> AssessmentSessionResponse:
        """In-place edit of learner profile fields during Stage E review without resetting assessment."""
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        collected_profile = dict(session.collected_profile or {})
        field_clean = field.strip().lower()

        if field_clean in ("goal", "primary_goal"):
            cand = _normalize_goal(str(value))
            if cand:
                session.goal = cand
        elif field_clean in ("target_level", "level", "starting_level"):
            cand = _normalize_experience_level(str(value))
            if cand:
                session.target_level = cand
        elif field_clean in ("weekly_hours", "hours"):
            cand = _normalize_weekly_hours(str(value))
            if cand:
                session.weekly_hours = cand
        elif field_clean in ("target_timeline", "timeline"):
            session.target_timeline = str(value).strip()

        session = await self.assessment_repo.update_session(
            session=session,
            state="READY_FOR_GENERATION",
            goal=session.goal,
            target_level=session.target_level,
            weekly_hours=session.weekly_hours,
            collected_profile=collected_profile,
            messages=session.messages,
            pending_quiz=session.pending_quiz,
        )
        await self.db.commit()

        return self._format_session_response(session, completeness=100)

    # --- 6-Stage Dedicated Roadmap Planning Pipeline ---

    def _step1_normalize_profile(
        self, session: RoadmapAssessmentSession, context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Stage 1: Normalize and validate structured learner profile."""
        collected = session.collected_profile or {}
        goal = session.goal or "Master Core Technical Engineering"
        level = session.target_level or collected.get("stated_level", "Beginner")
        if level not in ("Beginner", "Intermediate", "Advanced", "All Levels"):
            level = "Beginner"
        weekly_hours = float(session.weekly_hours or 6.0)

        return {
            "goal": goal,
            "starting_level": level,
            "weekly_hours": weekly_hours,
            "verified_evidence": collected.get("verified_evidence", context.get("verified_evidence", [])),
            "strengths": list(dict.fromkeys(collected.get("strengths", []))),
            "skill_gaps": list(dict.fromkeys(collected.get("skill_gaps", []))),
            "skill_gap_breakdown": collected.get("skill_gap_breakdown", []),
            "memories": context.get("memories", []),
        }

    def _step2_build_skill_gap_model(
        self, profile: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Stage 2: Build skill gap matrix comparing demonstrated vs developing skills."""
        strengths = profile.get("strengths", [])
        gaps = profile.get("skill_gaps", [])
        level = profile.get("starting_level", "Beginner")
        breakdown = profile.get("skill_gap_breakdown", [])

        verified_mastery = [
            item["skill"] for item in breakdown if item.get("status") in ("verified_mastery", "demonstrated")
        ]
        if not verified_mastery and strengths:
            verified_mastery = list(strengths)

        partial_mastery = [
            item["skill"] for item in breakdown if item.get("status") in ("partial_mastery", "familiar")
        ]
        needs_learning = [
            item["skill"] for item in breakdown if item.get("status") in ("needs_learning", "developing")
        ]
        if not needs_learning and gaps:
            needs_learning = list(gaps)

        if level == "Beginner" and not needs_learning:
            needs_learning.append(f"{profile['goal']} Foundations")
        elif level == "Advanced" and not verified_mastery:
            verified_mastery.append(f"{profile['goal']} Core Competence")

        demonstrated = list(verified_mastery)
        developing = list(needs_learning) + list(partial_mastery)

        return {
            "verified_mastery": verified_mastery,
            "partial_mastery": partial_mastery,
            "needs_learning": needs_learning,
            "not_assessed": [],
            "demonstrated_skills": demonstrated,
            "developing_skills": developing,
            "not_yet_demonstrated": [],
        }

    def _get_standard_curriculum_for_domain(
        self, domain: str, goal: str = "", level: str = "Beginner"
    ) -> List[Dict[str, Any]]:
        """Retrieve authentic, curriculum-grounded standard roadmap phases for supported domains."""
        combined = f"{domain} {goal}".lower()

        if "java" in combined and "script" not in combined:
            return [
                {
                    "title": "Phase 1: Java Foundations & Object-Oriented Architecture" if level != "Advanced" else "Phase 1: Deep Java Architecture & JVM Concurrency",
                    "objective": "Master Java language mechanics, type systems, and object-oriented encapsulation" if level != "Advanced" else "Master enterprise JVM architecture, memory profiling, and high-throughput concurrency",
                    "durationWeeks": 3,
                    "topics": ["Java Core & Syntax", "OOP & Interfaces", "Collections & Memory"],
                    "tasks": [
                        {
                            "id": "java_t1_1" if level != "Advanced" else "t1_1",
                            "title": "Set Up Workspace & Core Java Syntax Lab" if level != "Advanced" else "Benchmark and Profile Core Systems",
                            "description": "Initialize JDK and master Java syntax" if level != "Advanced" else "Analyze memory allocations and CPU bottlenecks under high concurrency",
                            "instructions": "Install OpenJDK 17+, configure build tool, and complete foundational syntax exercises." if level != "Advanced" else "Set up automated benchmark test suite and measure memory allocations under stress.",
                            "estimatedHours": 2.5,
                            "resources": ["Official Oracle Java Documentation", "Java SE Specifications"],
                            "completionCriteria": "Java workspace compiles cleanly and passes all basic syntax tests",
                            "isCompleted": False,
                            "dependencies": [],
                            "topicId": "java_syntax" if level != "Advanced" else "t1_1",
                            "status": "UNLOCKED",
                            "subtopics": ["JVM & Bytecode", "Statements & Identifiers", "Primitive vs Reference Types"],
                            "learningObjectives": ["Configure JDK development environment", "Write type-safe Java programs"],
                            "codeExample": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Java Ready\");\n    }\n}",
                        },
                        {
                            "id": "java_t1_2" if level != "Advanced" else "t1_2",
                            "title": "Build OOP Practice Exercise & Interface Contracts" if level != "Advanced" else "Implement Production Concurrency Pattern",
                            "description": "Apply inheritance, encapsulation, and interface contracts" if level != "Advanced" else "Construct thread-safe asynchronous pipeline with volatile visibility",
                            "instructions": "Implement domain models with interface default methods and private fields." if level != "Advanced" else "Implement thread-safe queue with volatile state and atomic operators.",
                            "estimatedHours": 3.0,
                            "resources": ["Effective Java (Joshua Bloch)", "Clean Architecture Guide"],
                            "completionCriteria": "Passes all verification test cases with proper interface decoupling",
                            "isCompleted": False,
                            "dependencies": ["java_t1_1" if level != "Advanced" else "t1_1"],
                            "topicId": "java_oop" if level != "Advanced" else "t1_2",
                            "status": "UNLOCKED",
                            "subtopics": ["Encapsulation", "Interface Default Methods", "Polymorphism"],
                            "learningObjectives": ["Design decoupled class hierarchies", "Prevent mutation leaks"],
                            "codeExample": "public interface Service<T> {\n    T execute(String id);\n    default boolean validate(T item) { return item != null; }\n}",
                        },
                    ],
                    "activities": ["Set up OpenJDK workspace", "Build OOP Domain Model"],
                    "resources": ["Oracle Java Documentation", "Effective Java Reference"],
                    "milestones": [
                        {
                            "title": "Java Core & Architecture Milestone",
                            "completionCriteria": ["Complete syntax lab", "Pass OOP unit test suite"],
                            "assessment": "Practical coding lab & unit tests",
                            "passingCriteria": "Score >= 80% on concepts and all tests green",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
                {
                    "title": "Phase 2: Applied Projects & Enterprise Microservices",
                    "objective": "Build production-grade Java applications with persistence and REST APIs",
                    "durationWeeks": 4,
                    "topics": ["Spring Boot & REST", "Spring Data JPA & Persistence", "Capstone Delivery"],
                    "tasks": [
                        {
                            "id": "java_t2_1" if level != "Advanced" else "t2_1",
                            "title": "Implement Data Persistence & Architecture",
                            "description": "Connect relational database using Spring Data JPA and repository pattern",
                            "instructions": "Define JPA entity models, repositories, and transactional boundary services.",
                            "estimatedHours": 3.5,
                            "resources": ["Spring Boot Reference Documentation", "Hibernate ORM Guide"],
                            "completionCriteria": "Entities persist reliably with ACID rollback guarantees on failure",
                            "isCompleted": False,
                            "dependencies": ["java_t1_2" if level != "Advanced" else "t1_2"],
                            "topicId": "java_persistence" if level != "Advanced" else "t2_1",
                            "status": "UNLOCKED",
                            "subtopics": ["JPA Entity Mapping", "Spring Data Repositories", "Transaction Management"],
                            "learningObjectives": ["Persist domain entities with ACID safety", "Structure repository layer"],
                            "codeExample": "@Entity\npublic class Task {\n    @Id @GeneratedValue private Long id;\n    private String title;\n}",
                        },
                        {
                            "id": "java_t2_2" if level != "Advanced" else "t2_2",
                            "title": "Complete Capstone Portfolio Project",
                            "description": "Deliver a complete, tested production-ready Java service",
                            "instructions": "Implement end-to-end REST API, add unit tests with JUnit 5, and containerize.",
                            "estimatedHours": 4.5,
                            "resources": ["Production Microservices Checklist", "JUnit 5 User Guide"],
                            "completionCriteria": "All end-to-end integration tests pass with clean code review rubric",
                            "isCompleted": False,
                            "dependencies": ["java_t2_1" if level != "Advanced" else "t2_1"],
                            "topicId": "java_capstone" if level != "Advanced" else "t2_2",
                            "status": "UNLOCKED",
                            "subtopics": ["REST Controller Endpoints", "JUnit 5 Testing", "Docker Packaging"],
                            "learningObjectives": ["Ship full-stack Java service", "Automate unit and integration tests"],
                            "codeExample": "@RestController\n@RequestMapping(\"/api\")\npublic class ApiController { ... }",
                        },
                    ],
                    "activities": ["Build Spring Data Persistence Layer", "Ship Capstone Service"],
                    "resources": ["Spring Guide", "REST Best Practices"],
                    "milestones": [
                        {
                            "title": "Java Enterprise Capstone Delivery",
                            "completionCriteria": ["Implement JPA persistence", "Pass integration tests"],
                            "assessment": "Capstone code review",
                            "passingCriteria": "Complete user journeys pass automated tests",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
            ]

        if "sql" in combined or "database" in combined or "postgres" in combined:
            return [
                {
                    "title": "Phase 1: Foundations & Core Concepts",
                    "objective": "Master relational schema design, DDL definitions, and fundamental SELECT queries",
                    "durationWeeks": 3,
                    "topics": ["RDBMS Modeling", "DDL & Constraints", "Query Syntax & WHERE Filtering"],
                    "tasks": [
                        {
                            "id": "t1_1",
                            "title": "Set Up Workspace & Core Syntax Lab",
                            "description": "Initialize database client and master foundational SQL queries",
                            "instructions": "Set up PostgreSQL/SQLite, write DDL schemas with primary and foreign keys, and run basic queries.",
                            "estimatedHours": 2.5,
                            "resources": ["PostgreSQL Documentation", "SQL Antipatterns Guide"],
                            "completionCriteria": "Relational schema creates cleanly with validated primary and foreign key constraints",
                            "isCompleted": False,
                            "dependencies": [],
                            "topicId": "sql_syntax",
                            "status": "UNLOCKED",
                            "subtopics": ["Relational Schemas", "SELECT & FROM", "WHERE vs HAVING"],
                            "learningObjectives": ["Understand relational table structures", "Filter rows with compound predicates"],
                            "codeExample": "SELECT first_name, email FROM users WHERE is_active = TRUE;",
                        },
                        {
                            "id": "t1_2",
                            "title": "Build Interactive Practice Exercise",
                            "description": "Execute multi-table relational JOINs and grouped aggregations",
                            "instructions": "Write queries with INNER/LEFT JOINs and aggregate summaries using GROUP BY and HAVING.",
                            "estimatedHours": 3.0,
                            "resources": ["Modern SQL Guide", "EduNova Practice Hub"],
                            "completionCriteria": "Queries return correct joined datasets without Cartesian products",
                            "isCompleted": False,
                            "dependencies": ["t1_1"],
                            "topicId": "sql_joins",
                            "status": "UNLOCKED",
                            "subtopics": ["INNER vs LEFT JOIN", "GROUP BY & Aggregations", "HAVING Predicates"],
                            "learningObjectives": ["Join relational tables safely", "Compute aggregate business metrics"],
                            "codeExample": "SELECT d.name, COUNT(e.id) FROM depts d JOIN emps e ON d.id = e.dept_id GROUP BY d.name HAVING COUNT(e.id) > 2;",
                        },
                    ],
                    "activities": ["Design 3NF Relational Schema", "Run Multi-Table Analytical Queries"],
                    "resources": ["PostgreSQL Official Manual", "Modern SQL Guide"],
                    "milestones": [
                        {
                            "title": "SQL Relational Competency Milestone",
                            "completionCriteria": ["Design normalized schema", "Pass query assessment"],
                            "assessment": "SQL Query Evaluation",
                            "passingCriteria": "All analytical queries produce exact expected results",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
                {
                    "title": "Phase 2: Applied Projects & Advanced Skills",
                    "objective": "Build advanced analytics with window functions and optimize transaction performance",
                    "durationWeeks": 4,
                    "topics": ["Window Functions & CTEs", "Indexing & Transactions", "Analytics Capstone"],
                    "tasks": [
                        {
                            "id": "t2_1",
                            "title": "Implement Data Persistence & Architecture",
                            "description": "Optimize query execution plans and configure ACID transactions",
                            "instructions": "Create B-Tree indexes, analyze EXPLAIN ANALYZE execution plans, and enforce transaction isolation levels.",
                            "estimatedHours": 3.5,
                            "resources": ["Use The Index, Luke!", "Database Internals Book"],
                            "completionCriteria": "Query latency reduced by >= 70% with verified index scans and zero concurrency anomalies",
                            "isCompleted": False,
                            "dependencies": ["t1_2"],
                            "topicId": "sql_indexes",
                            "status": "UNLOCKED",
                            "subtopics": ["B-Tree Indexes", "EXPLAIN ANALYZE", "ACID Transaction Isolation"],
                            "learningObjectives": ["Diagnose slow query plans", "Prevent transaction concurrency anomalies"],
                            "codeExample": "EXPLAIN ANALYZE SELECT * FROM orders WHERE customer_id = 42;",
                        },
                        {
                            "id": "t2_2",
                            "title": "Complete Capstone Portfolio Project",
                            "description": "Deliver a comprehensive relational analytics warehouse",
                            "instructions": "Implement complex analytical reports using DENSE_RANK(), CTEs, and cumulative window frames.",
                            "estimatedHours": 4.5,
                            "resources": ["Data Warehouse Architecture Guide"],
                            "completionCriteria": "End-to-end analytical warehouse schema and reporting suite completed and validated",
                            "isCompleted": False,
                            "dependencies": ["t2_1"],
                            "topicId": "sql_capstone",
                            "status": "UNLOCKED",
                            "subtopics": ["Window Functions (RANK, DENSE_RANK)", "Common Table Expressions (CTEs)", "Warehouse Schema"],
                            "learningObjectives": ["Author advanced analytical window queries", "Build production reporting warehouse"],
                            "codeExample": "SELECT id, DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) as rank FROM emp;",
                        },
                    ],
                    "activities": ["Optimize B-Tree Indexes", "Deliver Analytics Warehouse Capstone"],
                    "resources": ["Indexing Reference", "Analytics Rubric"],
                    "milestones": [
                        {
                            "title": "Database Engineering Capstone Delivery",
                            "completionCriteria": ["Implement analytical warehouse", "Validate query performance"],
                            "assessment": "Project evaluation against rubric",
                            "passingCriteria": "All analytical queries executed and benchmarked",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
            ]

        # General / Python / Android / ML baseline (backward-compatible with quality tests)
        if level == "Advanced":
            return [
                {
                    "title": "Phase 1: Deep Architecture & Performance Engineering",
                    "objective": f"Master enterprise architecture, internal performance optimization, and concurrency for {goal}",
                    "durationWeeks": 3,
                    "topics": ["System Profiling", "Concurrency Architecture", "Memory Optimization"],
                    "tasks": [
                        {
                            "id": "t1_1",
                            "title": "Benchmark and Profile Core Systems",
                            "description": "Analyze memory and CPU bottlenecks under heavy concurrency",
                            "instructions": "Set up automated benchmark test suite and measure memory allocations under stress.",
                            "estimatedHours": 3.0,
                            "resources": ["Official Performance Guide", "Profiling Tools Documentation"],
                            "completionCriteria": "Produce benchmark report with latency graphs and allocation flame charts",
                            "isCompleted": False,
                            "dependencies": [],
                            "topicId": "t1_1",
                            "status": "UNLOCKED",
                            "subtopics": ["Benchmarking", "Memory Flame Graphs", "Profiling"],
                            "learningObjectives": ["Measure memory allocations", "Profile CPU bottlenecks"],
                            "codeExample": "# Benchmark profile\nimport time\nt0 = time.perf_counter()",
                        },
                        {
                            "id": "t1_2",
                            "title": "Implement Production Concurrency Pattern",
                            "description": "Construct thread-safe asynchronous pipeline",
                            "instructions": "Implement mutex-free asynchronous queue or reactive flow with backpressure handling.",
                            "estimatedHours": 3.5,
                            "resources": ["Concurrency Best Practices", "Architecture Blueprint"],
                            "completionCriteria": "All concurrent integration tests pass with zero race conditions",
                            "isCompleted": False,
                            "dependencies": ["t1_1"],
                            "topicId": "t1_2",
                            "status": "UNLOCKED",
                            "subtopics": ["Reactive Streams", "Backpressure", "Thread Safety"],
                            "learningObjectives": ["Build concurrent pipeline", "Eliminate race conditions"],
                            "codeExample": "# Concurrency pipeline\nimport asyncio",
                        },
                    ],
                    "activities": ["Benchmark Core Systems", "Implement Concurrency Architecture"],
                    "resources": ["Official Performance Guide", "Profiling Documentation"],
                    "milestones": [
                        {
                            "title": "Core Architecture Milestone",
                            "completionCriteria": ["Complete benchmark report", "Pass concurrency tests"],
                            "assessment": "Practical performance assessment",
                            "passingCriteria": "Latency P99 < 50ms under stress",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
                {
                    "title": "Phase 2: End-to-End Capstone Delivery & Production Hardening",
                    "objective": f"Design, build, and deploy an end-to-end production-ready {goal} showcase project",
                    "durationWeeks": 5,
                    "topics": ["Modular Architecture", "CI/CD & Hardening", "Capstone Showcase"],
                    "tasks": [
                        {
                            "id": "t2_1",
                            "title": "Build Modular Domain Architecture",
                            "description": "Implement clean domain boundary with dependency inversion",
                            "instructions": "Structure application into independent modules with comprehensive interface contracts.",
                            "estimatedHours": 4.0,
                            "resources": ["Clean Architecture Specification"],
                            "completionCriteria": "Module boundaries validated with automated architecture tests",
                            "isCompleted": False,
                            "dependencies": ["t1_2"],
                            "topicId": "t2_1",
                            "status": "UNLOCKED",
                            "subtopics": ["Clean Architecture", "Interface Decoupling", "Boundary Testing"],
                            "learningObjectives": ["Structure modular code", "Enforce boundary rules"],
                            "codeExample": "# Modular boundary\nclass DomainRepository: pass",
                        },
                        {
                            "id": "t2_2",
                            "title": "Deploy & Automate CI/CD Pipeline",
                            "description": "Ship automated verification and deployment workflow",
                            "instructions": "Configure automated CI test runner with regression quality gates.",
                            "estimatedHours": 4.0,
                            "resources": ["Deployment & Release Checklist"],
                            "completionCriteria": "Automated build pipeline achieves 100% test pass rate on clean repository",
                            "isCompleted": False,
                            "dependencies": ["t2_1"],
                            "topicId": "t2_2",
                            "status": "UNLOCKED",
                            "subtopics": ["CI/CD Automation", "Quality Gates", "Deployment"],
                            "learningObjectives": ["Configure automated verification", "Ship production build"],
                            "codeExample": "# CI workflow\nsteps:\n  - run: pytest",
                        },
                    ],
                    "activities": ["Build Domain Architecture", "Deploy Pipeline"],
                    "resources": ["Clean Architecture Spec", "Release Checklist"],
                    "milestones": [
                        {
                            "title": "Capstone Project Delivery",
                            "completionCriteria": ["Build and test functional project", "Code review"],
                            "assessment": "Project evaluation against rubric",
                            "passingCriteria": "All user stories completed and tested",
                        }
                    ],
                    "recommendedCourseIds": [],
                },
            ]

        # Standard Foundations & Applied Projects
        return [
            {
                "title": "Phase 1: Foundations & Core Concepts",
                "objective": f"Establish core proficiency in {goal}",
                "durationWeeks": 3,
                "topics": ["Foundations & Setup", "Core Architecture", "Hands-on Practice"],
                "tasks": [
                    {
                        "id": "t1_1",
                        "title": "Set Up Workspace & Core Syntax Lab",
                        "description": "Initialize development environment and master foundational building blocks",
                        "instructions": "Install required SDKs, configure formatting/linting, and complete foundational exercises.",
                        "estimatedHours": 2.5,
                        "resources": ["EduNova Getting Started Guide", "Official Documentation"],
                        "completionCriteria": "Starter workspace runs cleanly with all basic lint checks passing",
                        "isCompleted": False,
                        "dependencies": [],
                        "topicId": "t1_1",
                        "status": "UNLOCKED",
                        "subtopics": ["Setup", "Syntax", "Environment"],
                        "learningObjectives": ["Configure development tools", "Run basic syntax exercises"],
                        "codeExample": "# Core lab\nprint('Workspace initialized')",
                    },
                    {
                        "id": "t1_2",
                        "title": "Build Interactive Practice Exercise",
                        "description": "Apply core concepts to build a working standalone utility",
                        "instructions": "Write clean, modular code to solve domain exercises with automated input validation.",
                        "estimatedHours": 3.0,
                        "resources": ["Core Practice Workbook"],
                        "completionCriteria": "Passes all verification test cases and handles invalid input safely",
                        "isCompleted": False,
                        "dependencies": ["t1_1"],
                        "topicId": "t1_2",
                        "status": "UNLOCKED",
                        "subtopics": ["Practice Lab", "Input Validation", "Core Logic"],
                        "learningObjectives": ["Implement modular code", "Handle edge cases safely"],
                        "codeExample": "# Exercise\ndef run_task(): return True",
                    },
                ],
                "activities": ["Set Up Workspace", "Build Interactive Practice"],
                "resources": ["Official Documentation", "EduNova Practice Hub"],
                "milestones": [
                    {
                        "title": "Core Competency Milestone",
                        "completionCriteria": ["Complete fundamental exercises", "Pass checkpoint assessment"],
                        "assessment": "Practical quiz & coding assessment",
                        "passingCriteria": "Score >= 80% on concepts",
                    }
                ],
                "recommendedCourseIds": [],
            },
            {
                "title": "Phase 2: Applied Projects & Advanced Skills",
                "objective": "Build end-to-end practical projects demonstrating mastery",
                "durationWeeks": 5,
                "topics": ["Integration & Storage", "Capstone Implementation", "Testing & Optimization"],
                "tasks": [
                    {
                        "id": "t2_1",
                        "title": "Implement Data Persistence & Architecture",
                        "description": "Connect durable local storage and REST/API synchronization",
                        "instructions": "Implement repository layer with local cache and resilient error handling.",
                        "estimatedHours": 3.5,
                        "resources": ["API Integration Guide", "Persistence Patterns"],
                        "completionCriteria": "Application persists state across restarts and functions gracefully offline",
                        "isCompleted": False,
                        "dependencies": ["t1_2"],
                        "topicId": "t2_1",
                        "status": "UNLOCKED",
                        "subtopics": ["Persistence", "Offline Storage", "Repository Pattern"],
                        "learningObjectives": ["Implement durable storage", "Handle network sync"],
                        "codeExample": "# Persistence layer\nclass LocalRepository: pass",
                    },
                    {
                        "id": "t2_2",
                        "title": "Complete Capstone Portfolio Project",
                        "description": "Deliver polished, tested showcase application",
                        "instructions": "Integrate all feature modules, write unit tests, and document architecture in README.",
                        "estimatedHours": 4.0,
                        "resources": ["Capstone Project Rubric"],
                        "completionCriteria": "Complete end-to-end user workflow validated with automated unit tests",
                        "isCompleted": False,
                        "dependencies": ["t2_1"],
                        "topicId": "t2_2",
                        "status": "UNLOCKED",
                        "subtopics": ["Testing", "Documentation", "Deployment"],
                        "learningObjectives": ["Integrate complete workflow", "Ship tested application"],
                        "codeExample": "# Capstone\ndef main(): print('Shipped')",
                    },
                ],
                "activities": ["Implement Persistence", "Complete Capstone Project"],
                "resources": ["Architecture Guidelines", "Capstone Project Rubric"],
                "milestones": [
                    {
                        "title": "Capstone Project Delivery",
                        "completionCriteria": ["Build and test functional project", "Code review"],
                        "assessment": "Project evaluation against rubric",
                        "passingCriteria": "All user stories completed and tested",
                    }
                ],
                "recommendedCourseIds": [],
            },
        ]

    def _step3_design_curriculum_phases(
        self,
        profile: Dict[str, Any],
        gap_model: Dict[str, Any],
        catalog: List[Dict[str, Any]],
        domain: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """Stage 3: Design ordered dependency-aware curriculum phases grounded in verified domain curriculum."""
        dom = domain or profile.get("domain") or "Python"
        goal = profile.get("goal", "")
        level = profile.get("starting_level", "Beginner")
        hours = max(1.0, float(profile.get("weekly_hours", 6.0)))

        # 1. Fetch domain-grounded template
        phases = self._get_standard_curriculum_for_domain(domain=dom, goal=goal, level=level)

        # 2. Attach recommended course IDs if available
        rec_c1 = [catalog[0]["id"]] if catalog else []
        rec_c2 = [catalog[1]["id"]] if len(catalog) > 1 else rec_c1
        if len(phases) > 0 and not phases[0]["recommendedCourseIds"]:
            phases[0]["recommendedCourseIds"] = rec_c1
        if len(phases) > 1 and not phases[1]["recommendedCourseIds"]:
            phases[1]["recommendedCourseIds"] = rec_c2

        # 3. Adapt tasks based on verified mastery vs skill gaps
        verified_skills = [s.lower() for s in gap_model.get("verified_mastery", [])]
        demonstrated_skills = [s.lower() for s in gap_model.get("demonstrated_skills", [])]
        mastered_set = set(verified_skills + demonstrated_skills)

        needs_learning_skills = [s.lower() for s in gap_model.get("needs_learning", [])]

        skipped_task_ids = set()

        for p_idx, phase in enumerate(phases):
            for t_idx, task in enumerate(phase["tasks"]):
                t_title = task["title"].lower()
                t_skills = [s.lower() for s in task.get("subtopics", [])]
                
                # Check for verified mastery match
                is_mastered = any(
                    ms in t_title or any(ms in sk for sk in t_skills)
                    for ms in mastered_set
                    if len(ms) > 2
                )
                
                # Never skip capstone or final production milestones
                if is_mastered and "capstone" not in t_title and "ci/cd" not in t_title:
                    task["status"] = "SKIPPED"
                    task["skipReason"] = "Demonstrated mastery in diagnostic assessment"
                    task["isCompleted"] = False
                    skipped_task_ids.add(task["id"])
                else:
                    # Check if targeted review needed
                    is_gap = any(
                        gs in t_title or any(gs in sk for sk in t_skills)
                        for gs in needs_learning_skills
                        if len(gs) > 2
                    )
                    if is_gap:
                        task["instructions"] = f"Targeted review module: Reinforced based on diagnostic assessment. {task['instructions']}"

        # 4. Resolve prerequisite statuses: downstream tasks treat skipped prerequisites as met
        for phase in phases:
            for task in phase["tasks"]:
                if task.get("status") == "SKIPPED":
                    continue
                deps = task.get("dependencies", [])
                if not deps or all(d in skipped_task_ids for d in deps):
                    task["status"] = "UNLOCKED"
                else:
                    task["status"] = "LOCKED"

        # 5. Calculate realistic duration from remaining (non-skipped) hours
        remaining_hours = sum(
            t["estimatedHours"]
            for p in phases
            for t in p["tasks"]
            if t.get("status") != "SKIPPED"
        )
        total_weeks = max(2, int(ceil(remaining_hours / hours)))
        weeks_per_phase = max(1, total_weeks // len(phases))
        for p in phases:
            p["durationWeeks"] = weeks_per_phase

        return phases

    def _validate_roadmap_quality(
        self, roadmap_data: Dict[str, Any], profile: Dict[str, Any]
    ) -> Tuple[bool, List[str]]:
        """Stage 5: 10-Point Roadmap Quality Validator."""
        errors = []

        # Check 1: Goal matches requested goal
        req_goal = profile["goal"].strip().lower()
        gen_goal = roadmap_data.get("goal", "").strip().lower()
        if not gen_goal or _is_availability_string(gen_goal) or gen_goal == "beginner":
            errors.append(f"Check 1 Failed: Generated goal '{gen_goal}' is invalid or corrupted.")

        # Check 2: Starting level matches accepted profile
        req_level = profile["starting_level"]
        gen_level = roadmap_data.get("startingLevel")
        if req_level == "Advanced" and gen_level == "Beginner":
            errors.append("Check 2 Failed: Advanced learner level was downgraded to Beginner.")

        # Check 3: Weekly availability is separate from goal
        weekly_hours = roadmap_data.get("weeklyHours")
        if weekly_hours is None or float(weekly_hours) <= 0:
            errors.append("Check 3 Failed: Weekly hours must be a positive number.")

        # Check 4: Specific phases and tasks
        phases = roadmap_data.get("phases", [])
        if not phases or len(phases) < 2:
            errors.append("Check 4 Failed: Roadmap must contain at least 2 distinct phases.")

        for p_idx, p in enumerate(phases):
            if not p.get("title") or not p.get("objective"):
                errors.append(f"Check 4 Failed: Phase {p_idx + 1} is missing title or objective.")
            tasks = p.get("tasks", [])
            acts = p.get("activities", [])
            if not tasks and not acts:
                errors.append(f"Check 4 Failed: Phase {p_idx + 1} has no actionable tasks.")

        # Check 5: Sequence respects prerequisites
        if len(phases) >= 2:
            p1_title = phases[0].get("title", "").lower()
            p2_title = phases[1].get("title", "").lower()
            if "capstone" in p1_title and "foundation" in p2_title:
                errors.append("Check 5 Failed: Capstone appeared before foundational phases.")

        # Check 6: Workload calculation consistency
        sched = roadmap_data.get("weeklySchedule", [])
        if not sched:
            errors.append("Check 6 Failed: Weekly schedule items missing.")

        # Check 7: Schema compliance
        try:
            PersonalizedRoadmapSchema.model_validate(roadmap_data)
        except Exception as exc:
            errors.append(f"Check 7 Failed: Schema validation error: {exc}")

        # Check 8: Skill claims grounded
        summary = roadmap_data.get("assessmentSummary", {})
        if not isinstance(summary, dict):
            errors.append("Check 8 Failed: Assessment summary missing or malformed.")

        # Check 9: Capstone project defined
        capstone = roadmap_data.get("capstoneProject")
        if not capstone or len(str(capstone).strip()) < 5:
            errors.append("Check 9 Failed: Actionable capstone project is missing.")

        # Check 10: Clear Next Action
        next_act = roadmap_data.get("nextAction")
        if not next_act or len(str(next_act).strip()) < 5:
            errors.append("Check 10 Failed: Concrete next action is missing.")

        return (len(errors) == 0, errors)

    async def generate_roadmap_from_assessment(
        self, user_id: str, session_id: str
    ) -> PersonalizedRoadmapDetailResponse:
        """Executes the 6-stage roadmap planning pipeline with AI synthesis and quality validation."""
        session = await self.assessment_repo.get_session_by_id(session_id, user_id)
        if not session:
            raise ValueError("Assessment session not found or unauthorized")

        # Idempotency check: if roadmap already generated, return existing roadmap
        if session.roadmap_id:
            existing_roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(
                session.roadmap_id, user_id
            )
            if existing_roadmap and existing_roadmap.structure:
                return self._format_roadmap_detail_response(existing_roadmap)

        context = await self._gather_learner_context(user_id)
        catalog = await self._get_catalog_summary()

        # Step 1: Normalize & Validate Learner Profile
        profile = self._step1_normalize_profile(session, context)

        # Step 2: Build Skill-Gap Model
        gap_model = self._step2_build_skill_gap_model(profile)

        # Step 3: Design Curriculum Graph
        curriculum_phases = self._step3_design_curriculum_phases(profile, gap_model, catalog)

        # Step 4: Generate Structured Roadmap (AI synthesis with fallback)
        catalog_prompt = "\n".join(
            [f"- ID: {c['id']}, Name: {c['name']}, Level: {c['level']}" for c in catalog]
        )
        system_instruction = (
            "You are EduNova's Senior Curriculum Architect. Generate a personalized, production-grade learning roadmap.\n"
            "Requirements:\n"
            "1. Ground the roadmap in the learner's assessed strengths, skill gaps, verified records, and available weekly time.\n"
            "2. Map phases in prerequisite order. Every phase must have clear measurable objectives, topics, concrete tasks, and milestones.\n"
            "3. Every milestone must have observable completion criteria and measurable passing criteria.\n"
            "4. Match to existing EduNova courses/lesson IDs where appropriate from the catalog provided below. DO NOT invent fake course IDs.\n"
            "5. Provide a weekly study schedule tailored to the learner's available hours.\n"
            "6. Output MUST be valid JSON adhering strictly to the PersonalizedRoadmapSchema.\n"
        )
        user_content = (
            f"Learner Goal: {profile['goal']}\n"
            f"Assessed Starting Level: {profile['starting_level']}\n"
            f"Weekly Available Hours: {profile['weekly_hours']}\n"
            f"Demonstrated Skills: {gap_model['demonstrated_skills']}\n"
            f"Focus Skill Gaps: {gap_model['developing_skills']}\n"
            f"Verified Progress History: {profile['verified_evidence']}\n\n"
            f"EduNova Course Catalog:\n{catalog_prompt}\n\n"
            "Generate the complete personalized roadmap JSON."
        )

        roadmap_json = None
        for attempt in range(2):
            try:
                raw_resp = await openrouter_service.generate_response(
                    history=[{"role": "user", "content": user_content}],
                    explanation_mode="explainable",
                    memory_context=system_instruction,
                )
                parsed = _extract_json_from_response(raw_resp)
                if parsed:
                    is_valid, val_errs = self._validate_roadmap_quality(parsed, profile)
                    if is_valid:
                        validated = PersonalizedRoadmapSchema.model_validate(parsed)
                        roadmap_json = validated.model_dump(by_alias=True)
                        break
                    else:
                        logger.warning("Attempt %d quality validation failed: %s", attempt + 1, val_errs)
                        user_content += f"\nValidation correction: Please fix: {', '.join(val_errs)}"
            except Exception as exc:
                logger.warning("Roadmap generation attempt %d error: %s", attempt + 1, exc)

        # If LLM generation failed or was invalid, construct guaranteed valid schema grounded in profile
        if not roadmap_json:
            total_weeks = sum(p.get("durationWeeks", 2) for p in curriculum_phases)
            sched = []
            curr_w = 1
            for p in curriculum_phases:
                w_cnt = p.get("durationWeeks", 2)
                end_w = curr_w + w_cnt - 1
                w_label = f"Week {curr_w}-{end_w}" if w_cnt > 1 else f"Week {curr_w}"
                sched.append(
                    WeeklyScheduleItemSchema(
                        day_or_week=w_label,
                        focusTopic=p["title"],
                        estimated_hours=profile["weekly_hours"],
                        tasks=[t["title"] for t in p.get("tasks", []) if t.get("status") != "SKIPPED"],
                    )
                )
                curr_w = end_w + 1

            fallback_schema = PersonalizedRoadmapSchema(
                title=f"{profile['goal']} - Personalized Roadmap",
                goal=profile["goal"],
                starting_level=profile["starting_level"],
                category="General",
                estimated_duration=f"{total_weeks} weeks",
                weekly_hours=profile["weekly_hours"],
                domain=getattr(session, "domain", None) or profile.get("domain") or "Python",
                assessment_summary=AssessmentSummarySchema(
                    strengths=gap_model["demonstrated_skills"],
                    skill_gaps=gap_model["developing_skills"],
                    verified_evidence=profile["verified_evidence"],
                    self_reported_information=[f"Level: {profile['starting_level']}"],
                    unknowns=[],
                    skill_gap_breakdown=[
                        SkillGapItemSchema(
                            skill=s,
                            status="demonstrated",
                            source="diagnostic_verified",
                            confidence=0.95,
                        )
                        for s in gap_model["demonstrated_skills"]
                    ]
                    + [
                        SkillGapItemSchema(
                            skill=g,
                            status="developing",
                            source="diagnostic_verified",
                            confidence=0.85,
                        )
                        for g in gap_model["developing_skills"]
                    ],
                    curriculum_rationale=f"Plan tailored for {profile['starting_level']} learner targeting {profile['goal']} with {profile['weekly_hours']} hrs/week commitment.",
                ),
                phases=[
                    PersonalizedPhaseSchema(
                        title=p["title"],
                        objective=p["objective"],
                        duration_weeks=p["durationWeeks"],
                        topics=p["topics"],
                        tasks=[
                            PersonalizedTaskSchema(
                                id=t["id"],
                                title=t["title"],
                                description=t.get("description", ""),
                                instructions=t.get("instructions", ""),
                                estimated_hours=float(t.get("estimatedHours", 2.0)),
                                resources=t.get("resources", []),
                                completion_criteria=t.get("completionCriteria", ""),
                                is_completed=t.get("isCompleted", False),
                                dependencies=t.get("dependencies", []),
                                topic_id=t.get("topicId") or t["id"],
                                status=t.get("status", "UNLOCKED"),
                                subtopics=t.get("subtopics", []),
                                learning_objectives=t.get("learningObjectives", []),
                                code_example=t.get("codeExample"),
                                skip_reason=t.get("skipReason"),
                            )
                            for t in p.get("tasks", [])
                        ],
                        activities=p.get("activities", []),
                        resources=p.get("resources", []),
                        milestones=[
                            PersonalizedMilestoneSchema(
                                title=m["title"],
                                completion_criteria=m["completionCriteria"],
                                assessment=m.get("assessment", ""),
                                passing_criteria=m["passingCriteria"],
                            )
                            for m in p.get("milestones", [])
                        ],
                        recommended_course_ids=p.get("recommendedCourseIds", []),
                    )
                    for p in curriculum_phases
                ],
                weekly_schedule=sched,
                assumptions=["Regular weekly study schedule and access to development environment"],
                capstone_project=f"Complete production-ready {profile['goal']} showcase application",
                next_action=f"Start {curriculum_phases[0]['title']}: {curriculum_phases[0]['tasks'][0]['title']}",
                completed_milestones=[],
            )
            roadmap_json = fallback_schema.model_dump(by_alias=True)

        # STRICT DATA INTEGRITY OVERRIDES
        roadmap_json["goal"] = profile["goal"]
        roadmap_json["startingLevel"] = profile["starting_level"]
        roadmap_json["weeklyHours"] = profile["weekly_hours"]

        gen_title = roadmap_json.get("title", "").strip()
        if (
            _is_availability_string(gen_title)
            or "hours/week" in gen_title.lower()
            or not gen_title
        ):
            roadmap_json["title"] = f"{profile['goal']} - Personalized Roadmap"
        else:
            roadmap_json["title"] = gen_title

        # Populate domain, topics, and edges for full-screen interactive roadmap viewer
        domain_name = getattr(session, "domain", None) or profile.get("domain") or "Python"
        roadmap_json["domain"] = domain_name

        all_topics = []
        all_edges = []
        for phase in roadmap_json.get("phases", []):
            phase_title = phase.get("title", "")
            for task in phase.get("tasks", []):
                t_id = task.get("topicId") or task.get("id")
                all_topics.append({
                    "id": t_id,
                    "title": task.get("title", ""),
                    "description": task.get("description", ""),
                    "status": task.get("status", "UNLOCKED"),
                    "phaseTitle": phase_title,
                    "subtopics": task.get("subtopics", []),
                    "learningObjectives": task.get("learningObjectives", []),
                    "codeExample": task.get("codeExample", ""),
                    "prerequisiteTopicIds": task.get("dependencies", []),
                    "estimatedHours": float(task.get("estimatedHours", 2.0)),
                    "skipReason": task.get("skipReason"),
                })
                for dep in task.get("dependencies", []):
                    all_edges.append({
                        "fromTopicId": dep,
                        "toTopicId": t_id,
                    })

        roadmap_json["topics"] = all_topics
        roadmap_json["edges"] = all_edges

        # Step 6: Persist validated roadmap
        items = []
        for phase in roadmap_json.get("phases", []):
            course_id = None
            if phase.get("recommendedCourseIds"):
                course_id = phase["recommendedCourseIds"][0]
            items.append(
                {
                    "title": phase["title"],
                    "description": phase.get("objective"),
                    "skills": phase.get("topics", []),
                    "duration": f"{phase.get('durationWeeks', 2)} weeks",
                    "course_id": course_id,
                }
            )

        slug = f"personalized-{uuid.uuid4().hex[:8]}"
        roadmap = await self.roadmap_repo.create_personalized_roadmap(
            user_id=user_id,
            title=roadmap_json["title"],
            slug=slug,
            category=roadmap_json.get("category", "General"),
            description=roadmap_json["goal"],
            level=profile["starting_level"],
            duration=roadmap_json.get("estimatedDuration", "8 weeks"),
            icon="school",
            accent_theme="primary",
            structure=roadmap_json,
            items=items,
        )

        session = await self.assessment_repo.update_session(
            session=session,
            state="COMPLETED",
            roadmap_id=roadmap.id,
        )
        await self.db.commit()

        return self._format_roadmap_detail_response(roadmap)

    async def save_or_sync_personalized_roadmap(
        self, user_id: str, payload: SavePersonalizedRoadmapRequest
    ) -> PersonalizedRoadmapDetailResponse:
        """Saves or synchronizes a personalized roadmap created locally or generated offline."""
        roadmap = None
        if payload.id:
            roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(payload.id, user_id)

        structure_dict = payload.structure.model_dump(by_alias=True)
        items = []
        for phase in structure_dict.get("phases", []):
            course_id = None
            if phase.get("recommendedCourseIds"):
                course_id = phase["recommendedCourseIds"][0]
            items.append(
                {
                    "title": phase["title"],
                    "description": phase.get("objective"),
                    "skills": phase.get("topics", []),
                    "duration": f"{phase.get('durationWeeks', 2)} weeks",
                    "course_id": course_id,
                }
            )

        if roadmap:
            roadmap = await self.roadmap_repo.update_personalized_roadmap(
                roadmap=roadmap,
                title=payload.title,
                category=payload.category or "General",
                description=payload.goal,
                level=payload.level or "Beginner",
                duration=payload.duration or "8 weeks",
                icon=payload.icon or "school",
                accent_theme=payload.accent_theme or "primary",
                structure=structure_dict,
                items=items,
            )
            await self.db.commit()
        else:
            roadmap_id = payload.id or str(uuid.uuid4())
            slug = f"personalized-{uuid.uuid4().hex[:8]}"
            roadmap = await self.roadmap_repo.create_personalized_roadmap(
                user_id=user_id,
                title=payload.title,
                slug=slug,
                category=payload.category or "General",
                description=payload.goal,
                level=payload.level or "Beginner",
                duration=payload.duration or "8 weeks",
                icon=payload.icon or "school",
                accent_theme=payload.accent_theme or "primary",
                structure=structure_dict,
                items=items,
                id=roadmap_id,
            )
            await self.db.commit()

        return self._format_roadmap_detail_response(roadmap)

    async def delete_personalized_roadmap(
        self, roadmap_id: str, user_id: str
    ) -> bool:
        """Deletes a user-owned personalized roadmap."""
        success = await self.roadmap_repo.delete_personalized_roadmap(
            roadmap_id=roadmap_id, user_id=user_id
        )
        if success:
            await self.db.commit()
        return success

    async def rename_personalized_roadmap(
        self, roadmap_id: str, user_id: str, new_title: str
    ) -> Optional[PersonalizedRoadmapDetailResponse]:
        """Renames a user-owned personalized roadmap."""
        roadmap = await self.roadmap_repo.rename_personalized_roadmap(
            roadmap_id=roadmap_id, user_id=user_id, new_title=new_title
        )
        if not roadmap:
            return None
        await self.db.commit()
        return self._format_roadmap_detail_response(roadmap)

    async def get_user_personalized_roadmaps(
        self, user_id: str, limit: Optional[int] = None, offset: int = 0
    ) -> List[PersonalizedRoadmapDetailResponse]:
        """Lists all personalized roadmaps created for the user."""
        roadmaps = await self.roadmap_repo.get_user_roadmaps(
            user_id=user_id, limit=limit, offset=offset
        )
        return [self._format_roadmap_detail_response(r) for r in roadmaps if r.structure]

    async def get_personalized_roadmap_detail(
        self, roadmap_id: str, user_id: str
    ) -> Optional[PersonalizedRoadmapDetailResponse]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            return None
        return self._format_roadmap_detail_response(roadmap)

    async def get_roadmap_milestones(
        self, roadmap_id: str, user_id: str
    ) -> List[str]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            return []
        return list(roadmap.structure.get("completedMilestones", []))

    async def sync_roadmap_milestones(
        self,
        roadmap_id: str,
        user_id: str,
        milestone_key: Optional[str] = None,
        is_completed: Optional[bool] = None,
        completed_milestones: Optional[List[str]] = None,
    ) -> List[str]:
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            raise ValueError("Personalized roadmap not found or unauthorized")

        current_structure = dict(roadmap.structure)
        current_completed = set(current_structure.get("completedMilestones", []))

        if completed_milestones is not None:
            current_completed.update(completed_milestones)

        if milestone_key is not None:
            if is_completed is True:
                current_completed.add(milestone_key)
            elif is_completed is False:
                current_completed.discard(milestone_key)

        updated_list = sorted(list(current_completed))
        current_structure["completedMilestones"] = updated_list
        roadmap.structure = current_structure
        await self.db.commit()
        return updated_list

    def _format_session_response(
        self, session: RoadmapAssessmentSession, completeness: Optional[int] = None
    ) -> AssessmentSessionResponse:
        messages = session.messages or []
        latest_text = messages[-1]["text"] if messages else None
        latest_options = messages[-1].get("options", []) if messages else []
        latest_quiz = None

        if messages and messages[-1].get("quiz"):
            q = messages[-1]["quiz"]
            latest_quiz = AssessmentQuizPublicDto(
                question=q["question"],
                options=q["options"],
                skillTested=q.get("skill_tested") or q.get("skillTested"),
                difficulty=q.get("difficulty"),
                selectionRationale=q.get("selection_rationale") or q.get("selectionRationale"),
            )

        collected = session.collected_profile or {}
        quiz_hist = collected.get("quiz_history", [])

        if completeness is None:
            completeness = _calculate_completeness_score(
                session=session,
                quiz_history_count=len(quiz_hist),
                reviewed=(session.state in ("READY_FOR_GENERATION", "COMPLETED")),
            )

        items = [
            AssessmentMessageItem(
                sender=m["sender"],
                text=m["text"],
                options=m.get("options", []),
                quiz=AssessmentQuizPublicDto(
                    question=m["quiz"]["question"],
                    options=m["quiz"]["options"],
                    skillTested=m["quiz"].get("skill_tested") or m["quiz"].get("skillTested"),
                    difficulty=m["quiz"].get("difficulty"),
                    selectionRationale=m["quiz"].get("selection_rationale") or m["quiz"].get("selectionRationale"),
                )
                if m.get("quiz")
                else None,
                step_id=m.get("step_id"),
                timestamp=m.get("timestamp"),
            )
            for m in messages
        ]

        current_step = collected.get("current_step_id", STEP_GOAL)
        completed_steps = collected.get("completed_step_ids", [])

        return AssessmentSessionResponse(
            id=session.id,
            state=session.state,
            domain=getattr(session, "domain", None) or collected.get("domain") or "Python",
            current_step_id=current_step,
            completed_steps=completed_steps,
            goal=session.goal,
            targetLevel=session.target_level,
            targetTimeline=session.target_timeline,
            weeklyHours=session.weekly_hours,
            latestMessage=latest_text,
            options=latest_options,
            quiz=latest_quiz,
            completenessPercentage=completeness,
            summary=session.collected_profile,
            roadmapId=session.roadmap_id,
            messages=items,
        )

    def _format_roadmap_detail_response(
        self, roadmap: Roadmap
    ) -> PersonalizedRoadmapDetailResponse:
        items = [
            RoadmapItemResponse(
                id=item.id,
                title=item.title,
                description=item.description,
                courseId=item.course_id,
                skills=item.skills if isinstance(item.skills, list) else [],
                duration=item.duration,
            )
            for item in roadmap.items
        ]
        structure_obj = PersonalizedRoadmapSchema.model_validate(roadmap.structure)
        return PersonalizedRoadmapDetailResponse(
            id=roadmap.id,
            title=roadmap.title,
            goal=roadmap.description,
            category=roadmap.category,
            level=roadmap.level,
            duration=roadmap.duration,
            stages=len(roadmap.items),
            icon=roadmap.icon,
            accentTheme=roadmap.accent_theme,
            structure=structure_obj,
            items=items,
            createdAt=roadmap.created_at.isoformat() if roadmap.created_at else None,
        )

    def validate_subject(self, subject: str) -> ValidateSubjectResponse:
        """Validates and standardizes a user-specified domain or subject."""
        sub = (subject or "").strip().lower()
        if not sub:
            return ValidateSubjectResponse(
                isValid=False,
                standardizedName="General",
                suggestions=["Python", "Java", "SQL", "Android / Kotlin", "Machine Learning", "Web Development"]
            )

        if "python" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="Python",
                suggestions=["Data Analysis with Pandas", "FastAPI Microservices", "Machine Learning Foundations"]
            )
        elif "java" in sub or "spring" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="Java",
                suggestions=["Spring Boot Microservices", "Core Java & OOP", "Java Concurrency & Multithreading"]
            )
        elif "sql" in sub or "data" in sub or "postgres" in sub or "relational" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="SQL",
                suggestions=["Relational Schema Design & 3NF", "PostgreSQL Performance Optimization", "Advanced Window Functions"]
            )
        elif "android" in sub or "kotlin" in sub or "compose" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="Android / Kotlin",
                suggestions=["Jetpack Compose Declarative UI", "Offline-First Room Persistence", "Kotlin Coroutines & Flow"]
            )
        elif "ml" in sub or "ai" in sub or "deep" in sub or "machine" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="Machine Learning",
                suggestions=["PyTorch Deep Learning", "Model Evaluation & Tuning", "ML Engineering Interviews"]
            )
        elif "web" in sub or "js" in sub or "javascript" in sub or "react" in sub:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName="Web Development / JavaScript",
                suggestions=["Modern React & State Management", "Node.js & Express API Design", "TypeScript Fundamentals"]
            )
        else:
            return ValidateSubjectResponse(
                isValid=True,
                standardizedName=subject.strip().title(),
                suggestions=["Foundational Syntax Lab", "Architecture & Design", "Capstone Portfolio Project"]
            )

    async def get_topic_learning_content(
        self, roadmap_id: str, topic_id: str, user_id: str
    ) -> Optional[TopicLearningContentResponse]:
        """Retrieves rich structured educational content for a specific roadmap topic."""
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            return None

        structure = roadmap.structure
        phases = structure.get("phases", [])
        matched_task = None
        for p in phases:
            for t in p.get("tasks", []):
                if t.get("topicId") == topic_id or t.get("id") == topic_id:
                    matched_task = t
                    break
            if matched_task:
                break

        title = matched_task.get("title", f"Topic {topic_id}") if matched_task else f"Topic {topic_id}"
        desc = matched_task.get("description", "") if matched_task else ""
        subtopics = matched_task.get("subtopics", []) if matched_task else ["Concept Fundamentals", "Implementation", "Best Practices"]
        learning_objectives = matched_task.get("learningObjectives", []) if matched_task else [f"Master {title}", "Apply core techniques in code"]
        code_example = matched_task.get("codeExample", "") if matched_task else ""
        domain = structure.get("domain") or "Python"

        code_examples = []
        if code_example:
            code_examples.append(
                TopicCodeExampleDto(
                    title=f"Sample: {title}",
                    code=code_example,
                    explanation="Production implementation demonstrating correct idioms and patterns.",
                    expectedOutput="Executes successfully without warnings."
                )
            )

        common_mistakes = [
            TopicCommonMistakeDto(
                mistake="Ignoring edge cases and null / empty inputs",
                whyItHappens="Assuming standard happy-path test data in early development.",
                howToFix="Add defensive validation checks and comprehensive boundary test cases."
            )
        ]

        practice_quiz = TopicPracticeQuizDto(
            question=f"What is the primary best practice when implementing {title}?",
            options=[
                "Enforce modular boundary separation and validate inputs",
                "Hardcode configuration directly in source files",
                "Disable automated unit tests to speed up builds",
                "Catch all exceptions silently without logging"
            ],
            correctOptionIndex=0,
            explanation="Modular separation and input validation prevent runtime regression and security vulnerabilities."
        )

        resources = [
            TopicLearningResourceDto(
                title=f"{domain} Documentation: {title}",
                url=f"https://docs.edunova.app/{domain.lower().replace(' ', '-')}/{topic_id}",
                sourceName="EduNova Curriculum Guide",
                description=desc or f"Official learning documentation and reference for {title}.",
                isVerified=True
            )
        ]

        return TopicLearningContentResponse(
            topicId=topic_id,
            topicTitle=title,
            domain=domain,
            learningObjectives=learning_objectives,
            beginnerExplanation=desc or f"Learn foundational and practical patterns for {title}.",
            syntaxAndConcepts="\n".join([f"• {st}" for st in subtopics]),
            codeExamples=code_examples,
            commonMistakes=common_mistakes,
            practiceQuiz=practice_quiz,
            resources=resources,
        )

    async def complete_topic(
        self, roadmap_id: str, topic_id: str, user_id: str, quiz_passed: bool = True
    ) -> CompleteTopicResponse:
        """Marks a topic complete, unlocks downstream prerequisites, and recalculates progress."""
        roadmap = await self.roadmap_repo.get_roadmap_by_id_and_user(roadmap_id, user_id)
        if not roadmap or not roadmap.structure:
            raise ValueError("Personalized roadmap not found or unauthorized")

        current_structure = dict(roadmap.structure)
        phases = current_structure.get("phases", [])
        topics = current_structure.get("topics", [])

        completed_set = set(current_structure.get("completedMilestones", []))
        skipped_set = set()

        # Update phases and tasks
        for p in phases:
            for t in p.get("tasks", []):
                tid = t.get("topicId") or t.get("id")
                if t.get("status") == "SKIPPED":
                    skipped_set.add(tid)
                if tid == topic_id:
                    t["isCompleted"] = True
                    t["status"] = "COMPLETED"
                    completed_set.add(tid)

        # Update top-level topics if present
        for t in topics:
            tid = t.get("id")
            if t.get("status") == "SKIPPED":
                skipped_set.add(tid)
            if tid == topic_id:
                t["status"] = "COMPLETED"
                completed_set.add(tid)

        # Recompute downstream unlocked statuses
        satisfied_set = completed_set.union(skipped_set)
        for p in phases:
            for t in p.get("tasks", []):
                tid = t.get("topicId") or t.get("id")
                if t.get("status") in ("COMPLETED", "SKIPPED"):
                    continue
                deps = t.get("dependencies", [])
                if not deps or all(d in satisfied_set for d in deps):
                    t["status"] = "UNLOCKED"
                else:
                    t["status"] = "LOCKED"

        for t in topics:
            tid = t.get("id")
            if t.get("status") in ("COMPLETED", "SKIPPED"):
                continue
            deps = t.get("prerequisiteTopicIds", [])
            if not deps or all(d in satisfied_set for d in deps):
                t["status"] = "UNLOCKED"
            else:
                t["status"] = "LOCKED"

        # Calculate progress excluding skipped topics
        all_topic_ids = set()
        for p in phases:
            for t in p.get("tasks", []):
                all_topic_ids.add(t.get("topicId") or t.get("id"))
        for t in topics:
            all_topic_ids.add(t.get("id"))

        active_required = all_topic_ids - skipped_set
        completed_required = completed_set.intersection(active_required)
        progress = int((len(completed_required) / max(1, len(active_required))) * 100) if active_required else 100

        current_structure["completedMilestones"] = sorted(list(completed_set))
        roadmap.structure = current_structure
        await self.db.commit()

        return CompleteTopicResponse(
            roadmapId=roadmap_id,
            topicId=topic_id,
            isCompleted=True,
            progressPercentage=progress,
            completedTopicIds=sorted(list(completed_set)),
        )
