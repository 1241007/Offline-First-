package com.offline_First.domain.model

import java.util.UUID

// ---------------------------------------------------------------------------
// Assessment Domain
// ---------------------------------------------------------------------------

/**
 * Categories of question that the diagnostic assessment can ask.
 */
enum class AssessmentQuestionType {
    MULTIPLE_CHOICE,    // Four options, one correct
    CODE_OUTPUT,        // Read code, predict printed output
    DEBUGGING,          // Identify / fix a bug in a snippet
    FREE_TEXT           // Open-ended conceptual question
}

/**
 * A single question served during the diagnostic assessment.
 * Options is non-empty only for MULTIPLE_CHOICE and CODE_OUTPUT.
 * CodeSnippet is non-null for CODE_OUTPUT and DEBUGGING.
 */
data class AssessmentQuestion(
    val id: String = UUID.randomUUID().toString(),
    val topicTag: String,                          // e.g. "python_basics", "loops", "classes"
    val questionType: AssessmentQuestionType,
    val prompt: String,
    val codeSnippet: String? = null,
    val options: List<String> = emptyList(),       // Indices 0-3 for MC / code-output
    val correctOptionIndex: Int? = null,           // null for FREE_TEXT
    val difficultyLevel: Int = 1,                  // 1 = easy, 2 = medium, 3 = hard
    val explanation: String = ""                   // Shown after answering
)

/**
 * Mastery level for a single topic, derived from assessment evidence.
 */
enum class TopicMasteryLevel {
    UNKNOWN,     // No evidence yet
    WEAK,        // Evidence of gaps — needs full coverage
    ADEQUATE,    // Some knowledge — may skip introductory content
    STRONG       // Demonstrated mastery — can accelerate or skip
}

/**
 * Mastery state for one topic, including the number of supporting evidence items.
 */
data class TopicMasteryState(
    val topicTag: String,
    val masteryLevel: TopicMasteryLevel = TopicMasteryLevel.UNKNOWN,
    val evidenceCount: Int = 0,                    // How many questions assessed this topic
    val correctCount: Int = 0                      // How many were answered correctly
)

/**
 * The complete state of an in-progress or completed assessment session.
 * This is what AssessmentViewModel drives the UI from.
 */
data class AssessmentSessionState(
    val sessionId: String = UUID.randomUUID().toString(),
    val domain: String = "Python",                 // e.g. "Python", "WebDev"
    val phase: AssessmentPhase = AssessmentPhase.PROFILE_COLLECTION,
    val profileGoal: String = "",                  // Learner's stated goal
    val profileLevel: String = "",                 // Learner's self-reported level
    val profileStudyHoursPerWeek: Int = 0,
    val profileTimelineWeeks: Int? = null,         // Optional target timeline

    // Current question being displayed (null when waiting or complete)
    val currentQuestion: AssessmentQuestion? = null,
    val questionNumber: Int = 0,                   // 1-based display counter
    val totalQuestions: Int = 0,                   // 0 = not yet determined (adaptive)

    // Per-topic mastery evidence collected so far
    val masteryMap: Map<String, TopicMasteryState> = emptyMap(),

    // Overall session progress
    val isComplete: Boolean = false,
    val generatedRoadmapId: String? = null,        // Set after roadmap generation

    // Feedback on most recent answer
    val lastAnswerCorrect: Boolean? = null,
    val lastAnswerExplanation: String = "",

    // Non-null when the backend wants the learner to confirm/correct a profile field
    val correctionPrompt: CorrectionPrompt? = null,

    // Error from the last operation, if any
    val errorMessage: String? = null
)

/**
 * Phases of the assessment flow.
 */
enum class AssessmentPhase {
    PROFILE_COLLECTION,   // Ask goal / level / study-time questions
    DIAGNOSTIC,           // Adaptive knowledge questions
    SUMMARY,              // Show assessed strengths & gaps
    ROADMAP_READY         // Roadmap has been generated; ready to view
}

/**
 * A backend prompt asking the learner to confirm or correct something (e.g. if
 * self-reported "Advanced" conflicts with diagnostic evidence of "Beginner" gaps).
 */
data class CorrectionPrompt(
    val field: String,       // e.g. "level", "goal"
    val currentValue: String,
    val suggestedValue: String,
    val reason: String
)

// ---------------------------------------------------------------------------
// Personalized Roadmap Domain
// ---------------------------------------------------------------------------

