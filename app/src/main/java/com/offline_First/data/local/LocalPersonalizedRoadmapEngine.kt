package com.offline_First.data.local

import com.offline_First.domain.model.*
import java.util.UUID
import kotlin.math.ceil

/**
 * Offline-first local diagnostic engine and personalized roadmap generator.
 * Seamlessly provides domain-aware assessments and DAG roadmap generation on-device
 * supporting Python, Java, SQL, Machine Learning, and Web tracks.
 */
object LocalPersonalizedRoadmapEngine {

    private var activeSession: AssessmentSessionState? = null
    private val savedRoadmaps = mutableListOf<PersonalizedRoadmapDetail>()

    fun getActiveSession(): AssessmentSessionState? = activeSession
    fun getSession(sessionId: String): AssessmentSessionState? = activeSession?.takeIf { it.sessionId == sessionId }

    private val PYTHON_QUESTIONS = listOf(
        AssessmentQuestion(
            id = "q_py_basics_1",
            topicTag = "python_basics",
            questionType = AssessmentQuestionType.CODE_OUTPUT,
            prompt = "What will print(type(4 / 2)) output in Python 3?",
            codeSnippet = "print(type(4 / 2))",
            options = listOf("<class 'int'>", "<class 'float'>", "<class 'double'>", "<class 'number'>"),
            correctOptionIndex = 1,
            difficultyLevel = 1,
            explanation = "In Python 3, division with '/' always yields a float even when the division has no remainder."
        ),
        AssessmentQuestion(
            id = "q_py_loops_1",
            topicTag = "python_loops",
            questionType = AssessmentQuestionType.CODE_OUTPUT,
            prompt = "What is the final value printed by this loop?",
            codeSnippet = "total = 0\nfor i in range(1, 5):\n    if i == 3:\n        continue\n    total += i\nprint(total)",
            options = listOf("7", "10", "6", "8"),
            correctOptionIndex = 0,
            difficultyLevel = 1,
            explanation = "range(1, 5) produces 1, 2, 3, 4. When i is 3, continue skips the addition, resulting in 1 + 2 + 4 = 7."
        ),
        AssessmentQuestion(
            id = "q_py_funcs_1",
            topicTag = "python_functions",
            questionType = AssessmentQuestionType.DEBUGGING,
            prompt = "What issue exists in this function definition with a default argument?",
            codeSnippet = "def add_item(item, target_list=[]):\n    target_list.append(item)\n    return target_list",
            options = listOf(
                "SyntaxError on function header",
                "Mutable default argument persists and mutates across calls",
                "TypeError: list has no method append",
                "Function cannot return target_list"
            ),
            correctOptionIndex = 1,
            difficultyLevel = 2,
            explanation = "Default argument expressions are evaluated once at function definition time. Mutable defaults retain mutations across calls."
        ),
        AssessmentQuestion(
            id = "q_py_ds_1",
            topicTag = "python_data_structures",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "What is the average time complexity of checking membership ('x in s') in a Python set vs list?",
            options = listOf(
                "O(1) for set, O(n) for list",
                "O(n) for set, O(1) for list",
                "O(log n) for set, O(1) for list",
                "O(1) for both"
            ),
            correctOptionIndex = 0,
            difficultyLevel = 2,
            explanation = "Python sets are implemented using hash tables with average O(1) membership test, whereas lists require linear O(n) scan."
        ),
        AssessmentQuestion(
            id = "q_py_oop_1",
            topicTag = "python_oop",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "What is the primary purpose of super().__init__() in a Python derived class?",
            options = listOf(
                "Instantiates a singleton instance",
                "Invokes the constructor/initializer of the parent class",
                "Destroys the current instance",
                "Creates private method attributes"
            ),
            correctOptionIndex = 1,
            difficultyLevel = 2,
            explanation = "super() delegates method calls to the superclass hierarchy, ensuring proper initialization of inherited state."
        ),
        AssessmentQuestion(
            id = "q_py_adv_1",
            topicTag = "python_advanced",
            questionType = AssessmentQuestionType.CODE_OUTPUT,
            prompt = "What is printed by next(gen) from this generator expression?",
            codeSnippet = "gen = (x * 2 for x in [1, 2, 3])\nprint(next(gen))",
            options = listOf("[2, 4, 6]", "1", "2", "StopIteration"),
            correctOptionIndex = 2,
            difficultyLevel = 3,
            explanation = "Generator expressions compute values lazily. The first call to next() yields 1 * 2 = 2."
        )
    )

