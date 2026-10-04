package com.offline_First.data

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIRepositoryTest {

    @Test
    fun verifyConnectionAndExplanationModeSwitching() = runBlocking {
        val repo = LocalAIRepository()

        // Switch to Offline
        repo.setConnectionMode(ConnectionMode.OFFLINE)
        assertEquals(ConnectionMode.OFFLINE, repo.observeConnectionMode().first())

        // Switch to Online
        repo.setConnectionMode(ConnectionMode.ONLINE)
        assertEquals(ConnectionMode.ONLINE, repo.observeConnectionMode().first())

        // Switch through all 3 explanation modes
        repo.setExplanationMode(ExplanationMode.TEACHER)
        assertEquals(ExplanationMode.TEACHER, repo.observeExplanationMode().first())

        repo.setExplanationMode(ExplanationMode.EXPLAINABLE)
        assertEquals(ExplanationMode.EXPLAINABLE, repo.observeExplanationMode().first())

        repo.setExplanationMode(ExplanationMode.GENERAL)
        assertEquals(ExplanationMode.GENERAL, repo.observeExplanationMode().first())
    }

    @Test
    fun verifyOfflineDownloadAndDeletionLifecycle() = runBlocking {
        val repo = LocalAIRepository()

        // Start download
        val startResult = repo.startOfflineAIDownload()
        assertTrue(startResult.isSuccess)

        // Verify download size is generic (e.g. 1.8 GB or 2.4 GB) and no technical specs are exposed
        val progress = repo.observeDownloadProgress().first()
        assertTrue(progress.downloadSize.endsWith("GB"))
        assertFalse(progress.downloadSize.contains("RAM"))
        assertFalse(progress.downloadSize.contains("Model"))

        // Delete Offline AI
        val deleteResult = repo.deleteOfflineAI()
        assertTrue(deleteResult.isSuccess)
        assertEquals(OfflineAIStatus.NOT_DOWNLOADED, repo.observeOfflineAIStatus().first())
    }

    @Test
    fun verifyCleanInitialSessionsAndChatManagement() = runBlocking {
        LocalAIRepository.resetState()
        val repo = LocalAIRepository()
        val initialSessions = repo.observeChatSessions().first()

        assertTrue("Starts with clean session list without dummy data", initialSessions.isEmpty())

        // Create new chat
        val newChat = repo.createNewChat().getOrThrow()
        assertEquals("New Conversation", newChat.title)

        // Rename chat
        repo.renameChat(newChat.id, "Compiler Design Notes")
        val updated = repo.observeChatSessions().first().first { it.id == newChat.id }
        assertEquals("Compiler Design Notes", updated.title)

        // Delete chat
        repo.deleteChat(newChat.id)
        assertFalse(repo.observeChatSessions().first().any { it.id == newChat.id })
    }

    @Test
    fun verifyTeacherExplanationToneInResponses() = runBlocking {
        val repo = LocalAIRepository()
        repo.setConnectionMode(ConnectionMode.ONLINE)
        repo.setExplanationMode(ExplanationMode.TEACHER)

        val response = repo.sendMessage("Explain Binary Search").getOrThrow()
        assertNotNull(response)
        assertFalse(response.fromUser)
        assertTrue(response.text.contains("Teacher's Explanation"))
        assertTrue(response.text.contains("Practice Question"))
    }

    @Test
    fun verifyExplainableModeReasoningInResponses() = runBlocking {
        val repo = LocalAIRepository()
        repo.setConnectionMode(ConnectionMode.ONLINE)
        repo.setExplanationMode(ExplanationMode.EXPLAINABLE)

        val response = repo.sendMessage("Explain Binary Search").getOrThrow()
        assertNotNull(response)
        assertFalse(response.fromUser)
        assertTrue(response.text.contains("Reasoning"))
    }
}
