package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.*
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedRoadmapScreen(
    onBack: () -> Unit,
    onRetakeAssessment: () -> Unit = {},
    onOpenTopicDetail: (topicId: String) -> Unit = {},
    viewModel: PersonalizedRoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val roadmap = remember(uiState.activeRoadmap) {
        uiState.activeRoadmap?.withSynthesizedTopicsIfEmpty()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var isRenameDialogOpen by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }
    var showRoadmapSwitcher by remember { mutableStateOf(false) }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = roadmap?.title ?: "Personalized Roadmap",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (roadmap != null) {
                                    IconButton(
                                        onClick = {
                                            renameDraft = roadmap.title
                                            isRenameDialogOpen = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            modifier = Modifier.size(16.dp),
                                            tint = onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${roadmap?.domain ?: "Python"} • ${roadmap?.goal ?: "Mastery"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        if (uiState.savedRoadmaps.size > 1) {
                            IconButton(onClick = { showRoadmapSwitcher = true }) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "All Saved Roadmaps",
                                    tint = primaryColor
                                )
                            }
                        }

                        IconButton(onClick = {
                            viewModel.resetAssessment()
                            onRetakeAssessment()
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retake Assessment",
                                tint = primaryColor
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (roadmap == null) {
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
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = primaryColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No active personalized roadmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Take a diagnostic assessment to generate your personalized learning path.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.resetAssessment()
                        onRetakeAssessment()
                    }) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Diagnostic Assessment")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Roadmap Overview & Progress Card
                item(key = "progress_header") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${roadmap.completedCount} of ${roadmap.totalTopics} Topics Completed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Estimated: ${roadmap.estimatedWeeks} weeks @ ${roadmap.studyHoursPerWeek}h/week",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${roadmap.progressPercentage}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roadmap.progressPercentage == 100) EduNovaSuccess else primaryColor
                                )
                            }

                            LinearProgressIndicator(
                                progress = { roadmap.progressPercentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (roadmap.progressPercentage == 100) EduNovaSuccess else primaryColor,
                                trackColor = primaryColor.copy(alpha = 0.12f)
                            )
                        }
                    }
                }

                // 2. Next Recommended Topic Banner
                roadmap.recommendedNextTopic?.let { nextTopic ->
                    item(key = "recommended_topic") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = EduNovaPrimary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = EduNovaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Recommended Next Step",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EduNovaPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = nextTopic.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (nextTopic.explanation.isNotEmpty()) {
                                    Text(
                                        text = nextTopic.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.setTopicInProgress(nextTopic.id)
                                            viewModel.loadTopicContent(roadmap.id, nextTopic.id)
                                            onOpenTopicDetail(nextTopic.id)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start Learning", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Section Title
                item(key = "sequence_title") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Learning Sequence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${roadmap.topics.size} Connected Topics",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 4. Data-Driven Connected Graph
                items(roadmap.topics, key = { it.id }) { topic ->
                    PersonalizedTopicCard(
                        topic = topic,
                        onTopicClick = {
                            viewModel.loadTopicContent(roadmap.id, topic.id)
                            onOpenTopicDetail(topic.id)
                        },
                        onToggleCompleted = { viewModel.toggleTopicCompleted(topic.id) }
                    )
                }
            }
        }
    }

    // Interactive Topic Detail Modal Bottom Sheet
    uiState.selectedTopic?.let { topic ->
        PersonalizedTopicBottomSheet(
            topic = topic,
            roadmap = roadmap,
            onDismiss = { viewModel.selectTopic(null) },
            onMarkCompleted = {
                viewModel.toggleTopicCompleted(topic.id)
                viewModel.selectTopic(null)
            },
            onMarkInProgress = {
                viewModel.setTopicInProgress(topic.id)
                viewModel.selectTopic(null)
            },
            onSkip = {
                viewModel.skipTopic(topic.id)
                viewModel.selectTopic(null)
            },
            onReset = {
                viewModel.resetTopicStatus(topic.id)
                viewModel.selectTopic(null)
            }
        )
    }

    // Rename Dialog
    if (isRenameDialogOpen && roadmap != null) {
        AlertDialog(
            onDismissRequest = { isRenameDialogOpen = false },
            title = { Text("Rename Roadmap") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Roadmap Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameDraft.isNotBlank()) {
                            viewModel.renameRoadmap(roadmap.id, renameDraft.trim())
                        }
                        isRenameDialogOpen = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenameDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRoadmapSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showRoadmapSwitcher = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Generated Roadmaps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.savedRoadmaps.size} Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .padding(bottom = 20.dp)
                ) {
                    items(uiState.savedRoadmaps, key = { it.id }) { item ->
                        val isSelected = item.id == roadmap?.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openRoadmap(item)
                                    showRoadmapSwitcher = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primaryColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.domain.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.completedCount} / ${item.totalTopics} completed • ${item.level.ifBlank { "All Levels" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
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

@Composable
private fun PersonalizedTopicCard(
    topic: PersonalizedRoadmapTopic,
    onTopicClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val isCompleted = topic.status == PersonalizedTopicStatus.COMPLETED
    val isLocked = topic.status == PersonalizedTopicStatus.LOCKED
    val isInProgress = topic.status == PersonalizedTopicStatus.IN_PROGRESS

    val borderColor = when {
        isCompleted -> EduNovaSuccess
        isInProgress -> EduNovaAccent
        isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        else -> EduNovaPrimary.copy(alpha = 0.5f)
    }

    val containerColor = when {
        isCompleted -> EduNovaSuccess.copy(alpha = 0.06f)
        isInProgress -> EduNovaAccent.copy(alpha = 0.05f)
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTopicClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon Indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = when {
                            isCompleted -> EduNovaSuccess
                            isInProgress -> EduNovaAccent
                            isLocked -> Color.Gray.copy(alpha = 0.2f)
                            else -> EduNovaPrimary.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Check
                        isInProgress -> Icons.Default.Schedule
                        isLocked -> Icons.Default.Lock
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when {
                        isCompleted -> Color.White
                        isInProgress -> Color.White
                        isLocked -> Color.Gray
                        else -> EduNovaPrimary
                    }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (topic.shortDescription.isNotEmpty()) {
                    Text(
                        text = topic.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Badges row: Mastery level, Acceleration, Duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (topic.accelerated) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduNovaSecondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⚡ Accelerated",
                                style = MaterialTheme.typography.labelSmall,
                                color = EduNovaSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    when (topic.masteryLevel) {
                        TopicMasteryLevel.WEAK -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduNovaAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Needs Practice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EduNovaAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        TopicMasteryLevel.STRONG -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduNovaSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Mastered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EduNovaSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {}
                    }

                    Text(
                        text = "${topic.estimatedMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick toggle button if unlocked
            if (!isLocked) {
                IconButton(onClick = onToggleCompleted) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (isCompleted) EduNovaSuccess else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalizedTopicBottomSheet(
    topic: PersonalizedRoadmapTopic,
    roadmap: PersonalizedRoadmapDetail?,
    onDismiss: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkInProgress: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status: ${topic.status.name} • ${topic.estimatedMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (topic.shortDescription.isNotEmpty()) {
                Text(
                    text = topic.shortDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // AI Explanation / Personalization Reason
            if (topic.explanation.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = topic.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Prerequisites
            if (topic.prerequisiteTopicIds.isNotEmpty() && roadmap != null) {
                Text(
                    text = "Prerequisites",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                topic.prerequisiteTopicIds.forEach { prereqId ->
                    val prereqTopic = roadmap.topics.find { it.id == prereqId }
                    val isDone = roadmap.completedTopicIds.contains(prereqId)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isDone) EduNovaSuccess else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prereqTopic?.title ?: prereqId,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (topic.status != PersonalizedTopicStatus.COMPLETED) {
                    Button(
                        onClick = onMarkCompleted,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaSuccess)
                    ) {
                        Text("Mark Completed")
                    }
                }

                if (topic.status != PersonalizedTopicStatus.IN_PROGRESS && topic.status != PersonalizedTopicStatus.COMPLETED) {
                    OutlinedButton(
                        onClick = onMarkInProgress,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("In Progress")
                    }
                }

                if (topic.status == PersonalizedTopicStatus.COMPLETED) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Status")
                    }
                }

                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }
    }
}
