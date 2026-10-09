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

    override suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetail> =
        generateRoadmapResult

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
}
