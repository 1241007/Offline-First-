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
import com.offline_First.domain.model.UserMemoryItem
import com.offline_First.data.provider.LLMProvider
import com.offline_First.data.provider.OnlineGeminiProvider
import com.offline_First.data.provider.OfflineLlamaProvider
import com.offline_First.data.local.systemPromptFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

private const val TAG = "OnlineAIRepository"

/**
 * Unified AI/Chat Layer implementation of AIRepository.
 * Provides a single, continuous chat experience across ONLINE and OFFLINE modes.
 * Delegates inference to OnlineGeminiProvider or OfflineLlamaProvider while
 * unifying chat sessions, messages, SQLite persistence, drafts, memories, and sync.
 */
class OnlineAIRepository(
    private val context: Context,
    private val delegate: LocalAIRepository = LocalAIRepository(context),
    private val userIdProvider: () -> String = { "default_user" }
) : AIRepository {

    val cacheDb by lazy { ChatCacheDatabase(context) }

    private val onlineProvider: LLMProvider by lazy { OnlineGeminiProvider() }
    private val offlineProvider: LLMProvider by lazy { OfflineLlamaProvider(delegate) }

    private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _currentSessionFlow = MutableStateFlow<ChatSession?>(null)
    private val _errorFlow = MutableStateFlow<String?>(null)
    private var activeStreamJob: Job? = null

    private var _currentExplanationMode = ExplanationMode.GENERAL

    val errorFlow: Flow<String?> = _errorFlow.asStateFlow()

    private val currentUserId: String
        get() = userIdProvider().ifBlank { "default_user" }

    init {
        // Load cached conversations for the active user
        val cached = cacheDb.getConversations(currentUserId)
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
                    lastUpdated = parseIsoToMillis(dto.updatedAt),
                    isArchived = dto.isArchived,
                    isPinned = dto.isPinned,
                    draftText = dto.draftText
                )
                cacheDb.upsertConversation(
                    id = dto.id,
                    userId = currentUserId,
                    title = dto.title,
                    createdAt = parseIsoToMillis(dto.createdAt),
                    updatedAt = parseIsoToMillis(dto.updatedAt),
                    isArchived = dto.isArchived,
                    isPinned = dto.isPinned,
                    draftText = dto.draftText
                )
                _sessionsFlow.value = listOf(session) + _sessionsFlow.value.filter { it.id != session.id }
                _currentSessionFlow.value = session
                Result.success(session)
            },
            onFailure = { error ->
                Log.e(TAG, "createNewChat failed: ${error.message}")
                val localId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val session = ChatSession(id = localId, title = "New Conversation", lastUpdated = now)
                cacheDb.upsertConversation(localId, currentUserId, "New Conversation", now, now)
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
        val cached = cacheDb.getConversationWithMessages(sessionId, currentUserId)
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
                        timestamp = parseIsoToMillis(msg.createdAt),
                        parentId = msg.parentId,
                        isEdited = msg.isEdited
                    )
                }
                val session = ChatSession(
                    id = detail.id,
                    title = detail.title,
                    lastUpdated = parseIsoToMillis(detail.updatedAt),
                    isArchived = detail.isArchived,
                    isPinned = detail.isPinned,
                    draftText = detail.draftText,
                    messages = messages
                )
                cacheDb.upsertConversation(
                    id = detail.id,
                    userId = currentUserId,
                    title = detail.title,
                    createdAt = parseIsoToMillis(detail.createdAt),
                    updatedAt = parseIsoToMillis(detail.updatedAt),
                    isArchived = detail.isArchived,
                    isPinned = detail.isPinned,
                    draftText = detail.draftText
                )
                val messagesWithRoles = messages.map { msg ->
                    Pair(msg, if (msg.fromUser) "user" else "assistant")
                }
                cacheDb.upsertMessages(messagesWithRoles, detail.id, currentUserId)
                _currentSessionFlow.value = session
                _sessionsFlow.value = _sessionsFlow.value.map {
                    if (it.id == session.id) session.copy(messages = emptyList()) else it
                }
            },
            onFailure = { error ->
                Log.w(TAG, "selectChat failed to fetch from backend: ${error.message}")
            }
        )
        Result.success(Unit)
    }

    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.updateConversationTitle(sessionId, currentUserId, newTitle)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(title = newTitle) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(title = newTitle)
        }
        ChatApiClient.updateConversation(sessionId, title = newTitle)
        Result.success(Unit)
    }

    override suspend fun deleteChat(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.deleteConversation(sessionId, currentUserId)
        val remaining = _sessionsFlow.value.filter { it.id != sessionId }
        _sessionsFlow.value = remaining
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = null
        }
        ChatApiClient.deleteConversation(sessionId)
        Result.success(Unit)
    }

    override suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.updateConversationDraft(sessionId, currentUserId, draftText)
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(draftText = draftText)
        }
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(draftText = draftText) else it
        }
        ChatApiClient.updateConversation(sessionId, draftText = draftText)
        Result.success(Unit)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.togglePin(sessionId, currentUserId, isPinned)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(isPinned = isPinned) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(isPinned = isPinned)
        }
        ChatApiClient.updateConversation(sessionId, isPinned = isPinned)
        Result.success(Unit)
    }

    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.toggleArchive(sessionId, currentUserId, isArchived)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(isArchived = isArchived) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(isArchived = isArchived)
        }
        ChatApiClient.updateConversation(sessionId, isArchived = isArchived)
        Result.success(Unit)
    }

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val streamFlow = streamMessage(prompt)
        val fullText = StringBuilder()
        try {
            streamFlow.collect { token ->
                fullText.append(token)
            }
            val lastMsg = _currentSessionFlow.value?.messages?.lastOrNull { !it.fromUser }
            if (lastMsg != null) {
                Result.success(lastMsg)
            } else {
                Result.success(ChatMessage(text = fullText.toString(), fromUser = false))
            }
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    override fun streamMessage(
        prompt: String,
        parentId: String?,
        clientMessageId: String?
    ): Flow<String> = flow {
        _errorFlow.value = null
        val current = _currentSessionFlow.value ?: createNewChat().getOrThrow()
        val userMsgId = clientMessageId ?: UUID.randomUUID().toString()
        val userTimestamp = System.currentTimeMillis()
        val tempUserMsg = ChatMessage(
            id = userMsgId,
            text = prompt,
            fromUser = true,
            timestamp = userTimestamp,
            parentId = parentId
        )

        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        val syncStatus = if (isOffline) "pending_sync" else "synced"

        // 1. Optimistically show & persist user message in local cache & unified session
        cacheDb.upsertMessage(
            id = userMsgId,
            conversationId = current.id,
            userId = currentUserId,
            role = "user",
            content = prompt,
            createdAt = userTimestamp,
            parentId = parentId,
            syncStatus = syncStatus
        )
        _currentSessionFlow.value = current.copy(
            messages = current.messages.filter { it.id != userMsgId } + tempUserMsg
        )

        // 2. Extract top relevant memories for prompt context
        val relevantMemories = cacheDb.getRelevantMemories(currentUserId, prompt, limit = 5)
        val memoryContext = if (relevantMemories.isNotEmpty()) {
            "\nStudent Context & Known Preferences:\n" +
                relevantMemories.joinToString("\n") { "- [${it.category}] ${it.content}" }
        } else ""

        val systemPrompt = systemPromptFor(_currentExplanationMode) + memoryContext

        // 3. Select active provider: Gemini for ONLINE, llama.cpp for OFFLINE
        val activeProvider: LLMProvider = if (isOffline) offlineProvider else onlineProvider

        val tempAiId = UUID.randomUUID().toString()
        val responseBuilder = StringBuilder()
        var streamCompletedNormally = false

        try {
            activeProvider.streamInference(
                prompt = prompt,
                conversationId = current.id,
                clientMessageId = userMsgId,
                parentId = parentId,
                explanationMode = _currentExplanationMode,
                history = current.messages,
                systemPrompt = systemPrompt
            ).collect { token ->
                responseBuilder.append(token)
                emit(token)

                // Update active session with live streaming text
                val activeMsg = ChatMessage(
                    id = tempAiId,
                    text = responseBuilder.toString(),
                    fromUser = false,
                    timestamp = System.currentTimeMillis(),
                    parentId = userMsgId
                )
                val curr = _currentSessionFlow.value ?: current
                val updatedList = curr.messages.filter { it.id != tempAiId } + activeMsg
                _currentSessionFlow.value = curr.copy(messages = updatedList)
            }
            streamCompletedNormally = true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Stream cancelled by client / stop generation")
            activeProvider.stopInference()
            throw cancelled
        } catch (error: Throwable) {
            Log.e(TAG, "Streaming failed: ${error.message}", error)
            _errorFlow.value = error.message ?: "AI generation failed"
            throw error
        } finally {
            if (!streamCompletedNormally && responseBuilder.isNotEmpty()) {
                val partialText = responseBuilder.toString()
                cacheDb.upsertMessage(
                    id = tempAiId,
                    conversationId = current.id,
                    userId = currentUserId,
                    role = "assistant",
                    content = partialText,
                    createdAt = System.currentTimeMillis(),
                    parentId = userMsgId,
                    syncStatus = syncStatus
                )
                val curr = _currentSessionFlow.value ?: current
                val stoppedMsg = ChatMessage(
                    id = tempAiId,
                    text = partialText,
                    fromUser = false,
                    timestamp = System.currentTimeMillis(),
                    parentId = userMsgId
                )
                _currentSessionFlow.value = curr.copy(
                    messages = curr.messages.filter { it.id != tempAiId } + stoppedMsg
                )
            } else if (streamCompletedNormally && responseBuilder.isNotEmpty()) {
                val finalContent = responseBuilder.toString()
                val finalAiMsg = ChatMessage(
                    id = tempAiId,
                    text = finalContent,
                    fromUser = false,
                    timestamp = System.currentTimeMillis(),
                    parentId = userMsgId
                )
                cacheDb.upsertMessage(
                    id = tempAiId,
                    conversationId = current.id,
                    userId = currentUserId,
                    role = "assistant",
                    content = finalContent,
                    createdAt = finalAiMsg.timestamp,
                    parentId = userMsgId,
                    syncStatus = syncStatus
                )
                val curr = _currentSessionFlow.value ?: current
                val finalizedList = curr.messages.filter { it.id != tempAiId } + finalAiMsg
                val finalTitle = if (current.messages.isEmpty() && prompt.isNotBlank()) {
                    prompt.take(40).trim().replaceFirstChar { it.uppercase() }
                } else current.title
                cacheDb.updateConversationTitle(current.id, currentUserId, finalTitle)
                val updatedSession = curr.copy(title = finalTitle, messages = finalizedList)
                _currentSessionFlow.value = updatedSession
                _sessionsFlow.value = listOf(updatedSession.copy(messages = emptyList())) +
                    _sessionsFlow.value.filter { it.id != updatedSession.id }
            }
        }
    }

    override suspend fun stopGeneration(): Result<Unit> {
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        if (isOffline) {
            offlineProvider.stopInference()
        } else {
            onlineProvider.stopInference()
        }
        activeStreamJob?.cancel()
        activeStreamJob = null
        return Result.success(Unit)
    }

    override fun regenerateLastResponse(): Flow<String> = flow {
        val current = _currentSessionFlow.value ?: return@flow
        val lastUserMsg = current.messages.lastOrNull { it.fromUser } ?: return@flow
        // Remove the last assistant message if present
        val prunedMessages = current.messages.dropLastWhile { !it.fromUser }
        _currentSessionFlow.value = current.copy(messages = prunedMessages)
        streamMessage(
            prompt = lastUserMsg.text,
            parentId = lastUserMsg.parentId,
            clientMessageId = null
        ).collect { emit(it) }
    }

    override fun editMessageAndRegenerate(messageId: String, newContent: String): Flow<String> = flow {
        val current = _currentSessionFlow.value ?: return@flow
        val targetIdx = current.messages.indexOfFirst { it.id == messageId }
        if (targetIdx == -1) return@flow

        val targetMsg = current.messages[targetIdx]
        // Prune messages after the edited message
        val truncatedMessages = current.messages.take(targetIdx)
        _currentSessionFlow.value = current.copy(messages = truncatedMessages)

        streamMessage(
            prompt = newContent,
            parentId = targetMsg.parentId,
            clientMessageId = null
        ).collect { emit(it) }
    }

    override suspend fun loadMoreMessages(
        sessionId: String,
        beforeTimestamp: Long?,
        limit: Int
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val local = cacheDb.getMessagesForConversation(sessionId, currentUserId, limit, beforeTimestamp)
        val isoCursor = beforeTimestamp?.let { Instant.ofEpochMilli(it).toString() }
        ChatApiClient.getConversationMessages(sessionId, limit = limit, beforeTimestamp = isoCursor).fold(
            onSuccess = { dtos ->
                val remote = dtos.map { dto ->
                    ChatMessage(
                        id = dto.id,
                        text = dto.content,
                        fromUser = dto.role == "user",
                        timestamp = parseIsoToMillis(dto.createdAt),
                        parentId = dto.parentId,
                        isEdited = dto.isEdited
                    )
                }
                val messagesWithRoles = remote.map { msg ->
                    Pair(msg, if (msg.fromUser) "user" else "assistant")
                }
                cacheDb.upsertMessages(messagesWithRoles, sessionId, currentUserId)
                Result.success(remote)
            },
            onFailure = {
                Result.success(local)
            }
        )
    }

    override suspend fun loadMoreConversations(
        beforeCursor: Long?,
        limit: Int
    ): Result<List<ChatSession>> = withContext(Dispatchers.IO) {
        val local = cacheDb.getConversations(currentUserId, limit, beforeCursor)
        val isoCursor = beforeCursor?.let { Instant.ofEpochMilli(it).toString() }
        ChatApiClient.listConversations(limit = limit, cursor = isoCursor).fold(
            onSuccess = { dtos ->
                val remote = dtos.map { dto ->
                    ChatSession(
                        id = dto.id,
                        title = dto.title,
                        lastUpdated = parseIsoToMillis(dto.updatedAt),
                        isArchived = dto.isArchived,
                        isPinned = dto.isPinned,
                        draftText = dto.draftText
                    ).also {
                        cacheDb.upsertConversation(
                            id = dto.id,
                            userId = currentUserId,
                            title = dto.title,
                            createdAt = parseIsoToMillis(dto.updatedAt),
                            updatedAt = parseIsoToMillis(dto.updatedAt),
                            isArchived = dto.isArchived,
                            isPinned = dto.isPinned,
                            draftText = dto.draftText
                        )
                    }
                }
                _sessionsFlow.value = (_sessionsFlow.value + remote).distinctBy { it.id }
                Result.success(remote)
            },
            onFailure = {
                Result.success(local)
            }
        )
    }

    override suspend fun getMemories(): Result<List<UserMemoryItem>> = withContext(Dispatchers.IO) {
        val cached = cacheDb.getMemories(currentUserId)
        ChatApiClient.listMemories().fold(
            onSuccess = { dtos ->
                val domainItems = dtos.map { dto ->
                    val item = UserMemoryItem(
                        id = dto.id,
                        category = dto.category,
                        content = dto.content,
                        importance = dto.importance,
                        confidence = dto.confidence,
                        active = dto.active
                    )
                    cacheDb.upsertMemory(item, currentUserId, null)
                    item
                }
                Result.success(domainItems)
            },
            onFailure = {
                Result.success(cached)
            }
        )
    }

    override suspend fun deleteMemory(memoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        cacheDb.deleteMemory(memoryId, currentUserId)
        ChatApiClient.deleteMemory(memoryId)
        Result.success(Unit)
    }

    override suspend fun syncOfflineData(): Result<Unit> = withContext(Dispatchers.IO) {
        val unsynced = cacheDb.getUnsyncedMessages(currentUserId)
        if (unsynced.isEmpty()) return@withContext Result.success(Unit)

        val syncItems = unsynced.map { (msg, convId) ->
            com.offline_First.data.remote.SyncMessageItemDto(
                id = msg.id,
                conversationId = convId,
                role = if (msg.fromUser) "user" else "assistant",
                content = msg.text,
                parentId = msg.parentId,
                createdAt = Instant.ofEpochMilli(msg.timestamp).toString()
            )
        }

        ChatApiClient.syncMessages(syncItems).fold(
            onSuccess = { syncedIds ->
                cacheDb.markMessagesSynced(syncedIds, currentUserId)
                Result.success(Unit)
            },
            onFailure = { error ->
                Log.w(TAG, "Offline sync failed: ${error.message}")
                Result.failure(error)
            }
        )
    }

    fun clearError() {
        _errorFlow.value = null
    }

    suspend fun refreshConversations() = withContext(Dispatchers.IO) {
        loadMoreConversations(null, 20)
    }

    private fun parseIsoToMillis(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
