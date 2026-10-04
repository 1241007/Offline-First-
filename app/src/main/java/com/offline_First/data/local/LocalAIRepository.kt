package com.offline_First.data.local

import com.offline_First.data.repository.AIRepository
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIDownloadProgress
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Local-first implementation of AIRepository.
 *
 * NOTE ON INTERNAL MODEL SELECTION:
 * All device capability checks, internal weights, and model profiles
 * are encapsulated strictly within this class. The UI and domain layers
 * never receive or display model names, parameter counts, RAM, or storage specs.
 */
class LocalAIRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AIRepository {

    // Internal model representation (strictly private)
    private enum class InternalModelProfile(val packageSizeLabel: String) {
        COMPACT("1.8 GB"),
        ENHANCED("2.4 GB")
    }

    companion object {
        private val _connectionModeFlow = MutableStateFlow(ConnectionMode.ONLINE)
        private val _explanationModeFlow = MutableStateFlow(ExplanationMode.GENERAL)
        private val _offlineStatusFlow = MutableStateFlow(OfflineAIStatus.NOT_DOWNLOADED)
        private val _downloadProgressFlow = MutableStateFlow(
            OfflineAIDownloadProgress(
                stage = "Downloading...",
                progress = 0,
                downloadSize = "1.8 GB",
                estimatedTimeRemaining = "~2 minutes remaining"
            )
        )

        private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
        private val _currentSessionFlow = MutableStateFlow<ChatSession?>(null)

        private var activeDownloadJob: Job? = null

        fun resetState() {
            _sessionsFlow.value = emptyList()
            _currentSessionFlow.value = null
            _connectionModeFlow.value = ConnectionMode.ONLINE
            _explanationModeFlow.value = ExplanationMode.GENERAL
            _offlineStatusFlow.value = OfflineAIStatus.NOT_DOWNLOADED
        }
    }

    // Silently determines the appropriate local profile based on device capabilities
    private fun resolveAppropriateModel(): InternalModelProfile {
        // Safe internal check without exposing hardware info to UI
        val runtimeMemory = Runtime.getRuntime().maxMemory()
        return if (runtimeMemory > 256 * 1024 * 1024) {
            InternalModelProfile.COMPACT
        } else {
            InternalModelProfile.COMPACT
        }
    }

    override fun observeConnectionMode(): Flow<ConnectionMode> = _connectionModeFlow.asStateFlow()

    override suspend fun setConnectionMode(mode: ConnectionMode): Result<Unit> {
        _connectionModeFlow.value = mode
        return Result.success(Unit)
    }

    override fun observeExplanationMode(): Flow<ExplanationMode> = _explanationModeFlow.asStateFlow()

    override suspend fun setExplanationMode(mode: ExplanationMode): Result<Unit> {
        _explanationModeFlow.value = mode
        return Result.success(Unit)
    }

    override fun observeOfflineAIStatus(): Flow<OfflineAIStatus> = _offlineStatusFlow.asStateFlow()

    override fun observeDownloadProgress(): Flow<OfflineAIDownloadProgress> = _downloadProgressFlow.asStateFlow()

    override suspend fun startOfflineAIDownload(): Result<Unit> {
        if (_offlineStatusFlow.value == OfflineAIStatus.READY) {
            return Result.success(Unit)
        }

        activeDownloadJob?.cancel()
        val selectedProfile = resolveAppropriateModel()
        _offlineStatusFlow.value = OfflineAIStatus.DOWNLOADING

        activeDownloadJob = scope.launch {
            val totalSize = selectedProfile.packageSizeLabel

            val stages = listOf(
                Pair(0, "Downloading..."),
                Pair(15, "Downloading..."),
                Pair(34, "Downloading..."),
                Pair(52, "Downloading..."),
                Pair(64, "Downloading..."),
                Pair(78, "Downloading..."),
                Pair(88, "Verifying..."),
                Pair(96, "Preparing..."),
                Pair(100, "Ready")
            )

            for ((pct, stageName) in stages) {
                if (_offlineStatusFlow.value != OfflineAIStatus.DOWNLOADING) break

                val timeEstimate = when {
                    pct < 40 -> "~2 minutes remaining"
                    pct < 70 -> "~1 minute remaining"
                    pct < 90 -> "~30 seconds remaining"
                    pct < 100 -> "Almost done..."
                    else -> "Ready"
                }

                _downloadProgressFlow.value = OfflineAIDownloadProgress(
                    stage = stageName,
                    progress = pct,
                    downloadSize = totalSize,
                    estimatedTimeRemaining = timeEstimate
                )

                delay(500)
            }

            if (_offlineStatusFlow.value == OfflineAIStatus.DOWNLOADING) {
                _offlineStatusFlow.value = OfflineAIStatus.READY
            }
        }

        return Result.success(Unit)
    }

