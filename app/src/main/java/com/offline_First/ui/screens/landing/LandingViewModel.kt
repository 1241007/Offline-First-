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
            val courses = courseRepository.getCourses()
            val featured = courseRepository.getFeaturedCourses()
            val subjects = learningRepository.getSubjects()
            val continueLearning = learningRepository.getContinueLearning()
            val exams = learningRepository.getUpcomingExams()
            val studyFocus = learningRepository.getStudyFocus()
            val errors = listOf(
                courses.exceptionOrNull(),
                featured.exceptionOrNull(),
                subjects.exceptionOrNull(),
                continueLearning.exceptionOrNull(),
                exams.exceptionOrNull(),
                studyFocus.exceptionOrNull()
            )
            loadedData = LandingUiState(
                isLoading = false,
                continueLearning = continueLearning.getOrNull(),
                subjects = subjects.getOrNull().orEmpty(),
                upcomingExams = exams.getOrNull().orEmpty(),
                studyFocus = studyFocus.getOrNull(),
                featuredCourses = featured.getOrNull().orEmpty(),
                exploreCourses = courses.getOrNull().orEmpty(),
                errorMessage = errors.firstNotNullOfOrNull { it?.localizedMessage }
                    ?: if (errors.any { it != null }) "Unable to load learning data." else null
            )
            publishState()
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
