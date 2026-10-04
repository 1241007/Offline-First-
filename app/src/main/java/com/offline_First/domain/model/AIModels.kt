package com.offline_First.domain.model

import java.util.UUID

/**
 * Two connection modes supported by EduNova AI:
 * - ONLINE: Uses online AI and requires internet.
 * - OFFLINE: Works completely locally on-device without internet.
 */
enum class ConnectionMode {
    ONLINE,
    OFFLINE
}

/**
 * Three explanation modes that govern HOW the AI explains concepts.
 * These function across both Online and Offline connection modes.
 */
enum class ExplanationMode(
    val displayName: String,
    val description: String
) {
    TEACHER(
        displayName = "Teacher Mode",
        description = "Explains concepts like a teacher with examples, step-by-step explanations and practice questions."
    ),
    GENERAL(
        displayName = "General Mode",
        description = "Normal conversational learning assistant."
    ),
    EXPLAINABLE(
        displayName = "Explainable Mode",
        description = "Provides clear, detailed explanations and reasoning."
    )
}

/**
 * Status of the local offline AI model.
 * Note: Model selection, parameter count, RAM and storage requirements
 * are strictly internal implementation details and never exposed here.
 */
enum class OfflineAIStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY
}

/**
 * User-facing download progress information for offline AI.
 * Displays only generic progress, file size, percentage, and time estimate.
 */
data class OfflineAIDownloadProgress(
    val stage: String = "Downloading...",
    val progress: Int = 0,
    val downloadSize: String = "1.8 GB",
    val estimatedTimeRemaining: String = "~2 minutes remaining"
)

/**
 * Individual chat message in a conversation.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Complete chat conversation session.
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)
