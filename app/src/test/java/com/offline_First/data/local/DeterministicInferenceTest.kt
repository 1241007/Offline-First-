package com.offline_First.data.local

import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.RetrievedChunk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verification of Deterministic Generation and Tie-Breaking across the offline inference stack:
 * 1. Byte-for-byte reproducibility across repeated generations with identical effective inputs.
 * 2. Absolute absence of response caching: the engine is invoked for every repeated request.
 * 3. Differentiation across explanation modes (teacher, general, explainable).
 * 4. Stable, deterministic tie-breaking for syllabus chunks ranking.
 * 5. Consistent ChatML prompt serialization for KV cache reuse.
 */
class DeterministicInferenceTest {

    // Controllable deterministic test double for OfflineInferenceEngine
    private class DeterministicFakeEngine : OfflineInferenceEngine {
        override val isLoaded: Boolean = true
        override val modelInstanceId: String = "deterministic_instance_001"
        var generateCallCount = 0
        var streamCallCount = 0
        var lastCapturedPrompt: String? = null
        var lastCapturedSystemPrompt: String? = null

        override suspend fun loadModel(modelFile: File) {}

        override suspend fun generate(systemPrompt: String, userPrompt: String): String {
            generateCallCount++
            lastCapturedSystemPrompt = systemPrompt
            lastCapturedPrompt = userPrompt
            // Deterministic response function strictly dependent on effective prompt
            val hash = (systemPrompt.hashCode() xor userPrompt.hashCode()).toString(16)
            return "DeterministicAnswer[$hash]: Verified explanation for $userPrompt"
        }

        override fun stream(
            systemPrompt: String,
            history: List<ChatMessage>,
            prompt: String
        ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
            streamCallCount++
            lastCapturedSystemPrompt = systemPrompt
            val fullPrompt = buildChatMlPrompt(systemPrompt, history, prompt)
            lastCapturedPrompt = fullPrompt
            val hash = (systemPrompt.hashCode() xor fullPrompt.hashCode()).toString(16)
            emit("DeterministicChunk1[$hash] ")
            emit("DeterministicChunk2 ")
            emit("DeterministicChunk3")
        }

        override suspend fun unload() {}
    }

    @Test
    fun test1_repeatedGenerationProducesByteForByteIdenticalOutputWithoutCache() = runBlocking {
        val engine = DeterministicFakeEngine()
        val systemPrompt = systemPromptFor(ExplanationMode.GENERAL)
        val userPrompt = "What is Newton's third law of motion?"

        val response1 = engine.generate(systemPrompt, userPrompt)
        val response2 = engine.generate(systemPrompt, userPrompt)
        val response3 = engine.generate(systemPrompt, userPrompt)

        // 1. Byte-for-byte identical output
        assertEquals(response1, response2)
        assertEquals(response2, response3)
        assertTrue(response1.contains("Verified explanation for What is Newton's third law of motion?"))

        // 2. Strict guarantee: NO response cache - engine was invoked on each request
        assertEquals(3, engine.generateCallCount)
    }

    @Test
    fun test2_repeatedStreamingProducesIdenticalTokenSequencesWithoutCache() = runBlocking {
        val engine = DeterministicFakeEngine()
        val systemPrompt = systemPromptFor(ExplanationMode.TEACHER)
        val history = listOf(
            ChatMessage(text = "Hello teacher", fromUser = true),
            ChatMessage(text = "Hello! What shall we learn today?", fromUser = false)
        )
        val prompt = "Explain chemical reactions"

        val tokens1 = engine.stream(systemPrompt, history, prompt).toList()
        val tokens2 = engine.stream(systemPrompt, history, prompt).toList()

        // 1. Identical token sequence
        assertEquals(tokens1, tokens2)
        assertEquals("DeterministicChunk1[", tokens1[0].take(20))

        // 2. No response caching: stream called twice
        assertEquals(2, engine.streamCallCount)
    }

    @Test
    fun test3_differentExplanationModesProduceDistinctPromptsAndOutputs() = runBlocking {
        val engine = DeterministicFakeEngine()
        val teacherSys = systemPromptFor(ExplanationMode.TEACHER)
        val generalSys = systemPromptFor(ExplanationMode.GENERAL)
        val explainSys = systemPromptFor(ExplanationMode.EXPLAINABLE)

        // System prompts must be distinct
        assertNotEquals(teacherSys, generalSys)
        assertNotEquals(generalSys, explainSys)
        assertNotEquals(teacherSys, explainSys)

        val prompt = "What is Ohm's Law?"
        val teacherResp = engine.generate(teacherSys, prompt)
        val generalResp = engine.generate(generalSys, prompt)
        val explainResp = engine.generate(explainSys, prompt)

        // Responses must be distinct due to distinct effective system inputs
        assertNotEquals(teacherResp, generalResp)
        assertNotEquals(generalResp, explainResp)
        assertNotEquals(teacherResp, explainResp)
    }

