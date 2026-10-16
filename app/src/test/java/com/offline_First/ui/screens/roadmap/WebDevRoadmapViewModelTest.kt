package com.offline_First.ui.screens.roadmap

import com.offline_First.data.local.WebDevRoadmapDataProvider
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
class WebDevRoadmapViewModelTest {

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

        override suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState> =
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
    private lateinit var viewModel: WebDevRoadmapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeRoadmapRepository()
        viewModel = WebDevRoadmapViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDataModelCompleteness() {
        val nodes = WebDevRoadmapDataProvider.getNodes()
        assertTrue("Expected 25+ nodes in Web Dev roadmap", nodes.size >= 25)

        val coreMilestones = nodes.filter { it.nodeType == WebDevNodeType.CORE_MILESTONE }
        assertTrue("Expected core spine milestones", coreMilestones.size >= 10)

        val internetNode = nodes.firstOrNull { it.id == "core_internet" }
        assertNotNull(internetNode)
        assertEquals("Internet", internetNode?.label)

        val reactNode = nodes.firstOrNull { it.id == "branch_react" }
        assertNotNull(reactNode)
        assertEquals(RecommendationType.PERSONAL_RECOMMENDATION, reactNode?.recommendation)
    }

    @Test
    fun testMarkNodeCompleted() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val node = viewModel.uiState.value.nodes.first()
        viewModel.markNodeCompleted(node.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.completedCount)
        assertTrue(state.isCompleted(node.id))
        assertFalse(state.isInProgress(node.id))
        assertTrue(state.progressPercentage > 0)
    }

    @Test
    fun testMarkNodeInProgressAndReset() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val node = viewModel.uiState.value.nodes.first()

        // In Progress
        viewModel.markNodeInProgress(node.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isInProgress(node.id))
        assertFalse(viewModel.uiState.value.isCompleted(node.id))

        // Reset
        viewModel.resetNode(node.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isInProgress(node.id))
        assertFalse(viewModel.uiState.value.isCompleted(node.id))
    }
}
