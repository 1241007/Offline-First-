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
    val errorMessage: String? = null
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
        viewModelScope.launch {
            repository.getMyPersonalizedRoadmaps().onSuccess { list ->
                _uiState.value = _uiState.value.copy(myPersonalizedRoadmaps = list)
            }
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

    // --- Interactive Assessment Advisor Actions ---

    fun startOrResumeAssessment(forceNew: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAssessmentLoading = true,
                errorMessage = null
            )

            if (!forceNew) {
                val activeResult = repository.getActiveAssessmentSession()
                val activeSession = activeResult.getOrNull()
                if (activeSession != null && activeSession.state != "COMPLETED") {
                    _uiState.value = _uiState.value.copy(
                        isAssessmentLoading = false,
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
                        assessmentSession = session,
                        assessmentMessages = session.messages,
                        generatedPersonalizedRoadmap = null,
                        selectedQuizIndex = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAssessmentLoading = false,
                        errorMessage = err.localizedMessage ?: "Failed to connect to EduNova AI Advisor."
                    )
                }
            )
        }
    }

    fun selectQuizOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedQuizIndex = index)
    }

    fun submitAssessmentAnswer(answer: String, quizSelectedIndex: Int? = null) {
        val currentSession = _uiState.value.assessmentSession ?: return
        if (_uiState.value.isSendingAssessmentMessage) return

        val text = answer.trim()
        if (text.isEmpty() && quizSelectedIndex == null) return

        val localStudentMsg = AssessmentMessage(
            speaker = AssessmentSpeaker.LEARNER,
            text = text
        )

        // Optimistically add user turn to message list
        val updatedList = _uiState.value.assessmentMessages + localStudentMsg
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
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isSendingAssessmentMessage = false,
                        errorMessage = err.localizedMessage ?: "Unable to send message to advisor."
                    )
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
                        selectedPersonalizedRoadmap = roadmapDetail
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
    }

    fun toggleMilestone(milestoneKey: String) {
        val current = _uiState.value.completedMilestones
        val updated = if (current.contains(milestoneKey)) {
            current - milestoneKey
        } else {
            current + milestoneKey
        }
        _uiState.value = _uiState.value.copy(completedMilestones = updated)
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
