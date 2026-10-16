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
import com.offline_First.domain.model.PersonalizedRoadmapDetail

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

object ChatApiClient {

    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 1000L

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(AppDns)
        .connectTimeout(ChatApiConfig.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private fun isEmulator(): Boolean {
        val fp = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        return fp.startsWith("generic") || fp.startsWith("unknown")
                || model.contains("google_sdk") || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || brand.startsWith("generic") || device.startsWith("generic")
                || product == "google_sdk" || product.contains("sdk_gphone")
    }

    private fun buildUrl(path: String): String {
        val base = ChatApiConfig.BASE_URL
        return "$base${ChatApiConfig.API_PREFIX}$path"
    }

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

    // --- Assessment API ---

    suspend fun validateSubject(subject: String): Result<ValidateSubjectResponseDto> = runCatching {
        val reqBody = json.encodeToString(ValidateSubjectRequestDto(subject))
        val (code, body) = post("/assessment/validate-subject", reqBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<ValidateSubjectResponseDto>(body)
    }

    suspend fun startAssessment(domain: String = "Python"): Result<AssessmentSessionDto> = runCatching {
        val encodedDomain = java.net.URLEncoder.encode(domain, "UTF-8")
        val (code, body) = post("/assessment/start?domain=$encodedDomain")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<AssessmentSessionDto>(body)
    }

    suspend fun getActiveAssessmentSession(): Result<AssessmentSessionDto?> = runCatching {
        val (code, body) = get("/assessment/active")
        if (code == 404) return@runCatching null
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<AssessmentSessionDto>(body)
    }

    suspend fun getAssessmentSession(sessionId: String): Result<AssessmentSessionDto> = runCatching {
        val (code, body) = get("/assessment/$sessionId")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<AssessmentSessionDto>(body)
    }

    suspend fun submitAssessmentAnswer(
        sessionId: String,
        answer: String,
        quizSelectedIndex: Int?
    ): Result<AssessmentSessionDto> = runCatching {
        val reqBody = json.encodeToString(SubmitAnswerRequestDto(answer, quizSelectedIndex))
        val (code, body) = post("/assessment/$sessionId/answer", reqBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<AssessmentSessionDto>(body)
    }

    suspend fun correctProfileField(
        sessionId: String,
        field: String,
        value: String
    ): Result<AssessmentSessionDto> = runCatching {
        val reqBody = json.encodeToString(CorrectProfileFieldRequestDto(field, value))
        val (code, body) = post("/assessment/$sessionId/correct", reqBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<AssessmentSessionDto>(body)
    }

    // --- Personalized Roadmap API ---

    suspend fun generatePersonalizedRoadmap(sessionId: String): Result<PersonalizedRoadmapDetailDto> = runCatching {
        val (code, body) = post("/assessment/$sessionId/generate-roadmap")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<PersonalizedRoadmapDetailDto>(body)
    }

    suspend fun getMyPersonalizedRoadmaps(
        limit: Int?,
        offset: Int
    ): Result<List<PersonalizedRoadmapDetailDto>> = runCatching {
        val params = buildString {
            append("?offset=$offset")
            if (limit != null) append("&limit=$limit")
        }
        val (code, body) = get("/personalized-roadmaps$params")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<List<PersonalizedRoadmapDetailDto>>(body)
    }

    suspend fun getPersonalizedRoadmapDetail(roadmapId: String): Result<PersonalizedRoadmapDetailDto> = runCatching {
        val (code, body) = get("/personalized-roadmaps/$roadmapId")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<PersonalizedRoadmapDetailDto>(body)
    }

    suspend fun getTopicLearningContent(roadmapId: String, topicId: String): Result<TopicLearningContentDto> = runCatching {
        val (code, body) = get("/personalized-roadmaps/$roadmapId/topics/$topicId/content")
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<TopicLearningContentDto>(body)
    }

    suspend fun completeTopic(
        roadmapId: String,
        topicId: String,
        quizPassed: Boolean = true
    ): Result<CompleteTopicResponseDto> = runCatching {
        val reqBody = json.encodeToString(CompleteTopicRequestDto(quizPassed = quizPassed))
        val (code, body) = post("/personalized-roadmaps/$roadmapId/topics/$topicId/complete", reqBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<CompleteTopicResponseDto>(body)
    }

    suspend fun savePersonalizedRoadmap(roadmap: PersonalizedRoadmapDetail): Result<PersonalizedRoadmapDetailDto> = runCatching {
        val reqBody = json.encodeToString(roadmap.toDto())
        val (code, body) = post("/personalized-roadmaps", reqBody)
        if (code !in 200..299) error("HTTP $code: $body")
        json.decodeFromString<PersonalizedRoadmapDetailDto>(body)
    }

    suspend fun deletePersonalizedRoadmap(roadmapId: String): Result<Unit> = runCatching {
        val url = buildUrl("/personalized-roadmaps/$roadmapId")
        android.util.Log.d("ChatApiClient", "DELETE request: $url")
        retryOnNetworkError {
            val request = okhttp3.Request.Builder()
                .url(url)
                .delete()
                .addHeader("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code !in 200..299) error("HTTP ${response.code}")
            }
        }
    }

    suspend fun renamePersonalizedRoadmap(
        roadmapId: String,
        newTitle: String
    ): Result<PersonalizedRoadmapDetailDto> = runCatching {
        val reqBody = json.encodeToString(RenameRoadmapRequestDto(newTitle))
        val url = buildUrl("/personalized-roadmaps/$roadmapId/rename")
        android.util.Log.d("ChatApiClient", "PATCH request: $url")
        retryOnNetworkError {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = reqBody.toRequestBody(mediaType)
            val request = okhttp3.Request.Builder()
                .url(url)
                .patch(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()
                if (code !in 200..299) error("HTTP $code: $body")
                json.decodeFromString<PersonalizedRoadmapDetailDto>(body)
            }
        }
    }
}

// Converts domain object to DTO for PUT/POST payloads
private fun PersonalizedRoadmapDetail.toDto() = PersonalizedRoadmapDetailDto(
    id = id,
    title = title,
    domain = domain,
    goal = goal,
    level = level,
    studyHoursPerWeek = studyHoursPerWeek,
    estimatedWeeks = estimatedWeeks,
    topics = topics.map { topic ->
        PersonalizedRoadmapTopicDto(
            id = topic.id,
            topicTag = topic.topicTag,
            title = topic.title,
            shortDescription = topic.shortDescription,
            estimatedMinutes = topic.estimatedMinutes,
            prerequisiteTopicIds = topic.prerequisiteTopicIds,
            status = when (topic.status) {
                com.offline_First.domain.model.PersonalizedTopicStatus.UNLOCKED -> "unlocked"
                com.offline_First.domain.model.PersonalizedTopicStatus.IN_PROGRESS -> "in_progress"
                com.offline_First.domain.model.PersonalizedTopicStatus.COMPLETED -> "completed"
                com.offline_First.domain.model.PersonalizedTopicStatus.SKIPPED -> "skipped"
                else -> "locked"
            },
            masteryLevel = when (topic.masteryLevel) {
                com.offline_First.domain.model.TopicMasteryLevel.WEAK -> "weak"
                com.offline_First.domain.model.TopicMasteryLevel.ADEQUATE -> "adequate"
                com.offline_First.domain.model.TopicMasteryLevel.STRONG -> "strong"
                else -> "unknown"
            },
            priorityScore = topic.priorityScore,
            accelerated = topic.accelerated,
            explanation = topic.explanation
        )
    },
    edges = edges.map { PersonalizedRoadmapEdgeDto(it.fromTopicId, it.toTopicId) },
    assessmentSessionId = assessmentSessionId,
    completedTopicIds = completedTopicIds.toList(),
    inProgressTopicIds = inProgressTopicIds.toList()
)
