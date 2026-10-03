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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.data.repository.ProfileRepository
import com.offline_First.domain.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLanguage: () -> Unit = {},
    initialName: String = "Asha Learner",
    initialEmail: String = "asha@example.com",
    profileRepository: ProfileRepository = remember { LocalProfileRepository() }
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var mobile by rememberSaveable { mutableStateOf("+91 98765 43210") }
    var interests by rememberSaveable { mutableStateOf("Android, UI design") }
    var level by rememberSaveable { mutableStateOf("Intermediate") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var savedName by rememberSaveable { mutableStateOf(initialName) }
    var savedEmail by rememberSaveable { mutableStateOf(initialEmail) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(profileRepository) {
        val profileResult = profileRepository.getUserProfile()
        profileResult.onSuccess { profile ->
            name = profile.fullName
            savedName = profile.fullName
            email = profile.email
            savedEmail = profile.email
            mobile = profile.mobile
            interests = profile.interests
            level = profile.level
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                OutlinedTextField(interests, { interests = it }, label = { Text("Learning interests") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(level, { level = it }, label = { Text("Current learning level") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (name.isBlank() || !email.contains("@")) {
                                error = "Enter a valid name and email."
                            } else {
                                savedName = name
                                savedEmail = email
                                error = null
                                editing = false
                                scope.launch {
                                    profileRepository.updateUserProfile(
                                        UserProfile(
                                            fullName = savedName,
                                            email = savedEmail,
                                            mobile = mobile,
                                            interests = interests,
                                            level = level
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) { Text("Save Changes") }
                    OutlinedButton(
                        onClick = {
                            name = savedName
                            email = savedEmail
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
