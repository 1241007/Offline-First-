package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteProfileAndLearningRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authenticatedApiClient: AuthenticatedApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens("test_access_token", "test_refresh_token")
        sessionManager = SessionManager(tokenStorage)
        val publicClient = OkHttpClient.Builder().build()
        authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getProfileParsesSchoolModeAndHeadersProperly() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "fullName": "Aarav Sharma",
                        "email": "aarav@example.com",
                        "mobile": "9876543210",
                        "interests": "Science",
                        "level": "Class 10",
                        "educationMode": "school"
                    }
                """.trimIndent())
        )

        val repo = RemoteProfileRepository(
            context = null,
            authenticatedApiClient = authenticatedApiClient,
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        val result = repo.getUserProfile()
        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertNotNull(profile)
        assertEquals("Aarav Sharma", profile?.fullName)
        assertEquals("aarav@example.com", profile?.email)
        assertEquals(EducationMode.SCHOOL, profile?.educationMode)
        assertEquals(EducationMode.SCHOOL, repo.observeUserProfile().first()?.educationMode)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/profile", recorded.path)
        assertEquals("Bearer test_access_token", recorded.getHeader("Authorization"))
    }

    @Test
    fun updateProfileSendsPutAndUpdatesState() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "fullName": "Aarav Sharma",
                        "email": "aarav@example.com",
                        "mobile": "9876543210",
                        "interests": "Science",
                        "level": "Class 10",
                        "educationMode": "school"
                    }
                """.trimIndent())
        )

        val repo = RemoteProfileRepository(
            context = null,
            authenticatedApiClient = authenticatedApiClient,
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        val updated = UserProfile(
            fullName = "Aarav Sharma",
            email = "aarav@example.com",
            mobile = "9876543210",
            interests = "Science",
            level = "Class 10",
            educationMode = EducationMode.SCHOOL
        )

        val res = repo.updateUserProfile(updated)
        assertTrue(res.isSuccess)
        assertEquals(EducationMode.SCHOOL, repo.observeUserProfile().first()?.educationMode)

        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/api/v1/profile", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"educationMode\":\"school\""))
    }

    @Test
    fun getInProgressCoursesParsesFromBackend() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    [
                        {
                            "id": "course-123",
                            "name": "Kotlin Essentials",
                            "lesson": "Introduction to Coroutines",
                            "progress": 0.45
                        }
                    ]
                """.trimIndent())
        )

        val repo = RemoteLearningRepository(
            authenticatedApiClient = authenticatedApiClient,
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        val result = repo.getInProgressCourses()
        assertTrue(result.isSuccess)
        val courses = result.getOrThrow()
        assertEquals(1, courses.size)
        assertEquals("Kotlin Essentials", courses[0].name)
        assertEquals(0.45f, courses[0].progress, 0.001f)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/learning/courses/in-progress", recorded.path)
        assertEquals("Bearer test_access_token", recorded.getHeader("Authorization"))
    }
}
