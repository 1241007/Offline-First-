package com.offline_First.data.remote

import com.offline_First.data.AppContainer
import com.offline_First.data.local.CourseProgressStore
import com.offline_First.domain.model.RevisionItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OnlineCourseRepositoryTest {

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
}
