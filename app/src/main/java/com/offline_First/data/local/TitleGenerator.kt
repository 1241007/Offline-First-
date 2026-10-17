package com.offline_First.data.local

import java.util.Locale

/**
 * Deterministic local conversation title generator for student-facing chatbot.
 * Produces concise, descriptive titles (approximately 3 to 8 words) from the first user prompt.
 */
object TitleGenerator {

    private val PREFIXES = listOf(
        "what is the meaning of ", "what is the difference between ",
        "what is an ", "what is a ", "what is the ", "what is ",
        "what are the ", "what are ",
        "could you please explain ", "can you please explain ",
        "can you explain to me ", "can you explain ",
        "tell me about the ", "tell me about ",
        "help me understand ", "help me with ",
        "how does ", "how do ", "how to ",
        "why does ", "why do ", "why is "
    )

    fun generateFallbackTitle(prompt: String): String {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return "New Conversation"

        var working = trimmed.replace(Regex("\\s+"), " ")
        val lower = working.lowercase(Locale.ROOT)

        for (prefix in PREFIXES) {
            if (lower.startsWith(prefix)) {
                working = working.substring(prefix.length).trim()
                break
            }
        }

        // Strip trailing punctuation & surrounding quotes
        working = working.trimEnd('?', '.', '!', ':', ';', ',').trim('"', '\'', '`').trim()
        if (working.isBlank()) {
            working = trimmed.replace(Regex("\\s+"), " ")
        }

        val words = working.split(" ").filter { it.isNotBlank() }
        val cappedWords = if (words.size > 8) words.take(8) else words
        var result = cappedWords.joinToString(" ")
        if (result.length > 50) {
            result = result.take(50).trim()
        }

        return if (result.isNotEmpty()) {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "New Conversation"
        }
    }
}
