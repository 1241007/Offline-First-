package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.local.LocalRoadmapRepository
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
    val isGenerating: Boolean = false,
    val generatedRoadmap: GeneratedRoadmapPreview? = null
)

class RoadmapViewModel(
    private val repository: RoadmapRepository = LocalRoadmapRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(roadmaps = UiState.Loading)
            val categories = repository.getCategories()
            val result = repository.getRoadmaps()
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        roadmaps = if (list.isEmpty()) UiState.Empty else UiState.Success(list),
                        categories = categories
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        roadmaps = UiState.Error(error.localizedMessage ?: "Failed to load roadmaps"),
                        categories = categories
                    )
                }
            )
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun generatePersonalizedRoadmap(
        goal: String,
        level: String,
        studyTime: String,
        interest: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val result = repository.generatePersonalizedRoadmap(goal, level, studyTime, interest)
            result.fold(
                onSuccess = { generated ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedRoadmap = generated
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                }
            )
        }
    }

    fun clearGeneratedRoadmap() {
        _uiState.value = _uiState.value.copy(generatedRoadmap = null)
    }
}
