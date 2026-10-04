package com.offline_First.data

import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.local.LocalLearningRepository
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.data.local.LocalRoadmapRepository
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositoryTest {

    @Before
    fun setUp() {
        LocalProfileRepository.resetToDefault()
    }

    @Test
    fun localRoadmapRepositoryReturnsCategoriesAndRoadmaps() = runBlocking {
        val repo = LocalRoadmapRepository()
        val categories = repo.getCategories()
        val roadmapsResult = repo.getRoadmaps()

        assertTrue(categories.contains("All"))
        assertTrue(categories.contains("Development"))
        assertTrue(roadmapsResult.isSuccess)

        val roadmaps = roadmapsResult.getOrThrow()
        assertEquals(9, roadmaps.size)
        assertTrue(roadmaps.any { it.title == "Frontend Developer" })
        assertTrue(roadmaps.any { it.title == "Android Developer" })
    }

    @Test
    fun localCourseRepositoryReturnsAllAndFeaturedCourses() = runBlocking {
        val repo = LocalCourseRepository()
        val allCoursesResult = repo.getCourses()
        val featuredCoursesResult = repo.getFeaturedCourses()

        assertTrue(allCoursesResult.isSuccess)
        assertTrue(featuredCoursesResult.isSuccess)

        val all = allCoursesResult.getOrThrow()
        val featured = featuredCoursesResult.getOrThrow()

        assertEquals(6, all.size)
        assertEquals(3, featured.size)
        assertEquals("Python", featured[0].name)
    }

    @Test
    fun localLearningRepositoryReturnsInProgressAndCompleted() = runBlocking {
        val repo = LocalLearningRepository()
        val inProgress = repo.getInProgressCourses().getOrThrow()
        val completed = repo.getCompletedCourses().getOrThrow()

        assertEquals(3, inProgress.size)
        assertEquals(2, completed.size)
        assertEquals("Kotlin Fundamentals", inProgress[0].name)
        assertEquals(1f, completed[0].progress, 0.001f)
    }

    @Test
    fun localProfileRepositoryUpdatesAndRetrievesProfile() = runBlocking {
        val repo = LocalProfileRepository()
        val initialProfile = repo.getUserProfile().getOrThrow()
        assertEquals("Asha Learner", initialProfile.fullName)

        val updated = UserProfile(
            fullName = "Rohan Sharma",
            email = "rohan@edunova.org",
            mobile = "+91 99999 88888",
            interests = "Kotlin, Cloud",
            level = "Advanced"
        )
        val updateResult = repo.updateUserProfile(updated)
        assertTrue(updateResult.isSuccess)

        val retrieved = repo.getUserProfile().getOrThrow()
        assertEquals("Rohan Sharma", retrieved.fullName)
        assertEquals("rohan@edunova.org", retrieved.email)
    }
}
