package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val roadmaps: UiState<List<RoadmapOption>> = UiState.Loading,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val dashboardTab: String = "All", // "All", "In Progress", "Completed", "Catalog"
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    // Personalized Roadmaps
    val myPersonalizedRoadmaps: List<PersonalizedRoadmapDetail> = emptyList(),
    val selectedPersonalizedRoadmap: PersonalizedRoadmapDetail? = null,
    // Assessment State Machine
    val isAssessmentLoading: Boolean = false,
    val isSendingAssessmentMessage: Boolean = false,
    val assessmentSession: AssessmentSessionState? = null,
    val assessmentMessages: List<AssessmentMessage> = emptyList(),
    val selectedQuizIndex: Int? = null,
    val isGenerating: Boolean = false,
    val generatedPersonalizedRoadmap: PersonalizedRoadmapDetail? = null,
    val generatedRoadmap: GeneratedRoadmapPreview? = null,
    val completedMilestones: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val assessmentError: String? = null
)

class RoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadMyPersonalizedRoadmaps()
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

    fun loadMyPersonalizedRoadmaps() {
        // 1. Immediately load durable cached roadmaps so screen displays with zero latency
        val cached = repository.getMyPersonalizedRoadmapsCached()
        if (cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(myPersonalizedRoadmaps = cached)
        }

        // 2. Fetch and merge cloud data asynchronously
        viewModelScope.launch {
            repository.getMyPersonalizedRoadmaps().onSuccess { list ->
                _uiState.value = _uiState.value.copy(myPersonalizedRoadmaps = list)
            }
        }
    }

    fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail) {
        viewModelScope.launch {
            repository.savePersonalizedRoadmap(roadmap).onSuccess { saved ->
                val current = _uiState.value.myPersonalizedRoadmaps
                val updated = (listOf(saved) + current).distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    myPersonalizedRoadmaps = updated,
                    selectedPersonalizedRoadmap = saved
                )
            }
        }
    }

    fun deletePersonalizedRoadmap(roadmapId: String) {
        viewModelScope.launch {
            repository.deletePersonalizedRoadmap(roadmapId).onSuccess {
                val updated = _uiState.value.myPersonalizedRoadmaps.filter { it.id != roadmapId }
                _uiState.value = _uiState.value.copy(
                    myPersonalizedRoadmaps = updated,
                    selectedPersonalizedRoadmap = if (_uiState.value.selectedPersonalizedRoadmap?.id == roadmapId) null else _uiState.value.selectedPersonalizedRoadmap
                )
            }
        }
    }

    fun renamePersonalizedRoadmap(roadmapId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renamePersonalizedRoadmap(roadmapId, newTitle).onSuccess { updatedItem ->
                val updatedList = _uiState.value.myPersonalizedRoadmaps.map {
                    if (it.id == roadmapId) updatedItem else it
                }
                _uiState.value = _uiState.value.copy(
                    myPersonalizedRoadmaps = updatedList,
                    selectedPersonalizedRoadmap = if (_uiState.value.selectedPersonalizedRoadmap?.id == roadmapId) updatedItem else _uiState.value.selectedPersonalizedRoadmap
                )
            }
        }
    }

    fun setDashboardTab(tab: String) {
        _uiState.value = _uiState.value.copy(dashboardTab = tab)
    }

    fun calculateRoadmapProgress(roadmap: PersonalizedRoadmapDetail, completedSet: Set<String>? = null): Int {
        val totalMilestones = roadmap.phases.flatMap { it.milestones }
        val totalTasks = roadmap.phases.flatMap { it.tasks }
        val totalItems = totalMilestones.size + totalTasks.size
        if (totalItems == 0) return 0

        val completedKeys = completedSet ?: if (_uiState.value.completedMilestones.isNotEmpty()) _uiState.value.completedMilestones else roadmap.completedMilestones.toSet()
        var completedCount = 0

        roadmap.phases.forEachIndexed { pIdx, phase ->
            phase.milestones.forEachIndexed { mIdx, m ->
                val key = "${roadmap.id}_${pIdx}_${mIdx}_${m.title}"
                if (completedKeys.contains(key) || completedKeys.contains(m.title) || m.isCompleted) {
                    completedCount++
                }
            }
            phase.tasks.forEach { t ->
                val key = "${roadmap.id}_task_${t.id}"
                if (completedKeys.contains(key) || completedKeys.contains(t.id) || t.isCompleted) {
                    completedCount++
                }
            }
        }
        return ((completedCount.toFloat() / totalItems.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun isTaskCompleted(roadmapId: String, taskId: String, completedSet: Set<String>? = null): Boolean {
        val keys = completedSet ?: _uiState.value.completedMilestones
        return keys.contains("${roadmapId}_task_${taskId}") || keys.contains(taskId)
    }

    fun isMilestoneCompleted(roadmapId: String, pIdx: Int, mIdx: Int, title: String, completedSet: Set<String>? = null): Boolean {
        val keys = completedSet ?: _uiState.value.completedMilestones
        val key = "${roadmapId}_${pIdx}_${mIdx}_${title}"
        return keys.contains(key) || keys.contains(title)
    }

    fun getNextActionableMilestone(roadmap: PersonalizedRoadmapDetail, completedSet: Set<String>? = null): String {
        val completedKeys = completedSet ?: roadmap.completedMilestones.toSet()
        for ((pIdx, phase) in roadmap.phases.withIndex()) {
            for (task in phase.tasks) {
                val tKey = "${roadmap.id}_task_${task.id}"
                val isDone = completedKeys.contains(tKey) || completedKeys.contains(task.id) || task.isCompleted
                if (!isDone) {
                    return task.title
                }
            }
            for ((mIdx, milestone) in phase.milestones.withIndex()) {
                val key = "${roadmap.id}_${pIdx}_${mIdx}_${milestone.title}"
                val isDone = completedKeys.contains(key) || completedKeys.contains(milestone.title) || milestone.isCompleted
                if (!isDone) {
                    return milestone.title
                }
            }
        }
        return "All milestones completed!"
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

    // --- Interactive Assessment Advisor Actions ---

    fun startOrResumeAssessment(forceNew: Boolean = false) {
        if (!forceNew && _uiState.value.assessmentSession != null && _uiState.value.assessmentSession?.state != "COMPLETED") {
            _uiState.value = _uiState.value.copy(
                isAssessmentLoading = false,
                assessmentError = null,
                errorMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAssessmentLoading = true,
                assessmentError = null,
                errorMessage = null
            )

            if (!forceNew) {
                val activeResult = repository.getActiveAssessmentSession()
                val activeSession = activeResult.getOrNull()
                if (activeSession != null && activeSession.state != "COMPLETED") {
                    _uiState.value = _uiState.value.copy(
                        isAssessmentLoading = false,
                        assessmentError = null,
                        assessmentSession = activeSession,
                        assessmentMessages = activeSession.messages,
                        selectedQuizIndex = null
                    )
                    return@launch
                }
            }

            // Start fresh session
            val startResult = repository.startPersonalizedAssessment()
            startResult.fold(
                onSuccess = { session ->
                    _uiState.value = _uiState.value.copy(
                        isAssessmentLoading = false,
                        assessmentError = null,
                        assessmentSession = session,
                        assessmentMessages = session.messages,
                        generatedPersonalizedRoadmap = null,
                        selectedQuizIndex = null
                    )
                },
                onFailure = { err ->
                    val friendlyMsg = when {
                        err.message?.contains("500") == true -> "Server temporarily unavailable. Please tap Retry to reconnect."
                        err.message?.contains("401") == true -> "Session expired. Please sign in again."
                        err.message?.contains("Unable to resolve host") == true -> "Network connection unavailable. Please check your internet connection."
                        else -> err.localizedMessage ?: "Failed to connect to EduNova AI Advisor."
                    }
                    _uiState.value = _uiState.value.copy(
                        isAssessmentLoading = false,
                        assessmentError = friendlyMsg,
                        errorMessage = friendlyMsg
                    )
                }
            )
        }
    }

    fun startNewAssessment() {
        _uiState.value = _uiState.value.copy(
            assessmentSession = null,
            assessmentMessages = emptyList(),
            generatedPersonalizedRoadmap = null,
            generatedRoadmap = null,
            selectedQuizIndex = null
        )
        startOrResumeAssessment(forceNew = true)
    }

    fun retryAssessment() {
        startOrResumeAssessment(forceNew = false)
    }

    fun selectQuizOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedQuizIndex = index)
    }

    fun submitAssessmentAnswer(
        answer: String,
        quizSelectedIndex: Int? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val currentSession = _uiState.value.assessmentSession ?: return
        if (_uiState.value.isSendingAssessmentMessage) return

        val text = answer.trim()
        if (text.isEmpty() && quizSelectedIndex == null) return

        val previousMessages = _uiState.value.assessmentMessages
        val localStudentMsg = AssessmentMessage(
            speaker = AssessmentSpeaker.LEARNER,
            text = text,
            stepId = currentSession.currentStepId
        )

        // Optimistically add user turn to message list
        val updatedList = previousMessages + localStudentMsg
        _uiState.value = _uiState.value.copy(
            assessmentMessages = updatedList,
            isSendingAssessmentMessage = true,
            selectedQuizIndex = null,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.submitAssessmentAnswer(
                sessionId = currentSession.id,
                answer = text,
                quizSelectedIndex = quizSelectedIndex
            )
            result.fold(
                onSuccess = { updatedSession ->
                    _uiState.value = _uiState.value.copy(
                        isSendingAssessmentMessage = false,
                        assessmentSession = updatedSession,
                        assessmentMessages = updatedSession.messages,
                        selectedQuizIndex = null
                    )
                    onComplete?.invoke(true)
                },
                onFailure = { err ->
                    // Roll back optimistic message to prevent duplicate ghost messages
                    _uiState.value = _uiState.value.copy(
                        isSendingAssessmentMessage = false,
                        assessmentMessages = previousMessages,
                        errorMessage = err.localizedMessage ?: "Unable to send message to advisor."
                    )
                    onComplete?.invoke(false)
                }
            )
        }
    }

    fun generatePersonalizedRoadmap() {
        val currentSession = _uiState.value.assessmentSession ?: return
        if (_uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null
            )
            val result = repository.generatePersonalizedRoadmap(currentSession.id)
            result.fold(
                onSuccess = { roadmapDetail ->
                    val legacyPreview = GeneratedRoadmapPreview(
                        goal = roadmapDetail.goal,
                        level = roadmapDetail.level,
                        studyTime = "${roadmapDetail.weeklyHours} hrs/week",
                        duration = roadmapDetail.duration,
                        stages = roadmapDetail.phases.map { it.title }
                    )
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedPersonalizedRoadmap = roadmapDetail,
                        generatedRoadmap = legacyPreview,
                        selectedPersonalizedRoadmap = roadmapDetail,
                        completedMilestones = roadmapDetail.completedMilestones.toSet()
                    )
                    loadMyPersonalizedRoadmaps()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = err.localizedMessage ?: "Failed to generate personalized roadmap."
                    )
                }
            )
        }
    }

    fun selectPersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail?) {
        _uiState.value = _uiState.value.copy(selectedPersonalizedRoadmap = roadmap)
        if (roadmap != null) {
            viewModelScope.launch {
                val completed = repository.getCompletedMilestones(roadmap.id)
                _uiState.value = _uiState.value.copy(
                    completedMilestones = if (completed.isNotEmpty()) completed else roadmap.completedMilestones.toSet()
                )
            }
        }
    }

    fun toggleMilestone(milestoneKey: String) {
        val currentRoadmap = _uiState.value.selectedPersonalizedRoadmap ?: _uiState.value.generatedPersonalizedRoadmap
        val current = _uiState.value.completedMilestones
        val updated = if (current.contains(milestoneKey)) {
            current - milestoneKey
        } else {
            current + milestoneKey
        }
        _uiState.value = _uiState.value.copy(completedMilestones = updated)

        if (currentRoadmap != null) {
            viewModelScope.launch {
                repository.toggleMilestoneProgress(currentRoadmap.id, milestoneKey)
                    .onSuccess { newSet ->
                        _uiState.value = _uiState.value.copy(completedMilestones = newSet)
                    }
            }
        }
    }

    fun toggleTask(taskId: String) {
        val currentRoadmap = _uiState.value.selectedPersonalizedRoadmap ?: _uiState.value.generatedPersonalizedRoadmap ?: return
        val taskKey = "${currentRoadmap.id}_task_${taskId}"
        toggleMilestone(taskKey)
    }

    fun correctProfileField(field: String, value: Any, onComplete: ((Boolean) -> Unit)? = null) {
        val currentSession = _uiState.value.assessmentSession ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingAssessmentMessage = true, errorMessage = null)
            repository.correctProfileField(currentSession.id, field, value.toString()).fold(
                onSuccess = { updatedSession ->
                    _uiState.value = _uiState.value.copy(
                        isSendingAssessmentMessage = false,
                        assessmentSession = updatedSession,
                        assessmentMessages = updatedSession.messages
                    )
                    onComplete?.invoke(true)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isSendingAssessmentMessage = false,
                        errorMessage = err.localizedMessage ?: "Failed to update profile field."
                    )
                    onComplete?.invoke(false)
                }
            )
        }
    }

    fun syncPendingChanges() {
        viewModelScope.launch {
            repository.syncPendingRoadmaps()
            repository.syncPendingMilestoneProgress()
            loadMyPersonalizedRoadmaps()
        }
    }

    fun resetAssessment() {
        _uiState.value = _uiState.value.copy(
            assessmentSession = null,
            assessmentMessages = emptyList(),
            generatedPersonalizedRoadmap = null,
            generatedRoadmap = null,
            selectedQuizIndex = null
        )
        startOrResumeAssessment(forceNew = true)
    }

    fun clearGeneratedRoadmap() {
        _uiState.value = _uiState.value.copy(
            generatedRoadmap = null,
            generatedPersonalizedRoadmap = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

