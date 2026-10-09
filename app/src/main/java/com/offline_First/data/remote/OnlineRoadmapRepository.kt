package com.offline_First.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OnlineRoadmapRepository(
    context: Context? = null
) : RoadmapRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun mapAccentTheme(theme: String): RoadmapAccentTheme {
        return when (theme.lowercase()) {
            "secondary", "purple", "teal", "orange" -> RoadmapAccentTheme.SECONDARY
            "accent", "red" -> RoadmapAccentTheme.ACCENT
            "success", "green" -> RoadmapAccentTheme.SUCCESS
            else -> RoadmapAccentTheme.PRIMARY
        }
    }

    private fun RoadmapDto.toDomain(): RoadmapOption {
        return RoadmapOption(
            id = id,
            title = title,
            category = category,
            description = description,
            skills = skills,
            level = level,
            duration = duration,
            stages = stages,
            icon = icon,
            accentTheme = mapAccentTheme(accentTheme),
            isPersonalized = false
        )
    }

    private fun AssessmentQuizPublicDto.toDomain(): DiagnosticQuiz {
        return DiagnosticQuiz(
            question = question,
            options = options,
            skillTested = skillTested
        )
    }

    private fun AssessmentMessageItemDto.toDomain(): AssessmentMessage {
        val spk = if (sender.lowercase() == "mentor" || sender.lowercase() == "assistant") {
            AssessmentSpeaker.MENTOR
        } else {
            AssessmentSpeaker.LEARNER
        }
        return AssessmentMessage(
            speaker = spk,
            text = text,
            options = options,
            quiz = quiz?.toDomain(),
            timestamp = timestamp
        )
    }

    private fun AssessmentSessionResponseDto.toDomain(): AssessmentSessionState {
        return AssessmentSessionState(
            id = id,
            state = state,
            goal = goal,
            targetLevel = targetLevel,
            targetTimeline = targetTimeline,
            weeklyHours = weeklyHours,
            latestMessage = latestMessage,
            options = options,
            quiz = quiz?.toDomain(),
            completenessPercentage = completenessPercentage,
            roadmapId = roadmapId,
            messages = messages.map { it.toDomain() }
        )
    }

    private fun PersonalizedMilestoneDto.toDomain(): PersonalizedMilestone {
        return PersonalizedMilestone(
            title = title,
            completionCriteria = completionCriteria,
            assessment = assessment,
            passingCriteria = passingCriteria
        )
    }

    private fun PersonalizedPhaseDto.toDomain(): PersonalizedPhase {
        return PersonalizedPhase(
            title = title,
            objective = objective,
            durationWeeks = durationWeeks,
            topics = topics,
            activities = activities,
            resources = resources,
            milestones = milestones.map { it.toDomain() },
            recommendedCourseIds = recommendedCourseIds
        )
    }

    private fun WeeklyScheduleItemDto.toDomain(): WeeklyScheduleItem {
        return WeeklyScheduleItem(
            dayOrWeek = dayOrWeek,
            focusTopic = focusTopic,
            estimatedHours = estimatedHours,
            tasks = tasks
        )
    }

    private fun AssessmentSummaryDto.toDomain(): AssessmentSummary {
        return AssessmentSummary(
            strengths = strengths,
            skillGaps = skillGaps,
            verifiedEvidence = verifiedEvidence,
            selfReportedInformation = selfReportedInformation,
            unknowns = unknowns
        )
    }

    private fun PersonalizedRoadmapDetailResponseDto.toDomain(): PersonalizedRoadmapDetail {
        return PersonalizedRoadmapDetail(
            id = id,
            title = title,
            goal = goal,
            category = category,
            level = level,
            duration = duration,
            stages = stages,
            icon = icon,
            accentTheme = mapAccentTheme(accentTheme),
            weeklyHours = structure.weeklyHours,
            assessmentSummary = structure.assessmentSummary.toDomain(),
            phases = structure.phases.map { it.toDomain() },
            weeklySchedule = structure.weeklySchedule.map { it.toDomain() },
            assumptions = structure.assumptions,
            capstoneProject = structure.capstoneProject,
            nextAction = structure.nextAction,
            createdAt = createdAt
        )
    }

    @Volatile
    private var cachedRoadmaps: List<RoadmapOption> = loadPersistedRoadmaps()

    @Volatile
    private var cachedCategories: List<String> = loadPersistedCategories()

    @Volatile
    private var cachedPersonalizedRoadmaps: List<PersonalizedRoadmapDetail> = loadPersistedPersonalizedRoadmaps()

    private fun loadPersistedRoadmaps(): List<RoadmapOption> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_ROADMAPS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<RoadmapDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun loadPersistedCategories(): List<String> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_CATEGORIES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun loadPersistedPersonalizedRoadmaps(): List<PersonalizedRoadmapDetail> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_PERSONALIZED_ROADMAPS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PersonalizedRoadmapDetailResponseDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun savePersistedRoadmaps(dtos: List<RoadmapDto>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_ROADMAPS, json.encodeToString(dtos)).apply()
        }
    }

    private fun savePersistedCategories(cats: List<String>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_CATEGORIES, json.encodeToString(cats)).apply()
        }
    }

    private fun savePersistedPersonalizedRoadmaps(dtos: List<PersonalizedRoadmapDetailResponseDto>) {
        val sp = prefs ?: return
        runCatching {
            sp.edit().putString(KEY_PERSONALIZED_ROADMAPS, json.encodeToString(dtos)).apply()
        }
    }

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        return ChatApiClient.getRoadmaps(category = category, limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (offset == 0 && category == null) {
                    cachedRoadmaps = list
                    savePersistedRoadmaps(dtos)
                }
                list
            }
            .recoverCatching { error ->
                if (cachedRoadmaps.isNotEmpty()) {
                    if (category != null) {
                        cachedRoadmaps.filter { it.category.equals(category, ignoreCase = true) }
                    } else {
                        cachedRoadmaps
                    }
                } else {
                    throw error
                }
            }
    }

    override suspend fun getCategories(): List<String> {
        return ChatApiClient.getRoadmapCategories()
            .map { cats ->
                if (cats.isNotEmpty()) {
                    cachedCategories = cats
                    savePersistedCategories(cats)
                }
                cats
            }
            .getOrElse {
                if (cachedCategories.isNotEmpty()) cachedCategories else emptyList()
            }
    }

    // --- Personalized Roadmap Assessment & Generation ---

    override suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState> {
        return ChatApiClient.startPersonalizedAssessment()
            .map { it.toDomain() }
    }

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> {
        return ChatApiClient.getActiveAssessmentSession()
            .map { it?.toDomain() }
    }

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> {
        return ChatApiClient.getAssessmentSession(sessionId)
            .map { it.toDomain() }
    }

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> {
        return ChatApiClient.submitAssessmentAnswer(
            sessionId = sessionId,
            answer = answer,
            quizSelectedIndex = quizSelectedIndex
        ).map { it.toDomain() }
    }

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.generatePersonalizedRoadmap(sessionId)
            .map { dto ->
                val domain = dto.toDomain()
                val updated = (listOf(dto) + loadPersistedPersonalizedRoadmapsDtos().filterNot { it.id == dto.id })
                savePersistedPersonalizedRoadmaps(updated)
                cachedPersonalizedRoadmaps = updated.map { it.toDomain() }
                domain
            }
    }

    private fun loadPersistedPersonalizedRoadmapsDtos(): List<PersonalizedRoadmapDetailResponseDto> {
        val sp = prefs ?: return emptyList()
        val raw = sp.getString(KEY_PERSONALIZED_ROADMAPS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PersonalizedRoadmapDetailResponseDto>>(raw)
        }.getOrDefault(emptyList())
    }

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> {
        return ChatApiClient.getMyPersonalizedRoadmaps(limit = limit, offset = offset)
            .map { dtos ->
                val list = dtos.map { it.toDomain() }
                if (offset == 0) {
                    cachedPersonalizedRoadmaps = list
                    savePersistedPersonalizedRoadmaps(dtos)
                }
                list
            }
            .recoverCatching { error ->
                if (cachedPersonalizedRoadmaps.isNotEmpty()) {
                    cachedPersonalizedRoadmaps
                } else {
                    throw error
                }
            }
    }

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.getPersonalizedRoadmapDetail(roadmapId)
            .map { it.toDomain() }
            .recoverCatching { error ->
                val found = cachedPersonalizedRoadmaps.find { it.id == roadmapId }
                found ?: throw error
            }
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        return Result.success(
            GeneratedRoadmapPreview(
                goal = goal,
                level = level,
                studyTime = studyTime,
                duration = "8 weeks",
                stages = listOf(
                    "Phase 1: $interest Core Foundations",
                    "Phase 2: Applied Projects & Architecture",
                    "Phase 3: Portfolio & Capstone Delivery"
                )
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "edunova_roadmaps_cache"
        private const val KEY_ROADMAPS = "persisted_roadmaps"
        private const val KEY_CATEGORIES = "persisted_categories"
        private const val KEY_PERSONALIZED_ROADMAPS = "persisted_personalized_roadmaps"
    }
}
