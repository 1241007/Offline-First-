package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import com.offline_First.domain.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OnlineRoadmapRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authenticatedApiClient: AuthenticatedApiClient
    private lateinit var repository: OnlineRoadmapRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens("test_access_token_123", "test_refresh_token_456")
        sessionManager = SessionManager(tokenStorage)

        val publicClient = OkHttpClient.Builder().build()
        authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        ChatApiClient.initialize(authenticatedApiClient.okHttpClient)
        ChatApiClient.customBaseUrl = server.url("").toString().removeSuffix("/")

        repository = OnlineRoadmapRepository(
            context = null,
            userIdProvider = { "test-user-123" }
        )
    }

    @After
    fun tearDown() {
        ChatApiClient.customBaseUrl = null
        server.shutdown()
    }

    @Test
    fun getRoadmaps_fetchesSuccessfullyFromServer() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    [
                        {
                            "id": "sys-1",
                            "title": "Android Developer",
                            "category": "Mobile",
                            "description": "Master Android and Kotlin",
                            "skills": ["Kotlin", "Compose"],
                            "level": "Intermediate",
                            "duration": "10 weeks",
                            "stages": 4,
                            "icon": "phone",
                            "accentTheme": "primary"
                        }
                    ]
                """.trimIndent())
        )

        val result = repository.getRoadmaps()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Android Developer", list[0].title)
        assertEquals("Mobile", list[0].category)
        assertEquals(listOf("Kotlin", "Compose"), list[0].skills)
    }

    @Test
    fun getCategories_fetchesSuccessfullyFromServer() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""["Mobile", "Web", "AI", "Cloud"]""")
        )

        val categories = repository.getCategories()
        assertEquals(listOf("Mobile", "Web", "AI", "Cloud"), categories)
    }

    @Test
    fun getMyPersonalizedRoadmaps_fetchesSuccessfullyFromServer() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    [
                        {
                            "id": "pers-101",
                            "title": "Custom Android Mastery",
                            "goal": "Build offline-first apps",
                            "category": "Mobile Development",
                            "level": "Advanced",
                            "duration": "6 weeks",
                            "stages": 2,
                            "icon": "school",
                            "accentTheme": "primary",
                            "structure": {
                                "title": "Custom Android Mastery",
                                "goal": "Build offline-first apps",
                                "startingLevel": "Advanced",
                                "category": "Mobile Development",
                                "estimatedDuration": "6 weeks",
                                "weeklyHours": 12.0,
                                "assessmentSummary": {
                                    "strengths": ["Kotlin Coroutines"],
                                    "skillGaps": ["SQLite sync"],
                                    "verifiedEvidence": [],
                                    "selfReportedInformation": [],
                                    "unknowns": []
                                },
                                "phases": [
                                    {
                                        "title": "Phase 1: Local Storage",
                                        "objective": "Build SQLite schema",
                                        "durationWeeks": 3,
                                        "topics": ["SQLiteOpenHelper", "Transactions"],
                                        "activities": ["Build local DB"],
                                        "resources": ["Android Docs"],
                                        "milestones": [
                                            {
                                                "title": "Create SQLite tables",
                                                "completionCriteria": ["Implement migrations"],
                                                "assessment": "Code review",
                                                "passingCriteria": "All queries run cleanly"
                                            }
                                        ],
                                        "recommendedCourseIds": []
                                    }
                                ],
                                "weeklySchedule": [],
                                "assumptions": [],
                                "capstoneProject": "Offline Chat App",
                                "nextAction": "Start Phase 1",
                                "completedMilestones": ["Phase 1_M1"]
                            },
                            "items": [],
                            "createdAt": "2026-10-09T12:00:00Z"
                        }
                    ]
                """.trimIndent())
        )

        val result = repository.getMyPersonalizedRoadmaps()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Custom Android Mastery", list[0].title)
        assertEquals(1, list[0].phases.size)
        assertEquals(listOf("Phase 1_M1"), list[0].completedMilestones)
    }

    @Test
    fun getRoadmapMilestones_and_syncRoadmapMilestones_success() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "roadmapId": "pers-101",
                        "completedMilestones": ["Phase 1_M1"]
                    }
                """.trimIndent())
        )

        val milestonesResp = ChatApiClient.getRoadmapMilestones("pers-101").getOrThrow()
        assertEquals("pers-101", milestonesResp.roadmapId)
        assertEquals(listOf("Phase 1_M1"), milestonesResp.completedMilestones)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "roadmapId": "pers-101",
                        "completedMilestones": ["Phase 1_M1", "Phase 1_M2"]
                    }
                """.trimIndent())
        )

        val syncResp = ChatApiClient.syncRoadmapMilestones(
            roadmapId = "pers-101",
            milestoneKey = "Phase 1_M2",
            isCompleted = true
        ).getOrThrow()

        assertEquals("pers-101", syncResp.roadmapId)
        assertEquals(listOf("Phase 1_M1", "Phase 1_M2"), syncResp.completedMilestones)
    }

    @Test
    fun savePersonalizedRoadmap_postsToServerSuccessfully() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""
                    {
                        "id": "pers-saved-102",
                        "title": "Offline-First Mobile Dev",
                        "goal": "Build robust offline apps",
                        "category": "Mobile Development",
                        "level": "Intermediate",
                        "duration": "8 weeks",
                        "stages": 1,
                        "icon": "school",
                        "accentTheme": "primary",
                        "structure": {
                            "title": "Offline-First Mobile Dev",
                            "goal": "Build robust offline apps",
                            "startingLevel": "Intermediate",
                            "category": "Mobile Development",
                            "estimatedDuration": "8 weeks",
                            "weeklyHours": 10.0,
                            "assessmentSummary": {
                                "strengths": ["Kotlin"],
                                "skillGaps": ["Sync Queue"],
                                "verifiedEvidence": [],
                                "selfReportedInformation": [],
                                "unknowns": []
                            },
                            "phases": [],
                            "weeklySchedule": [],
                            "assumptions": [],
                            "capstoneProject": "Offline App",
                            "nextAction": "Start",
                            "completedMilestones": ["Phase 1_M1"]
                        },
                        "items": [],
                        "createdAt": "2026-10-09T14:00:00Z"
                    }
                """.trimIndent())
        )

        val roadmap = PersonalizedRoadmapDetail(
            id = "pers-saved-102",
            title = "Offline-First Mobile Dev",
            goal = "Build robust offline apps",
            category = "Mobile Development",
            level = "Intermediate",
            duration = "8 weeks",
            stages = 1,
            icon = "school",
            accentTheme = RoadmapAccentTheme.PRIMARY,
            weeklyHours = 10.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(strengths = listOf("Kotlin"), skillGaps = listOf("Sync Queue")),
            assumptions = emptyList(),
            capstoneProject = "Offline App",
            nextAction = "Start",
            completedMilestones = listOf("Phase 1_M1")
        )

        val result = repository.savePersonalizedRoadmap(roadmap)
        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals("pers-saved-102", saved.id)
        assertEquals("Offline-First Mobile Dev", saved.title)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/roadmaps/personalized/save", recorded.path)
        assertEquals("POST", recorded.method)
    }

    @Test
    fun savePersonalizedRoadmap_handlesCloudFailureGracefully() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail": "Internal Server Error"}""")
        )

        val roadmap = PersonalizedRoadmapDetail(
            id = "pers-local-only-103",
            title = "Local Resilient Roadmap",
            goal = "Survive cloud downtime",
            category = "Mobile",
            level = "Beginner",
            duration = "4 weeks",
            stages = 1,
            icon = "school",
            accentTheme = RoadmapAccentTheme.PRIMARY,
            weeklyHours = 6.0,
            phases = emptyList(),
            weeklySchedule = emptyList(),
            assessmentSummary = AssessmentSummary(),
            assumptions = emptyList(),
            capstoneProject = "",
            nextAction = "",
            completedMilestones = emptyList()
        )

        // Must succeed locally even if server returns 500 error!
        val result = repository.savePersonalizedRoadmap(roadmap)
        assertTrue(result.isSuccess)
        val returned = result.getOrThrow()
        assertEquals("pers-local-only-103", returned.id)
        assertEquals("Local Resilient Roadmap", returned.title)
    }

    @Test
    fun generatePersonalizedRoadmapOffline_createsValidRoadmapPreview() = runBlocking {
        val result = repository.generatePersonalizedRoadmap(
            goal = "Build Android apps",
            level = "Beginner",
            studyTime = "1 hour/day",
            interest = "Android"
        )
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals("Build Android apps", preview.goal)
        assertEquals("Beginner", preview.level)
        assertEquals("1 hour/day", preview.studyTime)
        assertEquals(3, preview.stages.size)
    }
}

