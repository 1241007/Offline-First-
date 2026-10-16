package com.offline_First.data.remote

import com.offline_First.data.AppContainer
import com.offline_First.data.local.CourseProgressStore
import com.offline_First.domain.model.RevisionItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
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

    @Test
    fun onlineCourseRepositoryFailsWhenConfiguredWithoutFallback() = runBlocking {
        val repo = OnlineCourseRepository(localFallback = null)

        // In unit test environment without fallback, unreachable remote fails
        val coursesResult = repo.getCourses()
        assertTrue("Expected failure when offline/unreachable and no fallback", coursesResult.isFailure)

        val featuredResult = repo.getFeaturedCourses()
        assertTrue("Expected failure when offline/unreachable and no fallback", featuredResult.isFailure)

        val categoryResult = repo.getCoursesByCategory("cpp")
        assertTrue("Expected failure when offline/unreachable and no fallback", categoryResult.isFailure)

        val detailResult = repo.getCourseDetail("c-cpp")
        assertTrue("Expected failure when offline/unreachable and no fallback", detailResult.isFailure)
    }

    @Test
    fun onlineCourseRepositoryGracefullyFallsBackToLocalCoursesWhenRemoteFails() = runBlocking {
        val repo = OnlineCourseRepository(localFallback = com.offline_First.data.local.LocalCourseRepository())

        val coursesResult = repo.getCourses()
        assertTrue(coursesResult.isSuccess)
        assertEquals(7, coursesResult.getOrThrow().size)

        val featuredResult = repo.getFeaturedCourses()
        assertTrue(featuredResult.isSuccess)
        assertTrue(featuredResult.getOrThrow().isNotEmpty())

        val detailResult = repo.getCourseDetail("c-cpp")
        assertTrue(detailResult.isSuccess)
        assertEquals("C++ Programming", detailResult.getOrThrow()?.name)
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
    fun onlineCourseRepositoryResolvesCppBySlugAndUuidViaFallback() = runBlocking {
        val repo = OnlineCourseRepository(localFallback = com.offline_First.data.local.LocalCourseRepository())
        
        val detailBySlug = repo.getCourseDetail("cpp-programming").getOrThrow()
        assertNotNull(detailBySlug)
        assertEquals("C++ Programming", detailBySlug?.name)

        val backendUuid = "d7a46974-51e0-47b8-b80c-07f9c8f61530"
        com.offline_First.data.local.CourseIdNormalizer.registerRemoteCourse(backendUuid, "cpp-programming", "C++ Programming")

        val detailByUuid = repo.getCourseDetail(backendUuid).getOrThrow()
        assertNotNull(detailByUuid)
        assertEquals("C++ Programming", detailByUuid?.name)
    }

    @Test
    fun onlineCourseRepositoryInvalidatesAndUpdatesCacheOnCompletion() = runBlocking {
        val tempFile = File.createTempFile("test_online_cache", ".json")
        try {
            val progressStore = CourseProgressStore(tempFile)
            val repo = OnlineCourseRepository(
                localFallback = com.offline_First.data.local.LocalCourseRepository(progressStore),
                progressStore = progressStore
            )

            val detail1 = repo.getCourseDetail("c-cpp").getOrThrow()
            assertNotNull(detail1)
            val lesson1 = detail1!!.modules.first().lessons.first()
            assertFalse(lesson1.isCompleted)

            repo.markLessonCompleted("c-cpp", lesson1.id, true)

            // Cache should immediately reflect completion
            val detail2 = repo.getCourseDetail("c-cpp").getOrThrow()
            assertNotNull(detail2)
            val updatedLesson1 = detail2!!.modules.first().lessons.first()
            assertTrue(updatedLesson1.isCompleted)
        } finally {
            tempFile.delete()
        }
    }
}
