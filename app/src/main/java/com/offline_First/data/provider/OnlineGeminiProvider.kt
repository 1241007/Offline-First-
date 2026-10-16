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
    private var capturedCitations: List<com.offline_First.domain.model.SourceCitation> = emptyList()

    override fun getLastCitations(): List<com.offline_First.domain.model.SourceCitation> = capturedCitations

    override fun streamInference(
        prompt: String,
        conversationId: String,
        clientMessageId: String,
        parentId: String?,
        explanationMode: ExplanationMode,
        history: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String> = flow {
        capturedCitations = emptyList()
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

        var bufferedCitations: List<com.offline_First.data.remote.CitationDto>? = null

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
            when (chunk.type) {
                "token" -> {
                    val tokenText = chunk.content
                    if (tokenText != null) {
                        emit(tokenText)
                    }
                }
                "citations" -> {
                    if (!chunk.citations.isNullOrEmpty()) {
                        bufferedCitations = chunk.citations
                        capturedCitations = chunk.citations.map { c ->
                            com.offline_First.domain.model.SourceCitation(
                                textbookTitle = c.textbookTitle,
                                chapter = c.chapter,
                                section = c.section,
                                pageReference = c.pageReference,
                                sourceId = c.chunkId
                            )
                        }
                    }
                }
                "done" -> {
                    bufferedCitations?.let { list ->
                        if (list.isNotEmpty()) {
                            val formatted = buildString {
                                append("\n\n---\n**Curriculum References:**\n")
                                list.forEachIndexed { index, c ->
                                    val title = c.textbookTitle ?: "Curriculum Reference"
                                    val chapter = if (!c.chapter.isNullOrBlank()) " | Chapter: ${c.chapter}" else ""
                                    val section = if (!c.section.isNullOrBlank()) " | Section: ${c.section}" else ""
                                    val page = if (!c.pageReference.isNullOrBlank()) " | ${c.pageReference}" else ""
                                    append("${index + 1}. *$title*$chapter$section$page\n")
                                }
                            }
                            emit(formatted)
                            bufferedCitations = null
                        }
                    }
                }
            }
        }

        // Fallback in case 'done' event was omitted but stream finished
        bufferedCitations?.let { list ->
            if (list.isNotEmpty()) {
                val formatted = buildString {
                    append("\n\n---\n**Curriculum References:**\n")
                    list.forEachIndexed { index, c ->
                        val title = c.textbookTitle ?: "Curriculum Reference"
                        val chapter = if (!c.chapter.isNullOrBlank()) " | Chapter: ${c.chapter}" else ""
                        val section = if (!c.section.isNullOrBlank()) " | Section: ${c.section}" else ""
                        val page = if (!c.pageReference.isNullOrBlank()) " | ${c.pageReference}" else ""
                        append("${index + 1}. *$title*$chapter$section$page\n")
                    }
                }
                emit(formatted)
                bufferedCitations = null
            }
        }
    }

    override suspend fun stopInference() {
        activeJob?.cancel()
        activeJob = null
    }
}
