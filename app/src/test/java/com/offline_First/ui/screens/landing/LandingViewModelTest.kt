package com.offline_First.ui.screens.landing

import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingViewModelTest {

    @Test
    fun landingViewModelShowsEmptyDataAndReportsMissingBackend() {
        val viewModel = LandingViewModel(
            profileRepository = TestProfileRepository(),
            courseRepository = BackendNotConfiguredRepositories(),
            learningRepository = BackendNotConfiguredRepositories(),
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(EducationMode.GENERAL, state.educationMode)
        assertEquals("Learner", state.greetingName)
        assertTrue(state.featuredCourses.isEmpty())
        assertTrue(state.exploreCourses.isEmpty())
        assertTrue(state.subjects.isEmpty())
        assertTrue(state.errorMessage?.contains("not connected") == true)
    }

    @Test
    fun landingViewModelObservesProfileModeChanges() = runBlocking {
        val profileRepository = TestProfileRepository()
        val viewModel = LandingViewModel(
            profileRepository = profileRepository,
            courseRepository = BackendNotConfiguredRepositories(),
            learningRepository = BackendNotConfiguredRepositories(),
            coroutineContext = Dispatchers.Unconfined
        )

        assertEquals(EducationMode.GENERAL, viewModel.uiState.value.educationMode)
        profileRepository.updateUserProfile(
            UserProfile(
                fullName = "Rohan",
                email = "rohan@example.com",
                mobile = "",
                interests = "",
                level = "Class 11",
                educationMode = EducationMode.SCHOOL
            )
        )

        assertEquals(EducationMode.SCHOOL, viewModel.uiState.value.educationMode)
        assertEquals("Rohan", viewModel.uiState.value.greetingName)
    }

    private class TestProfileRepository : ProfileRepository {
        private val profile = MutableStateFlow<UserProfile?>(null)

        override suspend fun getUserProfile(): Result<UserProfile?> = Result.success(profile.value)

        override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
            this.profile.value = profile
            return Result.success(Unit)
        }

        override fun observeUserProfile(): Flow<UserProfile?> = profile.asStateFlow()
    }
}
