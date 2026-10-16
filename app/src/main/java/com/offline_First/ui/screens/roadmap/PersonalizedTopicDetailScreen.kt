package com.offline_First.ui.screens.roadmap

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.domain.model.TopicCodeExample
import com.offline_First.domain.model.TopicCommonMistake
import com.offline_First.domain.model.TopicLearningContent
import com.offline_First.domain.model.TopicLearningResource
import com.offline_First.domain.model.TopicPracticeQuiz
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedTopicDetailScreen(
    onBack: () -> Unit,
    viewModel: PersonalizedRoadmapViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val content = uiState.activeTopicContent
    val roadmap = uiState.activeRoadmap
    val isCompleted = roadmap?.completedTopicIds?.contains(content?.topicId ?: "") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = content?.topicTitle ?: "Topic Lesson",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${content?.domain ?: roadmap?.domain ?: "Course"} • Personalized Module",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Roadmap"
                        )
                    }
                },
                actions = {
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EduNovaSuccess.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduNovaSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EduNovaSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to Roadmap")
                    }

                    Button(
                        onClick = {
                            val rId = roadmap?.id ?: return@Button
                            val tId = content?.topicId ?: return@Button
                            val passed = uiState.topicQuizAnswerCorrect ?: true
                            viewModel.completeTopic(rId, tId, quizPassed = passed) {
                                Toast.makeText(context, "Topic marked completed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !uiState.isCompletingTopic && content != null,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) EduNovaSuccess else EduNovaPrimary
                        )
                    ) {
                        if (uiState.isCompletingTopic) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Completed ✓")
                        } else {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Completed")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoadingTopicContent -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = EduNovaPrimary)
                        Text(
                            text = "Generating comprehensive lesson...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.errorMessage ?: "Topic content not available",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onBack) {
                            Text("Return to Roadmap")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Objectives
                    if (content.learningObjectives.isNotEmpty()) {
                        item(key = "objectives") {
                            LearningObjectivesCard(objectives = content.learningObjectives)
                        }
                    }

                    // 2. Beginner Explanation
                    if (content.beginnerExplanation.isNotBlank()) {
                        item(key = "explanation") {
                            ConceptExplanationCard(
                                title = "Concept Overview",
                                explanation = content.beginnerExplanation
                            )
                        }
                    }

                    // 3. Syntax & Terminology
                    if (content.syntaxAndConcepts.isNotBlank()) {
                        item(key = "syntax") {
                            SyntaxConceptsCard(syntax = content.syntaxAndConcepts)
                        }
                    }

                    // 4. Code Examples
                    if (content.codeExamples.isNotEmpty()) {
                        item(key = "code_header") {
                            SectionHeader(
                                title = "Practical Code Examples",
                                icon = Icons.Default.Code
                            )
                        }
                        items(content.codeExamples) { example ->
                            CodeExampleCard(example = example)
                        }
                    }

                    // 5. Common Mistakes
                    if (content.commonMistakes.isNotEmpty()) {
                        item(key = "mistakes_header") {
                            SectionHeader(
                                title = "Common Mistakes & Fixes",
                                icon = Icons.Default.WarningAmber
                            )
                        }
                        items(content.commonMistakes) { mistake ->
                            CommonMistakeCard(mistake = mistake)
                        }
                    }

                    // 6. Interactive Practice Quiz
                    content.practiceQuiz?.let { quiz ->
                        item(key = "quiz") {
                            InteractivePracticeQuizCard(
                                quiz = quiz,
                                selectedIndex = uiState.topicQuizSelectedIndex,
                                isCorrect = uiState.topicQuizAnswerCorrect,
                                explanation = uiState.topicQuizExplanation,
                                onOptionSelected = { idx ->
                                    viewModel.submitTopicQuizAnswer(idx)
                                }
                            )
                        }
                    }

                    // 7. Verified Learning Resources
                    if (content.resources.isNotEmpty()) {
                        item(key = "resources_header") {
                            SectionHeader(
                                title = "Verified Learning Resources",
                                icon = Icons.Default.MenuBook
                            )
                        }
                        items(content.resources) { resource ->
                            VerifiedResourceCard(
                                resource = resource,
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open URL: $url", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EduNovaPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LearningObjectivesCard(objectives: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EduNovaPrimary.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Learning Objectives",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaPrimary
                )
            }

            objectives.forEach { obj ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = EduNovaSuccess,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = obj,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ConceptExplanationCard(title: String, explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SyntaxConceptsCard(syntax: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = EduNovaAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Core Syntax & Concepts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = syntax,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun CodeExampleCard(example: TopicCodeExample) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = example.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Code block
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = example.code,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (example.expectedOutput.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Output:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = example.expectedOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (example.explanation.isNotBlank()) {
                Text(
                    text = example.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommonMistakeCard(mistake: TopicCommonMistake) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mistake.mistake,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Why it happens: ${mistake.whyItHappens}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduNovaSuccess.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EduNovaSuccess,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to fix: ${mistake.howToFix}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractivePracticeQuizCard(
    quiz: TopicPracticeQuiz,
    selectedIndex: Int?,
    isCorrect: Boolean?,
    explanation: String?,
    onOptionSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaSecondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Quiz,
                    contentDescription = null,
                    tint = EduNovaSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Knowledge Check",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaSecondary
                )
            }

            Text(
                text = quiz.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quiz.options.forEachIndexed { index, optionText ->
                    val isThisSelected = selectedIndex == index
                    val isThisTheCorrectAnswer = quiz.correctOptionIndex == index

                    val (containerColor, borderColor) = when {
                        selectedIndex == null -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        isThisSelected && isCorrect == true -> EduNovaSuccess.copy(alpha = 0.15f) to EduNovaSuccess
                        isThisSelected && isCorrect == false -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f) to MaterialTheme.colorScheme.error
                        isThisTheCorrectAnswer && selectedIndex != null -> EduNovaSuccess.copy(alpha = 0.1f) to EduNovaSuccess.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(index) },
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A' + index)}. ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedIndex != null) {
                                if (isThisSelected && isCorrect == true) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EduNovaSuccess, modifier = Modifier.size(18.dp))
                                } else if (isThisSelected && isCorrect == false) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                } else if (isThisTheCorrectAnswer) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EduNovaSuccess, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = explanation != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (isCorrect == true) EduNovaSuccess else EduNovaSecondary).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isCorrect == true) "Great job!" else "Explanation & Hint:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect == true) EduNovaSuccess else EduNovaSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = explanation ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedResourceCard(
    resource: TopicLearningResource,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = resource.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = EduNovaPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (resource.isVerified) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified Resource",
                            tint = EduNovaSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Doc",
                            style = MaterialTheme.typography.labelSmall,
                            color = EduNovaSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (resource.description.isNotBlank()) {
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onOpenUrl(resource.url) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Documentation (${resource.url})",
                    color = EduNovaPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
