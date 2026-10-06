package com.offline_First.data.local

import android.app.ActivityManager
import android.content.Context
import com.offline_First.BuildConfig
import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import okhttp3.Request
import com.offline_First.data.remote.ChatApiClient
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.coroutineContext

internal fun useLargeOfflineModel(totalRamBytes: Long): Boolean =
    totalRamBytes >= 8L * 1024L * 1024L * 1024L

internal fun isValidGgufFile(file: File, minimumValidBytes: Long): Boolean {
    if (!file.isFile || file.length() < minimumValidBytes) return false
    return runCatching {
        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            val magic = ByteArray(4)
            input.readFully(magic)
            if (!magic.contentEquals(byteArrayOf(0x47, 0x47, 0x55, 0x46))) return@runCatching false
            val header = ByteArray(20)
            input.readFully(header)
            val fields = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val version = fields.int
            val tensorCount = fields.long
            val metadataCount = fields.long
            version in 2..3 && tensorCount > 0L && metadataCount > 0L
        }
    }.getOrDefault(false)
}

/** Local GGUF downloader and llama.cpp-backed chat repository. */
class LocalAIRepository(
    private val context: Context? = null,
    private val inferenceEngine: OfflineInferenceEngine? = context?.let { LlamaCppInferenceEngine() }
) : AIRepository {

    /** Device selection stays internal; the UI never asks the user to choose a model. */
    private enum class InternalModelProfile(
        val fileName: String,
        val sizeLabel: String,
        val minimumValidBytes: Long,
        val minimumFreeBytes: Long
    ) {
        COMPACT("Qwen2.5-1.5B-Instruct-Q4_K_M.gguf", "Approx. 1.0 GB", 500_000_000L, 768_000_000L),
        ENHANCED("Qwen2.5-3B-Instruct-Q4_K_M.gguf", "Approx. 2.0 GB", 1_000_000_000L, 1_300_000_000L)
    }

    companion object {
        private const val PREFS_NAME = "offline_ai_repository"
        private const val STATUS_KEY = "offline_ai_status"
        private const val CONNECTION_MODE_KEY = "connection_mode"
        private const val EXPLANATION_MODE_KEY = "explanation_mode"

        private val connectionMode = MutableStateFlow(ConnectionMode.ONLINE)
        private val explanationMode = MutableStateFlow(ExplanationMode.GENERAL)
        private val offlineStatus = MutableStateFlow(OfflineAIStatus.NOT_DOWNLOADED)
        private val downloadProgress = MutableStateFlow(OfflineAIDownloadProgress())
        private val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
        private val currentSession = MutableStateFlow<ChatSession?>(null)

        fun resetState() {
            sessions.value = emptyList()
            currentSession.value = null
            connectionMode.value = ConnectionMode.ONLINE
            explanationMode.value = ExplanationMode.GENERAL
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
        }
    }

    private val preferences by lazy {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val modelDirectory: File
        get() = context?.getDir("offline_models", Context.MODE_PRIVATE)
            ?: File(System.getProperty("java.io.tmpdir"), "offline_models")

    init {
        connectionMode.value = readEnum(CONNECTION_MODE_KEY, ConnectionMode.ONLINE)
        explanationMode.value = readEnum(EXPLANATION_MODE_KEY, ExplanationMode.GENERAL)
        val ready = runCatching { isValidModelFile(modelFile(selectModel()), selectModel()) }.getOrDefault(false)
        offlineStatus.value = if (ready) OfflineAIStatus.READY else OfflineAIStatus.NOT_DOWNLOADED
        downloadProgress.value = progressFor(selectModel(), ready)
        if (!ready) preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
    }

    private fun <T : Enum<T>> readEnum(key: String, default: T): T {
        val raw = preferences?.getString(key, null) ?: return default
        @Suppress("UNCHECKED_CAST")
        val enumClass = default.javaClass as Class<T>
        return runCatching { java.lang.Enum.valueOf(enumClass, raw) }.getOrDefault(default)
    }

    private fun selectModel(): InternalModelProfile {
        val totalRam = runCatching {
            val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            manager?.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        }.getOrDefault(0L)
        return if (useLargeOfflineModel(totalRam)) {
            InternalModelProfile.ENHANCED
        } else {
            InternalModelProfile.COMPACT
        }
    }

    private fun modelFile(profile: InternalModelProfile) = File(modelDirectory, profile.fileName)

    private fun modelUrl(profile: InternalModelProfile): String = when (profile) {
        InternalModelProfile.COMPACT -> BuildConfig.OFFLINE_MODEL_SMALL_URL
        InternalModelProfile.ENHANCED -> BuildConfig.OFFLINE_MODEL_LARGE_URL
    }

    /** Checks the GGUF signature and header fields, and rejects files too small to be either model. */
    private fun isValidModelFile(file: File, profile: InternalModelProfile): Boolean {
        return isValidGgufFile(file, profile.minimumValidBytes)
    }

    override fun observeConnectionMode(): Flow<ConnectionMode> = connectionMode.asStateFlow()

    internal fun currentConnectionMode(): ConnectionMode = connectionMode.value

    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> {
        connectionMode.value = mode
        preferences?.edit()?.putString(CONNECTION_MODE_KEY, mode.name)?.apply()
        return Result.success(Unit)
    }

    override fun observeExplanationMode(): Flow<ExplanationMode> = explanationMode.asStateFlow()

    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        explanationMode.value = mode
        preferences?.edit()?.putString(EXPLANATION_MODE_KEY, mode.name)?.apply()
        return Result.success(Unit)
    }

    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> = offlineStatus.asStateFlow()

    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> = downloadProgress.asStateFlow()

    override suspend fun startOfflineAIDownload(): Result<Unit> = withContext(Dispatchers.IO) {
        val profile = selectModel()
        val destination = modelFile(profile)

        // A valid model is kept and loaded. Never fetch the large file a second time.
        if (isValidModelFile(destination, profile)) {
            try {
                requireNotNull(inferenceEngine) { "The Android llama.cpp runtime is unavailable." }
                    .loadModel(destination)
                markReady(profile)
                return@withContext Result.success(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                return@withContext Result.failure(error)
            }
        }

        offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
        preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
        if (modelDirectory.usableSpace < profile.minimumFreeBytes) {
            return@withContext Result.failure(IllegalStateException("There is not enough free storage for Offline AI."))
        }
        val address = modelUrl(profile)
        if (address.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Offline AI model URL is not configured."))
        }

        offlineStatus.value = OfflineAIStatus.DOWNLOADING
        preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.DOWNLOADING.name)?.apply()
        downloadProgress.value = OfflineAIDownloadProgress(
            stage = "Connecting...", progress = 0, downloadSize = profile.sizeLabel,
            estimatedTimeRemaining = "Preparing download..."
        )
        modelDirectory.mkdirs()
        val partial = File(modelDirectory, profile.fileName + ".download")
        val jobContext = coroutineContext

        try {
            val request = Request.Builder()
                .url(address)
                .build()

            val response = ChatApiClient.okHttpClient.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("The Offline AI model download failed (HTTP ${res.code}).")
                }
                val body = res.body ?: throw IllegalStateException("Empty response body from model download.")
                val expectedBytes = body.contentLength()
                if (expectedBytes > 0L && modelDirectory.usableSpace < expectedBytes + 128L * 1024L * 1024L) {
                    throw IllegalStateException("There is not enough free storage for Offline AI.")
                }
                body.byteStream().use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            jobContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            val percent = if (expectedBytes > 0) {
                                ((downloaded * 100L) / expectedBytes).coerceIn(0L, 99L).toInt()
                            } else 0
                            downloadProgress.value = OfflineAIDownloadProgress(
                                stage = "Downloading...", progress = percent,
                                downloadSize = profile.sizeLabel,
                                estimatedTimeRemaining = estimateRemainingTime(percent)
                            )
                        }
                        output.fd.sync()
                    }
                }
                if (expectedBytes > 0L && partial.length() != expectedBytes) {
                    throw IllegalStateException("The Offline AI model download was incomplete.")
                }
            }

            downloadProgress.value = OfflineAIDownloadProgress(
                stage = "Verifying...", progress = 99, downloadSize = profile.sizeLabel,
                estimatedTimeRemaining = "Almost done..."
            )
            if (!isValidModelFile(partial, profile)) {
                throw IllegalStateException("The downloaded file is not a valid Qwen GGUF model.")
            }
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("Could not replace the existing Offline AI model file.")
            }
            if (!partial.renameTo(destination)) {
                throw IllegalStateException("Could not store the downloaded Offline AI model.")
            }
            requireNotNull(inferenceEngine) { "The Android llama.cpp runtime is unavailable." }
                .loadModel(destination)
            markReady(profile)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            partial.delete()
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
            preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
            throw cancelled
        } catch (error: Throwable) {
            partial.delete()
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
            preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
            val userMessage = when (error) {
                is java.net.UnknownHostException ->
                    "No internet connection. Connect to the internet and retry."
                is java.net.SocketTimeoutException ->
                    "Connection timed out. Check your internet connection and retry."
                is java.net.ConnectException ->
                    "Could not reach the download server. Check your internet connection and retry."
                else -> "Download failed: ${error.message ?: "Unknown error"}"
            }
            downloadProgress.value = OfflineAIDownloadProgress(
                stage = userMessage, progress = 0, downloadSize = profile.sizeLabel,
                estimatedTimeRemaining = "Retry later"
            )
            Result.failure(error)
        }
    }

    private fun markReady(profile: InternalModelProfile) {
        offlineStatus.value = OfflineAIStatus.READY
        preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.READY.name)?.apply()
        downloadProgress.value = OfflineAIDownloadProgress(
            stage = "Ready", progress = 100, downloadSize = profile.sizeLabel,
            estimatedTimeRemaining = "Ready"
        )
    }

    private fun progressFor(profile: InternalModelProfile, ready: Boolean) = if (ready) {
        OfflineAIDownloadProgress("Ready", 100, profile.sizeLabel, "Ready")
    } else {
        OfflineAIDownloadProgress("Downloading...", 0, profile.sizeLabel, "")
    }

    private fun estimateRemainingTime(progress: Int) = when {
        progress < 35 -> "Downloading..."
        progress < 70 -> "More than a minute remaining"
        progress < 90 -> "About a minute remaining"
        else -> "Almost done..."
    }

    override suspend fun deleteOfflineAI(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            inferenceEngine?.unload()
            val files = InternalModelProfile.entries.flatMap { profile ->
                listOf(modelFile(profile), File(modelDirectory, profile.fileName + ".download"))
            }
            val failedDelete = files.firstOrNull { it.exists() && !it.delete() }
            if (failedDelete != null) {
                return@withContext Result.failure(IllegalStateException("Unable to remove the downloaded Offline AI model."))
            }
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
            preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
            downloadProgress.value = progressFor(selectModel(), ready = false)
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    override fun observeChatSessions(): Flow<List<ChatSession>> = sessions.asStateFlow()

    override fun observeCurrentSession(): Flow<ChatSession?> = currentSession.asStateFlow()

    override suspend fun createNewChat(): Result<ChatSession> {
        val chat = ChatSession(id = UUID.randomUUID().toString(), title = "New Conversation")
        sessions.value = listOf(chat) + sessions.value
        currentSession.value = chat
        return Result.success(chat)
    }

    override suspend fun selectChat(sessionId: String): Result<Unit> {
        val chat = sessions.value.firstOrNull { it.id == sessionId }
        if (chat != null) currentSession.value = chat
        return Result.success(Unit)
    }

    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> {
        sessions.value = sessions.value.map { chat ->
            if (chat.id == sessionId) chat.copy(title = newTitle.ifBlank { chat.title }) else chat
        }
        if (currentSession.value?.id == sessionId) {
            currentSession.value = currentSession.value?.copy(title = newTitle.ifBlank { currentSession.value!!.title })
        }
        return Result.success(Unit)
    }

    override suspend fun deleteChat(sessionId: String): Result<Unit> {
        val remaining = sessions.value.filterNot { it.id == sessionId }
        sessions.value = remaining
        if (currentSession.value?.id == sessionId) {
            currentSession.value = remaining.firstOrNull()
        }
        return Result.success(Unit)
    }

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val profile = selectModel()
        val file = modelFile(profile)
        if (!isValidModelFile(file, profile)) {
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
            preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
            return@withContext Result.failure(
                IllegalStateException("Offline AI model is missing or invalid. Download Offline AI before chatting.")
            )
        }
        val engine = inferenceEngine ?: return@withContext Result.failure(
            IllegalStateException("The Android llama.cpp runtime is unavailable.")
        )
        val current = currentSession.value ?: createNewChat().getOrThrow()
        val userMessage = ChatMessage(text = prompt, fromUser = true)
        val response = try {
            engine.loadModel(file)
            engine.generate(systemPromptFor(explanationMode.value), buildConversationPrompt(current.messages, prompt))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            return@withContext Result.failure(error)
        }
        val assistantMessage = ChatMessage(text = response, fromUser = false)
        val title = if (current.title == "New Conversation" && prompt.isNotBlank()) {
            prompt.take(28).trim().replaceFirstChar { it.uppercase() }
        } else current.title
        val updated = current.copy(
            title = title,
            lastUpdated = System.currentTimeMillis(),
            messages = current.messages + userMessage + assistantMessage
        )
        currentSession.value = updated
        sessions.value = sessions.value.map { if (it.id == updated.id) updated else it }
        Result.success(assistantMessage)
    }

    suspend fun generateOfflineInference(
        systemPrompt: String,
        history: List<ChatMessage>,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val profile = selectModel()
        val file = modelFile(profile)
        if (!isValidModelFile(file, profile)) {
            offlineStatus.value = OfflineAIStatus.NOT_DOWNLOADED
            preferences?.edit()?.putString(STATUS_KEY, OfflineAIStatus.NOT_DOWNLOADED.name)?.apply()
            return@withContext Result.failure(
                IllegalStateException("Offline AI model is missing or invalid. Please download it in AI Settings.")
            )
        }
        val engine = inferenceEngine ?: return@withContext Result.failure(
            IllegalStateException("The Android llama.cpp runtime is unavailable on this device.")
        )
        try {
            engine.loadModel(file)
            val fullPrompt = buildConversationPrompt(history, prompt)
            val response = engine.generate(systemPrompt, fullPrompt)
            Result.success(response)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun stopOfflineInference(): Result<Unit> = withContext(Dispatchers.IO) {
        inferenceEngine?.unload()
        Result.success(Unit)
    }

    private fun buildConversationPrompt(history: List<ChatMessage>, prompt: String): String = buildString {
        history.takeLast(12).forEach { message ->
            append(if (message.fromUser) "User" else "Assistant")
            append(": ")
            append(message.text)
            append('\n')
        }
        append("User: ")
        append(prompt)
    }

    override fun streamMessage(
        prompt: String,
        parentId: String?,
        clientMessageId: String?
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        val result = sendMessage(prompt)
        if (result.isSuccess) {
            val text = result.getOrThrow().text
            val words = text.split(" ")
            for ((index, word) in words.withIndex()) {
                emit(if (index == 0) word else " $word")
                kotlinx.coroutines.delay(20)
            }
        } else {
            throw result.exceptionOrNull() ?: RuntimeException("Offline generation failed")
        }
    }

    override suspend fun stopGeneration(): Result<Unit> = Result.success(Unit)

    override fun regenerateLastResponse(): Flow<String> = kotlinx.coroutines.flow.flow {
        val current = currentSession.value ?: return@flow
        val lastUserMsg = current.messages.lastOrNull { it.fromUser } ?: return@flow
        streamMessage(lastUserMsg.text, lastUserMsg.parentId, null).collect { emit(it) }
    }

    override fun editMessageAndRegenerate(messageId: String, newContent: String): Flow<String> = kotlinx.coroutines.flow.flow {
        streamMessage(newContent, null, null).collect { emit(it) }
    }

    override suspend fun saveDraft(sessionId: String, draftText: String): Result<Unit> {
        currentSession.value = currentSession.value?.copy(draftText = draftText)
        return Result.success(Unit)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean): Result<Unit> {
        sessions.value = sessions.value.map { if (it.id == sessionId) it.copy(isPinned = isPinned) else it }
        return Result.success(Unit)
    }

    override suspend fun toggleArchive(sessionId: String, isArchived: Boolean): Result<Unit> {
        sessions.value = sessions.value.map { if (it.id == sessionId) it.copy(isArchived = isArchived) else it }
        return Result.success(Unit)
    }

    override suspend fun loadMoreMessages(sessionId: String, beforeTimestamp: Long?, limit: Int): Result<List<ChatMessage>> =
        Result.success(currentSession.value?.messages ?: emptyList())

    override suspend fun loadMoreConversations(beforeCursor: Long?, limit: Int): Result<List<ChatSession>> =
        Result.success(sessions.value)

    override suspend fun getMemories(): Result<List<com.offline_First.domain.model.UserMemoryItem>> =
        Result.success(emptyList())

    override suspend fun deleteMemory(memoryId: String): Result<Unit> = Result.success(Unit)

    override suspend fun syncOfflineData(): Result<Unit> = Result.success(Unit)
}
