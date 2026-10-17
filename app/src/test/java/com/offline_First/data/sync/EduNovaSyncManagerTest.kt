package com.offline_First.data.sync

import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryChatCacheDatabase
import com.offline_First.data.local.InMemoryTokenStorage
import com.offline_First.data.local.LocalLessonProgressRecord
import com.offline_First.data.local.LocalSavedCourse
import com.offline_First.data.local.LocalSavedRoadmap
import com.offline_First.data.remote.ChatApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class FakeNetworkMonitor(initialOnline: Boolean = false) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

class EduNovaSyncManagerTest {

    private lateinit var testDb: InMemoryChatCacheDatabase
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
    private val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mock HTTP routing
    private val mockResponses = mutableMapOf<Pair<String, String>, Pair<Int, String>>()
    private val executedRequests = mutableListOf<Pair<String, String>>()
    var throwNetworkError = false

    @Before
    fun setUp() {
        testDb = InMemoryChatCacheDatabase()
        tokenStorage = InMemoryTokenStorage()
        sessionManager = SessionManager(tokenStorage)
        fakeNetworkMonitor = FakeNetworkMonitor(initialOnline = true)
        mockResponses.clear()
        executedRequests.clear()
        throwNetworkError = false

        ChatApiClient.customBaseUrl = "http://mock.edunova.local"
        ChatApiClient.okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                if (throwNetworkError) {
                    throw IOException("Simulated network outage")
                }

                val req = chain.request()
                val path = req.url.encodedPath
                val method = req.method
                executedRequests.add(method to path)

                val matched = mockResponses[method to path] ?: run {
                    val courseMatch = Regex(".*/courses/([^/]+)/save.*").find(path)
                    val roadmapMatch = Regex(".*/roadmaps/([^/]+)/save.*").find(path)
                    val lessonMatch = Regex(".*/learning/lessons/([^/]+)/complete.*").find(path)
                    when {
                        courseMatch != null -> {
                            val cId = courseMatch.groupValues[1]
                            Pair(200, "{\"success\":true,\"courseId\":\"$cId\",\"isSaved\":${method == "POST"}}")
                        }
                        roadmapMatch != null -> {
                            val rId = roadmapMatch.groupValues[1]
                            Pair(200, "{\"success\":true,\"roadmapId\":\"$rId\",\"isSaved\":${method == "POST"}}")
                        }
                        lessonMatch != null -> Pair(200, "{\"success\":true}")
                        path.endsWith("/courses/saved") -> Pair(200, "[]")
                        path.endsWith("/roadmaps/saved") -> Pair(200, "[]")
                        else -> Pair(200, "{\"status\":\"ok\"}")
                    }
                }

                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(matched.first)
                    .message(if (matched.first in 200..299) "OK" else "Error HTTP ${matched.first}")
                    .body(matched.second.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    @After
    fun tearDown() {
        ChatApiClient.customBaseUrl = null
    }

    @Test
    fun testRestrictedOfflineSessionDefersSync() = runBlocking {
        // User is restored into a restricted offline session
        sessionManager.onRestoreOfflineSession("user-offline", "offline@edunova.org", "Offline User")

        testDb.savedCourses["user-offline" to "course-py"] = LocalSavedCourse(
            userId = "user-offline",
            courseId = "course-py",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val result = syncManager.syncNow()
        assertTrue(result.isSuccess)

        // No remote HTTP requests should have been made
        assertTrue(executedRequests.isEmpty())

        // Record should remain pending_sync
        assertEquals(1, testDb.getPendingSavedCourses("user-offline").size)
        assertEquals(1, syncManager.syncStatus.value.pendingOperationsCount)
    }

    @Test
    fun testPendingCourseSaveAndUnsaveSynchronization() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        // 1. Pending save
        testDb.savedCourses["user-1" to "course-algo"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-algo",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        mockResponses["POST" to "/api/v1/courses/course-algo/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-algo\",\"isSaved\":true}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val syncResult = syncManager.syncNow()
        assertTrue(syncResult.isSuccess)
        assertEquals(SyncState.SUCCESS, syncManager.syncStatus.value.state)
        assertEquals(0, syncManager.syncStatus.value.pendingOperationsCount)

        // Course must be marked synced in DB
        val course = testDb.savedCourses["user-1" to "course-algo"]
        assertNotNull(course)
        assertEquals("synced", course?.syncStatus)

        // 2. Queue unsave
        testDb.queueUnsaveCourse("user-1", "course-algo")
        assertEquals(1, testDb.getPendingUnsavedCourses("user-1").size)
        mockResponses["DELETE" to "/api/v1/courses/course-algo/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-algo\",\"isSaved\":false}"
        )

        val unsaveSyncResult = syncManager.syncNow()
        assertTrue(unsaveSyncResult.isSuccess)
        // Record must be completely removed from DB
        assertFalse(testDb.savedCourses.containsKey("user-1" to "course-algo"))
    }

    @Test
    fun testPendingRoadmapSaveAndUnsaveSynchronization() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        // 1. Pending roadmap save
        testDb.savedRoadmaps["user-1" to "roadmap-devops"] = LocalSavedRoadmap(
            userId = "user-1",
            roadmapId = "roadmap-devops",
            roadmapType = "system",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        mockResponses["POST" to "/api/v1/roadmaps/roadmap-devops/save"] = Pair(
            200,
            "{\"success\":true,\"roadmapId\":\"roadmap-devops\",\"isSaved\":true}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        syncManager.syncNow()
        val r = testDb.savedRoadmaps["user-1" to "roadmap-devops"]
        assertNotNull(r)
        assertEquals("synced", r?.syncStatus)

        // 2. Queue unsave
        testDb.queueUnsaveRoadmap("user-1", "roadmap-devops")
        mockResponses["DELETE" to "/api/v1/roadmaps/roadmap-devops/save"] = Pair(
            200,
            "{\"success\":true,\"roadmapId\":\"roadmap-devops\",\"isSaved\":false}"
        )

        syncManager.syncNow()
        assertFalse(testDb.savedRoadmaps.containsKey("user-1" to "roadmap-devops"))
    }

    @Test
    fun testUnsave404IsTreatedAsSuccessfullyUnsaved() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.savedCourses["user-1" to "course-ghost"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-ghost",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_unsave"
        )

        // Server returns 404 because record was already absent
        mockResponses["DELETE" to "/api/v1/courses/course-ghost/save"] = Pair(
            404,
            "{\"detail\":\"Course not saved\"}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val res = syncManager.syncNow()
        assertTrue(res.isSuccess)
        // Record must be deleted locally
        assertFalse(testDb.savedCourses.containsKey("user-1" to "course-ghost"))
        assertEquals(0, testDb.getPendingOperationsCount("user-1"))
    }

    @Test
    fun testPermanent4xxErrorMarksOperationFailedInsteadOfEndlessRetry() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.savedCourses["user-1" to "course-corrupt"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-corrupt",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        // Server returns 422 Unprocessable Entity
        mockResponses["POST" to "/api/v1/courses/course-corrupt/save"] = Pair(
            422,
            "{\"detail\":\"Unprocessable Entity HTTP 422\"}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val res = syncManager.syncNow()
        assertTrue(res.isSuccess)
        // Item should be marked failed in DB
        val item = testDb.savedCourses["user-1" to "course-corrupt"]
        assertEquals("failed", item?.syncStatus)
        // No longer counted as pending sync
        assertEquals(0, testDb.getPendingSavedCourses("user-1").size)
    }

    @Test
    fun testTransientNetworkFailureRetainsPendingQueueForRetry() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.savedCourses["user-1" to "course-retry"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-retry",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        // Simulate network failure
        throwNetworkError = true

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val res = syncManager.syncNow()
        assertTrue(res.isFailure)
        assertEquals(SyncState.FAILED, syncManager.syncStatus.value.state)

        // Record must still be pending_sync
        assertEquals(1, testDb.getPendingSavedCourses("user-1").size)

        // Recovery: network returns
        throwNetworkError = false
        mockResponses["POST" to "/api/v1/courses/course-retry/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-retry\",\"isSaved\":true}"
        )

        val retryRes = syncManager.syncNow()
        assertTrue(retryRes.isSuccess)
        assertEquals(SyncState.SUCCESS, syncManager.syncStatus.value.state)
        assertEquals(0, testDb.getPendingSavedCourses("user-1").size)
        assertEquals("synced", testDb.savedCourses["user-1" to "course-retry"]?.syncStatus)
    }

    @Test
    fun testLessonProgressSynchronization() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.lessonProgress["user-1" to "lesson-101"] = LocalLessonProgressRecord(
            userId = "user-1",
            courseId = "course-cs",
            lessonId = "lesson-101",
            isCompleted = true,
            completionTimestamp = System.currentTimeMillis(),
            syncStatus = "pending_sync"
        )

        mockResponses["POST" to "/api/v1/learning/lessons/lesson-101/complete"] = Pair(
            200,
            "{\"status\":\"completed\"}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val res = syncManager.syncNow()
        assertTrue(res.isSuccess)
        assertEquals("synced", testDb.lessonProgress["user-1" to "lesson-101"]?.syncStatus)
    }

    @Test
    fun testStrictUserIsolationAliceVsBob() = runBlocking {
        // Alice has pending course
        testDb.savedCourses["user-alice" to "course-alice"] = LocalSavedCourse(
            userId = "user-alice",
            courseId = "course-alice",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        // Bob has pending course
        testDb.savedCourses["user-bob" to "course-bob"] = LocalSavedCourse(
            userId = "user-bob",
            courseId = "course-bob",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        // Bob logs in
        sessionManager.onLoginSuccess("acc", "ref", "user-bob", "bob@edunova.org", "Bob")

        mockResponses["POST" to "/api/v1/courses/course-bob/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-bob\",\"isSaved\":true}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        syncManager.syncNow()

        // Bob's course synced
        assertEquals("synced", testDb.savedCourses["user-bob" to "course-bob"]?.syncStatus)

        // Alice's course untouched
        assertEquals("pending_sync", testDb.savedCourses["user-alice" to "course-alice"]?.syncStatus)
        assertEquals(1, testDb.getPendingSavedCourses("user-alice").size)
    }

    @Test
    fun testAutoSyncTriggeredWhenNetworkTransitionsOnline() = runBlocking {
        val netMonitor = FakeNetworkMonitor(initialOnline = false)
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.savedCourses["user-1" to "course-auto"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-auto",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        mockResponses["POST" to "/api/v1/courses/course-auto/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-auto\",\"isSaved\":true}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = netMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        // Initially offline
        assertEquals(1, testDb.getPendingSavedCourses("user-1").size)
        delay(100)

        // Network connection transitions to online
        netMonitor.setOnline(true)

        // Wait briefly for flow collector to fire auto-sync
        var retries = 0
        while (testDb.getPendingSavedCourses("user-1").isNotEmpty() && retries < 40) {
            delay(100)
            retries++
        }

        assertEquals(0, testDb.getPendingSavedCourses("user-1").size)
        assertEquals("synced", testDb.savedCourses["user-1" to "course-auto"]?.syncStatus)
    }

    @Test
    fun testTransientUnsaveFailureKeepsItemHiddenLocally() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        testDb.savedCourses["user-1" to "course-hidden"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-hidden",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "synced"
        )
        assertTrue(testDb.isCourseSaved("user-1", "course-hidden"))

        // Queue unsave offline
        testDb.queueUnsaveCourse("user-1", "course-hidden")

        // Must immediately be hidden locally
        assertFalse(testDb.isCourseSaved("user-1", "course-hidden"))
        assertFalse(testDb.isCourseOfflineReady("user-1", "course-hidden"))

        // Remote unsave fails with transient 500
        mockResponses["DELETE" to "/api/v1/courses/course-hidden/save"] = Pair(
            500,
            "{\"detail\":\"Internal server error HTTP 500\"}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val syncRes = syncManager.syncNow()
        assertTrue(syncRes.isFailure)

        // Still remains hidden locally while pending retry
        assertFalse(testDb.isCourseSaved("user-1", "course-hidden"))
        assertFalse(testDb.isCourseOfflineReady("user-1", "course-hidden"))
        assertEquals(1, testDb.getPendingUnsavedCourses("user-1").size)

        // Server recovers
        mockResponses["DELETE" to "/api/v1/courses/course-hidden/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-hidden\",\"isSaved\":false}"
        )

        val retryRes = syncManager.syncNow()
        assertTrue(retryRes.isSuccess)
        assertFalse(testDb.savedCourses.containsKey("user-1" to "course-hidden"))
    }

    @Test
    fun testQueuedUnsaveInterleavedWithNewSavePreservesNewSave() = runBlocking {
        sessionManager.onLoginSuccess("acc", "ref", "user-1", "u1@edunova.org", "User One")

        // 1. Course was unsaved offline
        testDb.savedCourses["user-1" to "course-toggle"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-toggle",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = false,
            syncStatus = "pending_unsave"
        )

        // 2. User re-saves the course before sync occurs
        testDb.savedCourses["user-1" to "course-toggle"] = LocalSavedCourse(
            userId = "user-1",
            courseId = "course-toggle",
            savedAt = System.currentTimeMillis(),
            downloadStatus = "completed",
            isOfflineReady = true,
            syncStatus = "pending_sync"
        )

        mockResponses["POST" to "/api/v1/courses/course-toggle/save"] = Pair(
            200,
            "{\"success\":true,\"courseId\":\"course-toggle\",\"isSaved\":true}"
        )

        val syncManager = EduNovaSyncManager(
            networkMonitor = fakeNetworkMonitor,
            sessionManager = sessionManager,
            cacheDb = testDb,
            scope = testScope
        )

        val syncRes = syncManager.syncNow()
        assertTrue(syncRes.isSuccess)

        // Must be saved and synced, never deleted
        val item = testDb.savedCourses["user-1" to "course-toggle"]
        assertNotNull(item)
        assertEquals("synced", item?.syncStatus)
        assertTrue(testDb.isCourseSaved("user-1", "course-toggle"))
    }
}
