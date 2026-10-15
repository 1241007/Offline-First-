package com.offline_First.ui.screens.ai

import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIViewModelTest {

    @Test
    fun initialStateHasExpectedDefaults() = runBlocking {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertEquals(AIScreen.LANDING, state.currentScreen)
        assertTrue(state.sessions.isEmpty())
        assertFalse(state.isDrawerOpen)
        assertFalse(state.showDeleteConfirmDialog)
    }

    @Test
    fun switchingScreenUpdatesState() = runBlocking {
        val viewModel = newViewModel()

        viewModel.setScreen(AIScreen.CHAT)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.DOWNLOAD_STATE)
        assertEquals(AIScreen.DOWNLOAD_STATE, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun switchingModesUpdatesUiState() = runBlocking {
        val viewModel = newViewModel()

        viewModel.setConnectionMode(ConnectionMode.OFFLINE)
        assertEquals(ConnectionMode.OFFLINE, viewModel.uiState.value.connectionMode)

        viewModel.setExplanationMode(ExplanationMode.TEACHER)
        assertEquals(ExplanationMode.TEACHER, viewModel.uiState.value.explanationMode)
    }

    @Test
    fun deleteOfflineAIConfirmationLifecycle() = runBlocking {
        val viewModel = newViewModel()

        viewModel.showDeleteConfirm(true)
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)

        viewModel.confirmDeleteOfflineAI()
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Offline AI") == true)
    }

    @Test
    fun backNavigationNeverEntersDownloadStateLoop() = runBlocking {
        val viewModel = newViewModel()

        // User starts at CHAT
        viewModel.setScreen(AIScreen.CHAT)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.currentScreen)
        assertEquals(AIScreen.LANDING, viewModel.uiState.value.previousScreen)

        // User opens SETTINGS from CHAT
        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)

        // User navigates into DOWNLOAD_STATE from SETTINGS
        viewModel.startOfflineDownload(navigateToDownloadState = true)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Offline AI") == true)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)

        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    // ============================================================
    // TEST 2 — OFFLINE SEND
    // Type "What is DBMS", Send -> input becomes "" AND message appears in chat
    // ============================================================
    @Test
    fun testOfflineSendImmediatelyClearsComposerAndAppearsInChat() = runBlocking {
        val repo = FakeUnifiedAIRepository()
        repo.setConnectionMode(ConnectionMode.OFFLINE)
        val viewModel = AIViewModel(aiRepository = repo, coroutineContext = Dispatchers.Unconfined)

        viewModel.createNewChat()
        viewModel.onDraftMessageChanged("What is DBMS")
        assertEquals("What is DBMS", viewModel.uiState.value.draftMessage)

        viewModel.sendMessage()

        // Input immediately becomes ""
        assertEquals("", viewModel.uiState.value.draftMessage)

        // User message immediately appears in chat
        val current = viewModel.uiState.value.currentSession
        assertTrue(current != null)
        val userMsg = current!!.messages.find { it.fromUser }
        assertTrue(userMsg != null)
        assertEquals("What is DBMS", userMsg!!.text)
    }

    // ============================================================
    // TEST 3 — ONLINE SEND
    // Type "What is DBMS", Send -> input becomes "" AND message appears in chat
    // ============================================================
    @Test
    fun testOnlineSendImmediatelyClearsComposerAndAppearsInChat() = runBlocking {
        val repo = FakeUnifiedAIRepository()
        repo.setConnectionMode(ConnectionMode.ONLINE)
        val viewModel = AIViewModel(aiRepository = repo, coroutineContext = Dispatchers.Unconfined)

        viewModel.createNewChat()
        viewModel.onDraftMessageChanged("What is DBMS")
        assertEquals("What is DBMS", viewModel.uiState.value.draftMessage)

        viewModel.sendMessage()

        // Input immediately becomes ""
        assertEquals("", viewModel.uiState.value.draftMessage)

        // User message immediately appears in chat
        val current = viewModel.uiState.value.currentSession
        assertTrue(current != null)
        val userMsg = current!!.messages.find { it.fromUser }
        assertTrue(userMsg != null)
        assertEquals("What is DBMS", userMsg!!.text)
    }

    // ============================================================
    // TEST 4 — DRAFT
    // Before send: draft_text = "What is DBMS"
    // After send: draft_text = "", message = "What is DBMS"
    // ============================================================
    @Test
    fun testDraftClearsOnSendAndMessagePersistedSeparately() = runBlocking {
        val repo = FakeUnifiedAIRepository()
        val viewModel = AIViewModel(aiRepository = repo, coroutineContext = Dispatchers.Unconfined)

        viewModel.createNewChat()
        val session = viewModel.uiState.value.currentSession!!

        // Before send: draft_text = "What is DBMS"
        viewModel.onDraftMessageChanged("What is DBMS")
        assertEquals("What is DBMS", repo.currentSessionFlow.value?.draftText)

        // After send:
        viewModel.sendMessage()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertEquals("", repo.currentSessionFlow.value?.draftText)
        val messages = repo.currentSessionFlow.value?.messages.orEmpty()
        assertTrue(messages.any { it.fromUser && it.text == "What is DBMS" })
    }

    // ============================================================
    // TEST 5 — MODE PARITY
    // Online and Offline produce identical composer and chat states
    // ============================================================
    @Test
    fun testModeParityBetweenOnlineAndOffline() = runBlocking {
        val prompt = "What is DBMS"

        // Run in ONLINE
        val onlineRepo = FakeUnifiedAIRepository()
        onlineRepo.setConnectionMode(ConnectionMode.ONLINE)
        val onlineVm = AIViewModel(aiRepository = onlineRepo, coroutineContext = Dispatchers.Unconfined)
        onlineVm.createNewChat()
        onlineVm.onDraftMessageChanged(prompt)
        onlineVm.sendMessage()

        // Run in OFFLINE
        val offlineRepo = FakeUnifiedAIRepository()
        offlineRepo.setConnectionMode(ConnectionMode.OFFLINE)
        val offlineVm = AIViewModel(aiRepository = offlineRepo, coroutineContext = Dispatchers.Unconfined)
        offlineVm.createNewChat()
        offlineVm.onDraftMessageChanged(prompt)
        offlineVm.sendMessage()

        // Composer parity
        assertEquals("", onlineVm.uiState.value.draftMessage)
        assertEquals("", offlineVm.uiState.value.draftMessage)

        // Message parity
        val onlineUserMsg = onlineVm.uiState.value.currentSession!!.messages.first { it.fromUser }
        val offlineUserMsg = offlineVm.uiState.value.currentSession!!.messages.first { it.fromUser }
        assertEquals(onlineUserMsg.text, offlineUserMsg.text)

        // Screen parity
        assertEquals(AIScreen.CHAT, onlineVm.uiState.value.currentScreen)
        assertEquals(AIScreen.CHAT, offlineVm.uiState.value.currentScreen)
    }

    // ============================================================
    // TEST 6 — STOP BUTTON
    // Idle: Stop hidden (isGeneratingResponse = false)
    // Generating: Stop visible (isGeneratingResponse = true)
    // Completed: Stop hidden (isGeneratingResponse = false)
    // ============================================================
    @Test
    fun testStopButtonVisibilityStateTransitions() = runBlocking {
        val repo = FakeUnifiedAIRepository(delayEmission = true)
        val viewModel = AIViewModel(aiRepository = repo, coroutineContext = Dispatchers.Unconfined)

        viewModel.createNewChat()

        // 1. IDLE: Stop hidden
        assertFalse(viewModel.uiState.value.isGeneratingResponse)

        // 2. GENERATING: Stop visible
        viewModel.onDraftMessageChanged("What is DBMS")
        viewModel.sendMessage()
        assertTrue(viewModel.uiState.value.isGeneratingResponse)

        // 3. STOP GENERATION: user presses Stop -> Stop hidden
        viewModel.stopGeneration()
        assertFalse(viewModel.uiState.value.isGeneratingResponse)
    }

    private fun newViewModel() = AIViewModel(
        aiRepository = BackendNotConfiguredRepositories(),
        coroutineContext = Dispatchers.Unconfined
    )
}