    private val JAVA_QUESTIONS = listOf(
        AssessmentQuestion(
            id = "q_java_oop_1",
            topicTag = "java_oop",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "In Java, what is the key difference between an interface and an abstract class?",
            options = listOf(
                "A class can implement multiple interfaces, but extend only one class",
                "Interfaces can have instance state fields, abstract classes cannot",
                "Abstract classes cannot have concrete method implementations",
                "Interfaces cannot have default methods"
            ),
            correctOptionIndex = 0,
            difficultyLevel = 1,
            explanation = "Java supports multiple interface implementation, but only single class inheritance."
        ),
        AssessmentQuestion(
            id = "q_java_mem_1",
            topicTag = "java_memory",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "Where are Java object instances allocated in JVM runtime memory?",
            options = listOf("Method Area", "Stack Frame", "Heap Space", "PC Register"),
            correctOptionIndex = 2,
            difficultyLevel = 2,
            explanation = "In the JVM, all object instances are dynamically allocated on the shared Heap space."
        ),
        AssessmentQuestion(
            id = "q_java_streams_1",
            topicTag = "java_streams",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "What characterizes intermediate operations in the Java Stream API?",
            options = listOf(
                "They execute eagerly and close the stream",
                "They are lazy and return a new Stream without processing elements until a terminal operation is invoked",
                "They mutate the underlying source collection directly",
                "They can only process parallel streams"
            ),
            correctOptionIndex = 1,
            difficultyLevel = 2,
            explanation = "Intermediate stream operations like filter() and map() are evaluated lazily upon terminal invocation."
        )
    )

    private val SQL_QUESTIONS = listOf(
        AssessmentQuestion(
            id = "q_sql_join_1",
            topicTag = "sql_joins",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "How does an INNER JOIN differ from a LEFT OUTER JOIN in SQL?",
            options = listOf(
                "INNER returns only matched rows from both tables; LEFT includes all rows from the left table with NULLs for unmatched right rows",
                "INNER returns all rows from right table; LEFT returns matches only",
                "They produce identical results if both tables have primary keys",
                "LEFT JOIN is only used with aggregate functions"
            ),
            correctOptionIndex = 0,
            difficultyLevel = 1,
            explanation = "LEFT JOIN preserves all rows from the left table regardless of whether matching rows exist on the right."
        ),
        AssessmentQuestion(
            id = "q_sql_group_1",
            topicTag = "sql_aggregations",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "When must a HAVING clause be used instead of WHERE in SQL?",
            options = listOf(
                "When filtering by string columns",
                "When filtering results based on an aggregate function (e.g. COUNT, SUM)",
                "When sorting records descending",
                "When joining more than two tables"
            ),
            correctOptionIndex = 1,
            difficultyLevel = 2,
            explanation = "WHERE filters rows before aggregation; HAVING filters groups after aggregate calculation."
        ),
        AssessmentQuestion(
            id = "q_sql_idx_1",
            topicTag = "sql_indexing",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "What is the primary trade-off of adding a B-Tree index to a SQL column?",
            options = listOf(
                "Disables foreign key constraints",
                "Speeds up equality and range SELECT queries but increases write (INSERT/UPDATE) overhead and disk usage",
                "Forces queries to run sequentially without parallel workers",
                "Allows duplicate primary key values"
            ),
            correctOptionIndex = 1,
            difficultyLevel = 2,
            explanation = "Indexes accelerate lookup performance at the expense of write latency and storage."
        )
    )

    private val ML_QUESTIONS = listOf(
        AssessmentQuestion(
            id = "q_ml_reg_1",
            topicTag = "ml_regularization",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "Which technique is most effective for mitigating overfitting in machine learning models?",
            options = listOf(
                "Increasing model complexity and training iterations indefinitely",
                "Removing validation and test sets",
                "Applying regularization (L1/L2) and early stopping",
                "Duplicating noisy training records"
            ),
            correctOptionIndex = 2,
            difficultyLevel = 1,
            explanation = "Regularization penalizes large parameter weights, preventing the model from fitting training noise."
        ),
        AssessmentQuestion(
            id = "q_ml_metrics_1",
            topicTag = "ml_evaluation",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "In fraud detection or disease screening where false negatives are critical, which metric should be prioritized?",
            options = listOf("Accuracy", "Precision", "Recall (Sensitivity)", "Mean Squared Error"),
            correctOptionIndex = 2,
            difficultyLevel = 2,
            explanation = "Recall measures the proportion of actual positives correctly identified, minimizing false negatives."
        )
    )

