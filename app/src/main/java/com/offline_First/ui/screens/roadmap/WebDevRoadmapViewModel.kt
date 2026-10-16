package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.local.WebDevRoadmapDataProvider
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebDevRoadmapUiState(
    val nodes: List<WebDevNode> = WebDevRoadmapDataProvider.getNodes(),
    val completedNodeIds: Set<String> = emptySet(),
    val inProgressNodeIds: Set<String> = emptySet(),
    val selectedNode: WebDevNode? = null,
    val celebrationMessage: String? = null,
    val isLoading: Boolean = false
) {
    val totalNodes: Int = nodes.size
    val completedCount: Int = completedNodeIds.size
    val inProgressCount: Int = inProgressNodeIds.size

    val progressPercentage: Int = if (totalNodes > 0) {
        ((completedCount.toFloat() / totalNodes.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    fun isCompleted(nodeId: String): Boolean = completedNodeIds.contains(nodeId)
    fun isInProgress(nodeId: String): Boolean = inProgressNodeIds.contains(nodeId)
}

class WebDevRoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val roadmapId = "web_dev_frontend_roadmap"
    private val _uiState = MutableStateFlow(WebDevRoadmapUiState())
    val uiState: StateFlow<WebDevRoadmapUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rawKeys = repository.getCompletedMilestones(roadmapId)

            val completed = mutableSetOf<String>()
            val inProgress = mutableSetOf<String>()

            for (key in rawKeys) {
                when {
                    key.startsWith("node_done_") -> completed.add(key.removePrefix("node_done_"))
                    key.startsWith("node_prog_") -> inProgress.add(key.removePrefix("node_prog_"))
                }
            }

            _uiState.update {
                it.copy(
                    completedNodeIds = completed,
                    inProgressNodeIds = inProgress,
                    isLoading = false
                )
            }
        }
    }

    fun selectNode(node: WebDevNode?) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun markNodeCompleted(nodeId: String) {
        viewModelScope.launch {
            val doneKey = "node_done_$nodeId"
            val progKey = "node_prog_$nodeId"

            if (!_uiState.value.completedNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, doneKey)
            }
            if (_uiState.value.inProgressNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, progKey)
            }

            _uiState.update { state ->
                val node = state.nodes.firstOrNull { it.id == nodeId }
                val title = node?.label ?: "Topic"
                state.copy(
                    completedNodeIds = state.completedNodeIds + nodeId,
                    inProgressNodeIds = state.inProgressNodeIds - nodeId,
                    celebrationMessage = "🎉 Completed: $title!"
                )
            }
        }
    }

    fun markNodeInProgress(nodeId: String) {
        viewModelScope.launch {
            val doneKey = "node_done_$nodeId"
            val progKey = "node_prog_$nodeId"

            if (_uiState.value.completedNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, doneKey)
            }
            if (!_uiState.value.inProgressNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, progKey)
            }

            _uiState.update { state ->
                state.copy(
                    completedNodeIds = state.completedNodeIds - nodeId,
                    inProgressNodeIds = state.inProgressNodeIds + nodeId,
                    celebrationMessage = "🚀 Started topic!"
                )
            }
        }
    }

    fun resetNode(nodeId: String) {
        viewModelScope.launch {
            val doneKey = "node_done_$nodeId"
            val progKey = "node_prog_$nodeId"

            if (_uiState.value.completedNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, doneKey)
            }
            if (_uiState.value.inProgressNodeIds.contains(nodeId)) {
                repository.toggleMilestoneProgress(roadmapId, progKey)
            }

            _uiState.update { state ->
                state.copy(
                    completedNodeIds = state.completedNodeIds - nodeId,
                    inProgressNodeIds = state.inProgressNodeIds - nodeId
                )
            }
        }
    }

    fun clearCelebration() {
        _uiState.update { it.copy(celebrationMessage = null) }
    }
}
