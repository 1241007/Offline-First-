package com.offline_First.data.repository

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.domain.model.UserMemoryItem
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    fun observeConnectionMode(): Flow<ConnectionMode>
    suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit>

    fun observeExplanationMode(): Flow<ExplanationMode>
    suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit>

    fun observeOfflineAIStatus(): Flow<OfflineAIStatus>
    fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress>

    suspend fun startOfflineAIDownload(): Result<Unit>
    suspend fun deleteOfflineAI(): Result<Unit>

    fun observeChatSessions(): Flow<List<ChatSession>>
    fun observeCurrentSession(): Flow<ChatSession?>

    suspend fun createNewChat(): Result<ChatSession>
    suspend fun selectChat(sessionId: String): Result<Unit>
    suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit>
    suspend fun deleteChat(sessionId: String): Result<Unit>

    suspend fun sendMessage(prompt: String): Result<ChatMessage>

    // --- Phase 2: Streaming, Branching, Memory, Pagination, Drafts ---
    fun streamMessage(
        prompt: String,
        parentId: String? = null,
        clientMessageId: String? = null
    ): Flow<String>

    suspend fun stopGeneration(): Result<Unit>
    fun regenerateLastResponse(): Flow<String>
    fun editMessageAndRegenerate(messageId: String, newContent: String): Flow<String>

    suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit>
    suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit>
    suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit>

    suspend fun loadMoreMessages(sessionId: String, beforeTimestamp: Long? = null, limit: Int = 30): Result<List<ChatMessage>>
    suspend fun loadMoreConversations(beforeCursor: Long? = null, limit: Int = 20): Result<List<ChatSession>>

    suspend fun getMemories(): Result<List<UserMemoryItem>>
    suspend fun deleteMemory(memoryId: String): Result<Unit>
    suspend fun syncOfflineData(): Result<Unit>
}
