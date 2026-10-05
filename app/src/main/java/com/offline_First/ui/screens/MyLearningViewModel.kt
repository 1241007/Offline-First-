package com.offline_First.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.LearningRepository
import com.offline_First.domain.model.LearningCourse
import com.offline_First.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyLearningUiState(
    val inProgressCourses: UiState<List<LearningCourse>> = UiState.Loading,
    val completedCourses: UiState<List<LearningCourse>> = UiState.Loading,
    val errorMessage: String? = null
)

class MyLearningViewModel(
    private val repository: LearningRepository = AppContainer.learningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
    }

    fun retry() {
        loadCourses()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = MyLearningUiState(
                inProgressCourses = UiState.Loading,
                completedCourses = UiState.Loading,
                errorMessage = null
            )
            val inProgress = repository.getInProgressCourses()
            val completed = repository.getCompletedCourses()
            val error = inProgress.exceptionOrNull() ?: completed.exceptionOrNull()
            _uiState.value = MyLearningUiState(
                inProgressCourses = inProgress.fold(
                    onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                    onFailure = { UiState.Error(it.localizedMessage ?: "Unable to load courses.") }
                ),
                completedCourses = completed.fold(
                    onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                    onFailure = { UiState.Error(it.localizedMessage ?: "Unable to load courses.") }
                ),
                errorMessage = error?.localizedMessage
            )
        }
    }
}
