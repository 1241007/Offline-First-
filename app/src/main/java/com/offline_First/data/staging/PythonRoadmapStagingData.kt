package com.offline_First.data.staging

/**
 * High-signal, offline-first staging data for EduNova Roadmap section.
 * Contains:
 * 1. Seven core Learning Paths
 * 2. Fully realized 6-Module Python Developer Roadmap with connected topics
 * 3. Rich topic lesson contents, code examples, and learning objectives
 * 4. Four practical Python projects
 * 5. Six frequently asked questions with in-depth answers
 */

data class LearningPathItem(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val category: String, // "Role-based" or "Skill-based"
    val level: String,
    val duration: String,
    val totalModules: Int,
    val totalProjects: Int,
    val isImplemented: Boolean = false
)

data class RoadmapLessonTopic(
    val id: String,
    val moduleId: String,
    val title: String,
    val summary: String,
    val estimatedMinutes: Int,
    val objectives: List<String>,
    val explanation: String,
    val codeSnippet: String,
    val keyTakeaways: List<String>
)

data class RoadmapModuleData(
    val id: String,
    val number: Int,
    val title: String,
    val description: String,
    val estimatedWeeks: String,
    val topics: List<RoadmapLessonTopic>
)

data class RoadmapProjectItem(
    val id: String,
    val title: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val estimatedHours: Int,
    val description: String,
    val relevantSkills: List<String>,
    val deliverables: List<String>,
    val instructions: String
)

data class RoadmapFaqItem(
    val id: String,
    val question: String,
    val answer: String
)

data class VisualRoadmapNode(
    val id: String,
    val title: String,
    val section: String,
    val isMajor: Boolean = false,
    val objectives: List<String> = emptyList(),
    val explanation: String = "",
    val codeSnippet: String = "",
    val badge: String? = null
)

data class LessonPackItem(
    val id: String,
    val title: String,
    val description: String,
    val lessonCount: Int,
    val durationMinutes: Int,
    val category: String
)

object PythonRoadmapStagingData {

    // -------------------------------------------------------------------------
    // ROLE-BASED AND SKILL-BASED COURSE CATALOG (Roadmap.sh compact grid)
    // -------------------------------------------------------------------------
    val ROLE_BASED_COURSES = listOf(
        "Frontend Development",
        "Backend Development",
        "Full-Stack Development",
        "Android Development",
        "DevOps",
        "Cybersecurity",
        "Data Analyst",
        "AI Engineer",
        "Machine Learning Engineer",
        "Data Engineer",
        "Software Architect",
        "QA Engineer",
        "UI/UX Designer",
        "Game Developer",
        "Product Manager",
        "Engineering Manager",
        "Network Engineer",
        "Business Intelligence Analyst"
    )

    val SKILL_BASED_COURSES = listOf(
        "Python Programming",
        "Java",
        "Machine Learning",
        "SQL",
        "JavaScript",
        "TypeScript",
        "C++",
        "React",
        "Node.js",
        "Angular",
        "Vue.js",
        "Data Structures & Algorithms",
        "Computer Science",
        "Python for Data Analysis",
        "R Programming",
        "Cloud Computing",
        "System Design",
        "Blockchain",
        "Generative AI",
        "Git & GitHub",
        "Linux"
    )

    val LESSON_PACKS = listOf(
        LessonPackItem(
            id = "pack_python_core",
            title = "Python Fundamentals & Syntax",
            description = "Quick-start guide to Python syntax, variables, conditionals, loops, and basic file operations.",
            lessonCount = 8,
            durationMinutes = 45,
            category = "Python"
        ),
        LessonPackItem(
            id = "pack_data_structures",
            title = "Essential Data Structures",
            description = "Master lists, tuples, sets, dictionaries, and memory efficiency in real-world scenarios.",
            lessonCount = 6,
            durationMinutes = 35,
            category = "Computer Science"
        ),
        LessonPackItem(
            id = "pack_fastapi_rest",
            title = "Building REST APIs with FastAPI",
            description = "Learn how to build asynchronous REST APIs, validate models with Pydantic, and write tests.",
            lessonCount = 7,
            durationMinutes = 50,
            category = "Backend"
        ),
        LessonPackItem(
            id = "pack_git_workflow",
            title = "Git & GitHub for Developers",
            description = "Branching strategies, resolving merge conflicts, pull requests, and commit conventions.",
            lessonCount = 5,
            durationMinutes = 30,
            category = "DevOps"
        )
    )

    // -------------------------------------------------------------------------
    // 1. SEVEN LEARNING PATHS (Screen 1)
    // -------------------------------------------------------------------------
    val LEARNING_PATHS = listOf(
        LearningPathItem(
            id = "python-developer",
            title = "Python Programming",
            description = "Master modern Python 3 syntax, data structures, OOP, file I/O, asynchronous concurrency, and API engineering.",
            iconKey = "python",
            category = "Role-based",
            level = "Beginner to Advanced",
            duration = "6-8 months",
            totalModules = 6,
            totalProjects = 4,
            isImplemented = true
        ),
        LearningPathItem(
            id = "fullstack-web",
            title = "Full-Stack Web Development",
            description = "Modern frontend with responsive UI frameworks, RESTful API design, relational data models, and cloud deployment.",
            iconKey = "full_stack",
            category = "Role-based",
            level = "Intermediate",
            duration = "8-10 months",
            totalModules = 7,
            totalProjects = 5,
            isImplemented = false
        ),
        LearningPathItem(
            id = "machine-learning",
            title = "Machine Learning",
            description = "Mathematical foundations, supervised and unsupervised learning, deep neural networks, and model deployment.",
            iconKey = "machine_learning",
            category = "Role-based",
            level = "Intermediate",
            duration = "10-12 months",
            totalModules = 8,
            totalProjects = 4,
            isImplemented = true
        ),
        LearningPathItem(
            id = "sql-databases",
            title = "SQL & Database Management",
            description = "Relational database schema design, ACID transactions, complex queries, indexing strategies, and performance tuning.",
            iconKey = "database",
            category = "Skill-based",
            level = "Beginner to Intermediate",
            duration = "3-4 months",
            totalModules = 5,
            totalProjects = 3,
            isImplemented = true
        ),
        LearningPathItem(
            id = "cloud-devops",
            title = "Cloud Computing & DevOps",
            description = "Containerization with Docker, Kubernetes orchestration, CI/CD automated pipelines, and cloud infrastructure.",
            iconKey = "cloud_computing",
            category = "Role-based",
            level = "Intermediate to Advanced",
            duration = "7-9 months",
            totalModules = 6,
            totalProjects = 4,
            isImplemented = false
        ),
        LearningPathItem(
            id = "cybersecurity",
            title = "Cybersecurity",
            description = "Network security, threat modeling, ethical hacking, vulnerability assessments, and defensive architecture.",
            iconKey = "cybersecurity",
            category = "Skill-based",
            level = "Beginner to Intermediate",
            duration = "8-10 months",
            totalModules = 6,
            totalProjects = 3,
            isImplemented = false
        ),
        LearningPathItem(
            id = "java-programming",
            title = "Java Programming",
            description = "Core Java syntax, JVM internals, multithreading, Spring Boot microservices, and enterprise software engineering.",
            iconKey = "java",
            category = "Skill-based",
            level = "Beginner to Intermediate",
            duration = "6-8 months",
            totalModules = 6,
            totalProjects = 4,
            isImplemented = false
        )
    )

