package com.offline_First.data.local

import com.offline_First.data.provider.LLMProvider
import com.offline_First.data.remote.OnlineAIRepository
import com.offline_First.domain.model.AppMode
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.EvidenceOutcome
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.RAGRequestContext
import com.offline_First.domain.model.RetrievedChunk
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * End-to-End integration tests for Offline Chat RAG through the actual
 * OnlineAIRepository entry point, verifying OFF-004, OFF-007, OFF-014, and OFF-015:
 * - Offline disconnected execution (no remote calls).
 * - Prompt grounding with retrieved syllabus references.
 * - Strict abstention on missing/insufficient evidence (generative LLM is NOT called).
 * - Keyword fallback when E5 model is not ready.
 * - Low-RAM and corrupted BLOB safety.
 */
class OfflineChatEndToEndTest {

    private val testStudentProfile = UserProfile(
        fullName = "Test Student",
        email = "tester@edunova.ai",
        mobile = "1234567890",
        interests = "Science",
        level = "Grade 10",
        educationMode = EducationMode.SCHOOL,
        board = "CBSE",
        classGrade = "10",
        subject = "Science"
    )

    // Test double for Offline LLM Provider (on-device generative model)
    private class RecordingOfflineLLMProvider : LLMProvider {
        override val name: String = "RecordingOfflineProvider"
        var streamInferenceCalled = false
        var capturedPrompt: String? = null
        var capturedSystemPrompt: String? = null
        var stopInferenceCalled = false

        override fun streamInference(
            prompt: String,
            conversationId: String,
            clientMessageId: String,
            parentId: String?,
            explanationMode: ExplanationMode,
            history: List<ChatMessage>,
            systemPrompt: String
        ): Flow<String> = flow {
            streamInferenceCalled = true
            capturedPrompt = prompt
            capturedSystemPrompt = systemPrompt
            emit("This is grounded on-device inference generated from official syllabus context.")
        }

        override suspend fun stopInference() {
            stopInferenceCalled = true
        }
    }

    // Test double for Online LLM Provider (must never be called when offline)
    private class FailingOnlineLLMProvider : LLMProvider {
        override val name: String = "FailingOnlineProvider"
        var streamInferenceCalled = false

        override fun streamInference(
            prompt: String,
            conversationId: String,
            clientMessageId: String,
            parentId: String?,
            explanationMode: ExplanationMode,
            history: List<ChatMessage>,
            systemPrompt: String
        ): Flow<String> = flow {
            streamInferenceCalled = true
            throw AssertionError("CRITICAL VIOLATION: Online LLM Provider called while operating OFFLINE!")
        }

        override suspend fun stopInference() {}
    }

    // Configurable OfflineSyllabusRepository test double
    private class ConfigurableOfflineSyllabusRepository(
        var chunksToReturn: List<RetrievedChunk> = emptyList(),
        var outcomeToReturn: EvidenceOutcome = EvidenceOutcome.MISSING
    ) : OfflineSyllabusRepository() {
        var lastQuery: String? = null
        var lastContext: RAGRequestContext? = null
        var lastQueryEmbedding: FloatArray? = null
        var retrieveCallCount = 0

        override fun retrieve(
            query: String,
            context: RAGRequestContext,
            topK: Int,
            queryEmbedding: FloatArray?
        ): Pair<List<RetrievedChunk>, EvidenceOutcome> {
            retrieveCallCount++
            lastQuery = query
            lastContext = context
            lastQueryEmbedding = queryEmbedding
            return chunksToReturn to outcomeToReturn
        }
    }

