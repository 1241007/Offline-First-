package com.offline_First.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying the Offline Model Lifecycle requirements:
 * 1. Model loads once across multiple messages
 * 2. Model remains loaded across generations
 * 3. Idle shutdown releases model after OFFLINE_MODEL_IDLE_TIMEOUT_MS (3 minutes)
 * 4. Reload after idle shutdown when user sends a new message
 * 5. Do not unload while generation is active
 * 6. Switching to online releases offline model immediately
 * 7. Concurrent requests do not load duplicate models
 * 8. Stop generation cancels without unloading model
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineModelLifecycleTest {

    private class FakeInferenceEngine : OfflineInferenceEngine {
        override var loadCount = 0
        override var unloadCount = 0
        var generateCount = 0
        var cancelCount = 0
        var loadedModelFile: File? = null
        override var isLoaded: Boolean = false
        override val modelInstanceId: String = "fake_instance_123"

        var onGenerateListener: (() -> Unit)? = null

        override suspend fun loadModel(modelFile: File) {
            loadCount++
            loadedModelFile = modelFile
            isLoaded = true
        }

        override suspend fun generate(systemPrompt: String, userPrompt: String): String {
            generateCount++
            onGenerateListener?.invoke()
            return "Response for $userPrompt"
        }

        override fun stream(
            systemPrompt: String,
            history: List<com.offline_First.domain.model.ChatMessage>,
            prompt: String
        ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
            generateCount++
            onGenerateListener?.invoke()
            emit("Token1 ")
            emit("Token2 ")
            emit("Token3")
        }

        override fun cancelCurrentGeneration() {
            cancelCount++
        }

        override suspend fun unload() {
            unloadCount++
            loadedModelFile = null
            isLoaded = false
        }
    }

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var dummyModelFile: File

    @Before
    fun setUp() {
        fakeEngine = FakeInferenceEngine()
        dummyModelFile = File.createTempFile("test-model", ".gguf").apply {
            writeBytes(ByteArray(1024))
            deleteOnExit()
        }
    }

    @Test
    fun test1_modelLoadsOnceAcrossMultipleMessages() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        // User sends 3 consecutive messages
        manager.generate("sys", "Message 1", dummyModelFile)
        manager.generate("sys", "Message 2", dummyModelFile)
        manager.generate("sys", "Message 3", dummyModelFile)

        // Assert: loadModel was invoked only ONCE
        assertEquals("loadModel should only be called once", 1, fakeEngine.loadCount)
        assertEquals("generate should be called 3 times", 3, fakeEngine.generateCount)
        assertTrue("Model should remain loaded", manager.isLoaded)
    }

    @Test
    fun test2_modelRemainsLoadedAcrossGenerations() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        manager.generate("sys", "Generation 1", dummyModelFile)
        assertTrue("Model should be loaded after first generation", fakeEngine.isLoaded)
        val loadedFileInstance = fakeEngine.loadedModelFile

        manager.generate("sys", "Generation 2", dummyModelFile)
        assertTrue("Model should stay loaded after second generation", fakeEngine.isLoaded)
        assertEquals("Model instance/file must remain identical", loadedFileInstance, fakeEngine.loadedModelFile)
        assertEquals("Unload should not have been called", 0, fakeEngine.unloadCount)
    }

    @Test
    fun test3_idleShutdownReleasesModelAfterTimeout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = scope,
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        // Enter offline mode and load model
        manager.onOfflineModeEntered(dummyModelFile)
        assertTrue("Model should be loaded", manager.isLoaded)
        assertEquals(0, fakeEngine.unloadCount)

        // Advance virtual time by 2 minutes (less than 3 min timeout)
        scope.advanceTimeBy(120_000L)
        assertTrue("Model should still be loaded after 2 minutes", manager.isLoaded)
        assertEquals(0, fakeEngine.unloadCount)

        // Advance by remaining 61 seconds (total > 3 minutes)
        scope.advanceTimeBy(61_000L)
        assertFalse("Model should be unloaded after 3 minutes idle", manager.isLoaded)
        assertEquals("Engine should be unloaded once", 1, fakeEngine.unloadCount)
    }

    @Test
    fun test4_reloadAfterIdleShutdownWhenMessageSent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = scope,
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        // Model initially loaded
        manager.ensureModelLoaded(dummyModelFile)
        assertEquals(1, fakeEngine.loadCount)

        // Idle timeout occurs
        scope.advanceTimeBy(OFFLINE_MODEL_IDLE_TIMEOUT_MS + 1000L)
        assertFalse("Model is unloaded due to idle timeout", manager.isLoaded)
        assertEquals(1, fakeEngine.unloadCount)

        // User sends another message after timeout
        val result = manager.generate("sys", "New message after timeout", dummyModelFile)
        assertTrue("Generation should succeed", result.isSuccess)
        assertEquals("Model should be reloaded", 2, fakeEngine.loadCount)
        assertTrue("Model should be loaded again", manager.isLoaded)
    }

    @Test
    fun test5_doNotUnloadDuringGeneration() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = scope,
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        manager.ensureModelLoaded(dummyModelFile)

        var inspectedDuringGeneration = false
        fakeEngine.onGenerateListener = {
            // While generation is actively executing:
            assertTrue("Generation must be flagged as active", manager.isGenerating)
            // Attempt to trigger unload or idle timeout during active generation
            scope.advanceTimeBy(OFFLINE_MODEL_IDLE_TIMEOUT_MS * 2)
            assertTrue("Model must NOT be unloaded while generation is running", manager.isLoaded)
            assertEquals("Unload count must be 0 during active generation", 0, fakeEngine.unloadCount)
            inspectedDuringGeneration = true
        }

        manager.generate("sys", "Prompt", dummyModelFile)
        assertTrue("Inspection during generation should have executed", inspectedDuringGeneration)
        assertFalse("Generation is no longer active", manager.isGenerating)
        assertTrue("Model is still alive after generation finishes", manager.isLoaded)
    }

    @Test
    fun test6_switchingToOnlineReleasesOfflineModel() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        manager.onOfflineModeEntered(dummyModelFile)
        assertTrue("Model should be loaded in offline mode", manager.isLoaded)

        // User switches to Online Mode
        manager.onOnlineModeEntered()
        assertFalse("Offline model should be released when switching to online", manager.isLoaded)
        assertEquals("Engine should be unloaded", 1, fakeEngine.unloadCount)
    }

    @Test
    fun test7_concurrentRequestsDoNotLoadDuplicateModels() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        // Launch 10 concurrent requests to load model
        val jobs = (1..10).map {
            async(Dispatchers.Default) {
                manager.ensureModelLoaded(dummyModelFile)
            }
        }
        val results = jobs.awaitAll()
        assertTrue("All load requests should succeed", results.all { it.isSuccess })
        assertEquals("loadModel must only be called ONCE even with 10 concurrent calls", 1, fakeEngine.loadCount)
    }

    @Test
    fun test8_stopGenerationCancelsWithoutUnloadingModel() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        manager.ensureModelLoaded(dummyModelFile)
        assertTrue(manager.isLoaded)

        // Stop generation
        manager.cancelGeneration()
        assertEquals("cancelCurrentGeneration should be invoked", 1, fakeEngine.cancelCount)
        assertEquals("unload should NOT be invoked when cancelling generation", 0, fakeEngine.unloadCount)
        assertTrue("Model must remain loaded in memory after cancelling", manager.isLoaded)
    }

    @Test
    fun test9_modelInstanceIdRemainsIdenticalAcrossConsecutiveMessages() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        // Turn 1
        manager.generate("sys", "Msg 1", dummyModelFile)
        val id1 = manager.modelInstanceId
        assertNotNull("Model instance ID must be non-null", id1)

        // Turn 2
        manager.generate("sys", "Msg 2", dummyModelFile)
        val id2 = manager.modelInstanceId
        assertEquals("Model instance ID must remain identical on 2nd response", id1, id2)

        // Turn 3
        manager.generate("sys", "Msg 3", dummyModelFile)
        val id3 = manager.modelInstanceId
        assertEquals("Model instance ID must remain identical on 3rd response", id1, id3)

        assertEquals("loadModel must only be called ONCE for 3 consecutive messages", 1, fakeEngine.loadCount)
        assertEquals("unload must NOT be called between consecutive messages", 0, fakeEngine.unloadCount)
    }

    @Test
    fun test10_streamingEmitsTokensProgressivelyBeforeCompletion() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = OfflineModelManager(
            engine = fakeEngine,
            coroutineScope = TestScope(testDispatcher),
            idleTimeoutMs = OFFLINE_MODEL_IDLE_TIMEOUT_MS
        )

        val streamFlow = manager.stream(
            systemPrompt = "sys",
            history = emptyList(),
            userPrompt = "Stream this",
            modelFile = dummyModelFile
        )
        val firstToken = streamFlow.first()
        assertEquals("Token1 ", firstToken)

        val allTokens = mutableListOf<String>()
        streamFlow.collect { allTokens.add(it) }
        assertEquals(listOf("Token1 ", "Token2 ", "Token3"), allTokens)
    }

    @Test
    fun test11_chatMlPromptEnablesPrefixMatching() {
        val sysPrompt = "You are an AI teacher."
        val turn1Prompt = buildChatMlPrompt(
            systemPrompt = sysPrompt,
            history = emptyList(),
            newPrompt = "Hello teacher"
        )

        // Expected Turn 1 structure
        val expectedTurn1 = "<|im_start|>system\nYou are an AI teacher.<|im_end|>\n<|im_start|>user\nHello teacher<|im_end|>\n<|im_start|>assistant\n"
        assertEquals(expectedTurn1, turn1Prompt)

        // Turn 2 with history
        val history = listOf(
            com.offline_First.domain.model.ChatMessage(text = "Hello teacher", fromUser = true),
            com.offline_First.domain.model.ChatMessage(text = "Hello student! How can I help?", fromUser = false)
        )
        val turn2Prompt = buildChatMlPrompt(
            systemPrompt = sysPrompt,
            history = history,
            newPrompt = "What is gravity?"
        )

        // Verify Turn 2 contains Turn 1 prompt + assistant response prefix
        val expectedTurn2Prefix = "<|im_start|>system\nYou are an AI teacher.<|im_end|>\n<|im_start|>user\nHello teacher<|im_end|>\n<|im_start|>assistant\nHello student! How can I help?<|im_end|>\n"
        assertTrue("Turn 2 must start with exact Turn 1 prefix for 100% KV cache hit", turn2Prompt.startsWith(expectedTurn2Prefix))
    }
}
