package com.offline_First.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.AuthRepository
import com.offline_First.data.repository.RegistrationInput
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AuthEvent {
    data object LoginSucceeded : AuthEvent
    data class Message(val text: String) : AuthEvent
}

data class AuthUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AppContainer.authRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun signIn(contact: String, password: String) {
        submit(
            operation = { repository.signIn(contact, password) },
            onSuccess = { eventChannel.send(AuthEvent.LoginSucceeded) }
        )
    }

    fun register(input: RegistrationInput) {
        submit(
            operation = { repository.register(input) },
            onSuccess = {
                eventChannel.send(AuthEvent.LoginSucceeded)
            }
        )
    }

    fun requestPasswordReset(contact: String) {
        submit(
            operation = { repository.requestPasswordReset(contact) },
            onSuccess = {
                eventChannel.send(
                    AuthEvent.Message(
                        "If an account exists with these details, password reset instructions will be sent."
                    )
                )
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun submit(
        operation: suspend () -> Result<Unit>,
        onSuccess: suspend () -> Unit
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch(coroutineContext) {
            _uiState.value = AuthUiState(isSubmitting = true)
            val result = operation()
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = formatAuthError(error)
                    )
                }
            )
            if (result.isSuccess) {
                _uiState.value = AuthUiState()
            } else {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    private fun formatAuthError(error: Throwable): String {
        val msg = error.localizedMessage ?: ""
        return when {
            error is java.net.UnknownHostException || error is java.net.ConnectException || msg.contains("Failed to connect", ignoreCase = true) ->
                "Unable to connect to EduNova server. Please check your internet connection and try again."
            error is java.net.SocketTimeoutException || msg.contains("timeout", ignoreCase = true) ->
                "Connection timed out. The server may be waking up, please try again in a moment."
            else -> msg.ifBlank { "The request could not be completed." }
        }
    }
}
