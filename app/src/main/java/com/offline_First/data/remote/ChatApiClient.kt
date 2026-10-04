package com.offline_First.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

object ChatApiClient {

    private fun buildUrl(path: String): URL =
        URL("${ChatApiConfig.BASE_URL}${ChatApiConfig.API_PREFIX}$path")

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = ChatApiConfig.CONNECT_TIMEOUT_MS
        conn.readTimeout = ChatApiConfig.READ_TIMEOUT_MS
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream ?: conn.inputStream
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private suspend fun get(path: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        android.util.Log.d("ChatApiClient", "GET request: $url")
        val conn = openConnection(url, "GET")
        try {
            conn.connect()
            val code = conn.responseCode
            val body = readResponse(conn)
            android.util.Log.d("ChatApiClient", "GET $url -> code $code")
            Pair(code, body)
        } catch (e: Exception) {
            android.util.Log.e("ChatApiClient", "GET $url failed with ${e.javaClass.simpleName}: ${e.message}")
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun post(path: String, bodyJson: String = ""): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path)
            android.util.Log.d("ChatApiClient", "POST request: $url")
            val conn = openConnection(url, "POST")
            conn.doOutput = true
            try {
                conn.connect()
                if (bodyJson.isNotEmpty()) {
                    conn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val body = readResponse(conn)
                android.util.Log.d("ChatApiClient", "POST $url -> code $code")
                Pair(code, body)
            } catch (e: Exception) {
                android.util.Log.e("ChatApiClient", "POST $url failed with ${e.javaClass.simpleName}: ${e.message}")
                throw e
            } finally {
                conn.disconnect()
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
}