/**
 * Learning status of one topic within the personalized roadmap.
 * Keeps the same surface as TopicLearningStatus for UI consistency,
 * but lives in a separate type so personalized and static roadmaps can evolve independently.
 */
enum class PersonalizedTopicStatus {
    LOCKED,        // Prerequisites not yet met
    UNLOCKED,      // Available to start
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

/**
 * A single topic node within a generated personalized roadmap.
 */
data class PersonalizedRoadmapTopic(
    val id: String = UUID.randomUUID().toString(),
    val topicTag: String,                           // Stable cross-domain key, e.g. "python_loops"
    val title: String,
    val shortDescription: String = "",
    val estimatedMinutes: Int = 20,
    val prerequisiteTopicIds: List<String> = emptyList(),
    val status: PersonalizedTopicStatus = PersonalizedTopicStatus.LOCKED,
    val masteryLevel: TopicMasteryLevel = TopicMasteryLevel.UNKNOWN,
    val priorityScore: Int = 0,                     // Higher = more important to tackle first
    val accelerated: Boolean = false,               // True if assessment evidence justified skipping intro
    val explanation: String = ""                    // AI-generated reason for inclusion or acceleration
)

/**
 * A directed dependency edge between two topics in the roadmap graph.
 */
data class PersonalizedRoadmapEdge(
    val fromTopicId: String,
    val toTopicId: String
)

/**
 * The complete persisted personalized roadmap for one learner.
 */
data class PersonalizedRoadmapDetail(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "My Python Roadmap",
    val domain: String = "Python",
    val goal: String = "",
    val level: String = "",
    val studyHoursPerWeek: Int = 0,
    val estimatedWeeks: Int = 0,
    val topics: List<PersonalizedRoadmapTopic> = emptyList(),
    val edges: List<PersonalizedRoadmapEdge> = emptyList(),
    val assessmentSessionId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),

    // Derived progress (computed locally for fast UI updates)
    val completedTopicIds: Set<String> = emptySet(),
    val inProgressTopicIds: Set<String> = emptySet()
) {
    val totalTopics: Int get() = topics.size
    val completedCount: Int get() = completedTopicIds.size
    val progressPercentage: Int get() =
        if (totalTopics == 0) 0
        else ((completedCount.toFloat() / totalTopics.toFloat()) * 100).toInt().coerceIn(0, 100)

    /** Next recommended topic: highest-priority UNLOCKED topic not yet started. */
    val recommendedNextTopic: PersonalizedRoadmapTopic?
        get() = topics
            .filter { it.status == PersonalizedTopicStatus.UNLOCKED }
            .maxByOrNull { it.priorityScore }

    /** Re-derive locked/unlocked status based on completed IDs (for offline-first updates). */
    fun withRecomputedStatuses(): PersonalizedRoadmapDetail {
        val updatedTopics = topics.map { topic ->
            when {
                completedTopicIds.contains(topic.id) ->
                    topic.copy(status = PersonalizedTopicStatus.COMPLETED)
                inProgressTopicIds.contains(topic.id) ->
                    topic.copy(status = PersonalizedTopicStatus.IN_PROGRESS)
                topic.prerequisiteTopicIds.all { prereqId ->
                    completedTopicIds.contains(prereqId)
                } -> topic.copy(status = PersonalizedTopicStatus.UNLOCKED)
                else -> topic.copy(status = PersonalizedTopicStatus.LOCKED)
            }
        }
        return copy(topics = updatedTopics)
    }
}

data class TopicLearningResource(
    val title: String,
    val url: String,
    val sourceName: String,
    val description: String,
    val isVerified: Boolean = true
)

data class TopicCodeExample(
    val title: String,
    val code: String,
    val explanation: String,
    val expectedOutput: String = ""
)

data class TopicCommonMistake(
    val mistake: String,
    val whyItHappens: String,
    val howToFix: String
)

data class TopicPracticeQuiz(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class TopicLearningContent(
    val topicId: String,
    val topicTitle: String,
    val domain: String,
    val learningObjectives: List<String> = emptyList(),
    val beginnerExplanation: String = "",
    val syntaxAndConcepts: String = "",
    val codeExamples: List<TopicCodeExample> = emptyList(),
    val commonMistakes: List<TopicCommonMistake> = emptyList(),
    val practiceQuiz: TopicPracticeQuiz? = null,
    val resources: List<TopicLearningResource> = emptyList()
)

