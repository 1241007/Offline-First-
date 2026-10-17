package com.offline_First.data.repository

import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.TokenStorage
import com.offline_First.data.remote.AuthApiClient
import com.offline_First.data.remote.AuthenticatedApiClient
import com.offline_First.data.remote.ApiException
import com.offline_First.data.remote.AuthenticationException
import com.offline_First.data.remote.ChatApiConfig
import com.offline_First.data.remote.ForgotPasswordRequestDto
import com.offline_First.data.remote.LoginRequestDto
import com.offline_First.data.remote.LogoutRequestDto
import com.offline_First.data.remote.RefreshTokenRequestDto
import com.offline_First.data.remote.RegisterRequestDto
import com.offline_First.data.remote.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException

class RemoteAuthRepository(
    private val authApiClient: AuthApiClient,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val baseUrl: String = ChatApiConfig.BASE_URL,
    private val isOnlineProvider: () -> Boolean = { true }
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun observeSession(): Flow<Boolean> = sessionManager.authState.map { it is AuthState.Authenticated }

    override suspend fun signIn(contact: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cachedIdentity = tokenStorage.getUserIdentity()
            val trimmedContact = contact.trim()
            val isMatchingCachedUser = cachedIdentity != null && cachedIdentity.userId.isNotBlank() && (
                trimmedContact.equals(cachedIdentity.email.trim(), ignoreCase = true) ||
                trimmedContact.equals(cachedIdentity.userId.trim(), ignoreCase = true)
            )

            // If device is already offline, restore offline session if user previously logged in
            if (!isOnlineProvider()) {
                if (isMatchingCachedUser) {
                    sessionManager.onRestoreOfflineSession(
                        userId = cachedIdentity!!.userId,
                        email = cachedIdentity.email,
                        fullName = cachedIdentity.fullName
                    )
                    return@withContext Result.success(Unit)
                } else {
                    return@withContext Result.failure(
                        IllegalStateException("No internet connection. Please connect to the internet to log in with this account.")
                    )
                }
            }

            val res = authApiClient.login(LoginRequestDto(contact = contact, password = password))
            res.fold(
                onSuccess = { dto ->
                    sessionManager.onLoginSuccess(
                        accessToken = dto.accessToken,
                        refreshToken = dto.refreshToken,
                        userId = dto.user.id,
                        email = dto.user.email,
                        fullName = dto.user.fullName
                    )
                    try {
                        com.offline_First.data.AppContainer.profileRepository.getUserProfile()
                    } catch (_: Exception) {}
                    Result.success(Unit)
                },
                onFailure = { error ->
                    // If network dropped or unreachable and user matches cached login, restore offline session!
                    val isNetworkError = error is java.io.IOException ||
                            error.message?.contains("Failed to connect", ignoreCase = true) == true ||
                            error.message?.contains("timeout", ignoreCase = true) == true ||
                            error.message?.contains("Unable to resolve", ignoreCase = true) == true

                    if (isNetworkError && isMatchingCachedUser) {
                        sessionManager.onRestoreOfflineSession(
                            userId = cachedIdentity!!.userId,
                            email = cachedIdentity.email,
                            fullName = cachedIdentity.fullName
                        )
                        Result.success(Unit)
                    } else {
                        Result.failure(error)
                    }
                }
            )
        }

    override suspend fun register(input: RegistrationInput): Result<Unit> =
        withContext(Dispatchers.IO) {
            val res = authApiClient.register(
                RegisterRequestDto(
                    fullName = input.fullName,
                    email = input.email,
                    mobile = input.mobile.ifBlank { null },
                    password = input.password
                )
            )
            res.fold(
                onSuccess = { dto ->
                    sessionManager.onLoginSuccess(
                        accessToken = dto.accessToken,
                        refreshToken = dto.refreshToken,
                        userId = dto.user.id,
                        email = dto.user.email,
                        fullName = dto.user.fullName
                    )
                    try {
                        com.offline_First.data.AppContainer.profileRepository.getUserProfile()
                    } catch (_: Exception) {}
                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        }

    override suspend fun requestPasswordReset(contact: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val res = authApiClient.forgotPassword(ForgotPasswordRequestDto(contact = contact))
            res.map { Unit }
        }

    override suspend fun logout(refreshToken: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val token = tokenStorage.getAccessToken()
            // Fire best-effort logout request to backend
            try {
                authApiClient.logout(token, LogoutRequestDto(refreshToken))
            } catch (_: Exception) {
                // Ignore network errors on logout
            }
            try {
                (com.offline_First.data.AppContainer.profileRepository as? com.offline_First.data.remote.RemoteProfileRepository)?.clearCachedProfile()
            } catch (_: Exception) {}
            sessionManager.logout(refreshToken)
            Result.success(Unit)
        }

    override suspend fun fetchMe(): Result<UserDto> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/me")
                .get()
                .build()

            authenticatedApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: Failed to fetch current user")
                }
                val body = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(UserDto.serializer(), body)
            }
        }
    }

    override suspend fun restoreSession(): Result<Boolean> = withContext(Dispatchers.IO) {
        val refreshToken = tokenStorage.getRefreshToken()
        val cachedIdentity = tokenStorage.getUserIdentity()

        if (cachedIdentity == null || cachedIdentity.userId.isBlank()) {
            sessionManager.setUnauthenticated()
            return@withContext Result.success(false)
        }

        // Fast-path: If currently offline, restore offline session immediately without waiting for network timeout
        if (!isOnlineProvider()) {
            sessionManager.onRestoreOfflineSession(
                userId = cachedIdentity.userId,
                email = cachedIdentity.email,
                fullName = cachedIdentity.fullName
            )
            return@withContext Result.success(true)
        }

        if (refreshToken.isNullOrBlank()) {
            sessionManager.onRestoreOfflineSession(
                userId = cachedIdentity.userId,
                email = cachedIdentity.email,
                fullName = cachedIdentity.fullName
            )
            return@withContext Result.success(true)
        }

        // Try refreshing token online to verify validity and obtain up-to-date access token
        val refreshRes = authApiClient.refresh(RefreshTokenRequestDto(refreshToken))
        if (refreshRes.isFailure) {
            val error = refreshRes.exceptionOrNull()
            val isExplicitAuthRejection = error is AuthenticationException ||
                    (error is ApiException && (error.statusCode == 401 || error.statusCode == 403)) ||
                    (error?.message?.contains("401") == true || error?.message?.contains("403") == true)

            if (isExplicitAuthRejection) {
                // Explicit rejection: follow expiry policy
                sessionManager.onSessionExpired()
                return@withContext Result.success(false)
            } else if (cachedIdentity != null && cachedIdentity.userId.isNotBlank()) {
                // Unreachable server / network failure: restore restricted offline session
                sessionManager.onRestoreOfflineSession(
                    userId = cachedIdentity.userId,
                    email = cachedIdentity.email,
                    fullName = cachedIdentity.fullName
                )
                return@withContext Result.success(true)
            } else {
                // Network failed and no cached identity exists: cannot fake user
                sessionManager.setUnauthenticated()
                return@withContext Result.success(false)
            }
        }

        val tokens = refreshRes.getOrThrow()
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)

        // Authoritative user info via /me
        val meRes = fetchMe()
        if (meRes.isFailure) {
            val meError = meRes.exceptionOrNull()
            val isMeAuthRejection = meError is AuthenticationException ||
                    (meError is ApiException && (meError.statusCode == 401 || meError.statusCode == 403)) ||
                    (meError?.message?.contains("401") == true || meError?.message?.contains("403") == true)

            if (isMeAuthRejection) {
                sessionManager.onSessionExpired()
                return@withContext Result.success(false)
            } else if (cachedIdentity != null && cachedIdentity.userId.isNotBlank()) {
                // Online refresh succeeded but /me encountered temporary network issue
                sessionManager.onLoginSuccess(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    userId = cachedIdentity.userId,
                    email = cachedIdentity.email,
                    fullName = cachedIdentity.fullName
                )
                return@withContext Result.success(true)
            } else {
                sessionManager.setUnauthenticated()
                return@withContext Result.success(false)
            }
        }

        val user = meRes.getOrThrow()
        sessionManager.onLoginSuccess(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            userId = user.id,
            email = user.email,
            fullName = user.fullName
        )
        try {
            com.offline_First.data.AppContainer.profileRepository.getUserProfile()
        } catch (_: Exception) {}
        Result.success(true)
    }

    override fun observeAuthState(): Flow<AuthState> = sessionManager.authState
}
