package com.offline_First.ui.screens.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.CourseRepository
import com.offline_First.data.repository.LearningRepository
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam
import com.offline_First.domain.model.UserProfile
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LandingUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val educationMode: EducationMode = EducationMode.GENERAL,
    val greetingName: String = "Learner",
    val continueLearning: ContinueLearningItem? = null,
    val subjects: List<Subject> = emptyList(),
    val upcomingExams: List<UpcomingExam> = emptyList(),
    val studyFocus: StudyFocusItem? = null,
    val featuredCourses: List<Course> = emptyList(),
    val exploreCourses: List<Course> = emptyList(),
    val isLoadingMoreCourses: Boolean = false,
    val hasMoreCourses: Boolean = true,
    val errorMessage: String? = null
)

class LandingViewModel(
    private val profileRepository: ProfileRepository = AppContainer.profileRepository,
    private val courseRepository: CourseRepository = AppContainer.courseRepository,
    private val learningRepository: LearningRepository = AppContainer.learningRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    private var currentProfile: UserProfile? = null
    private var loadedData = LandingUiState()

    init {
        observeProfileChanges()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(coroutineContext) {
            // Parallel execution: fetch all landing sections concurrently
            val coursesDeferred = async { courseRepository.getCourses(limit = 10, offset = 0) }
            val featuredDeferred = async { courseRepository.getFeaturedCourses() }
            val subjectsDeferred = async { learningRepository.getSubjects() }
            val continueDeferred = async { learningRepository.getContinueLearning() }
            val examsDeferred = async { learningRepository.getUpcomingExams() }
            val studyFocusDeferred = async { learningRepository.getStudyFocus() }

            val courses = coursesDeferred.await()
            val featured = featuredDeferred.await()
            val subjects = subjectsDeferred.await()
            val continueLearning = continueDeferred.await()
            val exams = examsDeferred.await()
            val studyFocus = studyFocusDeferred.await()

            val errors = listOf(
                courses.exceptionOrNull(),
                featured.exceptionOrNull(),
                subjects.exceptionOrNull(),
                continueLearning.exceptionOrNull(),
                exams.exceptionOrNull(),
                studyFocus.exceptionOrNull()
            )

            val courseList = courses.getOrNull().orEmpty()

            loadedData = LandingUiState(
                isLoading = false,
                continueLearning = continueLearning.getOrNull(),
                subjects = subjects.getOrNull().orEmpty(),
                upcomingExams = exams.getOrNull().orEmpty(),
                studyFocus = studyFocus.getOrNull(),
                featuredCourses = featured.getOrNull().orEmpty(),
                exploreCourses = courseList,
                hasMoreCourses = courseList.size >= 10,
                errorMessage = run {
                    val raw = errors.firstNotNullOfOrNull { it?.localizedMessage }
                    when {
                        raw == null -> if (errors.any { it != null }) "Unable to load learning data." else null
                        raw.contains("not connected") -> raw
                        errors.any { it is java.net.UnknownHostException || it is java.net.ConnectException || it is java.net.SocketTimeoutException } ->
                            "Unable to connect to server. Please check your internet connection."
                        raw.isNotBlank() -> raw
                        else -> "Unable to load learning data."
                    }
                }
            )
            publishState()
        }
    }

    fun loadMoreCourses() {
        val currentState = _uiState.value
        if (currentState.isLoadingMoreCourses || !currentState.hasMoreCourses) return

        viewModelScope.launch(coroutineContext) {
            _uiState.value = _uiState.value.copy(isLoadingMoreCourses = true)
            val currentCount = _uiState.value.exploreCourses.size
            val nextBatch = courseRepository.getCourses(limit = 10, offset = currentCount)
            nextBatch.fold(
                onSuccess = { newCourses ->
                    val combined = (_uiState.value.exploreCourses + newCourses).distinctBy { it.id }
                    loadedData = loadedData.copy(
                        exploreCourses = combined,
                        hasMoreCourses = newCourses.size >= 10,
                        isLoadingMoreCourses = false
                    )
                    publishState()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMoreCourses = false)
                }
            )
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch(coroutineContext) {
            profileRepository.observeUserProfile().collect { profile ->
                currentProfile = profile
                publishState()
            }
        }
    }

    private fun publishState() {
        val mode = currentProfile?.educationMode ?: EducationMode.GENERAL
        val greetingName = currentProfile?.fullName?.ifBlank { "Learner" } ?: "Learner"
        _uiState.value = loadedData.copy(
            profile = currentProfile,
            educationMode = mode,
            greetingName = greetingName
        )
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        loadInitialData()
    }

    fun clearError() {
        loadedData = loadedData.copy(errorMessage = null)
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
