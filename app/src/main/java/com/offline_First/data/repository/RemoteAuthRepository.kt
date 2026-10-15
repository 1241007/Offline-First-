package com.offline_First.data.repository

import com.offline_First.data.AuthState
import com.offline_First.data.SessionManager
import com.offline_First.data.local.TokenStorage
import com.offline_First.data.remote.AuthApiClient
import com.offline_First.data.remote.AuthenticatedApiClient
import com.offline_First.data.remote.ChatApiConfig
import com.offline_First.data.remote.ForgotPasswordRequestDto
import com.offline_First.data.remote.LoginRequestDto
import com.offline_First.data.remote.LogoutRequestDto
import com.offline_First.data.remote.RefreshTokenRequestDto
import com.offline_First.data.remote.RegisterRequestDto
import com.offline_First.data.remote.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException

class RemoteAuthRepository(
    private val authApiClient: AuthApiClient,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun signIn(contact: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
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
                onFailure = { Result.failure(it) }
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
        if (refreshToken.isNullOrBlank()) {
            sessionManager.setUnauthenticated()
            return@withContext Result.success(false)
        }

        // Try refreshing token to ensure valid session and obtain up-to-date access token
        val refreshRes = authApiClient.refresh(RefreshTokenRequestDto(refreshToken))
        if (refreshRes.isFailure) {
            sessionManager.onSessionExpired()
            return@withContext Result.success(false)
        }

        val tokens = refreshRes.getOrThrow()
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)

        // Authoritative user info via /me
        val meRes = fetchMe()
        if (meRes.isFailure) {
            sessionManager.setUnauthenticated()
            return@withContext Result.success(false)
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
