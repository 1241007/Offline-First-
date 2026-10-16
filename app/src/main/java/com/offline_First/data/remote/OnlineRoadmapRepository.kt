package com.offline_First.data.remote

import android.util.Log
import com.offline_First.data.local.LocalPersonalizedRoadmapEngine
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.RoadmapOption

private const val TAG = "OnlineRoadmapRepo"

/**
 * Remote implementation of [RoadmapRepository].
 *
 * INTEGRATION STATE (personal branch):
 * - Static roadmaps (getRoadmaps, getCategories): ✅ wired to backend.
 * - Milestone progress (getCompletedMilestones, toggleMilestoneProgress): ⚠️ local-only SharedPrefs
 *   until the milestone progress API is added in a future migration.
 * - Personalized assessment & roadmap generation: ✅ wired to backend assessment API.
 * - Personalized roadmap persistence: ✅ wired to backend personalized-roadmaps API.
 */
class OnlineRoadmapRepository : RoadmapRepository {

    // In-memory cache for fast UI rendering between refreshes
    private val _roadmapCache = mutableListOf<PersonalizedRoadmapDetail>()

    // Local milestone progress store (keyed: "$roadmapId/$milestoneKey")
    // Replaced with backend sync in syncPendingMilestoneProgress()
    private val _localMilestones = mutableMapOf<String, MutableSet<String>>()