/**
 * Test implementation of AIRepository verifying unified chat flow.
 */
private class FakeUnifiedAIRepository(
    private val delayEmission: Boolean = false
) : com.offline_First.data.repository.AIRepository {
    private val _connectionMode = kotlinx.coroutines.flow.MutableStateFlow(ConnectionMode.ONLINE)
    private val _explanationMode = kotlinx.coroutines.flow.MutableStateFlow(ExplanationMode.GENERAL)
    private val _sessions = kotlinx.coroutines.flow.MutableStateFlow<List<com.offline_First.domain.model.ChatSession>>(emptyList())
    val currentSessionFlow = kotlinx.coroutines.flow.MutableStateFlow<com.offline_First.domain.model.ChatSession?>(null)

    override fun observeConnectionMode(): kotlinx.coroutines.flow.Flow<ConnectionMode> = _connectionMode
    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> {
        _connectionMode.value = mode
        return Result.success(Unit)
    }

    override fun observeExplanationMode(): kotlinx.coroutines.flow.Flow<ExplanationMode> = _explanationMode
    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        _explanationMode.value = mode
        return Result.success(Unit)
    }

    override fun observeOfflineAIStatus() = kotlinx.coroutines.flow.MutableStateFlow(com.offline_First.domain.model.OfflineAIStatus.READY)
    override fun observeDownloadProgress() = kotlinx.coroutines.flow.MutableStateFlow(com.offline_First.domain.model.OfflineAIDownloadProgress())
    override suspend fun startOfflineAIDownload() = Result.success(Unit)
    override suspend fun deleteOfflineAI() = Result.success(Unit)

    override fun observeChatSessions() = _sessions
    override fun observeCurrentSession() = currentSessionFlow

    override suspend fun createNewChat(): Result<com.offline_First.domain.model.ChatSession> {
        val s = com.offline_First.domain.model.ChatSession(
            id = "test-session-${System.currentTimeMillis()}",
            title = "New Conversation",
            draftText = ""
        )
        currentSessionFlow.value = s
        _sessions.value = listOf(s) + _sessions.value
        return Result.success(s)
    }

    override suspend fun selectChat(sessionId: String): Result<Unit> {
        val found = _sessions.value.find { it.id == sessionId }
        currentSessionFlow.value = found
        return Result.success(Unit)
    }

    override suspend fun renameChat(sessionId: String, newTitle: String) = Result.success(Unit)
    override suspend fun deleteChat(sessionId: String) = Result.success(Unit)
    override suspend fun sendMessage(prompt: String): Result<com.offline_First.domain.model.ChatMessage> =
        Result.success(com.offline_First.domain.model.ChatMessage(text = "AI reply", fromUser = false))

    override fun streamMessage(prompt: String, parentId: String?, clientMessageId: String?): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        val current = currentSessionFlow.value ?: createNewChat().getOrThrow()
        val userMsg = com.offline_First.domain.model.ChatMessage(text = prompt, fromUser = true)
        val updated = current.copy(
            draftText = "",
            messages = current.messages + userMsg
        )
        currentSessionFlow.value = updated
        _sessions.value = _sessions.value.map { if (it.id == updated.id) updated else it }

        if (delayEmission) {
            // Keep stream active to simulate ongoing generation
            kotlinx.coroutines.delay(10_000)
        }
        emit("Token 1")
    }

    override suspend fun stopGeneration(): Result<Unit> = Result.success(Unit)
    override fun regenerateLastResponse() = kotlinx.coroutines.flow.flow { emit("Regen") }
    override fun editMessageAndRegenerate(messageId: String, newContent: String) = kotlinx.coroutines.flow.flow { emit("Edit") }

    override suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit> {
        val curr = currentSessionFlow.value
        if (curr?.id == sessionId) {
            currentSessionFlow.value = curr.copy(draftText = draftText)
        }
        _sessions.value = _sessions.value.map { if (it.id == sessionId) it.copy(draftText = draftText) else it }
        return Result.success(Unit)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean) = Result.success(Unit)
    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean) = Result.success(Unit)
    override suspend fun loadMoreMessages(sessionId: String, beforeTimestamp: Long?, limit: Int) = Result.success(emptyList<com.offline_First.domain.model.ChatMessage>())
    override suspend fun loadMoreConversations(beforeCursor: Long?, limit: Int) = Result.success(emptyList<com.offline_First.domain.model.ChatSession>())
    override suspend fun getMemories() = Result.success(emptyList<com.offline_First.domain.model.UserMemoryItem>())
    override suspend fun deleteMemory(memoryId: String) = Result.success(Unit)
    override suspend fun syncOfflineData() = Result.success(Unit)
}
