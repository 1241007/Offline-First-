package com.offline_First.ui.screens.landing

import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.data.repository.CourseRepository
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
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

    private open class FakeTestCourseRepository(
        private val delegate: CourseRepository = LocalCourseRepository()
    ) : CourseRepository by delegate

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

    @Test
    fun landingViewModelDistinguishesHttp500ServerFailure() {
        val failingRepo = object : FakeTestCourseRepository() {
            override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> =
                Result.failure(IllegalStateException("HTTP 500: {\"code\":\"INTERNAL_SERVER_ERROR\"}"))

            override suspend fun getFeaturedCourses(): Result<List<Course>> =
                Result.failure(IllegalStateException("HTTP 500: {\"code\":\"INTERNAL_SERVER_ERROR\"}"))
        }

        val viewModel = LandingViewModel(
            profileRepository = TestProfileRepository(),
            courseRepository = failingRepo,
            learningRepository = BackendNotConfiguredRepositories(),
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        assertTrue(state.coursesError != null)
        assertTrue("Expected 500 message but got: ${state.coursesError}", state.coursesError?.contains("HTTP 500") == true)
    }

    @Test
    fun landingViewModelDistinguishesNetworkConnectivityFailure() {
        val offlineRepo = object : FakeTestCourseRepository() {
            override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> =
                Result.failure(java.net.UnknownHostException("Unable to resolve host api.edunova.app"))

            override suspend fun getFeaturedCourses(): Result<List<Course>> =
                Result.failure(java.net.ConnectException("Failed to connect to api.edunova.app"))
        }

        val viewModel = LandingViewModel(
            profileRepository = TestProfileRepository(),
            courseRepository = offlineRepo,
            learningRepository = BackendNotConfiguredRepositories(),
            coroutineContext = Dispatchers.Unconfined
        )

        val state = viewModel.uiState.value
        assertEquals("Courses require an internet connection.", state.coursesError)
    }

    @Test
    fun landingViewModelRetryCoursesSuccessfullyUpdatesState() {
        var shouldSucceed = false
        val testCourse = Course(
            id = "c1",
            name = "Kotlin Basics",
            description = "Learn Kotlin",
            icon = "code",
            accent = CourseAccent.PRIMARY,
            category = "Programming",
            level = "Beginner",
            durationHours = 10,
            progress = 0f,
            totalLessons = 5,
            completedLessons = 0,
            modulesCount = 1
        )

        val flakyRepo = object : FakeTestCourseRepository() {
            override suspend fun getCourses(limit: Int?, offset: Int): Result<List<Course>> {
                return if (shouldSucceed) Result.success(listOf(testCourse))
                else Result.failure(IllegalStateException("HTTP 500: Server Error"))
            }

            override suspend fun getFeaturedCourses(): Result<List<Course>> {
                return if (shouldSucceed) Result.success(listOf(testCourse))
                else Result.failure(IllegalStateException("HTTP 500: Server Error"))
            }
        }

        val viewModel = LandingViewModel(
            profileRepository = TestProfileRepository(),
            courseRepository = flakyRepo,
            learningRepository = BackendNotConfiguredRepositories(),
            coroutineContext = Dispatchers.Unconfined
        )

        assertEquals("EduNova course servers are temporarily unavailable (HTTP 500). Please try again.", viewModel.uiState.value.coursesError)
        assertTrue(viewModel.uiState.value.exploreCourses.isEmpty())

        // Now recover and retry
        shouldSucceed = true
        viewModel.retryCourses()

        assertEquals(null, viewModel.uiState.value.coursesError)
        assertEquals(1, viewModel.uiState.value.exploreCourses.size)
        assertEquals("Kotlin Basics", viewModel.uiState.value.exploreCourses[0].name)
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
