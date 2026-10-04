package com.offline_First.ui.screens.ai

import com.offline_First.data.local.LocalAIRepository
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIViewModelTest {

    @Test
    fun initialStateHasExpectedDefaults() = runBlocking {
        val repo = LocalAIRepository()
        val viewModel = AIViewModel(aiRepository = repo)
        delay(100)

        val state = viewModel.uiState.value
        assertEquals(AIScreen.LANDING, state.currentScreen)
        assertNotNull(state.sessions)
        assertFalse(state.isDrawerOpen)
        assertFalse(state.showDeleteConfirmDialog)
    }

    @Test
    fun switchingScreenUpdatesState() = runBlocking {
        val repo = LocalAIRepository()
        val viewModel = AIViewModel(aiRepository = repo)

        viewModel.setScreen(AIScreen.CHAT)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.DOWNLOAD_STATE)
        assertEquals(AIScreen.DOWNLOAD_STATE, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun switchingModesUpdatesUiState() = runBlocking {
        val repo = LocalAIRepository()
        val viewModel = AIViewModel(aiRepository = repo)
        delay(100)

        viewModel.setConnectionMode(ConnectionMode.OFFLINE)
        delay(100)
        assertEquals(ConnectionMode.OFFLINE, viewModel.uiState.value.connectionMode)

        viewModel.setExplanationMode(ExplanationMode.TEACHER)
        delay(100)
        assertEquals(ExplanationMode.TEACHER, viewModel.uiState.value.explanationMode)
    }

    @Test
    fun deleteOfflineAIConfirmationLifecycle() = runBlocking {
        val repo = LocalAIRepository()
        val viewModel = AIViewModel(aiRepository = repo)

        viewModel.showDeleteConfirm(true)
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)

        viewModel.confirmDeleteOfflineAI()
        delay(100)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun backNavigationNeverEntersDownloadStateLoop() = runBlocking {
        val repo = LocalAIRepository()
        val viewModel = AIViewModel(aiRepository = repo, coroutineContext = kotlinx.coroutines.Dispatchers.Unconfined)

        // User starts at CHAT
        viewModel.setScreen(AIScreen.CHAT)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.currentScreen)
        assertEquals(AIScreen.LANDING, viewModel.uiState.value.previousScreen)

        // User opens SETTINGS from CHAT
        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)

        // User navigates into DOWNLOAD_STATE from SETTINGS
        viewModel.startOfflineDownload(navigateToDownloadState = true)
        assertEquals(AIScreen.DOWNLOAD_STATE, viewModel.uiState.value.currentScreen)
        // previousScreen should still remain CHAT, NOT SETTINGS or DOWNLOAD_STATE
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)

        // User clicks back from DOWNLOAD_STATE to SETTINGS
        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)
        // previousScreen must still be CHAT, NEVER DOWNLOAD_STATE
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)
    }
}
