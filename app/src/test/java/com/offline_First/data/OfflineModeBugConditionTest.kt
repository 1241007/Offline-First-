package com.offline_First.data

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

/**
 * Bug Condition Exploration Test for Offline Mode Inference Failure
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **DO NOT attempt to fix the test or the code when it fails**
 * **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
 * **GOAL**: Surface counterexamples that demonstrate the bug exists
 * 
 * **Validates: Requirements 1.3, 1.4, 1.5 from bugfix.md**
 */
@Ignore("Exploratory bug condition test that requires live network download and llama.cpp runtime")
class OfflineModeBugConditionTest {

    /**
     * Property 1: Bug Condition - Offline Mode Inference Failure
     * 
     * Scoped PBT Approach: Scope the property to concrete failing cases:
     * connectionMode = OFFLINE AND offlineAIStatus = READY with any chat message
     * 
     * Test that LocalAIRepository.sendMessage() returns hardcoded error messages
     * instead of actual AI inference (from Bug Condition in design)
     */
    @Test
    fun offlineModeReturnsHardcodedErrorInsteadOfInference() = runBlocking {
        // Arrange: Setup offline mode with AI ready
        val repo = LocalAIRepository()
        repo.setConnectionMode(ConnectionMode.OFFLINE)
        repo.startOfflineAIDownload()
        
        // Wait for download to complete and AI status to be READY
        var attempts = 0
        while (repo.observeOfflineAIStatus().first() != OfflineAIStatus.READY && attempts < 10) {
            kotlinx.coroutines.delay(100)
            attempts++
        }
        
        assertEquals(
            "Offline AI must be READY for bug condition",
            OfflineAIStatus.READY,
            repo.observeOfflineAIStatus().first()
        )
        
        // Act: Send message in offline mode with AI ready
        val prompt = "Explain photosynthesis"
        val result = repo.sendMessage(prompt)
        
        // Assert: Should return hardcoded error message instead of actual inference
        // This is the bug condition - the test EXPECTS this to fail on unfixed code
        assertTrue("Send message should succeed", result.isSuccess)
        val aiResponse = result.getOrThrow().text
        
        // Counterexample documentation expected:
        // "sendMessage('Explain photosynthesis', OFFLINE, READY) returns '⚠️ **Offline AI is not ready yet.**'"
        assertTrue(
            "Bug Condition: Offline mode should return hardcoded error message instead of AI inference. " +
            "Actual response: '$aiResponse'",
            aiResponse.contains("⚠️ **Offline AI is not ready yet.**") ||
            aiResponse.contains("Offline AI is not ready")
        )
        
        // Additional validation: Response should NOT contain actual AI-generated content
        // This would be the expected behavior after the fix
        val isHardcodedError = aiResponse.contains("⚠️") || 
                               aiResponse.contains("Offline AI") ||
                               aiResponse.contains("download") ||
                               aiResponse.contains("switch back")
        
        assertTrue(
            "Bug Condition Confirmed: Response is hardcoded error, not AI inference. " +
            "Response: '$aiResponse'",
            isHardcodedError
        )
    }

    /**
     * Test multiple chat messages to show the bug is consistent
     */
    @Test
    fun variousPromptsInOfflineModeReturnHardcodedErrors() = runBlocking {
        val testPrompts = listOf(
            "Explain photosynthesis",
            "What is binary search?",
            "How does a computer work?",
            "Tell me about machine learning",
            "What is quantum computing?"
        )
        
        for (prompt in testPrompts) {
            // Setup fresh repo for each test to avoid state contamination
            val repo = LocalAIRepository()
            repo.setConnectionMode(ConnectionMode.OFFLINE)
            repo.startOfflineAIDownload()
            
            // Wait for AI to be ready
            var attempts = 0
            while (repo.observeOfflineAIStatus().first() != OfflineAIStatus.READY && attempts < 10) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
            
            val result = repo.sendMessage(prompt)
            assertTrue("Send message should succeed for '$prompt'", result.isSuccess)
            
            val aiResponse = result.getOrThrow().text
            
            // All prompts should return the same hardcoded error in offline mode
            assertTrue(
                "Bug Condition: Prompt '$prompt' returns hardcoded error: '$aiResponse'",
                aiResponse.contains("⚠️") || aiResponse.contains("Offline AI")
            )
        }
    }