    // -------------------------------------------------------------------------
    // 2. SIX MODULES FOR PYTHON DEVELOPER (Screen 2 & 3)
    // -------------------------------------------------------------------------
    val PYTHON_MODULES = listOf(
        RoadmapModuleData(
            id = "mod_1_basics",
            number = 1,
            title = "Python Basics & Environment",
            description = "Master interpreter setup, syntax, variables, primitive types, control flow, and loop constructs.",
            estimatedWeeks = "2-3 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_1_1",
                    moduleId = "mod_1_basics",
                    title = "Basic Syntax & Variables",
                    summary = "Understand Python's whitespace indentation, variable declaration, dynamic typing, and memory model.",
                    estimatedMinutes = 20,
                    objectives = listOf(
                        "Understand indentation and PEP 8 style guide basics",
                        "Declare variables and understand dynamic typing",
                        "Explore primitive data types: int, float, str, bool"
                    ),
                    explanation = """
Python is a dynamically-typed, interpreted language that emphasizes code readability. Instead of curly braces {} or semicolons ;, Python uses indentation to define code blocks.

Variables in Python do not require explicit type declarations. The Python interpreter infers the type of a variable at runtime based on the assigned value. Variables are references to objects in memory.
                    """.trimIndent(),
                    codeSnippet = """
# Python Variable Assignment & Types
learner_name: str = "Alex"
age = 24  # Type inferred as int
completion_rate = 0.85  # Type inferred as float
is_enrolled = True

# Formatted string output (f-strings)
greeting = f"Learner: {learner_name}, Progress: {completion_rate * 100:.1f}%"
print(greeting)

# Dynamic re-binding
value = 42
print(f"value is {type(value)}")
value = "Now a string"
print(f"value is {type(value)}")
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Python uses 4 spaces per indentation level by convention (PEP 8).",
                        "Type hints (e.g. name: str) improve readability and enable static analysis with mypy.",
                        "Everything in Python is an object, including integers and functions."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_1_2",
                    moduleId = "mod_1_basics",
                    title = "Conditionals & Match-Case",
                    summary = "Write branch logic using if-elif-else statements, boolean operators, and modern structural pattern matching.",
                    estimatedMinutes = 25,
                    objectives = listOf(
                        "Formulate conditional logic using if, elif, and else",
                        "Combine logical operators: and, or, not",
                        "Utilize Python 3.10+ match-case structural pattern matching"
                    ),
                    explanation = """
Branching logic directs program execution based on truth values. Python evaluates expressions to True or False. Values like 0, empty strings "", empty lists [], and None are considered 'falsy'.

Python 3.10 introduced structural pattern matching via match and case, offering expressive inspection of data structures beyond simple equality checks.
                    """.trimIndent(),
                    codeSnippet = """
# Traditional if-elif-else
score = 88
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"

# Python 3.10+ Structural Pattern Matching
status_code = 404

match status_code:
    case 200:
        result = "Success: OK"
    case 400 | 422:
        result = "Client Error: Invalid Payload"
    case 404:
        result = "Client Error: Resource Not Found"
    case 500:
        result = "Server Error: Internal Failure"
    case _:
        result = "Unhandled status code"

print(f"Response: {result}")
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "The ternary operator syntax is: x if condition else y.",
                        "Match-case supports wildcards (_) and guards (if condition inside case)."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_1_3",
                    moduleId = "mod_1_basics",
                    title = "Loops & Iterations",
                    summary = "Iterate cleanly with for-in loops, while loops, range generator, and break/continue control statements.",
                    estimatedMinutes = 20,
                    objectives = listOf(
                        "Iterate over sequences using for ... in",
                        "Generate sequences using range(start, stop, step)",
                        "Control execution flow with break, continue, and else blocks on loops"
                    ),
                    explanation = """
Python's for loop operates directly over iterables (lists, tuples, ranges, dictionaries) rather than indexing with a counter.

A unique feature in Python is the loop else block: code inside else runs only if the loop completes without encountering a break statement.
                    """.trimIndent(),
                    codeSnippet = """
# Iterating with range and enumerate
languages = ["Python", "Kotlin", "SQL", "Go"]

for index, lang in enumerate(languages, start=1):
    print(f"{index}. {lang}")

# Search using for-else
target = "Rust"
for lang in languages:
    if lang == target:
        print(f"Found {target}!")
        break
else:
    print(f"{target} is not in the study list.")
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Use enumerate() when both index and item are required.",
                        "The loop else clause executes when the loop finishes normally without break."
                    )
                )
            )
        ),
        RoadmapModuleData(
            id = "mod_2_data_structures",
            number = 2,
            title = "Data Structures & Collections",
            description = "Explore lists, tuples, sets, dictionaries, list comprehensions, and efficient collection algorithms.",
            estimatedWeeks = "3-4 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_2_1",
                    moduleId = "mod_2_data_structures",
                    title = "Lists & Tuples",
                    summary = "Distinguish mutable sequences (lists) from immutable records (tuples), slicing, and memory considerations.",
                    estimatedMinutes = 30,
                    objectives = listOf(
                        "Perform list operations: append, extend, insert, pop, slice",
                        "Use negative indices and extended slice syntax [start:stop:step]",
                        "Understand tuple immutability and memory optimization"
                    ),
                    explanation = """
Lists are mutable ordered sequences enclosed in brackets []. They dynamic array semantics and support O(1) appending and random access.

Tuples are immutable sequences enclosed in parentheses (). Once created, their items cannot be modified or reordered. Tuples are faster to allocate and can be used as dictionary keys when their contents are hashable.
                    """.trimIndent(),
                    codeSnippet = """
# Slicing and List manipulation
skills = ["Syntax", "Functions", "OOP", "Async", "FastAPI"]

# Slicing: [start:stop:step]
core_skills = skills[0:3]       # ['Syntax', 'Functions', 'OOP']
reversed_skills = skills[::-1]   # Reverses list

# Tuple unpacking
coords = (37.7749, -122.4194)
lat, lon = coords
print(f"Latitude: {lat}, Longitude: {lon}")
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "List slicing creates a shallow copy of the sub-sequence.",
                        "Tuples communicate fixed data structure semantics to other developers."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_2_2",
                    moduleId = "mod_2_data_structures",
                    title = "Dictionaries & Sets",
                    summary = "Leverage hash table lookups with O(1) complexity, key-value mappings, set operations, and membership tests.",
                    estimatedMinutes = 30,
                    objectives = listOf(
                        "Create dictionaries and access keys safely using .get()",
                        "Iterate keys, values, and items in dictionaries",
                        "Perform mathematical set operations: union, intersection, difference"
                    ),
                    explanation = """
Dictionaries (dict) store key-value pairs backed by a hash table, ensuring O(1) average-time lookups, insertions, and deletions. Keys must be hashable.

Sets (set) store unique, unordered elements. They are ideal for deduplication and membership testing (x in my_set), which executes in O(1) compared to O(N) for lists.
                    """.trimIndent(),
                    codeSnippet = """
# Dictionary safety and manipulation
course_info = {
    "title": "Python Developer",
    "level": "Intermediate",
    "modules": 6
}

# Safe access with default fallback
duration = course_info.get("duration", "8 weeks")

# Set operations for skill gap analysis
required_skills = {"Python", "Git", "SQL", "Docker"}
learned_skills = {"Python", "Git"}

missing_skills = required_skills - learned_skills
print(f"Missing skills: {missing_skills}")  # {'Docker', 'SQL'}
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Always prefer dict.get(key, default) over raw indexing to avoid KeyError.",
                        "Sets eliminate duplicates automatically and provide instant membership testing."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_2_3",
                    moduleId = "mod_2_data_structures",
                    title = "List & Dict Comprehensions",
                    summary = "Write concise, expressive, and performant transformations using Python comprehensions.",
                    estimatedMinutes = 25,
                    objectives = listOf(
                        "Transform data with list comprehensions: [expr for item in iterable if cond]",
                        "Construct dictionary mappings with {k: v for ...}",
                        "Avoid nested comprehension anti-patterns to maintain readability"
                    ),
                    explanation = """
Comprehensions provide a concise syntax for deriving new sequences from existing iterables. Under the hood, Python optimizes comprehensions at the bytecode level, making them faster than equivalent manual loop-append patterns.
                    """.trimIndent(),
                    codeSnippet = """
numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# List comprehension: square even numbers
even_squares = [n ** 2 for n in numbers if n % 2 == 0]
print(f"Even squares: {even_squares}")  # [4, 16, 36, 64, 100]

# Dictionary comprehension: word length mapping
topics = ["Variables", "OOP", "Async", "Decorators"]
topic_lengths = {topic: len(topic) for topic in topics}
print(topic_lengths)  # {'Variables': 9, 'OOP': 3, ...}
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Comprehensions are idiomatic and faster than manual loops.",
                        "If a comprehension exceeds two clauses, refactor to a standard loop for readability."
                    )
                )
            )
        ),
        RoadmapModuleData(
            id = "mod_3_functions_oop",
            number = 3,
            title = "Functions & Object-Oriented Programming",
            description = "Master first-class functions, args/kwargs, classes, encapsulation, inheritance, and magic methods.",
            estimatedWeeks = "3-4 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_3_1",
                    moduleId = "mod_3_functions_oop",
                    title = "Functions, Scope & *args/**kwargs",
                    summary = "Define reusable functions, manage local vs global scope (LEGB rule), and handle variable-length arguments.",
                    estimatedMinutes = 30,
                    objectives = listOf(
                        "Define functions with positional, default, and keyword arguments",
                        "Accept arbitrary positional (*args) and keyword (**kwargs) arguments",
                        "Understand Python's LEGB (Local, Enclosing, Global, Built-in) scoping rules"
                    ),
                    explanation = """
Functions in Python are first-class citizens: they can be passed as arguments, assigned to variables, and returned from other functions.

The *args syntax collects extra positional arguments into a tuple, while **kwargs collects extra keyword arguments into a dictionary.
                    """.trimIndent(),
                    codeSnippet = """
def log_learning_event(user_id: str, event_type: str, *tags: str, **metadata):
    print(f"User: {user_id} | Event: {event_type}")
    print(f"Tags: {tags}")
    print(f"Metadata: {metadata}")

log_learning_event(
    "usr_42",
    "completed_topic",
    "python", "backend", "oop",
    score=95,
    time_spent_min=24
)
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Never use a mutable object (like [] or {}) as a default parameter value.",
                        "Python searches names following the LEGB scope order."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_3_2",
                    moduleId = "mod_3_functions_oop",
                    title = "Classes, Objects & Dunder Methods",
                    summary = "Design clean object models with __init__, __repr__, __str__, and custom operator overloading.",
                    estimatedMinutes = 35,
                    objectives = listOf(
                        "Define classes with __init__ constructor",
                        "Differentiate between instance variables and class variables",
                        "Implement dunder methods like __repr__, __str__, and __eq__"
                    ),
                    explanation = """
Object-Oriented Programming allows grouping state (attributes) and behavior (methods) into reusable templates called classes.

Dunder (double underscore) methods like __repr__ and __str__ govern how objects integrate with built-in Python protocols, including string conversion and equality checks.
                    """.trimIndent(),
                    codeSnippet = """
class RoadmapMilestone:
    # Class variable
    track_name = "Python Developer"

    def __init__(self, title: str, estimated_hours: int):
        # Instance variables
        self.title = title
        self.estimated_hours = estimated_hours
        self.is_completed = False

    def complete(self):
        self.is_completed = True

    def __repr__(self) -> str:
        status = "Done" if self.is_completed else "Pending"
        return f"Milestone('{self.title}', {self.estimated_hours}h, status={status})"

m = RoadmapMilestone("OOP Fundamentals", 4)
m.complete()
print(m)  # Milestone('OOP Fundamentals', 4h, status=Done)
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "self refers to the current instance of the class.",
                        "__repr__ should return an unambiguous representation of the object."
                    )
                )
            )
        ),
        RoadmapModuleData(
            id = "mod_4_modules_files",
            number = 4,
            title = "Modules & File I/O",
            description = "Organize code across packages, read/write files safely with context managers, and handle JSON serialization.",
            estimatedWeeks = "2-3 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_4_1",
                    moduleId = "mod_4_modules_files",
                    title = "File Handling & Context Managers",
                    summary = "Open, read, and write files deterministically using the with statement and pathlib.",
                    estimatedMinutes = 25,
                    objectives = listOf(
                        "Read and write text files safely using with open(...)",
                        "Understand why context managers prevent resource leaks",
                        "Use modern pathlib.Path for cross-platform file paths"
                    ),
                    explanation = """
File handles consume operating system resources. If an exception occurs before a file is closed, the file may become corrupted or locked.

Python's with statement implements the context manager protocol (__enter__ and __exit__), guaranteeing that file descriptors are closed immediately when execution leaves the block, even on errors.
                    """.trimIndent(),
                    codeSnippet = """
from pathlib import Path
import json

# Safe cross-platform file paths
notes_dir = Path("learner_notes")
notes_dir.mkdir(exist_ok=True)
file_path = notes_dir / "progress.json"

# Writing structured JSON
data = {"completed_modules": 4, "active_roadmap": "Python Developer"}
with open(file_path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)

# Reading safely
with open(file_path, "r", encoding="utf-8") as f:
    loaded = json.load(f)
    print(f"Restored: {loaded}")
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Always specify encoding='utf-8' when reading and writing text files.",
                        "Prefer pathlib.Path over legacy os.path."
                    )
                )
            )
        ),
        RoadmapModuleData(
            id = "mod_5_advanced",
            number = 5,
            title = "Advanced Python Concepts",
            description = "Harness iterators, generator expressions, function decorators, and asyncio concurrency.",
            estimatedWeeks = "3-4 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_5_1",
                    moduleId = "mod_5_advanced",
                    title = "Decorators & Closures",
                    summary = "Wrap functions to extend behavior dynamically using the @decorator syntax and functools.wraps.",
                    estimatedMinutes = 35,
                    objectives = listOf(
                        "Understand lexical closures and inner functions",
                        "Construct custom decorators that accept function arguments",
                        "Preserve function metadata using @functools.wraps"
                    ),
                    explanation = """
A decorator is a callable that takes a function as input and returns an enhanced function. Decorators are widely used in frameworks for authentication, timing, caching, and routing.
                    """.trimIndent(),
                    codeSnippet = """
import time
from functools import wraps

def timing_decorator(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        duration = time.perf_counter() - start
        print(f"[{func.__name__}] Executed in {duration * 1000:.2f}ms")
        return result
    return wrapper

@timing_decorator
def process_data(records: int):
    return sum(i * 2 for i in range(records))

result = process_data(100_000)
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Use @wraps to preserve docstrings and original function names.",
                        "Decorators execute once at function definition time."
                    )
                ),
                RoadmapLessonTopic(
                    id = "py_topic_5_2",
                    moduleId = "mod_5_advanced",
                    title = "Generators & Iterators",
                    summary = "Produce streaming memory-efficient sequences using yield and generator expressions.",
                    estimatedMinutes = 30,
                    objectives = listOf(
                        "Define generator functions using the yield keyword",
                        "Understand state retention and lazy evaluation",
                        "Process massive datasets without loading all items into RAM"
                    ),
                    explanation = """
Generators are functions that yield values one at a time, pausing their execution state between requests. Instead of computing an entire list and storing it in memory, generators compute elements on demand (lazy evaluation).
                    """.trimIndent(),
                    codeSnippet = """
# Generator that generates an infinite fibonacci stream
def fibonacci_stream():
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b

# Consume first 8 numbers lazily
fib = fibonacci_stream()
first_eight = [next(fib) for _ in range(8)]
print(f"Fibonacci: {first_eight}")  # [0, 1, 1, 2, 3, 5, 8, 13]
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "Generators have O(1) memory footprint regardless of dataset size.",
                        "Once a generator is exhausted, calling next() raises StopIteration."
                    )
                )
            )
        ),
        RoadmapModuleData(
            id = "mod_6_web_fastapi",
            number = 6,
            title = "Web Development & APIs with FastAPI",
            description = "Build high-performance REST APIs, define Pydantic schemas, handle dependency injection, and write async routes.",
            estimatedWeeks = "4 weeks",
            topics = listOf(
                RoadmapLessonTopic(
                    id = "py_topic_6_1",
                    moduleId = "mod_6_web_fastapi",
                    title = "FastAPI & Pydantic Data Validation",
                    summary = "Create REST endpoints, request/response models with Pydantic, and automatic OpenAPI documentation.",
                    estimatedMinutes = 35,
                    objectives = listOf(
                        "Instantiate a FastAPI application and declare route decorators",
                        "Validate request bodies with Pydantic BaseModel",
                        "Leverage automatic Swagger/OpenAPI documentation"
                    ),
                    explanation = """
FastAPI is a modern, high-performance web framework for building APIs with Python 3.8+ based on standard Python type hints. It runs on Starlette for web routing and Pydantic for data validation, achieving performance on par with NodeJS and Go.
                    """.trimIndent(),
                    codeSnippet = """
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="EduNova Learning API")

