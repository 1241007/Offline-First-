package com.offline_First.ui.screens.roadmap

import com.offline_First.data.local.PythonRoadmapDataProvider
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
class PythonRoadmapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRoadmapRepository : RoadmapRepository {
        val completedMilestonesStore = mutableSetOf<String>()

        override suspend fun getCompletedMilestones(roadmapId: String): Set<String> {
            return completedMilestonesStore.toSet()
        }

        override suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>> {
            if (completedMilestonesStore.contains(milestoneKey)) {
                completedMilestonesStore.remove(milestoneKey)
            } else {
                completedMilestonesStore.add(milestoneKey)
            }
            return Result.success(completedMilestonesStore.toSet())
        }

        override suspend fun getRoadmaps(limit: Int?, offset: Int, category: String?): Result<List<RoadmapOption>> =
            Result.success(emptyList())

        override suspend fun getCategories(): List<String> = emptyList()

        override suspend fun validateSubject(subject: String): Result<Pair<Boolean, String>> =
            Result.success(Pair(true, subject))

        override suspend fun startPersonalizedAssessment(domain: String): Result<AssessmentSessionState> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getTopicLearningContent(roadmapId: String, topicId: String): Result<TopicLearningContent> =
            Result.failure(UnsupportedOperationException())

        override suspend fun completeTopic(roadmapId: String, topicId: String, quizPassed: Boolean): Result<PersonalizedRoadmapDetail> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> =
            Result.success(null)

        override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> =
            Result.failure(UnsupportedOperationException())

        override suspend fun submitAssessmentAnswer(
            sessionId: String,
            answer: String,
            quizSelectedIndex: Int?
        ): Result<AssessmentSessionState> = Result.failure(UnsupportedOperationException())

        override suspend fun correctProfileField(
            sessionId: String,
            field: String,
            value: String
        ): Result<AssessmentSessionState> = Result.failure(UnsupportedOperationException())

        override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getMyPersonalizedRoadmaps(limit: Int?, offset: Int): Result<List<PersonalizedRoadmapDetail>> =
            Result.success(emptyList())

        override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> = emptyList()

        override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> =
            Result.failure(UnsupportedOperationException())

        override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> =
            Result.success(roadmap)

        override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> = Result.success(Unit)

        override suspend fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String): Result<PersonalizedRoadmapDetail> =
            Result.failure(UnsupportedOperationException())

        override suspend fun syncPendingMilestoneProgress(): Result<Unit> = Result.success(Unit)

        override suspend fun syncPendingRoadmaps(): Result<Unit> = Result.success(Unit)

        override suspend fun generatePersonalizedRoadmap(
            goal: String,
            level: String,
            studyTime: String,
            interest: String
        ): Result<GeneratedRoadmapPreview> = Result.failure(UnsupportedOperationException())
    }

    private lateinit var fakeRepo: FakeRoadmapRepository
    private lateinit var viewModel: PythonRoadmapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeRoadmapRepository()
        viewModel = PythonRoadmapViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsModulesAndTopics() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(8, state.modules.size)
        assertTrue(state.totalTopicsCount >= 20)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.overallPercentage)
    }

    @Test
    fun testDataModelCompleteness() {
        val modules = PythonRoadmapDataProvider.getModules()
        assertTrue("Expected 8 modules", modules.size >= 8)

        for (module in modules) {
            assertTrue("Module title must not be empty", module.title.isNotBlank())
            assertTrue("Module must have topics", module.topics.isNotEmpty())
            for (topic in module.topics) {
                assertTrue("Topic must have learning objectives", topic.learningObjectives.isNotEmpty())
                assertTrue("Topic must have code examples", topic.codeExamples.isNotEmpty())
                assertTrue("Topic must have practice questions", topic.practiceQuestions.isNotEmpty())
                val example = topic.codeExamples.first()
                assertTrue("Code example must have non-empty code", example.code.isNotBlank())
                assertTrue("Code example must have non-empty output", example.expectedOutput.isNotBlank())
            }
        }
    }

    @Test
    fun testMarkTopicCompletedUpdatesProgress() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val firstTopic = viewModel.uiState.value.modules.first().topics.first()
        viewModel.markTopicCompleted(firstTopic.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.completedCount)
        assertEquals(TopicLearningStatus.COMPLETED, state.progress.getStatus(firstTopic.id))
        assertTrue(state.overallPercentage > 0)
        assertNotNull(state.celebrationMessage)
    }

    @Test
    fun testMarkTopicInProgressAndSkip() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val topic = viewModel.uiState.value.modules.first().topics.first()

        // In Progress
        viewModel.markTopicInProgress(topic.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TopicLearningStatus.IN_PROGRESS, viewModel.uiState.value.progress.getStatus(topic.id))
        assertEquals(1, viewModel.uiState.value.inProgressCount)

        // Skip
        viewModel.skipTopic(topic.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TopicLearningStatus.SKIPPED, viewModel.uiState.value.progress.getStatus(topic.id))
        assertEquals(0, viewModel.uiState.value.inProgressCount)
        assertEquals(1, viewModel.uiState.value.skippedCount)

        // Reset
        viewModel.resetTopicStatus(topic.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TopicLearningStatus.NOT_STARTED, viewModel.uiState.value.progress.getStatus(topic.id))
        assertEquals(0, viewModel.uiState.value.skippedCount)
    }

    @Test
    fun testSearchAndFilter() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSearchQuery("decorators")
        assertEquals("decorators", viewModel.uiState.value.searchQuery)

        viewModel.setStatusFilter(TopicLearningStatus.COMPLETED)
        assertEquals(TopicLearningStatus.COMPLETED, viewModel.uiState.value.statusFilter)
    }

    @Test
    fun testModuleExpansionToggling() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val modId = "mod_3_data_structures"
        assertFalse(viewModel.uiState.value.expandedModuleIds.contains(modId))

        viewModel.toggleModuleExpansion(modId)
        assertTrue(viewModel.uiState.value.expandedModuleIds.contains(modId))

        viewModel.expandAllModules()
        assertEquals(viewModel.uiState.value.modules.size, viewModel.uiState.value.expandedModuleIds.size)

        viewModel.collapseAllModules()
        assertTrue(viewModel.uiState.value.expandedModuleIds.isEmpty())
    }
}