    private fun getQuestionsForDomain(domain: String): List<AssessmentQuestion> {
        val dom = domain.lowercase()
        return when {
            dom.contains("java") || dom.contains("spring") -> JAVA_QUESTIONS
            dom.contains("sql") || dom.contains("data") || dom.contains("postgres") -> SQL_QUESTIONS
            dom.contains("ml") || dom.contains("machine") || dom.contains("ai") || dom.contains("deep") -> ML_QUESTIONS
            else -> PYTHON_QUESTIONS
        }
    }

    private val PYTHON_CURRICULUM_TEMPLATE = listOf(
        TemplateTopic("python_intro", "Python Setup & Syntax Basics", "Interpreter, scripts, comments, print statements, and PEP 8 conventions.", 25, emptyList()),
        TemplateTopic("python_basics", "Variables, Numbers & Strings", "Data types, type casting, string methods, and formatted strings.", 35, listOf("python_intro")),
        TemplateTopic("python_control", "Conditionals & Boolean Logic", "if-elif-else statements, truthiness, comparison, and logical operators.", 30, listOf("python_basics")),
        TemplateTopic("python_loops", "Loops & Iterations", "for and while loops, break, continue, range(), and enumerate().", 45, listOf("python_control")),
        TemplateTopic("python_functions", "Functions, Arguments & Scope", "Parameters, *args, **kwargs, return values, and LEGB variable scope.", 50, listOf("python_loops")),
        TemplateTopic("python_data_structures", "Lists, Tuples & Sets", "Sequence operations, indexing, slicing, mutability, and set math.", 60, listOf("python_functions")),
        TemplateTopic("python_dicts", "Dictionaries & Hash Maps", "Key-value mapping, dict methods, dictionary views, and nested lookups.", 45, listOf("python_data_structures")),
        TemplateTopic("python_exceptions", "Error & Exception Handling", "try, except, else, finally, and raising custom exceptions.", 40, listOf("python_functions")),
        TemplateTopic("python_files", "File I/O & Context Managers", "Reading and writing files safely using 'with' statement context managers.", 35, listOf("python_dicts", "python_exceptions")),
        TemplateTopic("python_oop", "Object-Oriented Programming: Classes", "Classes, instances, self, __init__, instance vs class attributes, and methods.", 65, listOf("python_functions", "python_dicts")),
        TemplateTopic("python_oop_adv", "OOP Inheritance & Polymorphism", "Single/multiple inheritance, super(), dunder methods, and method overriding.", 55, listOf("python_oop")),
        TemplateTopic("python_advanced", "Comprehensions & Generators", "List/dict comprehensions, generator functions with yield, and memory efficiency.", 50, listOf("python_data_structures", "python_functions")),
        TemplateTopic("python_modules", "Modules, Packages & Virtualenvs", "Import system, standard library highlights, pip, venv, and package structure.", 40, listOf("python_files")),
        TemplateTopic("python_capstone", "Capstone Project: End-to-End Application", "Integrate data structures, OOP, file handling, and error checking into a real project.", 90, listOf("python_oop_adv", "python_advanced", "python_modules"))
    )

    private val JAVA_CURRICULUM_TEMPLATE = listOf(
        TemplateTopic("java_syntax", "Java Environment & Syntax Lab", "JDK setup, compiler (javac), JVM execution, primitive types, and operators.", 30, emptyList()),
        TemplateTopic("java_oop", "Object-Oriented Programming & Interfaces", "Classes, encapsulation, inheritance, interfaces, abstract classes, and polymorphism.", 50, listOf("java_syntax")),
        TemplateTopic("java_collections", "Collections Framework & Generics", "List, Set, Map hierarchies, ArrayList vs LinkedList, and generic type safety.", 45, listOf("java_oop")),
        TemplateTopic("java_exceptions", "Exception Handling & Resource Management", "Checked vs unchecked exceptions, try-with-resources, and custom exception types.", 35, listOf("java_collections")),
        TemplateTopic("java_streams", "Stream API & Lambdas", "Functional interfaces, map-filter-reduce pipelines, and collector aggregations.", 50, listOf("java_collections")),
        TemplateTopic("java_concurrency", "Multithreading & Concurrency", "Thread lifecycle, synchronization, ExecutorService, and atomic variables.", 60, listOf("java_streams")),
        TemplateTopic("java_persistence", "Data Persistence with JPA / Hibernate", "Entity modeling, ORM mappings, repositories, and transaction isolation.", 60, listOf("java_concurrency")),
        TemplateTopic("java_capstone", "Enterprise Spring Boot Microservice", "Build, containerize, and test production REST service with automated tests.", 90, listOf("java_persistence"))
    )

