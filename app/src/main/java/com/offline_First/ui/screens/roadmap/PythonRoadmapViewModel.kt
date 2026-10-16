package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.local.PythonRoadmapDataProvider
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PythonRoadmapUiState(
    val modules: List<PythonModule> = PythonRoadmapDataProvider.getModules(),
    val progress: PythonRoadmapProgress = PythonRoadmapProgress(),
    val selectedTopic: PythonTopic? = null,
    val expandedModuleIds: Set<String> = setOf("mod_1_fundamentals", "mod_2_control_flow"), // First two expanded by default
    val searchQuery: String = "",
    val statusFilter: TopicLearningStatus? = null,
    val isLoading: Boolean = false,
    val celebrationMessage: String? = null
) {
    val totalTopicsCount: Int = modules.sumOf { it.topics.size }

    val completedCount: Int = progress.totalCompleted
    val inProgressCount: Int = progress.totalInProgress
    val skippedCount: Int = progress.totalSkipped

    val overallPercentage: Int = if (totalTopicsCount > 0) {
        ((completedCount.toFloat() / totalTopicsCount.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val remainingMinutes: Int = modules.flatMap { it.topics }
        .filter { progress.getStatus(it.id) != TopicLearningStatus.COMPLETED && progress.getStatus(it.id) != TopicLearningStatus.SKIPPED }
        .sumOf { it.estimatedMinutes }

    fun getModuleProgress(moduleId: String): Int {
        val mod = modules.firstOrNull { it.id == moduleId } ?: return 0
        if (mod.topics.isEmpty()) return 0
        val done = mod.topics.count { progress.getStatus(it.id) == TopicLearningStatus.COMPLETED }
        return ((done.toFloat() / mod.topics.size.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun isModuleCompleted(moduleId: String): Boolean = getModuleProgress(moduleId) >= 100
}

class PythonRoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val roadmapId = "python_programming_roadmap"
    private val _uiState = MutableStateFlow(PythonRoadmapUiState())
    val uiState: StateFlow<PythonRoadmapUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rawKeys = repository.getCompletedMilestones(roadmapId)

            val completed = mutableSetOf<String>()
            val inProgress = mutableSetOf<String>()
            val skipped = mutableSetOf<String>()

            for (key in rawKeys) {
                when {
                    key.startsWith("topic_") -> completed.add(key.removePrefix("topic_"))
                    key.startsWith("inprogress_") -> inProgress.add(key.removePrefix("inprogress_"))
                    key.startsWith("skipped_") -> skipped.add(key.removePrefix("skipped_"))
                }
            }

            _uiState.update {
                it.copy(
                    progress = PythonRoadmapProgress(
                        completedTopicIds = completed,
                        inProgressTopicIds = inProgress,
                        skippedTopicIds = skipped
                    ),
                    isLoading = false
                )
            }
        }
    }

    fun selectTopic(topic: PythonTopic?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun toggleModuleExpansion(moduleId: String) {
        _uiState.update { state ->
            val updated = if (state.expandedModuleIds.contains(moduleId)) {
                state.expandedModuleIds - moduleId
            } else {
                state.expandedModuleIds + moduleId
            }
            state.copy(expandedModuleIds = updated)
        }
    }

    fun expandAllModules() {
        val allIds = _uiState.value.modules.map { it.id }.toSet()
        _uiState.update { it.copy(expandedModuleIds = allIds) }
    }

    fun collapseAllModules() {
        _uiState.update { it.copy(expandedModuleIds = emptySet()) }
    }

    fun markTopicCompleted(topicId: String) {
        viewModelScope.launch {
            val topicKey = "topic_$topicId"
            val inProgressKey = "inprogress_$topicId"
            val skippedKey = "skipped_$topicId"

            val currentStatus = _uiState.value.progress.getStatus(topicId)
            if (currentStatus != TopicLearningStatus.COMPLETED) {
                repository.toggleMilestoneProgress(roadmapId, topicKey)
            }
            if (currentStatus == TopicLearningStatus.IN_PROGRESS) {
                repository.toggleMilestoneProgress(roadmapId, inProgressKey)
            }
            if (currentStatus == TopicLearningStatus.SKIPPED) {
                repository.toggleMilestoneProgress(roadmapId, skippedKey)
            }

            _uiState.update { state ->
                val newCompleted = state.progress.completedTopicIds + topicId
                val newInProgress = state.progress.inProgressTopicIds - topicId
                val newSkipped = state.progress.skippedTopicIds - topicId
                val topic = state.modules.flatMap { it.topics }.firstOrNull { it.id == topicId }
                val title = topic?.title ?: "Topic"
                state.copy(
                    progress = PythonRoadmapProgress(
                        completedTopicIds = newCompleted,
                        inProgressTopicIds = newInProgress,
                        skippedTopicIds = newSkipped
                    ),
                    celebrationMessage = "🎉 Completed: $title!"
                )
            }
        }
    }

    fun markTopicInProgress(topicId: String) {
        viewModelScope.launch {
            val topicKey = "topic_$topicId"
            val inProgressKey = "inprogress_$topicId"
            val skippedKey = "skipped_$topicId"

            val currentStatus = _uiState.value.progress.getStatus(topicId)
            if (currentStatus == TopicLearningStatus.COMPLETED) {
                repository.toggleMilestoneProgress(roadmapId, topicKey)
            }
            if (currentStatus != TopicLearningStatus.IN_PROGRESS) {
                repository.toggleMilestoneProgress(roadmapId, inProgressKey)
            }
            if (currentStatus == TopicLearningStatus.SKIPPED) {
                repository.toggleMilestoneProgress(roadmapId, skippedKey)
            }

            _uiState.update { state ->
                val newCompleted = state.progress.completedTopicIds - topicId
                val newInProgress = state.progress.inProgressTopicIds + topicId
                val newSkipped = state.progress.skippedTopicIds - topicId
                state.copy(
                    progress = PythonRoadmapProgress(
                        completedTopicIds = newCompleted,
                        inProgressTopicIds = newInProgress,
                        skippedTopicIds = newSkipped
                    ),
                    celebrationMessage = "🚀 Learning started!"
                )
            }
        }
    }

    fun skipTopic(topicId: String) {
        viewModelScope.launch {
            val topicKey = "topic_$topicId"
            val inProgressKey = "inprogress_$topicId"
            val skippedKey = "skipped_$topicId"

            val currentStatus = _uiState.value.progress.getStatus(topicId)
            if (currentStatus == TopicLearningStatus.COMPLETED) {
                repository.toggleMilestoneProgress(roadmapId, topicKey)
            }
            if (currentStatus == TopicLearningStatus.IN_PROGRESS) {
                repository.toggleMilestoneProgress(roadmapId, inProgressKey)
            }
            if (currentStatus != TopicLearningStatus.SKIPPED) {
                repository.toggleMilestoneProgress(roadmapId, skippedKey)
            }

            _uiState.update { state ->
                val newCompleted = state.progress.completedTopicIds - topicId
                val newInProgress = state.progress.inProgressTopicIds - topicId
                val newSkipped = state.progress.skippedTopicIds + topicId
                state.copy(
                    progress = PythonRoadmapProgress(
                        completedTopicIds = newCompleted,
                        inProgressTopicIds = newInProgress,
                        skippedTopicIds = newSkipped
                    )
                )
            }
        }
    }

    fun resetTopicStatus(topicId: String) {
        viewModelScope.launch {
            val topicKey = "topic_$topicId"
            val inProgressKey = "inprogress_$topicId"
            val skippedKey = "skipped_$topicId"

            val currentStatus = _uiState.value.progress.getStatus(topicId)
            if (currentStatus == TopicLearningStatus.COMPLETED) {
                repository.toggleMilestoneProgress(roadmapId, topicKey)
            }
            if (currentStatus == TopicLearningStatus.IN_PROGRESS) {
                repository.toggleMilestoneProgress(roadmapId, inProgressKey)
            }
            if (currentStatus == TopicLearningStatus.SKIPPED) {
                repository.toggleMilestoneProgress(roadmapId, skippedKey)
            }

            _uiState.update { state ->
                val newCompleted = state.progress.completedTopicIds - topicId
                val newInProgress = state.progress.inProgressTopicIds - topicId
                val newSkipped = state.progress.skippedTopicIds - topicId
                state.copy(
                    progress = PythonRoadmapProgress(
                        completedTopicIds = newCompleted,
                        inProgressTopicIds = newInProgress,
                        skippedTopicIds = newSkipped
                    )
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setStatusFilter(filter: TopicLearningStatus?) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun clearCelebration() {
        _uiState.update { it.copy(celebrationMessage = null) }
    }
}
