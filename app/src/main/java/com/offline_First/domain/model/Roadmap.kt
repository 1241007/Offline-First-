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
    val id: String,
    val state: String,
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
    val messages: List<AssessmentMessage> = emptyList()
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
    val id: String,
    val title: String,
    val goal: String,
    val category: String,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    val accentTheme: RoadmapAccentTheme = RoadmapAccentTheme.PRIMARY,
    val weeklyHours: Double = 0.0,
    val assessmentSummary: AssessmentSummary = AssessmentSummary(),
    val phases: List<PersonalizedPhase> = emptyList(),
    val weeklySchedule: List<WeeklyScheduleItem> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val capstoneProject: String = "",
    val nextAction: String = "",
    val completedMilestones: List<String> = emptyList(),
    val createdAt: String? = null
)

// Legacy preview for backward compatibility
data class GeneratedRoadmapPreview(
    val goal: String,
    val level: String,
    val studyTime: String,
    val duration: String,
    val stages: List<String>
)
