package com.offline_First.data

import com.offline_First.data.repository.BackendNotConfiguredException
import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIRepositoryTest {

    @Test
    fun aiPreferencesCanBeChangedWithoutPretendingBackendIsConnected() = runBlocking {
        val repository = BackendNotConfiguredRepositories()

        repository.setConnectionMode(ConnectionMode.OFFLINE).getOrThrow()
        assertEquals(ConnectionMode.OFFLINE, repository.observeConnectionMode().first())

        repository.setExplanationMode(ExplanationMode.TEACHER).getOrThrow()
        assertEquals(ExplanationMode.TEACHER, repository.observeExplanationMode().first())
    }

    @Test
    fun offlineModelAndChatOperationsReturnExplicitIntegrationErrors() = runBlocking {
        val repository = BackendNotConfiguredRepositories()

        assertEquals(OfflineAIStatus.NOT_DOWNLOADED, repository.observeOfflineAIStatus().first())
        assertTrue(repository.observeChatSessions().first().isEmpty())
        assertEquals(null, repository.observeCurrentSession().first())

        assertBackendNotConfigured(repository.startOfflineAIDownload().exceptionOrNull())
        assertBackendNotConfigured(repository.deleteOfflineAI().exceptionOrNull())
        assertBackendNotConfigured(repository.createNewChat().exceptionOrNull())
        assertBackendNotConfigured(repository.sendMessage("Explain binary search").exceptionOrNull())
    }

    private fun assertBackendNotConfigured(error: Throwable?) {
        assertTrue(error is BackendNotConfiguredException)
        assertTrue(error?.localizedMessage?.contains("Backend contract required") == true)
    }
}
