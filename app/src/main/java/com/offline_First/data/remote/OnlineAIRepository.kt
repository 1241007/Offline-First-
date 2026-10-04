package com.offline_First.data.remote

import android.content.Context
import android.util.Log
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

private const val TAG = "OnlineAIRepository"

/**
 * Online implementation of AIRepository.
 * Connects to FastAPI backend for chat and Gemini AI responses.
 * Uses local SQLite as a cache layer.
 * Delegates settings management (ConnectionMode, ExplanationMode, OfflineAI)
 * to LocalAIRepository to preserve existing behavior.
 */
class OnlineAIRepository(
    private val context: Context,
    private val delegate: LocalAIRepository = LocalAIRepository()
) : AIRepository {

    private val cacheDb by lazy { ChatCacheDatabase(context) }

    private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _currentSessionFlow = MutableStateFlow<ChatSession?>(null)
    private val _errorFlow = MutableStateFlow<String?>(null)
    
    private var _currentExplanationMode = ExplanationMode.GENERAL

    val errorFlow: Flow<String?> = _errorFlow.asStateFlow()

    init {
        // Load cached conversations immediately
        val cached = cacheDb.getAllConversations()
        _sessionsFlow.value = cached
    }

    // --- Settings: delegate to LocalAIRepository ---

    override fun observeConnectionMode(): Flow<ConnectionMode> = delegate.observeConnectionMode()
    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> = delegate.setConnectionMode(mode)
    override fun observeExplanationMode(): Flow<ExplanationMode> = delegate.observeExplanationMode()
    
    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        _currentExplanationMode = mode
        return delegate.setExplanationMode(mode)
    }
    
    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> = delegate.observeOfflineAIStatus()
    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> = delegate.observeDownloadProgress()
    override suspend fun startOfflineAIDownload(): Result<Unit> = delegate.startOfflineAIDownload()
    override suspend fun deleteOfflineAI(): Result<Unit> = delegate.deleteOfflineAI()

    // --- Session management ---

    override fun observeChatSessions(): Flow<List<ChatSession>> = _sessionsFlow.asStateFlow()
    override fun observeCurrentSession(): Flow<ChatSession?> = _currentSessionFlow.asStateFlow()

    override suspend fun createNewChat(): Result<ChatSession> = withContext(Dispatchers.IO) {
        _errorFlow.value = null
        val remoteResult = ChatApiClient.createConversation()
        remoteResult.fold(
            onSuccess = { dto ->
                val session = ChatSession(
                    id = dto.id,
                    title = dto.title,
                    lastUpdated = parseIsoToMillis(dto.updatedAt)
                )
                cacheDb.upsertConversation(
                    id = dto.id,
                    title = dto.title,
                    createdAt = parseIsoToMillis(dto.createdAt),
                    updatedAt = parseIsoToMillis(dto.updatedAt)
                )
                _sessionsFlow.value = listOf(session) + _sessionsFlow.value
                    .filter { it.id != session.id }
                _currentSessionFlow.value = session
                Result.success(session)
            },
            onFailure = { error ->
                Log.e(TAG, "createNewChat failed: ${error.message}")
                // Fallback: create local-only session with a UUID
                val localId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val session = ChatSession(id = localId, title = "New Conversation", lastUpdated = now)
                cacheDb.upsertConversation(localId, "New Conversation", now, now)
                _sessionsFlow.value = listOf(session) + _sessionsFlow.value
                _currentSessionFlow.value = session
                _errorFlow.value = "Could not connect to server. Working offline."
                Result.success(session)
            }
        )
    }

    override suspend fun selectChat(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        _errorFlow.value = null
        // First show cached data
        val cached = cacheDb.getConversationWithMessages(sessionId)
        if (cached != null) {
            _currentSessionFlow.value = cached
        }
        // Then fetch fresh from backend
        ChatApiClient.getConversation(sessionId).fold(
            onSuccess = { detail ->
                val messages = detail.messages.map { msg ->
                    ChatMessage(
                        id = msg.id,
                        text = msg.content,
                        fromUser = msg.role == "user",
                        timestamp = parseIsoToMillis(msg.createdAt)
                    )
                }
                val session = ChatSession(
                    id = detail.id,
                    title = detail.title,
                    lastUpdated = parseIsoToMillis(detail.updatedAt),
                    messages = messages
                )
                // Update cache
                cacheDb.upsertConversation(
                    detail.id, detail.title,
                    parseIsoToMillis(detail.createdAt),
                    parseIsoToMillis(detail.updatedAt)
                )
                val messagesWithRoles = messages.map { msg ->
                    val role = if (msg.fromUser) "user" else "assistant"
                    Pair(msg, role)
                }
                cacheDb.upsertMessages(messagesWithRoles, detail.id)
                _currentSessionFlow.value = session
                // Update session in list
                _sessionsFlow.value = _sessionsFlow.value.map {
                    if (it.id == session.id) session.copy(messages = emptyList()) else it
                }
            },
            onFailure = { error ->
                Log.w(TAG, "selectChat failed to fetch from backend: ${error.message}")
                // Cached data already shown above
            }
        )
        Result.success(Unit)
    }

    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> {
        // Local rename only (backend rename not in scope)
        cacheDb.updateConversationTitle(sessionId, newTitle)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(title = newTitle) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(title = newTitle)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteChat(sessionId: String): Result<Unit> {
        cacheDb.deleteConversation(sessionId)
        val remaining = _sessionsFlow.value.filter { it.id != sessionId }
        _sessionsFlow.value = remaining
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = null
        }
        return Result.success(Unit)
    }

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        _errorFlow.value = null
        val current = _currentSessionFlow.value ?: createNewChat().getOrThrow()

        val modeStr = when (_currentExplanationMode) {
            ExplanationMode.TEACHER -> "teacher"
            ExplanationMode.EXPLAINABLE -> "explainable"
            else -> "general"
        }

        // Optimistically add user message to UI
        val tempUserMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = prompt,
            fromUser = true,
            timestamp = System.currentTimeMillis()
        )
        val optimisticSession = current.copy(
            messages = current.messages + tempUserMsg
        )
        _currentSessionFlow.value = optimisticSession

        // Call backend
        val result = ChatApiClient.sendMessage(
            conversationId = current.id,
            content = prompt,
            explanationMode = modeStr
        )

        result.fold(
            onSuccess = { response ->
                val userMsg = ChatMessage(
                    id = response.userMessage.id,
                    text = response.userMessage.content,
                    fromUser = true,
                    timestamp = parseIsoToMillis(response.userMessage.createdAt)
                )
                val aiMsg = ChatMessage(
                    id = response.assistantMessage.id,
                    text = response.assistantMessage.content,
                    fromUser = false,
                    timestamp = parseIsoToMillis(response.assistantMessage.createdAt)
                )

                // Persist to SQLite cache in single transaction
                cacheDb.upsertMessages(
                    listOf(
                        Pair(userMsg, "user"),
                        Pair(aiMsg, "assistant")
                    ),
                    current.id
                )

                // Replace optimistic message with confirmed messages, add AI response
                val finalMessages = current.messages + userMsg + aiMsg
                val now = System.currentTimeMillis()
                val updatedSession = current.copy(
                    messages = finalMessages,
                    lastUpdated = now
                )

                // Update title if backend updated it (if it's no longer "New Conversation")
                // Fetch updated conversation to get server title
                val titleToUse = if (current.messages.isEmpty()) {
                    // First message: title was probably updated by server
                    prompt.take(50).trim()
                } else {
                    current.title
                }

                cacheDb.upsertConversation(current.id, titleToUse, now, now)

                val finalSession = updatedSession.copy(title = titleToUse)
                _currentSessionFlow.value = finalSession
                _sessionsFlow.value = listOf(finalSession.copy(messages = emptyList())) +
                    _sessionsFlow.value.filter { it.id != finalSession.id }

                Result.success(aiMsg)
            },
            onFailure = { error ->
                Log.e(TAG, "sendMessage failed: ${error.message}")
                // Revert optimistic update — keep user message, show error
                _errorFlow.value = "Unable to connect. Please check your internet connection and try again."
                // Keep user message in UI so they don't lose it, but flag the session
                Result.failure(error)
            }
        )
    }

    fun clearError() {
        _errorFlow.value = null
    }

    suspend fun refreshConversations() = withContext(Dispatchers.IO) {
        ChatApiClient.listConversations().fold(
            onSuccess = { dtos ->
                val sessions = dtos.map { dto ->
                    ChatSession(
                        id = dto.id,
                        title = dto.title,
                        lastUpdated = parseIsoToMillis(dto.updatedAt)
                    ).also { session ->
                        cacheDb.upsertConversation(
                            dto.id, dto.title,
                            parseIsoToMillis(dto.updatedAt),
                            parseIsoToMillis(dto.updatedAt)
                        )
                    }
                }
                _sessionsFlow.value = sessions
            },
            onFailure = { error ->
                Log.w(TAG, "refreshConversations failed: ${error.message}")
                // Keep cached data
            }
        )
    }

    private fun parseIsoToMillis(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
