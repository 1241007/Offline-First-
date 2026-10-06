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
    ): Flow<String> = flow {
        val result = localAIRepository.generateOfflineInference(
            systemPrompt = systemPrompt,
            history = history,
            prompt = prompt
        )
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: IllegalStateException("Offline llama.cpp inference failed")
        }
        val fullText = result.getOrThrow()
        // Stream words progressively to deliver real-time token streaming UX
        val words = fullText.split(" ")
        for ((index, word) in words.withIndex()) {
            emit(if (index == 0) word else " $word")
            delay(20)
        }
    }

    override suspend fun stopInference() {
        localAIRepository.stopOfflineInference()
    }
}
