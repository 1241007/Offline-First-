package com.offline_First.ui.screens.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.remote.RoadmapWsEvent
import com.offline_First.data.repository.RoadmapRepository
import com.offline_First.domain.model.AssessmentPhase
import com.offline_First.domain.model.AssessmentQuestion
import com.offline_First.domain.model.AssessmentQuestionType
import com.offline_First.domain.model.AssessmentSessionState
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.PersonalizedRoadmapTopic
import com.offline_First.domain.model.PersonalizedTopicStatus
import com.offline_First.domain.model.TopicLearningContent
import com.offline_First.domain.model.TopicAssessmentResult
import com.offline_First.domain.model.TopicReviewResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class PersonalizedRoadmapUiState(
    val assessmentState: AssessmentSessionState? = null,
    val activeRoadmap: PersonalizedRoadmapDetail? = null,
    val savedRoadmaps: List<PersonalizedRoadmapDetail> = emptyList(),
    val targetCourseInput: String = "",
    val isValidatingCourse: Boolean = false,
    val courseValidationError: String? = null,
    val isLoading: Boolean = false,
    val isSubmittingAnswer: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val selectedTopic: PersonalizedRoadmapTopic? = null,
    val explanationMessage: String? = null,
    val activeTopicContent: TopicLearningContent? = null,
    val isLoadingTopicContent: Boolean = false,
    val isCompletingTopic: Boolean = false,
    val topicQuizSelectedIndex: Int? = null,
    val topicQuizAnswerCorrect: Boolean? = null,
    val topicQuizExplanation: String? = null,
    val hintsUnlockedLevel: Int = 0,
    val solutionRevealed: Boolean = false,
    val assessmentAnswers: Map<Int, Int> = emptyMap(),
    val isSubmittingTopicAssessment: Boolean = false,
    val assessmentResult: TopicAssessmentResult? = null,
    val isReviewingTopic: Boolean = false,
    val topicReviewMessage: String? = null,
    val roadmapJustGenerated: Boolean = false
)

