package com.offline_First.data.local

import com.offline_First.domain.model.*

/**
 * Structured static Python roadmap data provider.
 * Contains 8 sequential learning modules with connected topic nodes,
 * code examples, expected outputs, learning objectives, and practice questions.
 */
object PythonRoadmapDataProvider {

    fun getModules(): List<PythonModule> = listOf(
        // MODULE 1
        PythonModule(
            id = "mod_1_fundamentals",
            stageNumber = 1,
            title = "Python Fundamentals & Environment",
            subtitle = "Setup, Syntax & Core Execution",
            description = "Understand how Python executes code, configure your development environment, and write your first statements.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_intro_env",
                    moduleId = "mod_1_fundamentals",
                    title = "Getting Started & Environment Setup",
                    shortDescription = "CPython interpreter, Python virtual environments, and IDE setup.",
                    overview = "Python is a high-level, interpreted, dynamically typed programming language known for its readability and simplicity. In this topic, you set up the runtime environment and understand how code execution works.",
                    learningObjectives = listOf(
                        "Understand the difference between interpreted and compiled languages",
                        "Install and verify Python 3.10+ using the terminal",
                        "Create an isolated virtual environment using venv",
                        "Run Python scripts interactively via REPL and file execution"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The CPython Interpreter",
                            explanation = "CPython compiles source code (.py) into bytecode (.pyc), which is then interpreted by the Python Virtual Machine (PVM).",
                            keyPoints = listOf("Interpreted language with bytecode compilation step", "Platform independent through PVM", "Interactive REPL enables rapid prototyping")
                        ),
                        PythonSubtopic(
                            title = "Virtual Environments",
                            explanation = "Virtual environments isolate package dependencies for individual projects to avoid dependency conflicts across projects.",
                            keyPoints = listOf("Command: python -m venv .venv", "Activation: source .venv/bin/activate (Unix) or .venv\\Scripts\\activate (Windows)", "Prevents polluting system Python packages")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Checking Python Version & Basic REPL Expression",
                            code = """
import sys
import platform

print("Python Version:", platform.python_version())
print("Interpreter Path:", sys.executable)

# Simple expression
message = "Hello, EduNova Python Roadmap!"
print(message)
                            """.trimIndent(),
                            expectedOutput = """
Python Version: 3.11.8
Interpreter Path: /usr/local/bin/python3
Hello, EduNova Python Roadmap!
                            """.trimIndent(),
                            explanation = "The sys and platform standard library modules provide introspection into the current executing Python environment."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_env_1",
                            question = "Why is creating a virtual environment recommended when starting a new Python project?",
                            options = listOf(
                                "It speeds up Python execution by compiling to C machine code.",
                                "It isolates package dependencies and versions to prevent conflicts between projects.",
                                "It is required to run any Python script with python command.",
                                "It automatically converts Python code into binary executables."
                            ),
                            correctIndex = 1,
                            explanation = "Virtual environments isolate site-packages so different projects can use different versions of packages without conflict."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_variables_types",
                    moduleId = "mod_1_fundamentals",
                    title = "Variables, Types & Memory Model",
                    shortDescription = "Dynamic typing, primitive types (int, float, str, bool), and object references.",
                    overview = "In Python, variables are not memory containers that hold values; they are names that reference objects created in heap memory.",
                    learningObjectives = listOf(
                        "Understand variable assignment as reference binding",
                        "Work with primitive types: int, float, str, and bool",
                        "Perform explicit type casting using int(), str(), float(), bool()",
                        "Inspect object identity with id() and type with type()"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Everything is an Object",
                            explanation = "In Python, every value (numbers, strings, functions, classes) is an object with an identity, a type, and a value.",
                            keyPoints = listOf("id(x) gives the unique memory address identifier", "type(x) returns the object's class", "Variables point to objects rather than storing them directly")
                        ),
                        PythonSubtopic(
                            title = "Type Conversion & Casting",
                            explanation = "Python is strongly typed—it will not implicitly coerce incompatible types like adding an integer to a string.",
                            keyPoints = listOf("int('42') -> 42", "float('3.14') -> 3.14", "bool(0) is False; bool('hello') is True")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Dynamic Typing and Type Conversion",
                            code = """
# Dynamic typing
user_age_str = "24"
user_score = 98.5
is_enrolled = True

# Explicit type conversion
user_age = int(user_age_str)
total_score = user_age + user_score

print(f"Age: {user_age} ({type(user_age).__name__})")
print(f"Score: {user_score} ({type(user_score).__name__})")
print(f"Total: {total_score} ({type(total_score).__name__})")
print(f"Enrolled status: {is_enrolled}")
                            """.trimIndent(),
                            expectedOutput = """
Age: 24 (int)
Score: 98.5 (float)
Total: 122.5 (float)
Enrolled status: True
                            """.trimIndent(),
                            explanation = "Python handles type promotion smoothly; adding an int and a float produces a float."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_var_1",
                            question = "What will bool([]) evaluate to in Python?",
                            options = listOf("True", "False", "None", "Raises TypeError"),
                            correctIndex = 1,
                            explanation = "Empty collections (lists, tuples, dicts, sets, strings) and zero numeric values evaluate to False in a boolean context."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_strings_io",
                    moduleId = "mod_1_fundamentals",
                    title = "Strings Operations & Formatted I/O",
                    shortDescription = "f-strings, string slicing, immutability, and console I/O.",
                    overview = "Strings in Python are immutable sequences of Unicode characters. Master modern formatting with f-strings and slice notation.",
                    learningObjectives = listOf(
                        "Use modern f-strings (formatted string literals) with expressions and format specifiers",
                        "Master string slicing with [start:stop:step] syntax",
                        "Apply essential string methods: strip(), split(), join(), replace()",
                        "Handle user input safely with input() and input validation"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "f-strings Formatting",
                            explanation = "Introduced in Python 3.6, f-strings evaluate Python expressions at runtime directly inside string literals.",
                            keyPoints = listOf("f'{variable:.2f}' formats floats to 2 decimal places", "f'{name.upper()}' allows embedded method calls", "f'{x=}' prints variable name and its value for debugging")
                        ),
                        PythonSubtopic(
                            title = "String Slicing & Immutability",
                            explanation = "Strings cannot be modified in place. Slicing returns a new string using start, stop (exclusive), and step indices.",
                            keyPoints = listOf("text[0:4] extracts characters from index 0 to 3", "text[::-1] reverses a string", "text.strip() removes surrounding whitespace")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "f-strings and Slicing in Action",
                            code = """
course = "python programming mastery"

# Slicing and transformation
title_cased = course.title()
reversed_course = course[::-1]
words = course.split()

price = 49.998
discount = 0.20
final_price = price * (1 - discount)

print(f"Title: {title_cased}")
print(f"Words: {words}")
print(f"First 6 chars: '{course[:6]}'")
print(f"Final Price: ${'$'}{final_price:.2f}")
                            """.trimIndent(),
                            expectedOutput = """
Title: Python Programming Mastery
Words: ['python', 'programming', 'mastery']
First 6 chars: 'python'
Final Price: ${'$'}40.00
                            """.trimIndent(),
                            explanation = "f-strings with :.2f round the output for display without altering the original numeric value."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_str_1",
                            question = "Given s = 'Python', what does s[1:4] return?",
                            options = listOf("'Pyt'", "'yth'", "'ytho'", "'y'"),
                            correctIndex = 1,
                            explanation = "Slice index 1 is 'y', and index 4 is exclusive ('o' at index 4 is not included), yielding 'yth'."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 2
        PythonModule(
            id = "mod_2_control_flow",
            stageNumber = 2,
            title = "Control Flow & Decision Making",
            subtitle = "Conditionals, Loops & Iteration",
            description = "Control code execution path with branching logic, while loops, for loops, and sequence traversal.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_conditionals",
                    moduleId = "mod_2_control_flow",
                    title = "Conditionals & Boolean Logic",
                    shortDescription = "if, elif, else branches, comparison operators, and ternary expressions.",
                    overview = "Branching logic enables programs to execute different code paths based on dynamic data conditions.",
                    learningObjectives = listOf(
                        "Structure multi-branch logic with if, elif, and else",
                        "Combine conditions with and, or, and not",
                        "Use the conditional expression (ternary operator): value_if_true if condition else value_if_false",
                        "Evaluate truthiness and falsiness of custom and built-in objects"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Short-Circuit Evaluation",
                            explanation = "Python evaluates boolean operators from left to right and stops evaluation as soon as the result is determined.",
                            keyPoints = listOf("A and B: if A is false, B is never evaluated", "A or B: if A is true, B is never evaluated", "Useful for guarding against NoneType or division by zero")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Grade Calculator with Ternary Expression",
                            code = """
score = 87

if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
elif score >= 70:
    grade = "C"
else:
    grade = "F"

status = "Passed" if score >= 70 else "Needs Improvement"

print(f"Score: {score} -> Grade: {grade}")
print(f"Status: {status}")
                            """.trimIndent(),
                            expectedOutput = """
Score: 87 -> Grade: B
Status: Passed
                            """.trimIndent(),
                            explanation = "Elif statements are evaluated sequentially until a true expression is found."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_cond_1",
                            question = "What does the expression `None or 'Default'` evaluate to in Python?",
                            options = listOf("None", "True", "'Default'", "False"),
                            correctIndex = 2,
                            explanation = "Because None is falsy, the 'or' operator evaluates and returns the second truthy operand 'Default'."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_loops_iteration",
                    moduleId = "mod_2_control_flow",
                    title = "Loops & Iterable Traversal",
                    shortDescription = "for loops, while loops, range(), enumerate(), and zip().",
                    overview = "Iteration is at the heart of Python. Master pythonic looping with for-in loops and built-in iteration helpers.",
                    learningObjectives = listOf(
                        "Iterate through sequences using for-in loops and range()",
                        "Use enumerate() to access index and value simultaneously without manual counters",
                        "Combine parallel collections using zip()",
                        "Implement while loops with proper termination conditions"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "enumerate() and zip()",
                            explanation = "enumerate() provides a 0-indexed counter while iterating. zip() groups elements from multiple iterables into tuples.",
                            keyPoints = listOf("Avoid `for i in range(len(items))` in favour of `for i, item in enumerate(items)`", "zip(keys, values) creates pairs for dict construction")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Modern Iteration with enumerate and zip",
                            code = """
frameworks = ["FastAPI", "Django", "Flask"]
stars = [72000, 78000, 66000]

# enumerate
print("--- Enumeration ---")
for index, name in enumerate(frameworks, start=1):
    print(f"#{index}: {name}")

# zip
print("\n--- Parallel Pairing ---")
for name, star_count in zip(frameworks, stars):
    print(f"{name:8} -> {star_count:,} stars")
                            """.trimIndent(),
                            expectedOutput = """
--- Enumeration ---
#1: FastAPI
#2: Django
#3: Flask

--- Parallel Pairing ---
FastAPI  -> 72,000 stars
Django   -> 78,000 stars
Flask    -> 66,000 stars
                            """.trimIndent(),
                            explanation = "zip() stops when the shortest input iterable is exhausted."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_loop_1",
                            question = "What does `list(range(2, 8, 2))` generate in Python?",
                            options = listOf("[2, 4, 6, 8]", "[2, 4, 6]", "[2, 3, 4, 5, 6, 7]", "[4, 6, 8]"),
                            correctIndex = 1,
                            explanation = "The range starts at 2, steps by 2, and stops before 8, producing 2, 4, and 6."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_loop_control",
                    moduleId = "mod_2_control_flow",
                    title = "Loop Control & The for-else Clause",
                    shortDescription = "break, continue, pass, and the unique loop-else construct.",
                    overview = "Fine-tune loop behavior with break and continue statements, and leverage Python's unique for-else clause for search algorithms.",
                    learningObjectives = listOf(
                        "Exit loops early with break",
                        "Skip current iteration with continue",
                        "Understand when the `else` block of a loop executes",
                        "Use pass as a syntactic placeholder for future code"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The for...else Pattern",
                            explanation = "The `else` clause of a loop executes only if the loop terminates normally (without encountering a break).",
                            keyPoints = listOf("Ideal for search loops to detect 'not found' condition without boolean flags", "Break skips the else clause completely")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Prime Number Search Using for-else",
                            code = """
target = 13

for divisor in range(2, int(target ** 0.5) + 1):
    if target % divisor == 0:
        print(f"{target} is divisible by {divisor} (Not prime)")
        break
else:
    print(f"{target} is a PRIME number!")
                            """.trimIndent(),
                            expectedOutput = """
13 is a PRIME number!
                            """.trimIndent(),
                            explanation = "Since no divisor evenly divides 13, the loop completes without hitting break, triggering the else block."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_control_1",
                            question = "When does the `else` block attached to a `for` loop execute?",
                            options = listOf(
                                "When the loop terminates because of a `break` statement.",
                                "When the loop completes all its iterations without encountering a `break`.",
                                "On every iteration of the loop.",
                                "Only when the iterable passed to the loop was empty."
                            ),
                            correctIndex = 1,
                            explanation = "A loop's `else` clause executes if the loop finishes naturally without being terminated by `break`."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 3
        PythonModule(
            id = "mod_3_data_structures",
            stageNumber = 3,
            title = "Core Data Structures & Collections",
            subtitle = "Lists, Tuples, Dictionaries & Sets",
            description = "Master Python's built-in data structures, their performance characteristics, and comprehension syntax.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_lists_tuples",
                    moduleId = "mod_3_data_structures",
                    title = "Lists & Tuples (Mutable vs Immutable)",
                    shortDescription = "Ordered sequences, slicing, list methods, tuple packing and unpacking.",
                    overview = "Lists and tuples are ordered sequences. Lists are mutable and grow dynamically, while tuples are immutable and memory-efficient.",
                    learningObjectives = listOf(
                        "Manage list items with append(), extend(), insert(), pop(), and remove()",
                        "Distinguish mutability differences between lists and tuples",
                        "Use tuple packing and sequence unpacking with the * (splat) operator",
                        "Understand memory allocation differences between lists and tuples"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Sequence Unpacking",
                            explanation = "Python allows assigning elements of a tuple or list directly to multiple variables in a single statement.",
                            keyPoints = listOf("a, b = b, a swaps variables without temporary storage", "first, *middle, last = numbers captures variable-length elements")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "List Operations and Sequence Unpacking",
                            code = """
# Mutable list
skills = ["Python", "SQL"]
skills.append("Git")
skills.extend(["Docker", "FastAPI"])

print(f"All skills ({len(skills)}): {skills}")
removed = skills.pop(1)
print(f"Removed: {removed}, Remaining: {skills}")

# Tuple unpacking
point = (10, 20, 30)
x, y, z = point
print(f"Coordinates: x={x}, y={y}, z={z}")

# Star unpacking
first, *rest = skills
print(f"Primary: {first}, Secondary: {rest}")
                            """.trimIndent(),
                            expectedOutput = """
All skills (5): ['Python', 'SQL', 'Git', 'Docker', 'FastAPI']
Removed: SQL, Remaining: ['Python', 'Git', 'Docker', 'FastAPI']
Coordinates: x=10, y=20, z=30
Primary: Python, Secondary: ['Git', 'Docker', 'FastAPI']
                            """.trimIndent(),
                            explanation = "Tuples protect configuration or fixed data from accidental mutation during program execution."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_ds_1",
                            question = "Which statement about Python tuples is correct?",
                            options = listOf(
                                "Tuples can have new items added with tuple.append().",
                                "Tuples are immutable; once created, their elements cannot be changed.",
                                "Tuples cannot contain elements of different data types.",
                                "Tuples cannot be used as dictionary keys."
                            ),
                            correctIndex = 1,
                            explanation = "Tuples are immutable sequences, which also makes hashable tuples eligible for use as dictionary keys or set elements."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_dicts_sets",
                    moduleId = "mod_3_data_structures",
                    title = "Dictionaries & Sets",
                    shortDescription = "Key-value hash maps, fast lookups, set operations (union, intersection).",
                    overview = "Dictionaries and sets use hash tables under the hood, delivering O(1) average time complexity for lookups and insertions.",
                    learningObjectives = listOf(
                        "Create and update dictionaries using keys and get() with fallback defaults",
                        "Iterate through keys(), values(), and items() efficiently",
                        "Use sets for deduplication and mathematical set operations (union, intersection, difference)",
                        "Understand key requirements (hashable and immutable keys)"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Hash Tables & O(1) Lookups",
                            explanation = "Unlike searching a list (O(n)), checking membership `x in my_set` or `k in my_dict` is O(1) on average.",
                            keyPoints = listOf("Keys must be hashable (immutable objects like strings, ints, tuples)", "Dicts maintain insertion order since Python 3.7", "Sets guarantee uniqueness of elements")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Dictionary Aggregation & Set Operations",
                            code = """
# Dictionary with get fallback
user = {"name": "Antigravity", "role": "Engineer", "level": "Senior"}
salary = user.get("salary", "Confidential")
print(f"User: {user['name']} | Role: {user['role']} | Salary: {salary}")

# Sets: Deduplication & Math
frontend_devs = {"Alice", "Bob", "Charlie"}
backend_devs = {"Charlie", "David", "Alice"}

fullstack = frontend_devs.intersection(backend_devs)
all_devs = frontend_devs.union(backend_devs)

print(f"Fullstack (Intersection): {fullstack}")
print(f"Total Unique Devs: {len(all_devs)} -> {all_devs}")
                            """.trimIndent(),
                            expectedOutput = """
User: Antigravity | Role: Engineer | Salary: Confidential
Fullstack (Intersection): {'Alice', 'Charlie'}
Total Unique Devs: 4 -> {'Alice', 'Bob', 'Charlie', 'David'}
                            """.trimIndent(),
                            explanation = "user.get('key', default) avoids raising KeyError when a key is absent."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_dict_1",
                            question = "Why can a Python list NOT be used as a dictionary key?",
                            options = listOf(
                                "Lists are too slow to iterate over.",
                                "Lists are mutable and therefore unhashable.",
                                "Lists can only hold numerical data in Python.",
                                "Dictionary keys must always be strings."
                            ),
                            correctIndex = 1,
                            explanation = "Dictionary keys must be hashable so their hash value never changes during their lifetime. Lists are mutable and unhashable."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_comprehensions",
                    moduleId = "mod_3_data_structures",
                    title = "Comprehensions & Filtering",
                    shortDescription = "List, dictionary, and set comprehensions for clean data transformation.",
                    overview = "Comprehensions provide a concise, readable, and highly optimized syntax to derive new collections from existing iterables.",
                    learningObjectives = listOf(
                        "Write clean list comprehensions with transformation and filtering",
                        "Build dictionary comprehensions to remap or filter key-value pairs",
                        "Construct set comprehensions for unique derived elements",
                        "Know when to use comprehensions versus standard loops for readability"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Syntax: [expr for item in iterable if condition]",
                            explanation = "Comprehensions are executed in C-level loops, making them faster than append() inside standard for loops.",
                            keyPoints = listOf("Transformation part: expression at the beginning", "Filter part: if condition at the end", "Avoid nesting more than 2 loops inside one comprehension")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "List, Dict and Set Comprehensions",
                            code = """
numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# Even squares list comprehension
even_squares = [n ** 2 for n in numbers if n % 2 == 0]
print(f"Even squares: {even_squares}")

# Dict comprehension: name -> character count
languages = ["Python", "Kotlin", "Rust", "Go"]
length_map = {lang: len(lang) for lang in languages if len(lang) > 2}
print(f"Lengths: {length_map}")

# Set comprehension: unique extensions
filenames = ["app.py", "test.py", "index.html", "style.css", "main.py"]
extensions = {f.split(".")[-1] for f in filenames}
print(f"Unique extensions: {extensions}")
                            """.trimIndent(),
                            expectedOutput = """
Even squares: [4, 16, 36, 64, 100]
Lengths: {'Python': 6, 'Kotlin': 6, 'Rust': 4, 'Go': 2}
Unique extensions: {'py', 'html', 'css'}
                            """.trimIndent(),
                            explanation = "Set comprehensions automatically discard duplicate values produced during iteration."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_comp_1",
                            question = "What is the output of `[x * 2 for x in [1, 2, 3] if x > 1]`?",
                            options = listOf("[2, 4, 6]", "[4, 6]", "[2, 4]", "[6]"),
                            correctIndex = 1,
                            explanation = "Elements greater than 1 are 2 and 3; multiplying each by 2 yields [4, 6]."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 4
        PythonModule(
            id = "mod_4_functions",
            stageNumber = 4,
            title = "Functions & Modular Code",
            subtitle = "Parameters, Scope, Lambdas & Closures",
            description = "Write reusable functions, understand parameter passing, variable scope (LEGB), and functional helpers.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_functions_basics",
                    moduleId = "mod_4_functions",
                    title = "Defining Functions & Parameter Modes",
                    shortDescription = "def statements, return values, positional vs keyword arguments, default parameters.",
                    overview = "Functions are first-class citizens in Python. Understand parameter mechanisms, default argument pitfalls, and return statements.",
                    learningObjectives = listOf(
                        "Define functions with type hints and docstrings",
                        "Work with positional-only, keyword-only, and default arguments",
                        "Avoid the dangerous mutable default argument antipattern",
                        "Return single values, multiple values (via tuples), or None"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The Mutable Default Argument Gotcha",
                            explanation = "Default parameter values are evaluated once when the function is defined, not each time it is called.",
                            keyPoints = listOf("Antipattern: `def add(x, items=[])` shares the same list across calls", "Idiom: `def add(x, items=None): if items is None: items = []`")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Clean Functions with Type Hints and Defaults",
                            code = """
def calculate_price(
    base: float,
    tax_rate: float = 0.08,
    discount: float = 0.0
) -> float:
    '''Calculate final price after discount and tax.'''
    discounted = base * (1.0 - discount)
    return round(discounted * (1.0 + tax_rate), 2)

# Calling with defaults, positional, and keyword args
p1 = calculate_price(100.0)
p2 = calculate_price(100.0, discount=0.15)
p3 = calculate_price(base=250.0, tax_rate=0.05, discount=0.10)

print(f"P1 (Default): ${'$'}{p1}")
print(f"P2 (15% off): ${'$'}{p2}")
print(f"P3 (Custom):  ${'$'}{p3}")
                            """.trimIndent(),
                            expectedOutput = """
P1 (Default): ${'$'}108.0
P2 (15% off): ${'$'}91.8
P3 (Custom):  ${'$'}236.25
                            """.trimIndent(),
                            explanation = "Named keyword arguments make call sites self-documenting and order-independent."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_fn_1",
                            question = "Why is `def append_item(item, list_target=[])` considered bad practice in Python?",
                            options = listOf(
                                "Because default arguments must be numbers.",
                                "Because the same list object is retained and mutated across multiple function calls.",
                                "Because Python requires all parameters to have default values.",
                                "Because functions cannot accept lists as arguments."
                            ),
                            correctIndex = 1,
                            explanation = "Default arguments are evaluated once at definition time. Mutable objects like lists retain changes across invocations."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_args_kwargs",
                    moduleId = "mod_4_functions",
                    title = "Flexible Arguments (*args & **kwargs)",
                    shortDescription = "Accepting arbitrary positional and keyword arguments, argument unpacking.",
                    overview = "Learn how *args collects excess positional arguments into a tuple, while **kwargs collects excess keyword arguments into a dictionary.",
                    learningObjectives = listOf(
                        "Accept arbitrary positional arguments with *args",
                        "Accept arbitrary keyword arguments with **kwargs",
                        "Unpack iterables and dictionaries into function calls",
                        "Combine positional, *args, keyword-only, and **kwargs parameters cleanly"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Forwarding Arguments",
                            explanation = "Decorators and wrapper functions frequently use `*args, **kwargs` to pass all arguments transparently to wrapped functions.",
                            keyPoints = listOf("*args collects into a tuple", "**kwargs collects into a dict", "Unpacking at call site: `func(*my_list, **my_dict)`")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Flexible Logging Formatter with *args and **kwargs",
                            code = """
def build_query(table: str, *columns: str, **filters) -> str:
    col_str = ", ".join(columns) if columns else "*"
    query = f"SELECT {col_str} FROM {table}"
    if filters:
        conditions = [f"{k} = '{v}'" for k, v in filters.items()]
        query += " WHERE " + " AND ".join(conditions)
    return query

# Call with varying arguments
q1 = build_query("users", "id", "name", "email", status="active", role="admin")
q2 = build_query("logs")

print("Q1:", q1)
print("Q2:", q2)
                            """.trimIndent(),
                            expectedOutput = """
Q1: SELECT id, name, email FROM users WHERE status = 'active' AND role = 'admin'
Q2: SELECT * FROM logs
                            """.trimIndent(),
                            explanation = "*columns captures positional strings into a tuple; **filters captures keyword arguments into a dictionary."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_args_1",
                            question = "Inside a function defined with `def foo(*args):`, what type is `args`?",
                            options = listOf("list", "tuple", "dict", "set"),
                            correctIndex = 1,
                            explanation = "Python collects *args as an immutable tuple."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_scope_lambdas",
                    moduleId = "mod_4_functions",
                    title = "Variable Scope (LEGB) & Lambdas",
                    shortDescription = "Local, Enclosing, Global, Built-in scope resolution, anonymous functions.",
                    overview = "Master Python's LEGB scope lookup order, understand closure creation, and write inline lambda functions for sorting and mapping.",
                    learningObjectives = listOf(
                        "Understand LEGB: Local -> Enclosing -> Global -> Built-in resolution",
                        "Use global and nonlocal keywords appropriately",
                        "Write concise lambda expressions for key extraction in sorted(), min(), max()",
                        "Understand closures and inner functions"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "LEGB Rule",
                            explanation = "When looking up a name, Python searches Local scope, then Enclosing functions, then Global module, then Built-ins.",
                            keyPoints = listOf("Assigning to a variable inside a function makes it local unless declared global or nonlocal", "Lambdas are limited to a single expression")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Custom Sorting with Lambdas and Closures",
                            code = """
# Sorting complex data with lambda
students = [
    {"name": "Sophia", "gpa": 3.9, "credits": 84},
    {"name": "Lucas",  "gpa": 3.4, "credits": 92},
    {"name": "Maya",   "gpa": 3.9, "credits": 110}
]

# Sort by GPA descending, then credits descending
students.sort(key=lambda s: (-s["gpa"], -s["credits"]))

for rank, s in enumerate(students, start=1):
    print(f"#{rank} {s['name']}: GPA {s['gpa']}, Credits {s['credits']}")
                            """.trimIndent(),
                            expectedOutput = """
#1 Maya: GPA 3.9, Credits 110
#2 Sophia: GPA 3.9, Credits 84
#3 Lucas: GPA 3.4, Credits 92
                            """.trimIndent(),
                            explanation = "Returning a tuple in the key lambda enables multi-level tie-breaking sorting."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_scope_1",
                            question = "What is the correct order of scope resolution in Python (LEGB)?",
                            options = listOf(
                                "Local, Enclosing, Global, Built-in",
                                "Local, External, General, Binary",
                                "Global, Local, Enclosing, Built-in",
                                "Built-in, Global, Enclosing, Local"
                            ),
                            correctIndex = 0,
                            explanation = "Python resolves names from the innermost scope outwards: Local, Enclosing, Global, Built-in."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 5
        PythonModule(
            id = "mod_5_oop",
            stageNumber = 5,
            title = "Object-Oriented Programming (OOP)",
            subtitle = "Classes, Inheritance & Dunder Methods",
            description = "Build structured software using OOP principles: encapsulation, inheritance, polymorphism, and magic methods.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_classes_objects",
                    moduleId = "mod_5_oop",
                    title = "Classes, Instances & self",
                    shortDescription = "Creating blueprints, constructor __init__, instance vs class attributes.",
                    overview = "Classes are templates for creating objects. Learn the role of the `self` parameter, how `__init__` initializes instances, and class-level vs instance-level state.",
                    learningObjectives = listOf(
                        "Define classes and instantiate objects",
                        "Use __init__ to initialize instance attributes",
                        "Understand why `self` must be explicitly passed as the first parameter to instance methods",
                        "Distinguish between instance attributes and shared class attributes"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The `self` Parameter",
                            explanation = "When you call `obj.method()`, Python automatically translates it into `Class.method(obj)`. `self` refers to the specific instance calling the method.",
                            keyPoints = listOf("Always the first parameter of instance methods", "Enables instances to have their own private state", "Class attributes are shared across all instances")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "BankAccount Class with Encapsulated State",
                            code = """
class BankAccount:
    interest_rate = 0.04  # Class attribute

    def __init__(self, owner: str, initial_balance: float = 0.0):
        self.owner = owner          # Instance attribute
        self.balance = initial_balance

    def deposit(self, amount: float) -> float:
        if amount <= 0:
            raise ValueError("Deposit must be positive")
        self.balance += amount
        return self.balance

    def withdraw(self, amount: float) -> float:
        if amount > self.balance:
            raise ValueError("Insufficient funds")
        self.balance -= amount
        return self.balance

acct = BankAccount("Antigravity Dev", 500.0)
acct.deposit(250.0)
acct.withdraw(100.0)
print(f"Owner: {acct.owner} | Balance: ${'$'}{acct.balance:.2f} | Rate: {BankAccount.interest_rate * 100}%")
                            """.trimIndent(),
                            expectedOutput = """
Owner: Antigravity Dev | Balance: ${'$'}650.00 | Rate: 4.0%
                            """.trimIndent(),
                            explanation = "Instance methods update self.balance for that specific object instance."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_oop_1",
                            question = "What does the `self` parameter represent in an instance method?",
                            options = listOf(
                                "A reference to the Class definition itself.",
                                "A reference to the specific instance of the class calling the method.",
                                "A copy of global variables.",
                                "A reserved keyword that cannot be renamed."
                            ),
                            correctIndex = 1,
                            explanation = "In Python, `self` refers to the specific instance object upon which the method is called."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_inheritance_poly",
                    moduleId = "mod_5_oop",
                    title = "Inheritance & Polymorphism",
                    shortDescription = "Subclassing, super() calls, method overriding, and duck typing.",
                    overview = "Inheritance allows subclasses to reuse and extend behavior from parent classes. Polymorphism enables uniform interfaces across different object types.",
                    learningObjectives = listOf(
                        "Create subclasses inheriting from base classes",
                        "Use super().__init__() to invoke parent constructors",
                        "Override parent methods to customize subclass behavior",
                        "Understand Python's dynamic 'duck typing': if it quacks like a duck, it is a duck"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "super() and Method Overriding",
                            explanation = "super() delegates method calls to the next class in the Method Resolution Order (MRO).",
                            keyPoints = listOf("Allows subclasses to extend rather than replace parent initialization", "isinstance(obj, BaseClass) returns True for subclasses")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Polymorphic Notification System",
                            code = """
class Notification:
    def __init__(self, recipient: str):
        self.recipient = recipient

    def send(self, message: str) -> str:
        raise NotImplementedError("Subclasses must implement send()")

class EmailNotification(Notification):
    def send(self, message: str) -> str:
        return f"[EMAIL] To: {self.recipient} | Body: {message}"

class SMSNotification(Notification):
    def send(self, message: str) -> str:
        return f"[SMS] To: {self.recipient} | Text: {message[:40]}"

# Polymorphic dispatcher
notifications = [
    EmailNotification("alex@example.com"),
    SMSNotification("+15551234567")
]

for notifier in notifications:
    print(notifier.send("Your EduNova roadmap is updated!"))
                            """.trimIndent(),
                            expectedOutput = """
[EMAIL] To: alex@example.com | Body: Your EduNova roadmap is updated!
[SMS] To: +15551234567 | Text: Your EduNova roadmap is updated!
                            """.trimIndent(),
                            explanation = "The client code treats all notifiers identically, relying on each subclass's specific implementation."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_poly_1",
                            question = "What is the purpose of `super().__init__(...)` in a subclass's `__init__` method?",
                            options = listOf(
                                "It destroys the parent class object.",
                                "It calls the constructor of the parent class to initialize inherited attributes.",
                                "It prevents the subclass from creating new attributes.",
                                "It compiles the subclass to bytecode."
                            ),
                            correctIndex = 1,
                            explanation = "`super().__init__()` executes the parent class's constructor, ensuring base attributes are properly set up."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_encapsulation_dunder",
                    moduleId = "mod_5_oop",
                    title = "Properties & Dunder (Magic) Methods",
                    shortDescription = "@property getters/setters, __str__, __repr__, __len__, __eq__.",
                    overview = "Dunder (double underscore) methods allow custom classes to integrate seamlessly with Python's built-in operators and protocols (like str(), len(), ==).",
                    learningObjectives = listOf(
                        "Use @property to create managed getters and setters with validation",
                        "Implement __str__ (user-friendly string) and __repr__ (unambiguous developer representation)",
                        "Implement __len__ and __eq__ for container and equality protocols",
                        "Understand Python's conventions for private attributes (_single and __double underscore)"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "__str__ vs __repr__",
                            explanation = "`__str__` is intended for end users (called by print() and str()). `__repr__` is intended for developers/debugging (should ideally be valid Python code to recreate the object).",
                            keyPoints = listOf("If __str__ is missing, Python falls back to __repr__", "Implemented in interactive sessions and logging")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Vector Class with Dunder Operators & Property",
                            code = """
class Vector2D:
    def __init__(self, x: float, y: float):
        self.x = float(x)
        self.y = float(y)

    def __repr__(self) -> str:
        return f"Vector2D({self.x}, {self.y})"

    def __str__(self) -> str:
        return f"({self.x}, {self.y})"

    def __add__(self, other: "Vector2D") -> "Vector2D":
        return Vector2D(self.x + other.x, self.y + other.y)

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Vector2D):
            return False
        return self.x == other.x and self.y == other.y

    @property
    def magnitude(self) -> float:
        return (self.x ** 2 + self.y ** 2) ** 0.5

v1 = Vector2D(3, 4)
v2 = Vector2D(1, 2)
v3 = v1 + v2

print(f"v1 string: {v1}")
print(f"v1 repr:   {repr(v1)}")
print(f"v1 + v2:   {v3}")
print(f"v1 magnitude: {v1.magnitude:.1f}")
print(f"Equality check: {v1 == Vector2D(3, 4)}")
                            """.trimIndent(),
                            expectedOutput = """
v1 string: (3.0, 4.0)
v1 repr:   Vector2D(3.0, 4.0)
v1 + v2:   (4.0, 6.0)
v1 magnitude: 5.0
Equality check: True
                            """.trimIndent(),
                            explanation = "Implementing __add__ and __eq__ integrates custom classes with Python's + and == operators."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_dunder_1",
                            question = "Which dunder method is called when `print(my_object)` is executed?",
                            options = listOf("__print__", "__str__", "__show__", "__display__"),
                            correctIndex = 1,
                            explanation = "Python invokes `__str__` to obtain an informal, printable string representation of an object."
                        )
                    ),
                    estimatedMinutes = 25
                )
            )
        ),

        // MODULE 6
        PythonModule(
            id = "mod_6_io_exceptions",
            stageNumber = 6,
            title = "Error Handling & File Operations",
            subtitle = "Exceptions, Context Managers & JSON/CSV",
            description = "Build resilient software using robust exception handling, safely interact with the filesystem, and process data formats.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_exceptions",
                    moduleId = "mod_6_io_exceptions",
                    title = "Robust Exception Handling",
                    shortDescription = "try, except, else, finally blocks, custom exception classes, exception chaining.",
                    overview = "Exceptions represent error states during program execution. Writing defensive code with targeted exception handlers keeps your application from crashing.",
                    learningObjectives = listOf(
                        "Structure exception handling using try, except, else, and finally",
                        "Avoid catching generic `except Exception:` blindly",
                        "Define custom application-specific exception classes",
                        "Inspect exception objects and error messages with `as e`"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The `else` and `finally` Clauses",
                            explanation = "`else` runs only if NO exception occurred. `finally` runs unconditionally (even after returns or caught/uncaught exceptions), making it ideal for cleanup.",
                            keyPoints = listOf("try: risky code", "except SpecificError: handle failure", "else: code that needs successful try outcome", "finally: guaranteed cleanup")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Custom Exception with Full Try-Except-Else-Finally",
                            code = """
class ValidationError(Exception):
    '''Raised when data validation fails.'''
    pass

def register_user(username: str, age: int):
    try:
        if len(username) < 3:
            raise ValidationError("Username must be at least 3 characters")
        if age < 13:
            raise ValidationError("User must be at least 13 years old")
    except ValidationError as err:
        print(f"Validation Rejected: {err}")
    else:
        print(f"User '{username}' registered successfully!")
    finally:
        print("Registration attempt completed.")

register_user("al", 20)
print("---")
register_user("alex_dev", 22)
                            """.trimIndent(),
                            expectedOutput = """
Validation Rejected: Username must be at least 3 characters
Registration attempt completed.
---
User 'alex_dev' registered successfully!
Registration attempt completed.
                            """.trimIndent(),
                            explanation = "The finally block executes in both success and failure cases."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_exc_1",
                            question = "In a `try-except-else-finally` block, when does the `else` block execute?",
                            options = listOf(
                                "Only when an exception was raised and caught.",
                                "Only when NO exceptions were raised in the `try` block.",
                                "Regardless of whether an exception occurred.",
                                "Only when the `finally` block raises an error."
                            ),
                            correctIndex = 1,
                            explanation = "The `else` block runs only if the code in the `try` block executed without raising any exceptions."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_file_io",
                    moduleId = "mod_6_io_exceptions",
                    title = "File I/O & Context Managers (with)",
                    shortDescription = "Reading/writing files, text vs binary modes, the `with` statement.",
                    overview = "Learn how to read and write files using the `with` statement, which guarantees that file descriptors are safely closed even if exceptions occur.",
                    learningObjectives = listOf(
                        "Open and read files safely using `with open(..., mode)`",
                        "Distinguish between 'r' (read), 'w' (overwrite), 'a' (append), and 'b' (binary) modes",
                        "Read files line-by-line using memory-efficient iteration",
                        "Understand how the context manager protocol (`__enter__` and `__exit__`) works"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The `with` Statement",
                            explanation = "The context manager protocol ensures `file.close()` is called as soon as the `with` block exits.",
                            keyPoints = listOf("Guarantees resource release even on crashes", "Iterating `for line in f:` reads lines lazily without loading the whole file into RAM")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Simulating Context Manager and File Operations",
                            code = """
import io

# Simulating file stream with io.StringIO
virtual_file = io.StringIO()

# Writing with context manager
with virtual_file as f:
    f.write("Line 1: EduNova Python Roadmap\n")
    f.write("Line 2: Built-in File I/O Mastery\n")
    f.write("Line 3: Clean Context Management\n")

# Reading back content
content = virtual_file.getvalue()
lines = content.strip().split("\n")

print(f"Total lines: {len(lines)}")
for index, line in enumerate(lines, start=1):
    print(f"[{index}] {line}")
                            """.trimIndent(),
                            expectedOutput = """
Total lines: 3
[1] Line 1: EduNova Python Roadmap
[2] Line 2: Built-in File I/O Mastery
[3] Line 3: Clean Context Management
                            """.trimIndent(),
                            explanation = "Context managers ensure streams and handles are cleaned up automatically."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_file_1",
                            question = "Why is `with open('file.txt', 'r') as f:` preferred over `f = open('file.txt', 'r')`?",
                            options = listOf(
                                "It executes the read operation in a background thread.",
                                "It automatically closes the file handle when exiting the block, even if an exception occurs.",
                                "It compiles the file content to binary automatically.",
                                "It prevents any other program from modifying the file forever."
                            ),
                            correctIndex = 1,
                            explanation = "The `with` statement guarantees cleanup by automatically calling `f.close()` via `__exit__`."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_json_csv",
                    moduleId = "mod_6_io_exceptions",
                    title = "Structured Data: JSON & CSV",
                    shortDescription = "json.loads/dumps, json.load/dump, csv.reader and csv.DictReader.",
                    overview = "Real-world Python applications communicate via structured data formats. Learn standard library serialization with the json and csv modules.",
                    learningObjectives = listOf(
                        "Serialize and deserialize JSON data using json.dumps() and json.loads()",
                        "Read and write JSON files with json.dump() and json.load()",
                        "Parse CSV tabular records using csv.DictReader",
                        "Handle dates and custom types during JSON serialization"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "loads vs load, dumps vs dump",
                            explanation = "Methods with 's' work on strings (loads/dumps). Methods without 's' take a file-like stream object (load/dump).",
                            keyPoints = listOf("json.loads(str) -> Python dict/list", "json.dumps(obj, indent=2) -> formatted JSON string", "csv.DictReader maps headers to row dictionaries")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "JSON Serialization and Parsing",
                            code = """
import json

payload = {
    "roadmap": "Python Mastery",
    "version": 3.11,
    "modules_count": 8,
    "tags": ["programming", "backend", "ai"]
}

# Serialize to formatted JSON string
json_text = json.dumps(payload, indent=2)
print("Serialized JSON:\n" + json_text)

# Deserialize back to Python dict
parsed = json.loads(json_text)
print("\nParsed Type:", type(parsed).__name__)
print("Roadmap Name:", parsed["roadmap"])
print("First Tag:", parsed["tags"][0])
                            """.trimIndent(),
                            expectedOutput = """
Serialized JSON:
{
  "roadmap": "Python Mastery",
  "version": 3.11,
  "modules_count": 8,
  "tags": [
    "programming",
    "backend",
    "ai"
  ]
}

Parsed Type: dict
Roadmap Name: Python Mastery
First Tag: programming
                            """.trimIndent(),
                            explanation = "json.dumps with indent=2 produces human-readable formatted JSON."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_json_1",
                            question = "What is the difference between `json.loads` and `json.load`?",
                            options = listOf(
                                "`json.loads` reads from a string, while `json.load` reads from a file/stream.",
                                "`json.loads` is for loading arrays; `json.load` is for single values.",
                                "`json.loads` is asynchronous; `json.load` is synchronous.",
                                "`json.loads` is deprecated in Python 3."
                            ),
                            correctIndex = 0,
                            explanation = "`json.loads` ('load string') parses a JSON string, whereas `json.load` parses from a file-like stream."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 7
        PythonModule(
            id = "mod_7_packages_modules",
            stageNumber = 7,
            title = "Modules, Packages & Virtual Environments",
            subtitle = "Imports, Structure & Dependency Management",
            description = "Organize code into reusable modules, structure multi-file packages, and manage third-party dependencies using pip.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_modules_packages",
                    moduleId = "mod_7_packages_modules",
                    title = "Modules & Package Structure",
                    shortDescription = "import system, __name__ == '__main__', package __init__.py files.",
                    overview = "Every Python file is a module. Group related modules into packages with directories to scale codebases cleanly.",
                    learningObjectives = listOf(
                        "Import symbols cleanly using `import module` and `from module import name`",
                        "Understand the purpose of `if __name__ == '__main__':` boilerplate",
                        "Organize code into packages using directories with __init__.py",
                        "Avoid circular import dependency cycles"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "The `__name__` Variable",
                            explanation = "When a file is run directly, Python sets `__name__ = '__main__'`. When imported as a module, `__name__` is set to the module's name.",
                            keyPoints = listOf("Allows a file to act both as a reusable library and as an executable script", "Prevents test/sample code from running upon import")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Using __name__ == '__main__' Idiom",
                            code = """
def format_currency(amount: float) -> str:
    '''Reusable helper function.'''
    return f"${'$'}{amount:,.2f}"

def run_tests():
    '''Runs only when file is executed directly.'''
    assert format_currency(1234.5) == "${'$'}1,234.50"
    print("All internal tests passed!")

if __name__ == "__main__":
    print("Module executed directly:")
    run_tests()
    print("Formatted demo:", format_currency(9876543.21))
                            """.trimIndent(),
                            expectedOutput = """
Module executed directly:
All internal tests passed!
Formatted demo: ${'$'}9,876,543.21
                            """.trimIndent(),
                            explanation = "Other scripts can import `format_currency` without `run_tests()` being triggered."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_mod_1",
                            question = "Why do Python scripts use `if __name__ == '__main__':`?",
                            options = listOf(
                                "To speed up code execution through JIT compilation.",
                                "To ensure that specific code only runs when the file is executed directly, not when imported as a module.",
                                "To define the main entry class required by the operating system.",
                                "To prevent syntax errors in Python 3."
                            ),
                            correctIndex = 1,
                            explanation = "`__name__` is `'__main__'` only when the script is invoked directly as the program entry point."
                        )
                    ),
                    estimatedMinutes = 20
                ),
                PythonTopic(
                    id = "topic_pip_dependencies",
                    moduleId = "mod_7_packages_modules",
                    title = "Dependency Management with Pip",
                    shortDescription = "PyPI, pip install, requirements.txt, and modern lockfiles.",
                    overview = "The Python Package Index (PyPI) hosts hundreds of thousands of open-source packages. Learn how to install, pin, and manage project dependencies.",
                    learningObjectives = listOf(
                        "Search and install packages with `pip install`",
                        "Export deterministic dependency snapshots using `pip freeze > requirements.txt`",
                        "Install project dependencies using `pip install -r requirements.txt`",
                        "Understand semantic version constraints (e.g., package>=2.0,<3.0)"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "requirements.txt Best Practices",
                            explanation = "Always pin versions in production to ensure repeatable, deterministic builds across environments.",
                            keyPoints = listOf("Exact pinning: requests==2.31.0", "Compatible release: pydantic~=2.5", "Upgrading: pip install --upgrade package")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Typical requirements.txt and Command Usage",
                            code = """
# Sample requirements.txt content:
'''
fastapi>=0.110.0,<1.0.0
uvicorn[standard]==0.29.0
sqlalchemy>=2.0.28
pydantic==2.6.4
pytest==8.1.1
'''

# Standard terminal workflow:
# 1. python -m venv .venv
# 2. source .venv/bin/activate (or .venv\\Scripts\\activate on Windows)
# 3. pip install -r requirements.txt
# 4. pip list

print("Virtual environment workflow demonstrated.")
                            """.trimIndent(),
                            expectedOutput = """
Virtual environment workflow demonstrated.
                            """.trimIndent(),
                            explanation = "Keeping virtual environments separate from system packages prevents global version breakage."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_pip_1",
                            question = "Which command installs all dependencies listed in a requirements file?",
                            options = listOf(
                                "pip get requirements.txt",
                                "pip install -r requirements.txt",
                                "python setup.py install",
                                "pip compile requirements.txt"
                            ),
                            correctIndex = 1,
                            explanation = "The `-r` flag tells pip to read and install packages from the specified requirements file."
                        )
                    ),
                    estimatedMinutes = 20
                )
            )
        ),

        // MODULE 8
        PythonModule(
            id = "mod_8_practical_projects",
            stageNumber = 8,
            title = "Advanced Techniques & Practical Projects",
            subtitle = "Decorators, Generators & CLI Capstone",
            description = "Write idiomatic Python using function decorators, memory-efficient generators, and build a practical command-line tool.",
            iconKey = "python",
            topics = listOf(
                PythonTopic(
                    id = "topic_generators_decorators",
                    moduleId = "mod_8_practical_projects",
                    title = "Decorators & Generators",
                    shortDescription = "Function decorators with @wraps, generator functions with yield.",
                    overview = "Decorators allow modifying or extending function behavior cleanly. Generators yield values lazily, processing large datasets with minimal memory.",
                    learningObjectives = listOf(
                        "Understand closures and build custom function decorators",
                        "Use functools.wraps to preserve function metadata",
                        "Write memory-efficient generator functions using `yield`",
                        "Chain generator expressions to stream large datasets"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Decorators with @functools.wraps",
                            explanation = "A decorator takes a function as input, wraps it with pre/post processing, and returns the wrapper function.",
                            keyPoints = listOf("Syntax: @my_decorator above function definition", "Use @wraps(func) so __name__ and __doc__ aren't lost", "Common uses: logging, timing, authorization, caching")
                        ),
                        PythonSubtopic(
                            title = "Generators and Lazy Evaluation",
                            explanation = "Functions containing `yield` pause their execution and resume from where they left off when next() is called.",
                            keyPoints = listOf("Generators consume O(1) memory regardless of sequence length", "Ideal for reading gigabyte log files or infinite streams")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Execution Timer Decorator & Fibonacci Generator",
                            code = """
import time
from functools import wraps

# Custom timing decorator
def timer(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"[{func.__name__}] finished in {elapsed * 1000:.3f} ms")
        return result
    return wrapper

# Generator function
def fibonacci(limit: int):
    a, b = 0, 1
    count = 0
    while count < limit:
        yield a
        a, b = b, a + b
        count += 1

@timer
def compute_fibs(n: int):
    return list(fibonacci(n))

first_eight = compute_fibs(8)
print("Fibonacci numbers:", first_eight)
                            """.trimIndent(),
                            expectedOutput = """
[compute_fibs] finished in 0.045 ms
Fibonacci numbers: [0, 1, 1, 2, 3, 5, 8, 13]
                            """.trimIndent(),
                            explanation = "The @timer decorator wraps compute_fibs seamlessly without modifying its internal logic."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_gen_1",
                            question = "What happens when a generator function encounters a `yield` statement?",
                            options = listOf(
                                "It terminates the function and cleans up its local scope forever.",
                                "It produces a value to the caller and pauses its state until the next item is requested.",
                                "It restarts the function from the very beginning.",
                                "It converts the entire function into a tuple."
                            ),
                            correctIndex = 1,
                            explanation = "The `yield` statement yields the current value and preserves the local execution state until the generator is resumed."
                        )
                    ),
                    estimatedMinutes = 25
                ),
                PythonTopic(
                    id = "topic_capstone_cli",
                    moduleId = "mod_8_practical_projects",
                    title = "Capstone Project: CLI Task Manager",
                    shortDescription = "Building an end-to-end Python CLI application combining all learned concepts.",
                    overview = "Put everything together: classes, file I/O, error handling, command-line arguments, and comprehensions into a complete working CLI tool.",
                    learningObjectives = listOf(
                        "Design clean domain classes for tasks and task collections",
                        "Parse command line actions (add, list, complete, delete)",
                        "Persist tasks to a local JSON file across runs",
                        "Package and run the script as a standalone command"
                    ),
                    subtopics = listOf(
                        PythonSubtopic(
                            title = "Architecture of a Clean CLI",
                            explanation = "Separate concerns: CLI argument parser -> Application service / Manager -> Persistence layer.",
                            keyPoints = listOf("Model: Task dataclass with status and timestamp", "Repository: JsonTaskRepository with load and save", "CLI Interface: Simple interactive or argument-based command loop")
                        )
                    ),
                    codeExamples = listOf(
                        PythonCodeExample(
                            title = "Complete In-Memory CLI Task Manager Prototype",
                            code = """
from dataclasses import dataclass, field
import datetime

@dataclass
class Task:
    id: int
    title: str
    completed: bool = False
    created_at: str = field(default_factory=lambda: datetime.date.today().isoformat())

class TaskManager:
    def __init__(self):
        self.tasks: list[Task] = []
        self._next_id = 1

    def add_task(self, title: str) -> Task:
        task = Task(id=self._next_id, title=title)
        self.tasks.append(task)
        self._next_id += 1
        return task

    def complete_task(self, task_id: int) -> bool:
        for t in self.tasks:
            if t.id == task_id:
                t.completed = True
                return True
        return False

    def list_summary(self):
        print(f"\n--- EduNova Task Dashboard ({len(self.tasks)} items) ---")
        for t in self.tasks:
            status = "✓ Done" if t.completed else "○ Pending"
            print(f"[{t.id}] {status:9} | {t.title} ({t.created_at})")

# Demonstration
manager = TaskManager()
manager.add_task("Set up Python virtual environment")
manager.add_task("Master list and dictionary comprehensions")
manager.add_task("Build fullstack FastAPI application")

manager.complete_task(1)
manager.complete_task(2)

manager.list_summary()
                            """.trimIndent(),
                            expectedOutput = """
--- EduNova Task Dashboard (3 items) ---
[1] ✓ Done    | Set up Python virtual environment (2026-10-14)
[2] ✓ Done    | Master list and dictionary comprehensions (2026-10-14)
[3] ○ Pending | Build fullstack FastAPI application (2026-10-14)
                            """.trimIndent(),
                            explanation = "This capstone combines OOP classes, dataclasses, string formatting, list iteration, and state tracking."
                        )
                    ),
                    practiceQuestions = listOf(
                        PracticeQuestion(
                            id = "q_cap_1",
                            question = "Which module in Python's standard library is commonly used for parsing command-line flags and arguments?",
                            options = listOf("argparse", "cmdline", "terminal", "console_io"),
                            correctIndex = 0,
                            explanation = "`argparse` is Python's built-in, full-featured command-line argument parsing library."
                        )
                    ),
                    estimatedMinutes = 30
                )
            )
        )
    )

    fun getAllTopics(): List<PythonTopic> {
        return getModules().flatMap { it.topics }
    }

    fun getTopicById(topicId: String): PythonTopic? {
        return getAllTopics().firstOrNull { it.id == topicId }
    }
}
