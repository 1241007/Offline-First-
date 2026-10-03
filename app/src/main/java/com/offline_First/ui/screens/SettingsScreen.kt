package com.offline_First.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    language: String = "English",
    onLanguageSelected: (String) -> Unit = {}
) {
    var notifications by rememberSaveable { mutableStateOf(true) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val languages = listOf("English", "Hindi", "Marathi")

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
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            SettingsSection("ACCOUNT") {
                SettingsRow("Profile", "Manage your personal information", onOpenProfile)
                SettingsRow("Change Password", "Password changes are demo-only") {}
                LanguageRow(language, languageMenuExpanded, { languageMenuExpanded = true }, {
                    languageMenuExpanded = false
                    onLanguageSelected(it)
                }, languages)
            }
            SettingsSection("PREFERENCES") {
                ToggleRow("Notifications", "Receive learning reminders", notifications) { notifications = it }
                ToggleRow("Theme / Appearance", "Use dark appearance", darkTheme) { darkTheme = it }
            }
            SettingsSection("APP") {
                SettingsRow("About EduNova", "A focused learning experience") {}
                SettingsRow("Help & Support", "Support resources are being prepared") {}
                SettingsRow("Privacy / Terms", "Review demo policies") {}
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 10.dp), content = {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        content()
    })
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageRow(
    language: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onSelect: (String) -> Unit,
    languages: List<String>
) {
    Column {
        TextButton(onClick = onExpand, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Language", color = MaterialTheme.colorScheme.onSurface)
                    Text(language, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Choose")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onSelect(language) }) {
            languages.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}
