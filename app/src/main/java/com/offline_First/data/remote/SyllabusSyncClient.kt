package com.offline_First.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Interface for fetching curriculum syllabus packs and precomputed embeddings.
 */
interface SyllabusSyncClient {
    suspend fun fetchAvailablePacks(): List<SyllabusPackSummaryDto>
    suspend fun fetchPackManifest(packId: String): SyllabusManifestDto
    suspend fun fetchPackChunks(packId: String, cursor: String? = null, limit: Int = 200): SyllabusChunksResponseDto
}

/**
 * OkHttp-based implementation of SyllabusSyncClient.
 */
class RemoteSyllabusSyncClient(
    private val authenticatedClient: AuthenticatedApiClient,
    private val baseUrl: String = ChatApiConfig.BASE_URL
) : SyllabusSyncClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: OkHttpClient
        get() = authenticatedClient.okHttpClient

    override suspend fun fetchAvailablePacks(): List<SyllabusPackSummaryDto> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v1/syllabus/packs"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException("Failed to fetch syllabus packs (HTTP ${response.code}): $errBody")
        }

        val body = response.body?.string() ?: throw IOException("Empty response from /api/v1/syllabus/packs")
        val parsed = json.decodeFromString<SyllabusPackListResponseDto>(body)
        parsed.packs
    }

    override suspend fun fetchPackManifest(packId: String): SyllabusManifestDto = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v1/syllabus/packs/$packId/manifest"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException("Failed to fetch manifest for pack '$packId' (HTTP ${response.code}): $errBody")
        }

        val body = response.body?.string() ?: throw IOException("Empty response from manifest endpoint")
        json.decodeFromString<SyllabusManifestDto>(body)
    }

    override suspend fun fetchPackChunks(
        packId: String,
        cursor: String?,
        limit: Int
    ): SyllabusChunksResponseDto = withContext(Dispatchers.IO) {
        val httpUrlBuilder = "$baseUrl/api/v1/syllabus/packs/$packId/chunks"
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?: throw IllegalArgumentException("Invalid URL for pack chunks: $baseUrl")

        httpUrlBuilder.addQueryParameter("limit", limit.toString())
        if (!cursor.isNullOrBlank()) {
            httpUrlBuilder.addQueryParameter("cursor", cursor)
        }

        val request = Request.Builder()
            .url(httpUrlBuilder.build())
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException("Failed to fetch chunks for pack '$packId' (HTTP ${response.code}): $errBody")
        }

        val body = response.body?.string() ?: throw IOException("Empty response from chunks endpoint")
        json.decodeFromString<SyllabusChunksResponseDto>(body)
    }
}
