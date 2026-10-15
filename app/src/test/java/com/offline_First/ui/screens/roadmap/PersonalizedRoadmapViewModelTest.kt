package com.offline_First.ui.screens.roadmap

import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeRoadmapRepository : RoadmapRepository {
    var roadmapsList: List<RoadmapOption> = listOf(
        RoadmapOption(
            id = "r1",
            title = "Fullstack Web",
            category = "Web Dev",
            description = "Learn fullstack web development",
            skills = listOf("HTML", "CSS", "JS", "Python"),
            level = "Beginner",
            duration = "12 weeks",
            stages = 5,
            icon = "code"
        )
    )
    var categoriesList: List<String> = listOf("All", "Web Dev", "AI", "Mobile")
    var myPersonalizedRoadmapsList: List<PersonalizedRoadmapDetail> = emptyList()
    var activeAssessmentSession: AssessmentSessionState? = null
    var startAssessmentResult: Result<AssessmentSessionState> = Result.success(
        AssessmentSessionState(
            id = "sess-101",
            state = "NEEDS_MORE_INFO",
            messages = listOf(
                AssessmentMessage(
                    speaker = AssessmentSpeaker.MENTOR,
                    text = "Hello! What is your primary learning goal?"
                )
            ),
            completenessPercentage = 25
        )
    )
    var submitAnswerResult: Result<AssessmentSessionState>? = null
    var generateRoadmapResult: Result<PersonalizedRoadmapDetail> = Result.success(
        PersonalizedRoadmapDetail(
            id = "pm-201",
            title = "Personalized Fullstack Roadmap",
            goal = "Become a fullstack engineer",
            category = "Web Development",
            level = "Intermediate",
            duration = "8 weeks",
            stages = 1,
            icon = "code",
            weeklyHours = 10.0,
            phases = listOf(
                PersonalizedPhase(
                    title = "Phase 1: Backend Fundamentals",
                    objective = "Master FastAPI & DB design",
                    durationWeeks = 4,
                    topics = listOf("FastAPI", "SQLAlchemy"),
                    activities = listOf("Build REST API"),
                    resources = listOf("FastAPI Documentation"),
                    milestones = listOf(
                        PersonalizedMilestone(
                            title = "Build REST API with FastAPI",
                            completionCriteria = listOf("Deploy working CRUD API with tests"),
                            assessment = "Implement CRUD endpoint",
                            passingCriteria = "All tests pass",
                            isCompleted = false
                        )
                    ),
                    recommendedCourseIds = listOf("course-fastapi")
                )
            ),
            weeklySchedule = listOf(
                WeeklyScheduleItem(
                    dayOrWeek = "Mon",
                    focusTopic = "FastAPI endpoints",
                    estimatedHours = 2.0,
                    tasks = listOf("Setup route")
                )
            ),
            assessmentSummary = AssessmentSummary(
                strengths = listOf("Python syntax"),
                skillGaps = listOf("SQLAlchemy async"),
                verifiedEvidence = listOf("Answered Python quiz correctly"),
                selfReportedInformation = listOf("3 years Python experience"),
                unknowns = emptyList()
            ),
            assumptions = listOf("Has basic Python knowledge"),
            capstoneProject = "Fullstack Task Manager",
            nextAction = "Start Phase 1",
            createdAt = "2026-10-09T10:00:00Z"
        )
    )

    override suspend fun getRoadmaps(
        limit: Int?,
        offset: Int,
        category: String?
    ): Result<List<RoadmapOption>> {
        val filtered = if (category != null && category != "All") {
            roadmapsList.filter { it.category == category }
        } else {
            roadmapsList
        }
        return Result.success(filtered)
    }

    override suspend fun getCategories(): List<String> = categoriesList

    override suspend fun getActiveAssessmentSession(): Result<AssessmentSessionState?> =
        Result.success(activeAssessmentSession)

    override suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionState> =
        Result.success(
            activeAssessmentSession ?: startAssessmentResult.getOrThrow()
        )

    override suspend fun startPersonalizedAssessment(): Result<AssessmentSessionState> =
        startAssessmentResult

    override suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionState> {
        return submitAnswerResult ?: Result.success(
            AssessmentSessionState(
                id = sessionId,
                state = "DIAGNOSTIC_QUIZ",
                messages = listOf(
                    AssessmentMessage(speaker = AssessmentSpeaker.LEARNER, text = answer),
                    AssessmentMessage(
                        speaker = AssessmentSpeaker.MENTOR,
                        text = "Great! Here is a quick diagnostic question."
                    )
                ),
                quiz = DiagnosticQuiz(
                    question = "Which keyword creates an async function in Python?",
                    options = listOf("def async", "async def", "fn", "coroutine"),
                    skillTested = "Python Async"
                ),
                completenessPercentage = 60
            )
        )
    }

    override suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetail> {
        val detail = generateRoadmapResult.getOrThrow()
        myPersonalizedRoadmapsList = myPersonalizedRoadmapsList + detail
        return Result.success(detail)
    }

    override suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetail>> =
        Result.success(myPersonalizedRoadmapsList)

    override fun getMyPersonalizedRoadmapsCached(): List<PersonalizedRoadmapDetail> =
        myPersonalizedRoadmapsList

    var completedMilestonesMap: MutableMap<String, MutableSet<String>> = mutableMapOf()

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> =
        generateRoadmapResult

    override suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetail> {
        myPersonalizedRoadmapsList = (listOf(roadmap) + myPersonalizedRoadmapsList).distinctBy { it.id }
        return Result.success(roadmap)
    }

    override suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> {
        myPersonalizedRoadmapsList = myPersonalizedRoadmapsList.filter { it.id != roadmapId }
        completedMilestonesMap.remove(roadmapId)
        return Result.success(Unit)
    }

    override suspend fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String): Result<PersonalizedRoadmapDetail> {
        val target = myPersonalizedRoadmapsList.find { it.id == roadmapId } ?: generateRoadmapResult.getOrNull()?.takeIf { it.id == roadmapId }
        val updated = target?.copy(title = newTitle) ?: PersonalizedRoadmapDetail(
            id = roadmapId,
            title = newTitle,
            goal = "Renamed",
            category = "General",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "school"
        )
        myPersonalizedRoadmapsList = myPersonalizedRoadmapsList.map { if (it.id == roadmapId) updated else it }
        if (!myPersonalizedRoadmapsList.any { it.id == roadmapId }) {
            myPersonalizedRoadmapsList = myPersonalizedRoadmapsList + updated
        }
        return Result.success(updated)
    }

    override suspend fun getCompletedMilestones(roadmapId: String): Set<String> =
        completedMilestonesMap[roadmapId] ?: emptySet()


    override suspend fun toggleMilestoneProgress(roadmapId: String, milestoneKey: String): Result<Set<String>> {
        val set = completedMilestonesMap.getOrPut(roadmapId) { mutableSetOf() }
        if (set.contains(milestoneKey)) {
            set.remove(milestoneKey)
        } else {
            set.add(milestoneKey)
        }
        return Result.success(set.toSet())
    }

    override suspend fun syncPendingMilestoneProgress(): Result<Unit> = Result.success(Unit)

    override suspend fun syncPendingRoadmaps(): Result<Unit> = Result.success(Unit)


    override suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionState> {
        val current = activeAssessmentSession ?: startAssessmentResult.getOrThrow()
        val updated = when (field) {
            "goal" -> current.copy(goal = value)
            "target_level" -> current.copy(targetLevel = value)
            "weekly_hours" -> current.copy(weeklyHours = value.toDoubleOrNull() ?: 8.0)
            "target_timeline" -> current.copy(targetTimeline = value)
            else -> current
        }
        activeAssessmentSession = updated
        return Result.success(updated)
    }

    override suspend fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ): Result<GeneratedRoadmapPreview> = Result.success(
        GeneratedRoadmapPreview(
            goal = goal,
            level = level,
            studyTime = studyTime,
            duration = "8 weeks",
            stages = listOf("Stage 1", "Stage 2")
        )
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizedRoadmapViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeRoadmapRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeRoadmapRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialization_loadsRoadmapsAndPersonalizedRoadmaps() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state.roadmaps is UiState.Success)
        assertEquals(1, (state.roadmaps as UiState.Success).data.size)
        assertEquals("Fullstack Web", (state.roadmaps as UiState.Success).data.first().title)
        assertEquals(4, state.categories.size)
        assertTrue(state.myPersonalizedRoadmaps.isEmpty())
    }

    @Test
    fun startOrResumeAssessment_resumesActiveSessionWhenAvailable() = runTest {
        val existingSession = AssessmentSessionState(
            id = "sess-active",
            state = "DIAGNOSTIC_QUIZ",
            messages = listOf(
                AssessmentMessage(speaker = AssessmentSpeaker.MENTOR, text = "Welcome back! Continuing assessment.")
            ),
            completenessPercentage = 50
        )
        fakeRepo.activeAssessmentSession = existingSession

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment(forceNew = false)

        val state = viewModel.uiState.value
        assertNotNull(state.assessmentSession)
        assertEquals("sess-active", state.assessmentSession?.id)
        assertEquals("Welcome back! Continuing assessment.", state.assessmentMessages.first().text)
    }

    @Test
    fun startOrResumeAssessment_forceNew_startsFreshSession() = runTest {
        val existingSession = AssessmentSessionState(
            id = "sess-active",
            state = "COMPLETED",
            messages = emptyList(),
            completenessPercentage = 100
        )
        fakeRepo.activeAssessmentSession = existingSession

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment(forceNew = true)

        val state = viewModel.uiState.value
        assertNotNull(state.assessmentSession)
        assertEquals("sess-101", state.assessmentSession?.id)
        assertEquals("Hello! What is your primary learning goal?", state.assessmentMessages.first().text)
    }

    @Test
    fun selectQuizOption_updatesSelectedQuizIndex() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.selectQuizOption(2)

        assertEquals(2, viewModel.uiState.value.selectedQuizIndex)
    }

    @Test
    fun submitAssessmentAnswer_sendsAnswerAndReceivesQuizWithoutAnswerKey() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()

        // Submit learner's answer
        viewModel.submitAssessmentAnswer("I want to learn Python backend", quizSelectedIndex = null)

        val state = viewModel.uiState.value
        assertFalse(state.isSendingAssessmentMessage)
        assertNotNull(state.assessmentSession?.quiz)
        val quiz = state.assessmentSession?.quiz
        assertEquals("Which keyword creates an async function in Python?", quiz?.question)
        assertEquals(4, quiz?.options?.size)
        assertEquals("Python Async", quiz?.skillTested)
        // Diagnostic quiz on Android never exposes correct answers before submission
        assertNull(state.selectedQuizIndex)
    }

    @Test
    fun generatePersonalizedRoadmap_successSetsRoadmapDetailAndLegacyPreview() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()

        viewModel.generatePersonalizedRoadmap()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertNotNull(state.generatedPersonalizedRoadmap)
        assertEquals("Personalized Fullstack Roadmap", state.generatedPersonalizedRoadmap?.title)
        assertEquals(1, state.generatedPersonalizedRoadmap?.phases?.size)
        assertEquals("Phase 1: Backend Fundamentals", state.generatedPersonalizedRoadmap?.phases?.first()?.title)
        assertEquals("Build REST API with FastAPI", state.generatedPersonalizedRoadmap?.phases?.first()?.milestones?.first()?.title)

        // Legacy preview for compatibility
        assertNotNull(state.generatedRoadmap)
        assertEquals("Become a fullstack engineer", state.generatedRoadmap?.goal)

        // Stored into my personalized roadmaps list
        assertEquals(1, state.myPersonalizedRoadmaps.size)
    }

    @Test
    fun toggleMilestone_tracksCompletedState() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)

        viewModel.toggleMilestone("m-1")
        assertTrue(viewModel.uiState.value.completedMilestones.contains("m-1"))

        viewModel.toggleMilestone("m-2")
        assertTrue(viewModel.uiState.value.completedMilestones.contains("m-1"))
        assertTrue(viewModel.uiState.value.completedMilestones.contains("m-2"))

        viewModel.toggleMilestone("m-1")
        assertFalse(viewModel.uiState.value.completedMilestones.contains("m-1"))
        assertTrue(viewModel.uiState.value.completedMilestones.contains("m-2"))
    }

    @Test
    fun resetAssessment_resetsSessionAndStartsFresh() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()
        viewModel.generatePersonalizedRoadmap()
        assertNotNull(viewModel.uiState.value.generatedPersonalizedRoadmap)

        viewModel.resetAssessment()

        val state = viewModel.uiState.value
        assertNull(state.generatedPersonalizedRoadmap)
        assertNull(state.generatedRoadmap)
        assertNotNull(state.assessmentSession)
        assertEquals("sess-101", state.assessmentSession?.id)
    }

    @Test
    fun startAssessment_failureSetsErrorStateAndStopsLoading() = runTest {
        fakeRepo.startAssessmentResult = Result.failure(RuntimeException("HTTP 500: Internal Server Error"))
        fakeRepo.activeAssessmentSession = null
        val viewModel = RoadmapViewModel(repository = fakeRepo)

        viewModel.startOrResumeAssessment(forceNew = true)

        val state = viewModel.uiState.value
        assertFalse(state.isAssessmentLoading)
        assertNull(state.assessmentSession)
        assertTrue(state.assessmentMessages.isEmpty())
        assertNotNull(state.assessmentError)
        assertTrue(state.assessmentError!!.contains("Server temporarily unavailable") || state.assessmentError!!.contains("500"))
    }

    @Test
    fun retryAssessment_repeatsRequestAndRecoversOnSuccess() = runTest {
        // First simulate a failure
        fakeRepo.startAssessmentResult = Result.failure(RuntimeException("HTTP 500: Internal Server Error"))
        fakeRepo.activeAssessmentSession = null
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment(forceNew = true)

        assertNotNull(viewModel.uiState.value.assessmentError)

        // Now fix repository and retry
        fakeRepo.startAssessmentResult = Result.success(
            AssessmentSessionState(
                id = "sess-recovered",
                state = "COLLECTING_GOALS",
                messages = listOf(
                    AssessmentMessage(
                        speaker = AssessmentSpeaker.MENTOR,
                        text = "Welcome back! What would you like to learn?"
                    )
                ),
                completenessPercentage = 20
            )
        )

        viewModel.retryAssessment()

        val recoveredState = viewModel.uiState.value
        assertFalse(recoveredState.isAssessmentLoading)
        assertNull(recoveredState.assessmentError)
        assertNotNull(recoveredState.assessmentSession)
        assertEquals("sess-recovered", recoveredState.assessmentSession?.id)
        assertEquals(1, recoveredState.assessmentMessages.size)
    }

    @Test
    fun selectPersonalizedRoadmap_loadsPersistedCompletedMilestones() = runTest {
        val testRoadmap = fakeRepo.generateRoadmapResult.getOrThrow()
        fakeRepo.myPersonalizedRoadmapsList = listOf(testRoadmap)
        fakeRepo.completedMilestonesMap[testRoadmap.id] = mutableSetOf("Phase 1_M1", "Phase 1_M2")

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.selectPersonalizedRoadmap(testRoadmap)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testRoadmap, state.selectedPersonalizedRoadmap)
        assertEquals(setOf("Phase 1_M1", "Phase 1_M2"), state.completedMilestones)
    }

    @Test
    fun toggleMilestone_persistsProgressLocallyAndInRepository() = runTest {
        val testRoadmap = fakeRepo.generateRoadmapResult.getOrThrow()
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.selectPersonalizedRoadmap(testRoadmap)
        testScheduler.advanceUntilIdle()

        // Toggle first milestone on
        viewModel.toggleMilestone("Phase 1_M1")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.completedMilestones.contains("Phase 1_M1"))
        assertEquals(setOf("Phase 1_M1"), fakeRepo.completedMilestonesMap[testRoadmap.id])

        // Toggle second milestone on
        viewModel.toggleMilestone("Phase 1_M2")
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("Phase 1_M1", "Phase 1_M2"), viewModel.uiState.value.completedMilestones)
        assertEquals(setOf("Phase 1_M1", "Phase 1_M2"), fakeRepo.completedMilestonesMap[testRoadmap.id])

        // Toggle first milestone off
        viewModel.toggleMilestone("Phase 1_M1")
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("Phase 1_M2"), viewModel.uiState.value.completedMilestones)
        assertEquals(setOf("Phase 1_M2"), fakeRepo.completedMilestonesMap[testRoadmap.id])
    }

    @Test
    fun generatePersonalizedRoadmap_persistsAndInitializesMilestones() = runTest {
        fakeRepo.activeAssessmentSession = AssessmentSessionState(
            id = "sess-ready",
            state = "READY_FOR_GENERATION",
            completenessPercentage = 100
        )
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()
        testScheduler.advanceUntilIdle()

        viewModel.generatePersonalizedRoadmap()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertNotNull(state.generatedPersonalizedRoadmap)
        assertNotNull(state.selectedPersonalizedRoadmap)
        assertEquals("pm-201", state.selectedPersonalizedRoadmap?.id)
        assertEquals(1, state.myPersonalizedRoadmaps.size)
    }

    @Test
    fun loadMyPersonalizedRoadmaps_loadsCachedRoadmapsImmediatelyOnAppStart() = runTest {
        val preCachedRoadmap = PersonalizedRoadmapDetail(
            id = "cached-roadmap-1",
            title = "Cached Android Dev Path",
            goal = "Offline reading",
            category = "Mobile",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "phone",
            weeklyHours = 5.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(),
            assumptions = emptyList(),
            capstoneProject = "",
            nextAction = "",
            completedMilestones = listOf("M1")
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(preCachedRoadmap)

        // On App/Screen startup, ViewModel must load cached data immediately
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        assertEquals(1, viewModel.uiState.value.myPersonalizedRoadmaps.size)
        assertEquals("Cached Android Dev Path", viewModel.uiState.value.myPersonalizedRoadmaps[0].title)
    }

    @Test
    fun savePersonalizedRoadmap_persistsLocallyAndUpdatesUiState() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        val customRoadmap = PersonalizedRoadmapDetail(
            id = "custom-pers-555",
            title = "Custom Offline AI Engineering",
            goal = "Master on-device LLMs",
            category = "AI",
            level = "Advanced",
            duration = "12 weeks",
            stages = 2,
            icon = "psychology",
            weeklyHours = 15.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(),
            assumptions = emptyList(),
            capstoneProject = "Offline Agent",
            nextAction = "Build engine",
            completedMilestones = emptyList()
        )

        viewModel.savePersonalizedRoadmap(customRoadmap)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.myPersonalizedRoadmaps.any { it.id == "custom-pers-555" })
        assertEquals("Custom Offline AI Engineering", state.selectedPersonalizedRoadmap?.title)
    }

    @Test
    fun syncPendingChanges_syncsQueuedChangesAndRefreshesState() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        val testRoadmap = PersonalizedRoadmapDetail(
            id = "roadmap-sync-test",
            title = "Sync Test Roadmap",
            goal = "Verify idempotent sync",
            category = "General",
            level = "Intermediate",
            duration = "6 weeks",
            stages = 1,
            icon = "school",
            weeklyHours = 8.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(),
            assumptions = emptyList(),
            capstoneProject = "",
            nextAction = "",
            completedMilestones = emptyList()
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(testRoadmap)

        viewModel.selectPersonalizedRoadmap(testRoadmap)
        viewModel.toggleMilestone("Milestone_A")
        testScheduler.advanceUntilIdle()

        // Trigger sync
        viewModel.syncPendingChanges()
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("Milestone_A"), viewModel.uiState.value.completedMilestones)
        assertEquals(1, viewModel.uiState.value.myPersonalizedRoadmaps.size)
    }

    @Test
    fun repeatedSync_avoidsDuplicatesAndMaintainsIdempotency() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        val testRoadmap = PersonalizedRoadmapDetail(
            id = "roadmap-idempotent-test",
            title = "Idempotent Roadmap",
            goal = "No duplicates",
            category = "General",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "school",
            weeklyHours = 5.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(),
            assumptions = emptyList(),
            capstoneProject = "",
            nextAction = "",
            completedMilestones = emptyList()
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(testRoadmap)

        // Multiple sync calls
        viewModel.syncPendingChanges()
        testScheduler.advanceUntilIdle()
        viewModel.syncPendingChanges()
        testScheduler.advanceUntilIdle()

        val matching = viewModel.uiState.value.myPersonalizedRoadmaps.filter { it.id == "roadmap-idempotent-test" }
        assertEquals(1, matching.size)
    }

    @Test
    fun submitAssessmentAnswer_chip_and_freeText_unified_pipeline() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()

        var chipCallbackSuccess = false
        // Submit answer from predefined chip
        viewModel.submitAssessmentAnswer("2-4 hours/week") { success ->
            chipCallbackSuccess = success
        }
        testScheduler.advanceUntilIdle()

        assertTrue(chipCallbackSuccess)
        val state = viewModel.uiState.value
        assertFalse(state.isSendingAssessmentMessage)
        assertNotNull(state.assessmentSession)
        assertEquals(2, state.assessmentMessages.size)
        assertEquals("2-4 hours/week", state.assessmentMessages[0].text)

        var textCallbackSuccess = false
        // Submit answer from free-text
        viewModel.submitAssessmentAnswer("I want to master Kotlin Coroutines") { success ->
            textCallbackSuccess = success
        }
        testScheduler.advanceUntilIdle()

        assertTrue(textCallbackSuccess)
    }

    @Test
    fun submitAssessmentAnswer_failure_rollsBack_and_calls_completion_with_false() = runTest {
        fakeRepo.submitAnswerResult = Result.failure(RuntimeException("Network timeout"))
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()
        testScheduler.advanceUntilIdle()

        val initialMessages = viewModel.uiState.value.assessmentMessages
        val initialCount = initialMessages.size

        var completionResult: Boolean? = null
        viewModel.submitAssessmentAnswer("Failed answer attempt") { success ->
            completionResult = success
        }
        testScheduler.advanceUntilIdle()

        assertEquals(false, completionResult)
        val state = viewModel.uiState.value
        assertFalse(state.isSendingAssessmentMessage)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Network timeout"))
        // Assert optimistic message was rolled back to prevent ghost/duplicate entries
        assertEquals(initialCount, state.assessmentMessages.size)
    }

    @Test
    fun startOrResumeAssessment_preserves_inMemory_session_when_not_forced() = runTest {
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment(forceNew = false)
        testScheduler.advanceUntilIdle()

        val session1 = viewModel.uiState.value.assessmentSession
        assertNotNull(session1)

        // Calling startOrResumeAssessment again without forceNew retains in-memory session without resetting
        viewModel.startOrResumeAssessment(forceNew = false)
        testScheduler.advanceUntilIdle()

        assertSame(session1, viewModel.uiState.value.assessmentSession)
    }

    @Test
    fun assessment_quick_reply_options_update_from_server_response() = runTest {
        fakeRepo.submitAnswerResult = Result.success(
            AssessmentSessionState(
                id = "sess-101",
                state = "ASSESSING_AVAILABILITY",
                options = listOf("2-4 hours/week", "5-10 hours/week", "15+ hours/week"),
                messages = listOf(
                    AssessmentMessage(
                        speaker = AssessmentSpeaker.MENTOR,
                        text = "How many hours per week can you study?",
                        options = listOf("2-4 hours/week", "5-10 hours/week", "15+ hours/week")
                    )
                ),
                completenessPercentage = 60
            )
        )

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()
        testScheduler.advanceUntilIdle()

        viewModel.submitAssessmentAnswer("Learn Python")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ASSESSING_AVAILABILITY", state.assessmentSession?.state)
        assertEquals(listOf("2-4 hours/week", "5-10 hours/week", "15+ hours/week"), state.assessmentSession?.options)
    }

    @Test
    fun multiRoadmap_creatingSecondRoadmap_preservesFirstRoadmap() = runTest {
        val roadmap1 = PersonalizedRoadmapDetail(
            id = "roadmap-python-1",
            title = "Python for Data Science",
            goal = "Learn Python for data analysis",
            category = "Data Science",
            level = "Advanced",
            duration = "8 weeks",
            stages = 2,
            icon = "code",
            weeklyHours = 6.0
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(roadmap1)

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        assertEquals(1, viewModel.uiState.value.myPersonalizedRoadmaps.size)

        // User starts a new assessment for Kotlin Android
        viewModel.startNewAssessment()
        testScheduler.advanceUntilIdle()

        // Existing roadmaps remain intact
        assertEquals(1, viewModel.uiState.value.myPersonalizedRoadmaps.size)
        assertEquals("roadmap-python-1", viewModel.uiState.value.myPersonalizedRoadmaps[0].id)

        // Generate second roadmap
        val roadmap2 = PersonalizedRoadmapDetail(
            id = "roadmap-kotlin-2",
            title = "Kotlin Android App Development",
            goal = "Build Android apps with Kotlin",
            category = "Mobile",
            level = "Intermediate",
            duration = "10 weeks",
            stages = 3,
            icon = "phone",
            weeklyHours = 8.0
        )
        fakeRepo.generateRoadmapResult = Result.success(roadmap2)
        viewModel.generatePersonalizedRoadmap()
        testScheduler.advanceUntilIdle()

        // Both roadmaps exist independently
        val roadmaps = viewModel.uiState.value.myPersonalizedRoadmaps
        assertEquals(2, roadmaps.size)
        assertTrue(roadmaps.any { it.id == "roadmap-python-1" && it.title == "Python for Data Science" })
        assertTrue(roadmaps.any { it.id == "roadmap-kotlin-2" && it.title == "Kotlin Android App Development" })
    }

    @Test
    fun multiRoadmap_independentMilestoneProgress_doesNotCrossContaminate() = runTest {
        val roadmap1 = PersonalizedRoadmapDetail(
            id = "r-python",
            title = "Python Path",
            goal = "Python",
            category = "General",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "code",
            phases = listOf(
                PersonalizedPhase(
                    title = "P1",
                    objective = "Basics",
                    durationWeeks = 2,
                    milestones = listOf(PersonalizedMilestone(title = "Python M1"))
                )
            )
        )
        val roadmap2 = PersonalizedRoadmapDetail(
            id = "r-kotlin",
            title = "Kotlin Path",
            goal = "Kotlin",
            category = "General",
            level = "Advanced",
            duration = "4 weeks",
            stages = 1,
            icon = "phone",
            phases = listOf(
                PersonalizedPhase(
                    title = "P1",
                    objective = "Compose",
                    durationWeeks = 2,
                    milestones = listOf(PersonalizedMilestone(title = "Kotlin M1"))
                )
            )
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(roadmap1, roadmap2)

        val viewModel = RoadmapViewModel(repository = fakeRepo)

        // Select Python and complete its milestone
        viewModel.selectPersonalizedRoadmap(roadmap1)
        testScheduler.advanceUntilIdle()
        viewModel.toggleMilestone("r-python_0_0_Python M1")
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("r-python_0_0_Python M1"), fakeRepo.completedMilestonesMap["r-python"])
        assertNull(fakeRepo.completedMilestonesMap["r-kotlin"])

        // Select Kotlin and verify Python's milestone didn't leak
        viewModel.selectPersonalizedRoadmap(roadmap2)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.completedMilestones.isEmpty())

        // Complete Kotlin milestone
        viewModel.toggleMilestone("r-kotlin_0_0_Kotlin M1")
        testScheduler.advanceUntilIdle()

        assertEquals(setOf("r-python_0_0_Python M1"), fakeRepo.completedMilestonesMap["r-python"])
        assertEquals(setOf("r-kotlin_0_0_Kotlin M1"), fakeRepo.completedMilestonesMap["r-kotlin"])
    }

    @Test
    fun multiRoadmap_deleteRoadmap_removesCorrectItemOnly() = runTest {
        val roadmap1 = PersonalizedRoadmapDetail(id = "r-1", title = "Roadmap 1", goal = "Goal 1", category = "A", level = "Beginner", duration = "4w", stages = 1, icon = "1")
        val roadmap2 = PersonalizedRoadmapDetail(id = "r-2", title = "Roadmap 2", goal = "Goal 2", category = "B", level = "Advanced", duration = "6w", stages = 2, icon = "2")
        fakeRepo.myPersonalizedRoadmapsList = listOf(roadmap1, roadmap2)

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        assertEquals(2, viewModel.uiState.value.myPersonalizedRoadmaps.size)

        viewModel.deletePersonalizedRoadmap("r-1")
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.myPersonalizedRoadmaps.size)
        assertEquals("r-2", viewModel.uiState.value.myPersonalizedRoadmaps[0].id)
    }

    @Test
    fun multiRoadmap_renameRoadmap_updatesTitleCorrectly() = runTest {
        val roadmap1 = PersonalizedRoadmapDetail(id = "r-1", title = "Original Title", goal = "Goal 1", category = "A", level = "Beginner", duration = "4w", stages = 1, icon = "1")
        fakeRepo.myPersonalizedRoadmapsList = listOf(roadmap1)

        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.renamePersonalizedRoadmap("r-1", "Updated Custom Title")
        testScheduler.advanceUntilIdle()

        assertEquals("Updated Custom Title", viewModel.uiState.value.myPersonalizedRoadmaps[0].title)
    }

    @Test
    fun progressCalculation_and_nextActionableMilestone_areAccurate() = runTest {
        val roadmap = PersonalizedRoadmapDetail(
            id = "r-progress",
            title = "Progress Test",
            goal = "Test",
            category = "Test",
            level = "Intermediate",
            duration = "8 weeks",
            stages = 2,
            icon = "code",
            phases = listOf(
                PersonalizedPhase(
                    title = "Phase 1",
                    objective = "Obj 1",
                    durationWeeks = 2,
                    milestones = listOf(
                        PersonalizedMilestone(title = "Milestone 1", isCompleted = false),
                        PersonalizedMilestone(title = "Milestone 2", isCompleted = false)
                    )
                ),
                PersonalizedPhase(
                    title = "Phase 2",
                    objective = "Obj 2",
                    durationWeeks = 2,
                    milestones = listOf(
                        PersonalizedMilestone(title = "Milestone 3", isCompleted = false),
                        PersonalizedMilestone(title = "Milestone 4", isCompleted = false)
                    )
                )
            )
        )

        val viewModel = RoadmapViewModel(repository = fakeRepo)

        // 0 of 4 milestones completed
        assertEquals(0, viewModel.calculateRoadmapProgress(roadmap, emptySet()))
        assertEquals("Milestone 1", viewModel.getNextActionableMilestone(roadmap, emptySet()))

        // 2 of 4 milestones completed (50%)
        val completedSet = setOf("r-progress_0_0_Milestone 1", "r-progress_0_1_Milestone 2")
        assertEquals(50, viewModel.calculateRoadmapProgress(roadmap, completedSet))
        assertEquals("Milestone 3", viewModel.getNextActionableMilestone(roadmap, completedSet))

        // All 4 milestones completed (100%)
        val allCompleted = setOf(
            "r-progress_0_0_Milestone 1",
            "r-progress_0_1_Milestone 2",
            "r-progress_1_0_Milestone 3",
            "r-progress_1_1_Milestone 4"
        )
        assertEquals(100, viewModel.calculateRoadmapProgress(roadmap, allCompleted))
        assertEquals("All milestones completed!", viewModel.getNextActionableMilestone(roadmap, allCompleted))
    }

    @Test
    fun correctProfileField_updatesSessionStateCorrectly() = runTest {
        val initialSession = AssessmentSessionState(
            id = "sess-corr",
            state = "READY_FOR_GENERATION",
            goal = "Original Goal",
            targetLevel = "Beginner",
            weeklyHours = 8.0,
            targetTimeline = "8 weeks"
        )
        fakeRepo.activeAssessmentSession = initialSession
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.startOrResumeAssessment()
        testScheduler.advanceUntilIdle()

        viewModel.correctProfileField("goal", "Master Android Kotlin")
        testScheduler.advanceUntilIdle()
        assertEquals("Master Android Kotlin", viewModel.uiState.value.assessmentSession?.goal)

        viewModel.correctProfileField("weekly_hours", 15.0)
        testScheduler.advanceUntilIdle()
        assertEquals(15.0, viewModel.uiState.value.assessmentSession?.weeklyHours)

        viewModel.correctProfileField("target_level", "Intermediate")
        testScheduler.advanceUntilIdle()
        assertEquals("Intermediate", viewModel.uiState.value.assessmentSession?.targetLevel)
    }

    @Test
    fun toggleTask_updatesCompletedMilestonesAndProgress() = runTest {
        val roadmap = PersonalizedRoadmapDetail(
            id = "r-task-test",
            title = "Task Test",
            goal = "Test Goal",
            category = "Testing",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "code",
            phases = listOf(
                PersonalizedPhase(
                    title = "Phase 1",
                    objective = "Learn tasks",
                    durationWeeks = 2,
                    tasks = listOf(
                        PersonalizedTask(id = "t1", title = "Task 1", estimatedHours = 2.0),
                        PersonalizedTask(id = "t2", title = "Task 2", estimatedHours = 3.0)
                    ),
                    milestones = listOf(
                        PersonalizedMilestone(title = "Milestone 1", isCompleted = false)
                    )
                )
            )
        )
        fakeRepo.myPersonalizedRoadmapsList = listOf(roadmap)
        val viewModel = RoadmapViewModel(repository = fakeRepo)
        viewModel.selectPersonalizedRoadmap(roadmap)
        testScheduler.advanceUntilIdle()

        // 0 of 3 items done (0%)
        assertEquals(0, viewModel.calculateRoadmapProgress(roadmap))
        assertFalse(viewModel.isTaskCompleted("r-task-test", "t1"))

        // Toggle task 1
        viewModel.toggleTask("t1")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.isTaskCompleted("r-task-test", "t1"))
        // 1 of 3 items done (33%)
        assertEquals(33, viewModel.calculateRoadmapProgress(roadmap))

        // Toggle task 2 and milestone 1
        viewModel.toggleTask("t2")
        viewModel.toggleMilestone("r-task-test_0_0_Milestone 1")
        testScheduler.advanceUntilIdle()

        // 3 of 3 items done (100%)
        assertEquals(100, viewModel.calculateRoadmapProgress(roadmap))
    }
}




