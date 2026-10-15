package com.offline_First.data

import com.offline_First.data.local.InMemoryTokenStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        tokenStorage = InMemoryTokenStorage()
        sessionManager = SessionManager(tokenStorage)
    }

    @Test
    fun initial_state_is_initializing() {
        assertEquals(AuthState.Initializing, sessionManager.authState.value)
    }

    @Test
    fun onLoginSuccess_transitions_to_authenticated_and_saves_tokens() {
        sessionManager.onLoginSuccess(
            accessToken = "access_token_abc",
            refreshToken = "refresh_token_xyz",
            userId = "user_1",
            email = "user1@example.com",
            fullName = "User One"
        )

        val state = sessionManager.authState.value
        assertTrue(state is AuthState.Authenticated)
        val authState = state as AuthState.Authenticated
        assertEquals("user_1", authState.userId)
        assertEquals("user1@example.com", authState.email)
        assertEquals("User One", authState.fullName)

        assertEquals("access_token_abc", tokenStorage.getAccessToken())
        assertEquals("refresh_token_xyz", tokenStorage.getRefreshToken())
    }

    @Test
    fun logout_clears_tokens_and_transitions_to_unauthenticated() = runTest {
        sessionManager.onLoginSuccess(
            accessToken = "access_token_abc",
            refreshToken = "refresh_token_xyz",
            userId = "user_1",
            email = "user1@example.com",
            fullName = "User One"
        )

        sessionManager.logout("refresh_token_xyz")

        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
        assertEquals(null, tokenStorage.getAccessToken())
        assertEquals(null, tokenStorage.getRefreshToken())
    }

    @Test
    fun onSessionExpired_clears_tokens_and_transitions_to_session_expired() {
        sessionManager.onLoginSuccess(
            accessToken = "access_token_abc",
            refreshToken = "refresh_token_xyz",
            userId = "user_1",
            email = "user1@example.com",
            fullName = "User One"
        )

        sessionManager.onSessionExpired()

        assertEquals(AuthState.SessionExpired, sessionManager.authState.value)
        assertEquals(null, tokenStorage.getAccessToken())
        assertEquals(null, tokenStorage.getRefreshToken())
    }
}
