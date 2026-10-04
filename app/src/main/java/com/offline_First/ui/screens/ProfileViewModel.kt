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
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository = AppContainer.profileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUserProfile(profile).fold(
                onSuccess = {
                    _uiState.value = ProfileUiState(profile = profile, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Unable to save profile."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            repository.getUserProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = ProfileUiState(profile = profile, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Unable to load profile."
                    )
                }
            )
        }
    }
}
