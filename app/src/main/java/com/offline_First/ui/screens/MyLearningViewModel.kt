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
    val selectedTab: Int = 0, // 0 = In Progress, 1 = Completed
    val inProgressCourses: UiState<List<LearningCourse>> = UiState.Loading,
    val completedCourses: UiState<List<LearningCourse>> = UiState.Empty,
    val completedLoaded: Boolean = false,
    val errorMessage: String? = null
)

class MyLearningViewModel(
    private val repository: LearningRepository = AppContainer.learningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init {
        loadInProgressCourses()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        if (tabIndex == 1 && !_uiState.value.completedLoaded) {
            loadCompletedCourses()
        }
    }

    fun retry() {
        if (_uiState.value.selectedTab == 0) {
            loadInProgressCourses()
        } else {
            loadCompletedCourses()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun loadInProgressCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                inProgressCourses = UiState.Loading,
                errorMessage = null
            )
            val result = repository.getInProgressCourses()
            val errorMsg = result.exceptionOrNull()?.let { err ->
                when (err) {
                    is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
                        "Unable to connect to server. Please check your internet connection."
                    else -> err.localizedMessage?.takeIf { !it.contains("Exception") } ?: "Unable to load courses."
                }
            }
            _uiState.value = _uiState.value.copy(
                inProgressCourses = result.fold(
                    onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                    onFailure = { UiState.Error(errorMsg ?: "Unable to load courses.") }
                ),
                errorMessage = errorMsg
            )
        }
    }

    private fun loadCompletedCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                completedCourses = UiState.Loading,
                errorMessage = null
            )
            val result = repository.getCompletedCourses()
            val errorMsg = result.exceptionOrNull()?.let { err ->
                when (err) {
                    is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
                        "Unable to connect to server. Please check your internet connection."
                    else -> err.localizedMessage?.takeIf { !it.contains("Exception") } ?: "Unable to load courses."
                }
            }
            _uiState.value = _uiState.value.copy(
                completedCourses = result.fold(
                    onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                    onFailure = { UiState.Error(errorMsg ?: "Unable to load courses.") }
                ),
                completedLoaded = true,
                errorMessage = errorMsg
            )
        }
    }
}
