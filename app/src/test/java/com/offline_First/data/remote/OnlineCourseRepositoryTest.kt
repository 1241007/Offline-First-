package com.offline_First.data.remote

import com.offline_First.data.AppContainer
import com.offline_First.data.local.CourseProgressStore
import com.offline_First.data.local.InMemoryChatCacheDatabase
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.RevisionItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OnlineCourseRepositoryTest {

    @Before
    fun setUp() {
        ChatApiClient.customBaseUrl = "http://127.0.0.1:59999"
    }

    @After
    fun tearDown() {
        ChatApiClient.customBaseUrl = null
    }

    private fun createSampleCourseDetail(
        courseId: String,
        name: String,
        lessonCount: Int = 2
    ): CourseDetail {
        val moduleId = "mod-$courseId-1"
        val lessons = (1..lessonCount).map { i ->
            LessonItem(
                id = "les-$courseId-$i",
                moduleId = moduleId,
                courseId = courseId,
                title = "Lesson $i for $name",
                contentType = "article",
                durationMinutes = 10,
                displayOrder = i,
                description = "Description for lesson $i",
                resourceUrl = "https://example.com/res$i",
                resourceProvider = "Documentation"
            )
        }
        val module = CourseModule(
            id = moduleId,
            courseId = courseId,
            title = "Module 1",
            displayOrder = 1,
            lessons = lessons
        )
        return CourseDetail(
            id = courseId,
            name = name,
            description = "Test description for $name",
            icon = "code",
            accent = CourseAccent.PRIMARY,
            category = "programming",
            level = "Beginner",
            durationHours = 2,
            modules = listOf(module)
        )
    }

    @Test
    fun offlineCatalogReturnsOnlySavedOfflineReadyCoursesForCurrentUser() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        var currentOnline = false
        var activeUser = "userA"

        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { currentOnline },
            userIdProvider = { activeUser }
        )

        val courseA1 = createSampleCourseDetail("course-a1", "User A Completed Course", 2)
        val courseA2 = createSampleCourseDetail("course-a2", "User A Incomplete Course", 2)
        val courseB1 = createSampleCourseDetail("course-b1", "User B Completed Course", 2)

        // User A - course 1: completed & offline ready
        testDb.saveCourseForOffline("userA", courseA1, downloadStatus = "completed", syncStatus = "synced")

        // User A - course 2: still downloading, not offline ready
        testDb.saveCourseForOffline("userA", courseA2, downloadStatus = "downloading", syncStatus = "pending_sync")

        // User A - course 3: failed download
        testDb.recordFailedDownload("userA", "course-a3", "synced")

        // User B - course 1: completed
        testDb.saveCourseForOffline("userB", courseB1, downloadStatus = "completed", syncStatus = "synced")

        // When offline, getCourses must strictly return ONLY User A's completed, offline-ready course
        val result = repo.getCourses().getOrThrow()
        assertEquals(1, result.size)
        assertEquals("course-a1", result[0].id)
        assertEquals("User A Completed Course", result[0].name)

        // Featured offline should also only return User A's completed course
        val featured = repo.getFeaturedCourses().getOrThrow()
        assertEquals(1, featured.size)
        assertEquals("course-a1", featured[0].id)
    }

    @Test
    fun offlineUnsavedCourseCannotBeOpened() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { false },
            userIdProvider = { "userA" }
        )

        val unsavedResult = repo.getCourseDetail("unsaved-course-123")
        assertTrue("Opening unsaved course offline must fail", unsavedResult.isFailure)
        val errorMsg = unsavedResult.exceptionOrNull()?.message ?: ""
        assertEquals("This course isn't available offline. Connect to the internet and save it first.", errorMsg)
    }

    @Test
    fun offlineSavedCourseOpensWithCachedLessons() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { false },
            userIdProvider = { "userA" }
        )

        val courseA1 = createSampleCourseDetail("course-a1", "Saved Android Course", 3)
        testDb.saveCourseForOffline("userA", courseA1, downloadStatus = "completed", syncStatus = "synced")

        val result = repo.getCourseDetail("course-a1")
        assertTrue("Opening saved offline-ready course must succeed", result.isSuccess)
        val detail = result.getOrThrow()
        assertNotNull(detail)
        assertEquals("course-a1", detail?.id)
        assertEquals("Saved Android Course", detail?.name)
        assertEquals(1, detail?.modules?.size)
        assertEquals(3, detail?.modules?.first()?.lessons?.size)
    }

    @Test
    fun offlineEmptyStateReturnsEmptyList() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { false },
            userIdProvider = { "newUserWithoutCourses" }
        )

        val result = repo.getCourses().getOrThrow()
        assertTrue("Offline catalog with no saved courses must be empty", result.isEmpty())

        val featured = repo.getFeaturedCourses().getOrThrow()
        assertTrue("Offline featured courses with no saved courses must be empty", featured.isEmpty())
    }

    @Test
    fun userIsolationUserACannotSeeUserBCourses() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        var currentUser = "userA"
        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { false },
            userIdProvider = { currentUser }
        )

        val courseA = createSampleCourseDetail("course-a", "Course A", 1)
        val courseB = createSampleCourseDetail("course-b", "Course B", 1)

        testDb.saveCourseForOffline("userA", courseA, downloadStatus = "completed", syncStatus = "synced")
        testDb.saveCourseForOffline("userB", courseB, downloadStatus = "completed", syncStatus = "synced")

        // User A context
        currentUser = "userA"
        val userACourses = repo.getCourses().getOrThrow()
        assertEquals(1, userACourses.size)
        assertEquals("course-a", userACourses[0].id)

        // User A cannot open User B's course
        val aOpeningB = repo.getCourseDetail("course-b")
        assertTrue("User A opening User B course must fail offline", aOpeningB.isFailure)

        // Switch to User B context
        currentUser = "userB"
        val userBCourses = repo.getCourses().getOrThrow()
        assertEquals(1, userBCourses.size)
        assertEquals("course-b", userBCourses[0].id)

        // User B cannot open User A's course
        val bOpeningA = repo.getCourseDetail("course-a")
        assertTrue("User B opening User A course must fail offline", bOpeningA.isFailure)
    }

    @Test
    fun progressStoreIntegratesWithOnlineCourseRepository() = runBlocking {
        val tempFile = File.createTempFile("test_online_progress", ".json")
        try {
            val progressStore = CourseProgressStore(tempFile)
            val repo = OnlineCourseRepository(progressStore = progressStore)

            assertFalse(repo.isLessonCompleted("c-cpp", "les-cpp-101"))
            assertFalse(repo.isMarkedForRevision("c-cpp", "les-cpp-101"))

            // Mark completed
            repo.markLessonCompleted("c-cpp", "les-cpp-101", true)
            assertTrue(repo.isLessonCompleted("c-cpp", "les-cpp-101"))

            // Add revision item
            val revisionItem = RevisionItem(
                courseId = "c-cpp",
                courseName = "C++ Programming",
                lessonId = "les-cpp-101",
                lessonTitle = "Basic Concepts & Types",
                moduleTitle = "C++ Core Fundamentals",
                resourceUrl = "https://en.cppreference.com/w/cpp/language/basic_concepts",
                resourceProvider = "cppreference.com"
            )
            repo.addRevisionItem(revisionItem)
            assertTrue(repo.isMarkedForRevision("c-cpp", "les-cpp-101"))

            val revisionItems = repo.getRevisionItems().getOrThrow()
            assertEquals(1, revisionItems.size)
            assertEquals("les-cpp-101", revisionItems[0].lessonId)

            // Remove revision item
            repo.removeRevisionItem("c-cpp", "les-cpp-101")
            assertFalse(repo.isMarkedForRevision("c-cpp", "les-cpp-101"))
            assertEquals(0, repo.getRevisionItems().getOrThrow().size)

            // Unmark completion
            repo.markLessonCompleted("c-cpp", "les-cpp-101", false)
            assertFalse(repo.isLessonCompleted("c-cpp", "les-cpp-101"))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun appContainerProvidesOnlineCourseRepository() {
        assertTrue(
            "AppContainer.courseRepository must be OnlineCourseRepository for online-only catalog",
            AppContainer.courseRepository is OnlineCourseRepository
        )
    }

    @Test
    fun onlineCourseRepositorySavesAndUnsavesCourseWithFallbackDetail() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        val repo = OnlineCourseRepository(
            localFallback = com.offline_First.data.local.LocalCourseRepository(),
            cacheDb = testDb,
            userIdProvider = { "user-test" }
        )

        assertFalse(repo.isCourseSaved("user-test", "c-cpp"))
        assertFalse(repo.isCourseOfflineReady("user-test", "c-cpp"))

        // Save course: fetches detail via fallback and persists to cacheDb
        val saveResult = repo.saveCourse("c-cpp")
        assertTrue(saveResult.isSuccess)
        assertTrue(repo.isCourseSaved("user-test", "c-cpp"))
        assertTrue(repo.isCourseOfflineReady("user-test", "c-cpp"))

        // Unsave course
        val unsaveResult = repo.unsaveCourse("c-cpp")
        assertTrue(unsaveResult.isSuccess)
        assertFalse(repo.isCourseSaved("user-test", "c-cpp"))
    }

    @Test
    fun offlineSavedCourseOpensBySlugAndAlias() = runBlocking {
        val testDb = InMemoryChatCacheDatabase()
        var isOnline = false
        val repo = OnlineCourseRepository(
            cacheDb = testDb,
            isOnlineProvider = { isOnline },
            userIdProvider = { "user-offline-1" }
        )

        // Simulate backend UUID for JavaScript Essentials
        val remoteCourseUuid = "uuid-js-essentials-456"
        val jsCourseDetail = createSampleCourseDetail(
            courseId = remoteCourseUuid,
            name = "JavaScript Essentials",
            lessonCount = 15
        )

        // Save course offline under remoteCourseUuid
        testDb.saveCourseForOffline("user-offline-1", jsCourseDetail, downloadStatus = "completed", syncStatus = "synced")
        com.offline_First.data.local.CourseIdNormalizer.registerRemoteCourse(
            remoteCourseUuid,
            "javascript-essentials",
            "JavaScript Essentials"
        )

        // 1. Verify getSavedCourses includes the course
        val savedList = repo.getSavedCourses("user-offline-1").getOrThrow()
        assertEquals(1, savedList.size)
        assertEquals(remoteCourseUuid, savedList[0].id)
        assertEquals("JavaScript Essentials", savedList[0].name)
        assertEquals(15, savedList[0].totalLessons)

        // 2. Verify isCourseSaved and isCourseOfflineReady work with UUID
        assertTrue(repo.isCourseSaved(remoteCourseUuid))
        assertTrue(repo.isCourseOfflineReady(remoteCourseUuid))

        // 3. Verify isCourseSaved and isCourseOfflineReady work with slug
        assertTrue(repo.isCourseSaved("javascript-essentials"))
        assertTrue(repo.isCourseOfflineReady("javascript-essentials"))

        // 4. Verify isCourseSaved and isCourseOfflineReady work with c-js alias
        assertTrue(repo.isCourseSaved("c-js"))
        assertTrue(repo.isCourseOfflineReady("c-js"))

        // 5. Verify getCourseDetail returns full details offline when called with UUID
        val detailByUuid = repo.getCourseDetail(remoteCourseUuid).getOrThrow()
        assertNotNull(detailByUuid)
        assertEquals("JavaScript Essentials", detailByUuid?.name)
        assertEquals(15, detailByUuid?.totalLessonsCount)

        // 6. Verify getCourseDetail returns full details offline when called with slug!
        val detailBySlug = repo.getCourseDetail("javascript-essentials").getOrThrow()
        assertNotNull(detailBySlug)
        assertEquals("JavaScript Essentials", detailBySlug?.name)
        assertEquals(15, detailBySlug?.totalLessonsCount)

        // 7. Verify getCourseDetail returns full details offline when called with c-js!
        val detailByAlias = repo.getCourseDetail("c-js").getOrThrow()
        assertNotNull(detailByAlias)
        assertEquals("JavaScript Essentials", detailByAlias?.name)
        assertEquals(15, detailByAlias?.totalLessonsCount)
    }
}
