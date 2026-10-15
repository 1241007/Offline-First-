package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.data.staging.JavaRoadmapStagingData
import com.offline_First.data.staging.LearningPathItem
import com.offline_First.data.staging.MLRoadmapStagingData
import com.offline_First.data.staging.PythonRoadmapStagingData
import com.offline_First.data.staging.RoadmapLessonTopic
import com.offline_First.data.staging.RoadmapProjectItem
import com.offline_First.data.staging.SqlRoadmapStagingData
import com.offline_First.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RoadmapSubView {
    CATALOG,
    PYTHON_DEVELOPER,
    JAVA_DEVELOPER,
    MACHINE_LEARNING,
    SQL_ROADMAP,
    TOPIC_DETAIL
}

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
    val assessmentError: String? = null,

    // Roadmap.sh Journey Extensions
    val currentSubView: RoadmapSubView = RoadmapSubView.CATALOG,
    val selectedLearningPathId: String? = null,
    val selectedTopic: RoadmapLessonTopic? = null,
    val selectedProject: RoadmapProjectItem? = null,
    val selectedUpcomingPath: LearningPathItem? = null,
    val expandedModules: Set<String> = setOf("mod_1_basics"),
    val completedTopicIds: Set<String> = emptySet(),
    val completedProjectIds: Set<String> = emptySet(),
    val completedJavaTopicIds: Set<String> = emptySet(),
    val completedMlTopicIds: Set<String> = emptySet(),
    val completedSqlTopicIds: Set<String> = emptySet(),
    val catalogSearchQuery: String = "",
    val catalogFilterTab: String = "All", // "All", "Role-based", "Skill-based", "My Roadmaps"
    val pythonCourseTab: String = "Roadmap", // "Roadmap", "Projects", "AI Tutor"
    val javaCourseTab: String = "Roadmap", // "Roadmap", "Projects", "AI Tutor"
    val mlCourseTab: String = "Roadmap", // "Roadmap", "Projects", "AI Tutor"
    val sqlCourseTab: String = "Roadmap", // "Roadmap", "Projects", "AI Tutor"
    val aiTutorTopicPrompt: String? = null,
    val roadmapMainTab: String = "Roadmaps", // "Roadmaps", "Lesson Packs", "Project Ideas"
    val bookmarkedCourseNames: Set<String> = emptySet(),
    val selectedCourseOverview: Pair<String, String>? = null // (courseName, category)
)

class RoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadMyPersonalizedRoadmaps()
        loadPythonProgress()
        loadJavaProgress()
        loadMlProgress()
        loadSqlProgress()
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

    // --- Roadmap.sh Journey Extensions ---

    fun loadPythonProgress() {
        viewModelScope.launch {
            val completed = repository.getCompletedMilestones("python-developer")
            _uiState.value = _uiState.value.copy(completedTopicIds = completed)
        }
    }

    fun setCatalogSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(catalogSearchQuery = query)
    }

    fun setCatalogFilterTab(tab: String) {
        _uiState.value = _uiState.value.copy(catalogFilterTab = tab)
    }

    fun selectLearningPath(path: LearningPathItem) {
        if (path.id == "java-programming" || path.id == "java-developer") {
            openJavaDeveloperScreen()
            return
        }
        if (path.id == "machine-learning" || path.id == "machine-learning-engineer") {
            openMachineLearningScreen()
            return
        }
        if (path.id == "sql-databases" || path.id == "sql-roadmap") {
            openSqlScreen()
            return
        }
        if (path.id == "python-developer" || path.id == "python-programming") {
            openPythonDeveloperScreen()
            return
        }
        if (path.isImplemented) {
            _uiState.value = _uiState.value.copy(
                selectedLearningPathId = path.id,
                currentSubView = RoadmapSubView.PYTHON_DEVELOPER
            )
        } else {
            _uiState.value = _uiState.value.copy(selectedUpcomingPath = path)
        }
    }

    fun dismissUpcomingPathDialog() {
        _uiState.value = _uiState.value.copy(selectedUpcomingPath = null)
    }

    fun setRoadmapMainTab(tab: String) {
        _uiState.value = _uiState.value.copy(roadmapMainTab = tab)
    }

    fun toggleBookmarkCourse(courseName: String) {
        val current = _uiState.value.bookmarkedCourseNames
        val updated = if (current.contains(courseName)) current - courseName else current + courseName
        _uiState.value = _uiState.value.copy(bookmarkedCourseNames = updated)
    }

    fun selectCourseByName(courseName: String, category: String) {
        if (courseName.equals("Python", ignoreCase = true) || courseName.equals("Python Programming", ignoreCase = true) || courseName.equals("Python Developer", ignoreCase = true)) {
            openPythonDeveloperScreen()
            return
        }
        if (courseName.equals("Java", ignoreCase = true) || courseName.equals("Java Developer", ignoreCase = true) || courseName.equals("Java Programming", ignoreCase = true)) {
            openJavaDeveloperScreen()
            return
        }
        if (courseName.equals("Machine Learning", ignoreCase = true) || courseName.equals("Machine Learning Engineer", ignoreCase = true)) {
            openMachineLearningScreen()
            return
        }
        if (courseName.equals("SQL", ignoreCase = true) || courseName.equals("SQL & Database Management", ignoreCase = true) || courseName.equals("SQL Roadmap", ignoreCase = true)) {
            openSqlScreen()
            return
        }
        _uiState.value = _uiState.value.copy(selectedCourseOverview = Pair(courseName, category))
    }

    fun dismissCourseOverviewDialog() {
        _uiState.value = _uiState.value.copy(selectedCourseOverview = null)
    }

    fun setPythonCourseTab(tab: String) {
        _uiState.value = _uiState.value.copy(pythonCourseTab = tab)
    }

    fun toggleModuleExpansion(moduleId: String) {
        val current = _uiState.value.expandedModules
        val updated = if (current.contains(moduleId)) current - moduleId else current + moduleId
        _uiState.value = _uiState.value.copy(expandedModules = updated)
    }

    fun selectTopic(topic: RoadmapLessonTopic) {
        _uiState.value = _uiState.value.copy(
            selectedTopic = topic,
            currentSubView = RoadmapSubView.TOPIC_DETAIL
        )
    }

    fun selectProject(project: RoadmapProjectItem?) {
        _uiState.value = _uiState.value.copy(selectedProject = project)
    }

    fun toggleTopicCompletion(topicId: String) {
        val current = _uiState.value.completedTopicIds
        val updated = if (current.contains(topicId)) current - topicId else current + topicId
        _uiState.value = _uiState.value.copy(completedTopicIds = updated)

        viewModelScope.launch {
            repository.toggleMilestoneProgress("python-developer", topicId)
                .onSuccess { newSet ->
                    _uiState.value = _uiState.value.copy(completedTopicIds = newSet)
                }
        }
    }

    fun toggleProjectCompletion(projectId: String) {
        val current = _uiState.value.completedProjectIds
        val updated = if (current.contains(projectId)) current - projectId else current + projectId
        _uiState.value = _uiState.value.copy(completedProjectIds = updated)
        toggleTopicCompletion("project_$projectId")
    }

    fun navigateToNextTopic() {
        val current = _uiState.value.selectedTopic ?: return
        val allTopics = PythonRoadmapStagingData.PYTHON_MODULES.flatMap { it.topics }
        val idx = allTopics.indexOfFirst { it.id == current.id }
        if (idx in 0 until allTopics.lastIndex) {
            _uiState.value = _uiState.value.copy(selectedTopic = allTopics[idx + 1])
        }
    }

    fun navigateToPrevTopic() {
        val current = _uiState.value.selectedTopic ?: return
        val allTopics = PythonRoadmapStagingData.PYTHON_MODULES.flatMap { it.topics }
        val idx = allTopics.indexOfFirst { it.id == current.id }
        if (idx > 0) {
            _uiState.value = _uiState.value.copy(selectedTopic = allTopics[idx - 1])
        }
    }

    fun setAiTutorTopicPrompt(prompt: String?) {
        _uiState.value = _uiState.value.copy(aiTutorTopicPrompt = prompt)
    }

    fun setAiTutorPrompt(prompt: String?) {
        setAiTutorTopicPrompt(prompt)
    }

    fun calculatePythonProgress(): Int {
        val total = PythonRoadmapStagingData.ALL_VISUAL_NODES.size
        if (total == 0) return 0
        val completed = _uiState.value.completedTopicIds.count { id ->
            PythonRoadmapStagingData.ALL_VISUAL_NODES.any { it.id == id }
        }
        return ((completed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    // --- Java Roadmap Journey Support ---

    fun loadJavaProgress() {
        viewModelScope.launch {
            val completed = repository.getCompletedMilestones("java-developer")
            _uiState.value = _uiState.value.copy(completedJavaTopicIds = completed)
        }
    }

    fun calculateJavaProgress(): Int {
        val total = JavaRoadmapStagingData.ALL_VISUAL_NODES.size
        if (total == 0) return 0
        val completedCount = _uiState.value.completedJavaTopicIds.count { id ->
            JavaRoadmapStagingData.ALL_VISUAL_NODES.any { it.id == id }
        }
        return ((completedCount.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun setJavaCourseTab(tab: String) {
        _uiState.value = _uiState.value.copy(javaCourseTab = tab)
    }

    fun toggleJavaTopicCompletion(topicId: String) {
        val current = _uiState.value.completedJavaTopicIds
        val updated = if (current.contains(topicId)) current - topicId else current + topicId
        _uiState.value = _uiState.value.copy(completedJavaTopicIds = updated)

        viewModelScope.launch {
            repository.toggleMilestoneProgress("java-developer", topicId)
                .onSuccess { newSet ->
                    _uiState.value = _uiState.value.copy(completedJavaTopicIds = newSet)
                }
        }
    }

    fun openPythonDeveloperScreen() {
        _uiState.value = _uiState.value.copy(
            currentSubView = RoadmapSubView.PYTHON_DEVELOPER,
            selectedLearningPathId = "python-developer"
        )
    }

    fun openJavaDeveloperScreen() {
        _uiState.value = _uiState.value.copy(
            currentSubView = RoadmapSubView.JAVA_DEVELOPER,
            selectedLearningPathId = "java-developer"
        )
    }

    fun openMachineLearningScreen() {
        _uiState.value = _uiState.value.copy(
            currentSubView = RoadmapSubView.MACHINE_LEARNING,
            selectedLearningPathId = "machine-learning"
        )
    }

    fun openSqlScreen() {
        _uiState.value = _uiState.value.copy(
            currentSubView = RoadmapSubView.SQL_ROADMAP,
            selectedLearningPathId = "sql-databases"
        )
    }

    // --- Machine Learning Support ---

    fun loadMlProgress() {
        viewModelScope.launch {
            val completed = repository.getCompletedMilestones("machine-learning")
            _uiState.value = _uiState.value.copy(completedMlTopicIds = completed)
        }
    }

    fun calculateMlProgress(): Int {
        val total = MLRoadmapStagingData.ALL_VISUAL_NODES.size
        if (total == 0) return 0
        val completedCount = _uiState.value.completedMlTopicIds.count { id ->
            MLRoadmapStagingData.ALL_VISUAL_NODES.any { it.id == id }
        }
        return ((completedCount.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun setMlCourseTab(tab: String) {
        _uiState.value = _uiState.value.copy(mlCourseTab = tab)
    }

    fun toggleMlTopicCompletion(topicId: String) {
        val current = _uiState.value.completedMlTopicIds
        val updated = if (current.contains(topicId)) current - topicId else current + topicId
        _uiState.value = _uiState.value.copy(completedMlTopicIds = updated)

        viewModelScope.launch {
            repository.toggleMilestoneProgress("machine-learning", topicId)
                .onSuccess { newSet ->
                    _uiState.value = _uiState.value.copy(completedMlTopicIds = newSet)
                }
        }
    }

    // --- SQL Support ---

    fun loadSqlProgress() {
        viewModelScope.launch {
            val completed = repository.getCompletedMilestones("sql-databases")
            _uiState.value = _uiState.value.copy(completedSqlTopicIds = completed)
        }
    }

    fun calculateSqlProgress(): Int {
        val total = SqlRoadmapStagingData.ALL_VISUAL_NODES.size
        if (total == 0) return 0
        val completedCount = _uiState.value.completedSqlTopicIds.count { id ->
            SqlRoadmapStagingData.ALL_VISUAL_NODES.any { it.id == id }
        }
        return ((completedCount.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    fun setSqlCourseTab(tab: String) {
        _uiState.value = _uiState.value.copy(sqlCourseTab = tab)
    }

    fun toggleSqlTopicCompletion(topicId: String) {
        val current = _uiState.value.completedSqlTopicIds
        val updated = if (current.contains(topicId)) current - topicId else current + topicId
        _uiState.value = _uiState.value.copy(completedSqlTopicIds = updated)

        viewModelScope.launch {
            repository.toggleMilestoneProgress("sql-databases", topicId)
                .onSuccess { newSet ->
                    _uiState.value = _uiState.value.copy(completedSqlTopicIds = newSet)
                }
        }
    }

    fun navigateBackInRoadmap(): Boolean {
        val current = _uiState.value.currentSubView
        return when (current) {
            RoadmapSubView.TOPIC_DETAIL -> {
                _uiState.value = _uiState.value.copy(
                    currentSubView = RoadmapSubView.PYTHON_DEVELOPER,
                    selectedTopic = null
                )
                true
            }
            RoadmapSubView.JAVA_DEVELOPER,
            RoadmapSubView.PYTHON_DEVELOPER,
            RoadmapSubView.MACHINE_LEARNING,
            RoadmapSubView.SQL_ROADMAP -> {
                _uiState.value = _uiState.value.copy(
                    currentSubView = RoadmapSubView.CATALOG,
                    selectedLearningPathId = null
                )
                true
            }
            RoadmapSubView.CATALOG -> false
        }
    }
}


