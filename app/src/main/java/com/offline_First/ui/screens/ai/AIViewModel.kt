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
    }

    private fun observeRepositoryFlows() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.observeConnectionMode().collect { mode ->
                _uiState.value = _uiState.value.copy(connectionMode = mode)
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeExplanationMode().collect { mode ->
                _uiState.value = _uiState.value.copy(explanationMode = mode)
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeOfflineAIStatus().collect { status ->
                _uiState.value = _uiState.value.copy(offlineAIStatus = status)
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeDownloadProgress().collect { progressInfo ->
                _uiState.value = _uiState.value.copy(
                    downloadProgress = progressInfo.progress,
                    downloadSize = progressInfo.downloadSize,
                    estimatedTimeRemaining = progressInfo.estimatedTimeRemaining,
                    downloadStage = progressInfo.stage
                )
            }
        }

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeChatSessions().collect { sessionList ->
                _uiState.value = _uiState.value.copy(sessions = sessionList)
            }
        }

        var lastSessionId: String? = null
        viewModelScope.launch(coroutineContext) {
            aiRepository.observeCurrentSession().collect { session ->
                val sessionIdChanged = session?.id != lastSessionId
                lastSessionId = session?.id
                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    draftMessage = if (sessionIdChanged) {
                        session?.draftText.orEmpty()
                    } else {
                        _uiState.value.draftMessage
                    }
                )
            }
        }

        if (aiRepository is com.offline_First.data.repository.ModeAwareAIRepository) {
            viewModelScope.launch(coroutineContext) {
                aiRepository.onlineErrorFlow.collect { error ->
                    if (error != null) {
                        _uiState.value = _uiState.value.copy(errorMessage = error)
                    }
                }
            }
        }
    }

    fun setScreen(screen: AIScreen) {
        val prev = _uiState.value.currentScreen
        val newPrevious = if (prev == AIScreen.CHAT || prev == AIScreen.LANDING) {
            prev
        } else {
            _uiState.value.previousScreen
        }
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            previousScreen = newPrevious
        )
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
        _uiState.value = _uiState.value.copy(isDrawerOpen = isOpen)
    }

    fun setDeleteConfirmDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = visible)
    }

    fun createNewChat() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.createNewChat().fold(
                onSuccess = { session ->
                    _uiState.value = _uiState.value.copy(
                        isDrawerOpen = false,
                        currentScreen = AIScreen.CHAT,
                        activeToolName = null,
                        draftMessage = ""
                    )
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
                    _uiState.value = _uiState.value.copy(
                        isDrawerOpen = false,
                        currentScreen = AIScreen.CHAT,
                        activeToolName = null,
                        draftMessage = session?.draftText.orEmpty()
                    )
                },
                onFailure = ::reportError
            )
        }
    }

    fun startRenameChat(sessionId: String, currentTitle: String) {
        _uiState.value = _uiState.value.copy(
            renameTargetSessionId = sessionId,
            renameDraftText = currentTitle
        )
    }

    fun onRenameDraftChanged(text: String) {
        _uiState.value = _uiState.value.copy(renameDraftText = text)
    }

    fun confirmRenameChat() {
        val targetId = _uiState.value.renameTargetSessionId ?: return
        val newTitle = _uiState.value.renameDraftText
        viewModelScope.launch(coroutineContext) {
            aiRepository.renameChat(targetId, newTitle).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        renameTargetSessionId = null,
                        renameDraftText = ""
                    )
                },
                onFailure = ::reportError
            )
        }
    }

    fun cancelRenameChat() {
        _uiState.value = _uiState.value.copy(
            renameTargetSessionId = null,
            renameDraftText = ""
        )
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
        _uiState.value = _uiState.value.copy(draftMessage = draft)
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

        _uiState.value = _uiState.value.copy(
            draftMessage = "",
            isGeneratingResponse = true,
            currentScreen = AIScreen.CHAT,
            activeToolName = null,
            errorMessage = null
        )

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
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Generation failed")
                }
            } finally {
                _uiState.value = _uiState.value.copy(isGeneratingResponse = false)
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch(coroutineContext) {
            currentStreamingJob?.cancel()
            currentStreamingJob = null
            aiRepository.stopGeneration()
            _uiState.value = _uiState.value.copy(isGeneratingResponse = false)
        }
    }

    fun regenerateLastResponse() {
        if (_uiState.value.isGeneratingResponse) return
        _uiState.value = _uiState.value.copy(isGeneratingResponse = true, errorMessage = null)

        currentStreamingJob = viewModelScope.launch(coroutineContext) {
            try {
                aiRepository.regenerateLastResponse().collect {}
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Regeneration failed")
                }
            } finally {
                _uiState.value = _uiState.value.copy(isGeneratingResponse = false)
            }
        }
    }

    fun startEditMessage(messageId: String, currentText: String) {
        _uiState.value = _uiState.value.copy(
            editingMessageId = messageId,
            editDraftText = currentText
        )
    }

    fun onEditDraftChanged(newText: String) {
        _uiState.value = _uiState.value.copy(editDraftText = newText)
    }

    fun cancelEditMessage() {
        _uiState.value = _uiState.value.copy(editingMessageId = null, editDraftText = "")
    }

    fun confirmEditAndRegenerate() {
        val msgId = _uiState.value.editingMessageId ?: return
        val newText = _uiState.value.editDraftText.trim()
        if (newText.isBlank()) return

        _uiState.value = _uiState.value.copy(
            editingMessageId = null,
            editDraftText = "",
            isGeneratingResponse = true,
            errorMessage = null
        )

        currentStreamingJob = viewModelScope.launch(coroutineContext) {
            try {
                aiRepository.editMessageAndRegenerate(msgId, newText).collect {}
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Edit failed")
                }
            } finally {
                _uiState.value = _uiState.value.copy(isGeneratingResponse = false)
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
            _uiState.value = _uiState.value.copy(isLoadingMemories = true)
            aiRepository.getMemories().fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(
                        memories = items,
                        isLoadingMemories = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMemories = false)
                }
            )
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.deleteMemory(memoryId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        memories = _uiState.value.memories.filter { it.id != memoryId }
                    )
                },
                onFailure = ::reportError
            )
        }
    }

    fun setMemoriesSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showMemoriesSheet = visible)
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
        _uiState.value = _uiState.value.copy(
            activeToolName = toolName,
            showToolsSheet = false
        )
    }

    fun closeActiveTool() {
        _uiState.value = _uiState.value.copy(activeToolName = null)
    }

    fun setToolsSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showToolsSheet = visible)
    }

    fun setAttachmentsSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAttachmentsSheet = visible)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun reportError(error: Throwable) {
        _uiState.value = _uiState.value.copy(
            errorMessage = error.localizedMessage ?: "The request could not be completed."
        )
    }
}
