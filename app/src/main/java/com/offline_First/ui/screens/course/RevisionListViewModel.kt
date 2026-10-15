package com.offline_First.ui.screens.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.CourseRepository
import com.offline_First.domain.model.RevisionItem
import com.offline_First.domain.model.UiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RevisionListUiState(
    val state: UiState<List<RevisionItem>> = UiState.Loading,
    val errorMessage: String? = null
)

class RevisionListViewModel(
    private val repository: CourseRepository = AppContainer.courseRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionListUiState())
    val uiState: StateFlow<RevisionListUiState> = _uiState.asStateFlow()

    init {
        loadRevisionItems()
    }

    fun loadRevisionItems() {
        viewModelScope.launch(coroutineContext) {
            _uiState.value = RevisionListUiState(state = UiState.Loading)
            val result = repository.getRevisionItems()
            result.fold(
                onSuccess = { items ->
                    _uiState.value = RevisionListUiState(
                        state = if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                    )
                },
                onFailure = { err ->
                    _uiState.value = RevisionListUiState(
                        state = UiState.Error(err.localizedMessage ?: "Failed to load revision items."),
                        errorMessage = err.localizedMessage ?: "Failed to load revision items."
                    )
                }
            )
        }
    }

    fun removeRevisionItem(courseId: String, lessonId: String) {
        viewModelScope.launch(coroutineContext) {
            repository.removeRevisionItem(courseId, lessonId)
            // Refresh list
            val result = repository.getRevisionItems()
            result.fold(
                onSuccess = { items ->
                    _uiState.value = RevisionListUiState(
                        state = if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                    )
                },
                onFailure = {
                    // Retain current view with item filtered if getRevisionItems failed
                    val currentItems = (_uiState.value.state as? UiState.Success)?.data ?: emptyList()
                    val filtered = currentItems.filterNot { it.courseId == courseId && it.lessonId == lessonId }
                    _uiState.value = RevisionListUiState(
                        state = if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
                    )
                }
            )
        }
    }

    class Factory(
        private val repository: CourseRepository = AppContainer.courseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RevisionListViewModel(repository) as T
        }
    }
}
