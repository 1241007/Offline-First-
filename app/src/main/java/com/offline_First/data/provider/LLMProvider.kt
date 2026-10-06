package com.offline_First.data.provider

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.flow.Flow

/**
 * Common LLM Inference Provider contract.
 * The Unified AI/Chat Layer communicates with inference providers through this interface.
 * The chat system, sessions, SQLite cache, memories, and UI remain identical regardless of provider.
 */
interface LLMProvider {
    val name: String

    /**
     * Streams inference response tokens/chunks.
     */
    fun streamInference(
        prompt: String,
        conversationId: String,
        clientMessageId: String,
        parentId: String?,
        explanationMode: ExplanationMode,
        history: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String>

    /**
     * Immediately cancels or halts ongoing generation.
     */
    suspend fun stopInference()
}
