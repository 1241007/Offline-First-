package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoadmapDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String> = emptyList(),
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    @SerialName("accentTheme") val accentTheme: String = "primary"
)

@Serializable
data class RoadmapItemDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("courseId") val courseId: String? = null,
    val skills: List<String>? = null,
    val duration: String? = null
)

@Serializable
data class RoadmapDetailDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    @SerialName("accentTheme") val accentTheme: String = "primary",
    val items: List<RoadmapItemDto> = emptyList()
)

// --- Personalized Roadmap DTOs ---

@Serializable
data class AssessmentQuizPublicDto(
    val question: String,
    val options: List<String> = emptyList(),
    @SerialName("skill_tested") val skillTested: String? = null
)

@Serializable
data class AssessmentMessageItemDto(
    val sender: String,
    val text: String,
    val options: List<String> = emptyList(),
    val quiz: AssessmentQuizPublicDto? = null,
    val timestamp: String? = null
)

@Serializable
data class AssessmentSessionResponseDto(
    val id: String,
    val state: String,
    val goal: String? = null,
    @SerialName("targetLevel") val targetLevel: String? = null,
    @SerialName("targetTimeline") val targetTimeline: String? = null,
    @SerialName("weeklyHours") val weeklyHours: Double? = null,
    @SerialName("latestMessage") val latestMessage: String? = null,
    val options: List<String> = emptyList(),
    val quiz: AssessmentQuizPublicDto? = null,
    @SerialName("completenessPercentage") val completenessPercentage: Int = 0,
    @SerialName("roadmapId") val roadmapId: String? = null,
    val messages: List<AssessmentMessageItemDto> = emptyList()
)

@Serializable
data class SubmitAssessmentAnswerRequestDto(
    val answer: String,
    @SerialName("quizSelectedIndex") val quizSelectedIndex: Int? = null
)

@Serializable
data class PersonalizedMilestoneDto(
    val title: String,
    @SerialName("completionCriteria") val completionCriteria: List<String> = emptyList(),
    val assessment: String = "",
    @SerialName("passingCriteria") val passingCriteria: String = ""
)

@Serializable
data class PersonalizedPhaseDto(
    val title: String,
    val objective: String,
    @SerialName("durationWeeks") val durationWeeks: Int = 1,
    val topics: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val milestones: List<PersonalizedMilestoneDto> = emptyList(),
    @SerialName("recommendedCourseIds") val recommendedCourseIds: List<String> = emptyList()
)

@Serializable
data class WeeklyScheduleItemDto(
    @SerialName("dayOrWeek") val dayOrWeek: String,
    @SerialName("focusTopic") val focusTopic: String,
    @SerialName("estimatedHours") val estimatedHours: Double,
    val tasks: List<String> = emptyList()
)

@Serializable
data class AssessmentSummaryDto(
    val strengths: List<String> = emptyList(),
    @SerialName("skillGaps") val skillGaps: List<String> = emptyList(),
    @SerialName("verifiedEvidence") val verifiedEvidence: List<String> = emptyList(),
    @SerialName("selfReportedInformation") val selfReportedInformation: List<String> = emptyList(),
    val unknowns: List<String> = emptyList()
)

@Serializable
data class PersonalizedRoadmapStructureDto(
    val title: String,
    val goal: String,
    @SerialName("startingLevel") val startingLevel: String,
    val category: String = "General",
    @SerialName("estimatedDuration") val estimatedDuration: String,
    @SerialName("weeklyHours") val weeklyHours: Double,
    @SerialName("assessmentSummary") val assessmentSummary: AssessmentSummaryDto = AssessmentSummaryDto(),
    val phases: List<PersonalizedPhaseDto> = emptyList(),
    @SerialName("weeklySchedule") val weeklySchedule: List<WeeklyScheduleItemDto> = emptyList(),
    val assumptions: List<String> = emptyList(),
    @SerialName("capstoneProject") val capstoneProject: String = "",
    @SerialName("nextAction") val nextAction: String = ""
)

@Serializable
data class PersonalizedRoadmapDetailResponseDto(
    val id: String,
    val title: String,
    val goal: String,
    val category: String,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    @SerialName("accentTheme") val accentTheme: String = "primary",
    val structure: PersonalizedRoadmapStructureDto,
    val items: List<RoadmapItemDto> = emptyList(),
    @SerialName("createdAt") val createdAt: String? = null
)
