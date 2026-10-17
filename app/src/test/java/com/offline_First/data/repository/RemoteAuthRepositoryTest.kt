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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.Request

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
        assertFalse("Online sign-in must not be offline", authState.isOffline)
        assertEquals("access_token_1", tokenStorage.getAccessToken())
        assertEquals("refresh_token_1", tokenStorage.getRefreshToken())

        // Cached identity must be stored
        val cachedIdentity = tokenStorage.getUserIdentity()
        assertNotNull(cachedIdentity)
        assertEquals("u1", cachedIdentity?.userId)
        assertEquals("tester@example.com", cachedIdentity?.email)
    }

    @Test
    fun logout_clears_session_even_on_network_failure() = runTest {
        tokenStorage.saveTokens("token_to_clear", "rt_to_clear")
        tokenStorage.saveUserIdentity("u_clear", "clear@example.com", "Clear User")
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        val result = repository.logout("rt_to_clear")
        assertTrue(result.isSuccess)

        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(tokenStorage.getUserIdentity())
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun restoreSession_online_success_refreshes_and_fetches_me() = runTest {
        tokenStorage.saveTokens("old_access", "valid_refresh")
        tokenStorage.saveUserIdentity("u1", "u1@example.com", "Old Name")

        val refreshResponse = """
            {
                "accessToken": "new_access_token_88",
                "refreshToken": "new_refresh_token_99",
                "tokenType": "Bearer",
                "expiresIn": 1800,
                "user": {
                    "id": "u1",
                    "email": "u1@example.com",
                    "fullName": "Updated Name"
                }
            }
        """.trimIndent()
        val meResponse = """
            {
                "id": "u1",
                "email": "u1@example.com",
                "fullName": "Authoritative Full Name",
                "role": "student"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponse))
        server.enqueue(MockResponse().setResponseCode(200).setBody(meResponse))

        val result = repository.restoreSession()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val state = sessionManager.authState.value
        assertTrue(state is AuthState.Authenticated)
        val authState = state as AuthState.Authenticated
        assertEquals("u1", authState.userId)
        assertEquals("Authoritative Full Name", authState.fullName)
        assertFalse("Online restored session is not offline", authState.isOffline)
        assertEquals("new_access_token_88", tokenStorage.getAccessToken())
        assertEquals("new_refresh_token_99", tokenStorage.getRefreshToken())
    }

    @Test
    fun restoreSession_offline_with_cached_identity_restores_restricted_state() = runTest {
        // User logged in earlier and cached identity
        tokenStorage.saveTokens("cached_access_token", "cached_refresh_token")
        tokenStorage.saveUserIdentity("u_offline", "offline@example.com", "Offline Learner")

        // Offline repository with unreachable port
        val offlineRepo = RemoteAuthRepository(
            authApiClient = AuthApiClient(baseUrl = "http://127.0.0.1:59999"),
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999"
        )

        val result = offlineRepo.restoreSession()
        assertTrue("Restoration must succeed offline when valid cached identity exists", result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val state = sessionManager.authState.value
        assertTrue("State must be Authenticated", state is AuthState.Authenticated)
        val authState = state as AuthState.Authenticated
        assertEquals("u_offline", authState.userId)
        assertEquals("offline@example.com", authState.email)
        assertEquals("Offline Learner", authState.fullName)
        assertTrue("Restored offline session must have isOffline = true", authState.isOffline)

        // Tokens and identity must NOT be wiped by network failure
        assertEquals("cached_access_token", tokenStorage.getAccessToken())
        assertEquals("cached_refresh_token", tokenStorage.getRefreshToken())
        assertEquals("u_offline", tokenStorage.getUserIdentity()?.userId)
    }

    @Test
    fun restoreSession_explicit_401_rejection_expires_session() = runTest {
        tokenStorage.saveTokens("expired_access", "invalid_refresh")
        tokenStorage.saveUserIdentity("u_rejected", "rejected@example.com", "Rejected User")

        // Server explicitly rejects the refresh token with HTTP 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"AUTH_FAILED","message":"Invalid refresh token"}"""))

        val result = repository.restoreSession()
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())

        assertEquals(AuthState.SessionExpired, sessionManager.authState.value)
        assertNull("Tokens must be cleared on explicit 401 expiration", tokenStorage.getAccessToken())
        assertNull("Refresh token must be cleared on explicit 401 expiration", tokenStorage.getRefreshToken())
        assertNull("Cached identity must be cleared on explicit 401 expiration", tokenStorage.getUserIdentity())
    }

    @Test
    fun restoreSession_missing_identity_and_offline_does_not_create_fake_user() = runTest {
        // Refresh token exists, but NO cached identity exists
        tokenStorage.saveTokens("some_access", "some_refresh")
        tokenStorage.clearUserIdentity()

        val offlineRepo = RemoteAuthRepository(
            authApiClient = AuthApiClient(baseUrl = "http://127.0.0.1:59999"),
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999"
        )

        val result = offlineRepo.restoreSession()
        assertTrue(result.isSuccess)
        assertEquals("Must return false when identity is missing and network is offline", false, result.getOrNull())
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun account_switching_preserves_user_isolation() = runTest {
        // 1. User Alice signs in
        sessionManager.onLoginSuccess("token_alice", "refresh_alice", "user_alice", "alice@test.com", "Alice")
        assertEquals("user_alice", (sessionManager.authState.value as AuthState.Authenticated).userId)
        assertEquals("user_alice", tokenStorage.getUserIdentity()?.userId)

        // 2. Alice logs out (mock server responds 200 for logout)
        server.enqueue(MockResponse().setResponseCode(200))
        repository.logout("refresh_alice")
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
        assertNull(tokenStorage.getUserIdentity())

        // 3. User Bob signs in
        sessionManager.onLoginSuccess("token_bob", "refresh_bob", "user_bob", "bob@test.com", "Bob")
        assertEquals("user_bob", (sessionManager.authState.value as AuthState.Authenticated).userId)
        assertEquals("user_bob", tokenStorage.getUserIdentity()?.userId)

        // 4. Server is offline, Bob reopens the app
        val offlineRepo = RemoteAuthRepository(
            authApiClient = AuthApiClient(baseUrl = "http://127.0.0.1:59999"),
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999"
        )
        val result = offlineRepo.restoreSession()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val restoredState = sessionManager.authState.value as AuthState.Authenticated
        assertEquals("user_bob", restoredState.userId)
        assertFalse("Bob must not be Alice", restoredState.userId == "user_alice")
    }

    @Test
    fun offline_state_does_not_authorize_remote_api_without_valid_credentials() {
        // User restored in offline state, but token is invalid
        tokenStorage.saveTokens("invalid_token_999", "invalid_refresh_999")
        sessionManager.onRestoreOfflineSession("user_offline", "offline@test.com", "Offline User")

        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("Refresh Rejected"))

        val req = Request.Builder().url(server.url("/api/v1/protected")).build()
        val resp = authenticatedApiClient.okHttpClient.newCall(req).execute()

        // Remote API call must NOT be authorized
        assertEquals(401, resp.code)
    }

    @Test
    fun signIn_offline_succeeds_for_previously_authenticated_user() = runTest {
        tokenStorage.saveUserIdentity("user_cached_123", "cached_user@test.com", "Cached User")
        val offlineRepo = RemoteAuthRepository(
            authApiClient = authApiClient,
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999",
            isOnlineProvider = { false }
        )

        val result = offlineRepo.signIn("cached_user@test.com", "any_password")
        assertTrue("Offline sign in must succeed for matching cached user", result.isSuccess)

        val state = sessionManager.authState.value
        assertTrue(state is AuthState.Authenticated)
        val authState = state as AuthState.Authenticated
        assertEquals("user_cached_123", authState.userId)
        assertEquals("cached_user@test.com", authState.email)
        assertTrue("Offline sign in must be marked isOffline", authState.isOffline)
    }

    @Test
    fun signIn_offline_fails_for_unknown_user() = runTest {
        tokenStorage.saveUserIdentity("user_cached_123", "cached_user@test.com", "Cached User")
        val offlineRepo = RemoteAuthRepository(
            authApiClient = authApiClient,
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999",
            isOnlineProvider = { false }
        )

        val result = offlineRepo.signIn("stranger@test.com", "any_password")
        assertTrue("Offline sign in must fail for non-cached user", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No internet connection") == true)
    }

    @Test
    fun restoreSession_fast_path_when_offline() = runTest {
        tokenStorage.saveUserIdentity("user_fast_offline", "fast@test.com", "Fast Offline")
        val offlineRepo = RemoteAuthRepository(
            authApiClient = authApiClient,
            authenticatedApiClient = authenticatedApiClient,
            sessionManager = sessionManager,
            tokenStorage = tokenStorage,
            baseUrl = "http://127.0.0.1:59999",
            isOnlineProvider = { false }
        )

        val result = offlineRepo.restoreSession()
        assertTrue("Fast-path restoreSession must succeed offline", result.isSuccess)
        assertTrue(result.getOrNull() == true)
        val state = sessionManager.authState.value as AuthState.Authenticated
        assertEquals("user_fast_offline", state.userId)
        assertTrue(state.isOffline)
    }
}
