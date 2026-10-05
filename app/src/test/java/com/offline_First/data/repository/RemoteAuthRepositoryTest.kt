package com.offline_First.data.repository

import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.InMemoryTokenStorage
import com.offline_First.data.remote.AuthApiClient
import com.offline_First.data.remote.AuthenticatedApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteAuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authApiClient: AuthApiClient
    private lateinit var authenticatedApiClient: AuthenticatedApiClient
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val baseUrl = server.url("").toString().removeSuffix("/")
        tokenStorage = InMemoryTokenStorage()
        sessionManager = SessionManager(tokenStorage)
        authApiClient = AuthApiClient(baseUrl = baseUrl)
        authenticatedApiClient = AuthenticatedApiClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            publicClient = { OkHttpClient() },
            baseUrl = baseUrl
        )

        repository = RemoteAuthRepository(
            authApiClient = authApiClient,
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = baseUrl
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun signIn_success_updates_session() = runTest {
        val loginResponse = """
            {
                "accessToken": "access_token_1",
                "refreshToken": "refresh_token_1",
                "tokenType": "Bearer",
                "expiresIn": 1800,
                "user": {
                    "id": "u1",
                    "email": "tester@example.com",
                    "fullName": "Tester One"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse))

        val result = repository.signIn("tester@example.com", "Password123!")
        assertTrue(result.isSuccess)

        val state = sessionManager.authState.value
        assertTrue(state is AuthState.Authenticated)
        val authState = state as AuthState.Authenticated
        assertEquals("u1", authState.userId)
        assertEquals("Tester One", authState.fullName)
        assertEquals("access_token_1", tokenStorage.getAccessToken())
        assertEquals("refresh_token_1", tokenStorage.getRefreshToken())
    }

    @Test
    fun logout_clears_session_even_on_network_failure() = runTest {
        tokenStorage.saveTokens("token_to_clear", "rt_to_clear")
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        val result = repository.logout("rt_to_clear")
        assertTrue(result.isSuccess)

        assertEquals(null, tokenStorage.getAccessToken())
        assertEquals(null, tokenStorage.getRefreshToken())
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }
}
