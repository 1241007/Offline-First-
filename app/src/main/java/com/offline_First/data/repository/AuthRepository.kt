package com.offline_First.data.repository

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
}
