package com.offline_First.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.data.repository.RegistrationInput
import com.offline_First.ui.components.AuthHeader
import com.offline_First.ui.components.AuthPasswordField
import com.offline_First.ui.components.AuthTextField
import com.offline_First.ui.components.PrimaryAuthButton
import com.offline_First.ui.theme.EduNovaBackground
import com.offline_First.ui.theme.EduNovaError
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    onLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var mobileError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var termsError by rememberSaveable { mutableStateOf<String?>(null) }
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
    val uiState by viewModel.uiState.collectAsState()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthHeader(
                title = "Create your account",
                subtitle = "Start your learning journey with EduNova."
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTextField(
                    label = "Full name",
                    placeholder = "Enter your full name",
                    value = fullName,
                    onValueChange = { fullName = it; nameError = null },
                    error = nameError
                )
                AuthTextField(
                    label = "Email",
                    placeholder = "Enter your email",
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    error = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    )
                )
                AuthTextField(
                    label = "Mobile number",
                    placeholder = "Enter your mobile number",
                    value = mobile,
                    onValueChange = { mobile = it; mobileError = null },
                    error = mobileError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    )
                )
                AuthPasswordField(
                    label = "Password",
                    placeholder = "Create a password",
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    error = passwordError,
                    imeAction = ImeAction.Next
                )
                AuthPasswordField(
                    label = "Confirm password",
                    placeholder = "Confirm your password",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmError = null },
                    error = confirmError,
                    imeAction = ImeAction.Done
                )
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it; termsError = null }
                    )
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        FlowRow {
                            Text("I agree to the ")
                            Text(
                                text = "Terms & Conditions",
                                color = EduNovaPrimary,
                                modifier = Modifier.clickable(
                                    role = Role.Button,
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Terms and Privacy Policy will be available soon."
                                            )
                                        }
                                    }
                                )
                            )
                        }
                        Text(
                            text = "and Privacy Policy",
                            color = EduNovaPrimary,
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Terms and Privacy Policy will be available soon."
                                        )
                                    }
                                }
                            )
                        )
                        termsError?.let { Text(it, color = EduNovaError) }
                    }
                }
                PrimaryAuthButton(text = "Create Account") {
                    val nextNameError = if (fullName.isBlank()) "Please enter your name." else null
                    val nextEmailError = validateEmail(email)
                    val nextMobileError = validateMobile(mobile)
                    val nextPasswordError = when {
                        password.isBlank() -> "Password cannot be empty."
                        password.length < 6 -> "Password must contain at least 6 characters."
                        else -> null
                    }
                    val nextConfirmError = when {
                        confirmPassword.isBlank() -> "Please confirm your password."
                        confirmPassword != password -> "Passwords do not match."
                        else -> null
                    }
                    val nextTermsError = if (!termsAccepted) "Please accept the Terms & Conditions." else null
                    nameError = nextNameError
                    emailError = nextEmailError
                    mobileError = nextMobileError
                    passwordError = nextPasswordError
                    confirmError = nextConfirmError
                    termsError = nextTermsError
                    if (nextNameError == null && nextEmailError == null && nextMobileError == null &&
                        nextPasswordError == null && nextConfirmError == null && nextTermsError == null
                    ) {
                        viewModel.register(
                            RegistrationInput(
                                fullName = fullName.trim(),
                                email = email.trim(),
                                mobile = mobile.trim(),
                                password = password
                            )
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = EduNovaTextSecondary)
                TextButton(onClick = onLogin) { Text("Login") }
            }
        }
    }
}