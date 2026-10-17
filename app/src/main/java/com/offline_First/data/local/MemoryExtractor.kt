package com.offline_First.data.local

import com.offline_First.domain.model.UserMemoryItem
import java.util.UUID

/**
 * Client-side Long-Term Memory extractor matching backend memory service extraction rules.
 * Extracts high-signal preferences, goals, and learning profiles from user messages.
 */
object MemoryExtractor {

    data class ExtractedMemory(
        val category: String,
        val content: String,
        val importance: Float,
        val confidence: Float
    )

    private val prefRegex = Regex(
        """\b(?:i\s+prefer|i\s+code\s+in|i\s+use|write\s+in|code\s+in)\s+([A-Za-z\+\#]+)(?:\s+examples?)?""",
        RegexOption.IGNORE_CASE
    )

    private val goalRegex = Regex(
        """\b(?:preparing\s+for|studying\s+for|revision\s+for|target\s+is)\s+([A-Za-z0-9\s\-]{2,30}?)(?:\bexam|\binterviews?|\btests?|\bcollege|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    private val styleRegex = Regex(
        """\b(?:explain\s+(?:with|using)|give\s+(?:me)?)\s+(simple\s+examples?|visuals?|step\s+by\s+step|in-depth|short\s+answers?|real-world\s+analogies?)""",
        RegexOption.IGNORE_CASE
    )

    private val levelRegex = Regex(
        """\bi\s+am\s+a\s+(beginner|intermediate|advanced)\s+(?:in|at|student|learner)""",
        RegexOption.IGNORE_CASE
    )

    fun extract(text: String): List<ExtractedMemory> {
        val list = mutableListOf<ExtractedMemory>()

        prefRegex.find(text)?.let { m ->
            val lang = m.groupValues[1]
            val formattedLang = if (lang.lowercase() in listOf("c++", "c#")) {
                lang.uppercase()
            } else {
                lang.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            list.add(
                ExtractedMemory(
                    category = "PREFERENCE",
                    content = "Prefers $formattedLang for code examples",
                    importance = 1.2f,
                    confidence = 0.95f
                )
            )
        }

        goalRegex.find(text)?.let { m ->
            val target = m.groupValues[1].trim()
            if (target.isNotBlank()) {
                list.add(
                    ExtractedMemory(
                        category = "GOAL",
                        content = "Preparing for $target",
                        importance = 1.3f,
                        confidence = 0.90f
                    )
                )
            }
        }

        styleRegex.find(text)?.let { m ->
            val style = m.groupValues[1].trim().lowercase()
            list.add(
                ExtractedMemory(
                    category = "LEARNING_PROFILE",
                    content = "Prefers explanations with $style",
                    importance = 1.1f,
                    confidence = 0.85f
                )
            )
        }

        levelRegex.find(text)?.let { m ->
            val level = m.groupValues[1].lowercase()
            list.add(
                ExtractedMemory(
                    category = "LEARNING_PROFILE",
                    content = "Self-identified $level level learner",
                    importance = 1.0f,
                    confidence = 0.90f
                )
            )
        }

        return list
    }

    fun toDomainItems(extracted: List<ExtractedMemory>): List<UserMemoryItem> {
        return extracted.map {
            UserMemoryItem(
                id = UUID.randomUUID().toString(),
                category = it.category,
                content = it.content,
                importance = it.importance,
                confidence = it.confidence,
                active = true
            )
        }
    }
}