class TopicCreate(BaseModel):
    title: str = Field(min_length=3, max_length=100)
    estimated_minutes: int = Field(ge=5, le=180)

@app.post("/api/v1/topics", status_code=201)
async def create_topic(topic: TopicCreate):
    return {
        "status": "created",
        "topic": topic.model_dump(),
        "verified": True
    }
                    """.trimIndent(),
                    keyTakeaways = listOf(
                        "FastAPI generates interactive Swagger docs automatically at /docs.",
                        "Pydantic provides runtime validation and clean error messages."
                    )
                )
            )
        )
    )

    // -------------------------------------------------------------------------
    // 3. SIX LOCAL STAGING PROJECTS (Section 4 & Section 7)
    // -------------------------------------------------------------------------
    val PROJECTS = listOf(
        RoadmapProjectItem(
            id = "proj_1_cli_expense",
            title = "CLI Expense Tracker",
            difficulty = "Beginner",
            estimatedHours = 12,
            description = "Build an interactive command-line personal finance tool that records expenses, categorizes spending, and persists records to local JSON files.",
            relevantSkills = listOf("Python Syntax", "Control Flow", "File I/O", "JSON Serialization", "Input Validation"),
            deliverables = listOf(
                "Command-line menu (Add, View, Filter by Category, Delete)",
                "Local JSON data persistence with pathlib",
                "Formatted ASCII summary report with category breakdowns",
                "Defensive input validation for amounts and dates"
            ),
            instructions = """
1. Implement an Expense class storing id, description, amount, category, and date.
2. Build an ExpenseManager class handling add, delete, list, and summary calculations.
3. Use pathlib and json modules to persist records to 'expenses.json'.
4. Handle edge cases like empty records and corrupt JSON gracefully.
            """.trimIndent()
        ),
        RoadmapProjectItem(
            id = "proj_2_file_organizer",
            title = "File Organizer",
            difficulty = "Beginner",
            estimatedHours = 14,
            description = "Create an automated desktop file organizer script that sorts files in a target directory by type (images, documents, archives, videos) into organized subfolders.",
            relevantSkills = listOf("pathlib", "shutil", "File Metadata", "Automated Schedulers", "OS Interaction"),
            deliverables = listOf(
                "Automated directory scanner identifying extensions",
                "Safe moving with conflict collision resolution",
                "Summary log file documenting all organized items",
                "Dry-run preview mode before moving files"
            ),
            instructions = """
