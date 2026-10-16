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

    suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState>

    suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail>

    suspend fun getMyPersonalizedRoadmaps(
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<PersonalizedRoadmapDetail>>

    fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail>

    suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail>

    suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail>
    
    suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit>

    suspend fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String): Result<PersonalizedRoadmapDetail>


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

    // -----------------------------------------------------------------------
    // Diagnostic Assessment
    // -----------------------------------------------------------------------

    /**
     * Validates if a user-entered course/technology is a legitimate curriculum subject.
     * Returns Pair(isValid, standardizedName).
     */
    suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>>

    /**
     * Starts a new personalized assessment session for [domain].
     * Returns the initial [AssessmentSessionState] containing the first profile question.
     */
    suspend fun startPersonalizedAssessment(domain: String = "Python"): Result<AssessmentSessionState>

    /**
     * Returns the currently active (in-progress) assessment session, or null if none.
     */
    suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?>

    /**
     * Returns a specific assessment session by [sessionId].
     */
    suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState>

    /**
     * Submits the learner's answer to the current question.
     * [answer] is the free-text or option text.
     * [quizSelectedIndex] is the 0-based option index for MC / code-output questions (null for free text).
     * Returns the updated [AssessmentSessionState] with the next question or completion summary.
     */
    suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int? = null
    ): Result<AssessmentSessionState>

    /**
     * Learner explicitly corrects a profile field (e.g. after a mismatch prompt).
     * [field] is one of "goal", "level", "studyHours", etc.
     */
    suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState>

    // -----------------------------------------------------------------------
    // Personalized Roadmap Generation & Persistence
    // -----------------------------------------------------------------------

    /**
     * Triggers server-side roadmap generation from a completed assessment session.
     * Returns the fully-planned [PersonalizedRoadmapDetail] including topics and edges.
     */
    suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail>

    /**
     * Returns the learner's saved personalized roadmaps, newest first.
     */
    suspend fun getMyPersonalizedRoadmaps(
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<PersonalizedRoadmapDetail>>

    /**
     * Returns cached (last-fetched) personalized roadmaps without a network call.
     * Suitable for fast UI rendering while a refresh is in flight.
     */
    fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail>

    /**
     * Returns the full detail of a specific personalized roadmap by [roadmapId].
     */
    suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail>

    /**
     * Persists (creates or updates) a personalized roadmap locally and/or remotely.
     * Used to save offline-first progress updates back to the backend.
     */
    suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail>

    /**
     * Permanently deletes a personalized roadmap and all associated topic progress.
     */
    suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit>

    /**
     * Renames a personalized roadmap.
     */
    suspend fun renamePersonalizedRoadmap(
        roadmapId: String,
        newTitle: String
    ): Result<PersonalizedRoadmapDetail>

    /**
     * Pushes any locally-pending personalized roadmap changes to the remote backend.
     */
    suspend fun syncPendingRoadmaps(): Result<Unit>

    /**
     * Retrieves rich, dedicated learning module for [topicId] within [roadmapId].
     */
    suspend fun getTopicLearningContent(roadmapId: String, topicId: String): Result<com.offline_First.domain.model.TopicLearningContent>

    /**
     * Marks [topicId] completed, re-evaluates prerequisites, and returns updated roadmap.
     */
    suspend fun completeTopic(roadmapId: String, topicId: String, quizPassed: Boolean = true): Result<PersonalizedRoadmapDetail>
}
