package com.offline_First.data.provider

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Offline LLM Provider running on-device inference via llama.cpp and Qwen GGUF model.
 */
class OfflineLlamaProvider(
    private val localAIRepository: LocalAIRepository
) : LLMProvider {
    override val name: String = "llama.cpp"

    override fun streamInference(
        prompt: String,
        conversationId: String,
        clientMessageId: String,
        parentId: String?,
        explanationMode: ExplanationMode,
        history: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String> = localAIRepository.streamOfflineInference(
        systemPrompt = systemPrompt,
        history = history,
        prompt = prompt
    )

    override suspend fun stopInference() {
        localAIRepository.stopOfflineInference()
    }
}