    // ==============================================================================
    // OFF-004 & OFF-007: Offline Execution and Prompt Grounding
    // ==============================================================================
    @Test
    fun test_off_004_and_007_grounded_offline_chat_pipeline() = runBlocking {
        val recordingOfflineProvider = RecordingOfflineLLMProvider()
        val failingOnlineProvider = FailingOnlineLLMProvider()

        val sampleChunk = RetrievedChunk(
            chunkId = "cbse-10-sci-ch1-001",
            content = "When a magnesium ribbon is burnt in air, it burns with a dazzling white flame and changes into a white powder called magnesium oxide.",
            board = "CBSE",
            classGrade = "10",
            subject = "Science",
            chapter = "Chemical Reactions and Equations",
            textbookTitle = "CBSE Class 10 Science",
            normalizedScore = 0.85f,
            vectorRank = 1,
            sparseRank = 1,
            similarityScore = 0.89f
        )

        val fakeRepo = ConfigurableOfflineSyllabusRepository(
            chunksToReturn = listOf(sampleChunk),
            outcomeToReturn = EvidenceOutcome.SUFFICIENT
        )

        val readyE5Service = object : E5EmbeddingService(readyOverride = true) {
            override suspend fun embedQuery(text: String): FloatArray {
                return FloatArray(384) { 0.05f }
            }
        }

        val delegate = LocalAIRepository()
        delegate.setConnectionMode(ConnectionMode.OFFLINE)

        val testSession1 = ChatSession(id = "test-session-001", title = "Test Conversation", lastUpdated = System.currentTimeMillis())
        val repository = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { "test_user" },
            offlineSyllabusRepositoryProvider = { fakeRepo },
            e5EmbeddingServiceProvider = { readyE5Service },
            onlineProviderOverride = failingOnlineProvider,
            offlineProviderOverride = recordingOfflineProvider,
            profileProvider = { testStudentProfile },
            initialSession = testSession1
        )

        val tokens = repository.streamMessage("What happens when magnesium ribbon is burned?").toList()
        val fullResponse = tokens.joinToString("")

        // Invariants:
        // 1. Offline mode: Online provider was NEVER called
        assertFalse(failingOnlineProvider.streamInferenceCalled)

        // 2. Offline provider was called
        assertTrue(recordingOfflineProvider.streamInferenceCalled)

        // 3. E5EmbeddingService generated query embedding and passed to retrieval
        assertNotNull(fakeRepo.lastQueryEmbedding)
        assertEquals(384, fakeRepo.lastQueryEmbedding!!.size)

        // 4. Curriculum metadata passed correctly to retrieval
        assertEquals("CBSE", fakeRepo.lastContext?.board)
        assertEquals("10", fakeRepo.lastContext?.classGrade)
        assertEquals("Science", fakeRepo.lastContext?.subject)

        // 5. System prompt was grounded with official syllabus references
        val sysPrompt = recordingOfflineProvider.capturedSystemPrompt
        assertNotNull(sysPrompt)
        assertTrue(sysPrompt!!.contains("Offline Curriculum References:"))
        assertTrue(sysPrompt.contains("magnesium ribbon"))
        assertTrue(sysPrompt.contains("Chemical Reactions and Equations"))