    private val SQL_CURRICULUM_TEMPLATE = listOf(
        TemplateTopic("sql_modeling", "Relational Schema Design & DDL", "3NF normalization, tables, primary keys, and foreign key constraints.", 30, emptyList()),
        TemplateTopic("sql_queries", "SELECT Queries & Filtering", "Projection, WHERE filtering, logical operators, and NULL handling.", 35, listOf("sql_modeling")),
        TemplateTopic("sql_joins", "Multi-Table Relational JOINs", "INNER, LEFT, RIGHT, FULL OUTER, and CROSS joins with foreign keys.", 45, listOf("sql_queries")),
        TemplateTopic("sql_aggregations", "GROUP BY & Analytical Aggregations", "COUNT, SUM, AVG, MIN, MAX with GROUP BY and HAVING filters.", 40, listOf("sql_joins")),
        TemplateTopic("sql_indexing", "B-Tree Indexes & Query Optimization", "Index types, EXPLAIN execution plans, index scans vs sequential scans.", 45, listOf("sql_aggregations")),
        TemplateTopic("sql_transactions", "ACID Transactions & Concurrency", "BEGIN, COMMIT, ROLLBACK, dirty reads, and transaction isolation levels.", 40, listOf("sql_indexing")),
        TemplateTopic("sql_window", "Window Functions & Common Table Expressions", "ROW_NUMBER, RANK, DENSE_RANK, OVER partitions, and recursive CTEs.", 50, listOf("sql_transactions")),
        TemplateTopic("sql_capstone", "Analytics Warehouse & Reporting Capstone", "Design analytical reporting schema and author production business intelligence queries.", 90, listOf("sql_window"))
    )

    private val ML_CURRICULUM_TEMPLATE = listOf(
        TemplateTopic("ml_foundations", "Mathematical Foundations for ML", "Linear algebra, matrix operations, multivariate calculus, and probability.", 40, emptyList()),
        TemplateTopic("ml_data_prep", "Data Preprocessing & Feature Engineering", "Handling missing values, scaling, one-hot encoding, and train-test splits.", 45, listOf("ml_foundations")),
        TemplateTopic("ml_supervised", "Supervised Learning Algorithms", "Linear regression, logistic regression, decision trees, and random forests.", 50, listOf("ml_data_prep")),
        TemplateTopic("ml_evaluation", "Model Evaluation & Cross-Validation", "Confusion matrix, Precision, Recall, F1, ROC-AUC, and k-fold validation.", 40, listOf("ml_supervised")),
        TemplateTopic("ml_regularization", "Regularization & Hyperparameter Tuning", "L1/L2 penalties, Dropout, GridSearchCV, and preventing overfitting.", 45, listOf("ml_evaluation")),
        TemplateTopic("ml_deep_learning", "Deep Learning with PyTorch", "Tensors, autograd, neural network layers, loss functions, and backprop.", 60, listOf("ml_regularization")),
        TemplateTopic("ml_capstone", "End-to-End ML Pipeline Capstone", "Train, evaluate, and deploy a production machine learning service with API inference.", 90, listOf("ml_deep_learning"))
    )

    private fun getTemplateForDomain(domain: String): List<TemplateTopic> {
        val dom = domain.lowercase()
        return when {
            dom.contains("java") || dom.contains("spring") -> JAVA_CURRICULUM_TEMPLATE
            dom.contains("sql") || dom.contains("data") || dom.contains("postgres") -> SQL_CURRICULUM_TEMPLATE
            dom.contains("ml") || dom.contains("machine") || dom.contains("ai") || dom.contains("deep") -> ML_CURRICULUM_TEMPLATE
            else -> PYTHON_CURRICULUM_TEMPLATE
        }
    }

    private data class TemplateTopic(
        val tag: String,
        val title: String,
        val desc: String,
        val minutes: Int,
        val prereqTags: List<String>
    )