    // -----------------------------------------------------------------------
    // Static roadmap catalogue
    // -----------------------------------------------------------------------

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
            accentTheme = mapAccentTheme(accentTheme)
        )
    }

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        return ChatApiClient.getRoadmaps()
            .map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getCategories(): List<String> {
        return ChatApiClient.getRoadmapCategories().getOrElse { emptyList() }
    }

    // -----------------------------------------------------------------------
    // Milestone progress (local-first, pending backend API)
    // -----------------------------------------------------------------------

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> {
        return _localMilestones[roadmapId]?.toSet() ?: emptySet()
    }

    override suspend fun toggleMilestoneProgress(
        roadmapId: String,
        milestoneKey: String
    ): Result<Set<String>> {
        val set = _localMilestones.getOrPut(roadmapId) { mutableSetOf() }
        if (set.contains(milestoneKey)) {
            set.remove(milestoneKey)
        } else {
            set.add(milestoneKey)
        }
        return Result.success(set.toSet())
    }

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> {
        // INTEGRATION POINT: POST /api/v1/learning/milestones/sync
        // Currently a no-op until that endpoint is added.
        Log.d(TAG, "syncPendingMilestoneProgress: pending backend milestone sync endpoint")
        return Result.success(Unit)
    }

    // -----------------------------------------------------------------------
    // Legacy single-overload (kept for RoadmapBuilderScreen compatibility)
    // -----------------------------------------------------------------------

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> {
        // Not implemented in Phase 1 — RoadmapBuilderScreen now redirects to assessment flow
        return Result.failure(UnsupportedOperationException(
            "Use startPersonalizedAssessment() to begin the personalized roadmap flow."
        ))
    }

    // -----------------------------------------------------------------------
    // Diagnostic Assessment (Offline-First: tries backend, falls back to local engine)
    // -----------------------------------------------------------------------

    override suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>> {
        val remote = ChatApiClient.validateSubject(subject)
            .map { Pair(it.isValid, it.standardizedName) }
        if (remote.isSuccess) return remote
        Log.w(TAG, "Backend validateSubject failed, using local validation for '$subject'")
        val clean = subject.trim()
        val isValid = clean.length >= 2
        return Result.success(Pair(isValid, clean.replaceFirstChar { it.uppercase() }))
    }

    override suspend fun startPersonalizedAssessment(domain: String): Result<AssessmentSessionState> {
        val remote = ChatApiClient.startAssessment(domain).map { dto -> dto.toDomain() }
        if (remote.isSuccess) return remote
        Log.w(TAG, "Backend startAssessment failed (${remote.exceptionOrNull()?.message}), using LocalPersonalizedRoadmapEngine fallback")
        return Result.success(LocalPersonalizedRoadmapEngine.startAssessment().copy(domain = domain))
    }

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> {
        val remote = ChatApiClient.getActiveAssessmentSession().map { dto -> dto?.toDomain() }
        if (remote.isSuccess) return remote
        return Result.success(null)
    }

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> {
        val remote = ChatApiClient.getAssessmentSession(sessionId).map { dto -> dto.toDomain() }
        if (remote.isSuccess) return remote
        return Result.success(LocalPersonalizedRoadmapEngine.startAssessment())
    }

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> {
        val remote = ChatApiClient.submitAssessmentAnswer(sessionId, answer, quizSelectedIndex)
            .map { dto -> dto.toDomain() }
        if (remote.isSuccess) return remote
        Log.w(TAG, "Backend submitAssessmentAnswer failed (${remote.exceptionOrNull()?.message}), using LocalPersonalizedRoadmapEngine")
        return Result.success(LocalPersonalizedRoadmapEngine.submitAnswer(sessionId, answer, quizSelectedIndex))
    }

    override suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState> {
        val remote = ChatApiClient.correctProfileField(sessionId, field, value)
            .map { dto -> dto.toDomain() }
        if (remote.isSuccess) return remote
        return Result.success(LocalPersonalizedRoadmapEngine.correctProfileField(sessionId, field, value))
    }

    // -----------------------------------------------------------------------
    // Personalized Roadmap Generation & Persistence
    // -----------------------------------------------------------------------

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> {
        val remote = ChatApiClient.generatePersonalizedRoadmap(sessionId)
            .map { dto ->
                dto.toDomain().also { roadmap ->
                    _roadmapCache.removeIf { it.id == roadmap.id }
                    _roadmapCache.add(0, roadmap)
                }
            }
        if (remote.isSuccess) return remote
        Log.w(TAG, "Backend generatePersonalizedRoadmap failed (${remote.exceptionOrNull()?.message}), synthesizing locally via LocalPersonalizedRoadmapEngine")
        val local = LocalPersonalizedRoadmapEngine.generateRoadmap(sessionId)
        _roadmapCache.removeIf { it.id == local.id }
        _roadmapCache.add(0, local)
        return Result.success(local)
    }

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> {
        return ChatApiClient.getMyPersonalizedRoadmaps(limit, offset)
            .map { dtos ->
                dtos.map { it.toDomain() }.also { roadmaps ->
                    _roadmapCache.clear()
                    _roadmapCache.addAll(roadmaps)
                }
            }
    }

    override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> {
        return _roadmapCache.toList()
    }

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.getPersonalizedRoadmapDetail(roadmapId)
            .map { dto -> dto.toDomain() }
            .recoverCatching {
                _roadmapCache.find { it.id == roadmapId } ?: error("Roadmap not found")
            }
    }

    override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.savePersonalizedRoadmap(roadmap)
            .map { dto ->
                dto.toDomain().also { updated ->
                    _roadmapCache.removeIf { it.id == updated.id }
                    _roadmapCache.add(0, updated)
                }
            }
    }

    override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> {
        return ChatApiClient.deletePersonalizedRoadmap(roadmapId)
            .also { result ->
                if (result.isSuccess) _roadmapCache.removeIf { it.id == roadmapId }
            }
    }

    override suspend fun renamePersonalizedRoadmap(
        roadmapId: String,
        newTitle: String
    ): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.renamePersonalizedRoadmap(roadmapId, newTitle)
            .map { dto ->
                dto.toDomain().also { updated ->
                    _roadmapCache.removeIf { it.id == updated.id }
                    _roadmapCache.add(0, updated)
                }
            }
    }

    override suspend fun syncPendingRoadmaps(): Result<Unit> {
        // INTEGRATION POINT: Future offline-first sync of pending personalized roadmap edits.
        Log.d(TAG, "syncPendingRoadmaps: no pending sync needed (roadmaps are saved immediately)")
        return Result.success(Unit)
    }

    override suspend fun getTopicLearningContent(
        roadmapId: String,
        topicId: String
    ): Result<com.offline_First.domain.model.TopicLearningContent> {
        return ChatApiClient.getTopicLearningContent(roadmapId, topicId)
            .map { it.toDomain() }
    }

    override suspend fun completeTopic(
        roadmapId: String,
        topicId: String,
        quizPassed: Boolean
    ): Result<PersonalizedRoadmapDetail> {
        return ChatApiClient.completeTopic(roadmapId, topicId, quizPassed)
            .map { dto ->
                val domainRoadmap = dto.roadmap.toDomain()
                _roadmapCache.removeIf { it.id == domainRoadmap.id }
                _roadmapCache.add(0, domainRoadmap)
                domainRoadmap
            }
            .recoverCatching { error ->
                Log.w(TAG, "Backend completeTopic failed ($error), updating locally")
                val current = getPersonalizedRoadmapDetail(roadmapId).getOrThrow()
                val updated = current.copy(
                    completedTopicIds = current.completedTopicIds + topicId
                ).withRecomputedStatuses()
                savePersonalizedRoadmap(updated).getOrThrow()
            }
    }
}
