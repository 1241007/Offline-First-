package com.offline_First.data.repository

import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.LearningCourse
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class BackendNotConfiguredException(feature: String) :
    IllegalStateException("$feature is not connected. Backend contract required.")

class BackendNotConfiguredRepositories :
    AIRepository,
    AuthRepository,
    CourseRepository,
    LearningRepository,
    ProfileRepository,
    RoadmapRepository {

    private val connectionMode = MutableStateFlow(ConnectionMode.ONLINE)
    private val explanationMode = MutableStateFlow(ExplanationMode.GENERAL)
    private val authSession = MutableStateFlow(false)

    override fun observeSession(): Flow<Boolean> = authSession.asStateFlow()

    override suspend fun restoreSession(): Result<Unit> = Result.success(Unit)

    override fun observeConnectionMode(): Flow<ConnectionMode> = connectionMode.asStateFlow()

    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> {
        connectionMode.value = mode
        return Result.success(Unit)
    }

    override fun observeExplanationMode(): Flow<ExplanationMode> = explanationMode.asStateFlow()

    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        explanationMode.value = mode
        return Result.success(Unit)
    }

    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> =
        flowOf(OfflineAIStatus.NOT_DOWNLOADED)

    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> =
        flowOf(OfflineAIDownloadProgress())

    override suspend fun startOfflineAIDownload(): Result<Unit> = unavailable("Offline AI")

    override suspend fun deleteOfflineAI(): Result<Unit> = unavailable("Offline AI")

    override fun observeChatSessions(): Flow<List<ChatSession>> = flowOf(emptyList())

    override fun observeCurrentSession(): Flow<ChatSession?> = flowOf(null)

    override suspend fun createNewChat(): Result<ChatSession> = unavailable("Chat history")

    override suspend fun selectChat(sessionId: String): Result<Unit> = unavailable("Chat history")

    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> =
        unavailable("Chat history")

    override suspend fun deleteChat(sessionId: String): Result<Unit> = unavailable("Chat history")

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = unavailable("AI chat")

    override suspend fun signIn(contact: String, password: String): Result<Unit> =
        unavailable("Authentication")

    override suspend fun register(input: RegistrationInput): Result<Unit> =
        unavailable("Authentication")

    override suspend fun requestPasswordReset(contact: String): Result<Unit> =
        unavailable("Authentication")

    override suspend fun signOut(): Result<Unit> {
        authSession.value = false
        return Result.success(Unit)
    }

    override suspend fun getCourses(): Result<List<Course>> = unavailable("Course catalog")

    override suspend fun getFeaturedCourses(): Result<List<Course>> =
        unavailable("Featured courses")

    override suspend fun getInProgressCourses(): Result<List<LearningCourse>> =
        unavailable("Learning progress")

    override suspend fun getCompletedCourses(): Result<List<LearningCourse>> =
        unavailable("Learning progress")

    override suspend fun getSubjects(): Result<List<Subject>> = unavailable("School subjects")

    override suspend fun getContinueLearning(): Result<ContinueLearningItem?> =
        unavailable("Learning progress")

    override suspend fun getUpcomingExams(): Result<List<UpcomingExam>> =
        unavailable("Upcoming exams")

    override suspend fun getStudyFocus(): Result<StudyFocusItem?> = unavailable("Study plan")

    override suspend fun getUserProfile(): Result<UserProfile?> = Result.success(null)

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> =
        unavailable("User profile")

    override fun observeUserProfile(): Flow<UserProfile?> = flowOf(null)

    // -----------------------------------------------------------------------
    // RoadmapRepository — static roadmaps
    // -----------------------------------------------------------------------

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> = unavailable("Roadmaps")

    override suspend fun getCategories(): List<String> = emptyList()

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> = emptySet()

    override suspend fun toggleMilestoneProgress(
        roadmapId: String,
        milestoneKey: String
    ): Result<Set<String>> = unavailable("Milestone progress")

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> = Result.success(Unit)

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> = unavailable("Personalized roadmap (legacy)")

    // -----------------------------------------------------------------------
    // RoadmapRepository — diagnostic assessment
    // -----------------------------------------------------------------------

    override suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>> =
        unavailable("Subject validation")

    override suspend fun startPersonalizedAssessment(domain: String): Result<AssessmentSessionState> =
        unavailable("Personalized assessment")

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> =
        Result.success(null)

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> =
        unavailable("Assessment session")

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> = unavailable("Assessment answer submission")

    override suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState> = unavailable("Assessment profile correction")

    // -----------------------------------------------------------------------
    // RoadmapRepository — personalized roadmap generation & persistence
    // -----------------------------------------------------------------------

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> =
        unavailable("Personalized roadmap generation")

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> = unavailable("My personalized roadmaps")

    override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> = emptyList()

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> =
        unavailable("Personalized roadmap detail")

    override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> =
        unavailable("Save personalized roadmap")

    override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> =
        unavailable("Delete personalized roadmap")

    override suspend fun renamePersonalizedRoadmap(
        roadmapId: String,
        newTitle: String
    ): Result<PersonalizedRoadmapDetail> = unavailable("Rename personalized roadmap")

    override suspend fun syncPendingRoadmaps(): Result<Unit> = Result.success(Unit)

    override suspend fun getTopicLearningContent(
        roadmapId: String,
        topicId: String
    ): Result<com.offline_First.domain.model.TopicLearningContent> =
        unavailable("Topic learning content")

    override suspend fun completeTopic(
        roadmapId: String,
        topicId: String,
        quizPassed: Boolean
    ): Result<PersonalizedRoadmapDetail> = unavailable("Topic completion")

    // -----------------------------------------------------------------------
    // Internal helper
    // -----------------------------------------------------------------------

    private fun <T> unavailable(feature: String): Result<T> =
        Result.failure(BackendNotConfiguredException(feature))
}
