package com.offline_First.data.remote

import com.offline_First.domain.model.AssessmentPhase
import com.offline_First.domain.model.AssessmentQuestion
import com.offline_First.domain.model.AssessmentQuestionType
import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.CorrectionPrompt
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.PersonalizedRoadmapEdge
import com.offline_First.domain.model.PersonalizedRoadmapTopic
import com.offline_First.domain.model.PersonalizedTopicStatus
import com.offline_First.domain.model.TopicMasteryLevel
import com.offline_First.domain.model.TopicMasteryState
import com.offline_First.domain.model.TopicLearningContent
import com.offline_First.domain.model.TopicLearningResource
import com.offline_First.domain.model.TopicCodeExample
import com.offline_First.domain.model.TopicCommonMistake
import com.offline_First.domain.model.TopicPracticeQuiz
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Assessment DTOs (matches backend /api/v1/assessment/* responses)
// ---------------------------------------------------------------------------

@Serializable
data class AssessmentQuestionDto(
    val id: String,
    @SerialName("topic_tag") val topicTag: String,
    @SerialName("question_type") val questionType: String,  // "multiple_choice" | "code_output" | "debugging" | "free_text"
    val prompt: String,
    @SerialName("code_snippet") val codeSnippet: String? = null,
    val options: List<String> = emptyList(),
    @SerialName("correct_option_index") val correctOptionIndex: Int? = null,
    @SerialName("difficulty_level") val difficultyLevel: Int = 1,
    val explanation: String = ""
)

@Serializable
data class TopicMasteryStateDto(
    @SerialName("topic_tag") val topicTag: String,
    @SerialName("mastery_level") val masteryLevel: String,  // "unknown" | "weak" | "adequate" | "strong"
    @SerialName("evidence_count") val evidenceCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0
)

@Serializable
data class CorrectionPromptDto(
    val field: String,
    @SerialName("current_value") val currentValue: String,
    @SerialName("suggested_value") val suggestedValue: String,
    val reason: String
)

@Serializable
data class AssessmentSessionDto(
    @SerialName("session_id") val sessionId: String,
    val domain: String = "Python",
    val phase: String = "profile_collection",  // "profile_collection" | "diagnostic" | "summary" | "roadmap_ready"
    @SerialName("profile_goal") val profileGoal: String = "",
    @SerialName("profile_level") val profileLevel: String = "",
    @SerialName("profile_study_hours_per_week") val profileStudyHoursPerWeek: Int = 0,
    @SerialName("profile_timeline_weeks") val profileTimelineWeeks: Int? = null,
    @SerialName("current_question") val currentQuestion: AssessmentQuestionDto? = null,
    @SerialName("question_number") val questionNumber: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("mastery_map") val masteryMap: Map<String, TopicMasteryStateDto> = emptyMap(),
    @SerialName("is_complete") val isComplete: Boolean = false,
    @SerialName("generated_roadmap_id") val generatedRoadmapId: String? = null,
    @SerialName("last_answer_correct") val lastAnswerCorrect: Boolean? = null,
    @SerialName("last_answer_explanation") val lastAnswerExplanation: String = "",
    @SerialName("correction_prompt") val correctionPrompt: CorrectionPromptDto? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

// Request DTOs
@Serializable
data class SubmitAnswerRequestDto(
    val answer: String,
    @SerialName("quiz_selected_index") val quizSelectedIndex: Int? = null
)

@Serializable
data class CorrectProfileFieldRequestDto(
    val field: String,
    val value: String
)

// ---------------------------------------------------------------------------
// Personalized Roadmap DTOs (matches /api/v1/personalized-roadmaps/*)
// ---------------------------------------------------------------------------

@Serializable
data class PersonalizedRoadmapTopicDto(
    val id: String,
    @SerialName("topic_tag") val topicTag: String,
    val title: String,
    @SerialName("short_description") val shortDescription: String = "",
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 20,
    @SerialName("prerequisite_topic_ids") val prerequisiteTopicIds: List<String> = emptyList(),
    val status: String = "locked",           // "locked"|"unlocked"|"in_progress"|"completed"|"skipped"
    @SerialName("mastery_level") val masteryLevel: String = "unknown",
    @SerialName("priority_score") val priorityScore: Int = 0,
    val accelerated: Boolean = false,
    val explanation: String = ""
)

@Serializable
data class PersonalizedRoadmapEdgeDto(
    @SerialName("from_topic_id") val fromTopicId: String,
    @SerialName("to_topic_id") val toTopicId: String
)

@Serializable
data class PersonalizedRoadmapDetailDto(
    val id: String,
    val title: String = "My Python Roadmap",
    val domain: String = "Python",
    val goal: String = "",
    val level: String = "",
    @SerialName("study_hours_per_week") val studyHoursPerWeek: Int = 0,
    @SerialName("estimated_weeks") val estimatedWeeks: Int = 0,
    val topics: List<PersonalizedRoadmapTopicDto> = emptyList(),
    val edges: List<PersonalizedRoadmapEdgeDto> = emptyList(),
    @SerialName("assessment_session_id") val assessmentSessionId: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("completed_topic_ids") val completedTopicIds: List<String> = emptyList(),
    @SerialName("in_progress_topic_ids") val inProgressTopicIds: List<String> = emptyList()
)

@Serializable
data class RenameRoadmapRequestDto(val title: String)

// ---------------------------------------------------------------------------
// Domain mapping extension functions
// ---------------------------------------------------------------------------

fun AssessmentQuestionDto.toDomain(): AssessmentQuestion = AssessmentQuestion(
    id = id,
    topicTag = topicTag,
    questionType = when (questionType) {
        "code_output" -> AssessmentQuestionType.CODE_OUTPUT
        "debugging" -> AssessmentQuestionType.DEBUGGING
        "free_text" -> AssessmentQuestionType.FREE_TEXT
        else -> AssessmentQuestionType.MULTIPLE_CHOICE
    },
    prompt = prompt,
    codeSnippet = codeSnippet,
    options = options,
    correctOptionIndex = correctOptionIndex,
    difficultyLevel = difficultyLevel,
    explanation = explanation
)

fun TopicMasteryStateDto.toDomain(): TopicMasteryState = TopicMasteryState(
    topicTag = topicTag,
    masteryLevel = when (masteryLevel) {
        "weak" -> TopicMasteryLevel.WEAK
        "adequate" -> TopicMasteryLevel.ADEQUATE
        "strong" -> TopicMasteryLevel.STRONG
        else -> TopicMasteryLevel.UNKNOWN
    },
    evidenceCount = evidenceCount,
    correctCount = correctCount
)

fun AssessmentSessionDto.toDomain(): AssessmentSessionState = AssessmentSessionState(
    sessionId = sessionId,
    domain = domain,
    phase = when (phase) {
        "diagnostic" -> AssessmentPhase.DIAGNOSTIC
        "summary" -> AssessmentPhase.SUMMARY
        "roadmap_ready" -> AssessmentPhase.ROADMAP_READY
        else -> AssessmentPhase.PROFILE_COLLECTION
    },
    profileGoal = profileGoal,
    profileLevel = profileLevel,
    profileStudyHoursPerWeek = profileStudyHoursPerWeek,
    profileTimelineWeeks = profileTimelineWeeks,
    currentQuestion = currentQuestion?.toDomain(),
    questionNumber = questionNumber,
    totalQuestions = totalQuestions,
    masteryMap = masteryMap.mapValues { (_, v) -> v.toDomain() },
    isComplete = isComplete,
    generatedRoadmapId = generatedRoadmapId,
    lastAnswerCorrect = lastAnswerCorrect,
    lastAnswerExplanation = lastAnswerExplanation,
    correctionPrompt = correctionPrompt?.let {
        CorrectionPrompt(
            field = it.field,
            currentValue = it.currentValue,
            suggestedValue = it.suggestedValue,
            reason = it.reason
        )
    },
    errorMessage = errorMessage
)

fun PersonalizedRoadmapTopicDto.toDomain(): PersonalizedRoadmapTopic = PersonalizedRoadmapTopic(
    id = id,
    topicTag = topicTag,
    title = title,
    shortDescription = shortDescription,
    estimatedMinutes = estimatedMinutes,
    prerequisiteTopicIds = prerequisiteTopicIds,
    status = when (status) {
        "unlocked" -> PersonalizedTopicStatus.UNLOCKED
        "in_progress" -> PersonalizedTopicStatus.IN_PROGRESS
        "completed" -> PersonalizedTopicStatus.COMPLETED
        "skipped" -> PersonalizedTopicStatus.SKIPPED
        else -> PersonalizedTopicStatus.LOCKED
    },
    masteryLevel = when (masteryLevel) {
        "weak" -> TopicMasteryLevel.WEAK
        "adequate" -> TopicMasteryLevel.ADEQUATE
        "strong" -> TopicMasteryLevel.STRONG
        else -> TopicMasteryLevel.UNKNOWN
    },
    priorityScore = priorityScore,
    accelerated = accelerated,
    explanation = explanation
)

fun PersonalizedRoadmapDetailDto.toDomain(): PersonalizedRoadmapDetail = PersonalizedRoadmapDetail(
    id = id,
    title = title,
    domain = domain,
    goal = goal,
    level = level,
    studyHoursPerWeek = studyHoursPerWeek,
    estimatedWeeks = estimatedWeeks,
    topics = topics.map { it.toDomain() },
    edges = edges.map { PersonalizedRoadmapEdge(it.fromTopicId, it.toTopicId) },
    assessmentSessionId = assessmentSessionId,
    completedTopicIds = completedTopicIds.toSet(),
    inProgressTopicIds = inProgressTopicIds.toSet()
).withRecomputedStatuses()

// ---------------------------------------------------------------------------
// Dynamic Subject & Topic Learning DTOs
// ---------------------------------------------------------------------------



@Serializable
data class TopicLearningResourceDto(
    val title: String,
    val url: String,
    @SerialName("source_name") val sourceName: String,
    val description: String,
    @SerialName("is_verified") val isVerified: Boolean = true
)

@Serializable
data class TopicCodeExampleDto(
    val title: String,
    val code: String,
    val explanation: String,
    @SerialName("expected_output") val expectedOutput: String = ""
)

@Serializable
data class TopicCommonMistakeDto(
    val mistake: String,
    @SerialName("why_it_happens") val whyItHappens: String,
    @SerialName("how_to_fix") val howToFix: String
)

@Serializable
data class TopicPracticeQuizDto(
    val question: String,
    val options: List<String> = emptyList(),
    @SerialName("correct_option_index") val correctOptionIndex: Int = 0,
    val explanation: String = ""
)

@Serializable
data class TopicLearningContentDto(
    @SerialName("topic_id") val topicId: String,
    @SerialName("topic_title") val topicTitle: String,
    val domain: String,
    @SerialName("learning_objectives") val learningObjectives: List<String> = emptyList(),
    @SerialName("beginner_explanation") val beginnerExplanation: String = "",
    @SerialName("syntax_and_concepts") val syntaxAndConcepts: String = "",
    @SerialName("code_examples") val codeExamples: List<TopicCodeExampleDto> = emptyList(),
    @SerialName("common_mistakes") val commonMistakes: List<TopicCommonMistakeDto> = emptyList(),
    @SerialName("practice_quiz") val practiceQuiz: TopicPracticeQuizDto? = null,
    val resources: List<TopicLearningResourceDto> = emptyList()
)



fun TopicLearningResourceDto.toDomain(): TopicLearningResource = TopicLearningResource(
    title = title,
    url = url,
    sourceName = sourceName,
    description = description,
    isVerified = isVerified
)

fun TopicCodeExampleDto.toDomain(): TopicCodeExample = TopicCodeExample(
    title = title,
    code = code,
    explanation = explanation,
    expectedOutput = expectedOutput
)

fun TopicCommonMistakeDto.toDomain(): TopicCommonMistake = TopicCommonMistake(
    mistake = mistake,
    whyItHappens = whyItHappens,
    howToFix = howToFix
)

fun TopicPracticeQuizDto.toDomain(): TopicPracticeQuiz = TopicPracticeQuiz(
    question = question,
    options = options,
    correctOptionIndex = correctOptionIndex,
    explanation = explanation
)

fun TopicLearningContentDto.toDomain(): TopicLearningContent = TopicLearningContent(
    topicId = topicId,
    topicTitle = topicTitle,
    domain = domain,
    learningObjectives = learningObjectives,
    beginnerExplanation = beginnerExplanation,
    syntaxAndConcepts = syntaxAndConcepts,
    codeExamples = codeExamples.map { it.toDomain() },
    commonMistakes = commonMistakes.map { it.toDomain() },
    practiceQuiz = practiceQuiz?.toDomain(),
    resources = resources.map { it.toDomain() }
)

