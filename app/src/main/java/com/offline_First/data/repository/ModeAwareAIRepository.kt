package com.offline_First.data.repository

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/** Routes chat operations to the local GGUF repository only while Offline is selected. */
class ModeAwareAIRepository(
    private val offline: LocalAIRepository,
    private val online: OnlineAIRepository
) : AIRepository {
    private fun active(): AIRepository = when (offline.currentConnectionMode()) {
        ConnectionMode.OFFLINE -> offline
        ConnectionMode.ONLINE -> online
    }

    val onlineErrorFlow: Flow<String?> get() = online.errorFlow

    override fun observeConnectionMode(): Flow<ConnectionMode> = offline.observeConnectionMode()
    override suspend fun setConnectionMode(mode: ConnectionMode) = offline.setConnectionMode(mode)
    override fun observeExplanationMode(): Flow<ExplanationMode> = offline.observeExplanationMode()
    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        offline.setExplanationMode(mode).getOrThrow()
        return online.setExplanationMode(mode)
    }

    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> = offline.observeOfflineAIStatus()
    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> = offline.observeDownloadProgress()
    override suspend fun startOfflineAIDownload() = offline.startOfflineAIDownload()
    override suspend fun deleteOfflineAI() = offline.deleteOfflineAI()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChatSessions(): Flow<List<ChatSession>> =
        offline.observeConnectionMode().flatMapLatest { mode -> repositoryFor(mode).observeChatSessions() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentSession(): Flow<ChatSession?> =
        offline.observeConnectionMode().flatMapLatest { mode -> repositoryFor(mode).observeCurrentSession() }

    override suspend fun createNewChat() = active().createNewChat()
    override suspend fun selectChat(sessionId: String) = active().selectChat(sessionId)
    override suspend fun renameChat(sessionId: String, newTitle: String) = active().renameChat(sessionId, newTitle)
    override suspend fun deleteChat(sessionId: String) = active().deleteChat(sessionId)
    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = active().sendMessage(prompt)

    suspend fun refreshOnlineConversations() {
        if (offline.currentConnectionMode() == ConnectionMode.ONLINE) online.refreshConversations()
    }

    private fun repositoryFor(mode: ConnectionMode): AIRepository = when (mode) {
        ConnectionMode.OFFLINE -> offline
        ConnectionMode.ONLINE -> online
    }
}