1. Scan source directory using pathlib.Path.iterdir().
2. Group files based on extensions into predefined category folders.
3. Use shutil.move() to safely move files without overwriting existing files.
4. Output execution audit log with timestamps and count of sorted files.
            """.trimIndent()
        ),
        RoadmapProjectItem(
            id = "proj_3_web_scraper",
            title = "Web Scraper and Analyzer",
            difficulty = "Intermediate",
            estimatedHours = 18,
            description = "Develop an automated data collection pipeline that fetches job listings or tech articles, parses HTML, cleans text, and exports statistical summaries.",
            relevantSkills = listOf("HTTP Requests", "BeautifulSoup4 / Selectors", "Data Cleaning", "CSV Export", "Error Retries"),
            deliverables = listOf(
                "Robust HTTP scraper with rate-limiting and User-Agent headers",
                "HTML parsing extracting titles, salaries, and required skills",
                "CSV exporter preserving structured tabular data",
                "Top 10 in-demand skills statistical frequency report"
            ),
            instructions = """
1. Use httpx or requests to download target web pages.
2. Parse HTML nodes using BeautifulSoup or regex pattern extraction.
3. Implement polite scraping: 1-second delay between requests and exponential backoff.
4. Clean and normalize text: lowercase skill names, trim whitespace.
5. Export clean datasets to CSV and print aggregate frequency statistics.
            """.trimIndent()
        ),
        RoadmapProjectItem(
            id = "proj_4_fastapi_rest",
            title = "FastAPI REST API",
            difficulty = "Intermediate",
            estimatedHours = 24,
            description = "Create a production-ready asynchronous RESTful microservice with authentication, relational SQLite storage, Pydantic validation, and complete test suite.",
            relevantSkills = listOf("FastAPI", "AsyncIO", "Pydantic v2", "SQLAlchemy / SQLite", "JWT Auth", "Pytest"),
            deliverables = listOf(
                "CRUD endpoints for roadmap topics and user progress",
                "JWT authentication middleware protecting private endpoints",
                "Pydantic schemas with custom field validators",
                "100% passing Pytest integration test suite with TestClient"
            ),
            instructions = """
1. Configure FastAPI app with CORS and custom error handlers.
2. Define SQLAlchemy models: User and RoadmapItem.
3. Implement password hashing with bcrypt and dual-token JWT rotation.
4. Create dependency injection functions for database sessions.
5. Write unit tests testing success paths and 401/422 validation error cases.
            """.trimIndent()
        ),
        RoadmapProjectItem(
            id = "proj_5_db_app",
            title = "Database-Connected Application",
            difficulty = "Intermediate",
            estimatedHours = 28,
            description = "Build a full-featured data management application connected to a relational database (SQLite/PostgreSQL) with migrations, transactions, and indexing.",
            relevantSkills = listOf("SQLAlchemy ORM", "Alembic Migrations", "ACID Transactions", "Foreign Keys & Relationships", "Query Optimization"),
            deliverables = listOf(
                "Relational schema with One-to-Many and Many-to-Many relationships",
                "Alembic migration scripts for versioned database schema upgrades",
                "Transaction-safe repository pattern isolating business logic",
                "Comprehensive integration tests verifying rollback on error"
            ),
            instructions = """
1. Design relational schema with Users, Courses, and Progress tables.
2. Initialize and apply Alembic migrations.
3. Implement session management and atomic commit/rollback mechanisms.
4. Write complex SQL queries with joins, grouping, and aggregations.
            """.trimIndent()
        ),
        RoadmapProjectItem(
            id = "proj_6_capstone",
            title = "Full Python Capstone Project",
            difficulty = "Advanced",
            estimatedHours = 40,
            description = "Synthesize all roadmap capabilities to create an offline-capable learning companion with background task execution, database caching, and AI integration.",
            relevantSkills = listOf("Clean Architecture", "Concurrency", "Database Optimization", "AI API Integration", "Packaging"),
            deliverables = listOf(
                "Multi-tier modular application architecture",
                "Local cache layer with SQLite fallback",
                "Asynchronous background worker queue",
                "Interactive CLI or Web UI with streaming AI explanations",
                "Complete README, architecture documentation, and test suite"
            ),
            instructions = """
1. Architect the system using clean separation: Presentation, Domain, Data layers.
2. Implement offline caching that operates seamlessly when disconnected.
3. Integrate OpenRouter or on-device LLM inference for code tutor assistance.
4. Add performance telemetry measuring execution latency.
5. Package the project with pyproject.toml and automated GitHub Actions workflow.
            """.trimIndent()
        )
    )

    // -------------------------------------------------------------------------
    // 4. SIX FREQUENTLY ASKED QUESTIONS (Section 9)
    // -------------------------------------------------------------------------
    val FAQS = listOf(
        RoadmapFaqItem(
            id = "faq_1",
            question = "What is a Python developer?",
            answer = "A Python developer designs, writes, debugs, and deploys applications using the Python programming language. Depending on the specialization, Python developers build web backend APIs (FastAPI, Django), data science and machine learning pipelines (Pandas, PyTorch), automated system tools and scrapers, or enterprise microservices."
        ),
        RoadmapFaqItem(
            id = "faq_2",
            question = "What should I learn before Python?",
            answer = "No prior programming experience is required before starting Python. Basic computer literacy—such as navigating terminal/command prompt, understanding file directories, and basic arithmetic logic—is sufficient. Python's clear, readable syntax makes it the ideal first language for newcomers."
        ),
        RoadmapFaqItem(
            id = "faq_3",
            question = "How long does it take to learn Python?",
            answer = "For beginners studying 8-10 hours per week, mastering Python fundamentals (syntax, loops, data structures, functions) typically takes 6 to 8 weeks. Achieving job-readiness—including OOP, file I/O, web frameworks like FastAPI, databases, and building full portfolio projects—takes approximately 6 to 8 months of consistent hands-on practice."
        ),
        RoadmapFaqItem(
            id = "faq_4",
            question = "What projects can I build with Python?",
            answer = "Python is exceptionally versatile. You can build:\n• Web APIs and Backends (FastAPI, Django, Flask)\n• Automation Scripts & File Organizers (pathlib, shutil)\n• Web Scrapers & Data Analyzers (BeautifulSoup, httpx)\n• Database Applications (SQLite, PostgreSQL, SQLAlchemy)\n• Machine Learning & AI Agents (PyTorch, Scikit-learn)\n• Command-Line Utilities (Typer, Click, Rich)"
        ),
        RoadmapFaqItem(
            id = "faq_5",
            question = "How should I practice Python?",
            answer = "1. Write code daily rather than only watching video tutorials ('tutorial hell').\n2. Build small, concrete personal projects that solve real problems (e.g. CLI expense tracker, automated file sorter).\n3. Solve 1-2 coding challenges on platforms like LeetCode or Codewars to internalize data structures.\n4. Read open-source code and learn to debug by reading error tracebacks carefully."
        ),
        RoadmapFaqItem(
            id = "faq_6",
            question = "What should I learn after Python basics?",
            answer = "Once comfortable with variables, loops, and functions, proceed through:\n1. Object-Oriented Programming (Classes, inheritance, encapsulation)\n2. Virtual environments (venv, uv, poetry) and package management (pip)\n3. Relational databases & SQL (PostgreSQL, SQLite, SQLAlchemy)\n4. Web frameworks (FastAPI or Django) for backend engineering\n5. Concurrency (asyncio) and Automated testing (pytest)."
        )
    )

    val PYTHON_PROJECTS: List<RoadmapProjectItem> get() = PROJECTS
    val PYTHON_FAQS: List<RoadmapFaqItem> get() = FAQS

    // -------------------------------------------------------------------------
    // 5. VISUAL ROADMAP CONNECTED NODES (Roadmap.sh Interactive Diagram)
    // -------------------------------------------------------------------------

    // Milestone 1: Learn the Basics
    val BASICS_CENTER_NODE = VisualRoadmapNode(
        id = "basics_center",
        title = "Learn the Basics",
        section = "Python Fundamentals",
        isMajor = true,
        badge = "Milestone 1",
        explanation = "The foundational building blocks of Python programming. Master variables, control flow, functions, and standard data structures before moving forward.",
        objectives = listOf(
            "Install Python 3 and configure your IDE environment",
            "Understand variables, dynamic typing, and operator semantics",
            "Control program execution flow using conditionals and loops",
            "Write modular functions with type hints and defensive error handling"
        ),
        codeSnippet = """# Welcome to Python!
