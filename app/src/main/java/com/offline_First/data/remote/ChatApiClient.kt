package com.offline_First.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

object ChatApiClient {

    private const val MAX_RETRIES = 5
    private const val RETRY_DELAY_MS = 5000L

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(AppDns)
        .connectTimeout(ChatApiConfig.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private fun buildUrl(path: String): String =
        "${ChatApiConfig.BASE_URL}${ChatApiConfig.API_PREFIX}$path"

    /**
     * Retries the block up to MAX_RETRIES times on transient network errors
     * (UnknownHostException, SocketTimeoutException, ConnectException).
     * This handles Render free-tier cold starts where the first request may fail
     * before the server finishes waking up.
     */
    private suspend fun <T> retryOnNetworkError(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: java.net.UnknownHostException) {
                lastError = e
                android.util.Log.w("ChatApiClient", "Attempt ${attempt + 1}/$MAX_RETRIES failed (UnknownHost — possible Render cold-start or DNS delay), retrying...")
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
            // Exponential back-off capped at 15 s: 5s, 10s, 15s, 15s, …
            if (attempt < MAX_RETRIES - 1) {
                val waitMs = (RETRY_DELAY_MS * (attempt + 1)).coerceAtMost(15_000L)
                android.util.Log.d("ChatApiClient", "Waiting ${waitMs}ms before attempt ${attempt + 2}…")
                delay(waitMs)
            }
        }
        throw lastError!!
    }

    private suspend fun get(path: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        android.util.Log.d("ChatApiClient", "GET request: $url")
        retryOnNetworkError {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()
                android.util.Log.d("ChatApiClient", "GET $url -> code $code")
                Pair(code, body)
            }
        }
    }

    private suspend fun post(path: String, bodyJson: String = ""): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path)
            android.util.Log.d("ChatApiClient", "POST request: $url")
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
                    val code = response.code
                    val body = response.body?.string().orEmpty()
                    android.util.Log.d("ChatApiClient", "POST $url -> code $code")
                    Pair(code, body)
                }
            }
        }

    // --- Public API ---

    suspend fun createConversation(): Result<ConversationDto> = runCatching {
        val (code, body) = post("/chat/conversations")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<ConversationDto>(body)
    }

    suspend fun listConversations(): Result<List<ConversationSummaryDto>> = runCatching {
        val (code, body) = get("/chat/conversations")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<ConversationSummaryDto>>(body)
    }

    suspend fun getConversation(id: String): Result<ConversationDetailDto> = runCatching {
        val (code, body) = get("/chat/conversations/$id")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<ConversationDetailDto>(body)
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        explanationMode: String
    ): Result<SendMessageResponseDto> = runCatching {
        val requestBody = json.encodeToString(
            SendMessageRequestDto(content = content, explanationMode = explanationMode)
        )
        val (code, body) = post("/chat/conversations/$conversationId/messages", requestBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<SendMessageResponseDto>(body)
    }

    // --- Courses API ---

    suspend fun getCourses(featured: Boolean? = null): Result<List<CourseDto>> = runCatching {
        val path = if (featured == true) "/courses?featured=true" else "/courses"
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

    suspend fun getRoadmaps(): Result<List<RoadmapDto>> = runCatching {
        val (code, body) = get("/roadmaps")
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
