package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.TokenStorage
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.locks.ReentrantLock

class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val publicClient: () -> OkHttpClient,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) : Authenticator {

    private val lock = ReentrantLock()
    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loop: if response already retried on 401, give up
        if (response.priorResponse?.code == 401) {
            return null
        }

        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        lock.lock()
        try {
            val currentToken = tokenStorage.getAccessToken()

            // Check if another thread has already refreshed the token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val currentRefreshToken = tokenStorage.getRefreshToken()
            if (currentRefreshToken.isNullOrBlank()) {
                sessionManager.onSessionExpired()
                return null
            }

            sessionManager.setRefreshing()

            val refreshPayload = json.encodeToString(
                RefreshTokenRequestDto.serializer(),
                RefreshTokenRequestDto(refreshToken = currentRefreshToken)
            )
            val refreshRequest = Request.Builder()
                .url("$baseUrl/api/v1/auth/refresh")
                .post(refreshPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val refreshResponse = try {
                publicClient().newCall(refreshRequest).execute()
            } catch (e: Exception) {
                sessionManager.onSessionExpired()
                return null
            }

            if (!refreshResponse.isSuccessful) {
                refreshResponse.close()
                sessionManager.onSessionExpired()
                return null
            }

            val responseBody = refreshResponse.body?.string() ?: run {
                refreshResponse.close()
                sessionManager.onSessionExpired()
                return null
            }
            refreshResponse.close()

            val tokens = try {
                json.decodeFromString(AuthTokensResponseDto.serializer(), responseBody)
            } catch (e: Exception) {
                sessionManager.onSessionExpired()
                return null
            }

            tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
            sessionManager.onTokensRefreshed(tokens.accessToken, tokens.refreshToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
        } finally {
            lock.unlock()
        }
    }
}
