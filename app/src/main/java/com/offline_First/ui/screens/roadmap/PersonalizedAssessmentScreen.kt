package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.AssessmentPhase
import com.offline_First.domain.model.AssessmentQuestion
import com.offline_First.domain.model.AssessmentQuestionType
import com.offline_First.domain.model.TopicMasteryLevel
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedAssessmentScreen(
    onBack: () -> Unit,
    onRoadmapReady: () -> Unit,
    viewModel: PersonalizedRoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.assessmentState

    LaunchedEffect(uiState.roadmapJustGenerated) {
        if (uiState.roadmapJustGenerated) {
            viewModel.consumeRoadmapGenerated()
            onRoadmapReady()
        }
    }

    // Auto-trigger roadmap generation when assessment completes
    var hasAutoTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(session?.phase, session?.isComplete) {
        if (session != null &&
            session.phase == AssessmentPhase.SUMMARY &&
            session.isComplete &&
            !hasAutoTriggered &&
            !uiState.isGenerating &&
            session.correctionPrompt == null  // Don't auto-trigger if there's a correction prompt
        ) {
            hasAutoTriggered = true
            viewModel.generateRoadmap()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTextAnswer by remember { mutableStateOf("") }
    var hasAnsweredCurrent by remember { mutableStateOf(false) }

    LaunchedEffect(session?.currentQuestion?.id) {
        selectedOptionIndex = null
        selectedTextAnswer = ""
        hasAnsweredCurrent = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (session != null) "${session.domain} Diagnostic Assessment" else "Personalized Learning Path",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (session?.phase) {
                                    AssessmentPhase.PROFILE_COLLECTION -> "Phase 1: Learning Profile"
                                    AssessmentPhase.DIAGNOSTIC -> "Phase 2: Adaptive Knowledge Check"
                                    AssessmentPhase.SUMMARY -> "Phase 3: Diagnostic Insights"
                                    else -> "Step 1: Choose Course or Skill"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                        }

                        if (session != null && session.totalQuestions > 0) {
                            Text(
                                text = "${session.questionNumber}/${session.totalQuestions}",
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    if (session != null && session.totalQuestions > 0) {
                        LinearProgressIndicator(
                            progress = { session.questionNumber.toFloat() / session.totalQuestions.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = primaryColor,
                            trackColor = primaryColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (session == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "What do you want to learn?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter any programming language, framework, database, or technical skill. EduNova will design a customized diagnostic assessment and adaptive roadmap.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.targetCourseInput,
                                onValueChange = { viewModel.updateCourseInput(it) },
                                label = { Text("Course, Language or Skill") },
                                placeholder = { Text("e.g. React, Java, SQL, DSA, Python, Machine Learning") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                isError = uiState.courseValidationError != null
                            )

                            if (uiState.courseValidationError != null) {
                                Text(
                                    text = uiState.courseValidationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "Popular Suggestions:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("React", "Python", "Java", "SQL", "DSA", "Machine Learning", "Go", "Docker").forEach { chip ->
                                    FilterChip(
                                        selected = uiState.targetCourseInput.equals(chip, ignoreCase = true),
                                        onClick = { viewModel.updateCourseInput(chip) },
                                        label = { Text(chip) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            val displayError = uiState.courseValidationError ?: uiState.errorMessage
                            if (displayError != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = displayError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = {
                                    val target = uiState.targetCourseInput.ifBlank { "React" }
                                    viewModel.validateAndStartAssessment(target)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                enabled = !uiState.isValidatingCourse && !uiState.isLoading
                            ) {
                                if (uiState.isValidatingCourse || uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Validating & Preparing Assessment...")
                                } else {
                                    Text("Start Diagnostic Assessment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SUMMARY PHASE
                if (session.phase == AssessmentPhase.SUMMARY || session.isComplete) {
                    item(key = "summary_card") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(EduNovaSuccess.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EduNovaSuccess,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Assessment Completed!",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Diagnostic evaluation gathered across ${session.masteryMap.size} core topics.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider()

                                Text(
                                    text = "Topic Mastery Evidence",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                session.masteryMap.values.forEach { mastery ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mastery.topicTag.replace("python_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        val (badgeColor, label) = when (mastery.masteryLevel) {
                                            TopicMasteryLevel.STRONG -> EduNovaSuccess to "Strong (Accelerated)"
                                            TopicMasteryLevel.ADEQUATE -> EduNovaPrimary to "Proficient"
                                            TopicMasteryLevel.WEAK -> EduNovaAccent to "Needs Practice"
                                            TopicMasteryLevel.UNKNOWN -> Color.Gray to "Insufficient Data"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Profile Inconsistency Prompt
                                session.correctionPrompt?.let { correction ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = EduNovaAccent.copy(alpha = 0.1f)),
                                        border = BorderStroke(1.dp, EduNovaAccent.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = EduNovaAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Curriculum Recommendation",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = EduNovaAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Text(
                                                text = correction.reason,
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.correctProfileField(correction.field, correction.suggestedValue)
                                                    }
                                                ) {
                                                    Text("Accept ${correction.suggestedValue}")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (uiState.errorMessage != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.errorMessage!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { viewModel.generateRoadmap() },
                                    enabled = !uiState.isGenerating,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isGenerating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Building Personalized Roadmap...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate My Personalized Roadmap")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // QUESTION PRESENTATION (Profile or Diagnostic)
                    val question = session.currentQuestion ?: AssessmentQuestion(
                        id = session.currentStepId ?: "step_${session.completedSteps.size + 1}",
                        topicTag = "general",
                        questionType = AssessmentQuestionType.MULTIPLE_CHOICE,
                        prompt = session.latestMessage ?: "Please select an option to continue:",
                        options = session.options
                    )
                    item(key = question.id) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Question Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = primaryColor.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = when (question.questionType) {
                                                    AssessmentQuestionType.CODE_OUTPUT -> "Code Output Prediction"
                                                    AssessmentQuestionType.DEBUGGING -> "Debugging"
                                                    else -> "Multiple Choice"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = primaryColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        if (session.phase == AssessmentPhase.DIAGNOSTIC) {
                                            Text(
                                                text = "Difficulty Level: ${question.difficultyLevel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Prompt
                                    Text(
                                        text = question.prompt,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 24.sp
                                    )

                                    // Code Snippet Box (if present)
                                    question.codeSnippet?.let { snippet ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF1E1E1E),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = snippet,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = Color(0xFFE0E0E0),
                                                lineHeight = 18.sp,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }

                                    // Options List
                                    question.options.forEachIndexed { index, optionText ->
                                        val isSelected = selectedOptionIndex == index
                                        val isCorrect = question.correctOptionIndex == index

                                        val (btnBorder, btnBg) = when {
                                            hasAnsweredCurrent && isCorrect ->
                                                BorderStroke(1.5.dp, EduNovaSuccess) to EduNovaSuccess.copy(alpha = 0.12f)
                                            hasAnsweredCurrent && isSelected && !isCorrect ->
                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) to MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                            isSelected ->
                                                BorderStroke(1.5.dp, primaryColor) to primaryColor.copy(alpha = 0.08f)
                                            else ->
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) to surfaceColor
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !hasAnsweredCurrent) {
                                                    selectedOptionIndex = index
                                                    selectedTextAnswer = optionText
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            border = btnBorder,
                                            color = btnBg
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            if (isSelected) primaryColor else Color.Transparent,
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ('A' + index).toString(),
                                                        color = if (isSelected) Color.White else onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = optionText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Feedback explanation card (shown after answering in diagnostic mode)
                                    if (hasAnsweredCurrent && question.explanation.isNotEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = question.explanation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action Button
                                    if (!hasAnsweredCurrent) {
                                        Button(
                                            onClick = {
                                                if (session.phase == AssessmentPhase.DIAGNOSTIC && question.correctOptionIndex != null) {
                                                    hasAnsweredCurrent = true
                                                } else {
                                                    viewModel.submitAnswer(
                                                        answer = selectedTextAnswer,
                                                        quizSelectedIndex = selectedOptionIndex
                                                    )
                                                }
                                            },
                                            enabled = selectedOptionIndex != null && !uiState.isSubmittingAnswer,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (session.phase == AssessmentPhase.DIAGNOSTIC) "Submit Answer" else "Next")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.submitAnswer(
                                                    answer = selectedTextAnswer,
                                                    quizSelectedIndex = selectedOptionIndex
                                                )
                                            },
                                            enabled = !uiState.isSubmittingAnswer,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Continue")
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
