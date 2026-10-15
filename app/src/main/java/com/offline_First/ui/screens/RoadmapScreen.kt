package com.offline_First.ui.screens

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
import com.offline_First.ui.components.EduNovaFilterChip
import com.offline_First.ui.screens.roadmap.RoadmapViewModel
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.utils.IconResolver

private fun RoadmapAccentTheme.toColor(): Color = when (this) {
    RoadmapAccentTheme.PRIMARY -> EduNovaPrimary
    RoadmapAccentTheme.SECONDARY -> EduNovaSecondary
    RoadmapAccentTheme.ACCENT -> EduNovaAccent
    RoadmapAccentTheme.SUCCESS -> EduNovaSuccess
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    onBack: () -> Unit = {},
    onBuildRoadmap: () -> Unit = {},
    viewModel: RoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val roadmapPrimary = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var selectedRoadmap by remember { mutableStateOf<RoadmapOption?>(null) }
    var selectedPersonalizedRoadmap by remember { mutableStateOf<PersonalizedRoadmapDetail?>(null) }
    var roadmapToRename by remember { mutableStateOf<PersonalizedRoadmapDetail?>(null) }
    var renameDraftTitle by rememberSaveable { mutableStateOf("") }
    var roadmapToDelete by remember { mutableStateOf<PersonalizedRoadmapDetail?>(null) }

    val allStandardRoadmaps = when (val state = uiState.roadmaps) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    val personalizedRoadmaps = uiState.myPersonalizedRoadmaps

    // Calculate stats
    val totalRoadmaps = personalizedRoadmaps.size
    val completedRoadmapsCount = personalizedRoadmaps.count { roadmap ->
        val progress = viewModel.calculateRoadmapProgress(roadmap, uiState.completedMilestones)
        progress >= 100
    }
    val activeRoadmapsCount = totalRoadmaps - completedRoadmapsCount
    val overallProgressAvg = if (totalRoadmaps > 0) {
        personalizedRoadmaps.map { viewModel.calculateRoadmapProgress(it, uiState.completedMilestones) }.average().toInt()
    } else 0

    // Filter personalized roadmaps based on selected tab
    val filteredPersonalized = remember(personalizedRoadmaps, uiState.dashboardTab, uiState.completedMilestones) {
        when (uiState.dashboardTab) {
            "In Progress" -> personalizedRoadmaps.filter {
                val p = viewModel.calculateRoadmapProgress(it, uiState.completedMilestones)
                p < 100
            }
            "Completed" -> personalizedRoadmaps.filter {
                val p = viewModel.calculateRoadmapProgress(it, uiState.completedMilestones)
                p >= 100
            }
            else -> personalizedRoadmaps
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Learning",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = {
                            viewModel.startNewAssessment()
                            onBuildRoadmap()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Roadmap", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Overview Card
            item(key = "learning-overview-stats") {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = roadmapPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Learning Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (totalRoadmaps > 0) "$activeRoadmapsCount active roadmap${if (activeRoadmapsCount == 1) "" else "s"} in progress" else "Start your first personalized learning roadmap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (totalRoadmaps > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem(label = "Active Plans", value = "$activeRoadmapsCount", color = roadmapPrimary)
                                StatItem(label = "Completed", value = "$completedRoadmapsCount", color = EduNovaSuccess)
                                StatItem(label = "Avg Progress", value = "$overallProgressAvg%", color = EduNovaAccent)
                            }
                        }
                    }
                }
            }

            // Tabs / Filters
            item(key = "dashboard-filter-tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All", "In Progress", "Completed", "Catalog")
                    tabs.forEach { tab ->
                        EduNovaFilterChip(
                            text = if (tab == "All") "All (${personalizedRoadmaps.size})" else tab,
                            selected = uiState.dashboardTab == tab,
                            onClick = { viewModel.setDashboardTab(tab) },
                            selectedContainerColor = roadmapPrimary
                        )
                    }
                }
            }

            // Personalized Roadmaps Section
            if (uiState.dashboardTab != "Catalog") {
                if (filteredPersonalized.isEmpty()) {
                    item(key = "empty-personalized-roadmaps") {
                        EmptyRoadmapsCard(
                            tab = uiState.dashboardTab,
                            roadmapPrimary = roadmapPrimary,
                            onCreateNew = {
                                viewModel.startNewAssessment()
                                onBuildRoadmap()
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredPersonalized,
                        key = { it.id }
                    ) { roadmap ->
                        val progress = viewModel.calculateRoadmapProgress(roadmap, uiState.completedMilestones)
                        val nextMilestone = viewModel.getNextActionableMilestone(roadmap, uiState.completedMilestones)

                        PersonalizedRoadmapCard(
                            roadmap = roadmap,
                            progress = progress,
                            nextMilestone = nextMilestone,
                            roadmapPrimary = roadmapPrimary,
                            onContinueLearning = {
                                viewModel.selectPersonalizedRoadmap(roadmap)
                                selectedPersonalizedRoadmap = roadmap
                            },
                            onRename = {
                                roadmapToRename = roadmap
                                renameDraftTitle = roadmap.title
                            },
                            onDelete = {
                                roadmapToDelete = roadmap
                            }
                        )
                    }
                }
            }

            // Catalog Roadmaps Section
            if (uiState.dashboardTab == "Catalog" || uiState.dashboardTab == "All") {
                if (allStandardRoadmaps.isNotEmpty()) {
                    item(key = "standard-roadmaps-header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Standard Developer Tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            EduNovaBadge("Catalog")
                        }
                    }

                    items(
                        items = allStandardRoadmaps,
                        key = { "std_${it.id}" }
                    ) { stdRoadmap ->
                        StandardRoadmapCard(
                            roadmap = stdRoadmap,
                            roadmapPrimary = roadmapPrimary,
                            onClick = { selectedRoadmap = stdRoadmap }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    roadmapToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { roadmapToRename = null },
            title = {
                Text("Rename Roadmap", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Give this roadmap a customized title to easily identify it in your dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = renameDraftTitle,
                        onValueChange = { renameDraftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Roadmap Title") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDraftTitle.isNotBlank()) {
                            viewModel.renamePersonalizedRoadmap(target.id, renameDraftTitle.trim())
                            roadmapToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { roadmapToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    roadmapToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { roadmapToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Roadmap?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${target.title}\"? All saved milestone progress for this roadmap will be removed. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePersonalizedRoadmap(target.id)
                        roadmapToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { roadmapToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Standard Roadmap Details Dialog
    selectedRoadmap?.let { roadmap ->
        RoadmapDetailDialog(
            roadmap = roadmap,
            roadmapPrimary = roadmapPrimary,
            onDismiss = { selectedRoadmap = null }
        )
    }

    // Detailed Personalized Roadmap Dialog
    selectedPersonalizedRoadmap?.let { pRoadmap ->
        PersonalizedRoadmapDetailDialog(
            roadmap = pRoadmap,
            completedMilestones = uiState.completedMilestones,
            onToggleMilestone = { key -> viewModel.toggleMilestone(key) },
            roadmapPrimary = roadmapPrimary,
            onDismiss = { selectedPersonalizedRoadmap = null }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyRoadmapsCard(
    tab: String,
    roadmapPrimary: Color,
    onCreateNew: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = roadmapPrimary, modifier = Modifier.size(32.dp))
            }
            Text(
                text = if (tab == "Completed") "No completed roadmaps yet" else "Create your first learning roadmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (tab == "Completed") "Keep learning and completing milestones to track your progress here." else "Tell EduNova your learning goal, assess your current skill level, and get a tailored step-by-step roadmap with practice projects.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Button(
                onClick = onCreateNew,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create your first roadmap", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PersonalizedRoadmapCard(
    roadmap: PersonalizedRoadmapDetail,
    progress: Int,
    nextMilestone: String,
    roadmapPrimary: Color,
    onContinueLearning: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompleted = progress >= 100
    val statusText = when {
        isCompleted -> "Completed"
        progress > 0 -> "In Progress"
        else -> "Not Started"
    }
    val statusColor = when {
        isCompleted -> EduNovaSuccess
        progress > 0 -> roadmapPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCompleted) EduNovaSuccess.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Goal & Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isCompleted) EduNovaSuccess.copy(alpha = 0.12f) else roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isCompleted) "✓" else "✦", color = if (isCompleted) EduNovaSuccess else roadmapPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = roadmap.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Goal: ${roadmap.goal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Badges Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EduNovaBadge(roadmap.level, containerColor = roadmapPrimary.copy(alpha = 0.08f), contentColor = roadmapPrimary)
                EduNovaBadge("${roadmap.duration} • ${roadmap.weeklyHours} hrs/wk")
                EduNovaBadge(statusText, containerColor = statusColor.copy(alpha = 0.12f), contentColor = statusColor)
            }

            // Progress Bar & Percentage
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) EduNovaSuccess else roadmapPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isCompleted) EduNovaSuccess else roadmapPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Next Actionable Milestone
            if (!isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = roadmapPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Next Milestone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(nextMilestone, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Continue Learning CTA Button
            Button(
                onClick = onContinueLearning,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) EduNovaSuccess.copy(alpha = 0.12f) else roadmapPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCompleted) "Review Completed Roadmap" else "Continue Learning",
                    color = if (isCompleted) EduNovaSuccess else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StandardRoadmapCard(
    roadmap: RoadmapOption,
    roadmapPrimary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = roadmap.accentTheme.toColor()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconResolver.resolve(roadmap.icon),
                        contentDescription = "${roadmap.title} icon",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = roadmap.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = roadmap.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = roadmap.skills.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${roadmap.level} • ${roadmap.stages} stages",
                    style = MaterialTheme.typography.labelMedium,
                    color = roadmapPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = roadmap.duration,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoadmapDetailDialog(
    roadmap: RoadmapOption,
    roadmapPrimary: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = roadmap.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = roadmap.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Difficulty: ${roadmap.level}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Duration: ${roadmap.duration}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Stages: ${roadmap.stages}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Skills covered:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roadmap.skills.forEach { skill ->
                        EduNovaBadge(
                            text = skill,
                            containerColor = roadmapPrimary.copy(alpha = 0.08f),
                            contentColor = roadmapPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PersonalizedRoadmapDetailDialog(
    roadmap: PersonalizedRoadmapDetail,
    completedMilestones: Set<String>,
    onToggleMilestone: (String) -> Unit,
    roadmapPrimary: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = roadmapPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = roadmap.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Goal: ${roadmap.goal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EduNovaBadge("Level: ${roadmap.level}")
                        EduNovaBadge("Duration: ${roadmap.duration}")
                        EduNovaBadge("${roadmap.weeklyHours} hrs/week")
                    }
                }

                if (roadmap.assessmentSummary.strengths.isNotEmpty() || roadmap.assessmentSummary.skillGaps.isNotEmpty() || roadmap.assessmentSummary.skillGapBreakdown.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Diagnostic Skill Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                if (roadmap.assessmentSummary.strengths.isNotEmpty()) {
                                    Text("Verified Strengths:", style = MaterialTheme.typography.labelSmall, color = EduNovaSuccess, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        roadmap.assessmentSummary.strengths.forEach { s ->
                                            EduNovaBadge(s, containerColor = EduNovaSuccess.copy(alpha = 0.15f), contentColor = EduNovaSuccess)
                                        }
                                    }
                                }

                                if (roadmap.assessmentSummary.skillGapBreakdown.isNotEmpty()) {
                                    Text("Target Skill Gaps:", style = MaterialTheme.typography.labelSmall, color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        roadmap.assessmentSummary.skillGapBreakdown.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("• ", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                                                Text(item.skill, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                                EduNovaBadge(
                                                    text = item.status.replace("_", " "),
                                                    containerColor = EduNovaPrimary.copy(alpha = 0.1f),
                                                    contentColor = EduNovaPrimary
                                                )
                                            }
                                            if (!item.rationale.isNullOrBlank()) {
                                                Text(
                                                    text = item.rationale,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (roadmap.assessmentSummary.skillGaps.isNotEmpty()) {
                                    Text("Skill Targets:", style = MaterialTheme.typography.labelSmall, color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        roadmap.assessmentSummary.skillGaps.forEach { gap ->
                                            EduNovaBadge(gap, containerColor = EduNovaPrimary.copy(alpha = 0.12f), contentColor = EduNovaPrimary)
                                        }
                                    }
                                }

                                if (!roadmap.assessmentSummary.curriculumRationale.isNullOrBlank()) {
                                    Text(
                                        text = roadmap.assessmentSummary.curriculumRationale,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                items(roadmap.phases.size) { pIdx ->
                    val phase = roadmap.phases[pIdx]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(roadmapPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${pIdx + 1}", color = roadmapPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(phase.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${phase.durationWeeks} weeks • ${phase.objective}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Practical Tasks
                            if (phase.tasks.isNotEmpty()) {
                                Text("Structured Tasks (${phase.tasks.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = roadmapPrimary)
                                phase.tasks.forEach { task ->
                                    val taskKey = "${roadmap.id}_task_${task.id}"
                                    val isDone = completedMilestones.contains(taskKey) || completedMilestones.contains(task.id) || task.isCompleted

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleMilestone(taskKey) }
                                            .padding(vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDone) EduNovaSuccess.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Checkbox(
                                                checked = isDone,
                                                onCheckedChange = { onToggleMilestone(taskKey) },
                                                colors = CheckboxDefaults.colors(checkedColor = EduNovaSuccess)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    EduNovaBadge("${task.estimatedHours}h", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (task.instructions.isNotBlank()) {
                                                    Text(
                                                        text = task.instructions,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Milestones Checklist
                            if (phase.milestones.isNotEmpty()) {
                                Text("Milestones:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                phase.milestones.forEachIndexed { mIdx, milestone ->
                                    val milestoneKey = "${roadmap.id}_${pIdx}_${mIdx}_${milestone.title}"
                                    val isDone = completedMilestones.contains(milestoneKey) || completedMilestones.contains(milestone.title) || milestone.isCompleted

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleMilestone(milestoneKey) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
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
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium
                                            )
                                            if (milestone.passingCriteria.isNotBlank()) {
                                                Text(
                                                    text = "Criteria: ${milestone.passingCriteria}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = roadmapPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (roadmap.capstoneProject.isNotBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Capstone Project:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(roadmap.capstoneProject, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
            ) {
                Text("Close")
            }
        }
    )
}
