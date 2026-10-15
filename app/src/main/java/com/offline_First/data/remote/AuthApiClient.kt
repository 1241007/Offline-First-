package com.offline_First.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthApiClient(
    private val baseUrl: String = ChatApiConfig.BASE_URL
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(AppDns)
        .connectTimeout(ChatApiConfig.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor(CorrelationIdInterceptor())
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private fun parseError(responseBody: String?, statusCode: Int): Exception {
        if (!responseBody.isNullOrBlank()) {
            try {
                val err = json.decodeFromString(ApiErrorDto.serializer(), responseBody)
                return IOException("${err.code}: ${err.message}")
            } catch (_: Exception) {
            }
        }
        return IOException("HTTP error $statusCode")
    }

    suspend fun register(dto: RegisterRequestDto): Result<AuthTokensResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(RegisterRequestDto.serializer(), dto)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/register")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw parseError(body, response.code)
                    }
                    json.decodeFromString(AuthTokensResponseDto.serializer(), body ?: "")
                }
            }
        }

    suspend fun login(dto: LoginRequestDto): Result<AuthTokensResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(LoginRequestDto.serializer(), dto)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/login")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw parseError(body, response.code)
                    }
                    json.decodeFromString(AuthTokensResponseDto.serializer(), body ?: "")
                }
            }
        }

    suspend fun refresh(dto: RefreshTokenRequestDto): Result<AuthTokensResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(RefreshTokenRequestDto.serializer(), dto)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/refresh")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw parseError(body, response.code)
                    }
                    json.decodeFromString(AuthTokensResponseDto.serializer(), body ?: "")
                }
            }
        }

    suspend fun forgotPassword(dto: ForgotPasswordRequestDto): Result<ForgotPasswordResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(ForgotPasswordRequestDto.serializer(), dto)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/forgot-password")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw parseError(body, response.code)
                    }
                    json.decodeFromString(ForgotPasswordResponseDto.serializer(), body ?: "")
                }
            }
        }

    suspend fun resetPassword(dto: ResetPasswordRequestDto): Result<AuthMessageResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(ResetPasswordRequestDto.serializer(), dto)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/reset-password")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw parseError(body, response.code)
                    }
                    json.decodeFromString(AuthMessageResponseDto.serializer(), body ?: "")
                }
            }
        }

    suspend fun logout(
        accessToken: String?,
        dto: LogoutRequestDto? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(
                LogoutRequestDto.serializer(),
                dto ?: LogoutRequestDto()
            )
            val builder = Request.Builder()
                .url("$baseUrl/api/v1/auth/logout")
                .post(payload.toRequestBody("application/json".toMediaType()))

            if (!accessToken.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $accessToken")
            }

            okHttpClient.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string()
                    throw parseError(body, response.code)
                }
            }
        }
    }
}
