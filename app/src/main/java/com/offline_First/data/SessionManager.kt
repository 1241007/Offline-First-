package com.offline_First.data

import com.offline_First.data.local.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String,
        val fullName: String
    ) : AuthState()
    object Refreshing : AuthState()
    object SessionExpired : AuthState()
}

class SessionManager(
    private val tokenStorage: TokenStorage
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun onLoginSuccess(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        fullName: String
    ) {
        tokenStorage.saveTokens(accessToken, refreshToken)
        _authState.value = AuthState.Authenticated(
            userId = userId,
            email = email,
            fullName = fullName
        )
    }

    suspend fun logout(refreshToken: String? = null) {
        tokenStorage.clearTokens()
        _authState.value = AuthState.Unauthenticated
    }

    fun onSessionExpired() {
        tokenStorage.clearTokens()
        _authState.value = AuthState.SessionExpired
    }

    fun onTokensRefreshed(newAccessToken: String, newRefreshToken: String) {
        tokenStorage.saveTokens(newAccessToken, newRefreshToken)
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = current
        }
    }

    fun setUnauthenticated() {
        tokenStorage.clearTokens()
        _authState.value = AuthState.Unauthenticated
    }

    fun setRefreshing() {
        _authState.value = AuthState.Refreshing
    }

    fun currentRefreshToken(): String? {
        return tokenStorage.getRefreshToken()
    }
}
