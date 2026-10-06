package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val roadmaps: UiState<List<RoadmapOption>> = UiState.Loading,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isGenerating: Boolean = false,
    val generatedRoadmap: GeneratedRoadmapPreview? = null,
    val errorMessage: String? = null
)

class RoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                roadmaps = UiState.Loading,
                errorMessage = null
            )
            val categories = repository.getCategories()
            val categoryFilter = if (_uiState.value.selectedCategory == "All") null else _uiState.value.selectedCategory
            val result = repository.getRoadmaps(limit = 10, offset = 0, category = categoryFilter)
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        roadmaps = if (list.isEmpty()) UiState.Empty else UiState.Success(list),
                        categories = categories,
                        hasMore = list.size >= 10
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        roadmaps = UiState.Error(error.localizedMessage ?: "Failed to load roadmaps"),
                        categories = categories,
                        errorMessage = error.localizedMessage ?: "Failed to load roadmaps."
                    )
                }
            )
        }
    }

    fun loadMoreRoadmaps() {
        val current = _uiState.value
        val currentList = (current.roadmaps as? UiState.Success)?.data ?: return
        if (current.isLoadingMore || !current.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val categoryFilter = if (current.selectedCategory == "All") null else current.selectedCategory
            val result = repository.getRoadmaps(limit = 10, offset = currentList.size, category = categoryFilter)
            result.fold(
                onSuccess = { newItems ->
                    val combined = (currentList + newItems).distinctBy { it.id }
                    _uiState.value = _uiState.value.copy(
                        roadmaps = UiState.Success(combined),
                        hasMore = newItems.size >= 10,
                        isLoadingMore = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadData()
    }

    fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            val result = repository.generatePersonalizedRoadmap(goal, level, studyTime, interest)
            result.fold(
                onSuccess = { generated ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedRoadmap = generated
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = it.localizedMessage ?: "Unable to generate a roadmap."
                    )
                }
            )
        }
    }

    fun clearGeneratedRoadmap() {
        _uiState.value = _uiState.value.copy(generatedRoadmap = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
