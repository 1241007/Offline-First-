"""Canonical Course Knowledge Graph and Diagnostic Bank for EduNova Roadmap"""

from typing import Dict, Any, List, Optional


COURSE_KNOWLEDGE_GRAPHS: Dict[str, Dict[str, Any]] = {
    "python": {
        "domain": "Python",
        "description": "Comprehensive Python Mastery from Core Syntax to Systems Architecture",
        "skills": {
            "py_syntax": {
                "id": "py_syntax",
                "name": "Python Syntax, Types & Dynamic Semantics",
                "category": "Foundations",
                "description": "Variables, numeric types, strings, mutable vs immutable semantics",
                "prerequisites": [],
                "estimated_hours": 3.0,
                "subtopics": ["Numeric Types & Precision", "String Interpolation & Slicing", "Type Conversion & Dynamic Typing"],
                "pass_criteria": "Demonstrates correct understanding of Python typing and reference semantics",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_1",
                        "prompt": "In Python 3, what is the exact output and type resulting from print(type(4 / 2))?",
                        "question_type": "code_output",
                        "options": ["<class 'int'>", "<class 'float'>", "<class 'double'>", "<class 'number'>"],
                        "correct_index": 1,
                        "code_snippet": "print(type(4 / 2))",
                        "explanation": "In Python 3, the '/' division operator always performs float division yielding a float.",
                        "difficulty": "Beginner",
                        "selection_rationale": "Evaluates foundational knowledge of Python 3 division semantics and runtime types."
                    }
                ]
            },
            "py_control_flow": {
                "id": "py_control_flow",
                "name": "Control Flow, Iterators & List Comprehensions",
                "category": "Foundations",
                "description": "Conditionals, while/for loops, break/continue, range, iterators",
                "prerequisites": ["py_syntax"],
                "estimated_hours": 3.5,
                "subtopics": ["Iterables & Iterators", "List/Dict Comprehensions", "Loop Optimization & Generators"],
                "pass_criteria": "Writes idiomatic comprehensions and understands loop short-circuiting",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_2",
                        "prompt": "What is the final value printed by this loop?\n\ntotal = 0\nfor i in range(1, 5):\n    if i == 3:\n        continue\n    total += i\nprint(total)",
                        "question_type": "code_output",
                        "options": ["7", "10", "6", "8"],
                        "correct_index": 0,
                        "code_snippet": "total = 0\nfor i in range(1, 5):\n    if i == 3:\n        continue\n    total += i\nprint(total)",
                        "explanation": "range(1, 5) yields 1, 2, 3, 4. When i is 3, continue skips the addition, resulting in 1 + 2 + 4 = 7.",
                        "difficulty": "Beginner",
                        "selection_rationale": "Verifies precise tracing of loop controls and range bounds."
                    }
                ]
            },
            "py_functions": {
                "id": "py_functions",
                "name": "Functions, Scope, Closures & Default Arguments",
                "category": "Core Programming",
                "description": "First-class functions, args/kwargs, LEGB scope rules, closures, decorators",
                "prerequisites": ["py_syntax"],
                "estimated_hours": 4.0,
                "subtopics": ["Default Argument Trap", "*args and **kwargs", "Decorators & Wrappers", "Closure State"],
                "pass_criteria": "Avoids mutable default traps and constructs functional decorators",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_3",
                        "prompt": "What serious bug exists in this function?\n\ndef add_item(item, target_list=[]):\n    target_list.append(item)\n    return target_list",
                        "question_type": "debugging",
                        "options": [
                            "SyntaxError on function header",
                            "Mutable default argument persists and accumulates state across subsequent calls",
                            "TypeError: list has no method append",
                            "None of the above"
                        ],
                        "correct_index": 1,
                        "code_snippet": "def add_item(item, target_list=[]):\n    target_list.append(item)\n    return target_list",
                        "explanation": "Default argument expressions are evaluated once when the function is defined. Mutable defaults persist across invocations.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Determines whether the student understands Python function definition vs call-time binding."
                    }
                ]
            },
            "py_oop": {
                "id": "py_oop",
                "name": "Object-Oriented Programming & Dunder Protocols",
                "category": "Architecture",
                "description": "Classes, inheritance, polymorphism, encapsulation, __init__, __str__, __enter__/__exit__",
                "prerequisites": ["py_functions"],
                "estimated_hours": 5.0,
                "subtopics": ["Class vs Instance Attributes", "Super() & MRO", "Context Managers (with statement)", "Dunder Protocols"],
                "pass_criteria": "Designs decoupled classes with proper dunder method protocols",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_4",
                        "prompt": "What occurs during method resolution in Python when super().__init__() is called in multiple inheritance?",
                        "question_type": "conceptual",
                        "options": [
                            "It executes according to C3 Method Resolution Order (MRO)",
                            "It randomly picks one superclass",
                            "It always calls object.__init__ directly",
                            "It throws a diamond dependency error"
                        ],
                        "correct_index": 0,
                        "explanation": "Python employs the C3 Linearization algorithm to determine a deterministic MRO across multiple inheritance hierarchies.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Tests understanding of class hierarchies and MRO in production Python."
                    }
                ]
            },
            "py_data_structures": {
                "id": "py_data_structures",
                "name": "Data Structures, Collections & Performance",
                "category": "Core Programming",
                "description": "Lists, dicts, sets, tuples, heapq, collections module, time complexity",
                "prerequisites": ["py_syntax"],
                "estimated_hours": 4.5,
                "subtopics": ["Hash Maps & Hash Collisions", "Time Complexity Analysis", "collections.defaultdict & deque", "Set Operations"],
                "pass_criteria": "Selects appropriate collection types with optimal O(1) vs O(n) characteristics",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_5",
                        "prompt": "What is the average time complexity of checking membership ('x in s') in a Python set vs list?",
                        "question_type": "multiple_choice",
                        "options": ["O(1) for set, O(n) for list", "O(n) for set, O(1) for list", "O(log n) for set, O(1) for list", "O(1) for both"],
                        "correct_index": 0,
                        "explanation": "Python sets are implemented using hash tables offering average O(1) membership lookup compared to list linear O(n) scans.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Tests fundamental computational complexity and internal hash table semantics."
                    }
                ]
            },
            "py_concurrency": {
                "id": "py_concurrency",
                "name": "Asynchronous Programming, Asyncio & GIL",
                "category": "Advanced Systems",
                "description": "GIL implications, threading vs multiprocessing vs asyncio, event loops, coroutines",
                "prerequisites": ["py_oop", "py_functions"],
                "estimated_hours": 6.0,
                "subtopics": ["Global Interpreter Lock (GIL)", "Async/Await & Event Loops", "Thread-Safe Queues", "Multiprocessing Pool"],
                "pass_criteria": "Builds high-throughput async pipelines avoiding blocking the event loop",
                "diagnostic_questions": [
                    {
                        "id": "diag_py_6",
                        "prompt": "In an asyncio application, what happens if a synchronous time.sleep(5) is executed inside an async coroutine?",
                        "question_type": "debugging",
                        "options": [
                            "It blocks the entire OS thread and halts the entire asyncio event loop for 5 seconds",
                            "Asyncio automatically converts it into a non-blocking background task",
                            "Only that single coroutine pauses while others run concurrently",
                            "It raises a RuntimeError"
                        ],
                        "correct_index": 0,
                        "explanation": "time.sleep() is a blocking synchronous call that freezes the thread executing the single-threaded asyncio event loop. asyncio.sleep() should be awaited instead.",
                        "difficulty": "Advanced",
                        "selection_rationale": "Verifies clear distinction between cooperative async coroutines and synchronous blocking calls."
                    }
                ]
            }
        }
    },
    "java": {
        "domain": "Java",
        "description": "Enterprise Java Architecture, JVM Internals & Concurrency",
        "skills": {
            "java_syntax": {
                "id": "java_syntax",
                "name": "Java Syntax, Primitives & Object References",
                "category": "Foundations",
                "description": "Primitive types, wrapper classes, reference semantics, stack vs heap allocations",
                "prerequisites": [],
                "estimated_hours": 3.0,
                "subtopics": ["Primitive vs Reference Types", "Autoboxing & Unboxing", "Heap vs Stack Memory"],
                "pass_criteria": "Understands Java value vs reference mechanics",
                "diagnostic_questions": [
                    {
                        "id": "diag_java_1",
                        "prompt": "In Java 8 and later, how does an interface with `default` methods fundamentally differ from an `abstract class`?",
                        "question_type": "multiple_choice",
                        "options": [
                            "An abstract class can maintain mutable instance state (fields), while an interface cannot have instance state",
                            "Interfaces can only contain private methods, whereas abstract classes have public methods",
                            "Abstract classes cannot implement any methods with bodies",
                            "There is no difference; default methods completely deprecated abstract classes"
                        ],
                        "correct_index": 0,
                        "explanation": "Interfaces cannot have instance state/variables (only static final constants), whereas abstract classes can have constructors and mutable instance fields.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Verifies foundational object-oriented design and Java type system principles."
                    }
                ]
            },
            "java_concurrency": {
                "id": "java_concurrency",
                "name": "Java Concurrency, Memory Model & Volatile",
                "category": "Advanced Systems",
                "description": "Java Memory Model (JMM), volatile, synchronized, AtomicInteger, thread safety",
                "prerequisites": ["java_syntax"],
                "estimated_hours": 5.5,
                "subtopics": ["Volatile Happens-Before", "CAS Operations & Atomics", "Thread Pools & Executors", "Lock Contention"],
                "pass_criteria": "Implements thread-safe data pipelines without race conditions",
                "diagnostic_questions": [
                    {
                        "id": "diag_java_2",
                        "prompt": "What is the primary guarantee provided by the `volatile` keyword in Java multi-threaded concurrency?",
                        "question_type": "conceptual",
                        "options": [
                            "Guarantees visibility of changes across threads and prevents instruction reordering without acquiring a mutual exclusion lock",
                            "Guarantees compound operations like `count++` are executed atomically without race conditions",
                            "Prevents garbage collection from ever removing the object from JVM memory",
                            "Causes the thread to yield execution immediately to higher priority threads"
                        ],
                        "correct_index": 0,
                        "explanation": "`volatile` establishes a happens-before relationship ensuring memory visibility and preventing reordering, but does NOT guarantee atomicity for compound operations.",
                        "difficulty": "Advanced",
                        "selection_rationale": "Evaluates understanding of Java concurrency, memory visibility, and thread safety."
                    }
                ]
            },
            "java_collections": {
                "id": "java_collections",
                "name": "Java Collections Framework & JVM Performance",
                "category": "Core Architecture",
                "description": "HashMap, ConcurrentHashMap, ArrayList vs LinkedList, Treeify threshold",
                "prerequisites": ["java_syntax"],
                "estimated_hours": 4.5,
                "subtopics": ["HashMap Treeify Threshold", "ConcurrentHashMap Segmenting", "Generics & Type Erasure"],
                "pass_criteria": "Selects appropriate collection structures with O(1) performance",
                "diagnostic_questions": [
                    {
                        "id": "diag_java_3",
                        "prompt": "In Java Collections, what occurs in `HashMap` when multiple keys hash to the same bucket and collisions exceed TREEIFY_THRESHOLD (8)?",
                        "question_type": "multiple_choice",
                        "options": [
                            "The internal bucket structure converts from a linked list into a balanced Red-Black tree to maintain O(log n) worst-case lookups",
                            "The HashMap immediately throws an OutOfMemoryError and crashes",
                            "All older colliding keys are discarded and overwritten by the newest key",
                            "The table size is doubled and all existing entries are rehashed into an array of vectors"
                        ],
                        "correct_index": 0,
                        "explanation": "In Java 8+, HashMap converts linked lists in buckets to Red-Black Trees when collisions reach 8, reducing lookup time from O(n) to O(log n).",
                        "difficulty": "Advanced",
                        "selection_rationale": "Tests deep knowledge of data structure implementations in the Java standard library."
                    }
                ]
            }
        }
    },
    "sql": {
        "domain": "SQL",
        "description": "Relational Modeling, Query Optimization & Analytics",
        "skills": {
            "sql_basics": {
                "id": "sql_basics",
                "name": "Relational Schemas, Constraints & Joins",
                "category": "Foundations",
                "description": "Primary/Foreign keys, INNER/LEFT/FULL joins, GROUP BY, aggregations",
                "prerequisites": [],
                "estimated_hours": 3.5,
                "subtopics": ["Schema Constraints & Foreign Keys", "Join Varieties (INNER, LEFT, RIGHT, CROSS)", "Aggregation & HAVING"],
                "pass_criteria": "Writes multi-table relational queries with correct cardinality",
                "diagnostic_questions": [
                    {
                        "id": "diag_sql_1",
                        "prompt": "What is the difference between WHERE and HAVING in SQL aggregate queries?",
                        "question_type": "conceptual",
                        "options": [
                            "WHERE filters rows before grouping/aggregation; HAVING filters aggregated groups after GROUP BY",
                            "WHERE is used only with numerical fields, HAVING only with strings",
                            "HAVING is an older deprecated syntax for WHERE",
                            "There is no difference in execution order or semantics"
                        ],
                        "correct_index": 0,
                        "explanation": "WHERE filters source rows before aggregation; HAVING filters grouped result sets after GROUP BY.",
                        "difficulty": "Beginner",
                        "selection_rationale": "Verifies fundamental SQL query execution order understanding."
                    }
                ]
            },
            "sql_indexing": {
                "id": "sql_indexing",
                "name": "B-Tree Indexing, Query Plans & Performance",
                "category": "Optimization",
                "description": "EXPLAIN ANALYZE, B-Tree index scan vs seq scan, composite index column order",
                "prerequisites": ["sql_basics"],
                "estimated_hours": 5.0,
                "subtopics": ["B-Tree & Hash Indexes", "Composite Index Prefix Matching", "Execution Plans (Seq Scan vs Index Scan)"],
                "pass_criteria": "Optimizes queries to utilize index scans and minimize disk I/O",
                "diagnostic_questions": [
                    {
                        "id": "diag_sql_2",
                        "prompt": "Given a composite B-Tree index on (department_id, created_at), which query CANNOT efficiently use the index?",
                        "question_type": "debugging",
                        "options": [
                            "SELECT * FROM employees WHERE created_at = '2026-01-01' (skipping department_id)",
                            "SELECT * FROM employees WHERE department_id = 5 AND created_at = '2026-01-01'",
                            "SELECT * FROM employees WHERE department_id = 5",
                            "All of the above can fully utilize the index equally"
                        ],
                        "correct_index": 0,
                        "explanation": "Composite B-Tree indexes require the leading column (leftmost prefix) to seek effectively. Querying only created_at cannot use the index prefix.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Evaluates knowledge of index structure and query planner optimization."
                    }
                ]
            }
        }
    },
    "machine_learning": {
        "domain": "Machine Learning",
        "description": "Predictive Modeling, Deep Learning & LLM Systems",
        "skills": {
            "ml_foundations": {
                "id": "ml_foundations",
                "name": "Supervised Learning, Loss Functions & Evaluation",
                "category": "Foundations",
                "description": "Linear/Logistic regression, precision/recall, ROC-AUC, bias-variance tradeoff",
                "prerequisites": [],
                "estimated_hours": 4.0,
                "subtopics": ["Precision, Recall & F1-Score", "Bias-Variance Tradeoff", "Cross-Validation & Data Leakage"],
                "pass_criteria": "Selects appropriate evaluation metrics avoiding data leakage",
                "diagnostic_questions": [
                    {
                        "id": "diag_ml_1",
                        "prompt": "When evaluating a rare-disease diagnostic model where only 0.1% of patients have the condition, why is Accuracy an invalid metric?",
                        "question_type": "conceptual",
                        "options": [
                            "A dummy model predicting 'Negative' for all cases achieves 99.9% accuracy while finding zero true cases",
                            "Accuracy cannot be calculated on numerical datasets",
                            "Accuracy requires balanced regression targets",
                            "Accuracy only works for multi-class classification"
                        ],
                        "correct_index": 0,
                        "explanation": "In severe class imbalance, accuracy is misleading because a trivial majority-class classifier appears accurate while failing on the minority class. Precision, Recall, and PR-AUC are required.",
                        "difficulty": "Beginner",
                        "selection_rationale": "Verifies understanding of classification metrics and class imbalance."
                    }
                ]
            },
            "ml_deep_learning": {
                "id": "ml_deep_learning",
                "name": "Neural Networks, Backpropagation & PyTorch",
                "category": "Deep Learning",
                "description": "Activation functions, gradient descent, backpropagation, vanishing gradients, PyTorch tensors",
                "prerequisites": ["ml_foundations"],
                "estimated_hours": 6.5,
                "subtopics": ["Vanishing/Exploding Gradients", "Adam vs SGD Optimizer", "PyTorch Autograd & Computational Graphs"],
                "pass_criteria": "Builds and trains neural network architectures in PyTorch",
                "diagnostic_questions": [
                    {
                        "id": "diag_ml_2",
                        "prompt": "Why did ReLU largely replace Sigmoid as the default activation function in hidden layers of deep neural networks?",
                        "question_type": "conceptual",
                        "options": [
                            "ReLU mitigates vanishing gradients for positive inputs because its gradient is a constant 1, enabling deeper network training",
                            "ReLU restricts all outputs between 0 and 1",
                            "ReLU produces smooth differentiable curves everywhere including negative values",
                            "ReLU requires complex matrix exponentiation"
                        ],
                        "correct_index": 0,
                        "explanation": "Sigmoid saturates at both tails with gradients approaching 0, causing vanishing gradients in deep networks. ReLU has gradient 1 for x > 0.",
                        "difficulty": "Intermediate",
                        "selection_rationale": "Tests knowledge of neural network training dynamics and activation functions."
                    }
                ]
            }
        }
    }
}


def get_course_knowledge_graph(domain: str) -> Dict[str, Any]:
    dom_clean = domain.strip().lower()
    if "java" in dom_clean and "script" not in dom_clean:
        return COURSE_KNOWLEDGE_GRAPHS["java"]
    elif "sql" in dom_clean or "database" in dom_clean:
        return COURSE_KNOWLEDGE_GRAPHS["sql"]
    elif "ml" in dom_clean or "machine learning" in dom_clean or "ai" in dom_clean:
        return COURSE_KNOWLEDGE_GRAPHS["machine_learning"]
    else:
        return COURSE_KNOWLEDGE_GRAPHS["python"]