def greet_learner(name: str) -> str:
    return f"Welcome {name} to the Python Roadmap!"

print(greet_learner("EduNova Developer"))""".trimIndent()
    )

    // Fundamentals Nodes (Exact Roadmap.sh Layout)
    val NODE_SYNTAX = VisualRoadmapNode(
        id = "node_syntax",
        title = "Basic Syntax",
        section = "Python Fundamentals",
        objectives = listOf("Indentation as code blocks", "Colons in statements", "Case-sensitive identifiers"),
        explanation = "Python uses indentation (whitespace) instead of curly braces to define blocks.",
        codeSnippet = """if score >= 90:
    print('Grade: A')
else:
    print('Keep practicing!')""".trimIndent()
    )

    val NODE_VARIABLES = VisualRoadmapNode(
        id = "node_variables",
        title = "Variables and Data Types",
        section = "Python Fundamentals",
        objectives = listOf("int, float, bool, str", "Dynamic typing", "type() introspection"),
        explanation = "Variables reference objects in memory without manual type declarations.",
        codeSnippet = """name = 'Pythonista'
year = 2026
is_active = True
rating = 4.95""".trimIndent()
    )

    val NODE_OPERATORS = VisualRoadmapNode(
        id = "node_operators",
        title = "Operators",
        section = "Python Fundamentals",
        objectives = listOf("Arithmetic (+, -, *, //, **)", "Comparison (==, !=, <, >)", "Logical (and, or, not)"),
        explanation = "Standard arithmetic, comparison, identity (is), and membership (in) operators.",
        codeSnippet = """x = 10
