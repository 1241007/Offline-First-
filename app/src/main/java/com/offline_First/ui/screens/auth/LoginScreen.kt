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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offline_First.ui.components.AuthHeader
import com.offline_First.ui.components.AuthPasswordField
import com.offline_First.ui.components.AuthTextField
import com.offline_First.ui.components.PrimaryAuthButton
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onLoginSuccess: () -> Unit = {}
) {
    var contact by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var contactError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAFD),
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
                title = "Welcome back",
                subtitle = "Continue your learning journey."
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthTextField(
                    label = "Email or mobile number",
                    placeholder = "Email or mobile number",
                    value = contact,
                    onValueChange = { contact = it; contactError = null },
                    error = contactError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )
                AuthPasswordField(
                    label = "Password",
                    placeholder = "Password",
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    error = passwordError,
                    imeAction = ImeAction.Done
                )
                Row {
                    TextButton(
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Password recovery will be available soon.") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Forgot password?", fontWeight = FontWeight.SemiBold)
                    }
                }
                PrimaryAuthButton(text = "Login") {
                    val nextContactError = validateLoginContact(contact)
                    val nextPasswordError = when {
                        password.isBlank() -> "Password cannot be empty."
                        password.length < 6 -> "Password must contain at least 6 characters."
                        else -> null
                    }
                    contactError = nextContactError
                    passwordError = nextPasswordError
                    if (nextContactError == null && nextPasswordError == null) {
                        onLoginSuccess()
                    }
                }
            }
            Row {
                Text("New to EduNova?", color = Color(0xFF5F6B85))
                TextButton(onClick = onRegister) { Text("Create an account") }
            }
        }
    }
}