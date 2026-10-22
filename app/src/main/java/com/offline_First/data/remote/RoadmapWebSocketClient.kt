package com.offline_First.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID

sealed class RoadmapWsEvent {
    data class SessionReady(
        val sessionId: String,
        val domain: String,
        val greeting: String,
        val currentQuestionJson: JSONObject?,
        val completenessPercentage: Int
    ) : RoadmapWsEvent()

    data class NextQuestion(
        val sessionId: String,
        val feedback: String,
        val nextQuestionJson: JSONObject?,
        val completenessPercentage: Int
    ) : RoadmapWsEvent()

    data class SessionComplete(
        val sessionId: String,
        val message: String,
        val feedback: String,
        val isReadyForGeneration: Boolean
    ) : RoadmapWsEvent()

    data class RoadmapGenerationStarted(
        val sessionId: String,
        val message: String
    ) : RoadmapWsEvent()

    data class RoadmapReady(
        val sessionId: String,
        val roadmapJson: JSONObject
    ) : RoadmapWsEvent()

    data class Error(
        val code: String,
        val message: String
    ) : RoadmapWsEvent()

    data object Connected : RoadmapWsEvent()
    data object Disconnected : RoadmapWsEvent()
}

class RoadmapWebSocketClient(
    private val okHttpClient: OkHttpClient = ChatApiClient.okHttpClient,
    private val tokenProvider: () -> String? = { null },
    private val userIdProvider: () -> String = { "default_user" }
) {
    companion object {
        private const val TAG = "RoadmapWebSocketClient"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null

    private val _events = MutableSharedFlow<RoadmapWsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RoadmapWsEvent> = _events.asSharedFlow()

    @Volatile
    var isConnected: Boolean = false
        private set

    fun connect(baseUrl: String, sessionId: String? = null) {
        if (isConnected) return

        val wsUrl = buildWsUrl(baseUrl, sessionId)
        Log.i(TAG, "Connecting to Roadmap WebSocket: $wsUrl")

        val reqBuilder = Request.Builder().url(wsUrl)
        tokenProvider()?.let { token ->
            if (token.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        webSocket = okHttpClient.newWebSocket(reqBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Roadmap WebSocket onOpen")
                isConnected = true
                scope.launch { _events.emit(RoadmapWsEvent.Connected) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Roadmap WebSocket received: $text")
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Roadmap WebSocket onClosing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Roadmap WebSocket onClosed")
                isConnected = false
                scope.launch { _events.emit(RoadmapWsEvent.Disconnected) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Roadmap WebSocket onFailure: ${t.message}", t)
                isConnected = false
                scope.launch {
                    _events.emit(RoadmapWsEvent.Error(code = "WS_FAILURE", message = t.message ?: "Connection error"))
                    _events.emit(RoadmapWsEvent.Disconnected)
                }
            }
        })
    }

    fun startRoadmap(domain: String, goal: String? = null, level: String? = null, weeklyHours: Float? = null) {
        val payload = JSONObject().apply {
            put("domain", domain)
            goal?.let { put("goal", it) }
            level?.let { put("target_level", it) }
            weeklyHours?.let { put("weekly_hours", it.toDouble()) }
        }
        val eventObj = JSONObject().apply {
            put("event", "start_roadmap")
            put("payload", payload)
        }
        send(eventObj.toString())
    }

    fun submitAnswer(sessionId: String, messageId: String = UUID.randomUUID().toString(), questionId: String?, answerText: String?, quizIndex: Int?) {
        val payload = JSONObject().apply {
            put("message_id", messageId)
            questionId?.let { put("question_id", it) }
            answerText?.let { put("answer_text", it) }
            quizIndex?.let { put("quiz_selected_index", it) }
        }
        val eventObj = JSONObject().apply {
            put("event", "submit_answer")
            put("session_id", sessionId)
            put("payload", payload)
        }
        send(eventObj.toString())
    }

    fun generateRoadmap(sessionId: String) {
        val eventObj = JSONObject().apply {
            put("event", "generate_roadmap")
            put("session_id", sessionId)
            put("payload", JSONObject())
        }
        send(eventObj.toString())
    }

    fun resumeSession(sessionId: String) {
        val eventObj = JSONObject().apply {
            put("event", "resume_session")
            put("session_id", sessionId)
            put("payload", JSONObject())
        }
        send(eventObj.toString())
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
        isConnected = false
    }

    private fun send(jsonText: String) {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "Attempted to send over disconnected WebSocket: $jsonText")
            return
        }
        webSocket?.send(jsonText)
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val eventType = json.optString("event")
            val sessionId = json.optString("session_id")
            val payload = json.optJSONObject("payload") ?: JSONObject()

            val mappedEvent: RoadmapWsEvent? = when (eventType) {
                "session_ready" -> RoadmapWsEvent.SessionReady(
                    sessionId = sessionId,
                    domain = payload.optString("domain", "Python"),
                    greeting = payload.optString("greeting", ""),
                    currentQuestionJson = payload.optJSONObject("currentQuestion"),
                    completenessPercentage = payload.optInt("completenessPercentage", 20)
                )
                "next_question" -> RoadmapWsEvent.NextQuestion(
                    sessionId = sessionId,
                    feedback = payload.optString("feedback", ""),
                    nextQuestionJson = payload.optJSONObject("nextQuestion"),
                    completenessPercentage = payload.optInt("completenessPercentage", 50)
                )
                "session_complete" -> RoadmapWsEvent.SessionComplete(
                    sessionId = sessionId,
                    message = payload.optString("message", "Assessment complete"),
                    feedback = payload.optString("feedback", ""),
                    isReadyForGeneration = payload.optBoolean("isReadyForGeneration", true)
                )
                "roadmap_generation_started" -> RoadmapWsEvent.RoadmapGenerationStarted(
                    sessionId = sessionId,
                    message = payload.optString("message", "Synthesizing roadmap...")
                )
                "roadmap_ready" -> {
                    val rMap = payload.optJSONObject("roadmap") ?: JSONObject()
                    RoadmapWsEvent.RoadmapReady(
                        sessionId = sessionId,
                        roadmapJson = rMap
                    )
                }
                "error" -> RoadmapWsEvent.Error(
                    code = payload.optString("code", "ERROR"),
                    message = payload.optString("message", "Server error")
                )
                else -> null
            }

            mappedEvent?.let { ev ->
                scope.launch { _events.emit(ev) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming WebSocket JSON: ${e.message}", e)
        }
    }

    private fun buildWsUrl(httpBaseUrl: String, sessionId: String?): String {
        val wsBase = httpBaseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        val cleanBase = wsBase.trimEnd('/')
        val uId = userIdProvider()

        val queryParams = mutableListOf<String>()
        if (uId.isNotBlank()) {
            queryParams.add("user_id=$uId")
        }
        if (!sessionId.isNullOrBlank()) {
            queryParams.add("session_id=$sessionId")
        }

        val qs = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "$cleanBase/api/v1/roadmaps/ws/personalized$qs"
    }
}
