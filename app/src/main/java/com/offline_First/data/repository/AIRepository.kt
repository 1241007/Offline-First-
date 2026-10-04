package com.offline_First.data.repository

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
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
}
