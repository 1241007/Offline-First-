package com.offline_First.ui.screens.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.CourseRepository
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.UiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val courseDetailState: UiState<CourseDetail> = UiState.Loading,
    val completedLessonsCount: Int = 0,
    val totalLessonsCount: Int = 0,
    val progressPercentage: Float = 0f,
    val isCourseCompleted: Boolean = false,
    val certificate: CourseCertificate? = null,
    val learnerName: String = "Learner",
    val errorMessage: String? = null
)

class CourseDetailViewModel(
    private val courseId: String,
    private val repository: CourseRepository = AppContainer.courseRepository,
    private val profileRepository: ProfileRepository = AppContainer.profileRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        loadCourseDetail()
        observeProgressUpdates()
    }

    private fun observeProgressUpdates() {
        viewModelScope.launch(coroutineContext) {
            repository.observeProgressUpdates().collect { version ->
                if (version > 0L) {
                    val currentDetail = (_uiState.value.courseDetailState as? UiState.Success)?.data
                    if (currentDetail != null) {
                        refreshProgressOnly(currentDetail)
                    }
                }
            }
        }
    }

    private suspend fun refreshProgressOnly(detail: CourseDetail) {
        val allLessons = detail.modules.flatMap { it.lessons }
        val allLessonIds = allLessons.map { it.id }
        val totalCount = allLessonIds.size
        val completedCount = repository.getCompletedLessonCount(courseId, allLessonIds)
        val progress = repository.getCourseProgressPercentage(courseId, allLessonIds)
        val isCompleted = repository.isCourseCompleted(courseId, allLessonIds)

        var cert = repository.getCertificate(courseId)
        if (cert == null && isCompleted && totalCount > 0) {
            val learnerName = _uiState.value.learnerName
            cert = repository.issueCertificate(courseId, detail.name, learnerName, totalCount)
        }

        val updatedModules = detail.modules.map { m ->
            m.copy(
                lessons = m.lessons.map { l ->
                    l.copy(isCompleted = repository.isLessonCompleted(courseId, l.id))
                }
            )
        }

        _uiState.value = _uiState.value.copy(
            courseDetailState = UiState.Success(detail.copy(modules = updatedModules)),
            completedLessonsCount = completedCount,
            totalLessonsCount = totalCount,
            progressPercentage = progress,
            isCourseCompleted = isCompleted,
            certificate = cert
        )
    }

    fun loadCourseDetail() {
        viewModelScope.launch(coroutineContext) {
            _uiState.value = _uiState.value.copy(
                courseDetailState = UiState.Loading,
                errorMessage = null
            )
            val result = repository.getCourseDetail(courseId)
            result.fold(
                onSuccess = { detail ->
                    if (detail != null) {
                        val allLessons = detail.modules.flatMap { it.lessons }
                        val allLessonIds = allLessons.map { it.id }
                        val totalCount = allLessonIds.size
                        val completedCount = repository.getCompletedLessonCount(courseId, allLessonIds)
                        val progress = repository.getCourseProgressPercentage(courseId, allLessonIds)
                        val isCompleted = repository.isCourseCompleted(courseId, allLessonIds)

                        val profile = profileRepository.getUserProfile().getOrNull()
                        val learnerName = profile?.fullName?.ifBlank { "Learner" } ?: "Learner"

                        // Issue certificate if completed and not already issued
                        var cert = repository.getCertificate(courseId)
                        if (cert == null && isCompleted && totalCount > 0) {
                            cert = repository.issueCertificate(courseId, detail.name, learnerName, totalCount)
                        }

                        // Update lesson completion states inside the CourseDetail object
                        val updatedModules = detail.modules.map { m ->
                            m.copy(
                                lessons = m.lessons.map { l ->
                                    l.copy(isCompleted = repository.isLessonCompleted(courseId, l.id))
                                }
                            )
                        }

                        _uiState.value = CourseDetailUiState(
                            courseDetailState = UiState.Success(detail.copy(modules = updatedModules)),
                            completedLessonsCount = completedCount,
                            totalLessonsCount = totalCount,
                            progressPercentage = progress,
                            isCourseCompleted = isCompleted,
                            certificate = cert,
                            learnerName = learnerName
                        )
                    } else {
                        _uiState.value = CourseDetailUiState(
                            courseDetailState = UiState.Empty,
                            errorMessage = "Course details not found."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = CourseDetailUiState(
                        courseDetailState = UiState.Error(error.localizedMessage ?: "Failed to load course details."),
                        errorMessage = error.localizedMessage ?: "Failed to load course details."
                    )
                }
            )
        }
    }

    fun toggleLessonCompleted(lessonId: String) {
        val currentDetail = (_uiState.value.courseDetailState as? UiState.Success)?.data ?: return
        viewModelScope.launch(coroutineContext) {
            val wasCompleted = repository.isLessonCompleted(courseId, lessonId)
            val newCompleted = !wasCompleted
            repository.markLessonCompleted(courseId, lessonId, newCompleted)
            refreshProgressOnly(currentDetail)
        }
    }

    class Factory(
        private val courseId: String,
        private val repository: CourseRepository = AppContainer.courseRepository,
        private val profileRepository: ProfileRepository = AppContainer.profileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseDetailViewModel(courseId, repository, profileRepository) as T
        }
    }
}
