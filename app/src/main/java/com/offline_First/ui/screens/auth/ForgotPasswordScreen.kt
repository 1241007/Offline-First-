package com.offline_First.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.data.repository.RegistrationInput
import com.offline_First.ui.components.AuthHeader
import com.offline_First.ui.components.AuthTextField
import com.offline_First.ui.components.PrimaryAuthButton
import kotlinx.coroutines.launch
import com.offline_First.ui.theme.EduNovaBackground

@Composable
fun ForgotPasswordScreen(
    onLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contact by rememberSaveable { mutableStateOf("") }
    var contactError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.LoginSucceeded -> Unit
                is AuthEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = EduNovaBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AuthHeader(
                title = "Forgot password?",
                subtitle = "Enter your email or mobile number and we'll help you reset your password."
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTextField(
                    label = "Email or mobile number",
                    placeholder = "Email or mobile number",
                    value = contact,
                    onValueChange = { contact = it; contactError = null },
                    error = contactError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                PrimaryAuthButton(
                    text = if (uiState.isSubmitting) "Sending reset link..." else "Send reset link",
                    enabled = !uiState.isSubmitting
                ) {
                    val nextError = validateLoginContact(contact)
                    contactError = nextError
                    if (nextError == null) {
                        viewModel.requestPasswordReset(contact.trim())
                    }
                }
            }
            Row {
                TextButton(onClick = onLogin) {
                    Text("Back to Login", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
