package com.offline_First.ui.screens.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.RevisionItem
import com.offline_First.domain.model.UiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LessonViewerData(
    val course: CourseDetail,
    val module: CourseModule,
    val lesson: LessonItem,
    val previousLesson: LessonItem?,
    val nextLesson: LessonItem?,
    val totalLessonsInCourse: Int,
    val currentLessonIndex: Int // 1-based index
)

data class CourseQuizState(
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val score: Int = 0,
    val isQuizFinished: Boolean = false
)

data class LessonViewerUiState(
    val state: UiState<LessonViewerData> = UiState.Loading,
    val isCompleted: Boolean = false,
    val isMarkedForRevision: Boolean = false,
    val quizState: CourseQuizState = CourseQuizState(),
    val errorMessage: String? = null
)

class LessonViewerViewModel(
    private val courseId: String,
    private val lessonId: String,
    private val repository: CourseRepository = AppContainer.courseRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonViewerUiState())
    val uiState: StateFlow<LessonViewerUiState> = _uiState.asStateFlow()

    init {
        loadLesson(lessonId)
    }

    fun loadLesson(targetLessonId: String) {
        viewModelScope.launch(coroutineContext) {
            _uiState.value = _uiState.value.copy(
                state = UiState.Loading,
                errorMessage = null
            )
            val courseResult = repository.getCourseDetail(courseId)
            courseResult.fold(
                onSuccess = { course ->
                    if (course == null) {
                        _uiState.value = LessonViewerUiState(
                            state = UiState.Empty,
                            errorMessage = "Course details not found."
                        )
                        return@launch
                    }

                    // Flatten ordered lessons across modules
                    val flattenedLessons = course.modules.sortedBy { it.displayOrder }.flatMap { mod ->
                        mod.lessons.sortedBy { it.displayOrder }.map { l -> mod to l }
                    }

                    val index = flattenedLessons.indexOfFirst { it.second.id == targetLessonId }
                    if (index == -1) {
                        _uiState.value = LessonViewerUiState(
                            state = UiState.Empty,
                            errorMessage = "Lesson '$targetLessonId' not found in course."
                        )
                        return@launch
                    }

                    val (currentModule, currentLesson) = flattenedLessons[index]
                    val prevLesson = if (index > 0) flattenedLessons[index - 1].second else null
                    val nextLesson = if (index < flattenedLessons.lastIndex) flattenedLessons[index + 1].second else null

                    val isCompleted = repository.isLessonCompleted(courseId, currentLesson.id)
                    val isMarked = repository.isMarkedForRevision(courseId, currentLesson.id)

                    _uiState.value = LessonViewerUiState(
                        state = UiState.Success(
                            LessonViewerData(
                                course = course,
                                module = currentModule,
                                lesson = currentLesson,
                                previousLesson = prevLesson,
                                nextLesson = nextLesson,
                                totalLessonsInCourse = flattenedLessons.size,
                                currentLessonIndex = index + 1
                            )
                        ),
                        isCompleted = isCompleted,
                        isMarkedForRevision = isMarked,
                        quizState = CourseQuizState()
                    )
                },
                onFailure = { err ->
                    _uiState.value = LessonViewerUiState(
                        state = UiState.Error(err.localizedMessage ?: "Failed to load lesson."),
                        errorMessage = err.localizedMessage ?: "Failed to load lesson."
                    )
                }
            )
        }
    }

    fun toggleLessonCompleted() {
        val current = (_uiState.value.state as? UiState.Success)?.data ?: return
        val newStatus = !_uiState.value.isCompleted
        _uiState.value = _uiState.value.copy(isCompleted = newStatus)
        viewModelScope.launch(coroutineContext) {
            repository.markLessonCompleted(courseId, current.lesson.id, newStatus)
        }
    }

    fun toggleMarkForRevision() {
        val current = (_uiState.value.state as? UiState.Success)?.data ?: return
        val newStatus = !_uiState.value.isMarkedForRevision
        _uiState.value = _uiState.value.copy(isMarkedForRevision = newStatus)
        viewModelScope.launch(coroutineContext) {
            if (newStatus) {
                val item = RevisionItem(
                    courseId = courseId,
                    courseName = current.course.name,
                    lessonId = current.lesson.id,
                    lessonTitle = current.lesson.title,
                    moduleTitle = current.module.title,
                    resourceUrl = current.lesson.resourceUrl,
                    resourceProvider = current.lesson.resourceProvider
                )
                repository.addRevisionItem(item)
            } else {
                repository.removeRevisionItem(courseId, current.lesson.id)
            }
        }
    }

    fun selectQuizOption(index: Int) {
        if (_uiState.value.quizState.isAnswerSubmitted) return
        _uiState.value = _uiState.value.copy(
            quizState = _uiState.value.quizState.copy(selectedOptionIndex = index)
        )
    }

    fun submitQuizAnswer() {
        val currentData = (_uiState.value.state as? UiState.Success)?.data ?: return
        val quiz = currentData.lesson.quiz ?: return
        val currentQuizState = _uiState.value.quizState
        if (currentQuizState.isAnswerSubmitted) return // Prevent double scoring
        val selected = currentQuizState.selectedOptionIndex ?: return
        val questions = quiz.questions
        if (currentQuizState.currentQuestionIndex !in questions.indices) return

        val currentQuestion = questions[currentQuizState.currentQuestionIndex]
        val isCorrect = selected == currentQuestion.correctIndex
        val newScore = if (isCorrect) currentQuizState.score + 1 else currentQuizState.score

        _uiState.value = _uiState.value.copy(
            quizState = currentQuizState.copy(
                isAnswerSubmitted = true,
                score = newScore
            )
        )
    }

    fun nextQuizQuestion() {
        val currentData = (_uiState.value.state as? UiState.Success)?.data ?: return
        val quiz = currentData.lesson.quiz ?: return
        val currentQuizState = _uiState.value.quizState
        val totalQuestions = quiz.questions.size

        if (currentQuizState.currentQuestionIndex < totalQuestions - 1) {
            _uiState.value = _uiState.value.copy(
                quizState = currentQuizState.copy(
                    currentQuestionIndex = currentQuizState.currentQuestionIndex + 1,
                    selectedOptionIndex = null,
                    isAnswerSubmitted = false
                )
            )
        } else {
            _uiState.value = _uiState.value.copy(
                quizState = currentQuizState.copy(
                    isQuizFinished = true
                )
            )
        }
    }

    fun retryQuiz() {
        _uiState.value = _uiState.value.copy(
            quizState = CourseQuizState()
        )
    }

    class Factory(
        private val courseId: String,
        private val lessonId: String,
        private val repository: CourseRepository = AppContainer.courseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LessonViewerViewModel(courseId, lessonId, repository) as T
        }
    }
}
