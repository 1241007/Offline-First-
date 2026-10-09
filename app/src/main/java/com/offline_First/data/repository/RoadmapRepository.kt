package com.offline_First.data.repository

import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.RoadmapOption

/**
 * Repository interface for system and personalized roadmap operations.
 * Handles online API communication, offline caching, and session persistence.
 */
interface RoadmapRepository {
    suspend fun getRoadmaps(
        limit: Int? = null,
        offset: Int = 0,
        category: String? = null
    ): Result<List<RoadmapOption>>

    suspend fun getCategories(): List<String>

    // --- Personalized Roadmap Assessment & Generation ---

    suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState>

    suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?>

    suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState>

    suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int? = null
    ): Result<AssessmentSessionState>

    suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail>

    suspend fun getMyPersonalizedRoadmaps(
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<PersonalizedRoadmapDetail>>

    fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail>

    suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail>

    suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail>

    // --- Offline-First Milestone Progress & Sync ---

    suspend fun getCompletedMilestones(roadmapId: String): Set<String>

    suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>>

    suspend fun syncPendingMilestoneProgress(): Result<Unit>

    suspend fun syncPendingRoadmaps(): Result<Unit>

    // Legacy method for backward compatibility
    suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview>
}
