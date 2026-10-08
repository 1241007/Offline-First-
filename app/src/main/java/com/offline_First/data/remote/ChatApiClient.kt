package com.offline_First.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

object ChatApiClient {

    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 1000L

    private val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(AppDns)
            .connectTimeout(ChatApiConfig.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var customClient: OkHttpClient? = null

    var okHttpClient: OkHttpClient
        get() = customClient ?: defaultClient
        set(value) {
            customClient = value
        }

    fun initialize(client: OkHttpClient) {
        customClient = client
    }

    @Volatile
    var customBaseUrl: String? = null

    private fun buildUrl(path: String): String {
        val base = customBaseUrl ?: ChatApiConfig.BASE_URL
        return "$base${ChatApiConfig.API_PREFIX}$path"
    }

    private suspend fun <T> retryOnNetworkError(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: java.net.UnknownHostException) {
                lastError = e
                android.util.Log.w("ChatApiClient", "Attempt ${attempt + 1}/$MAX_RETRIES failed (UnknownHost), retrying...")
            } catch (e: java.net.SocketTimeoutException) {
                lastError = e
                android.util.Log.w("ChatApiClient", "Attempt ${attempt + 1}/$MAX_RETRIES failed (Timeout), retrying...")
            } catch (e: java.net.ConnectException) {
                lastError = e
                android.util.Log.w("ChatApiClient", "Attempt ${attempt + 1}/$MAX_RETRIES failed (ConnectException), retrying...")
            } catch (e: java.io.IOException) {
                lastError = e
                android.util.Log.w("ChatApiClient", "Attempt ${attempt + 1}/$MAX_RETRIES failed (IOException: ${e.message}), retrying...")
            }
            if (attempt < MAX_RETRIES - 1) {
                val waitMs = (RETRY_DELAY_MS * (attempt + 1)).coerceAtMost(10_000L)
                delay(waitMs)
            }
        }
        throw lastError!!
    }

    private suspend fun get(path: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        retryOnNetworkError {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                Pair(response.code, response.body?.string().orEmpty())
            }
        }
    }

    private suspend fun post(path: String, bodyJson: String = ""): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path)
            retryOnNetworkError {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = bodyJson.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    Pair(response.code, response.body?.string().orEmpty())
                }
            }
        }

    private suspend fun patch(path: String, bodyJson: String = ""): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path)
            retryOnNetworkError {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = bodyJson.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .patch(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    Pair(response.code, response.body?.string().orEmpty())
                }
            }
        }

    private suspend fun delete(path: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        retryOnNetworkError {
            val request = Request.Builder()
                .url(url)
                .delete()
                .addHeader("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                Pair(response.code, response.body?.string().orEmpty())
            }
        }
    }

    // --- Chat APIs ---

    suspend fun createConversation(): Result<ConversationDto> = runCatching {
        val (code, body) = post("/chat/conversations")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<ConversationDto>(body)
    }

    suspend fun listConversations(
        limit: Int = 20,
        cursor: String? = null,
        search: String? = null,
        includeArchived: Boolean = false
    ): Result<List<ConversationSummaryDto>> = runCatching {
        val params = mutableListOf("limit=$limit")
        if (cursor != null) params.add("cursor=$cursor")
        if (!search.isNullOrBlank()) params.add("search=$search")
        if (includeArchived) params.add("include_archived=true")
        val path = "/chat/conversations?" + params.joinToString("&")
        val (code, body) = get(path)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<ConversationSummaryDto>>(body)
    }

    suspend fun getConversation(id: String): Result<ConversationDetailDto> = runCatching {
        val (code, body) = get("/chat/conversations/$id")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<ConversationDetailDto>(body)
    }

    suspend fun getConversationMessages(
        conversationId: String,
        limit: Int = 30,
        beforeTimestamp: String? = null
    ): Result<List<MessageDto>> = runCatching {
        val params = mutableListOf("limit=$limit")
        if (beforeTimestamp != null) params.add("before_timestamp=$beforeTimestamp")
        val path = "/chat/conversations/$conversationId/messages?" + params.joinToString("&")
        val (code, body) = get(path)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<MessageDto>>(body)
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        explanationMode: String = "general",
        clientMessageId: String? = null,
        parentId: String? = null
    ): Result<SendMessageResponseDto> = runCatching {
        android.util.Log.i("ChatApiClient", "ONLINE_CHAT: request_started")
        val requestBody = json.encodeToString(
            SendMessageRequestDto(
                content = content,
                explanationMode = explanationMode,
                clientMessageId = clientMessageId,
                parentId = parentId
            )
        )
        android.util.Log.i("ChatApiClient", "ONLINE_CHAT: request_authenticated")
        val (code, body) = post("/chat/conversations/$conversationId/messages", requestBody)
        if (code !in 200..299) {
            android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed HTTP $code")
            error("HTTP $code: $body")
        }
        android.util.Log.i("ChatApiClient", "ONLINE_CHAT: stream_completed")
        json.decodeFromString<SendMessageResponseDto>(body)
    }.onFailure {
        android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed ${it.message}")
    }

    /**
     * POST Server-Sent Events (SSE) AI generation streaming with cancellation support.
     * Cancelling this Flow immediately cancels the underlying HTTP call, signaling Stop Generation to backend.
     */
    fun streamMessage(
        conversationId: String,
        content: String,
        explanationMode: String = "general",
        clientMessageId: String? = null,
        parentId: String? = null
    ): Flow<StreamEventDto> = callbackFlow {
        android.util.Log.i("ChatApiClient", "ONLINE_CHAT: request_started")
        val url = buildUrl("/chat/conversations/$conversationId/messages/stream")
        val bodyJson = json.encodeToString(
            SendMessageRequestDto(
                content = content,
                explanationMode = explanationMode,
                clientMessageId = clientMessageId,
                parentId = parentId
            )
        )
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(mediaType))
            .addHeader("Accept", "text/event-stream")
            .build()

        android.util.Log.i("ChatApiClient", "ONLINE_CHAT: request_authenticated")

        val streamingClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val call = streamingClient.newCall(request)

        try {
            val response: Response = call.execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed HTTP ${response.code}")
                val errorMsg = when (response.code) {
                    401 -> "HTTP 401: Authentication required. Please log in to your account."
                    403 -> "HTTP 403: Access denied. Please check your account permissions."
                    502 -> "HTTP 502: AI service temporarily unavailable. Please try again."
                    else -> "HTTP ${response.code}: $errorBody"
                }
                close(java.io.IOException(errorMsg))
                return@callbackFlow
            }

            android.util.Log.i("ChatApiClient", "ONLINE_CHAT: stream_started")

            val body = response.body
            if (body == null) {
                android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed Empty response body")
                close(java.io.IOException("Empty response body"))
                return@callbackFlow
            }

            var hasLoggedFirstToken = false
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data.isNotEmpty()) {
                        try {
                            val event = json.decodeFromString<StreamEventDto>(data)
                            if (event.type == "error") {
                                android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed ${event.detail}")
                                close(java.io.IOException(event.detail ?: "Streaming error from AI service"))
                                return@callbackFlow
                            }
                            if (event.type == "token" && !hasLoggedFirstToken) {
                                hasLoggedFirstToken = true
                                android.util.Log.i("ChatApiClient", "ONLINE_CHAT: first_token_received")
                            }
                            trySend(event)
                            if (event.type == "done") {
                                android.util.Log.i("ChatApiClient", "ONLINE_CHAT: stream_completed")
                                break
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatApiClient", "Error parsing SSE event: $data", e)
                        }
                    }
                }
                line = reader.readLine()
            }
            close()
        } catch (e: Exception) {
            android.util.Log.e("ChatApiClient", "ONLINE_CHAT: request_failed ${e.message}")
            close(e)
        }

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateConversation(
        conversationId: String,
        title: String? = null,
        isArchived: Boolean? = null,
        isPinned: Boolean? = null,
        draftText: String? = null
    ): Result<Unit> = runCatching {
        val map = mutableMapOf<String, Any?>()
        if (title != null) map["title"] = title
        if (isArchived != null) map["is_archived"] = isArchived
        if (isPinned != null) map["is_pinned"] = isPinned
        if (draftText != null) map["draft_text"] = draftText
        val (code, body) = patch("/chat/conversations/$conversationId", json.encodeToString(map))
        if (code !in 200..299) error("HTTP $code: $body")
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        val (code, body) = delete("/chat/conversations/$conversationId")
        if (code !in 200..299) error("HTTP $code: $body")
    }

    suspend fun syncMessages(messages: List<SyncMessageItemDto>): Result<List<String>> = runCatching {
        val requestBody = json.encodeToString(SyncMessagesRequestDto(messages = messages))
        val (code, body) = post("/chat/sync", requestBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<SyncMessagesResponseDto>(body).syncedIds
    }

    // --- Long-Term Memory APIs ---

    suspend fun listMemories(): Result<List<MemoryDto>> = runCatching {
        val (code, body) = get("/memory")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<MemoryListResponseDto>(body).memories
    }

    suspend fun createMemory(category: String, content: String): Result<MemoryDto> = runCatching {
        val requestBody = json.encodeToString(MemoryCreateRequestDto(category = category, content = content))
        val (code, body) = post("/memory", requestBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<MemoryDto>(body)
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> = runCatching {
        val (code, body) = delete("/memory/$memoryId")
        if (code !in 200..299) error("HTTP $code: $body")
    }

    suspend fun clearAllMemories(): Result<Unit> = runCatching {
        val (code, body) = delete("/memory")
        if (code !in 200..299) error("HTTP $code: $body")
    }

    // --- Courses API ---

    suspend fun getCourses(
        featured: Boolean? = null,
        category: String? = null,
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<CourseDto>> = runCatching {
        val params = mutableListOf<String>()
        if (featured == true) params.add("featured=true")
        if (category != null) params.add("category=$category")
        if (limit != null) params.add("limit=$limit")
        if (offset > 0) params.add("offset=$offset")
        val path = if (params.isEmpty()) "/courses" else "/courses?" + params.joinToString("&")
        val (code, body) = get(path)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<CourseDto>>(body)
    }

    suspend fun getCourseDetail(courseId: String): Result<CourseDetailDto> = runCatching {
        val (code, body) = get("/courses/$courseId")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<CourseDetailDto>(body)
    }

    // --- Roadmaps API ---

    suspend fun getRoadmaps(
        category: String? = null,
        limit: Int? = null,
        offset: Int = 0
    ): Result<List<RoadmapDto>> = runCatching {
        val params = mutableListOf<String>()
        if (category != null) params.add("category=$category")
        if (limit != null) params.add("limit=$limit")
        if (offset > 0) params.add("offset=$offset")
        val path = if (params.isEmpty()) "/roadmaps" else "/roadmaps?" + params.joinToString("&")
        val (code, body) = get(path)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<RoadmapDto>>(body)
    }

    suspend fun getRoadmapDetail(roadmapId: String): Result<RoadmapDetailDto> = runCatching {
        val (code, body) = get("/roadmaps/$roadmapId")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<RoadmapDetailDto>(body)
    }

    suspend fun getRoadmapCategories(): Result<List<String>> = runCatching {
        val (code, body) = get("/roadmaps/categories")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<String>>(body)
    }
}