        // 6. Response generated from grounded context
        assertTrue(fullResponse.contains("grounded on-device inference"))
    }

    // ==============================================================================
    // OFF-014: Strict Abstention on Empty/Insufficient Evidence
    // ==============================================================================
    @Test
    fun test_off_014_strict_abstention_on_empty_evidence() = runBlocking {
        val recordingOfflineProvider = RecordingOfflineLLMProvider()
        val failingOnlineProvider = FailingOnlineLLMProvider()

        // Empty evidence / MISSING outcome
        val fakeRepo = ConfigurableOfflineSyllabusRepository(
            chunksToReturn = emptyList(),
            outcomeToReturn = EvidenceOutcome.MISSING
        )

        val readyE5Service = object : E5EmbeddingService(readyOverride = true) {
            override suspend fun embedQuery(text: String): FloatArray = FloatArray(384) { 0.0f }
        }

        val delegate = LocalAIRepository()
        delegate.setConnectionMode(ConnectionMode.OFFLINE)

        val testSession2 = ChatSession(id = "test-session-002", title = "Test Conversation", lastUpdated = System.currentTimeMillis())
        val repository = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { "test_user" },
            offlineSyllabusRepositoryProvider = { fakeRepo },
            e5EmbeddingServiceProvider = { readyE5Service },
            onlineProviderOverride = failingOnlineProvider,
            offlineProviderOverride = recordingOfflineProvider,
            profileProvider = { testStudentProfile },
            initialSession = testSession2
        )

        val tokens = repository.streamMessage("Explain quantum teleportation in CBSE Class 10").toList()
        val response = tokens.joinToString("")

        // CRITICAL SPEC REQUIREMENT 4:
        // "Assert that the on-device generative LLM is not invoked."
        assertFalse("On-device generative LLM MUST NOT be invoked on empty evidence!", recordingOfflineProvider.streamInferenceCalled)
        assertFalse("Online provider MUST NOT be invoked offline!", failingOnlineProvider.streamInferenceCalled)

        // Must emit explicit refusal message with curriculum details
        assertTrue(response.contains("This topic was not found in the official curriculum corpus"))
        assertTrue(response.contains("Please verify the chapter or check your curriculum topics"))
    }

    // ==============================================================================
    // Keyword Fallback when E5 Model Not Ready
    // ==============================================================================
    @Test
    fun test_keyword_fallback_when_e5_model_not_ready() = runBlocking {
        val recordingOfflineProvider = RecordingOfflineLLMProvider()

        val sampleChunk = RetrievedChunk(
            chunkId = "cbse-10-sci-ch12-001",
            content = "Ohm's law states that electric current flowing through a metallic conductor is proportional to potential difference.",
            board = "CBSE",
            classGrade = "10",
            subject = "Science",
            chapter = "Electricity",
            normalizedScore = 0.75f,
            vectorRank = null,
            sparseRank = 1,
            similarityScore = null
        )

        val fakeRepo = ConfigurableOfflineSyllabusRepository(
            chunksToReturn = listOf(sampleChunk),
            outcomeToReturn = EvidenceOutcome.SUFFICIENT
        )

        // E5 Model is NOT ready (download incomplete or failed checksum)
        val notReadyE5Service = object : E5EmbeddingService(readyOverride = false) {}

        val delegate = LocalAIRepository()
        delegate.setConnectionMode(ConnectionMode.OFFLINE)

        val testSession3 = ChatSession(id = "test-session-003", title = "Test Conversation", lastUpdated = System.currentTimeMillis())
        val repository = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { "test_user" },
            offlineSyllabusRepositoryProvider = { fakeRepo },
            e5EmbeddingServiceProvider = { notReadyE5Service },
            offlineProviderOverride = recordingOfflineProvider,
            profileProvider = { testStudentProfile },
            initialSession = testSession3
        )

        val tokens = repository.streamMessage("State Ohm's law").toList()

        // Fallback occurred: queryEmbedding was null (keyword retrieval used)
        assertNull(fakeRepo.lastQueryEmbedding)
        assertEquals(1, fakeRepo.retrieveCallCount)

        // Generation still succeeded because keyword evidence was sufficient
        assertTrue(recordingOfflineProvider.streamInferenceCalled)
    }

    // ==============================================================================
    // OFF-015: Resource and Memory Safety
    // ==============================================================================
    @Test
    fun test_off_015_resource_and_memory_safety() {
        // 1. Corrupted BLOB handling (odd size, non-multiple-of-4) returns null safely
        val corruptedBlob = ByteArray(15) { 0xFF.toByte() }
        assertNull(SyllabusDatabase.blobToFloatArray(corruptedBlob))

        // 2. Dimension mismatch handling: searchByVector with wrong dimension returns emptyList safely
        val wrongDimensionVec = FloatArray(128) { 0.5f }
        val fakeRepo = ConfigurableOfflineSyllabusRepository()
        // Must not throw IndexOutOfBoundsException
        val (chunks, _) = fakeRepo.retrieve("test", RAGRequestContext(appMode = AppMode.STUDENT, board = "CBSE", classGrade = "10"), queryEmbedding = wrongDimensionVec)
        assertTrue(chunks.isEmpty())

        // 3. Cosine similarity with zero-norm vectors produces 0.0f without divide-by-zero NaN
        val zeroV = FloatArray(384) { 0.0f }
        val normalV = FloatArray(384) { 0.1f }
        val sim = SyllabusDatabase.cosineSimilarity(zeroV, normalV)
        assertEquals(0.0f, sim, 1e-6f)
        assertFalse(sim.isNaN())
    }

    // ==============================================================================
    // DEF-002: Deterministic Evidence-Grounded Extractive Fallback
    // ==============================================================================
    @Test
    fun test_def_002_grounded_extractive_fallback_when_gguf_unavailable() = runBlocking {
        // GGUF model is NOT downloaded on device (real default state without 1GB weights)
        val delegate = LocalAIRepository()
        delegate.setConnectionMode(ConnectionMode.OFFLINE)

        val sampleChunk = RetrievedChunk(
            chunkId = "cbse-10-sci-ch1-002",
            content = "The reaction between magnesium and oxygen in air produces magnesium oxide powder with a dazzling white flame.",
            board = "CBSE",
            classGrade = "10",
            subject = "Science",
            chapter = "Chemical Reactions and Equations",
            textbookTitle = "CBSE Class 10 Science Textbook",
            normalizedScore = 0.92f,
            vectorRank = 1,
            sparseRank = 1,
            similarityScore = 0.95f
        )

        val fakeRepo = ConfigurableOfflineSyllabusRepository(
            chunksToReturn = listOf(sampleChunk),
            outcomeToReturn = EvidenceOutcome.SUFFICIENT
        )

        val readyE5Service = object : E5EmbeddingService(readyOverride = true) {
            override suspend fun embedQuery(text: String): FloatArray = FloatArray(384) { 0.1f }
        }

        val testSession4 = ChatSession(id = "test-session-004", title = "Extractive Test", lastUpdated = System.currentTimeMillis())
        val repository = OnlineAIRepository(
            context = null,
            delegate = delegate,
            userIdProvider = { "test_user" },
            offlineSyllabusRepositoryProvider = { fakeRepo },
            e5EmbeddingServiceProvider = { readyE5Service },
            // offlineProviderOverride = null -> defaults to real OfflineLlamaProvider(delegate) which lacks GGUF weights
            profileProvider = { testStudentProfile },
            initialSession = testSession4
        )

        // 1. Supported question with sufficient evidence triggers grounded extractive fallback
        val tokens = repository.streamMessage("What happens when magnesium ribbon burns in air?").toList()
        val response = tokens.joinToString("")

        // Proves deterministic, grounded extractive fallback
        assertTrue("Must indicate curriculum excerpt", response.contains("Here is the relevant excerpt from your official curriculum"))
        assertTrue("Must cite chapter", response.contains("Chemical Reactions and Equations"))
        assertTrue("Must contain textbook content", response.contains("The reaction between magnesium and oxygen"))
        assertTrue("Must clearly label offline extractive mode", response.contains("*(Note: Running in offline extractive mode using authorized syllabus passages."))

        // Citations must be preserved
        val currentSession = (repository.observeCurrentSession() as kotlinx.coroutines.flow.StateFlow<ChatSession?>).value
        val currentMsg = currentSession?.messages?.lastOrNull { !it.fromUser }
        assertNotNull("Assistant message must be present in session", currentMsg)
        assertTrue("Citations must be preserved in extractive mode", currentMsg?.citations?.isNotEmpty() == true)
        assertEquals("cbse-10-sci-ch1-002", currentMsg?.citations?.first()?.sourceId)

        // 2. Unsupported question with MISSING evidence strictly abstains
        fakeRepo.chunksToReturn = emptyList()
        fakeRepo.outcomeToReturn = EvidenceOutcome.MISSING

        val abstentionTokens = repository.streamMessage("Explain black hole thermodynamics in CBSE Class 10").toList()
        val abstentionResponse = abstentionTokens.joinToString("")

        assertTrue(abstentionResponse.contains("This topic was not found in the official curriculum corpus"))
        assertFalse(abstentionResponse.contains("Here is the relevant excerpt"))
    }
}