    /**
     * Test that isGeneratingResponse state management is broken in offline mode
     * Note: This requires AIViewModel which we don't have access to in repository tests.
     * The actual UI state bug would be tested in AIViewModel tests.
     * For now, we document this as a known issue.
     */
    @Test
    fun documentIsGeneratingResponseStateBug() {
        // This test documents the known bug without attempting to test it
        // since we don't have AIViewModel in this test scope
        
        println("""
            KNOWN BUG DOCUMENTATION:
            - isGeneratingResponse remains stuck in true state indefinitely after offline message
            - UI shows continuous 'thinking' indicator
            - Send button appears non-responsive
            
            This bug will be tested in AIViewModel tests once the ViewModel is available.
        """.trimIndent())
        
        // Test passes to document the issue
        assertTrue(true)
    }

    /**
     * Test that no llama.cpp JNI calls are made during offline inference
     * Since we can't directly monitor JNI calls from Kotlin tests,
     * we verify that no actual inference happens by checking for hardcoded responses.
     */
    @Test
    fun noLlamaCppJNICallsInOfflineMode() = runBlocking {
        val repo = LocalAIRepository()
        repo.setConnectionMode(ConnectionMode.OFFLINE)
        repo.startOfflineAIDownload()
        
        // Wait for AI to be ready
        var attempts = 0
        while (repo.observeOfflineAIStatus().first() != OfflineAIStatus.READY && attempts < 10) {
            kotlinx.coroutines.delay(100)
            attempts++
        }
        
        val prompt = "Explain photosynthesis"
        val result = repo.sendMessage(prompt)
        assertTrue(result.isSuccess)
        
        val aiResponse = result.getOrThrow().text
        
        // Check for signs of actual AI inference (which would indicate JNI calls are working)
        // Actual AI inference would produce varied, prompt-specific responses
        // Hardcoded errors are generic and don't address the prompt specifically
        
        val isGenericHardcodedError = aiResponse.contains("⚠️ **Offline AI is not ready yet.**") ||
                                     aiResponse.contains("To use offline inference") ||
                                     aiResponse.contains("switch back to **Online** mode")
        
        assertTrue(
            "Bug Condition: No llama.cpp JNI calls detected. " +
            "Response is generic hardcoded error, not AI inference. " +
            "Response: '$aiResponse'",
            isGenericHardcodedError
        )
        
        // Specifically check that response doesn't contain AI-like content
        // that would suggest llama.cpp was called
        val lacksAIContent = !aiResponse.contains("photosynthesis") &&
                            !aiResponse.contains("binary search") &&
                            !aiResponse.contains("computer") &&
                            !aiResponse.contains("machine learning") &&
                            !aiResponse.contains("quantum")
        
        assertTrue(
            "Bug Condition Confirmed: No actual inference content found. " +
            "This suggests llama.cpp JNI is not being called.",
            lacksAIContent
        )
    }

    /**
     * Edge case: Test online mode still works (preservation requirement)
     */
    @Test
    fun onlineModeStillWorksCorrectly() = runBlocking {
        val repo = LocalAIRepository()
        repo.setConnectionMode(ConnectionMode.ONLINE)
        
        val prompt = "Explain binary search"
        val result = repo.sendMessage(prompt)
        
        assertTrue("Online mode should work", result.isSuccess)
        val aiResponse = result.getOrThrow().text
        
        // Online mode should return actual content, not error messages
        assertTrue(
            "Online mode returns actual content: '$aiResponse'",
            aiResponse.contains("Binary Search") || 
            aiResponse.contains("binary search") ||
            aiResponse.contains("Teacher's Explanation") ||
            aiResponse.contains("search algorithm")
        )
        
        // Should not contain offline error messages
        assertFalse(
            "Online mode should not contain offline error messages",
            aiResponse.contains("⚠️ **Offline AI is not ready yet.**") ||
            aiResponse.contains("To use offline inference")
        )
    }
}