class PersonalizedRoadmapViewModel(
    private val repository: RoadmapRepository = AppContainer.roadmapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalizedRoadmapUiState())
    val uiState: StateFlow<PersonalizedRoadmapUiState> = _uiState.asStateFlow()

    init {
        loadSavedRoadmaps()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            repository.observeRoadmapWsEvents().collect { event ->
                when (event) {
                    is RoadmapWsEvent.SessionReady -> {
                        val q = parseQuestionFromJson(event.currentQuestionJson)
                        _uiState.update { state ->
                            val current = state.assessmentState ?: AssessmentSessionState(
                                id = event.sessionId,
                                sessionId = event.sessionId,
                                domain = event.domain
                            )
                            state.copy(
                                isLoading = false,
                                assessmentState = current.copy(
                                    id = event.sessionId,
                                    sessionId = event.sessionId,
                                    domain = event.domain,
                                    currentQuestion = q ?: current.currentQuestion,
                                    phase = AssessmentPhase.DIAGNOSTIC,
                                    completenessPercentage = event.completenessPercentage,
                                    questionNumber = 1,
                                    totalQuestions = 5
                                )
                            )
                        }
                    }
                    is RoadmapWsEvent.NextQuestion -> {
                        val q = parseQuestionFromJson(event.nextQuestionJson)
                        _uiState.update { state ->
                            val current = state.assessmentState ?: return@update state
                            state.copy(
                                isSubmittingAnswer = false,
                                assessmentState = current.copy(
                                    currentQuestion = q ?: current.currentQuestion,
                                    lastAnswerExplanation = event.feedback,
                                    completenessPercentage = event.completenessPercentage,
                                    questionNumber = current.questionNumber + 1
                                )
                            )
                        }
                    }
                    is RoadmapWsEvent.SessionComplete -> {
                        _uiState.update { state ->
                            val current = state.assessmentState ?: return@update state
                            state.copy(
                                isSubmittingAnswer = false,
                                assessmentState = current.copy(
                                    phase = AssessmentPhase.SUMMARY,
                                    isComplete = true,
                                    lastAnswerExplanation = event.feedback
                                )
                            )
                        }
                    }
                    is RoadmapWsEvent.RoadmapGenerationStarted -> {
                        _uiState.update { it.copy(isGenerating = true) }
                    }
                    is RoadmapWsEvent.RoadmapReady -> {
                        viewModelScope.launch {
                            repository.getPersonalizedRoadmapDetail(event.sessionId)
                                .onSuccess { roadmap ->
                                    _uiState.update { state ->
                                        state.copy(
                                            isGenerating = false,
                                            activeRoadmap = roadmap,
                                            savedRoadmaps = listOf(roadmap) + state.savedRoadmaps.filter { it.id != roadmap.id },
                                            assessmentState = null,
                                            roadmapJustGenerated = true
                                        )
                                    }
                                }
                                .onFailure {
                                    _uiState.update { it.copy(isGenerating = false) }
                                }
                        }
                    }
                    is RoadmapWsEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSubmittingAnswer = false,
                                isGenerating = false,
                                errorMessage = event.message
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun parseQuestionFromJson(qJson: JSONObject?): AssessmentQuestion? {
        if (qJson == null) return null
        val qTypeStr = qJson.optString("question_type", "MULTIPLE_CHOICE")
        val qType = try {
            AssessmentQuestionType.valueOf(qTypeStr.uppercase())
        } catch (_: Exception) {
            AssessmentQuestionType.MULTIPLE_CHOICE
        }
        val opts = mutableListOf<String>()
        qJson.optJSONArray("options")?.let { arr ->
            for (i in 0 until arr.length()) {
                opts.add(arr.optString(i))
            }
        }
        return AssessmentQuestion(
            id = qJson.optString("id", UUID.randomUUID().toString()),
            topicTag = qJson.optString("topic_tag", "core"),
            questionType = qType,
            prompt = qJson.optString("prompt", ""),
            codeSnippet = if (qJson.has("code_snippet") && !qJson.isNull("code_snippet")) qJson.optString("code_snippet") else null,
            options = opts,
            correctOptionIndex = if (qJson.has("correct_option_index") && !qJson.isNull("correct_option_index")) qJson.optInt("correct_option_index") else null,
            difficultyLevel = qJson.optInt("difficulty_level", 1),
            explanation = qJson.optString("explanation", "")
        )
    }

    fun updateCourseInput(input: String) {
        _uiState.update { it.copy(targetCourseInput = input, courseValidationError = null) }
    }

    // -----------------------------------------------------------------------
    // Assessment Session Management
    // -----------------------------------------------------------------------

    fun validateAndStartAssessment(subject: String, onStarted: () -> Unit = {}) {
        val trimmed = subject.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(courseValidationError = "Please enter a valid course name (e.g., Python, React, Java, SQL, DSA).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingCourse = true, courseValidationError = null) }
            repository.validateSubject(trimmed)
                .onSuccess { (isValid, stdName) ->
                    _uiState.update { it.copy(isValidatingCourse = false) }
                    if (isValid) {
                        startAssessment(stdName)
                        onStarted()
                    } else {
                        _uiState.update {
                            it.copy(
                                courseValidationError = "'$trimmed' is not recognized as a valid technology or curriculum topic. Please enter a specific subject like Python, React, Java, SQL, DSA, or Machine Learning."
                            )
                        }
                    }
                }
                .onFailure {
                    // Fallback to starting assessment directly with user entered name
                    _uiState.update { it.copy(isValidatingCourse = false) }
                    startAssessment(trimmed)
                    onStarted()
                }
        }
    }

    fun startAssessment(domain: String = "Python") {
        repository.connectRoadmapWs()
        repository.sendWsStartRoadmap(domain = domain)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.startPersonalizedAssessment(domain)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            assessmentState = session,
                            activeRoadmap = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to start assessment"
                        )
                    }
                }
        }
    }

    fun submitAnswer(answer: String, quizSelectedIndex: Int? = null) {
        val currentSession = _uiState.value.assessmentState ?: return
        val currentQ = currentSession.currentQuestion
        _uiState.update { it.copy(isSubmittingAnswer = true, errorMessage = null) }

        if (repository.isWsConnected()) {
            repository.sendWsSubmitAnswer(
                sessionId = currentSession.sessionId,
                questionId = currentQ?.id,
                answerText = answer,
                quizIndex = quizSelectedIndex
            )
            // The response will arrive via observeWebSocketEvents() (RoadmapWsEvent.NextQuestion / SessionComplete)
        } else {
            viewModelScope.launch {
                repository.submitAssessmentAnswer(
                    sessionId = currentSession.sessionId,
                    answer = answer,
                    quizSelectedIndex = quizSelectedIndex
                ).onSuccess { updatedSession ->
                    _uiState.update {
                        it.copy(
                            isSubmittingAnswer = false,
                            assessmentState = updatedSession
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmittingAnswer = false,
                            errorMessage = error.localizedMessage ?: "Failed to submit answer"
                        )
                    }
                }
            }
        }
    }

    fun correctProfileField(field: String, value: String) {
        val currentSession = _uiState.value.assessmentState ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.correctProfileField(currentSession.sessionId, field, value)
                .onSuccess { updatedSession ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            assessmentState = updatedSession
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to correct field"
                        )
                    }
                }
        }
    }

    fun dismissCorrectionPrompt() {
        _uiState.update { state ->
            state.copy(
                assessmentState = state.assessmentState?.copy(correctionPrompt = null)
            )
        }
    }

    // -----------------------------------------------------------------------
    // Roadmap Generation & CRUD
    // -----------------------------------------------------------------------

    fun resetAssessment() {
        _uiState.update {
            it.copy(
                assessmentState = null,
                targetCourseInput = "",
                courseValidationError = null,
                roadmapJustGenerated = false
            )
        }
    }

    fun consumeRoadmapGenerated() {
        _uiState.update { it.copy(roadmapJustGenerated = false) }
    }

    fun generateRoadmap() {
        val currentSession = _uiState.value.assessmentState ?: return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        if (repository.isWsConnected()) {
            repository.sendWsGenerateRoadmap(currentSession.sessionId)
            // The response will arrive via observeWebSocketEvents() (RoadmapWsEvent.RoadmapReady)
        } else {
            viewModelScope.launch {
                repository.generatePersonalizedRoadmap(currentSession.sessionId)
                    .onSuccess { roadmap ->
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                activeRoadmap = roadmap,
                                savedRoadmaps = listOf(roadmap) + state.savedRoadmaps.filter { it.id != roadmap.id },
                                assessmentState = null,
                                roadmapJustGenerated = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                errorMessage = error.localizedMessage ?: "Failed to generate roadmap"
                            )
                        }
                    }
            }
        }
    }

    fun loadSavedRoadmaps() {
        viewModelScope.launch {
            repository.getMyPersonalizedRoadmaps()
                .onSuccess { roadmaps ->
                    _uiState.update { state ->
                        state.copy(
                            savedRoadmaps = roadmaps,
                            activeRoadmap = state.activeRoadmap ?: roadmaps.firstOrNull()
                        )
                    }
                }
        }
    }

    fun openRoadmap(roadmap: PersonalizedRoadmapDetail) {
        _uiState.update { it.copy(activeRoadmap = roadmap, assessmentState = null) }
    }

    // -----------------------------------------------------------------------
    // Dedicated Topic Learning Content & Completion
    // -----------------------------------------------------------------------

    fun loadTopicContent(roadmapId: String, topicId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingTopicContent = true,
                    activeTopicContent = null,
                    topicQuizSelectedIndex = null,
                    topicQuizAnswerCorrect = null,
                    topicQuizExplanation = null,
                    hintsUnlockedLevel = 0,
                    solutionRevealed = false,
                    assessmentAnswers = emptyMap(),
                    assessmentResult = null,
                    topicReviewMessage = null,
                    errorMessage = null
                )
            }
            repository.getTopicLearningContent(roadmapId, topicId)
                .onSuccess { content ->
                    _uiState.update {
                        it.copy(
                            isLoadingTopicContent = false,
                            activeTopicContent = content
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingTopicContent = false,
                            errorMessage = error.localizedMessage ?: "Failed to load topic lessons"
                        )
                    }
                }
        }
    }

    fun unlockNextHint() {
        _uiState.update {
            it.copy(hintsUnlockedLevel = (it.hintsUnlockedLevel + 1).coerceAtMost(3))
        }
    }

    fun revealSolution() {
        _uiState.update {
            it.copy(solutionRevealed = true)
        }
    }

    fun selectAssessmentAnswer(questionIndex: Int, optionIndex: Int) {
        _uiState.update {
            val updated = it.assessmentAnswers.toMutableMap()
            updated[questionIndex] = optionIndex
            it.copy(assessmentAnswers = updated)
        }
    }

    fun resetAssessmentState() {
        _uiState.update {
            it.copy(
                assessmentAnswers = emptyMap(),
                assessmentResult = null
            )
        }
    }

    fun reviewTopic(roadmapId: String, topicId: String, onReviewed: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewingTopic = true, errorMessage = null) }
            repository.reviewTopic(roadmapId, topicId)
                .onSuccess { reviewResult ->
                    _uiState.update { state ->
                        val updatedRoadmap = state.activeRoadmap?.let { r ->
                            val updatedTopics = r.topics.map { t ->
                                if (t.id == topicId && !t.status.isDone) {
                                    t.copy(status = PersonalizedTopicStatus.REVIEWED)
                                } else t
                            }
                            r.copy(topics = updatedTopics)
                        }
                        state.copy(
                            isReviewingTopic = false,
                            topicReviewMessage = reviewResult.message,
                            activeRoadmap = updatedRoadmap ?: state.activeRoadmap
                        )
                    }
                    onReviewed()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isReviewingTopic = false,
                            errorMessage = err.localizedMessage ?: "Failed to record topic review"
                        )
                    }
                }
        }
    }

    fun submitTopicAssessment(
        roadmapId: String,
        topicId: String,
        onEvaluated: (TopicAssessmentResult) -> Unit = {}
    ) {
        val state = _uiState.value
        val questions = state.activeTopicContent?.assessmentQuestions ?: emptyList()
        val answersList = (0 until questions.size).map { idx ->
            state.assessmentAnswers[idx] ?: -1
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingTopicAssessment = true, errorMessage = null) }
            repository.submitTopicAssessment(
                roadmapId = roadmapId,
                topicId = topicId,
                answers = answersList,
                hintsUsed = state.hintsUnlockedLevel,
                solutionRevealed = state.solutionRevealed
            ).onSuccess { res ->
                _uiState.update { curr ->
                    val updatedRoadmap = curr.activeRoadmap?.let { r ->
                        val completedSet = r.completedTopicIds.toMutableSet()
                        val inProgressSet = r.inProgressTopicIds.toMutableSet()
                        if (res.passed) {
                            completedSet.add(topicId)
                            inProgressSet.remove(topicId)
                        }
                        val status = when (res.status) {
                            "MASTERED" -> PersonalizedTopicStatus.MASTERED
                            "VERIFIED_COMPLETE" -> PersonalizedTopicStatus.VERIFIED_COMPLETE
                            else -> PersonalizedTopicStatus.NEEDS_REVISION
                        }
                        val updatedTopics = r.topics.map { t ->
                            if (t.id == topicId) t.copy(status = status) else t
                        }
                        r.copy(
                            topics = updatedTopics,
                            completedTopicIds = completedSet,
                            inProgressTopicIds = inProgressSet,
                            updatedAtMillis = System.currentTimeMillis()
                        ).withRecomputedStatuses()
                    }
                    curr.copy(
                        isSubmittingTopicAssessment = false,
                        assessmentResult = res,
                        activeRoadmap = updatedRoadmap ?: curr.activeRoadmap,
                        savedRoadmaps = if (updatedRoadmap != null) {
                            curr.savedRoadmaps.map { if (it.id == updatedRoadmap.id) updatedRoadmap else it }
                        } else curr.savedRoadmaps
                    )
                }
                onEvaluated(res)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingTopicAssessment = false,
                        errorMessage = err.localizedMessage ?: "Failed to evaluate assessment"
                    )
                }
            }
        }
    }

    fun submitTopicQuizAnswer(selectedIndex: Int) {
        val quiz = _uiState.value.activeTopicContent?.practiceQuiz ?: return
        val isCorrect = selectedIndex == quiz.correctOptionIndex
        _uiState.update {
            it.copy(
                topicQuizSelectedIndex = selectedIndex,
                topicQuizAnswerCorrect = isCorrect,
                topicQuizExplanation = quiz.explanation
            )
        }
    }

    fun completeTopic(
        roadmapId: String,
        topicId: String,
        quizPassed: Boolean = true,
        onCompleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompletingTopic = true, errorMessage = null) }
            repository.completeTopic(roadmapId, topicId, quizPassed)
                .onSuccess { updatedRoadmap ->
                    _uiState.update { state ->
                        state.copy(
                            isCompletingTopic = false,
                            activeRoadmap = updatedRoadmap,
                            savedRoadmaps = state.savedRoadmaps.map { if (it.id == updatedRoadmap.id) updatedRoadmap else it }
                        )
                    }
                    onCompleted()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCompletingTopic = false,
                            errorMessage = error.localizedMessage ?: "Failed to save topic completion"
                        )
                    }
                }
        }
    }

    fun clearActiveTopicContent() {
        _uiState.update {
            it.copy(
                activeTopicContent = null,
                topicQuizSelectedIndex = null,
                topicQuizAnswerCorrect = null,
                topicQuizExplanation = null,
                hintsUnlockedLevel = 0,
                solutionRevealed = false,
                assessmentAnswers = emptyMap(),
                assessmentResult = null,
                topicReviewMessage = null
            )
        }
    }

    fun toggleTopicCompleted(topicId: String) {
        val roadmap = _uiState.value.activeRoadmap ?: return
        val isAlreadyCompleted = roadmap.completedTopicIds.contains(topicId)
        completeTopic(roadmap.id, topicId, quizPassed = !isAlreadyCompleted)
    }

    fun setTopicInProgress(topicId: String) {
        val roadmap = _uiState.value.activeRoadmap ?: return
        val currentCompleted = roadmap.completedTopicIds.toMutableSet().apply { remove(topicId) }
        val currentInProgress = roadmap.inProgressTopicIds.toMutableSet().apply { add(topicId) }

        val updated = roadmap.copy(
            completedTopicIds = currentCompleted,
            inProgressTopicIds = currentInProgress,
            updatedAtMillis = System.currentTimeMillis()
        ).withRecomputedStatuses()

        updateRoadmapLocallyAndPersist(updated)
    }

    fun skipTopic(topicId: String) {
        val roadmap = _uiState.value.activeRoadmap ?: return
        val updatedTopics = roadmap.topics.map {
            if (it.id == topicId) it.copy(status = PersonalizedTopicStatus.SKIPPED) else it
        }
        val updated = roadmap.copy(
            topics = updatedTopics,
            updatedAtMillis = System.currentTimeMillis()
        )
        updateRoadmapLocallyAndPersist(updated)
    }

    fun resetTopicStatus(topicId: String) {
        val roadmap = _uiState.value.activeRoadmap ?: return
        val currentCompleted = roadmap.completedTopicIds.toMutableSet().apply { remove(topicId) }
        val currentInProgress = roadmap.inProgressTopicIds.toMutableSet().apply { remove(topicId) }
        val updated = roadmap.copy(
            completedTopicIds = currentCompleted,
            inProgressTopicIds = currentInProgress,
            updatedAtMillis = System.currentTimeMillis()
        ).withRecomputedStatuses()
        updateRoadmapLocallyAndPersist(updated)
    }

    private fun updateRoadmapLocallyAndPersist(updated: PersonalizedRoadmapDetail) {
        _uiState.update { state ->
            state.copy(
                activeRoadmap = updated,
                savedRoadmaps = state.savedRoadmaps.map { if (it.id == updated.id) updated else it }
            )
        }
        viewModelScope.launch {
            repository.savePersonalizedRoadmap(updated)
        }
    }

    fun renameRoadmap(roadmapId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renamePersonalizedRoadmap(roadmapId, newTitle)
                .onSuccess { renamed ->
                    _uiState.update { state ->
                        state.copy(
                            activeRoadmap = if (state.activeRoadmap?.id == roadmapId) renamed else state.activeRoadmap,
                            savedRoadmaps = state.savedRoadmaps.map { if (it.id == roadmapId) renamed else it }
                        )
                    }
                }
        }
    }

    fun deleteRoadmap(roadmapId: String) {
        viewModelScope.launch {
            repository.deletePersonalizedRoadmap(roadmapId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            activeRoadmap = if (state.activeRoadmap?.id == roadmapId) null else state.activeRoadmap,
                            savedRoadmaps = state.savedRoadmaps.filter { it.id != roadmapId }
                        )
                    }
                }
        }
    }

    fun selectTopic(topic: PersonalizedRoadmapTopic?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectRoadmapWs()
    }
}
