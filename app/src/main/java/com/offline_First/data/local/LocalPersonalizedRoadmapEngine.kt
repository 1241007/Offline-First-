package com.offline_First.data.local

import com.offline_First.domain.model.*
import java.util.UUID
import kotlin.math.ceil

/**
 * Offline-first local diagnostic engine and personalized roadmap generator.
 * Seamlessly provides instant assessments and DAG roadmap generation on-device
 * when the remote API is unreachable or returns 404.
 */
object LocalPersonalizedRoadmapEngine {

    private var activeSession: AssessmentSessionState? = null
    private val savedRoadmaps = mutableListOf<PersonalizedRoadmapDetail>()

    private val DIAGNOSTIC_QUESTIONS = listOf(
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
            codeSnippet = """
total = 0
for i in range(1, 5):
    if i == 3:
        continue
    total += i
print(total)
            """.trimIndent(),
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
            codeSnippet = """
def add_item(item, target_list=[]):
    target_list.append(item)
    return target_list
            """.trimIndent(),
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
            id = "q_py_exc_1",
            topicTag = "python_exceptions",
            questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
            prompt = "In a try...except...else...finally statement, when does the 'else' block execute?",
            options = listOf(
                "Always, regardless of errors",
                "Only when an exception was handled",
                "Only when no exception was raised in the try block",
                "Only when finally fails"
            ),
            correctOptionIndex = 2,
            difficultyLevel = 2,
            explanation = "The 'else' block runs only if the try clause finishes without throwing any exception."
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

    private data class TemplateTopic(
        val tag: String,
        val title: String,
        val desc: String,
        val minutes: Int,
        val prereqTags: List<String>
    )

    fun startAssessment(): AssessmentSessionState {
        val session = AssessmentSessionState(
            sessionId = UUID.randomUUID().toString(),
            domain = "Python",
            phase = AssessmentPhase.PROFILE_COLLECTION,
            questionNumber = 1,
            totalQuestions = 7,
            currentQuestion = AssessmentQuestion(
                id = "prof_goal",
                topicTag = "profile_goal",
                questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                prompt = "What is your primary goal with Python?",
                options = listOf(
                    "Get a Software Developer / Backend job",
                    "Build my own web projects & tools",
                    "Learn Data Science & AI / ML",
                    "Prepare for university exams / academic prep"
                ),
                explanation = "Your goal helps us tailor milestone priorities and project themes."
            ),
            masteryMap = emptyMap()
        )
        activeSession = session
        return session
    }

    fun submitAnswer(sessionId: String, answer: String, quizSelectedIndex: Int?): AssessmentSessionState {
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
                            prompt = "What best describes your current self-reported experience level?",
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
                    activeSession = curr.copy(
                        profileStudyHoursPerWeek = hours,
                        phase = AssessmentPhase.DIAGNOSTIC,
                        questionNumber = 4,
                        currentQuestion = DIAGNOSTIC_QUESTIONS[0]
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
                newCorr >= 2 -> TopicMasteryLevel.STRONG
                newCorr >= 1 -> TopicMasteryLevel.ADEQUATE
                else -> TopicMasteryLevel.WEAK
            }
            mastery[tag] = currState.copy(masteryLevel = newLvl, evidenceCount = newEv, correctCount = newCorr)

            if (curr.questionNumber < curr.totalQuestions) {
                val nextNum = curr.questionNumber + 1
                val usedTags = mastery.keys
                val nextQ = DIAGNOSTIC_QUESTIONS.find { it.topicTag !in usedTags }
                    ?: DIAGNOSTIC_QUESTIONS[nextNum % DIAGNOSTIC_QUESTIONS.size]

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
        val tagToId = PYTHON_CURRICULUM_TEMPLATE.associate { it.tag to "topic_${it.tag}" }

        var totalMinutes = 0
        val topics = mutableListOf<PersonalizedRoadmapTopic>()
        val edges = mutableListOf<PersonalizedRoadmapEdge>()

        for (t in PYTHON_CURRICULUM_TEMPLATE) {
            val nodeId = tagToId[t.tag]!!
            val prereqIds = t.prereqTags.mapNotNull { tagToId[it] }
            val m = session.masteryMap[t.tag]?.masteryLevel ?: TopicMasteryLevel.UNKNOWN

            val accelerated = (m == TopicMasteryLevel.STRONG)
            val score = when (m) {
                TopicMasteryLevel.WEAK -> 100
                TopicMasteryLevel.ADEQUATE -> 65
                TopicMasteryLevel.STRONG -> 20
                TopicMasteryLevel.UNKNOWN -> 45
            }
            val exp = when (m) {
                TopicMasteryLevel.WEAK -> "Identified as a critical growth area in diagnostic assessment. Prioritized for mastery."
                TopicMasteryLevel.STRONG -> "Demonstrated strong knowledge in diagnostic assessment. Accelerated pathway applied."
                TopicMasteryLevel.ADEQUATE -> "Solid foundation demonstrated. Standard progression."
                TopicMasteryLevel.UNKNOWN -> "Core milestone for your learning goal."
            }
            val status = if (prereqIds.isEmpty()) PersonalizedTopicStatus.UNLOCKED else PersonalizedTopicStatus.LOCKED

            totalMinutes += if (accelerated) (t.minutes * 0.4).toInt() else t.minutes

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
                    accelerated = accelerated,
                    explanation = exp
                )
            )

            prereqIds.forEach { p -> edges.add(PersonalizedRoadmapEdge(p, nodeId)) }
        }

        val hours = session.profileStudyHoursPerWeek.coerceAtLeast(1)
        val weeks = ceil((totalMinutes / 60.0) / hours).toInt().coerceAtLeast(1)

        val roadmap = PersonalizedRoadmapDetail(
            id = UUID.randomUUID().toString(),
            title = if (session.profileGoal.isNotBlank()) "Python: ${session.profileGoal.take(30)}" else "Personalized Python Roadmap",
            domain = session.domain,
            goal = session.profileGoal.ifBlank { "Master Python" },
            level = session.profileLevel.ifBlank { "Beginner" },
            studyHoursPerWeek = hours,
            estimatedWeeks = weeks,
            topics = topics,
            edges = edges,
            assessmentSessionId = session.sessionId
        )

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
