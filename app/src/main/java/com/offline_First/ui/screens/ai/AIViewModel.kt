package com.offline_First.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIStatus
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 5 Main UI States/Screens in EduNova AI:
 * - LANDING: Screen 1 - AI Landing Page
 * - CHAT: Screen 4 - Clean Chat Interface
 * - SETTINGS: Screen 2 - AI Settings
 * - DOWNLOAD_STATE: Screen 3 - Offline AI Download State
 *
 * Screen 5 (Chat History) is presented via a Material 3 Drawer inside the chat interface.
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
    val downloadSize: String = "1.8 GB",
    val estimatedTimeRemaining: String = "~2 minutes remaining",
    val downloadStage: String = "Downloading...",
    val sessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val isDrawerOpen: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isGeneratingResponse: Boolean = false,
    val draftMessage: String = "",
    val activeToolName: String? = null,
    val showToolsSheet: Boolean = false,
    val showAttachmentsSheet: Boolean = false,
    val renameTargetSessionId: String? = null,
    val renameDraftText: String = "",
    val previousScreen: AIScreen = AIScreen.LANDING
)

class AIViewModel(
    private val aiRepository: AIRepository = LocalAIRepository(),
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryFlows()
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

        viewModelScope.launch(coroutineContext) {
            aiRepository.observeCurrentSession().collect { session ->
                _uiState.value = _uiState.value.copy(currentSession = session)
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

    fun setConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.setConnectionMode(mode)
        }
    }

    fun setExplanationMode(mode: ExplanationMode) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.setExplanationMode(mode)
        }
    }

    fun startOfflineDownload(navigateToDownloadState: Boolean = false) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.startOfflineAIDownload()
            if (navigateToDownloadState) {
                setScreen(AIScreen.DOWNLOAD_STATE)
            }
        }
    }

    fun showDeleteConfirm(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = show)
    }

    fun confirmDeleteOfflineAI() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.deleteOfflineAI()
            _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
        }
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isDrawerOpen = isOpen)
    }

    fun createNewChat() {
        viewModelScope.launch(coroutineContext) {
            aiRepository.createNewChat()
            _uiState.value = _uiState.value.copy(
                isDrawerOpen = false,
                currentScreen = AIScreen.CHAT,
                activeToolName = null
            )
        }
    }

    fun selectChat(sessionId: String) {
        viewModelScope.launch(coroutineContext) {
            aiRepository.selectChat(sessionId)
            _uiState.value = _uiState.value.copy(
                isDrawerOpen = false,
                currentScreen = AIScreen.CHAT,
                activeToolName = null
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
            aiRepository.renameChat(targetId, newTitle)
            _uiState.value = _uiState.value.copy(
                renameTargetSessionId = null,
                renameDraftText = ""
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
            aiRepository.deleteChat(sessionId)
        }
    }

    fun onDraftMessageChanged(draft: String) {
        _uiState.value = _uiState.value.copy(draftMessage = draft)
    }

    fun sendMessage(promptOverride: String? = null) {
        val promptToSend = (promptOverride ?: _uiState.value.draftMessage).trim()
        if (promptToSend.isBlank()) return

        _uiState.value = _uiState.value.copy(
            draftMessage = "",
            isGeneratingResponse = true,
            currentScreen = AIScreen.CHAT,
            activeToolName = null
        )

        viewModelScope.launch(coroutineContext) {
            aiRepository.sendMessage(promptToSend)
            _uiState.value = _uiState.value.copy(isGeneratingResponse = false)
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
}
