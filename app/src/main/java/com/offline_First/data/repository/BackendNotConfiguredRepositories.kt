package com.offline_First.data.repository

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

    override fun streamMessage(
        prompt: String,
        parentId: String?,
        clientMessageId: String?
    ): Flow<String> = flowOf()

    override suspend fun stopGeneration(): Result<Unit> = Result.success(Unit)

    override fun regenerateLastResponse(): Flow<String> = flowOf()

    override fun editMessageAndRegenerate(messageId: String, newContent: String): Flow<String> = flowOf()

    override suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit> = Result.success(Unit)

    override suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun loadMoreMessages(sessionId: String, beforeTimestamp: Long?, limit: Int): Result<List<ChatMessage>> =
        unavailable("Chat history")

    override suspend fun loadMoreConversations(beforeCursor: Long?, limit: Int): Result<List<ChatSession>> =
        unavailable("Chat history")

    override suspend fun getMemories(): Result<List<com.offline_First.domain.model.UserMemoryItem>> =
        Result.success(emptyList())

    override suspend fun deleteMemory(memoryId: String): Result<Unit> = Result.success(Unit)

    override suspend fun syncOfflineData(): Result<Unit> = Result.success(Unit)

    override suspend fun signIn(contact: String, password: String): Result<Unit> =
        unavailable("Authentication")

    override suspend fun register(input: RegistrationInput): Result<Unit> =
        unavailable("Authentication")

    override suspend fun requestPasswordReset(contact: String): Result<Unit> =
        unavailable("Authentication")

    override suspend fun logout(refreshToken: String?): Result<Unit> =
        unavailable("Authentication")

    override suspend fun restoreSession(): Result<Boolean> = Result.success(false)

    override fun observeAuthState(): Flow<com.offline_First.data.AuthState> =
        flowOf(com.offline_First.data.AuthState.Unauthenticated)

    override suspend fun fetchMe(): Result<com.offline_First.data.remote.UserDto> =
        Result.failure(UnsupportedOperationException("Backend not configured"))

    override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> = unavailable("Course catalog")

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

    override suspend fun getStudyFocus(): Result<StudyFocusItem?> =
        unavailable("Study plan")

    override suspend fun getUserProfile(): Result<UserProfile?> = Result.success(null)

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> =
        unavailable("User profile")

    override fun observeUserProfile(): Flow<UserProfile?> = flowOf(null)

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> = unavailable("Roadmaps")

    override suspend fun getCategories(): List<String> = emptyList()

    override suspend fun startPersonalizedAssessment(): Result<com.offline_First.domain.model.AssessmentSessionState> =
        unavailable("Personalized roadmap assessment")

    override suspend fun getActiveAssessmentSession(): Result<com.offline_First.domain.model.AssessmentSessionState?> =
        unavailable("Active assessment session")

    override suspend fun getAssessmentSession(sessionId: String): Result<com.offline_First.domain.model.AssessmentSessionState> =
        unavailable("Assessment session")

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<com.offline_First.domain.model.AssessmentSessionState> =
        unavailable("Assessment message")

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<com.offline_First.domain.model.PersonalizedRoadmapDetail> =
        unavailable("Personalized roadmap generation")

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<com.offline_First.domain.model.PersonalizedRoadmapDetail>> =
        unavailable("User personalized roadmaps")

    override fun getMyPersonalizedRoadmapsCached(): List<com.offline_First.domain.model.PersonalizedRoadmapDetail> =
        emptyList()

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<com.offline_First.domain.model.PersonalizedRoadmapDetail> =
        unavailable("Personalized roadmap detail")

    override suspend fun savePersonalizedRoadmap(roadmap: com.offline_First.domain.model.PersonalizedRoadmapDetail): Result<com.offline_First.domain.model.PersonalizedRoadmapDetail> =
        unavailable("Save personalized roadmap")

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> = emptySet()

    override suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>> =
        unavailable("Milestone sync")

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> = Result.success(Unit)

    override suspend fun syncPendingRoadmaps(): Result<Unit> = Result.success(Unit)

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> = unavailable("Personalized roadmap")

    private fun <T> unavailable(feature: String): Result<T> =
        Result.failure(BackendNotConfiguredException(feature))
}
