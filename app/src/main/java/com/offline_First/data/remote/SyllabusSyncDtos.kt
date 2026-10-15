package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyllabusPackSummaryDto(
    @SerialName("pack_id") val packId: String,
    val board: String,
    @SerialName("class_grade") val classGrade: String,
    val subject: String,
    val language: String = "en",
    @SerialName("chunk_count") val chunkCount: Int = 0,
    @SerialName("corpus_version") val corpusVersion: String = "1.0",
    @SerialName("embedding_model") val embeddingModel: String = "intfloat/multilingual-e5-small",
    @SerialName("embedding_version") val embeddingVersion: String = "1.0",
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class SyllabusPackListResponseDto(
    val packs: List<SyllabusPackSummaryDto>,
    @SerialName("total_count") val totalCount: Int
)

@Serializable
data class SyllabusManifestDto(
    @SerialName("pack_id") val packId: String,
    val board: String,
    @SerialName("class_grade") val classGrade: String,
    val subject: String,
    val language: String = "en",
    @SerialName("chunk_count") val chunkCount: Int,
    @SerialName("corpus_version") val corpusVersion: String,
    @SerialName("embedding_model") val embeddingModel: String = "intfloat/multilingual-e5-small",
    @SerialName("embedding_version") val embeddingVersion: String = "1.0",
    @SerialName("embedding_dimension") val embeddingDimension: Int = 384,
    @SerialName("content_hash") val contentHash: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class SyllabusChunkDto(
    @SerialName("chunk_id") val chunkId: String,
    val content: String,
    val board: String,
    @SerialName("class_grade") val classGrade: String,
    val subject: String,
    val chapter: String? = null,
    val section: String? = null,
    val language: String = "en",
    @SerialName("textbook_title") val textbookTitle: String? = null,
    @SerialName("page_reference") val pageReference: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("corpus_version") val corpusVersion: String? = null,
    @SerialName("content_hash") val contentHash: String? = null,
    @SerialName("embedding_model") val embeddingModel: String = "intfloat/multilingual-e5-small",
    @SerialName("embedding_version") val embeddingVersion: String = "1.0",
    val embedding: List<Float>? = null
)

@Serializable
data class SyllabusChunksResponseDto(
    @SerialName("pack_id") val packId: String,
    val chunks: List<SyllabusChunkDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_chunks") val totalChunks: Int = 0,
    @SerialName("embedding_model") val embeddingModel: String = "intfloat/multilingual-e5-small",
    @SerialName("embedding_version") val embeddingVersion: String = "1.0",
    @SerialName("embedding_dimension") val embeddingDimension: Int = 384
)
