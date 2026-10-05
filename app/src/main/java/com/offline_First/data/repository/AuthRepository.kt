package com.offline_First.data.repository

import kotlinx.coroutines.flow.Flow

data class RegistrationInput(
    val fullName: String,
    val email: String,
    val mobile: String,
    val password: String
)

interface AuthRepository {
    fun observeSession(): Flow<Boolean>
    suspend fun restoreSession(): Result<Unit>
    suspend fun signIn(contact: String, password: String): Result<Unit>
    suspend fun register(input: RegistrationInput): Result<Unit>
    suspend fun requestPasswordReset(contact: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
