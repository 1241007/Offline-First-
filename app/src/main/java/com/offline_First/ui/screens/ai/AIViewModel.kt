package com.offline_First.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.domain.model.UserMemoryItem
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Main UI screens in EduNova AI
 */
enum class AIScreen {
    LANDING,
    CHAT,
    SETTINGS,
    DOWNLOAD_STATE
}

data class AIUiState(
    val currentScreen: AIScreen = AIScreen.LANDING,
    val connectionMode: ConnectionMode = ConnectionMode.ONLINE,
    val explanationMode: ExplanationMode = ExplanationMode.GENERAL,
    val offlineAIStatus: OfflineAIStatus = OfflineAIStatus.NOT_DOWNLOADED,
    val downloadProgress: Int = 0,
    val downloadSize: String = "",
    val estimatedTimeRemaining: String = "",
    val downloadStage: String = "",
    val sessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val isDrawerOpen: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isGeneratingResponse: Boolean = false,
    val draftMessage: String = "",
    val activeToolName: String? = null,
    val showToolsSheet: Boolean = false,
    val showAttachmentsSheet: Boolean = false,
    val showMemoriesSheet: Boolean = false,
    val memories: List<UserMemoryItem> = emptyList(),
    val isLoadingMemories: Boolean = false,
    val editingMessageId: String? = null,
    val editDraftText: String = "",
    val renameTargetSessionId: String? = null,
    val renameDraftText: String = "",
    val previousScreen: AIScreen = AIScreen.LANDING,
    val errorMessage: String? = null
)