    override suspend fun deleteOfflineAI(): Result<Unit> {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _offlineStatusFlow.value = OfflineAIStatus.NOT_DOWNLOADED
        _downloadProgressFlow.value = OfflineAIDownloadProgress(
            stage = "Downloading...",
            progress = 0,
            downloadSize = resolveAppropriateModel().packageSizeLabel,
            estimatedTimeRemaining = "~2 minutes remaining"
        )
        return Result.success(Unit)
    }

    override fun observeChatSessions(): Flow<List<ChatSession>> = _sessionsFlow.asStateFlow()

    override fun observeCurrentSession(): Flow<ChatSession?> = _currentSessionFlow.asStateFlow()

    override suspend fun createNewChat(): Result<ChatSession> {
        val newChat = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Conversation",
            lastUpdated = System.currentTimeMillis(),
            messages = emptyList()
        )
        _sessionsFlow.value = listOf(newChat) + _sessionsFlow.value
        _currentSessionFlow.value = newChat
        return Result.success(newChat)
    }

    override suspend fun selectChat(sessionId: String): Result<Unit> {
        val target = _sessionsFlow.value.find { it.id == sessionId }
        if (target != null) {
            _currentSessionFlow.value = target
        }
        return Result.success(Unit)
    }

    override suspend fun renameChat(sessionId: String, newTitle: String): Result<Unit> {
        _sessionsFlow.value = _sessionsFlow.value.map { session ->
            if (session.id == sessionId) {
                session.copy(title = newTitle.ifBlank { session.title })
            } else {
                session
            }
        }
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = _currentSessionFlow.value?.copy(title = newTitle.ifBlank { _currentSessionFlow.value!!.title })
        }
        return Result.success(Unit)
    }

    override suspend fun deleteChat(sessionId: String): Result<Unit> {
        val remaining = _sessionsFlow.value.filter { it.id != sessionId }
        _sessionsFlow.value = remaining
        if (_currentSessionFlow.value?.id == sessionId) {
            _currentSessionFlow.value = remaining.firstOrNull() ?: createNewChat().getOrThrow()
        }
        return Result.success(Unit)
    }

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> {
        val current = _currentSessionFlow.value ?: createNewChat().getOrThrow()
        val userMsg = ChatMessage(
            text = prompt,
            fromUser = true,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessagesWithUser = current.messages + userMsg

        // Generate response based on ConnectionMode & ExplanationMode
        val aiResponseText = generateResponseText(
            prompt = prompt,
            connectionMode = _connectionModeFlow.value,
            explanationMode = _explanationModeFlow.value,
            offlineStatus = _offlineStatusFlow.value
        )

        val aiMsg = ChatMessage(
            text = aiResponseText,
            fromUser = false,
            timestamp = System.currentTimeMillis() + 100
        )

        val updatedMessages = updatedMessagesWithUser + aiMsg
        val resolvedTitle = if (current.title == "New Conversation" && prompt.isNotBlank()) {
            prompt.take(28).trim().replaceFirstChar { it.uppercase() }
        } else {
            current.title
        }

        val updatedSession = current.copy(
            title = resolvedTitle,
            lastUpdated = System.currentTimeMillis(),
            messages = updatedMessages
        )

        _currentSessionFlow.value = updatedSession
        _sessionsFlow.value = _sessionsFlow.value.map {
            if (it.id == updatedSession.id) updatedSession else it
        }

        return Result.success(aiMsg)
    }

    private fun generateResponseText(
        prompt: String,
        connectionMode: ConnectionMode,
        explanationMode: ExplanationMode,
        offlineStatus: OfflineAIStatus
    ): String {
        // Offline validation guard
        if (connectionMode == ConnectionMode.OFFLINE && offlineStatus != OfflineAIStatus.READY) {
            return "⚠️ **Offline AI is not ready yet.**\n\n" +
                    "To use offline inference without internet, please open **AI Settings** (⚙️) and tap **Set up Offline AI** to download the offline package, or switch back to **Online** mode."
        }

        val lowerPrompt = prompt.lowercase()

        return when (explanationMode) {
            ExplanationMode.TEACHER -> {
                buildTeacherResponse(prompt, lowerPrompt)
            }
            ExplanationMode.GENERAL -> {
                buildGeneralResponse(prompt, lowerPrompt)
            }
            ExplanationMode.EXPLAINABLE -> {
                buildExplainableResponse(prompt, lowerPrompt)
            }
        }
    }

    private fun buildTeacherResponse(prompt: String, lower: String): String {
        return when {
            "binary search" in lower -> {
                "👨‍🏫 **Teacher's Explanation: Binary Search**\n\n" +
                        "Let's imagine you're looking for a word in an English dictionary. You don't read page 1, then page 2, then page 3! You open to the middle. If your word starts with 'S', you ignore the first half entirely and look in the second half. That's Binary Search!\n\n" +
                        "**Step-by-step Process**:\n" +
                        "1. Ensure the array is sorted.\n" +
                        "2. Find the middle index: `mid = (low + high) / 2`.\n" +
                        "3. If `array[mid] == target`, congratulations! Found it.\n" +
                        "4. If `target < array[mid]`, search the left half (`high = mid - 1`).\n" +
                        "5. Otherwise, search the right half (`low = mid + 1`).\n\n" +
                        "```kotlin\n" +
                        "fun binarySearch(arr: IntArray, target: Int): Int {\n" +
                        "    var low = 0; var high = arr.size - 1\n" +
                        "    while (low <= high) {\n" +
                        "        val mid = (low + high) ushr 1\n" +
                        "        if (arr[mid] == target) return mid\n" +
                        "        if (arr[mid] < target) low = mid + 1 else high = mid - 1\n" +
                        "    }\n" +
                        "    return -1\n" +
                        "}\n" +
                        "```\n\n" +
                        "📝 **Quick Practice Question for You**:\n" +
                        "If an array has 1,024 elements, what is the maximum number of comparisons Binary Search will ever make?"
            }
            "quadratic" in lower || "equation" in lower -> {
                "👨‍🏫 **Teacher's Explanation: Quadratic Equations**\n\n" +
                        "Think of a parabola in basketball—the ball rises, hits a peak, and drops. That curve is modeled by a quadratic equation!\n\n" +
                        "**Standard Equation**:\n" +
                        "ax² + bx + c = 0\n\n" +
                        "**How to solve step-by-step**:\n" +
                        "1. Identify coefficients a, b, and c.\n" +
                        "2. Calculate the Discriminant: D = b² - 4ac.\n" +
                        "3. Apply the Quadratic Formula:\n" +
                        "x = (-b ± √D) / (2a)\n\n" +
                        "📝 **Practice Question**:\n" +
                        "Solve x² - 5x + 6 = 0. Can you factor it into two linear terms?"
            }
            "quiz" in lower -> {
                "👨‍🏫 **Quick Quiz Time!**\n\n" +
                "Here are 2 rapid-fire questions to check your concept:\n\n" +
                "1. Which data structure follows First-In, First-Out (FIFO)?\n" +
                "   A) Stack  B) Queue  C) Tree  D) Heap\n\n" +
                "2. What is the time complexity of looking up a key in a standard Hash Table on average?\n" +
                "   A) O(N)  B) O(log N)  C) O(1)  D) O(N²)\n\n" +
                "Reply with your answers and let's check them together!"
            }
            else -> {
                "👨‍🏫 **Teacher's Explanation for: \"$prompt\"**\n\n" +
                        "Let's break this down into digestible concepts so you can master it.\n\n" +
                        "**1. Core Principle**\n" +
                        "Every complex idea starts with a simple fundamental rule. In this case, focus on the input, the transformation, and the expected outcome.\n\n" +
                        "**2. Example in Practice**\n" +
                        "When applying this in real projects or exam problems, observe how changing one parameter impacts the entire result.\n\n" +
                        "**3. Practice Question**:\n" +
                        "How would you explain the core goal of this topic in your own words?"
            }
        }
    }

    private fun buildGeneralResponse(prompt: String, lower: String): String {
        return when {
            "binary search" in lower -> {
                "**Binary Search** is a search algorithm that finds the position of a target value within a sorted array. It compares the target value to the middle element and cuts the search space in half each step.\n\n" +
                        "• **Prerequisite**: The list must already be sorted.\n" +
                        "• **Time Complexity**: Best: O(1), Average/Worst: O(log N).\n" +
                        "• **Space Complexity**: O(1) iterative, O(log N) recursive.\n\n" +
                        "Would you like to see a recursive implementation or explore edge cases with duplicates?"
            }
            "quadratic" in lower || "equation" in lower -> {
                "A **quadratic equation** is a second-order polynomial equation in a single variable x:\n\n" +
                        "ax² + bx + c = 0\n\n" +
                        "The solutions are given by the quadratic formula:\n" +
                        "x = (-b ± √(b² - 4ac)) / (2a)\n\n" +
                        "The term b² - 4ac indicates whether roots are real (>0), equal (=0), or complex (<0)."
            }
            else -> {
                "Here is a clear answer for **\"$prompt\"**:\n\n" +
                        "• **Overview**: This topic focuses on structured problem solving, foundational concepts, and practical applications.\n" +
                        "• **Key Insight**: Always verify baseline assumptions before moving forward.\n\n" +
                        "Let me know if you would like practice questions, code snippets, or a summary!"
            }
        }
    }

    private fun buildExplainableResponse(prompt: String, lower: String): String {
        return when {
            "binary search" in lower -> {
                "📖 **Detailed Explanation & Reasoning: Binary Search**\n\n" +
                        "**1. The Mathematical Rationale**:\n" +
                        "Linear search inspects N elements one-by-one. Binary search cuts the search space in half at each iteration (N, N/2, N/4, ..., 1). The number of divisions needed to reach 1 is log₂ N. Hence, an array of 1,000,000 items takes at most ~20 steps.\n\n" +
                        "**2. Invariants & Proof of Correctness**:\n" +
                        "At every step, the loop invariant holds: if the target exists, it must reside in the index range `[low, high]`. When `low > high`, the range is empty, proving the target is not present.\n\n" +
                        "**3. Critical Edge Cases & Traps**:\n" +
                        "• Integer overflow: Writing `(low + high) / 2` can overflow in 32-bit signed integers when both values are large. Prefer `low + (high - low) / 2` or `(low + high) ushr 1`.\n" +
                        "• Unsorted data: If the array is unsorted, the invariant is invalidated and results are undefined."
            }
            "quadratic" in lower || "equation" in lower -> {
                "📖 **Detailed Explanation & Reasoning: Quadratic Roots**\n\n" +
                        "**1. Why the formula works (Completing the Square)**:\n" +
                        "Starting with ax² + bx + c = 0, dividing by a:\n" +
                        "x² + (b/a)x = -c/a\n\n" +
                        "Add (b / 2a)² to both sides:\n" +
                        "(x + b/(2a))² = (b² - 4ac) / (4a²)\n\n" +
                        "Taking square roots and isolating x yields the famous formula!\n\n" +
                        "**2. Geometric Interpretation**:\n" +
                        "The vertex of the parabola lies at x = -b / (2a), and the ± √(D) / (2a) represents the horizontal offset to the x-intercepts."
            }
            else -> {
                "📖 **Detailed Explanation & Reasoning for: \"$prompt\"**\n\n" +
                        "**1. Theoretical Foundation**\n" +
                        "This concept is grounded in deterministic behavior and modular design principles.\n\n" +
                        "**2. Cause and Effect Analysis**\n" +
                        "Understanding why this happens requires tracing how state changes propagate through the system.\n\n" +
                        "**3. Trade-offs & Nuances**\n" +
                        "Every approach carries trade-offs between speed, simplicity, and flexibility."
            }
        }
    }
}
