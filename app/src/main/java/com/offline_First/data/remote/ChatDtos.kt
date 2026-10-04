package com.offline_First.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ConversationSummaryDto(
    val id: String,
    val title: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: String,  // "user" | "assistant"
    val content: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ConversationDetailDto(
    val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val messages: List<MessageDto>
)

@Serializable
data class SendMessageRequestDto(
    val content: String,
    @SerialName("explanation_mode") val explanationMode: String
)

@Serializable
data class SendMessageResponseDto(
    @SerialName("user_message") val userMessage: MessageDto,
    @SerialName("assistant_message") val assistantMessage: MessageDto
)