class AIViewModel(
    private val aiRepository: AIRepository = AppContainer.aiRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    companion object {
        fun provideFactory(application: android.app.Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AIViewModel(
                        aiRepository = com.offline_First.data.AppContainer.aiRepository
                    ) as T
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()

    private var currentStreamingJob: Job? = null

    init {
        observeRepositoryFlows()
        loadMemories()
        refreshConversations()
    }

    private fun observeRepositoryFlows() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.observeConnectionMode().collect { mode ->
                _uiState.update { it.copy(connectionMode = mode) }
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeExplanationMode().collect { mode ->
                _uiState.update { it.copy(explanationMode = mode) }
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeOfflineAIStatus().collect { status ->
                _uiState.update { it.copy(offlineAIStatus = status) }
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeDownloadProgress().collect { progressInfo ->
                _uiState.update {
                    it.copy(
                        downloadProgress = progressInfo.progress,
                        downloadSize = progressInfo.downloadSize,
                        estimatedTimeRemaining = progressInfo.estimatedTimeRemaining,
                        downloadStage = progressInfo.stage
                    )
                }
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeChatSessions().collect { sessionList ->
                _uiState.update { it.copy(sessions = sessionList) }
            }
        }

        var lastSessionId: String? = null
        viewModelScope.launch(coroutineContext) {
            aiRepository.observeCurrentSession().collect { session ->
                val sessionIdChanged = session?.id != lastSessionId
                lastSessionId = session?.id
                _uiState.update { current ->
                    current.copy(
                        currentSession = session,
                        draftMessage = if (sessionIdChanged) {
                            session?.draftText.orEmpty()
                        } else {
                            current.draftMessage
                        }
                    )
                }
            }
        }

        if (aiRepository is com.offline_First.data.repository.ModeAwareAIRepository) {
            viewModelScope.launch(coroutineContext) {
                aiRepository.onlineErrorFlow.collect { error ->
                    if (error != null) {
                        _uiState.update { it.copy(errorMessage = error) }
                    }
                }
            }
        }
    }

    fun setScreen(screen: AIScreen) {
        _uiState.update { state ->
            val prev = state.currentScreen
            val newPrevious = if (prev == AIScreen.CHAT || prev == AIScreen.LANDING) {
                prev
            } else {
                state.previousScreen
            }
            state.copy(
                currentScreen = screen,
                previousScreen = newPrevious
            )
        }
    }

    fun openChat() = setScreen(AIScreen.CHAT)
    fun openSettings() = setScreen(AIScreen.SETTINGS)
    fun openDownloadState() = setScreen(AIScreen.DOWNLOAD_STATE)

    fun goBackFromSettings() = setScreen(_uiState.value.previousScreen)
    fun goBackFromDownloadState() = setScreen(AIScreen.SETTINGS)

    fun setConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.setConnectionMode(mode).onFailure(::reportError)
        }
    }

    fun setExplanationMode(mode: ExplanationMode) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.setExplanationMode(mode).onFailure(::reportError)
        }
    }

    fun startOfflineDownload(navigateToDownloadState: Boolean = false) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.startOfflineAIDownload().fold(
                onSuccess = {
                    if (navigateToDownloadState) openDownloadState()
                },
                onFailure = ::reportError
            )
        }
    }

    fun showDeleteConfirm(show: Boolean) {
        setDeleteConfirmDialogVisible(show)
    }

    fun confirmDeleteOfflineAI() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.deleteOfflineAI().fold(
                onSuccess = {
                    showDeleteConfirm(false)
                },
                onFailure = ::reportError
            )
        }
    }

    fun deleteOfflineModel() {
        confirmDeleteOfflineAI()
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = isOpen) }
    }

    fun setDeleteConfirmDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = visible) }
    }

    fun createNewChat() {
        currentStreamingJob?.cancel()
        currentStreamingJob = null
        viewModelScope.launch(coroutineContext) {
            aiRepository.createNewChat().fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            isDrawerOpen = false,
                            currentScreen = AIScreen.CHAT,
                            currentSession = session,
                            activeToolName = null,
                            draftMessage = "",
                            isGeneratingResponse = false
                        )
                    }
                },
                onFailure = ::reportError
            )
        }
    }

    fun selectChat(sessionId: String) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.selectChat(sessionId).fold(
                onSuccess = {
                    val session = _uiState.value.sessions.find { it.id == sessionId }
                    _uiState.update { current ->
                        current.copy(
                            isDrawerOpen = false,
                            currentScreen = AIScreen.CHAT,
                            currentSession = if (current.currentSession?.id == sessionId) current.currentSession else session?.copy(messages = emptyList()),
                            activeToolName = null,
                            draftMessage = session?.draftText.orEmpty(),
                            isGeneratingResponse = false
                        )
                    }
                },
                onFailure = ::reportError
            )
        }
    }

    fun startRenameChat(sessionId: String, currentTitle: String) {
        _uiState.update {
            it.copy(
                renameTargetSessionId = sessionId,
                renameDraftText = currentTitle
            )
        }
    }

    fun onRenameDraftChanged(text: String) {
        _uiState.update { it.copy(renameDraftText = text) }
    }

    fun confirmRenameChat() {
        val targetId = _uiState.value.renameTargetSessionId ?: return
        val newTitle = _uiState.value.renameDraftText
        viewModelScope.launch(coroutineContext) {
            aiRepository.renameChat(targetId, newTitle).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            renameTargetSessionId = null,
                            renameDraftText = ""
                        )
                    }
                },
                onFailure = ::reportError
            )
        }
    }

    fun cancelRenameChat() {
        _uiState.update {
            it.copy(
                renameTargetSessionId = null,
                renameDraftText = ""
            )
        }
    }

    fun deleteChat(sessionId: String) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.deleteChat(sessionId).onFailure(::reportError)
        }
    }

    fun togglePin(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.togglePin(sessionId, isPinned).onFailure(::reportError)
        }
    }

    fun toggleArchive(sessionId: String, isArchived: Boolean) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.toggleArchive(sessionId, isArchived).onFailure(::reportError)
        }
    }

    fun onDraftMessageChanged(draft: String) {
        _uiState.update { it.copy(draftMessage = draft) }
        val currentId = _uiState.value.currentSession?.id
        if (currentId != null) {
            viewModelScope.launch(coroutineContext) {
                aiRepository.saveDraft(currentId, draft)
            }
        }
    }

    fun sendMessage(
        promptOverride: String? = null,
        sendTimestamp: Long = com.offline_First.data.local.MonotonicClock.elapsedMillis()
    ) {
        if (_uiState.value.isGeneratingResponse) return
        val promptToSend = (promptOverride ?: _uiState.value.draftMessage).trim()
        if (promptToSend.isBlank()) return

        android.util.Log.i("AIViewModel", "OFFLINE_LATENCY: send_clicked (t=${sendTimestamp}ms)")

        _uiState.update {
            it.copy(
                draftMessage = "",
                isGeneratingResponse = true,
                currentScreen = AIScreen.CHAT,
                activeToolName = null,
                errorMessage = null
            )
        }

        // Clear persisted draft
        _uiState.value.currentSession?.id?.let { convId ->
            viewModelScope.launch(coroutineContext) { aiRepository.saveDraft(convId, "") }
        }

        currentStreamingJob = viewModelScope.launch(coroutineContext) {
            try {
                var firstTokenUi = false
                aiRepository.streamMessage(promptToSend).collect {
                    if (!firstTokenUi) {
                        firstTokenUi = true
                        val uiReceived = com.offline_First.data.local.MonotonicClock.elapsedMillis()
                        android.util.Log.i("AIViewModel", "OFFLINE_LATENCY: ui_received, +${uiReceived - sendTimestamp}ms from send_clicked")
                    }
                    // Chunks update currentSession in repository
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Generation failed") }
                }
            } finally {
                _uiState.update { it.copy(isGeneratingResponse = false) }
                loadMemories()
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch(coroutineContext) {
            currentStreamingJob?.cancel()
            currentStreamingJob = null
            aiRepository.stopGeneration()
            _uiState.update { it.copy(isGeneratingResponse = false) }
        }
    }

    fun regenerateLastResponse() {
        if (_uiState.value.isGeneratingResponse) return
        _uiState.update { it.copy(isGeneratingResponse = true, errorMessage = null) }

        currentStreamingJob = viewModelScope.launch(coroutineContext) {
            try {
                aiRepository.regenerateLastResponse().collect {}
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Regeneration failed") }
                }
            } finally {
                _uiState.update { it.copy(isGeneratingResponse = false) }
            }
        }
    }

    fun startEditMessage(messageId: String, currentText: String) {
        _uiState.update {
            it.copy(
                editingMessageId = messageId,
                editDraftText = currentText
            )
        }
    }

    fun onEditDraftChanged(newText: String) {
        _uiState.update { it.copy(editDraftText = newText) }
    }

    fun cancelEditMessage() {
        _uiState.update { it.copy(editingMessageId = null, editDraftText = "") }
    }

    fun confirmEditAndRegenerate() {
        val msgId = _uiState.value.editingMessageId ?: return
        val newText = _uiState.value.editDraftText.trim()
        if (newText.isBlank()) return

        _uiState.update {
            it.copy(
                editingMessageId = null,
                editDraftText = "",
                isGeneratingResponse = true,
                errorMessage = null
            )
        }

        currentStreamingJob = viewModelScope.launch(coroutineContext) {
            try {
                aiRepository.editMessageAndRegenerate(msgId, newText).collect {}
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Edit failed") }
                }
            } finally {
                _uiState.update { it.copy(isGeneratingResponse = false) }
            }
        }
    }

    fun loadMoreMessages() {
        val current = _uiState.value.currentSession ?: return
        val oldestTimestamp = current.messages.firstOrNull()?.timestamp ?: return
        viewModelScope.launch(coroutineContext) {
            aiRepository.loadMoreMessages(current.id, beforeTimestamp = oldestTimestamp)
        }
    }

    fun loadMoreConversations() {
        val oldestUpdated = _uiState.value.sessions.lastOrNull()?.lastUpdated
        viewModelScope.launch(coroutineContext) {
            aiRepository.loadMoreConversations(beforeCursor = oldestUpdated)
        }
    }

    fun loadMemories() {
        viewModelScope.launch(coroutineContext) {
            _uiState.update { it.copy(isLoadingMemories = true) }
            aiRepository.getMemories().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            memories = items,
                            isLoadingMemories = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMemories = false) }
                }
            )
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.deleteMemory(memoryId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            memories = state.memories.filter { it.id != memoryId }
                        )
                    }
                },
                onFailure = ::reportError
            )
        }
    }

    fun setMemoriesSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showMemoriesSheet = visible) }
        if (visible) loadMemories()
    }

    fun syncOfflineData() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.syncOfflineData().onFailure(::reportError)
        }
    }

    fun refreshConversations() {
        val repo = aiRepository
        if (repo is com.offline_First.data.repository.ModeAwareAIRepository) {
            viewModelScope.launch(coroutineContext) {
                repo.refreshOnlineConversations()
            }
        }
    }

    fun openTool(toolName: String) {
        _uiState.update {
            it.copy(
                activeToolName = toolName,
                showToolsSheet = false
            )
        }
    }

    fun closeActiveTool() {
        _uiState.update { it.copy(activeToolName = null) }
    }

    fun setToolsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showToolsSheet = visible) }
    }

    fun setAttachmentsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showAttachmentsSheet = visible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun reportError(error: Throwable) {
        _uiState.update {
            it.copy(
                errorMessage = error.localizedMessage ?: "The request could not be completed."
            )
        }
    }
}