    fun startAssessment(domain: String = "Python"): AssessmentSessionState {
        val session = AssessmentSessionState(
            sessionId = UUID.randomUUID().toString(),
            domain = domain,
            phase = AssessmentPhase.PROFILE_COLLECTION,
            questionNumber = 1,
            totalQuestions = 6,
            currentQuestion = AssessmentQuestion(
                id = "prof_goal",
                topicTag = "profile_goal",
                questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                prompt = "What is your primary goal with $domain?",
                options = listOf(
                    "Get a Software Developer / Engineering job",
                    "Build production applications and projects",
                    "Deepen technical architecture & performance",
                    "Prepare for technical interviews / certifications"
                ),
                explanation = "Your goal helps us tailor milestone priorities and project themes."
            ),
            masteryMap = emptyMap()
        )
        activeSession = session
        return session
    }

    fun submitAnswer(sessionId: String, answer: String, quizSelectedIndex: Int? = null): AssessmentSessionState {
        val curr = activeSession?.takeIf { it.sessionId == sessionId } ?: startAssessment()
        val currQ = curr.currentQuestion ?: return curr
        val tag = currQ.topicTag
        val mastery = curr.masteryMap.toMutableMap()

        if (curr.phase == AssessmentPhase.PROFILE_COLLECTION) {
            when (tag) {
                "profile_goal" -> {
                    activeSession = curr.copy(
                        profileGoal = answer,
                        questionNumber = 2,
                        currentQuestion = AssessmentQuestion(
                            id = "prof_level",
                            topicTag = "profile_level",
                            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                            prompt = "What best describes your current self-reported experience level in ${curr.domain}?",
                            options = listOf(
                                "Complete Beginner (no prior coding)",
                                "Some Experience (know basics/syntax)",
                                "Intermediate / Advanced (built apps)"
                            ),
                            explanation = "We will independently verify this with diagnostic checks."
                        )
                    )
                }
                "profile_level" -> {
                    activeSession = curr.copy(
                        profileLevel = answer,
                        questionNumber = 3,
                        currentQuestion = AssessmentQuestion(
                            id = "prof_hours",
                            topicTag = "profile_study_hours",
                            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                            prompt = "How many hours per week can you study consistently?",
                            options = listOf("3 hours / week", "6 hours / week", "10 hours / week", "15+ hours / week"),
                            explanation = "Used to accurately estimate completion timeline in weeks."
                        )
                    )
                }
                "profile_study_hours" -> {
                    val hours = when {
                        answer.contains("3") -> 3
                        answer.contains("6") -> 6
                        answer.contains("10") -> 10
                        answer.contains("15") -> 15
                        else -> 5
                    }
                    val questions = getQuestionsForDomain(curr.domain)
                    activeSession = curr.copy(
                        profileStudyHoursPerWeek = hours,
                        phase = AssessmentPhase.DIAGNOSTIC,
                        questionNumber = 4,
                        currentQuestion = questions[0]
                    )
                }
            }
            return activeSession!!
        }

        if (curr.phase == AssessmentPhase.DIAGNOSTIC) {
            val isCorrect = (quizSelectedIndex == currQ.correctOptionIndex)
            val currState = mastery[tag] ?: TopicMasteryState(tag)
            val newEv = currState.evidenceCount + 1
            val newCorr = currState.correctCount + (if (isCorrect) 1 else 0)
            val newLvl = when {
                newCorr >= 1 -> TopicMasteryLevel.STRONG
                newCorr == 0 && newEv > 0 -> TopicMasteryLevel.WEAK
                else -> TopicMasteryLevel.UNKNOWN
            }
            mastery[tag] = currState.copy(masteryLevel = newLvl, evidenceCount = newEv, correctCount = newCorr)

            if (curr.questionNumber < curr.totalQuestions) {
                val nextNum = curr.questionNumber + 1
                val usedTags = mastery.keys
                val questions = getQuestionsForDomain(curr.domain)
                val nextQ = questions.find { it.topicTag !in usedTags }
                    ?: questions[nextNum % questions.size]

                activeSession = curr.copy(
                    questionNumber = nextNum,
                    lastAnswerCorrect = isCorrect,
                    lastAnswerExplanation = currQ.explanation,
                    masteryMap = mastery,
                    currentQuestion = nextQ
                )
            } else {
                val weakCount = mastery.values.count { it.masteryLevel == TopicMasteryLevel.WEAK }
                val correction = if (curr.profileLevel.contains("Advanced", ignoreCase = true) && weakCount >= 2) {
                    CorrectionPrompt(
                        field = "level",
                        currentValue = curr.profileLevel,
                        suggestedValue = "Intermediate",
                        reason = "Diagnostic evidence revealed foundational gaps in $weakCount core areas. Starting at Intermediate ensures robust mastery."
                    )
                } else null

                activeSession = curr.copy(
                    phase = AssessmentPhase.SUMMARY,
                    isComplete = true,
                    lastAnswerCorrect = isCorrect,
                    lastAnswerExplanation = currQ.explanation,
                    masteryMap = mastery,
                    currentQuestion = null,
                    correctionPrompt = correction
                )
            }
            return activeSession!!
        }

        return curr
    }

