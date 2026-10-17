package com.offline_First.data.remote

import android.content.Context
import android.util.Log
import com.offline_First.data.local.ChatCacheDatabase
import com.offline_First.data.local.LocalAIRepository
import com.offline_First.data.local.SyllabusDatabase
import com.offline_First.data.local.OfflineSyllabusRepository
import com.offline_First.data.local.E5EmbeddingService
import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.RetrievedChunk
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.domain.model.UserMemoryItem
import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.RAGRequestContext
import com.offline_First.data.provider.LLMProvider
import com.offline_First.data.provider.OnlineGeminiProvider
import com.offline_First.data.provider.OfflineLlamaProvider
import com.offline_First.data.local.systemPromptFor
import com.offline_First.data.local.MemoryExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private val context: Context? = null,
    private val delegate: LocalAIRepository = LocalAIRepository(context),
    private val userIdProvider: () -> String = { "default_user" },
    private val offlineSyllabusRepositoryProvider: () -> OfflineSyllabusRepository = {
        context?.let { OfflineSyllabusRepository(it) } ?: OfflineSyllabusRepository()
    },
    private val e5EmbeddingServiceProvider: () -> E5EmbeddingService = { E5EmbeddingService(context) },
    private val onlineProviderOverride: LLMProvider? = null,
    private val offlineProviderOverride: LLMProvider? = null,
    private val cacheDbOverride: ChatCacheDatabase? = null,
    private val profileProvider: () -> com.offline_First.domain.model.UserProfile? = {
        try { com.offline_First.data.AppContainer.profileRepository.currentProfile } catch (_: Throwable) { null }
    },
    private val initialSession: ChatSession? = null
) : AIRepository {

    val cacheDb by lazy {
        cacheDbOverride ?: context?.let { ChatCacheDatabase(it) } ?: ChatCacheDatabase(context ?: object : android.content.ContextWrapper(null) {})
    }
    private val offlineSyllabusRepository by lazy { offlineSyllabusRepositoryProvider() }
    private val e5EmbeddingService by lazy { e5EmbeddingServiceProvider() }

    private val onlineProvider: LLMProvider by lazy { onlineProviderOverride ?: OnlineGeminiProvider() }
    private val offlineProvider: LLMProvider by lazy { offlineProviderOverride ?: OfflineLlamaProvider(delegate) }

    private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _currentSessionFlow = MutableStateFlow<ChatSession?>(null)
    private val _errorFlow = MutableStateFlow<String?>(null)
    private var activeStreamJob: Job? = null

    private var _currentExplanationMode = ExplanationMode.GENERAL

    val errorFlow: Flow<String?> = _errorFlow.asStateFlow()

    private val currentUserId: String
        get() = userIdProvider().ifBlank { "default_user" }

    private inline fun safeCacheDb(action: (ChatCacheDatabase) -> Unit) {
        try {
            action(cacheDb)
        } catch (e: Throwable) {
            Log.w(TAG, "Cache operation failed: ${e.message}")
        }
    }

    init {
        if (initialSession != null) {
            _currentSessionFlow.value = initialSession
            _sessionsFlow.value = listOf(initialSession)
        } else {
            // Load cached conversations for the active user
            val cached = try {
                cacheDb.getConversations(currentUserId)
            } catch (_: Throwable) {
                emptyList()
            }
            _sessionsFlow.value = cached
        }
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
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        if (isOffline) {
            val localId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val session = ChatSession(id = localId, title = "New Conversation", lastUpdated = now)
            safeCacheDb {
                it.upsertConversation(localId, currentUserId, "New Conversation", now, now, syncStatus = "pending_sync")
            }
            _sessionsFlow.value = listOf(session) + _sessionsFlow.value.filter { it.id != localId }
            _currentSessionFlow.value = session
            return@withContext Result.success(session)
        }

        val remoteResult = try {
            ChatApiClient.createConversation()
        } catch (e: Throwable) {
            Result.failure(e)
        }
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
                safeCacheDb {
                    it.upsertConversation(
                        id = dto.id,
                        userId = currentUserId,
                        title = dto.title,
                        createdAt = parseIsoToMillis(dto.createdAt),
                        updatedAt = parseIsoToMillis(dto.updatedAt),
                        isArchived = dto.isArchived,
                        isPinned = dto.isPinned,
                        draftText = dto.draftText
                    )
                }
                _sessionsFlow.value = listOf(session) + _sessionsFlow.value.filter { it.id != session.id }
                _currentSessionFlow.value = session
                Result.success(session)
            },
            onFailure = { error ->
                Log.w(TAG, "createNewChat remote failed, creating local session: ${error.message}")
                val localId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val session = ChatSession(id = localId, title = "New Conversation", lastUpdated = now)
                safeCacheDb {
                    it.upsertConversation(localId, currentUserId, "New Conversation", now, now, syncStatus = "pending_sync")
                }
                _sessionsFlow.value = listOf(session) + _sessionsFlow.value.filter { it.id != localId }
                _currentSessionFlow.value = session
                Result.success(session)
            }
        )
    }

    override suspend fun selectChat(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        _errorFlow.value = null
        // 1. Immediately resolve cached or in-memory session to isolate and show immediately
        val cached = cacheDb.getConversationWithMessages(sessionId, currentUserId)
        if (cached != null) {
            _currentSessionFlow.value = cached
        } else {
            val inMemory = _sessionsFlow.value.find { it.id == sessionId }
            if (inMemory != null) {
                _currentSessionFlow.value = inMemory
            }
        }

        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        if (isOffline) {
            return@withContext Result.success(Unit)
        }

        // 2. Fetch fresh conversation details and messages from backend
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
                safeCacheDb {
                    it.upsertConversation(
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
                    it.upsertMessages(messagesWithRoles, detail.id, currentUserId)
                }
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
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        val syncStatus = if (isOffline) "pending_sync" else "synced"
        cacheDb.updateConversationTitle(sessionId, currentUserId, newTitle, syncStatus)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(title = newTitle) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(title = newTitle)
        }
        if (!isOffline) {
            ChatApiClient.updateConversation(sessionId, title = newTitle)
        }
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
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        val syncStatus = if (isOffline) "pending_sync" else "synced"
        cacheDb.updateConversationDraft(sessionId, currentUserId, draftText, syncStatus)
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(draftText = draftText)
        }
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(draftText = draftText) else it
        }
        if (!isOffline) {
            ChatApiClient.updateConversation(sessionId, draftText = draftText)
        }
        Result.success(Unit)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        val syncStatus = if (isOffline) "pending_sync" else "synced"
        cacheDb.togglePin(sessionId, currentUserId, isPinned, syncStatus)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(isPinned = isPinned) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(isPinned = isPinned)
        }
        if (!isOffline) {
            ChatApiClient.updateConversation(sessionId, isPinned = isPinned)
        }
        Result.success(Unit)
    }

    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        val syncStatus = if (isOffline) "pending_sync" else "synced"
        cacheDb.toggleArchive(sessionId, currentUserId, isArchived, syncStatus)
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == sessionId) it.copy(isArchived = isArchived) else it
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(isArchived = isArchived)
        }
        if (!isOffline) {
            ChatApiClient.updateConversation(sessionId, isArchived = isArchived)
        }
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
        val originatingConvId = current.id
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

        // 1. Immediately clear draft in local cache, session, and conversation list
        safeCacheDb { it.updateConversationDraft(originatingConvId, currentUserId, "") }
        val sessionWithoutDraft = current.copy(
            draftText = "",
            messages = current.messages.filter { it.id != userMsgId } + tempUserMsg
        )
        val currStart = _currentSessionFlow.value
        if (currStart == null || currStart.id == originatingConvId) {
            _currentSessionFlow.value = sessionWithoutDraft
        }
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == originatingConvId) it.copy(draftText = "") else it
        }

        // 2. Optimistically show & persist user message in local cache & unified session
        safeCacheDb {
            it.upsertMessage(
                id = userMsgId,
                conversationId = originatingConvId,
                userId = currentUserId,
                role = "user",
                content = prompt,
                createdAt = userTimestamp,
                parentId = parentId,
                syncStatus = syncStatus
            )
        }

        // 2. Extract long-term memories from user prompt and persist locally & cloud
        val extractedMemories = MemoryExtractor.extract(prompt)
        for (extracted in extractedMemories) {
            val memoryItem = UserMemoryItem(
                id = UUID.randomUUID().toString(),
                category = extracted.category,
                content = extracted.content,
                importance = extracted.importance,
                confidence = extracted.confidence,
                active = true
            )
            safeCacheDb { it.upsertMemory(memoryItem, currentUserId, originatingConvId) }
            if (!isOffline) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ChatApiClient.createMemory(extracted.category, extracted.content)
                    } catch (_: Throwable) {}
                }
            }
        }

        // 3. Extract top relevant memories for prompt context
        val relevantMemories = try {
            cacheDb.getRelevantMemories(currentUserId, prompt, limit = 5)
        } catch (_: Throwable) {
            emptyList()
        }
        val memoryContext = if (relevantMemories.isNotEmpty()) {
            "\nStudent Context & Known Preferences:\n" +
                relevantMemories.joinToString("\n") { "- [${it.category}] ${it.content}" }
        } else ""

        val profile = profileProvider()
        val isSchool = profile?.educationMode == com.offline_First.domain.model.EducationMode.SCHOOL

        var syllabusContext = ""
        var offlineCitations: List<com.offline_First.domain.model.SourceCitation> = emptyList()
        if (isOffline && isSchool) {
            val ragContext = RAGRequestContext(
                executionMode = ConnectionMode.OFFLINE,
                appMode = AppMode.STUDENT,
                board = profile?.board,
                classGrade = profile?.classGrade,
                subject = profile?.subject
            )

            val (chunks, outcome) = if (e5EmbeddingService.isModelReady()) {
                val queryEmb = e5EmbeddingService.embedQuery(prompt)
                offlineSyllabusRepository.retrieve(
                    query = prompt,
                    context = ragContext,
                    topK = 3,
                    queryEmbedding = queryEmb
                )
            } else {
                // Graceful keyword fallback when E5 model artifact is not ready
                offlineSyllabusRepository.retrieve(
                    query = prompt,
                    context = ragContext,
                    topK = 3,
                    queryEmbedding = null
                )
            }

            if (outcome == EvidenceOutcome.MISSING || chunks.isEmpty()) {
                // Strict abstention behavior on empty/insufficient syllabus evidence
                val bStr = profile?.board ?: "your curriculum"
                val cStr = if (profile?.classGrade != null) " Class ${profile.classGrade}" else ""
                val sStr = if (profile?.subject != null) " ${profile.subject}" else ""
                val abstentionText = "This topic was not found in the official curriculum corpus for $bStr$cStr$sStr. Please verify the chapter or check your curriculum topics."

                emit(abstentionText)

                val abstentionMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = abstentionText,
                    fromUser = false,
                    timestamp = System.currentTimeMillis(),
                    parentId = userMsgId
                )
                safeCacheDb {
                    it.upsertMessage(
                        id = abstentionMsg.id,
                        conversationId = originatingConvId,
                        userId = currentUserId,
                        role = "assistant",
                        content = abstentionText,
                        createdAt = abstentionMsg.timestamp,
                        parentId = userMsgId,
                        syncStatus = syncStatus
                    )
                }
                val curr = _currentSessionFlow.value
                if (curr != null && curr.id == originatingConvId) {
                    val finalizedList = curr.messages + abstentionMsg
                    val updatedSession = curr.copy(messages = finalizedList)
                    _currentSessionFlow.value = updatedSession
                }
                _sessionsFlow.value = _sessionsFlow.value.map {
                    if (it.id == originatingConvId) it.copy(lastUpdated = abstentionMsg.timestamp) else it
                }

                return@flow
            } else {
                offlineCitations = chunks.map { c ->
                    com.offline_First.domain.model.SourceCitation(
                        textbookTitle = c.textbookTitle,
                        chapter = c.chapter,
                        section = c.section,
                        pageReference = c.pageReference,
                        sourceId = c.chunkId
                    )
                }
                syllabusContext = "\nOffline Curriculum References:\n" + chunks.mapIndexed { idx: Int, c: RetrievedChunk ->
                    "[${idx + 1}] ${c.textbookTitle ?: ""} (Chapter: ${c.chapter ?: "N/A"}): ${c.content}"
                }.joinToString("\n\n")
            }
        }

        val systemPrompt = systemPromptFor(_currentExplanationMode) + memoryContext + syllabusContext

        // 3. Select active provider: Gemini for ONLINE, llama.cpp for OFFLINE
        val activeProvider: LLMProvider = if (isOffline) {
            Log.i("OnlineAIRepository", "OFFLINE_LATENCY: repository_received")
            offlineProvider
        } else onlineProvider

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

                // Update active session with live streaming text ONLY IF active session is still the originating conversation!
                val curr = _currentSessionFlow.value
                if (curr != null && curr.id == originatingConvId) {
                    val currentCitations = if (isOffline) offlineCitations else activeProvider.getLastCitations()
                    val activeMsg = ChatMessage(
                        id = tempAiId,
                        text = responseBuilder.toString(),
                        fromUser = false,
                        timestamp = System.currentTimeMillis(),
                        parentId = userMsgId,
                        citations = currentCitations
                    )
                    val messages = curr.messages
                    val updatedList = if (messages.isNotEmpty() && messages.last().id == tempAiId) {
                        messages.toMutableList().apply { set(lastIndex, activeMsg) }
                    } else {
                        messages + activeMsg
                    }
                    _currentSessionFlow.value = curr.copy(messages = updatedList)
                }
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
            val resolvedCitations = if (isOffline) offlineCitations else activeProvider.getLastCitations()
            if (!streamCompletedNormally && responseBuilder.isNotEmpty()) {
                val partialText = responseBuilder.toString()
                safeCacheDb {
                    it.upsertMessage(
                        id = tempAiId,
                        conversationId = originatingConvId,
                        userId = currentUserId,
                        role = "assistant",
                        content = partialText,
                        createdAt = System.currentTimeMillis(),
                        parentId = userMsgId,
                        syncStatus = syncStatus
                    )
                }
                val curr = _currentSessionFlow.value
                if (curr != null && curr.id == originatingConvId) {
                    val stoppedMsg = ChatMessage(
                        id = tempAiId,
                        text = partialText,
                        fromUser = false,
                        timestamp = System.currentTimeMillis(),
                        parentId = userMsgId,
                        citations = resolvedCitations
                    )
                    _currentSessionFlow.value = curr.copy(
                        messages = curr.messages.filter { it.id != tempAiId } + stoppedMsg
                    )
                }
                _sessionsFlow.value = _sessionsFlow.value.map {
                    if (it.id == originatingConvId) it.copy(lastUpdated = System.currentTimeMillis()) else it
                }
            } else if (streamCompletedNormally && responseBuilder.isNotEmpty()) {
                val finalContent = responseBuilder.toString()
                val finalAiMsg = ChatMessage(
                    id = tempAiId,
                    text = finalContent,
                    fromUser = false,
                    timestamp = System.currentTimeMillis(),
                    parentId = userMsgId,
                    citations = resolvedCitations
                )
                val isFirstExchange = current.messages.count { it.fromUser } <= 1 &&
                    (current.title == "New Conversation" || current.title.isBlank())
                val finalTitle = if (isFirstExchange && prompt.isNotBlank()) {
                    activeProvider.getLastTitle()?.takeIf { it.isNotBlank() }
                        ?: com.offline_First.data.local.TitleGenerator.generateFallbackTitle(prompt)
                } else current.title

                safeCacheDb {
                    it.upsertMessage(
                        id = tempAiId,
                        conversationId = originatingConvId,
                        userId = currentUserId,
                        role = "assistant",
                        content = finalContent,
                        createdAt = finalAiMsg.timestamp,
                        parentId = userMsgId,
                        syncStatus = syncStatus
                    )
                    it.updateConversationTitle(originatingConvId, currentUserId, finalTitle)
                }

                if (!isOffline && isFirstExchange && finalTitle != "New Conversation") {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                            ChatApiClient.updateConversation(originatingConvId, title = finalTitle)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Failed to sync generated title: ${e.message}")
                        }
                    }
                }

                val curr = _currentSessionFlow.value
                if (curr != null && curr.id == originatingConvId) {
                    val finalizedList = curr.messages.filter { it.id != tempAiId } + finalAiMsg
                    val updatedSession = curr.copy(title = finalTitle, messages = finalizedList)
                    _currentSessionFlow.value = updatedSession
                }
                _sessionsFlow.value = _sessionsFlow.value.map {
                    if (it.id == originatingConvId) it.copy(title = finalTitle, lastUpdated = finalAiMsg.timestamp)
                    else it
                }
            } else if (streamCompletedNormally && responseBuilder.isEmpty()) {
                Log.w(TAG, "Stream completed normally but response was empty")
                _errorFlow.value = "No response generated by AI. Please try again."
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
        val lastAssistantMsg = current.messages.lastOrNull { !it.fromUser }
        if (lastAssistantMsg != null) {
            safeCacheDb {
                it.deleteMessage(lastAssistantMsg.id, currentUserId)
            }
        }
        val prunedMessages = current.messages.dropLastWhile { !it.fromUser }
        _currentSessionFlow.value = current.copy(messages = prunedMessages.filter { it.id != lastUserMsg.id })
        streamMessage(
            prompt = lastUserMsg.text,
            parentId = lastUserMsg.parentId,
            clientMessageId = lastUserMsg.id
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
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        if (isOffline) {
            _sessionsFlow.value = (local + _sessionsFlow.value).distinctBy { it.id }
            return@withContext Result.success(local)
        }
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
                        safeCacheDb { db ->
                            db.upsertConversation(
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
                }
                _sessionsFlow.value = (remote + local + _sessionsFlow.value).distinctBy { it.id }
                Result.success(remote)
            },
            onFailure = {
                _sessionsFlow.value = (local + _sessionsFlow.value).distinctBy { it.id }
                Result.success(local)
            }
        )
    }

    override suspend fun getMemories(): Result<List<UserMemoryItem>> = withContext(Dispatchers.IO) {
        val cached = try { cacheDb.getMemories(currentUserId) } catch (_: Throwable) { emptyList() }
        val isOffline = delegate.currentConnectionMode() == ConnectionMode.OFFLINE
        if (isOffline) {
            return@withContext Result.success(cached)
        }
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
                    safeCacheDb { it.upsertMemory(item, currentUserId, null) }
                    item
                }
                val combined = (domainItems + cached).distinctBy { "${it.category}:${it.content.trim().lowercase()}" }
                Result.success(combined)
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
        // 1. Sync pending offline conversation metadata
        val unsyncedConvs = cacheDb.getUnsyncedConversations(currentUserId)
        for (conv in unsyncedConvs) {
            ChatApiClient.updateConversation(
                conversationId = conv.id,
                title = conv.title,
                isArchived = conv.isArchived,
                isPinned = conv.isPinned,
                draftText = conv.draftText
            ).onSuccess {
                cacheDb.markConversationSynced(conv.id, currentUserId)
            }
        }

        // 2. Sync pending offline messages
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
