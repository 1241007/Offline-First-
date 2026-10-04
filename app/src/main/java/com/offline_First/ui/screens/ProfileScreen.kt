package com.offline_First.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import com.offline_First.ui.components.EduNovaFilterChip

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLanguage: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var interests by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var educationMode by rememberSaveable { mutableStateOf(com.offline_First.domain.model.EducationMode.GENERAL) }
    var savedEducationMode by rememberSaveable { mutableStateOf(com.offline_First.domain.model.EducationMode.GENERAL) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var savedName by rememberSaveable { mutableStateOf("") }
    var savedEmail by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { profile ->
            name = profile.fullName
            savedName = profile.fullName
            email = profile.email
            savedEmail = profile.email
            mobile = profile.mobile
            interests = profile.interests
            level = profile.level
            educationMode = profile.educationMode
            savedEducationMode = profile.educationMode
            editing = false
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier.statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape),
                color = primaryColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        savedName.firstOrNull()?.uppercase() ?: "E",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Text(savedName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(savedEmail, color = onSurfaceVariant)
            if (!editing) {
                ProfileValue("Full Name", savedName)
                ProfileValue("Email", savedEmail)
                ProfileValue("Mobile Number", mobile)
                ProfileValue(
                    "Education Mode",
                    if (savedEducationMode == EducationMode.SCHOOL) "School (Class 9–12)" else "General (College & Beyond)"
                )
                ProfileValue("Learning interests", interests)
                ProfileValue("Current learning level", level)
                Button(
                    onClick = { editing = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Edit Profile")
                }
                OutlinedButton(onClick = onLanguage, modifier = Modifier.fillMaxWidth()) {
                    Text("Language")
                }
            } else {
                OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    email,
                    { email = it },
                    label = { Text("Email") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Education Mode",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EduNovaFilterChip(
                            text = "School (Class 9–12)",
                            selected = educationMode == EducationMode.SCHOOL,
                            onClick = { educationMode = EducationMode.SCHOOL }
                        )
                        EduNovaFilterChip(
                            text = "General (College & Beyond)",
                            selected = educationMode == EducationMode.GENERAL,
                            onClick = { educationMode = EducationMode.GENERAL }
                        )
                    }
                }
                OutlinedTextField(interests, { interests = it }, label = { Text("Learning interests") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(level, { level = it }, label = { Text("Current learning level") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (name.isBlank() || !email.contains("@")) {
                                error = "Enter a valid name and email."
                            } else {
                                error = null
                                viewModel.saveProfile(
                                    UserProfile(
                                        fullName = name,
                                        email = email,
                                        mobile = mobile,
                                        interests = interests,
                                        level = level,
                                        educationMode = educationMode
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) { Text("Save Changes") }
                    OutlinedButton(
                        onClick = {
                            name = savedName
                            email = savedEmail
                            mobile = uiState.profile?.mobile.orEmpty()
                            interests = uiState.profile?.interests.orEmpty()
                            level = uiState.profile?.level.orEmpty()
                            educationMode = savedEducationMode
                            error = null
                            editing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
