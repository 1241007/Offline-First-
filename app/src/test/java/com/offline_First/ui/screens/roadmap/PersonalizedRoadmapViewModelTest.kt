package com.offline_First.ui.screens.roadmap

import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizedRoadmapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRoadmapRepository : RoadmapRepository {
        var startAssessmentResult: Result<AssessmentSessionState> = Result.success(
            AssessmentSessionState(
                sessionId = "test-session-123",
                domain = "Python",
                phase = AssessmentPhase.PROFILE_COLLECTION,
                questionNumber = 1,
                totalQuestions = 7,
                currentQuestion = AssessmentQuestion(
                    id = "q1",
                    topicTag = "profile_goal",
                    questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                    prompt = "What is your primary goal?",
                    options = listOf("Get a job", "Build projects")
                )
            )
        )

        var submitAnswerResult: Result<AssessmentSessionState> = Result.success(
            AssessmentSessionState(
                sessionId = "test-session-123",
                domain = "Python",
                phase = AssessmentPhase.DIAGNOSTIC,
                questionNumber = 4,
                totalQuestions = 7,
                lastAnswerCorrect = true,
                currentQuestion = AssessmentQuestion(
                    id = "q4",
                    topicTag = "python_basics",
                    questionType = AssessmentQuestionType.CODE_OUTPUT,
                    prompt = "What will print(type(4 / 2)) output?",
                    options = listOf("int", "float")
                )
            )
        )

        val sampleRoadmap = PersonalizedRoadmapDetail(
            id = "roadmap-123",
            title = "My Python Path",
            domain = "Python",
            goal = "Get a job",
            level = "Beginner",
            studyHoursPerWeek = 5,
            estimatedWeeks = 8,
            topics = listOf(
                PersonalizedRoadmapTopic(
                    id = "topic-1",
                    topicTag = "python_basics",
                    title = "Variables & Types",
                    prerequisiteTopicIds = emptyList(),
                    status = PersonalizedTopicStatus.UNLOCKED,
                    priorityScore = 50
                ),
                PersonalizedRoadmapTopic(
                    id = "topic-2",
                    topicTag = "python_loops",
                    title = "Loops & Iterations",
                    prerequisiteTopicIds = listOf("topic-1"),
                    status = PersonalizedTopicStatus.LOCKED,
                    priorityScore = 90
                )
            ),
            edges = listOf(PersonalizedRoadmapEdge("topic-1", "topic-2")),
            assessmentSessionId = "test-session-123"
        )

        val roadmapsStore = mutableListOf<PersonalizedRoadmapDetail>()

        override suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState> = startAssessmentResult

        override suspend fun submitAssessmentAnswer(
            sessionId: String,
            answer: String,
            quizSelectedIndex: Int?
        ): Result<AssessmentSessionState> = submitAnswerResult

        override suspend fun correctProfileField(
            sessionId: String,
            field: String,
            value: String
        ): Result<AssessmentSessionState> = Result.success(
            AssessmentSessionState(sessionId = sessionId, profileLevel = value)
        )

        override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> {
            roadmapsStore.add(sampleRoadmap)
            return Result.success(sampleRoadmap)
        }

        override suspend fun getMyPersonalizedRoadmaps(limit: Int?, offset: Int): Result<List<PersonalizedRoadmapDetail>> =
            Result.success(roadmapsStore.toList())

        override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> = roadmapsStore.toList()

        override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> {
            val found = roadmapsStore.find { it.id == roadmapId } ?: sampleRoadmap
            return Result.success(found)
        }

        override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> {
            roadmapsStore.removeIf { it.id == roadmap.id }
            roadmapsStore.add(roadmap)
            return Result.success(roadmap)
        }

        override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> {
            roadmapsStore.removeIf { it.id == roadmapId }
            return Result.success(Unit)
        }

        override suspend fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String): Result<PersonalizedRoadmapDetail> {
            val found = roadmapsStore.find { it.id == roadmapId } ?: sampleRoadmap
            val updated = found.copy(title = newTitle)
            roadmapsStore.removeIf { it.id == roadmapId }
            roadmapsStore.add(updated)
            return Result.success(updated)
        }

        override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> = Result.success(null)
        override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> = startAssessmentResult
        override suspend fun getRoadmaps(limit: Int?, offset: Int, category: String?): Result<List<RoadmapOption>> = Result.success(emptyList())
        override suspend fun getCategories(): List<String> = emptyList()
        override suspend fun getCompletedMilestones(roadmapId: String): Set<String> = emptySet()
        override suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>> = Result.success(emptySet())
        override suspend fun syncPendingMilestoneProgress(): Result<Unit> = Result.success(Unit)
        override suspend fun syncPendingRoadmaps(): Result<Unit> = Result.success(Unit)
        override suspend fun generatePersonalizedRoadmap(goal: String, level: String, studyTime: String, interest: String): Result<GeneratedRoadmapPreview> =
            Result.failure(UnsupportedOperationException())
    }

    private lateinit var fakeRepo: FakeRoadmapRepository
    private lateinit var viewModel: PersonalizedRoadmapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeRoadmapRepository()
        viewModel = PersonalizedRoadmapViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartAssessmentLoadsFirstQuestion() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.assessmentState)
        assertEquals("test-session-123", state.assessmentState?.sessionId)
        assertEquals(AssessmentPhase.PROFILE_COLLECTION, state.assessmentState?.phase)
        assertEquals(1, state.assessmentState?.questionNumber)
    }

    @Test
    fun testSubmitAnswerUpdatesSession() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitAnswer("Get a job", quizSelectedIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AssessmentPhase.DIAGNOSTIC, state.assessmentState?.phase)
        assertEquals(4, state.assessmentState?.questionNumber)
        assertTrue(state.assessmentState?.lastAnswerCorrect == true)
    }

    @Test
    fun testGenerateRoadmapCreatesActiveRoadmap() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateRoadmap()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeRoadmap)
        assertEquals("My Python Path", state.activeRoadmap?.title)
        assertEquals(2, state.activeRoadmap?.topics?.size)
        assertEquals(0, state.activeRoadmap?.progressPercentage)
    }

    @Test
    fun testToggleTopicCompletedUnlocksPrerequisiteDownstream() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateRoadmap()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially topic-1 is UNLOCKED, topic-2 is LOCKED
        val initialRoadmap = viewModel.uiState.value.activeRoadmap!!
        assertEquals(PersonalizedTopicStatus.UNLOCKED, initialRoadmap.topics[0].status)
        assertEquals(PersonalizedTopicStatus.LOCKED, initialRoadmap.topics[1].status)

        // Complete topic-1
        viewModel.toggleTopicCompleted("topic-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedRoadmap = viewModel.uiState.value.activeRoadmap!!
        assertEquals(PersonalizedTopicStatus.COMPLETED, updatedRoadmap.topics[0].status)
        // With topic-1 completed, topic-2 should now be UNLOCKED!
        assertEquals(PersonalizedTopicStatus.UNLOCKED, updatedRoadmap.topics[1].status)
        assertEquals(50, updatedRoadmap.progressPercentage)
    }

    @Test
    fun testRecommendedNextTopicSelectsHighestPriorityUnlocked() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateRoadmap()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially only topic-1 is unlocked
        assertEquals("topic-1", viewModel.uiState.value.activeRoadmap?.recommendedNextTopic?.id)

        // Complete topic-1 -> topic-2 unlocks with priority 90
        viewModel.toggleTopicCompleted("topic-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("topic-2", viewModel.uiState.value.activeRoadmap?.recommendedNextTopic?.id)
    }

    @Test
    fun testRenameRoadmapUpdatesState() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateRoadmap()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameRoadmap("roadmap-123", "Advanced Python Mastery")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Advanced Python Mastery", viewModel.uiState.value.activeRoadmap?.title)
    }

    @Test
    fun testDeleteRoadmapRemovesFromActiveAndSaved() = runTest {
        viewModel.startAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateRoadmap()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteRoadmap("roadmap-123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeRoadmap)
        assertTrue(viewModel.uiState.value.savedRoadmaps.isEmpty())
    }
}
