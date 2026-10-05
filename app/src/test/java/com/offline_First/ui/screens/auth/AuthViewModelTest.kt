package com.offline_First.ui.screens.auth

import com.offline_First.data.AuthState
import com.offline_First.data.remote.UserDto
import com.offline_First.data.repository.AuthRepository
import com.offline_First.data.repository.RegistrationInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepository(
    var signInResult: Result<Unit> = Result.success(Unit),
    var registerResult: Result<Unit> = Result.success(Unit),
    var resetResult: Result<Unit> = Result.success(Unit)
) : AuthRepository {
    override suspend fun signIn(contact: String, password: String): Result<Unit> = signInResult
    override suspend fun register(input: RegistrationInput): Result<Unit> = registerResult
    override suspend fun requestPasswordReset(contact: String): Result<Unit> = resetResult
    override suspend fun logout(refreshToken: String?): Result<Unit> = Result.success(Unit)
    override suspend fun restoreSession(): Result<Boolean> = Result.success(true)
    override suspend fun fetchMe(): Result<UserDto> = Result.success(UserDto("u1", "u1@example.com", null, "User One"))
    override fun observeAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun signIn_success_emits_LoginSucceeded() = runTest(testDispatcher) {
        val repo = FakeAuthRepository(signInResult = Result.success(Unit))
        val viewModel = AuthViewModel(repository = repo, coroutineContext = testDispatcher)

        viewModel.signIn("user@example.com", "Password123")
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(AuthEvent.LoginSucceeded, event)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun signIn_failure_sets_errorMessage() = runTest(testDispatcher) {
        val repo = FakeAuthRepository(signInResult = Result.failure(Exception("Invalid credentials")))
        val viewModel = AuthViewModel(repository = repo, coroutineContext = testDispatcher)

        viewModel.signIn("user@example.com", "WrongPassword")
        advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun register_success_emits_LoginSucceeded() = runTest(testDispatcher) {
        val repo = FakeAuthRepository(registerResult = Result.success(Unit))
        val viewModel = AuthViewModel(repository = repo, coroutineContext = testDispatcher)

        viewModel.register(
            RegistrationInput("New User", "new@example.com", "1234567890", "Password123")
        )
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(AuthEvent.LoginSucceeded, event)
    }
}