y = 3
print(x // y)  # Floor division: 3
print(x ** y)  # Exponent: 1000""".trimIndent()
    )

    val NODE_OPERATORS_AT = VisualRoadmapNode(
        id = "node_operators_at",
        title = "@",
        section = "Python Fundamentals",
        objectives = listOf("Decorator syntax prefix @", "Matrix multiplication operator @ in Python 3.5+", "numpy @ operator"),
        explanation = "In Python, @ prefixes decorator annotations and functions as matrix multiplication.",
        codeSnippet = """@my_decorator
def calculate():
    pass
# A @ B  # Matrix multiplication""".trimIndent()
    )

    val NODE_STRINGS = VisualRoadmapNode(
        id = "node_strings",
        title = "Working with Strings",
        section = "Python Fundamentals",
        objectives = listOf("f-strings formatting", "Slicing [start:stop:step]", "Methods (strip, split, replace)"),
        explanation = "Strings are immutable sequences with rich slicing and formatting capabilities.",
        codeSnippet = """msg = 'python developer'
print(msg.title())  # 'Python Developer'
print(f'Length: {len(msg)}')""".trimIndent()
    )

    val NODE_CONDITIONALS = VisualRoadmapNode(
        id = "node_conditionals",
        title = "Conditionals",
        section = "Python Fundamentals",
        objectives = listOf("if, elif, else branches", "Ternary expressions", "Structural pattern matching"),
        explanation = "Control execution branches based on truthy and falsy boolean evaluations.",
        codeSnippet = """status = 200
message = 'Success' if status == 200 else 'Failed'
print(message)""".trimIndent()
    )

    val NODE_LOOPS = VisualRoadmapNode(
        id = "node_loops",
        title = "Loops",
        section = "Python Fundamentals",
        objectives = listOf("for loops with range()", "while condition loops", "break and continue"),
        explanation = "Iterate over collections or repeat actions until conditions are satisfied.",
        codeSnippet = """for i in range(1, 4):
    print(f'Attempt {i}')""".trimIndent()
    )

    val NODE_LISTS = VisualRoadmapNode(
        id = "node_lists",
        title = "Lists",
        section = "Python Fundamentals",
        objectives = listOf("Ordered mutable sequence", "append(), pop(), extend()", "Negative index slicing"),
        explanation = "Dynamic arrays holding heterogeneous elements with O(1) amortized append.",
        codeSnippet = """items = ['fastapi', 'pytest', 'uv']
items.append('docker')
print(items[0])""".trimIndent()
    )

    val NODE_TUPLES = VisualRoadmapNode(
        id = "node_tuples",
        title = "Tuples",
        section = "Python Fundamentals",
        objectives = listOf("Immutable sequences", "Tuple unpacking", "Hashable dictionary keys"),
        explanation = "Lightweight immutable sequences ideal for fixed records and coordinate pairs.",
        codeSnippet = """point = (10, 20)
x, y = point
print(f'X={x}, Y={y}')""".trimIndent()
    )

    val NODE_SETS = VisualRoadmapNode(
        id = "node_sets",
        title = "Sets",
        section = "Python Fundamentals",
        objectives = listOf("Unique elements", "Union, intersection, difference", "O(1) average lookup"),
        explanation = "Unordered collections of unique hashable elements with fast membership checks.",
        codeSnippet = """tags = {'python', 'compose', 'kotlin'}
tags.add('python')""".trimIndent()
    )

    val NODE_DICTS = VisualRoadmapNode(
        id = "node_dicts",
        title = "Dictionaries",
        section = "Python Fundamentals",
        objectives = listOf("Key-value hash map", "get() with default fallback", "dict.items() iteration"),
        explanation = "Key-value hash table providing O(1) average key lookup and update.",
        codeSnippet = """user = {'id': 101, 'role': 'Admin'}
print(user.get('role', 'Member'))""".trimIndent()
    )

    val NODE_CASTING = VisualRoadmapNode(
        id = "node_casting",
        title = "Type Casting",
        section = "Python Fundamentals",
        objectives = listOf("Explicit int(), float(), str()", "Implicit promotion", "ValueError handling"),
        explanation = "Convert data safely between primitive types.",
        codeSnippet = """raw_count = '42'
count = int(raw_count)
price = float('19.99')""".trimIndent()
    )

    val NODE_FUNCTIONS = VisualRoadmapNode(
        id = "node_functions",
        title = "Functions, Builtin Functions",
        section = "Python Fundamentals",
        objectives = listOf("def statements", "*args and **kwargs", "Built-ins (len, sum, min, max, zip, enumerate)"),
        explanation = "Modular reusable code blocks supporting flexible argument passing and standard built-ins.",
        codeSnippet = """def calculate_total(price: float, tax: float = 0.08) -> float:
    return round(price * (1 + tax), 2)""".trimIndent()
    )

    val NODE_EXCEPTIONS = VisualRoadmapNode(
        id = "node_exceptions",
        title = "Exceptions",
        section = "Python Fundamentals",
        objectives = listOf("try, except, else, finally", "raise custom exceptions", "Exception hierarchy"),
        explanation = "Graceful error recovery preventing unhandled crashes.",
        codeSnippet = """try:
    result = 10 / 2
except ZeroDivisionError:
    result = 0
finally:
    print('Executed')""".trimIndent()
    )

    val NODE_COMMENTS = VisualRoadmapNode(
        id = "node_comments",
        title = "Comments",
        section = "Python Fundamentals",
        objectives = listOf("# Single line comments", "PEP 257 docstring conventions", "help() introspection"),
        explanation = "Clear in-code documentation inspectable at runtime.",
        codeSnippet = """def add(a: int, b: int) -> int:
    '''Add two integers and return the sum.'''
    return a + b""".trimIndent()
    )

    val NODE_SCOPE_ANNOT = VisualRoadmapNode(
        id = "node_scope_annot",
        title = "Type Annot.",
        section = "Python Fundamentals",
        objectives = listOf("Type hints (int, str, list[str])", "Union and Optional", "LEGB scope rule"),
        explanation = "Document expected parameter and return types for clarity and static analysis.",
        codeSnippet = """def find_user(user_id: int) -> str | None:
    return f'User_{user_id}' if user_id > 0 else None""".trimIndent()
    )

    // Milestone 2: Data Structures & Algorithms
    val DSA_CENTER_NODE = VisualRoadmapNode(
        id = "dsa_center",
        title = "Data Structures & Algorithms",
        section = "Data Structures & Algorithms",
        isMajor = true,
        badge = "Milestone 2",
        explanation = "Master fundamental data structures and algorithmic complexity to write scalable, high-performance Python code.",
        objectives = listOf(
            "Understand memory layouts for lists, hash tables, and linked nodes",
            "Implement stacks, queues, heaps, and binary search trees",
            "Analyze Big-O time and space complexity",
            "Implement recursion, sorting, and searching patterns"
        ),
        codeSnippet = """# Binary Search O(log n)
def binary_search(arr: list[int], target: int) -> int:
    left, right = 0, len(arr) - 1
    while left <= right:
        mid = (left + right) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    return -1""".trimIndent()
    )

    val NODE_ARRAYS_LISTS = VisualRoadmapNode(
        id = "node_arrays_lists",
        title = "Arrays and Linked Lists",
        section = "Data Structures & Algorithms",
        objectives = listOf("Dynamic array reallocation", "Contiguous memory layout", "Node pointer chain"),
        explanation = "Python lists are dynamic arrays; linked lists link independent nodes via references.",
        codeSnippet = """class Node:
    def __init__(self, val, next=None):
        self.val = val
        self.next = next""".trimIndent()
    )

    val NODE_HASH_MAPS = VisualRoadmapNode(
        id = "node_hash_maps",
        title = "HashMaps",
        section = "Data Structures & Algorithms",
        objectives = listOf("Hash functions & collisions", "collections.defaultdict", "collections.Counter"),
        explanation = "Hash tables map keys to values with O(1) average lookup and insertion.",
        codeSnippet = """from collections import Counter
counts = Counter('edunova python')
print(counts['o'])""".trimIndent()
    )

    val NODE_STACKS_QUEUES = VisualRoadmapNode(
        id = "node_stacks_queues",
        title = "Heaps, Stacks and Queues",
        section = "Data Structures & Algorithms",
        objectives = listOf("LIFO stack pattern", "FIFO queue pattern with deque", "Min-heap via heapq"),
        explanation = "Ordered collections and priority queues essential for algorithmic problems.",
        codeSnippet = """from collections import deque
import heapq
q = deque(['a', 'b'])
q.append('c')
first = q.popleft()""".trimIndent()
    )

    val NODE_BST = VisualRoadmapNode(
        id = "node_bst",
        title = "Binary Search Tree",
        section = "Data Structures & Algorithms",
        objectives = listOf("BST properties", "In-order, pre-order, post-order traversal", "O(log n) search"),
        explanation = "Tree structures where left child < node <= right child for efficient lookups.",
        codeSnippet = """class TreeNode:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None""".trimIndent()
    )

    val NODE_RECURSION = VisualRoadmapNode(
        id = "node_recursion",
        title = "Recursion",
        section = "Data Structures & Algorithms",
        objectives = listOf("Base cases and stack frames", "Call stack limits", "Memoization patterns"),
        explanation = "Solve problems by having functions call themselves on reduced subproblems.",
        codeSnippet = """def fib(n: int) -> int:
    return n if n <= 1 else fib(n-1) + fib(n-2)""".trimIndent()
    )

    val NODE_SORTING_SEARCHING = VisualRoadmapNode(
        id = "node_sorting_searching",
        title = "Sorting Algorithms",
        section = "Data Structures & Algorithms",
        objectives = listOf("Timsort in sorted()", "QuickSort & MergeSort concepts", "Custom key lambdas"),
        explanation = "Efficient sorting and searching patterns to organize datasets.",
        codeSnippet = """data = [{'val': 5}, {'val': 2}]
sorted_data = sorted(data, key=lambda x: x['val'])""".trimIndent()
    )

    // Package Managers Cluster (Left of DSA)
    val NODE_PYPI = VisualRoadmapNode(id = "node_pypi", title = "PyPI", section = "Package Management", objectives = listOf("Python Package Index", "Publishing packages"), explanation = "Official third-party software repository for Python.", codeSnippet = "# https://pypi.org")
    val NODE_PIP = VisualRoadmapNode(id = "node_pip", title = "Pip", section = "Package Management", objectives = listOf("pip install", "requirements.txt pinning"), explanation = "Standard tool for installing Python packages.", codeSnippet = "pip install fastapi")
    val NODE_CONDA = VisualRoadmapNode(id = "node_conda", title = "Conda", section = "Package Management", objectives = listOf("Binary package management", "Environment creation"), explanation = "Cross-platform package and environment manager popular in data science.", codeSnippet = "conda create -n env python=3.12")
    val NODE_UV = VisualRoadmapNode(id = "node_uv", title = "uv", section = "Package Management", objectives = listOf("Rust-based package manager", "10-100x faster than pip"), explanation = "Extremely fast modern Python package installer written in Rust.", codeSnippet = "uv pip install httpx")
    val NODE_POETRY = VisualRoadmapNode(id = "node_poetry", title = "Poetry", section = "Package Management", objectives = listOf("Deterministic poetry.lock", "Dependency management"), explanation = "Comprehensive packaging and dependency management tool.", codeSnippet = "poetry add requests")
    val NODE_PDM = VisualRoadmapNode(id = "node_pdm", title = "pdm", section = "Package Management", objectives = listOf("PEP 582 support", "pyproject.toml native"), explanation = "Modern Python package manager supporting latest PEP standards.", codeSnippet = "pdm add fastapi")
    val NODE_PKG_MANAGERS = VisualRoadmapNode(id = "node_pkg_managers", title = "Package Managers", section = "Package Management", isMajor = true, objectives = listOf("Resolving dependencies", "Lockfiles", "Environment isolation"), explanation = "Tools for organizing, installing, and locking project dependencies.", codeSnippet = "pip freeze > requirements.txt")

    // Advanced Concepts Cluster (Right of DSA)
    val NODE_BUILTIN = VisualRoadmapNode(id = "node_builtin", title = "Builtin", section = "Advanced Python", objectives = listOf("sys, os, math modules", "Standard library"), explanation = "Python's extensive built-in module catalog.", codeSnippet = "import sys, os")
    val NODE_CUSTOM = VisualRoadmapNode(id = "node_custom", title = "Custom", section = "Advanced Python", objectives = listOf("Writing custom modules", "__all__ export list"), explanation = "Creating user-defined modules and packages.", codeSnippet = "from my_module import calculate")
    val NODE_MODULES = VisualRoadmapNode(id = "node_modules", title = "Modules", section = "Advanced Python", isMajor = true, objectives = listOf("Module namespaces", "import mechanics"), explanation = "Python files containing definitions and statements.", codeSnippet = "import math as m")
    val NODE_ERROR_HANDLING = VisualRoadmapNode(id = "node_error_handling", title = "Error Handling", section = "Advanced Python", isMajor = true, objectives = listOf("Custom exception hierarchy", "try/except chains"), explanation = "Robust error recovery mechanisms.", codeSnippet = "class CustomError(Exception): pass")
    val NODE_LAMBDAS = VisualRoadmapNode(id = "node_lambdas", title = "Lambdas", section = "Advanced Python", isMajor = true, objectives = listOf("Anonymous functions", "Short transformations"), explanation = "Small anonymous functions defined with lambda keyword.", codeSnippet = "square = lambda x: x * x")
    val NODE_DECORATORS = VisualRoadmapNode(id = "node_decorators", title = "Decorators", section = "Advanced Python", isMajor = true, objectives = listOf("Function decorators", "functools.wraps"), explanation = "Wrap functions to dynamically modify their behavior.", codeSnippet = "@timed_execution; def process(): pass")
    val NODE_ITERATORS = VisualRoadmapNode(id = "node_iterators", title = "Iterators", section = "Advanced Python", isMajor = true, objectives = listOf("__iter__ and __next__ protocols", "StopIteration"), explanation = "Objects representing a stream of data that can be iterated over.", codeSnippet = "it = iter([1, 2, 3]); next(it)")
    val NODE_REGEX = VisualRoadmapNode(id = "node_regex", title = "Regular Expressions", section = "Advanced Python", isMajor = true, objectives = listOf("re module", "Pattern matching and extraction"), explanation = "Powerful text matching and parsing engine.", codeSnippet = "import re; match = re.search(r'\\\\d+', 'Age: 25')")
    val NODE_VARIABLE_SCOPE = VisualRoadmapNode(id = "node_variable_scope", title = "Variable Scope", section = "Advanced Python", isMajor = true, objectives = listOf("global and nonlocal keywords", "LEGB rule"), explanation = "Rules determining the visibility and lifetime of variables.", codeSnippet = "nonlocal count")

    // Milestone 3: Object-Oriented Programming
    val OOP_CENTER_NODE = VisualRoadmapNode(
        id = "oop_center",
        title = "Object Oriented Programming",
        section = "Object-Oriented Programming",
        isMajor = true,
        badge = "Milestone 3",
        explanation = "Design clean, reusable, maintainable architectures using object-oriented principles, encapsulation, and magic dunder protocols.",
        objectives = listOf(
            "Model domain concepts with classes, attributes, and methods",
            "Leverage inheritance and polymorphism to eliminate duplication",
            "Enforce encapsulation with private fields and property decorators",
            "Harness magic methods (__repr__, __eq__, __len__) for native integration"
        ),
        codeSnippet = """class Course:
    def __init__(self, title: str):
        self.title = title

    def __repr__(self) -> str:
        return f"Course({self.title})"

py = Course("Python Developer")
print(py)""".trimIndent()
    )

    val NODE_CLASSES = VisualRoadmapNode(id = "node_classes", title = "Classes", section = "Object-Oriented Programming", objectives = listOf("class blueprint", "Instantiation"), explanation = "User-defined blueprints for objects.", codeSnippet = "class User: pass")
    val NODE_METHODS = VisualRoadmapNode(id = "node_methods", title = "Methods", section = "Object-Oriented Programming", objectives = listOf("Instance methods", "@classmethod, @staticmethod"), explanation = "Functions defined inside class bodies.", codeSnippet = "def run(self): pass")
    val NODE_INHERITANCE = VisualRoadmapNode(id = "node_inheritance", title = "Inheritance", section = "Object-Oriented Programming", objectives = listOf("Subclassing", "super() calls"), explanation = "Deriving specialized child classes from parent classes.", codeSnippet = "class Dog(Animal): pass")
    val NODE_ENCAPSULATION = VisualRoadmapNode(id = "node_encapsulation", title = "Encapsulation", section = "Object-Oriented Programming", objectives = listOf("Private variables _ and __", "@property getters/setters"), explanation = "Restricting direct access to object internals.", codeSnippet = "@property; def name(self): return self._name")

    // Left of OOP
    val NODE_COMMON_PACKAGES = VisualRoadmapNode(id = "node_common_packages", title = "Common Packages", section = "Package Management", isMajor = true, objectives = listOf("requests, httpx, pydantic", "rich, click"), explanation = "Essential libraries widely adopted across Python projects.", codeSnippet = "import httpx, pydantic")
    val NODE_PYPROJECT = VisualRoadmapNode(id = "node_pyproject", title = "pyproject.toml", section = "Package Management", objectives = listOf("PEP 518/621 standard", "Tool configuration"), explanation = "Unified configuration file for modern Python development.", codeSnippet = "[project]; name = 'app'")
    val NODE_COMPREHENSIONS = VisualRoadmapNode(id = "node_comprehensions", title = "List Comprehensions", section = "Advanced Python", isMajor = true, objectives = listOf("List, set, dict comprehensions", "Filtering"), explanation = "Concise syntax for creating sequences.", codeSnippet = "squares = [x*x for x in range(5)]")
    val NODE_GENERATOR_EXPRESSIONS = VisualRoadmapNode(id = "node_generator_expressions", title = "Generator Expressions", section = "Advanced Python", isMajor = true, objectives = listOf("Lazy evaluation", "Memory efficiency"), explanation = "On-demand stream generation saving memory.", codeSnippet = "gen = (x*x for x in range(100))")
    val NODE_PARADIGMS = VisualRoadmapNode(id = "node_paradigms", title = "Paradigms", section = "Advanced Python", isMajor = true, objectives = listOf("Functional vs OOP", "Imperative programming"), explanation = "Python supports multiple programming styles seamlessly.", codeSnippet = "# Functional: map, filter, reduce")
    val NODE_CONTEXT_MANAGERS = VisualRoadmapNode(id = "node_context_managers", title = "Context Manager", section = "Advanced Python", isMajor = true, objectives = listOf("with statement", "__enter__ and __exit__"), explanation = "Safe resource lifecycle management.", codeSnippet = "with open('f.txt') as f: data = f.read()")

    // Right of OOP
    val NODE_PIPENV = VisualRoadmapNode(id = "node_pipenv", title = "Pipenv", section = "Package Management", objectives = listOf("Pipfile & Pipfile.lock", "Automatic venv"), explanation = "Tool bringing virtualenv and pip together.", codeSnippet = "pipenv install requests")
    val NODE_VIRTUALENV = VisualRoadmapNode(id = "node_virtualenv", title = "virtualenv", section = "Package Management", objectives = listOf("Isolated environments", "python -m venv"), explanation = "Dependency isolation across projects.", codeSnippet = "python3 -m venv .venv")
    val NODE_PYENV = VisualRoadmapNode(id = "node_pyenv", title = "pyenv", section = "Package Management", objectives = listOf("Multiple Python versions", "Global and local versions"), explanation = "Simple Python version management.", codeSnippet = "pyenv install 3.12.0")
    val NODE_STATIC_TYPING = VisualRoadmapNode(id = "node_static_typing", title = "Static Typing", section = "Code Quality", isMajor = true, objectives = listOf("Type hints", "Type checkers"), explanation = "Optional static type analysis for large codebases.", codeSnippet = "from typing import Sequence")
    val NODE_TYPING = VisualRoadmapNode(id = "node_typing", title = "typing", section = "Code Quality", objectives = listOf("Union, Optional, Any", "TypeVar, Generic"), explanation = "Standard library module for type hints.", codeSnippet = "from typing import Optional")
    val NODE_MYPY = VisualRoadmapNode(id = "node_mypy", title = "mypy", section = "Code Quality", objectives = listOf("Static type checker", "Strict mode"), explanation = "Compile-time type analysis catching bugs before execution.", codeSnippet = "mypy app/ --strict")
    val NODE_PYRIGHT = VisualRoadmapNode(id = "node_pyright", title = "pyright", section = "Code Quality", objectives = listOf("Fast type checker", "VS Code Pylance engine"), explanation = "High-speed static type checker from Microsoft.", codeSnippet = "pyright src/")
    val NODE_PYRE = VisualRoadmapNode(id = "node_pyre", title = "pyre", section = "Code Quality", objectives = listOf("High-performance type analysis", "Meta open source"), explanation = "Performant type checker designed for large Python repositories.", codeSnippet = "pyre check")
    val NODE_PYDANTIC = VisualRoadmapNode(id = "node_pydantic", title = "Pydantic", section = "Web Development", objectives = listOf("Runtime validation", "BaseModel schemas"), explanation = "Data validation using Python type annotations.", codeSnippet = "class User(BaseModel): name: str")

    // Milestone 4: Concurrency
    val CONCURRENCY_CENTER_NODE = VisualRoadmapNode(
        id = "concurrency_center",
        title = "Concurrency",
        section = "Concurrency",
        isMajor = true,
        badge = "Milestone 4",
        explanation = "Master asynchronous programming, threading, multiprocessing, and event loop management for high-throughput applications.",
        objectives = listOf(
            "Differentiate I/O-bound vs CPU-bound concurrency",
            "Write non-blocking code with async, await, and asyncio",
            "Utilize threading for I/O operations and multiprocessing for CPU compute",
            "Understand the Global Interpreter Lock (GIL) and free-threaded Python"
        ),
        codeSnippet = """import asyncio

async def fetch_topic(id: int) -> str:
    await asyncio.sleep(0.1)
    return f"Topic_{id}"

async def main():
    results = await asyncio.gather(fetch_topic(1), fetch_topic(2))
    print(results)

asyncio.run(main())""".trimIndent()
    )

    val NODE_MULTIPROCESSING = VisualRoadmapNode(id = "node_multiprocessing", title = "Multiprocessing", section = "Concurrency", objectives = listOf("Process Pool", "Bypassing GIL for CPU tasks"), explanation = "Spawns separate interpreter processes across CPU cores.", codeSnippet = "from multiprocessing import Pool")
    val NODE_ASYNCHRONY = VisualRoadmapNode(id = "node_asynchrony", title = "Asynchrony", section = "Concurrency", objectives = listOf("async & await", "Event loops"), explanation = "Single-threaded cooperative multitasking for network I/O.", codeSnippet = "async def query(): pass")
    val NODE_GIL = VisualRoadmapNode(id = "node_gil", title = "GIL", section = "Concurrency", objectives = listOf("Global Interpreter Lock", "Free-threading in 3.13+"), explanation = "Mutex protecting access to Python objects.", codeSnippet = "# Threading for I/O, Multiprocessing for CPU")
    val NODE_THREADING = VisualRoadmapNode(id = "node_threading", title = "Threading", section = "Concurrency", objectives = listOf("threading.Thread", "Thread synchronization locks"), explanation = "Concurrent threads for overlapping I/O latency.", codeSnippet = "import threading")
    val NODE_GLOB = VisualRoadmapNode(id = "node_glob", title = "glob", section = "File Handling", objectives = listOf("Pattern matching file paths", "rglob in pathlib"), explanation = "Locate files matching wildcard patterns across directories.", codeSnippet = "import glob; files = glob.glob('*.py')")

    // Left of Concurrency: Frameworks
    val NODE_LEARN_FRAMEWORK = VisualRoadmapNode(id = "node_learn_framework", title = "Learn a Framework", section = "Web Development", isMajor = true, objectives = listOf("Selecting the right tool", "Micro vs full-stack"), explanation = "Build backend services, web applications, and APIs.", codeSnippet = "from fastapi import FastAPI")
    val NODE_PLOTLY_DASH = VisualRoadmapNode(id = "node_plotly_dash", title = "Plotly Dash", section = "Web Development", objectives = listOf("Interactive web analytics", "Data visualization"), explanation = "Framework for analytical web applications in Python.", codeSnippet = "import dash")
    val NODE_PYRAMID = VisualRoadmapNode(id = "node_pyramid", title = "Pyramid", section = "Web Development", objectives = listOf("Flexible web framework", "URL dispatching"), explanation = "Modular web framework that scales from small to large.", codeSnippet = "from pyramid.config import Configurator")
    val NODE_GEVENT = VisualRoadmapNode(id = "node_gevent", title = "gevent", section = "Web Development", objectives = listOf("Coroutines with greenlet", "Monkey patching"), explanation = "Coroutine-based Python networking library.", codeSnippet = "import gevent")
    val NODE_AIOHTTP = VisualRoadmapNode(id = "node_aiohttp", title = "aiohttp", section = "Web Development", objectives = listOf("Asynchronous HTTP client/server", "WebSockets"), explanation = "Asynchronous HTTP client and server for asyncio.", codeSnippet = "import aiohttp")
    val NODE_TORNADO = VisualRoadmapNode(id = "node_tornado", title = "Tornado", section = "Web Development", objectives = listOf("Non-blocking network I/O", "Long polling & WebSockets"), explanation = "Scalable non-blocking web server and framework.", codeSnippet = "import tornado.web")
    val NODE_SANIC = VisualRoadmapNode(id = "node_sanic", title = "Sanic", section = "Web Development", objectives = listOf("Fast async web server", "uvloop integration"), explanation = "Async Python web server built for speed.", codeSnippet = "from sanic import Sanic")
    val NODE_FASTAPI = VisualRoadmapNode(id = "node_fastapi", title = "Fast API", section = "Web Development", objectives = listOf("High performance ASGI", "Automatic OpenAPI docs", "Dependency injection"), explanation = "Modern, high-performance web framework for APIs.", codeSnippet = "from fastapi import FastAPI; app = FastAPI()")
    val NODE_DJANGO = VisualRoadmapNode(id = "node_django", title = "Django", section = "Web Development", objectives = listOf("Batteries-included framework", "ORM & migrations", "Built-in Admin"), explanation = "Full-stack framework for enterprise applications.", codeSnippet = "# python manage.py migrate")
    val NODE_FLASK = VisualRoadmapNode(id = "node_flask", title = "Flask", section = "Web Development", objectives = listOf("Microframework", "Blueprints", "Jinja2 templating"), explanation = "Lightweight WSGI framework ideal for microservices.", codeSnippet = "from flask import Flask; app = Flask(__name__)")

    // Right of Concurrency: Quality & File Handling
    val NODE_CODE_FORMATTING = VisualRoadmapNode(id = "node_code_formatting", title = "Code Formatting", section = "Code Quality", isMajor = true, objectives = listOf("PEP 8 compliance", "Automated code formatting"), explanation = "Enforce clean, uniform code style across projects.", codeSnippet = "ruff format .")
    val NODE_YAPF = VisualRoadmapNode(id = "node_yapf", title = "yapf", section = "Code Quality", objectives = listOf("Google Python formatter", "Clang-format algorithm"), explanation = "Opinionated Python formatter from Google.", codeSnippet = "yapf -i app.py")
    val NODE_BLACK = VisualRoadmapNode(id = "node_black", title = "black", section = "Code Quality", objectives = listOf("Uncompromising formatter", "Deterministic AST"), explanation = "The uncompromising Python code formatter.", codeSnippet = "black src/")
    val NODE_RUFF = VisualRoadmapNode(id = "node_ruff", title = "ruff", section = "Code Quality", objectives = listOf("Rust-powered speed", "Replaces flake8 and black"), explanation = "Blazing fast Python linter and formatter written in Rust.", codeSnippet = "ruff check . --fix")
    val NODE_FILE_HANDLING = VisualRoadmapNode(id = "node_file_handling", title = "File Handling", section = "File Handling", isMajor = true, objectives = listOf("Reading & writing files", "JSON & CSV processing"), explanation = "Persist and process data files reliably.", codeSnippet = "from pathlib import Path; Path('f.txt').write_text('Data')")
    val NODE_SPHINX = VisualRoadmapNode(id = "node_sphinx", title = "Sphinx", section = "Documentation", objectives = listOf("Autodoc generation", "HTML documentation"), explanation = "Tool for creating intelligent and beautiful documentation.", codeSnippet = "sphinx-build -b html docs _build")

    // Milestone 5: Testing
    val TESTING_CENTER_NODE = VisualRoadmapNode(
        id = "testing_center",
        title = "Testing",
        section = "Testing",
        isMajor = true,
        badge = "Milestone 5",
        explanation = "Ensure production reliability through automated unit testing, integration tests, fixtures, and continuous quality checks.",
        objectives = listOf(
            "Write automated test suites using pytest",
            "Implement reusable test fixtures and mock external dependencies",
            "Apply Test-Driven Development (TDD) practices",
            "Configure logging and interactive debugging tools"
        ),
        codeSnippet = """import pytest

@pytest.fixture
def sample_user():
    return {"id": 101, "role": "Developer"}

def test_user_role(sample_user):
    assert sample_user["role"] == "Developer"""".trimIndent()
    )

    val NODE_TOX = VisualRoadmapNode(id = "node_tox", title = "tox", section = "Testing", objectives = listOf("Automating test environments", "Multi-Python testing"), explanation = "Command-line driven automated testing across multiple Python versions.", codeSnippet = "tox -e py311,py312")
    val NODE_UNITTEST = VisualRoadmapNode(id = "node_unittest", title = "unittest / pyUnit", section = "Testing", objectives = listOf("Standard library TestCase", "Assertions"), explanation = "Python's built-in unit testing framework.", codeSnippet = "import unittest; class Test(unittest.TestCase): pass")
    val NODE_DOCTEST = VisualRoadmapNode(id = "node_doctest", title = "doctest", section = "Testing", objectives = listOf("Testing docstrings", "Interactive examples"), explanation = "Verify code examples embedded directly in documentation docstrings.", codeSnippet = "import doctest; doctest.testmod()")
    val NODE_PYTEST = VisualRoadmapNode(id = "node_pytest", title = "pytest", section = "Testing", objectives = listOf("@pytest.fixture", "assert introspection", "Parametrization"), explanation = "The industry standard testing tool with clean assertions and fixtures.", codeSnippet = "def test_answer(): assert 2 + 2 == 4")

    // Complete list of all interactive roadmap nodes
    val ALL_VISUAL_NODES: List<VisualRoadmapNode> = listOf(
        BASICS_CENTER_NODE,
        NODE_SYNTAX, NODE_VARIABLES, NODE_OPERATORS, NODE_OPERATORS_AT, NODE_STRINGS, NODE_CONDITIONALS, NODE_LOOPS,
        NODE_LISTS, NODE_TUPLES, NODE_SETS, NODE_DICTS, NODE_CASTING, NODE_FUNCTIONS, NODE_EXCEPTIONS, NODE_COMMENTS, NODE_SCOPE_ANNOT,
        DSA_CENTER_NODE,
        NODE_ARRAYS_LISTS, NODE_HASH_MAPS, NODE_STACKS_QUEUES, NODE_BST, NODE_RECURSION, NODE_SORTING_SEARCHING,
        NODE_PYPI, NODE_PIP, NODE_CONDA, NODE_UV, NODE_POETRY, NODE_PDM, NODE_PKG_MANAGERS,
        NODE_BUILTIN, NODE_CUSTOM, NODE_MODULES, NODE_ERROR_HANDLING, NODE_LAMBDAS, NODE_DECORATORS, NODE_ITERATORS, NODE_REGEX, NODE_VARIABLE_SCOPE,
        OOP_CENTER_NODE,
        NODE_CLASSES, NODE_METHODS, NODE_INHERITANCE, NODE_ENCAPSULATION,
        NODE_COMMON_PACKAGES, NODE_PYPROJECT, NODE_COMPREHENSIONS, NODE_GENERATOR_EXPRESSIONS, NODE_PARADIGMS, NODE_CONTEXT_MANAGERS,
        NODE_PIPENV, NODE_VIRTUALENV, NODE_PYENV, NODE_STATIC_TYPING, NODE_TYPING, NODE_MYPY, NODE_PYRIGHT, NODE_PYRE, NODE_PYDANTIC,
        CONCURRENCY_CENTER_NODE,
        NODE_MULTIPROCESSING, NODE_ASYNCHRONY, NODE_GIL, NODE_THREADING, NODE_GLOB,
        NODE_LEARN_FRAMEWORK, NODE_PLOTLY_DASH, NODE_PYRAMID, NODE_GEVENT, NODE_AIOHTTP, NODE_TORNADO, NODE_SANIC, NODE_FASTAPI, NODE_DJANGO, NODE_FLASK,
        NODE_CODE_FORMATTING, NODE_YAPF, NODE_BLACK, NODE_RUFF, NODE_FILE_HANDLING, NODE_SPHINX,
        TESTING_CENTER_NODE,
        NODE_TOX, NODE_UNITTEST, NODE_DOCTEST, NODE_PYTEST
    )
}
