package com.offline_First.data.remote

import com.offline_First.data.local.InMemoryTokenStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: InMemoryTokenStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStorage = InMemoryTokenStorage()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun adds_authorization_header_when_token_is_present() {
        tokenStorage.saveTokens("my_jwt_token", "my_refresh_token")

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val request = Request.Builder()
            .url(server.url("/api/v1/profile"))
            .build()

        client.newCall(request).execute().use { }

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer my_jwt_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun does_not_add_authorization_header_when_token_is_absent() {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val request = Request.Builder()
            .url(server.url("/api/v1/profile"))
            .build()

        client.newCall(request).execute().use { }

        val recordedRequest = server.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }
}
