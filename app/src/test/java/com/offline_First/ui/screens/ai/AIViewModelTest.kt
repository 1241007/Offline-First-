package com.offline_First.ui.screens.ai

import com.offline_First.data.repository.BackendNotConfiguredRepositories
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIViewModelTest {

    @Test
    fun initialStateHasExpectedDefaults() = runBlocking {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertEquals(AIScreen.LANDING, state.currentScreen)
        assertTrue(state.sessions.isEmpty())
        assertFalse(state.isDrawerOpen)
        assertFalse(state.showDeleteConfirmDialog)
    }

    @Test
    fun switchingScreenUpdatesState() = runBlocking {
        val viewModel = newViewModel()

        viewModel.setScreen(AIScreen.CHAT)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.SETTINGS)
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)

        viewModel.setScreen(AIScreen.DOWNLOAD_STATE)
        assertEquals(AIScreen.DOWNLOAD_STATE, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun switchingModesUpdatesUiState() = runBlocking {
        val viewModel = newViewModel()

        viewModel.setConnectionMode(ConnectionMode.OFFLINE)
        assertEquals(ConnectionMode.OFFLINE, viewModel.uiState.value.connectionMode)

        viewModel.setExplanationMode(ExplanationMode.TEACHER)
        assertEquals(ExplanationMode.TEACHER, viewModel.uiState.value.explanationMode)
    }

    @Test
    fun deleteOfflineAIConfirmationLifecycle() = runBlocking {
        val viewModel = newViewModel()

        viewModel.showDeleteConfirm(true)
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)

        viewModel.confirmDeleteOfflineAI()
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Offline AI") == true)
    }

    @Test
    fun backNavigationNeverEntersDownloadStateLoop() = runBlocking {
        val viewModel = newViewModel()

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
        assertEquals(AIScreen.SETTINGS, viewModel.uiState.value.currentScreen)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Offline AI") == true)
        assertEquals(AIScreen.CHAT, viewModel.uiState.value.previousScreen)

        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    private fun newViewModel() = AIViewModel(
        aiRepository = BackendNotConfiguredRepositories(),
        coroutineContext = Dispatchers.Unconfined
    )
}
