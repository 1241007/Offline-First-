package com.offline_First.domain.model

enum class RoadmapAccentTheme {
    PRIMARY,
    SECONDARY,
    ACCENT,
    SUCCESS
}

data class RoadmapOption(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY,
    val isPersonalized: Boolean = false
)

enum class AssessmentSpeaker {
    MENTOR,
    LEARNER
}

data class DiagnosticQuiz(
    val question: String,
    val options: List<String>,
    val skillTested: String? = null,
    val difficulty: String? = null,
    val selectionRationale: String? = null
)

data class AssessmentMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: AssessmentSpeaker,
    val text: String,
    val options: List<String> = emptyList(),
    val quiz: DiagnosticQuiz? = null,
    val stepId: String? = null,
    val timestamp: String? = null
)

data class AssessmentSessionState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val state: String = "in_progress",
    val currentStepId: String? = null,
    val completedSteps: List<String> = emptyList(),
    val goal: String? = null,
    val targetLevel: String? = null,
    val targetTimeline: String? = null,
    val weeklyHours: Double? = null,
    val latestMessage: String? = null,
    val options: List<String> = emptyList(),
    val quiz: DiagnosticQuiz? = null,
    val completenessPercentage: Int = 0,
    val summary: Map<String, Any?>? = null,
    val roadmapId: String? = null,
    val messages: List<AssessmentMessage> = emptyList(),

    // Extra fields for adaptive assessment
    val sessionId: String = id,
    val domain: String = "Python",
    val phase: AssessmentPhase = AssessmentPhase.PROFILE_COLLECTION,
    val profileGoal: String = "",
    val profileLevel: String = "",
    val profileStudyHoursPerWeek: Int = 0,
    val profileTimelineWeeks: Int? = null,
    val currentQuestion: AssessmentQuestion? = null,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 0,
    val masteryMap: Map<String, TopicMasteryState> = emptyMap(),
    val isComplete: Boolean = false,
    val generatedRoadmapId: String? = null,
    val lastAnswerCorrect: Boolean? = null,
    val lastAnswerExplanation: String = "",
    val correctionPrompt: CorrectionPrompt? = null,
    val errorMessage: String? = null
)

data class PersonalizedTask(
    val id: String,
    val title: String,
    val description: String = "",
    val instructions: String = "",
    val estimatedHours: Double = 1.0,
    val resources: List<String> = emptyList(),
    val completionCriteria: String = "",
    val isCompleted: Boolean = false,
    val dependencies: List<String> = emptyList()
)

data class PersonalizedMilestone(
    val title: String,
    val completionCriteria: List<String> = emptyList(),
    val assessment: String = "",
    val passingCriteria: String = "",
    val isCompleted: Boolean = false
)

data class PersonalizedPhase(
    val title: String,
    val objective: String,
    val durationWeeks: Int,
    val topics: List<String> = emptyList(),
    val tasks: List<PersonalizedTask> = emptyList(),
    val activities: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val milestones: List<PersonalizedMilestone> = emptyList(),
    val recommendedCourseIds: List<String> = emptyList()
)

data class WeeklyScheduleItem(
    val dayOrWeek: String,
    val focusTopic: String,
    val estimatedHours: Double,
    val tasks: List<String> = emptyList()
)

data class SkillGapItem(
    val skill: String,
    val status: String = "developing",
    val source: String = "diagnostic_verified",
    val confidence: Double = 1.0,
    val rationale: String? = null
)

data class AssessmentSummary(
    val strengths: List<String> = emptyList(),
    val skillGaps: List<String> = emptyList(),
    val verifiedEvidence: List<String> = emptyList(),
    val selfReportedInformation: List<String> = emptyList(),
    val unknowns: List<String> = emptyList(),
    val skillGapBreakdown: List<SkillGapItem> = emptyList(),
    val curriculumRationale: String? = null
)

data class PersonalizedRoadmapDetail(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "My Personalized Roadmap",
    val goal: String = "",
    val category: String = "",
    val level: String = "",
    val duration: String = "",
    val stages: Int = 0,
    val icon: String = "",
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY,
    val weeklyHours: Double = 0.0,
    val assessmentSummary: AssessmentSummary = AssessmentSummary(),
    val phases: List<PersonalizedPhase> = emptyList(),
    val weeklySchedule: List<WeeklyScheduleItem> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val capstoneProject: String = "",
    val nextAction: String = "",
    val completedMilestones: List<String> = emptyList(),
    val createdAt: String? = null,

    // Extra fields for topic-based model
    val domain: String = "Python",
    val studyHoursPerWeek: Int = 0,
    val estimatedWeeks: Int = 0,
    val topics: List<PersonalizedRoadmapTopic> = emptyList(),
    val edges: List<PersonalizedRoadmapEdge> = emptyList(),
    val assessmentSessionId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),

    // Derived progress
    val completedTopicIds: Set<String> = emptySet(),
    val inProgressTopicIds: Set<String> = emptySet()
) {
    val totalTopics: Int get() = if (topics.isNotEmpty()) topics.size else phases.sumOf { it.milestones.size }
    val completedCount: Int get() = if (completedTopicIds.isNotEmpty()) completedTopicIds.size else completedMilestones.size
    val progressPercentage: Int get() =
        if (totalTopics == 0) 0
        else ((completedCount.toFloat() / totalTopics.toFloat()) * 100).toInt().coerceIn(0, 100)

    val recommendedNextTopic: PersonalizedRoadmapTopic?
        get() = topics
            .filter { it.status == PersonalizedTopicStatus.UNLOCKED }
            .maxByOrNull { it.priorityScore }

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

// Legacy preview for backward compatibility
data class GeneratedRoadmapPreview(
    val goal: String,
    val level: String,
    val studyTime: String,
    val duration: String,
    val stages: List<String>
)
