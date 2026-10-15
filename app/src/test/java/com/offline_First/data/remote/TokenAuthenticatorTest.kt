package com.offline_First.data.remote

import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
        sessionManager = SessionManager(tokenStorage)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun refresh_success_updates_storage_and_retries_with_new_token() {
        tokenStorage.saveTokens("expired_access_token", "valid_refresh_token")

        val publicClient = OkHttpClient.Builder().build()
        val authenticator = TokenAuthenticator(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .authenticator(authenticator)
            .build()

        // 1. First call to protected endpoint -> returns 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        // 2. Refresh call -> returns new tokens
        val refreshJson = """
            {
                "accessToken": "new_access_token",
                "refreshToken": "new_refresh_token",
                "tokenType": "Bearer",
                "expiresIn": 1800,
                "user": {
                    "id": "u1",
                    "email": "u1@example.com",
                    "fullName": "User One"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(refreshJson))
        // 3. Retry of protected endpoint -> returns 200
        server.enqueue(MockResponse().setResponseCode(200).setBody("Protected Data"))

        val req = Request.Builder().url(server.url("/api/v1/profile")).build()
        val resp = authenticatedClient.newCall(req).execute()

        assertEquals(200, resp.code)
        assertEquals("new_access_token", tokenStorage.getAccessToken())
        assertEquals("new_refresh_token", tokenStorage.getRefreshToken())
    }

    @Test
    fun refresh_failure_marks_session_expired() {
        tokenStorage.saveTokens("expired_access_token", "invalid_refresh_token")

        val publicClient = OkHttpClient.Builder().build()
        val authenticator = TokenAuthenticator(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { publicClient },
            baseUrl = server.url("").toString().removeSuffix("/")
        )

        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .authenticator(authenticator)
            .build()

        // 1. Protected call -> 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        // 2. Refresh call -> 401 (invalid refresh token)
        server.enqueue(MockResponse().setResponseCode(401).setBody("Invalid token"))

        val req = Request.Builder().url(server.url("/api/v1/profile")).build()
        val resp = authenticatedClient.newCall(req).execute()

        assertEquals(401, resp.code)
        assertTrue(sessionManager.authState.value is AuthState.SessionExpired)
    }
}
