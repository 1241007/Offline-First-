package com.offline_First.data.repository

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.domain.model.UserMemoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Routes chat operations to local offline inference or remote FastAPI/Gemini backend.
 * Provides complete cross-mode chat continuity: sessions, messages, drafts, and memory
 * remain single and identical when toggling between ONLINE and OFFLINE modes.
 */
class ModeAwareAIRepository(
    private val offline: LocalAIRepository,
    private val online: OnlineAIRepository
) : AIRepository {

    val onlineErrorFlow: Flow<String?> get() = online.errorFlow

    private fun isOffline(): Boolean = offline.currentConnectionMode() == ConnectionMode.OFFLINE

    override fun observeConnectionMode(): Flow<ConnectionMode> = offline.observeConnectionMode()

    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> {
        val result = offline.setConnectionMode(mode)
        if (mode == ConnectionMode.ONLINE) {
            // Auto-sync any offline generated messages when reconnecting online
            online.syncOfflineData()
        }
        return result
    }

    override fun observeExplanationMode(): Flow<ExplanationMode> = offline.observeExplanationMode()
    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        offline.setExplanationMode(mode).getOrThrow()
        return online.setExplanationMode(mode)
    }

    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> = offline.observeOfflineAIStatus()
    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> = offline.observeDownloadProgress()
    override suspend fun startOfflineAIDownload(): Result<Unit> = offline.startOfflineAIDownload()
    override suspend fun deleteOfflineAI(): Result<Unit> = offline.deleteOfflineAI()

    // Unify on single session source: both modes share the exact same active conversation and history
    override fun observeChatSessions(): Flow<List<ChatSession>> = online.observeChatSessions()
    override fun observeCurrentSession(): Flow<ChatSession?> = online.observeCurrentSession()

    override suspend fun createNewChat(): Result<ChatSession> = online.createNewChat()
    override suspend fun selectChat(sessionId: String): Result<Unit> = online.selectChat(sessionId)
    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> = online.renameChat(sessionId, newTitle)
    override suspend fun deleteChat(sessionId: String): Result<Unit> = online.deleteChat(sessionId)

    // Unified AI/Chat System: all chat features flow through the unified chat layer
    // which delegates LLM inference to OnlineGeminiProvider or OfflineLlamaProvider.
    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = online.sendMessage(prompt)

    override fun streamMessage(
        prompt: String,
        parentId: String?,
        clientMessageId: String?
    ): Flow<String> = online.streamMessage(prompt, parentId, clientMessageId)

    override suspend fun stopGeneration(): Result<Unit> = online.stopGeneration()

    override fun regenerateLastResponse(): Flow<String> = online.regenerateLastResponse()

    override fun editMessageAndRegenerate(messageId: String, newContent: String): Flow<String> =
        online.editMessageAndRegenerate(messageId, newContent)

    override suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit> =
        online.saveDraft(sessionId, draftText)

    override suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit> =
        online.togglePin(sessionId, isPinned)

    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit> =
        online.toggleArchive(sessionId, isArchived)

    override suspend fun loadMoreMessages(
        sessionId: String,
        beforeTimestamp: Long?,
        limit: Int
    ): Result<List<ChatMessage>> = online.loadMoreMessages(sessionId, beforeTimestamp, limit)

    override suspend fun loadMoreConversations(
        beforeCursor: Long?,
        limit: Int
    ): Result<List<ChatSession>> = online.loadMoreConversations(beforeCursor, limit)

    override suspend fun getMemories(): Result<List<UserMemoryItem>> = online.getMemories()

    override suspend fun deleteMemory(memoryId: String): Result<Unit> = online.deleteMemory(memoryId)

    override suspend fun syncOfflineData(): Result<Unit> = online.syncOfflineData()

    suspend fun refreshOnlineConversations() {
        if (!isOffline()) online.refreshConversations()
    }
}
