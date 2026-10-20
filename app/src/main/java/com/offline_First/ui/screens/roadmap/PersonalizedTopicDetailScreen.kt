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
import com.offline_First.domain.model.IndependentPracticeTask
import com.offline_First.domain.model.ProgressiveHints
import com.offline_First.domain.model.TopicAssessmentResult
import com.offline_First.domain.model.AssessmentFeedbackItem
import com.offline_First.domain.model.PersonalizedTopicStatus
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
    val currentTopicStatus = uiState.assessmentResult?.status
        ?: content?.currentStatus
        ?: roadmap?.topics?.find { it.id == content?.topicId }?.status?.name
        ?: if (isCompleted) "COMPLETED" else "NOT_STARTED"

    val isMastered = currentTopicStatus == "MASTERED"
    val isVerified = isMastered || currentTopicStatus == "VERIFIED_COMPLETE" || currentTopicStatus == "COMPLETED" || isCompleted
    val isReviewed = currentTopicStatus == "REVIEWED" || currentTopicStatus == "ASSESSMENT_PENDING"
    val isNeedsRevision = currentTopicStatus == "NEEDS_REVISION"

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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isMastered -> Color(0xFFFFB300).copy(alpha = 0.18f)
                            isVerified -> EduNovaSuccess.copy(alpha = 0.15f)
                            isNeedsRevision -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            isReviewed -> EduNovaSecondary.copy(alpha = 0.15f)
                            else -> EduNovaPrimary.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isMastered -> Icons.Default.Star
                                    isVerified -> Icons.Default.CheckCircle
                                    isNeedsRevision -> Icons.Default.WarningAmber
                                    isReviewed -> Icons.Default.AssignmentLate
                                    else -> Icons.Default.Pending
                                },
                                contentDescription = null,
                                tint = when {
                                    isMastered -> Color(0xFFFF8F00)
                                    isVerified -> EduNovaSuccess
                                    isNeedsRevision -> MaterialTheme.colorScheme.error
                                    isReviewed -> EduNovaSecondary
                                    else -> EduNovaPrimary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isMastered -> "Mastered 🌟"
                                    isVerified -> "Verified Complete ✓"
                                    isNeedsRevision -> "Needs Revision"
                                    isReviewed -> "Reviewed (Assessment Pending)"
                                    else -> "In Progress"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isMastered -> Color(0xFFFF8F00)
                                    isVerified -> EduNovaSuccess
                                    isNeedsRevision -> MaterialTheme.colorScheme.error
                                    isReviewed -> EduNovaSecondary
                                    else -> EduNovaPrimary
                                },
                                fontWeight = FontWeight.Bold
                            )
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
                        Text("Roadmap")
                    }

                    if (isVerified) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMastered) Color(0xFFFF8F00) else EduNovaSuccess
                            )
                        ) {
                            Icon(
                                if (isMastered) Icons.Default.Star else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isMastered) "Mastered 🌟" else "Verified Complete ✓")
                        }
                    } else if (isReviewed) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Complete the verification assessment below to verify this topic!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EduNovaSecondary
                            ),
                            border = BorderStroke(1.5.dp, EduNovaSecondary)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Assessment ↓")
                        }
                    } else {
                        Button(
                            onClick = {
                                val rId = roadmap?.id ?: return@Button
                                val tId = content?.topicId ?: return@Button
                                viewModel.reviewTopic(rId, tId) {
                                    Toast.makeText(
                                        context,
                                        "Topic marked reviewed! Complete the assessment below to verify.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = !uiState.isReviewingTopic && content != null,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary)
                        ) {
                            if (uiState.isReviewingTopic) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark as Reviewed")
                            }
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
                    // 1. Why this skill matters & prerequisites
                    if (content.whyItMatters.isNotBlank() || content.prerequisites.isNotEmpty()) {
                        item(key = "why_it_matters") {
                            WhyItMattersAndPrereqCard(
                                whyItMatters = content.whyItMatters,
                                prerequisites = content.prerequisites
                            )
                        }
                    }

                    // 2. Learning Objectives
                    if (content.learningObjectives.isNotEmpty()) {
                        item(key = "objectives") {
                            LearningObjectivesCard(objectives = content.learningObjectives)
                        }
                    }

                    // 3. Beginner Explanation
                    if (content.beginnerExplanation.isNotBlank()) {
                        item(key = "explanation") {
                            ConceptExplanationCard(
                                title = "Concept Overview",
                                explanation = content.beginnerExplanation
                            )
                        }
                    }

                    // 4. Syntax & Terminology
                    if (content.syntaxAndConcepts.isNotBlank()) {
                        item(key = "syntax") {
                            SyntaxConceptsCard(syntax = content.syntaxAndConcepts)
                        }
                    }

                    // 5. Guided Investigation
                    if (content.investigationGuidance.isNotBlank()) {
                        item(key = "guided_investigation") {
                            GuidedInvestigationCard(guidance = content.investigationGuidance)
                        }
                    }

                    // 6. Code Examples
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

                    // 7. Common Mistakes
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

                    // 8. Independent Practice Task
                    content.independentTask?.let { task ->
                        item(key = "independent_task") {
                            IndependentTaskCard(task = task)
                        }
                    }

                    // 9. Progressive Hints
                    content.hints?.let { hints ->
                        item(key = "progressive_hints") {
                            ProgressiveHintsCard(
                                hints = hints,
                                unlockedLevel = uiState.hintsUnlockedLevel,
                                solutionRevealed = uiState.solutionRevealed,
                                onUnlockNext = { viewModel.unlockNextHint() },
                                onRevealSolution = { viewModel.revealSolution() }
                            )
                        }
                    }

                    // 10. Interactive Practice Quiz (quick concept check)
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

                    // 11. Topic Assessment Feedback (if available)
                    if (uiState.assessmentResult != null) {
                        item(key = "assessment_result") {
                            AssessmentResultFeedbackCard(
                                result = uiState.assessmentResult!!,
                                onRetry = { viewModel.resetAssessmentState() }
                            )
                        }
                    }

                    // 12. Topic Verification Assessment (multi-question gate)
                    if (content.assessmentQuestions.isNotEmpty() && (uiState.assessmentResult == null || !uiState.assessmentResult!!.passed)) {
                        item(key = "assessment_card") {
                            TopicVerificationAssessmentCard(
                                questions = content.assessmentQuestions,
                                selectedAnswers = uiState.assessmentAnswers,
                                isSubmitting = uiState.isSubmittingTopicAssessment,
                                onSelectAnswer = { qIdx, optIdx ->
                                    viewModel.selectAssessmentAnswer(qIdx, optIdx)
                                },
                                onSubmit = {
                                    val rId = roadmap?.id ?: return@TopicVerificationAssessmentCard
                                    val tId = content.topicId
                                    viewModel.submitTopicAssessment(rId, tId) { res ->
                                        if (res.passed) {
                                            Toast.makeText(
                                                context,
                                                if (res.status == "MASTERED") "Assessment Perfect! Topic Mastered 🌟" else "Assessment Passed! Topic Verified Complete ✓",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Score: ${res.scorePercentage}%. Review the feedback and try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // 13. Verified Learning Resources
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

@Composable
private fun WhyItMattersAndPrereqCard(
    whyItMatters: String,
    prerequisites: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Why This Skill Matters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaPrimary
                )
            }

            if (whyItMatters.isNotBlank()) {
                Text(
                    text = whyItMatters,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (prerequisites.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prerequisites:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prerequisites.forEach { prereq ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = prereq,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedInvestigationCard(guidance: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaSecondary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = EduNovaSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guided Investigation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaSecondary
                )
            }
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun IndependentTaskCard(task: IndependentPracticeTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EduNovaAccent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Construction,
                    contentDescription = null,
                    tint = EduNovaAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Independent Practice Lab",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaAccent
                    )
                    Text(
                        text = "Attempt independently before checking solutions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = task.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (task.instructions.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Lab Instructions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EduNovaSuccess.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, EduNovaSuccess.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = EduNovaSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deliverable: ${task.expectedDeliverable}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EduNovaSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressiveHintsCard(
    hints: ProgressiveHints,
    unlockedLevel: Int,
    solutionRevealed: Boolean,
    onUnlockNext: () -> Unit,
    onRevealSolution: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = EduNovaSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Progressive Hints",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaSecondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (solutionRevealed) "Assisted" else if (unlockedLevel > 0) "Hint $unlockedLevel of 3" else "3 Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = EduNovaSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Use hints sequentially if you are stuck. Revealing the complete solution reduces unassisted mastery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Hint 1
            if (unlockedLevel >= 1) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EduNovaSecondary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EduNovaSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Hint 1: Concept Direction",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = hints.hint1Concept, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Hint 2
            if (unlockedLevel >= 2) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EduNovaSecondary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EduNovaSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🔍 Hint 2: Strategy & Steps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = hints.hint2Strategy, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Hint 3
            if (unlockedLevel >= 3) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EduNovaSecondary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EduNovaSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🧱 Hint 3: Code Scaffold & Structure",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = hints.hint3Scaffold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Solution Outline
            if (solutionRevealed && hints.solution != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ Solution Outline (Assisted Review)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hints.solution,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unlockedLevel < 3) {
                    OutlinedButton(
                        onClick = onUnlockNext,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock Hint ${unlockedLevel + 1} of 3")
                    }
                } else if (!solutionRevealed && hints.solution != null) {
                    OutlinedButton(
                        onClick = onRevealSolution,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reveal Solution Outline")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicVerificationAssessmentCard(
    questions: List<TopicPracticeQuiz>,
    selectedAnswers: Map<Int, Int>,
    isSubmitting: Boolean,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EduNovaPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FactCheck,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Topic Verification Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaPrimary
                    )
                    Text(
                        text = "Score >= 70% required to verify completion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            questions.forEachIndexed { qIdx, q ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${qIdx + 1} of ${questions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaPrimary
                            )
                            if (!q.skillTested.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = q.skillTested,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            q.options.forEachIndexed { optIdx, optText ->
                                val isSelected = selectedAnswers[qIdx] == optIdx
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectAnswer(qIdx, optIdx) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EduNovaPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) EduNovaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${('A' + optIdx)}. ",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) EduNovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EduNovaPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val allAnswered = questions.indices.all { selectedAnswers.containsKey(it) }

            Button(
                onClick = onSubmit,
                enabled = allAnswered && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evaluating Evidence...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (allAnswered) "Submit Verification Assessment" else "Answer All Questions (${selectedAnswers.size}/${questions.size})")
                }
            }
        }
    }
}

@Composable
private fun AssessmentResultFeedbackCard(
    result: TopicAssessmentResult,
    onRetry: () -> Unit
) {
    val passed = result.passed
    val cardBorderColor = if (passed) EduNovaSuccess else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (passed) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = cardBorderColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.status == "MASTERED") "Mastered 🌟" else if (passed) "Verified Complete ✓" else "Needs Revision",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardBorderColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBorderColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${result.scorePercentage}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardBorderColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!passed && !result.revisionGuidance.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Targeted Revision Guidance:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.revisionGuidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Per question feedback
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.feedback.forEachIndexed { fIdx, fb ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (fb.isCorrect) EduNovaSuccess.copy(alpha = 0.06f) else MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                        border = BorderStroke(
                            1.dp,
                            if (fb.isCorrect) EduNovaSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (fb.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (fb.isCorrect) EduNovaSuccess else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Question ${fIdx + 1}: ${if (fb.isCorrect) "Correct" else "Incorrect"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fb.isCorrect) EduNovaSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = fb.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!passed) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-attempt Assessment")
                }
            }
        }
    }
}
