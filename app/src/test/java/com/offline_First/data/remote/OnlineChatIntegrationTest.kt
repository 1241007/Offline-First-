package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests covering the complete Online Chat client requirements:
 * 1. Correct authenticated request (Bearer token + Correlation ID)
 * 2. Correct endpoint path and parameters
 * 3. SSE response parsing (metadata, token, done)
 * 4. Token refresh behavior on 401
 * 5. 401 handling when refresh fails
 * 6. Gemini error event handling
 * 7. Streaming cancellation
 * 8. Successful online response
 * 9. Network failure handling
 * 10. Backend failure (500) handling
 */
class OnlineChatIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authenticatedApiClient: AuthenticatedApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens("test_access_token_123", "test_refresh_token_456")
        sessionManager = SessionManager(tokenStorage)

        val publicClient = OkHttpClient.Builder().build()
        authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        ChatApiClient.initialize(authenticatedApiClient.okHttpClient)
        ChatApiClient.customBaseUrl = server.url("").toString().removeSuffix("/")
    }

    @After
    fun tearDown() {
        ChatApiClient.customBaseUrl = null
        server.shutdown()
    }

    @Test
    fun test1_correctAuthenticatedRequest() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"done\"}\n\n")
        )

        ChatApiClient.streamMessage("conv-1", "Test message").toList()

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull("Request should reach server", request)
        assertEquals("Bearer test_access_token_123", request!!.getHeader("Authorization"))
        assertNotNull("Correlation ID must be present", request.getHeader("X-Correlation-ID"))
        assertTrue("Correlation ID must not be empty", request.getHeader("X-Correlation-ID")!!.isNotBlank())
    }

    @Test
    fun test2_correctEndpointPathAndBody() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"done\"}\n\n")
        )

        ChatApiClient.streamMessage(
            conversationId = "conv-abc-789",
            content = "Explain algorithms",
            explanationMode = "teacher",
            clientMessageId = "client-msg-1",
            parentId = "parent-msg-0"
        ).toList()

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("POST", request!!.method)
        assertEquals("/api/v1/chat/conversations/conv-abc-789/messages/stream", request.path)
        val body = request.body.readUtf8()
        assertTrue("Request body contains prompt content", body.contains("\"content\":\"Explain algorithms\""))
        assertTrue("Request body contains explanation_mode", body.contains("\"explanation_mode\":\"teacher\""))
    }

    @Test
    fun test3_sseResponseParsing() = runBlocking {
        val ssePayload = """
            data: {"type":"metadata","assistant_message_id":"msg-42"}

            data: {"type":"token","content":"Quantum "}

            data: {"type":"token","content":"Computing"}

            data: {"type":"done"}

        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(ssePayload)
        )

        val events = ChatApiClient.streamMessage("conv-1", "Hello").toList()

        assertEquals(4, events.size)
        assertEquals("metadata", events[0].type)
        assertEquals("msg-42", events[0].assistantMessageId)

        assertEquals("token", events[1].type)
        assertEquals("Quantum ", events[1].content)

        assertEquals("token", events[2].type)
        assertEquals("Computing", events[2].content)

        assertEquals("done", events[3].type)
    }

    @Test
    fun test4_tokenRefreshBehaviorOn401() = runBlocking {
        // 1. Initial stream request returns 401
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )
        // 2. Token refresh endpoint returns new tokens
        val refreshResponse = """
            {
                "accessToken": "refreshed_access_token_999",
                "refreshToken": "refreshed_refresh_token_888",
                "tokenType": "Bearer",
                "expiresIn": 1800,
                "user": {
                    "id": "u1",
                    "email": "u1@example.com",
                    "fullName": "User One"
                }
            }
        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(refreshResponse)
        )
        // 3. Retried stream request returns 200 with tokens
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"token\",\"content\":\"Success after refresh\"}\n\ndata: {\"type\":\"done\"}\n\n")
        )

        val events = ChatApiClient.streamMessage("conv-1", "Refresh test").toList()

        assertEquals(2, events.size)
        assertEquals("Success after refresh", events[0].content)

        // Verify storage received new tokens
        assertEquals("refreshed_access_token_999", tokenStorage.getAccessToken())
        assertEquals("refreshed_refresh_token_888", tokenStorage.getRefreshToken())

        // Verify retried request had new token
        server.takeRequest() // 1st stream
        server.takeRequest() // refresh
        val retried = server.takeRequest() // retried stream
        assertEquals("Bearer refreshed_access_token_999", retried.getHeader("Authorization"))
    }

    @Test
    fun test5_401HandlingWhenRefreshFails() = runBlocking {
        // Initial request returns 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        // Refresh request also fails with 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("Refresh token expired"))

        try {
            ChatApiClient.streamMessage("conv-1", "Should fail auth").toList()
            fail("Expected exception when refresh fails")
        } catch (e: Exception) {
            // Should fail with HTTP 401
            assertTrue(e.message?.contains("401") == true || e is IllegalStateException)
        }
    }

    @Test
    fun test6_geminiErrorEventHandling() = runBlocking {
        val ssePayload = """
            data: {"type":"error","detail":"Gemini rate limit exceeded (429)"}

        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(ssePayload)
        )

        try {
            ChatApiClient.streamMessage("conv-1", "Trigger error").toList()
            fail("Expected stream to fail when error event received")
        } catch (e: Exception) {
            assertTrue(
                "Error message should contain detail from server: ${e.message}",
                e.message?.contains("Gemini rate limit exceeded") == true
            )
        }
    }

    @Test
    fun test7_streamingCancellation() = runBlocking {
        // Streaming multiple tokens
        val ssePayload = """
            data: {"type":"token","content":"Chunk 1"}

            data: {"type":"token","content":"Chunk 2"}

            data: {"type":"token","content":"Chunk 3"}

            data: {"type":"done"}

        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(ssePayload)
        )

        // Collect only the first token then cancel
        val firstToken = ChatApiClient.streamMessage("conv-1", "Cancel test")
            .take(1)
            .first()

        assertEquals("Chunk 1", firstToken.content)
    }

    @Test
    fun test8_successfulOnlineResponseViaSendMessage() = runBlocking {
        val responseJson = """
            {
                "user_message": {
                    "id": "u-msg-1",
                    "conversation_id": "conv-1",
                    "role": "user",
                    "content": "Hello assistant",
                    "created_at": "2026-01-01T00:00:00Z"
                },
                "assistant_message": {
                    "id": "a-msg-1",
                    "conversation_id": "conv-1",
                    "role": "assistant",
                    "content": "EduNova AI Assistant",
                    "created_at": "2026-01-01T00:00:00Z"
                }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val result = ChatApiClient.sendMessage("conv-1", "Hello assistant")
        assertTrue("sendMessage should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("EduNova AI Assistant", response.assistantMessage.content)
        assertEquals("assistant", response.assistantMessage.role)
    }

    @Test
    fun test9_networkFailureHandling() = runBlocking {
        // Disconnect immediately to simulate network drop
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        try {
            ChatApiClient.streamMessage("conv-1", "Network fail").toList()
            fail("Expected network failure exception")
        } catch (e: Exception) {
            // Should catch network error (IOException or IllegalStateException)
            assertNotNull(e)
        }
    }

    @Test
    fun test10_backendFailure500Handling() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"detail":"Internal Server Error in GeminiService"}""")
        )

        try {
            ChatApiClient.streamMessage("conv-1", "Server 500").toList()
            fail("Expected failure on 500 status code")
        } catch (e: Exception) {
            assertTrue("Exception message mentions 500: ${e.message}", e.message?.contains("500") == true)
        }
    }
}
