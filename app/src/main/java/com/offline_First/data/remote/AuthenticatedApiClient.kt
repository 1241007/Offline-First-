package com.offline_First.data.remote

import com.offline_First.data.SessionManager
import com.offline_First.data.local.TokenStorage
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AuthenticatedApiClient(
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val publicClient: () -> OkHttpClient,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(AppDns)
        .connectTimeout(ChatApiConfig.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(ChatApiConfig.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor(CorrelationIdInterceptor())
        .addInterceptor(AuthInterceptor(tokenStorage))
        .authenticator(TokenAuthenticator(tokenStorage, sessionManager, publicClient, baseUrl))
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
}
