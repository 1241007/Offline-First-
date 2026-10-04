package com.offline_First.ui.screens.landing

import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LandingViewModelTest {

    @Before
    fun setUp() {
        LocalProfileRepository.resetToDefault()
    }

    @After
    fun tearDown() {
        LocalProfileRepository.resetToDefault()
    }

    @Test
    fun landingViewModel_resolvesGeneralModeFromProfile() = runBlocking {
        val profileRepo = LocalProfileRepository()
        profileRepo.updateUserProfile(
            UserProfile(
                fullName = "Alex College",
                email = "alex@example.com",
                mobile = "+91 91234 56789",
                interests = "Data Science",
                level = "Advanced",
                educationMode = EducationMode.GENERAL
            )
        )

        val viewModel = LandingViewModel(
            profileRepository = profileRepo,
            courseRepository = LocalCourseRepository()
        )

        // Wait briefly for coroutines to resolve state
        delay(100)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(EducationMode.GENERAL, state.educationMode)
        assertEquals("Alex College", state.greetingName)
        assertTrue(state.featuredCourses.isNotEmpty())
        assertTrue(state.exploreCourses.isNotEmpty())
    }

    @Test
    fun landingViewModel_resolvesSchoolModeWithSubjectsAndContinueLearning() = runBlocking {
        val profileRepo = LocalProfileRepository()
        profileRepo.updateUserProfile(
            UserProfile(
                fullName = "Asha Student",
                email = "asha.school@example.com",
                mobile = "+91 98765 43210",
                interests = "Science & Math",
                level = "Class 10",
                educationMode = EducationMode.SCHOOL
            )
        )

        val viewModel = LandingViewModel(
            profileRepository = profileRepo,
            courseRepository = LocalCourseRepository()
        )

        delay(100)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(EducationMode.SCHOOL, state.educationMode)
        assertEquals("Asha Student", state.greetingName)

        // Check Continue Learning Hero Card
        assertNotNull(state.continueLearning)
        assertEquals("Mathematics", state.continueLearning?.subjectName)
        assertEquals("Quadratic Equations", state.continueLearning?.topicName)
        assertEquals(0.80f, state.continueLearning?.progress ?: 0f, 0.001f)

        // Check Academic Subjects (not programming languages)
        assertTrue(state.subjects.isNotEmpty())
        val subjectNames = state.subjects.map { it.name }
        assertTrue(subjectNames.contains("Mathematics"))
        assertTrue(subjectNames.contains("Physics"))
        assertTrue(subjectNames.contains("Chemistry"))
        assertTrue(subjectNames.contains("Biology"))
        assertFalse(subjectNames.contains("Python"))
        assertFalse(subjectNames.contains("Java"))

        // Check Study Focus / Next Step
        assertNotNull(state.studyFocus)
        assertEquals("Mathematics", state.studyFocus?.subjectName)
    }

    @Test
    fun landingViewModel_switchesDynamicallyWhenProfileModeChanges() = runBlocking {
        val profileRepo = LocalProfileRepository()
        profileRepo.updateUserProfile(
            UserProfile(
                fullName = "Rohan",
                email = "rohan@example.com",
                mobile = "+91 99999 00000",
                interests = "Math",
                level = "Class 11",
                educationMode = EducationMode.SCHOOL
            )
        )

        val viewModel = LandingViewModel(
            profileRepository = profileRepo,
            courseRepository = LocalCourseRepository()
        )

        delay(100)
        assertEquals(EducationMode.SCHOOL, viewModel.uiState.value.educationMode)

        // Now user edits profile in ProfileScreen to GENERAL mode
        profileRepo.updateUserProfile(
            UserProfile(
                fullName = "Rohan",
                email = "rohan@example.com",
                mobile = "+91 99999 00000",
                interests = "Math",
                level = "College",
                educationMode = EducationMode.GENERAL
            )
        )

        delay(100)
        // LandingViewModel must have automatically updated to GENERAL
        assertEquals(EducationMode.GENERAL, viewModel.uiState.value.educationMode)
    }
}
