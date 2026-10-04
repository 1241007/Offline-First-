package com.offline_First.ui.screens.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.local.LocalCourseRepository
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.data.repository.CourseRepository
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
    val greetingName: String = "",
    val continueLearning: ContinueLearningItem? = null,
    val subjects: List<Subject> = emptyList(),
    val upcomingExams: List<UpcomingExam> = emptyList(),
    val studyFocus: StudyFocusItem? = null,
    val featuredCourses: List<Course> = emptyList(),
    val exploreCourses: List<Course> = emptyList(),
    val errorMessage: String? = null
)

class LandingViewModel(
    private val profileRepository: ProfileRepository = LocalProfileRepository(),
    private val courseRepository: CourseRepository = LocalCourseRepository(),
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeProfileChanges()
    }

    private fun loadInitialData() {
        viewModelScope.launch(coroutineContext) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val courses = courseRepository.getCourses().getOrDefault(emptyList())
            val featured = courseRepository.getFeaturedCourses().getOrDefault(emptyList())
            val profile = profileRepository.getUserProfile().getOrNull()

            updateResolvedState(profile, courses, featured)
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch(coroutineContext) {
            profileRepository.observeUserProfile().collect { profile ->
                val courses = _uiState.value.exploreCourses.ifEmpty {
                    courseRepository.getCourses().getOrDefault(emptyList())
                }
                val featured = _uiState.value.featuredCourses.ifEmpty {
                    courseRepository.getFeaturedCourses().getOrDefault(emptyList())
                }
                updateResolvedState(profile, courses, featured)
            }
        }
    }

    private fun updateResolvedState(
        profile: UserProfile?,
        courses: List<Course>,
        featured: List<Course>
    ) {
        val mode = profile?.educationMode ?: EducationMode.GENERAL
        val name = profile?.fullName?.ifBlank { "Learner" } ?: "Learner"

        // Backend-ready school subject model list
        val schoolSubjects = listOf(
            Subject(
                id = "sub-math",
                name = "Mathematics",
                icon = "🧮",
                progress = 0.80f,
                totalTopics = 12,
                completedChapters = 8,
                totalChapters = 12
            ),
            Subject(
                id = "sub-phy",
                name = "Physics",
                icon = "⚛",
                progress = 0.65f,
                totalTopics = 10,
                completedChapters = 6,
                totalChapters = 10
            ),
            Subject(
                id = "sub-chem",
                name = "Chemistry",
                icon = "🧪",
                progress = 0.45f,
                totalTopics = 8,
                completedChapters = 4,
                totalChapters = 8
            ),
            Subject(
                id = "sub-bio",
                name = "Biology",
                icon = "🧬",
                progress = 0.90f,
                totalTopics = 14,
                completedChapters = 12,
                totalChapters = 14
            )
        )

        val continueItem = ContinueLearningItem(
            subjectName = "Mathematics",
            topicName = "Quadratic Equations",
            lessonInfo = "Lesson 8 of 12",
            progress = 0.80f,
            estimatedMinutes = 10,
            practiceQuestionsCount = 5
        )

        val upcomingExams = listOf(
            UpcomingExam(
                id = "exam-1",
                title = "Mathematics – Unit Test",
                daysRemaining = 3,
                className = "Class 10"
            )
        )

        val nextStep = StudyFocusItem(
            subjectName = "Mathematics",
            actionTitle = "Practice / Continue Quadratic Equations",
            lessonInfo = "Lesson 8 of 12"
        )

        _uiState.value = LandingUiState(
            isLoading = false,
            profile = profile,
            educationMode = mode,
            greetingName = name,
            continueLearning = continueItem,
            subjects = schoolSubjects,
            upcomingExams = upcomingExams,
            studyFocus = nextStep,
            featuredCourses = featured,
            exploreCourses = courses
        )
    }
}
