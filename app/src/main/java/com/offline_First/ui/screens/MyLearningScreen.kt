package com.offline_First.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaTextSecondary

private data class LearningCourse(
    val name: String,
    val lesson: String,
    val progress: Float
)

private val inProgressCourses = listOf(
    LearningCourse("Kotlin Fundamentals", "Lesson 8 of 12", 0.67f),
    LearningCourse("UI Design with Compose", "Lesson 4 of 10", 0.4f),
    LearningCourse("Python for Beginners", "Lesson 6 of 16", 0.38f)
)

private val completedCourses = listOf(
    LearningCourse("Programming Basics", "Completed 12 lessons", 1f),
    LearningCourse("Git and GitHub Essentials", "Completed 8 lessons", 1f)
)

@Composable
fun MyLearningScreen(
    onBack: () -> Unit = {},
    userName: String = "Asha Learner"
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("In Progress", "Completed", "Certificates & Badges")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            item {
                Spacer(Modifier.height(8.dp))
                Text("My Learning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Keep building momentum on your learning journey.", color = EduNovaTextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedTab == index) EduNovaPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                tab,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            when (selectedTab) {
                0 -> items(inProgressCourses) { course ->
                    LearningCourseCard(course, actionLabel = "Continue Learning")
                }
                1 -> items(completedCourses) { course ->
                    LearningCourseCard(course, actionLabel = "View Course")
                }
                else -> item {
                    EmptyCertificatesCard()
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LearningCourseCard(course: LearningCourse, actionLabel: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(course.lesson, color = EduNovaTextSecondary)
                }
                Text("${(course.progress * 100).toInt()}%", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { course.progress },
                modifier = Modifier.fillMaxWidth(),
                color = EduNovaPrimary,
                trackColor = EduNovaPrimaryContainer
            )
            Button(onClick = {}, shape = RoundedCornerShape(10.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyCertificatesCard() {
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
                color = EduNovaTextSecondary
            )
            OutlinedButton(onClick = {}, shape = RoundedCornerShape(10.dp)) {
                Text("Explore courses")
            }
        }
    }
}
