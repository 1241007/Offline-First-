package com.offline_First.data.provider

import com.offline_First.data.remote.ChatApiClient
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Online LLM Provider communicating with FastAPI & OpenRouter via SSE streaming.
 */
class OnlineGeminiProvider(
    private val profileProvider: () -> com.offline_First.domain.model.UserProfile? = {
        try {
            com.offline_First.data.AppContainer.profileRepository.currentProfile
        } catch (_: Throwable) {
            null
        }
    }
) : LLMProvider {
    override val name: String = "Online"
    private var activeJob: Job? = null

    override fun streamInference(
        prompt: String,
        conversationId: String,
        clientMessageId: String,
        parentId: String?,
        explanationMode: ExplanationMode,
        history: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String> = flow {
        val modeStr = when (explanationMode) {
            ExplanationMode.TEACHER -> "teacher"
            ExplanationMode.EXPLAINABLE -> "explainable"
            else -> "general"
        }

        val profile = profileProvider()
        val isSchool = profile?.educationMode == com.offline_First.domain.model.EducationMode.SCHOOL
        val appMode = if (isSchool) "student" else "general"
        val board = if (isSchool) profile?.board else null
        val classGrade = if (isSchool) profile?.classGrade else null
        val subject = if (isSchool) profile?.subject else null
        val language = profile?.preferredLanguage ?: "en"

        ChatApiClient.streamMessage(
            conversationId = conversationId,
            content = prompt,
            explanationMode = modeStr,
            clientMessageId = clientMessageId,
            parentId = parentId,
            appMode = appMode,
            board = board,
            classGrade = classGrade,
            subject = subject,
            language = language
        ).collect { chunk ->
            if (chunk.type == "token" && chunk.content != null) {
                emit(chunk.content)
            }
        }
    }

    override suspend fun stopInference() {
        activeJob?.cancel()
        activeJob = null
    }
}
