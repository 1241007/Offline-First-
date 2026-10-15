package com.offline_First.data.repository

import com.offline_First.data.AuthState
import com.offline_First.data.remote.UserDto
import kotlinx.coroutines.flow.Flow

data class RegistrationInput(
    val fullName: String,
    val email: String,
    val mobile: String,
    val password: String
)

interface AuthRepository {
    suspend fun signIn(contact: String, password: String): Result<Unit>
    suspend fun register(input: RegistrationInput): Result<Unit>
    suspend fun requestPasswordReset(contact: String): Result<Unit>
    suspend fun logout(refreshToken: String? = null): Result<Unit>
    suspend fun restoreSession(): Result<Boolean>
    suspend fun fetchMe(): Result<UserDto>
    fun observeAuthState(): Flow<AuthState>
}
