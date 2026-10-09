package com.offline_First.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.*
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.screens.roadmap.RoadmapViewModel
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoadmapBuilderScreen(
    onBack: () -> Unit = {},
    onStartLearning: () -> Unit = {},
    viewModel: RoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var draftText by rememberSaveable { mutableStateOf("") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Initialize or resume active assessment on screen launch
    LaunchedEffect(Unit) {
        viewModel.startOrResumeAssessment()
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.assessmentMessages.size, uiState.isSendingAssessmentMessage, uiState.isGenerating) {
        if (uiState.assessmentMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.assessmentMessages.size)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val session = uiState.assessmentSession
    val isReadyForGeneration = session?.state == "READY_FOR_GENERATION" || uiState.generatedPersonalizedRoadmap != null
    val isBusy = uiState.isAssessmentLoading || uiState.isSendingAssessmentMessage || uiState.isGenerating

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 6.dp, end = 16.dp, bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to roadmaps"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "AI Learning Advisor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isReadyForGeneration) EduNovaSuccess.copy(alpha = 0.15f)
                                            else primaryColor.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (session?.state) {
                                            "READY_FOR_GENERATION" -> "Ready to Build"
                                            "ASSESSING_SKILLS" -> "Assessing Skills"
                                            "COLLECTING_GOALS" -> "Goal Discovery"
                                            "COLLECTING_PROGRESS" -> "Analyzing Background"
                                            "COMPLETED" -> "Roadmap Created"
                                            else -> "Adaptive Assessment"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isReadyForGeneration) EduNovaSuccess else primaryColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                "Personalized learning path based on your real skills",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.resetAssessment() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset Assessment",
                                tint = onSurfaceVariant
                            )
                        }
                    }

                    // Progress bar
                    val progressFraction = (session?.completenessPercentage ?: 15) / 100f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, top = 6.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isReadyForGeneration) EduNovaSuccess else primaryColor,
                            trackColor = primaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Main content: either Generated Roadmap Review, Loading, Error View, OR Conversation Chat List
            if (uiState.generatedPersonalizedRoadmap != null) {
                PersonalizedRoadmapReviewView(
                    roadmap = uiState.generatedPersonalizedRoadmap!!,
                    completedMilestones = uiState.completedMilestones,
                    onToggleMilestone = { key -> viewModel.toggleMilestone(key) },
                    onStartLearning = onStartLearning,
                    onAdjustAnswers = { viewModel.clearGeneratedRoadmap() },
                    primaryColor = primaryColor
                )
            } else if (uiState.isAssessmentLoading && uiState.assessmentMessages.isEmpty()) {
                AssessmentInitialLoadingView(
                    primaryColor = primaryColor,
                    onSurfaceVariant = onSurfaceVariant
                )
            } else if (uiState.assessmentMessages.isEmpty() && (uiState.assessmentError != null || (uiState.assessmentSession == null && !uiState.isAssessmentLoading))) {
                AssessmentErrorStateView(
                    errorMessage = uiState.assessmentError ?: "Unable to connect to AI Advisor. Please check your internet connection.",
                    onRetry = { viewModel.retryAssessment() },
                    onBack = onBack,
                    primaryColor = primaryColor,
                    surfaceColor = surfaceColor,
                    outlineColor = outlineColor
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = uiState.assessmentMessages,
                        key = { index, msg -> "${msg.speaker}_${msg.id}_$index" }
                    ) { index, message ->
                        val isLatestMentorMessage = index == uiState.assessmentMessages.lastIndex && message.speaker == AssessmentSpeaker.MENTOR

                        AssessmentBubble(
                            message = message,
                            isLatest = isLatestMentorMessage,
                            selectedQuizIndex = uiState.selectedQuizIndex,
                            onSelectQuizOption = { optIdx -> viewModel.selectQuizOption(optIdx) },
                            onSubmitQuiz = { optIdx, answerText ->
                                viewModel.submitAssessmentAnswer(answerText, optIdx)
                            },
                            isSubmitting = uiState.isSendingAssessmentMessage,
                            primaryColor = primaryColor,
                            surfaceColor = surfaceColor,
                            primaryContainer = primaryContainer,
                            outlineColor = outlineColor
                        )
                    }

                    if (uiState.isSendingAssessmentMessage || uiState.isGenerating) {
                        item(key = "typing-indicator") {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.2f)),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        if (uiState.isGenerating) "Generating your personalized roadmap..."
                                        else "EduNova Advisor is analyzing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Ready for Generation Banner CTA
                if (session?.state == "READY_FOR_GENERATION" && !uiState.isGenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                        border = BorderStroke(1.5.dp, primaryColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Ready",
                                    tint = EduNovaSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Assessment Complete!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Your goals, current skills, and timeline have been analyzed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.generatePersonalizedRoadmap() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate My Personalized Roadmap", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Quick Option Chips
                val latestMentorMsg = uiState.assessmentMessages.lastOrNull { it.speaker == AssessmentSpeaker.MENTOR }
                val hasPendingQuiz = uiState.assessmentSession?.quiz != null || uiState.assessmentMessages.lastOrNull()?.quiz != null
                val activeOptions = uiState.assessmentSession?.options?.ifEmpty { latestMentorMsg?.options ?: emptyList() } ?: latestMentorMsg?.options ?: emptyList()
                val showQuickOptions = activeOptions.isNotEmpty() && !hasPendingQuiz && !isBusy && session?.state != "READY_FOR_GENERATION" && session?.state != "COMPLETED"

                if (showQuickOptions) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeOptions.forEach { optionText ->
                            SuggestionChip(
                                onClick = {
                                    if (!isBusy) {
                                        viewModel.submitAssessmentAnswer(optionText)
                                    }
                                },
                                enabled = !isBusy,
                                label = { Text(optionText, fontSize = 13.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = primaryColor.copy(alpha = 0.06f),
                                    labelColor = primaryColor
                                ),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = surfaceColor,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Reply to advisor or ask a question...") },
                            maxLines = 3,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = outlineColor.copy(alpha = 0.3f),
                                cursorColor = primaryColor
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (draftText.isNotBlank() && !isBusy) {
                                    val textToSend = draftText
                                    viewModel.submitAssessmentAnswer(textToSend) { success ->
                                        if (success) {
                                            draftText = ""
                                        }
                                    }
                                }
                            },
                            enabled = draftText.isNotBlank() && !isBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (draftText.isNotBlank() && !isBusy) primaryColor else primaryColor.copy(alpha = 0.15f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (draftText.isNotBlank() && !isBusy) Color.White else onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentBubble(
    message: AssessmentMessage,
    isLatest: Boolean,
    selectedQuizIndex: Int?,
    onSelectQuizOption: (Int) -> Unit,
    onSubmitQuiz: (Int, String) -> Unit,
    isSubmitting: Boolean,
    primaryColor: Color,
    surfaceColor: Color,
    primaryContainer: Color,
    outlineColor: Color
) {
    val isMentor = message.speaker == AssessmentSpeaker.MENTOR

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMentor) Arrangement.Start else Arrangement.End
    ) {
        if (isMentor) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(if (message.quiz != null) 0.95f else 0.85f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMentor) 4.dp else 18.dp,
                bottomEnd = if (isMentor) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMentor) surfaceColor else primaryColor
            ),
            border = BorderStroke(1.dp, if (isMentor) outlineColor.copy(alpha = 0.2f) else Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isMentor) "EduNova Mentor" else "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMentor) primaryColor else Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMentor) MaterialTheme.colorScheme.onSurface else Color.White,
                    lineHeight = 20.sp
                )

                // Interactive Diagnostic Quiz Card
                message.quiz?.let { quiz ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Quiz,
                                    contentDescription = "Quiz",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Skill Diagnostic Check",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                                quiz.skillTested?.let { skill ->
                                    Spacer(modifier = Modifier.weight(1f))
                                    EduNovaBadge(skill)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = quiz.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            quiz.options.forEachIndexed { optIdx, optText ->
                                val isSelected = selectedQuizIndex == optIdx
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(enabled = isLatest && !isSubmitting) {
                                            onSelectQuizOption(optIdx)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) primaryColor else outlineColor.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                if (isLatest && !isSubmitting) onSelectQuizOption(optIdx)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (isLatest) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (selectedQuizIndex != null) {
                                            val chosenText = quiz.options[selectedQuizIndex]
                                            onSubmitQuiz(selectedQuizIndex, chosenText)
                                        }
                                    },
                                    enabled = selectedQuizIndex != null && !isSubmitting,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("Submit Answer", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalizedRoadmapReviewView(
    roadmap: PersonalizedRoadmapDetail,
    completedMilestones: Set<String>,
    onToggleMilestone: (String) -> Unit,
    onStartLearning: () -> Unit,
    onAdjustAnswers: () -> Unit,
    primaryColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item(key = "roadmap-hero-card") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", color = Color.White, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = roadmap.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Goal: ${roadmap.goal}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EduNovaBadge("Level: ${roadmap.level}")
                        EduNovaBadge("Duration: ${roadmap.duration}")
                        EduNovaBadge("Commitment: ${roadmap.weeklyHours} hrs/week")
                        EduNovaBadge("${roadmap.phases.size} Phases")
                    }
                }
            }
        }

        // Assessment Summary: Strengths & Skill Gaps
        item(key = "assessment-strengths-gaps") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Advisor Assessment Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (roadmap.assessmentSummary.strengths.isNotEmpty()) {
                        Text(
                            "Verified Strengths & Background:",
                            style = MaterialTheme.typography.labelMedium,
                            color = EduNovaSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            roadmap.assessmentSummary.strengths.forEach { s ->
                                EduNovaBadge(s, containerColor = EduNovaSuccess.copy(alpha = 0.12f), contentColor = EduNovaSuccess)
                            }
                        }
                    }

                    if (roadmap.assessmentSummary.skillGaps.isNotEmpty()) {
                        Text(
                            "Target Skill Gaps to Bridge:",
                            style = MaterialTheme.typography.labelMedium,
                            color = EduNovaAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            roadmap.assessmentSummary.skillGaps.forEach { g ->
                                EduNovaBadge(g, containerColor = EduNovaAccent.copy(alpha = 0.12f), contentColor = EduNovaAccent)
                            }
                        }
                    }
                }
            }
        }

        // Phases & Milestones
        item(key = "phases-header") {
            Text(
                "Learning Phases & Milestones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(
            items = roadmap.phases,
            key = { pIdx, phase -> "phase_${pIdx}_${phase.title}" }
        ) { pIdx, phase ->
            var expanded by rememberSaveable { mutableStateOf(true) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${pIdx + 1}", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = phase.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${phase.durationWeeks} weeks • ${phase.objective}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle phase",
                            tint = primaryColor
                        )
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Topics covered
                        if (phase.topics.isNotEmpty()) {
                            Text("Topics Covered:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                phase.topics.forEach { topic ->
                                    EduNovaBadge(topic)
                                }
                            }
                        }

                        // Practical Activities
                        if (phase.activities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Practical Activities:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            phase.activities.forEach { act ->
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = primaryColor, fontWeight = FontWeight.Bold)
                                    Text(act, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Milestones Checklist
                        if (phase.milestones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Milestone Criteria:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            phase.milestones.forEachIndexed { mIdx, milestone ->
                                val milestoneKey = "${roadmap.id}_${pIdx}_${mIdx}_${milestone.title}"
                                val isDone = completedMilestones.contains(milestoneKey)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDone) EduNovaSuccess.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleMilestone(milestoneKey) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Checkbox(
                                            checked = isDone,
                                            onCheckedChange = { onToggleMilestone(milestoneKey) },
                                            colors = CheckboxDefaults.colors(checkedColor = EduNovaSuccess)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = milestone.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (milestone.passingCriteria.isNotBlank()) {
                                                Text(
                                                    text = "Passing Threshold: ${milestone.passingCriteria}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = primaryColor
                                                )
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

        // Capstone & Next Action
        item(key = "capstone-card") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Capstone", tint = EduNovaAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Target Capstone Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(roadmap.capstoneProject, style = MaterialTheme.typography.bodyMedium)

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Next Step", tint = primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Immediate Next Action:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(roadmap.nextAction, style = MaterialTheme.typography.bodyMedium, color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Bottom CTAs
        item(key = "review-ctas") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartLearning,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Track Progress", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
                OutlinedButton(
                    onClick = onAdjustAnswers,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adjust Goals or Retake Assessment")
                }
            }
        }
    }
}

@Composable
private fun AssessmentInitialLoadingView(
    primaryColor: Color,
    onSurfaceVariant: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = primaryColor,
                        strokeWidth = 3.5.dp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Consulting AI Learning Advisor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analyzing your profile and curriculum goals to tailor your assessment...",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AssessmentErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    outlineColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Connection Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "AI Advisor Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Connection", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Roadmaps")
                }
            }
        }
    }
}

