package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("draft_text") val draftText: String = ""
)

@Serializable
data class ConversationSummaryDto(
    val id: String,
    val title: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("draft_text") val draftText: String = ""
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: String,  // "user" | "assistant"
    val content: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ConversationDetailDto(
    val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("draft_text") val draftText: String = "",
    val messages: List<MessageDto> = emptyList()
)

@Serializable
data class SendMessageRequestDto(
    val content: String,
    @SerialName("explanation_mode") val explanationMode: String,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("parent_id") val parentId: String? = null
)

@Serializable
data class SendMessageResponseDto(
    @SerialName("user_message") val userMessage: MessageDto,
    @SerialName("assistant_message") val assistantMessage: MessageDto
)

@Serializable
data class StreamEventDto(
    val type: String, // "metadata" | "token" | "done"
    val content: String? = null,
    @SerialName("user_message_id") val userMessageId: String? = null,
    @SerialName("assistant_message_id") val assistantMessageId: String? = null,
    val title: String? = null
)

@Serializable
data class SyncMessageItemDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SyncMessagesRequestDto(
    val messages: List<SyncMessageItemDto>
)

@Serializable
data class SyncMessagesResponseDto(
    @SerialName("synced_ids") val syncedIds: List<String>
)

@Serializable
data class MemoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val category: String,
    val content: String,
    val importance: Float = 1.0f,
    val confidence: Float = 1.0f,
    val active: Boolean = true
)

@Serializable
data class MemoryListResponseDto(
    val memories: List<MemoryDto> = emptyList()
)

@Serializable
data class MemoryCreateRequestDto(
    val category: String,
    val content: String,
    val importance: Float = 1.0f,
    val confidence: Float = 1.0f
)
