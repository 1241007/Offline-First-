package com.offline_First.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.offline_First.domain.model.LearningCourse
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.UiState

@Composable
fun MyLearningScreen(
    onBack: () -> Unit = {},
    userName: String = "",
    viewModel: MyLearningViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("In Progress", "Completed", "Certificates & Badges") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    val inProgressCourses = (uiState.inProgressCourses as? UiState.Success)?.data.orEmpty()
    val completedCourses = (uiState.completedCourses as? UiState.Success)?.data.orEmpty()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("EduNova", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search courses")
                }
                Text(userName, style = MaterialTheme.typography.labelLarge)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "my-learning-header") {
                Spacer(Modifier.height(8.dp))
                Text("My Learning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Keep building momentum on your learning journey.", color = onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedTab == index) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                tab,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            when (selectedTab) {
                0 -> items(
                    items = inProgressCourses,
                    key = { it.id }
                ) { course ->
                    LearningCourseCard(
                        course = course,
                        actionLabel = "Continue Learning",
                        primaryColor = primaryColor,
                        primaryContainer = primaryContainer,
                        onSurfaceVariant = onSurfaceVariant
                    )
                }
                1 -> items(
                    items = completedCourses,
                    key = { it.id }
                ) { course ->
                    LearningCourseCard(
                        course = course,
                        actionLabel = "View Course",
                        primaryColor = primaryColor,
                        primaryContainer = primaryContainer,
                        onSurfaceVariant = onSurfaceVariant
                    )
                }
                else -> item(key = "empty-certificates") {
                    EmptyCertificatesCard(onSurfaceVariant = onSurfaceVariant)
                }
            }
            item(key = "bottom-spacer") { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LearningCourseCard(
    course: LearningCourse,
    actionLabel: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    primaryContainer: androidx.compose.ui.graphics.Color,
    onSurfaceVariant: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(course.lesson, color = onSurfaceVariant)
                }
                Text("${(course.progress * 100).toInt()}%", color = primaryColor, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { course.progress },
                modifier = Modifier.fillMaxWidth(),
                color = primaryColor,
                trackColor = primaryContainer
            )
            Button(
                onClick = {},
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyCertificatesCard(onSurfaceVariant: androidx.compose.ui.graphics.Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Certificates & Badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Complete a course to see your certificates and badges here.",
                color = onSurfaceVariant
            )
            OutlinedButton(onClick = {}, shape = RoundedCornerShape(10.dp)) {
                Text("Explore courses")
            }
        }
    }
}
