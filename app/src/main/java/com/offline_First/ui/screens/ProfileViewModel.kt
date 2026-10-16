package com.offline_First.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline_First.data.AppContainer
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class ProfileViewModel(
    private val repository: ProfileRepository = AppContainer.profileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUserProfile().collect { cached ->
                if (cached != null && _uiState.value.profile == null) {
                    _uiState.value = _uiState.value.copy(profile = cached, isLoading = false)
                }
            }
        }
        loadProfile()
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
            repository.updateUserProfile(profile).fold(
                onSuccess = {
                    _uiState.value = ProfileUiState(
                        profile = profile,
                        isLoading = false,
                        isSaving = false,
                        saveSuccess = true
                    )
                },
                onFailure = { error ->
                    val userMsg = when (error) {
                        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
                            "Unable to connect to server. Please check your internet connection."
                        else -> error.localizedMessage?.takeIf { !it.contains("Exception") } ?: "Unable to save profile."
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = userMsg
                    )
                }
            )
        }
    }

    fun reload() {
        loadProfile()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getUserProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = ProfileUiState(profile = profile, isLoading = false)
                },
                onFailure = { error ->
                    val userMsg = when (error) {
                        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
                            "Unable to connect to server. Please check your internet connection."
                        else -> error.localizedMessage?.takeIf { !it.contains("Exception") } ?: "Unable to load profile."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = userMsg
                    )
                }
            )
        }
    }
}