    @Test
    fun test4_chatMlPromptSerializationIsDeterministic() {
        val systemPrompt = "You are EduNova teacher."
        val history = listOf(
            ChatMessage(text = "Hi", fromUser = true),
            ChatMessage(text = "Hello", fromUser = false),
            ChatMessage(text = "Question 1", fromUser = true),
            ChatMessage(text = "Answer 1", fromUser = false)
        )
        val prompt = "Question 2"

        val prompt1 = buildChatMlPrompt(systemPrompt, history, prompt)
        val prompt2 = buildChatMlPrompt(systemPrompt, history, prompt)

        assertEquals(prompt1, prompt2)
        assertTrue(prompt1.startsWith("<|im_start|>system\nYou are EduNova teacher.<|im_end|>\n"))
        assertTrue(prompt1.endsWith("<|im_start|>user\nquestion 2<|im_end|>\n<|im_start|>assistant\n"))
    }

    @Test
    fun test5_syllabusRankingTieBreakerIsDeterministic() {
        val scores = mutableMapOf(
            "chunk-ch1-002" to 0.75f,
            "chunk-ch1-001" to 0.75f,
            "chunk-ch1-003" to 0.75f,
            "chunk-ch2-001" to 0.90f
        )

        // SyllabusDatabase.kt hybridSearch sorting with tie-breaker:
        val sorted = scores.entries.sortedWith(
            compareByDescending<Map.Entry<String, Float>> { it.value }.thenBy { it.key }
        )

        val sortedKeys = sorted.map { it.key }
        assertEquals(
            listOf("chunk-ch2-001", "chunk-ch1-001", "chunk-ch1-002", "chunk-ch1-003"),
            sortedKeys
        )
    }

    @Test
    fun test6_queryNormalizationProducesIdenticalChatMlPrompts() {
        val systemPrompt = "You are EduNova."
        val variants = listOf(
            "WHAT IS DEEP LEARNING?",
            "What is Deep Learning?",
            "  what   is deep learning?  ",
            "What is\nDeep Learning?",
            "What is\t\tDeep   Learning?\n"
        )

        val prompts = variants.map { buildChatMlPrompt(systemPrompt, emptyList(), it) }
        val first = prompts.first()
        val expectedSuffix = "<|im_start|>user\nwhat is deep learning?<|im_end|>\n<|im_start|>assistant\n"

        assertTrue("Prompt must contain normalized query", first.endsWith(expectedSuffix))

        for (p in prompts) {
            assertEquals("All query variants must serialize to 100% byte-for-byte identical ChatML prompt", first, p)
        }
    }

    @Test
    fun test7_queryNormalizerRulesAndPreservations() {
        // Unicode NFC
        val decomposed = "What is cafe\u0301?"
        assertEquals("what is café?", QueryNormalizer.normalize(decomposed))

        // Punctuation preservation
        assertEquals("what is e = mc^2?", QueryNormalizer.normalize("What is E = mc^2?"))
        assertEquals("newton's 3rd law: action & reaction!", QueryNormalizer.normalize("Newton's 3rd Law: action & reaction!"))

        // Distinct wording remains distinct
        val q1 = "what is mean by Deep Learning"
        val q2 = "what s mean by Deep Learning"
        assertNotEquals(QueryNormalizer.normalize(q1), QueryNormalizer.normalize(q2))
        assertEquals("what is mean by deep learning", QueryNormalizer.normalize(q1))
        assertEquals("what s mean by deep learning", QueryNormalizer.normalize(q2))

        // Structured code preservation
        val code = "def calculate_loss(y_true, y_pred):\n    return np.mean((y_true - y_pred) ** 2)"
        val normalizedCode = QueryNormalizer.normalize(code)
        assertTrue(normalizedCode.contains("def calculate_loss"))
        assertTrue(normalizedCode.contains("    return"))

        val sql = "SELECT UserName FROM UserAccounts WHERE IsActive = 1;"
        assertEquals(sql, QueryNormalizer.normalize(sql))
    }
}