    fun correctProfileField(sessionId: String, field: String, value: String): AssessmentSessionState {
        val curr = activeSession?.takeIf { it.sessionId == sessionId } ?: return startAssessment()
        val updated = when (field) {
            "level" -> curr.copy(profileLevel = value, correctionPrompt = null)
            "goal" -> curr.copy(profileGoal = value, correctionPrompt = null)
            else -> curr.copy(correctionPrompt = null)
        }
        activeSession = updated
        return updated
    }

    fun generateRoadmap(sessionId: String): PersonalizedRoadmapDetail {
        val session = activeSession?.takeIf { it.sessionId == sessionId } ?: startAssessment()
        val template = getTemplateForDomain(session.domain)
        val tagToId = template.associate { it.tag to it.tag }

        var totalMinutes = 0
        val topics = mutableListOf<PersonalizedRoadmapTopic>()
        val edges = mutableListOf<PersonalizedRoadmapEdge>()
        val skippedNodeIds = mutableSetOf<String>()

        for (t in template) {
            val nodeId = tagToId[t.tag]!!
            val prereqIds = t.prereqTags.mapNotNull { tagToId[it] }
            val m = session.masteryMap[t.tag]?.masteryLevel ?: TopicMasteryLevel.UNKNOWN

            val demonstrated = (m == TopicMasteryLevel.STRONG)
            val isSkipped = demonstrated && !t.tag.contains("capstone")

            if (isSkipped) {
                skippedNodeIds.add(nodeId)
            }

            val score = when (m) {
                TopicMasteryLevel.WEAK -> 100
                TopicMasteryLevel.ADEQUATE -> 65
                TopicMasteryLevel.STRONG -> 20
                TopicMasteryLevel.UNKNOWN -> 45
            }
            val exp = when {
                isSkipped -> "Demonstrated mastery in diagnostic assessment. Accelerated past foundational requirement."
                m == TopicMasteryLevel.WEAK -> "Identified as a critical growth area in diagnostic assessment. Prioritized for mastery."
                m == TopicMasteryLevel.ADEQUATE -> "Solid foundation demonstrated. Standard progression."
                else -> "Core milestone for your learning goal."
            }

            val status = when {
                isSkipped -> PersonalizedTopicStatus.SKIPPED
                prereqIds.isEmpty() || prereqIds.all { it in skippedNodeIds } -> PersonalizedTopicStatus.UNLOCKED
                else -> PersonalizedTopicStatus.LOCKED
            }

            if (!isSkipped) {
                totalMinutes += t.minutes
            }

            topics.add(
                PersonalizedRoadmapTopic(
                    id = nodeId,
                    topicTag = t.tag,
                    title = t.title,
                    shortDescription = t.desc,
                    estimatedMinutes = t.minutes,
                    prerequisiteTopicIds = prereqIds,
                    status = status,
                    masteryLevel = m,
                    priorityScore = score,
                    accelerated = isSkipped,
                    explanation = exp
                )
            )

            prereqIds.forEach { p -> edges.add(PersonalizedRoadmapEdge(p, nodeId)) }
        }

        val hours = session.profileStudyHoursPerWeek.coerceAtLeast(1)
        val weeks = ceil((totalMinutes / 60.0) / hours).toInt().coerceAtLeast(1)

        val domainTitle = session.domain.ifBlank { "Personalized" }
        val chunkSize = ceil(topics.size / 3.0).toInt().coerceAtLeast(1)
        val phaseChunks = topics.chunked(chunkSize)
        val phases = phaseChunks.mapIndexed { idx, chunkTopics ->
            val phaseNum = idx + 1
            val phaseTitle = when (phaseNum) {
                1 -> "Phase 1: Foundations & Core Concepts"
                2 -> "Phase 2: Intermediate Implementation & Systems"
                else -> "Phase 3: Production Mastery & Capstone"
            }
            val phaseObjective = "Master ${chunkTopics.firstOrNull()?.title ?: domainTitle} through ${chunkTopics.lastOrNull()?.title ?: ""}"
            val nonSkippedMinutes = chunkTopics.filter { it.status != PersonalizedTopicStatus.SKIPPED }.sumOf { it.estimatedMinutes }
            val phaseWeeks = ceil((nonSkippedMinutes / 60.0) / hours).toInt().coerceAtLeast(1)

            val tasks = chunkTopics.map { topic ->
                PersonalizedTask(
                    id = "task_${topic.id}",
                    title = topic.title,
                    description = topic.shortDescription,
                    instructions = "Hands-on project and exercises for ${topic.title}",
                    estimatedHours = topic.estimatedMinutes / 60.0,
                    resources = listOf("Official Documentation for ${session.domain}"),
                    completionCriteria = "Pass all diagnostic and unit exercises for ${topic.title}",
                    isCompleted = false,
                    dependencies = topic.prerequisiteTopicIds,
                    topicId = topic.id,
                    status = topic.status.name,
                    subtopics = listOf("${topic.title} Fundamentals", "${topic.title} Best Practices"),
                    learningObjectives = listOf("Understand ${topic.title}", "Apply ${topic.title} in projects"),
                    skipReason = topic.explanation?.takeIf { topic.status == PersonalizedTopicStatus.SKIPPED }
                )
            }

            val milestones = listOf(
                PersonalizedMilestone(
                    title = "Complete Phase $phaseNum Topics",
                    completionCriteria = listOf("Pass all module assessments"),
                    assessment = "Project Review",
                    passingCriteria = "All exercises verified",
                    isCompleted = false
                )
            )

            PersonalizedPhase(
                title = phaseTitle,
                objective = phaseObjective,
                durationWeeks = phaseWeeks,
                topics = chunkTopics.map { it.title },
                tasks = tasks,
                activities = chunkTopics.map { "Build practical exercise for ${it.title}" },
                resources = listOf("Official Documentation for ${session.domain}"),
                milestones = milestones
            )
        }

        val roadmap = PersonalizedRoadmapDetail(
            id = UUID.randomUUID().toString(),
            title = if (session.profileGoal.isNotBlank()) "$domainTitle: ${session.profileGoal.take(30)}" else "$domainTitle Learning Roadmap",
            domain = session.domain,
            category = session.domain,
            duration = "$weeks weeks",
            goal = session.profileGoal.ifBlank { "Master ${session.domain}" },
            level = session.profileLevel.ifBlank { "Beginner" },
            studyHoursPerWeek = hours,
            estimatedWeeks = weeks,
            stages = phases.size,
            phases = phases,
            topics = topics,
            edges = edges,
            assessmentSessionId = session.sessionId
        ).withRecomputedStatuses()

        savedRoadmaps.removeIf { it.id == roadmap.id }
        savedRoadmaps.add(0, roadmap)
        activeSession = session.copy(phase = AssessmentPhase.ROADMAP_READY, generatedRoadmapId = roadmap.id)
        return roadmap
    }

    fun getSavedRoadmaps(): List<PersonalizedRoadmapDetail> = savedRoadmaps.toList()

    fun getRoadmap(roadmapId: String): PersonalizedRoadmapDetail? = savedRoadmaps.find { it.id == roadmapId }

    fun saveRoadmap(roadmap: PersonalizedRoadmapDetail): PersonalizedRoadmapDetail {
        savedRoadmaps.removeIf { it.id == roadmap.id }
        savedRoadmaps.add(0, roadmap)
        return roadmap
    }

    fun deleteRoadmap(roadmapId: String) {
        savedRoadmaps.removeIf { it.id == roadmapId }
    }

    fun renameRoadmap(roadmapId: String, newTitle: String): PersonalizedRoadmapDetail? {
        val found = savedRoadmaps.find { it.id == roadmapId } ?: return null
        val updated = found.copy(title = newTitle, updatedAtMillis = System.currentTimeMillis())
        saveRoadmap(updated)
        return updated
    }
}
