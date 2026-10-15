package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatApiClientAuthTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authenticatedApiClient: AuthenticatedApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens("test_online_access_token", "test_online_refresh_token")
        sessionManager = SessionManager(tokenStorage)
        val publicClient = OkHttpClient.Builder().build()
        authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )
        ChatApiClient.initialize(authenticatedApiClient.okHttpClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun onlineStreamMessageIncludesAuthorizationHeaderAndReceivesSseWithout401() = runBlocking {
        // Enqueue SSE streaming response with tokens
        val sseBody = """
            data: {"type":"metadata","message_id":"assistant-msg-1"}

            data: {"type":"token","content":"Database "}

            data: {"type":"token","content":"Management "}

            data: {"type":"token","content":"System"}

            data: {"type":"done"}

        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        // Using direct call through ChatApiClient's okHttpClient to the mock server URL
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = okhttp3.Request.Builder()
            .url(server.url("/api/v1/chat/conversations/conv-123/messages/stream"))
            .post(
                """{"content":"What is DBMS","explanation_mode":"general"}"""
                    .toRequestBody(mediaType)
            )
            .addHeader("Accept", "text/event-stream")
            .build()

        val streamingClient = ChatApiClient.okHttpClient.newBuilder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        val response = streamingClient.newCall(request).execute()
        assertEquals(200, response.code)
        assertTrue(response.isSuccessful)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/chat/conversations/conv-123/messages/stream", recorded.path)
        assertEquals("Bearer test_online_access_token", recorded.getHeader("Authorization"))
        assertEquals("text/event-stream", recorded.getHeader("Accept"))
        assertNotNull(recorded.getHeader("X-Correlation-ID"))

        response.close()
    }

    @Test
    fun authenticatedChatApiClientAttachesBearerTokenToNormalApiRequests() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"conv-123","title":"New Conversation","created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z","is_archived":false,"is_pinned":false,"draft_text":""}""")
        )

        val request = okhttp3.Request.Builder()
            .url(server.url("/api/v1/chat/conversations"))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        val response = ChatApiClient.okHttpClient.newCall(request).execute()
        assertEquals(200, response.code)

        val recorded = server.takeRequest()
        assertEquals("Bearer test_online_access_token", recorded.getHeader("Authorization"))
        assertNotNull(recorded.getHeader("X-Correlation-ID"))
        response.close()
    }
}
