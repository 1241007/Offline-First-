package com.offline_First.data.local

import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.CourseQuiz
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.QuizQuestion
import com.offline_First.domain.model.RevisionItem
import java.io.File
import kotlinx.coroutines.flow.Flow

class LocalCourseRepository(
    val progressStore: CourseProgressStore = CourseProgressStore()
) : CourseRepository {

    private val courseDetails: List<CourseDetail> = listOf(
        // 1. C++ Programming
        CourseDetail(
            id = "c-cpp",
            name = "C++ Programming",
            description = "High-performance systems and OOP.",
            icon = "C+",
            accent = CourseAccent.PRIMARY,
            category = "C++",
            level = "Intermediate",
            durationHours = 40,
            prerequisites = listOf("Basic computer literacy", "Problem-solving fundamentals"),
            modules = listOf(
                CourseModule(
                    id = "mod-cpp-1",
                    courseId = "c-cpp",
                    title = "C++ Core Fundamentals",
                    description = "Master variables, data types, control structures, and memory references.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-cpp-101",
                            moduleId = "mod-cpp-1",
                            courseId = "c-cpp",
                            title = "Basic Concepts & Types",
                            description = "Language syntax, declarations, basic types, and execution model.",
                            contentType = "article",
                            durationMinutes = 25,
                            displayOrder = 1,
                            resourceUrl = "https://en.cppreference.com/w/cpp/language/basic_concepts",
                            resourceProvider = "cppreference.com"
                        ),
                        LessonItem(
                            id = "les-cpp-102",
                            moduleId = "mod-cpp-1",
                            courseId = "c-cpp",
                            title = "Functions and Control Flow",
                            description = "Conditional statements, iterative loops, and modular function definitions.",
                            contentType = "article",
                            durationMinutes = 30,
                            displayOrder = 2,
                            resourceUrl = "https://www.learncpp.com/cpp-tutorial/introduction-to-functions/",
                            resourceProvider = "LearnCpp.com"
                        ),
                        LessonItem(
                            id = "les-cpp-103",
                            moduleId = "mod-cpp-1",
                            courseId = "c-cpp",
                            title = "Pointers & Memory Architecture",
                            description = "Stack vs heap memory, pointers, dereferencing, and address arithmetic.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 3,
                            resourceUrl = "https://www.learncpp.com/cpp-tutorial/introduction-to-pointers/",
                            resourceProvider = "LearnCpp.com"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-cpp-2",
                    courseId = "c-cpp",
                    title = "Object-Oriented Programming & Templates",
                    description = "Constructors, inheritance, polymorphism, and generic template programming.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-cpp-201",
                            moduleId = "mod-cpp-2",
                            courseId = "c-cpp",
                            title = "Classes and Encapsulation",
                            description = "Access specifiers, member functions, constructors, and RAII principles.",
                            contentType = "article",
                            durationMinutes = 40,
                            displayOrder = 1,
                            resourceUrl = "https://www.learncpp.com/cpp-tutorial/welcome-to-object-oriented-programming/",
                            resourceProvider = "LearnCpp.com"
                        ),
                        LessonItem(
                            id = "les-cpp-202",
                            moduleId = "mod-cpp-2",
                            courseId = "c-cpp",
                            title = "Polymorphism & Virtual Tables",
                            description = "Dynamic dispatch, pure virtual methods, and abstract classes.",
                            contentType = "quiz",
                            durationMinutes = 30,
                            displayOrder = 2,
                            resourceUrl = "https://en.cppreference.com/w/cpp/language/virtual",
                            resourceProvider = "cppreference.com",
                            quiz = CourseQuiz(
                                id = "quiz-cpp-2",
                                title = "C++ OOP & Memory Quiz",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "What keyword ensures dynamic dispatch for a base class method in C++?",
                                        options = listOf("static", "inline", "virtual", "override"),
                                        correctIndex = 2,
                                        explanation = "The 'virtual' keyword enables dynamic polymorphism via a vtable lookup at runtime."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "What does RAII stand for in C++?",
                                        options = listOf(
                                            "Resource Allocation Is Initialization",
                                            "Resource Acquisition Is Initialization",
                                            "Random Access Inlined Indexing",
                                            "Return And Invoke Instantly"
                                        ),
                                        correctIndex = 1,
                                        explanation = "RAII ties resource lifecycle (memory, file handles) to object lifetime via constructors and destructors."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 2. Data Structures and Algorithms
        CourseDetail(
            id = "c-dsa",
            name = "Data Structures & Algorithms",
            description = "Master computational problem-solving, complexity analysis, and competitive coding paradigms.",
            icon = "⌘",
            accent = CourseAccent.ACCENT,
            category = "Data Structures and Algorithms",
            level = "Intermediate",
            durationHours = 60,
            prerequisites = listOf("Basic programming experience in any language"),
            modules = listOf(
                CourseModule(
                    id = "mod-dsa-1",
                    courseId = "c-dsa",
                    title = "Complexity Analysis & Linear Structures",
                    description = "Asymptotic analysis, arrays, linked lists, stacks, and queues.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-dsa-101",
                            moduleId = "mod-dsa-1",
                            courseId = "c-dsa",
                            title = "Asymptotic Analysis & Big-O",
                            description = "Time and space complexity bounds (O, Ω, Θ).",
                            contentType = "article",
                            durationMinutes = 30,
                            displayOrder = 1,
                            resourceUrl = "https://en.wikipedia.org/wiki/Big_O_notation",
                            resourceProvider = "Wikipedia"
                        ),
                        LessonItem(
                            id = "les-dsa-102",
                            moduleId = "mod-dsa-1",
                            courseId = "c-dsa",
                            title = "Linked Lists and Pointer Traversal",
                            description = "Singly linked, doubly linked, and circular list operations.",
                            contentType = "article",
                            durationMinutes = 40,
                            displayOrder = 2,
                            resourceUrl = "https://www.geeksforgeeks.org/data-structures/linked-list/",
                            resourceProvider = "GeeksforGeeks"
                        ),
                        LessonItem(
                            id = "les-dsa-103",
                            moduleId = "mod-dsa-1",
                            courseId = "c-dsa",
                            title = "Hash Tables and Collision Resolution",
                            description = "Chaining, open addressing, and constant amortized lookups.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 3,
                            resourceUrl = "https://www.geeksforgeeks.org/hash-table-data-structure/",
                            resourceProvider = "GeeksforGeeks"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-dsa-2",
                    courseId = "c-dsa",
                    title = "Non-Linear Structures & Algorithmic Paradigms",
                    description = "Trees, graphs, divide & conquer, and dynamic programming.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-dsa-201",
                            moduleId = "mod-dsa-2",
                            courseId = "c-dsa",
                            title = "Binary Search Trees & Traversals",
                            description = "Inorder, preorder, postorder, and level-order search strategies.",
                            contentType = "article",
                            durationMinutes = 50,
                            displayOrder = 1,
                            resourceUrl = "https://www.geeksforgeeks.org/binary-search-tree-data-structure/",
                            resourceProvider = "GeeksforGeeks"
                        ),
                        LessonItem(
                            id = "les-dsa-202",
                            moduleId = "mod-dsa-2",
                            courseId = "c-dsa",
                            title = "Graph Traversal (BFS & DFS)",
                            description = "Breadth-first and depth-first searches with cycle detection.",
                            contentType = "article",
                            durationMinutes = 50,
                            displayOrder = 2,
                            resourceUrl = "https://www.geeksforgeeks.org/graph-data-structure-and-algorithms/",
                            resourceProvider = "GeeksforGeeks"
                        ),
                        LessonItem(
                            id = "les-dsa-203",
                            moduleId = "mod-dsa-2",
                            courseId = "c-dsa",
                            title = "Dynamic Programming Essentials",
                            description = "Optimal substructure, overlapping subproblems, memoization and tabulation.",
                            contentType = "quiz",
                            durationMinutes = 45,
                            displayOrder = 3,
                            resourceUrl = "https://en.wikipedia.org/wiki/Dynamic_programming",
                            resourceProvider = "Wikipedia",
                            quiz = CourseQuiz(
                                id = "quiz-dsa-1",
                                title = "DSA Concept Check",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "What is the worst-case lookup time in a balanced binary search tree (e.g. AVL, Red-Black)?",
                                        options = listOf("O(1)", "O(log n)", "O(n)", "O(n log n)"),
                                        correctIndex = 1,
                                        explanation = "Balanced BSTs guarantee tree height of O(log n), so lookups never exceed O(log n)."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "Which algorithm paradigm solves subproblems once and stores results in a lookup table?",
                                        options = listOf("Greedy", "Divide and Conquer", "Dynamic Programming", "Backtracking"),
                                        correctIndex = 2,
                                        explanation = "Dynamic programming optimizes recursive problems by caching overlapping subproblem results."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 3. Data Science
        CourseDetail(
            id = "c-ds",
            name = "Data Science",
            description = "Extract actionable intelligence from raw data using modern analytical tooling and statistical workflows.",
            icon = "DS",
            accent = CourseAccent.SECONDARY,
            category = "Data Science",
            level = "Intermediate",
            durationHours = 45,
            prerequisites = listOf("Basic Python knowledge", "High-school algebra and statistics"),
            modules = listOf(
                CourseModule(
                    id = "mod-ds-1",
                    courseId = "c-ds",
                    title = "Data Wrangling & Exploration",
                    description = "Vectorized arithmetic with NumPy and structured data analysis with Pandas.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-ds-101",
                            moduleId = "mod-ds-1",
                            courseId = "c-ds",
                            title = "NumPy for Numerical Computing",
                            description = "N-dimensional arrays, broadcasting, vectorization, and indexing.",
                            contentType = "article",
                            durationMinutes = 35,
                            displayOrder = 1,
                            resourceUrl = "https://numpy.org/doc/stable/user/quickstart.html",
                            resourceProvider = "NumPy Documentation"
                        ),
                        LessonItem(
                            id = "les-ds-102",
                            moduleId = "mod-ds-1",
                            courseId = "c-ds",
                            title = "Pandas Series & DataFrames",
                            description = "Data ingestion, handling missing values, filtering, and grouping.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 2,
                            resourceUrl = "https://pandas.pydata.org/docs/user_guide/10min.html",
                            resourceProvider = "Pandas Documentation"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-ds-2",
                    courseId = "c-ds",
                    title = "Visualization & Statistical Interpretation",
                    description = "Chart generation, distribution analysis, and statistical hypothesis testing.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-ds-201",
                            moduleId = "mod-ds-2",
                            courseId = "c-ds",
                            title = "Data Visualization with Matplotlib",
                            description = "Line plots, histograms, scatter plots, and figure customization.",
                            contentType = "article",
                            durationMinutes = 40,
                            displayOrder = 1,
                            resourceUrl = "https://matplotlib.org/stable/tutorials/introductory/quick_start.html",
                            resourceProvider = "Matplotlib Documentation"
                        ),
                        LessonItem(
                            id = "les-ds-202",
                            moduleId = "mod-ds-2",
                            courseId = "c-ds",
                            title = "Exploratory Data Analysis (EDA)",
                            description = "Uncovering patterns, anomalies, hypothesis testing, and summary statistics.",
                            contentType = "quiz",
                            durationMinutes = 35,
                            displayOrder = 2,
                            resourceUrl = "https://en.wikipedia.org/wiki/Exploratory_data_analysis",
                            resourceProvider = "Wikipedia",
                            quiz = CourseQuiz(
                                id = "quiz-ds-1",
                                title = "Data Science Foundations Quiz",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "Which Pandas function is most commonly used to inspect the first few rows of a DataFrame?",
                                        options = listOf("df.top()", "df.peek()", "df.head()", "df.first()"),
                                        correctIndex = 2,
                                        explanation = "df.head(n) returns the first n rows (default 5) of the DataFrame."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "What is the primary benefit of NumPy vectorization over standard Python loops?",
                                        options = listOf(
                                            "Automatic parallel multi-threading across networks",
                                            "Compiled C-level contiguous memory operations without loop overhead",
                                            "Strict static type safety at compile time",
                                            "Zero memory footprint"
                                        ),
                                        correctIndex = 1,
                                        explanation = "NumPy executes operations on contiguous memory buffers in optimized C routines."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 4. Python Programming
        CourseDetail(
            id = "c-py",
            name = "Python Programming",
            description = "Build a strong programming foundation with elegant, readable, and versatile Python code.",
            icon = "Py",
            accent = CourseAccent.PRIMARY,
            category = "Python",
            level = "Beginner",
            durationHours = 30,
            prerequisites = listOf("None - beginner friendly"),
            modules = listOf(
                CourseModule(
                    id = "mod-py-1",
                    courseId = "c-py",
                    title = "Python Syntax and Control Flow",
                    description = "Variables, types, logical operators, and loop constructs.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-py-101",
                            moduleId = "mod-py-1",
                            courseId = "c-py",
                            title = "An Informal Introduction to Python",
                            description = "Python numbers, strings, and terminal execution.",
                            contentType = "article",
                            durationMinutes = 20,
                            displayOrder = 1,
                            resourceUrl = "https://docs.python.org/3/tutorial/introduction.html",
                            resourceProvider = "Python Software Foundation"
                        ),
                        LessonItem(
                            id = "les-py-102",
                            moduleId = "mod-py-1",
                            courseId = "c-py",
                            title = "Control Flow Tools",
                            description = "if statements, for statements, the range() function, and break/continue.",
                            contentType = "article",
                            durationMinutes = 30,
                            displayOrder = 2,
                            resourceUrl = "https://docs.python.org/3/tutorial/controlflow.html",
                            resourceProvider = "Python Software Foundation"
                        ),
                        LessonItem(
                            id = "les-py-103",
                            moduleId = "mod-py-1",
                            courseId = "c-py",
                            title = "Defining Functions & Arguments",
                            description = "Function syntax, default argument values, and docstrings.",
                            contentType = "article",
                            durationMinutes = 30,
                            displayOrder = 3,
                            resourceUrl = "https://docs.python.org/3/tutorial/controlflow.html#defining-functions",
                            resourceProvider = "Python Software Foundation"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-py-2",
                    courseId = "c-py",
                    title = "Built-in Data Structures & Modules",
                    description = "Lists, tuples, dictionaries, sets, and modular imports.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-py-201",
                            moduleId = "mod-py-2",
                            courseId = "c-py",
                            title = "Data Structures in Detail",
                            description = "List comprehensions, the del statement, tuples, sets, and dictionaries.",
                            contentType = "article",
                            durationMinutes = 40,
                            displayOrder = 1,
                            resourceUrl = "https://docs.python.org/3/tutorial/datastructures.html",
                            resourceProvider = "Python Software Foundation"
                        ),
                        LessonItem(
                            id = "les-py-202",
                            moduleId = "mod-py-2",
                            courseId = "c-py",
                            title = "Modules and Standard Library",
                            description = "Organizing scripts into modules, packages, and importing functions.",
                            contentType = "quiz",
                            durationMinutes = 30,
                            displayOrder = 2,
                            resourceUrl = "https://docs.python.org/3/tutorial/modules.html",
                            resourceProvider = "Python Software Foundation",
                            quiz = CourseQuiz(
                                id = "quiz-py-1",
                                title = "Python Fundamentals Quiz",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "Which collection type in Python is ordered, mutable, and allows duplicate members?",
                                        options = listOf("Set", "Dictionary", "Tuple", "List"),
                                        correctIndex = 3,
                                        explanation = "Lists in Python maintain insertion order, support mutation (append/pop), and permit duplicates."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "What does the 'is' operator test for in Python?",
                                        options = listOf(
                                            "Value equality equivalent to ==",
                                            "Object identity (same memory address)",
                                            "Subclass relationship",
                                            "Variable scope"
                                        ),
                                        correctIndex = 1,
                                        explanation = "'is' checks whether two variables refer to the exact same object in memory."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 5. Machine Learning
        CourseDetail(
            id = "c-ml",
            name = "Machine Learning",
            description = "Train predictive models, understand mathematical foundations, and apply algorithms to real-world tasks.",
            icon = "ML",
            accent = CourseAccent.ACCENT,
            category = "Machine Learning",
            level = "Advanced",
            durationHours = 55,
            prerequisites = listOf("Python proficiency", "Linear algebra and calculus basics"),
            modules = listOf(
                CourseModule(
                    id = "mod-ml-1",
                    courseId = "c-ml",
                    title = "Supervised Learning Fundamentals",
                    description = "Regression, classification, and loss function minimization.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-ml-101",
                            moduleId = "mod-ml-1",
                            courseId = "c-ml",
                            title = "Getting Started with Machine Learning",
                            description = "Problem formulation, feature matrices, and training workflows.",
                            contentType = "article",
                            durationMinutes = 30,
                            displayOrder = 1,
                            resourceUrl = "https://scikit-learn.org/stable/getting_started.html",
                            resourceProvider = "Scikit-Learn Documentation"
                        ),
                        LessonItem(
                            id = "les-ml-102",
                            moduleId = "mod-ml-1",
                            courseId = "c-ml",
                            title = "Linear & Logistic Models",
                            description = "Ordinary least squares, gradient descent, and sigmoid activation for classification.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 2,
                            resourceUrl = "https://scikit-learn.org/stable/modules/linear_model.html",
                            resourceProvider = "Scikit-Learn Documentation"
                        ),
                        LessonItem(
                            id = "les-ml-103",
                            moduleId = "mod-ml-1",
                            courseId = "c-ml",
                            title = "Decision Trees & Ensemble Methods",
                            description = "Gini impurity, entropy, bagging, random forests, and gradient boosting.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 3,
                            resourceUrl = "https://scikit-learn.org/stable/modules/tree.html",
                            resourceProvider = "Scikit-Learn Documentation"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-ml-2",
                    courseId = "c-ml",
                    title = "Evaluation, Tuning & Neural Networks",
                    description = "Cross-validation, bias-variance tradeoff, and multi-layer perceptrons.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-ml-201",
                            moduleId = "mod-ml-2",
                            courseId = "c-ml",
                            title = "Cross-Validation and Hyperparameter Tuning",
                            description = "K-fold cross-validation, grid search, and avoiding data leakage.",
                            contentType = "article",
                            durationMinutes = 35,
                            displayOrder = 1,
                            resourceUrl = "https://scikit-learn.org/stable/modules/cross_validation.html",
                            resourceProvider = "Scikit-Learn Documentation"
                        ),
                        LessonItem(
                            id = "les-ml-202",
                            moduleId = "mod-ml-2",
                            courseId = "c-ml",
                            title = "Neural Networks (Multi-layer Perceptron)",
                            description = "Forward propagation, backpropagation, and loss convergence.",
                            contentType = "quiz",
                            durationMinutes = 45,
                            displayOrder = 2,
                            resourceUrl = "https://scikit-learn.org/stable/modules/neural_networks_supervised.html",
                            resourceProvider = "Scikit-Learn Documentation",
                            quiz = CourseQuiz(
                                id = "quiz-ml-1",
                                title = "Machine Learning Checkpoint",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "What condition occurs when a model performs exceptionally well on training data but poorly on unseen test data?",
                                        options = listOf("Underfitting", "Overfitting", "Zero bias", "Data normalization"),
                                        correctIndex = 1,
                                        explanation = "Overfitting happens when a model learns noise and specifics of training data, harming generalization."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "Which activation function is traditionally used in binary classification output layers?",
                                        options = listOf("ReLU", "Linear", "Sigmoid", "Softmax"),
                                        correctIndex = 2,
                                        explanation = "The Sigmoid function squashes output into the [0, 1] range, representing a Bernoulli probability."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 6. Full Stack Development
        CourseDetail(
            id = "c-fs",
            name = "Full Stack Development",
            description = "Build full-fledged, scalable web applications from responsive frontends to RESTful backend services.",
            icon = "</>",
            accent = CourseAccent.PRIMARY,
            category = "Full Stack Development",
            level = "Intermediate",
            durationHours = 70,
            prerequisites = listOf("Basic HTML & web concepts"),
            modules = listOf(
                CourseModule(
                    id = "mod-fs-1",
                    courseId = "c-fs",
                    title = "Frontend Fundamentals & Modern JavaScript",
                    description = "Semantic HTML, responsive CSS layouts, and modern ES6+ programming.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-fs-101",
                            moduleId = "mod-fs-1",
                            courseId = "c-fs",
                            title = "Structuring the Web with HTML5",
                            description = "Semantic markup, forms, accessibility, and SEO foundations.",
                            contentType = "article",
                            durationMinutes = 25,
                            displayOrder = 1,
                            resourceUrl = "https://developer.mozilla.org/en-US/docs/Learn/HTML",
                            resourceProvider = "MDN Web Docs"
                        ),
                        LessonItem(
                            id = "les-fs-102",
                            moduleId = "mod-fs-1",
                            courseId = "c-fs",
                            title = "Modern CSS: Flexbox & Grid",
                            description = "Fluid layouts, media queries, CSS variables, and modern styling techniques.",
                            contentType = "article",
                            durationMinutes = 35,
                            displayOrder = 2,
                            resourceUrl = "https://developer.mozilla.org/en-US/docs/Learn/CSS",
                            resourceProvider = "MDN Web Docs"
                        ),
                        LessonItem(
                            id = "les-fs-103",
                            moduleId = "mod-fs-1",
                            courseId = "c-fs",
                            title = "JavaScript ES6+ Language Essentials",
                            description = "Promises, async/await, closures, fetch API, and DOM manipulation.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 3,
                            resourceUrl = "https://developer.mozilla.org/en-US/docs/Learn/JavaScript",
                            resourceProvider = "MDN Web Docs"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-fs-2",
                    courseId = "c-fs",
                    title = "Backend APIs and Database Integration",
                    description = "REST APIs, asynchronous request handling, and relational databases.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-fs-201",
                            moduleId = "mod-fs-2",
                            courseId = "c-fs",
                            title = "RESTful APIs with FastAPI",
                            description = "Path parameters, request validation, Pydantic schemas, and OpenAPI docs.",
                            contentType = "article",
                            durationMinutes = 45,
                            displayOrder = 1,
                            resourceUrl = "https://fastapi.tiangolo.com/tutorial/",
                            resourceProvider = "FastAPI Documentation"
                        ),
                        LessonItem(
                            id = "les-fs-202",
                            moduleId = "mod-fs-2",
                            courseId = "c-fs",
                            title = "PostgreSQL & Database Modeling",
                            description = "Relational schema design, indexes, transactions, and foreign keys.",
                            contentType = "quiz",
                            durationMinutes = 40,
                            displayOrder = 2,
                            resourceUrl = "https://www.postgresql.org/docs/current/tutorial.html",
                            resourceProvider = "PostgreSQL Documentation",
                            quiz = CourseQuiz(
                                id = "quiz-fs-1",
                                title = "Full Stack Fundamentals Quiz",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "Which HTTP method is idempotent and intended to completely replace an existing resource?",
                                        options = listOf("POST", "PATCH", "PUT", "DELETE"),
                                        correctIndex = 2,
                                        explanation = "PUT is idempotent and replaces the target resource with the uploaded payload."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "What is the primary role of CORS in browser web applications?",
                                        options = listOf(
                                            "Accelerating image rendering in the DOM",
                                            "Restricting web pages from making cross-origin HTTP requests unless permitted by server headers",
                                            "Compressing JavaScript bundle sizes",
                                            "Encrypting database connections"
                                        ),
                                        correctIndex = 1,
                                        explanation = "Cross-Origin Resource Sharing (CORS) enforces browser security boundaries on API calls across origins."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 7. Software Development Engineer (SDE)
        CourseDetail(
            id = "c-sde",
            name = "Software Development Engineer (SDE)",
            description = "Master production-grade system architecture, clean design patterns, and engineering principles.",
            icon = "SDE",
            accent = CourseAccent.SECONDARY,
            category = "Software Development Engineer (SDE)",
            level = "Advanced",
            durationHours = 80,
            prerequisites = listOf("Solid understanding of DSA", "Backend programming foundation"),
            modules = listOf(
                CourseModule(
                    id = "mod-sde-1",
                    courseId = "c-sde",
                    title = "Software Architecture & Clean Design",
                    description = "SOLID principles, design patterns, and decoupled architectural layers.",
                    displayOrder = 1,
                    lessons = listOf(
                        LessonItem(
                            id = "les-sde-101",
                            moduleId = "mod-sde-1",
                            courseId = "c-sde",
                            title = "SOLID Design Principles",
                            description = "Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.",
                            contentType = "article",
                            durationMinutes = 40,
                            displayOrder = 1,
                            resourceUrl = "https://en.wikipedia.org/wiki/SOLID",
                            resourceProvider = "Wikipedia"
                        ),
                        LessonItem(
                            id = "les-sde-102",
                            moduleId = "mod-sde-1",
                            courseId = "c-sde",
                            title = "Gang of Four Design Patterns",
                            description = "Factory, Singleton, Observer, Strategy, and Adapter patterns in practice.",
                            contentType = "article",
                            durationMinutes = 50,
                            displayOrder = 2,
                            resourceUrl = "https://refactoring.guru/design-patterns",
                            resourceProvider = "Refactoring Guru"
                        )
                    )
                ),
                CourseModule(
                    id = "mod-sde-2",
                    courseId = "c-sde",
                    title = "Distributed Systems & Engineering Practices",
                    description = "System scalability, caching strategies, CI/CD, and reliability engineering.",
                    displayOrder = 2,
                    lessons = listOf(
                        LessonItem(
                            id = "les-sde-201",
                            moduleId = "mod-sde-2",
                            courseId = "c-sde",
                            title = "System Design & Scalability Principles",
                            description = "Horizontal scaling, load balancers, database sharding, and caching hierarchies.",
                            contentType = "article",
                            durationMinutes = 60,
                            displayOrder = 1,
                            resourceUrl = "https://github.com/donnemartin/system-design-primer",
                            resourceProvider = "System Design Primer"
                        ),
                        LessonItem(
                            id = "les-sde-202",
                            moduleId = "mod-sde-2",
                            courseId = "c-sde",
                            title = "CI/CD & Production Testing Strategies",
                            description = "Automated test pyramids, continuous integration pipelines, and deployment automation.",
                            contentType = "quiz",
                            durationMinutes = 40,
                            displayOrder = 2,
                            resourceUrl = "https://docs.github.com/en/actions",
                            resourceProvider = "GitHub Docs",
                            quiz = CourseQuiz(
                                id = "quiz-sde-1",
                                title = "SDE Architecture & Design Check",
                                questions = listOf(
                                    QuizQuestion(
                                        id = 1,
                                        question = "Which SOLID principle asserts that high-level modules should not depend on low-level modules, but rather on abstractions?",
                                        options = listOf(
                                            "Single Responsibility Principle",
                                            "Open-Closed Principle",
                                            "Interface Segregation Principle",
                                            "Dependency Inversion Principle"
                                        ),
                                        correctIndex = 3,
                                        explanation = "The Dependency Inversion Principle decouples high-level policy from low-level implementation details."
                                    ),
                                    QuizQuestion(
                                        id = 2,
                                        question = "In distributed systems, what does the CAP theorem state is impossible to guarantee simultaneously in the presence of network partitions?",
                                        options = listOf(
                                            "Consistency and Availability",
                                            "Concurrency and Atomicity",
                                            "Compression and Authentication",
                                            "Throughput and Latency"
                                        ),
                                        correctIndex = 0,
                                        explanation = "Under network partition (P), a distributed system must choose between Consistency (C) and Availability (A)."
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private val courseDetailsMap: Map<String, CourseDetail> =
        courseDetails.associateBy { it.id }

    private val allCourses: List<Course> =
        courseDetails.map { it.toCourse() }

    init {
        // Register local courses for identifier normalization
        courseDetails.forEach { CourseIdNormalizer.registerLocalCourse(it) }
    }

    private fun Course.withCurrentProgress(): Course {
        val completed = progressStore.getCompletedLessonCount(id)
        val prog = if (totalLessons > 0) (completed.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f) else 0f
        return copy(completedLessons = completed, progress = prog)
    }

    fun bindStorageFile(file: File) {
        progressStore.bindStorageFile(file)
    }

    override fun observeProgressUpdates(): Flow<Long> = progressStore.progressUpdates

    override suspend fun getCompletedLessonCount(courseId: String): Int =
        progressStore.getCompletedLessonCount(courseId)

    override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> {
        val list = if (limit != null) {
            allCourses.drop(offset).take(limit)
        } else if (offset > 0) {
            allCourses.drop(offset)
        } else {
            allCourses
        }
        return Result.success(list.map { it.withCurrentProgress() })
    }

    override suspend fun getFeaturedCourses(): Result<List<Course>> {
        // Feature Python, DSA, and Full Stack by ID
        val featuredIds = setOf("c-py", "c-dsa", "c-fs")
        val featured = allCourses.filter { it.id in featuredIds }
        return Result.success(featured.map { it.withCurrentProgress() })
    }

    override suspend fun getCoursesByCategory(categorySlug: String): Result<List<Course>> {
        val filtered = allCourses.filter {
            it.category.equals(categorySlug, ignoreCase = true) ||
            it.id.equals(categorySlug, ignoreCase = true) ||
            CourseIdNormalizer.isSameCourse(it.id, categorySlug)
        }
        return Result.success(filtered.map { it.withCurrentProgress() })
    }

    override suspend fun getCourseDetail(courseId: String): Result<CourseDetail?> {
        val resolvedId = CourseIdNormalizer.resolveToLocalId(courseId) ?: courseId
        val base = courseDetailsMap[resolvedId]
            ?: courseDetails.firstOrNull { CourseIdNormalizer.isSameCourse(it.id, courseId) }
            ?: return Result.success(null)
        val updatedModules = base.modules.map { module ->
            module.copy(
                lessons = module.lessons.map { lesson ->
                    lesson.copy(
                        isCompleted = progressStore.isLessonCompleted(courseId, lesson.id),
                        isMarkedForRevision = progressStore.isMarkedForRevision(courseId, lesson.id)
                    )
                }
            )
        }
        return Result.success(base.copy(modules = updatedModules))
    }

    override suspend fun markLessonCompleted(courseId: String, lessonId: String, completed: Boolean): Result<Unit> {
        progressStore.markLessonCompleted(courseId, lessonId, completed)
        return Result.success(Unit)
    }

    override suspend fun isLessonCompleted(courseId: String, lessonId: String): Boolean {
        return progressStore.isLessonCompleted(courseId, lessonId)
    }

    override suspend fun getCompletedLessonCount(courseId: String, lessonIds: Collection<String>): Int {
        return progressStore.getCompletedLessonCount(courseId, lessonIds)
    }

    override suspend fun getCourseProgressPercentage(courseId: String, lessonIds: Collection<String>): Float {
        return progressStore.getCourseProgressPercentage(courseId, lessonIds)
    }

    override suspend fun isCourseCompleted(courseId: String, lessonIds: Collection<String>): Boolean {
        return progressStore.isCourseCompleted(courseId, lessonIds)
    }

    override suspend fun getCertificate(courseId: String): CourseCertificate? {
        return progressStore.getCertificate(courseId)
    }

    override suspend fun getAllCertificates(): List<CourseCertificate> {
        return progressStore.getAllCertificates()
    }

    override suspend fun issueCertificate(
        courseId: String,
        courseName: String,
        learnerName: String,
        totalLessons: Int
    ): CourseCertificate? {
        return progressStore.issueCertificate(courseId, courseName, learnerName, totalLessons)
    }

    override suspend fun addRevisionItem(item: RevisionItem): Result<Unit> {
        progressStore.addRevisionItem(item)
        return Result.success(Unit)
    }

    override suspend fun removeRevisionItem(courseId: String, lessonId: String): Result<Unit> {
        progressStore.removeRevisionItem(courseId, lessonId)
        return Result.success(Unit)
    }

    override suspend fun isMarkedForRevision(courseId: String, lessonId: String): Boolean {
        return progressStore.isMarkedForRevision(courseId, lessonId)
    }

    override suspend fun getRevisionItems(): Result<List<RevisionItem>> {
        return Result.success(progressStore.getRevisionItems())
    }
}
