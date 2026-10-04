package com.offline_First.data

import com.offline_First.data.repository.BackendNotConfiguredException
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {

    @Test
    fun dataRepositoriesFailClearlyUntilBackendIsConfigured() = runBlocking {
        val repositories = BackendNotConfiguredRepositories()

        assertBackendNotConfigured(repositories.getCourses().exceptionOrNull())
        assertBackendNotConfigured(repositories.getFeaturedCourses().exceptionOrNull())
        assertBackendNotConfigured(repositories.getInProgressCourses().exceptionOrNull())
        assertBackendNotConfigured(repositories.getCompletedCourses().exceptionOrNull())
        assertBackendNotConfigured(repositories.getSubjects().exceptionOrNull())
        assertBackendNotConfigured(repositories.getContinueLearning().exceptionOrNull())
        assertBackendNotConfigured(repositories.getUpcomingExams().exceptionOrNull())
        assertBackendNotConfigured(repositories.getStudyFocus().exceptionOrNull())
        assertBackendNotConfigured(repositories.getRoadmaps().exceptionOrNull())
        assertTrue(repositories.getCategories().isEmpty())
        assertBackendNotConfigured(
            repositories.generatePersonalizedRoadmap("goal", "level", "time", "interest").exceptionOrNull()
        )
    }

    @Test
    fun profileRepositoryStartsWithoutFakeUserAndDoesNotClaimSaveSucceeded() = runBlocking {
        val repositories = BackendNotConfiguredRepositories()
        val profile = UserProfile(
            fullName = "Learner",
            email = "learner@example.com",
            mobile = "",
            interests = "",
            level = "",
            educationMode = EducationMode.GENERAL
        )

        assertNull(repositories.getUserProfile().getOrThrow())
        assertNull(repositories.observeUserProfile().first())
        assertBackendNotConfigured(repositories.updateUserProfile(profile).exceptionOrNull())
    }

    @Test
    fun authenticationRequiresBackendImplementation() = runBlocking {
        val repositories = BackendNotConfiguredRepositories()

        assertBackendNotConfigured(repositories.signIn("learner@example.com", "password").exceptionOrNull())
        assertBackendNotConfigured(
            repositories.register(
                com.offline_First.data.repository.RegistrationInput(
                    fullName = "Learner",
                    email = "learner@example.com",
                    mobile = "",
                    password = "password"
                )
            ).exceptionOrNull()
        )
        assertBackendNotConfigured(
            repositories.requestPasswordReset("learner@example.com").exceptionOrNull()
        )
    }

    private fun assertBackendNotConfigured(error: Throwable?) {
        assertTrue(error is BackendNotConfiguredException)
        assertFalse(error?.localizedMessage.isNullOrBlank())
    }
}
