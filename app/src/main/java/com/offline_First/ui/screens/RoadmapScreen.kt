package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.offline_First.domain.model.GeneratedRoadmapPreview
import com.offline_First.domain.model.RoadmapAccentTheme
import com.offline_First.domain.model.RoadmapOption
import com.offline_First.domain.model.UiState
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
    var showPersonalizationSheet by rememberSaveable { mutableStateOf(false) }

    var selectedGoal by rememberSaveable { mutableStateOf("Get a job") }
    var selectedLevel by rememberSaveable { mutableStateOf("Beginner") }
    var selectedStudyTime by rememberSaveable { mutableStateOf("1 hour/day") }
    var selectedInterest by rememberSaveable { mutableStateOf("AI / ML") }

    val allRoadmaps = when (val state = uiState.roadmaps) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    val filteredRoadmaps = remember(uiState.selectedCategory, allRoadmaps) {
        if (uiState.selectedCategory == "All") {
            allRoadmaps
        } else {
            allRoadmaps.filter { it.category == uiState.selectedCategory }
        }
    }

    val featureRoadmaps = remember(filteredRoadmaps) {
        filteredRoadmaps.take(2)
    }

    val roadmapRows = remember(filteredRoadmaps) {
        filteredRoadmaps.chunked(2)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
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
                    text = "Roadmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "header-text") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Developer Roadmaps",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Follow a structured path, learn the right skills, and build your way toward your goal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            item(key = "chat-then-build-card") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = roadmapPrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, roadmapPrimary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(roadmapPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✦",
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chat then Build",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tell EduNova about your goals, current level, and available study time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EduNovaBadge("Your goal")
                            EduNovaBadge("Current level")
                            EduNovaBadge("Study time")
                            EduNovaBadge("Interests")
                        }

                        Button(
                            onClick = onBuildRoadmap,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chat & Build")
                        }
                    }
                }
            }

            uiState.generatedRoadmap?.let { generated ->
                item(key = "generated-roadmap-result") {
                    GeneratedRoadmapCard(
                        generated = generated,
                        roadmapPrimary = roadmapPrimary,
                        onReset = { viewModel.clearGeneratedRoadmap() }
                    )
                }
            }

            if (uiState.isGenerating) {
                item(key = "generating-progress-card") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(roadmapPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AI",
                                    color = roadmapPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Building your roadmap...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "EduNova is creating a learning path based on your goals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item(key = "category-selector") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Explore by interest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.categories.forEach { category ->
                            EduNovaFilterChip(
                                text = category,
                                selected = category == uiState.selectedCategory,
                                onClick = { viewModel.selectCategory(category) },
                                selectedContainerColor = roadmapPrimary
                            )
                        }
                    }
                }
            }

            if (featureRoadmaps.isNotEmpty()) {
                item(key = "popular-this-week") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Popular this week",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            featureRoadmaps.forEach { roadmap ->
                                FeaturedRoadmapCard(
                                    roadmap = roadmap,
                                    roadmapPrimary = roadmapPrimary,
                                    onClick = { selectedRoadmap = roadmap },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "standard-roadmaps-header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Standard Roadmaps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a structured path and start learning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (val state = uiState.roadmaps) {
                is UiState.Loading -> {
                    item(key = "loading-indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = roadmapPrimary)
                        }
                    }
                }
                is UiState.Error -> {
                    item(key = "error-indicator") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Unable to load roadmaps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.loadData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (filteredRoadmaps.isEmpty()) {
                        item(key = "empty-roadmaps") {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No roadmaps found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Try another category.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = roadmapRows,
                            key = { row -> row.joinToString("-") { it.id } }
                        ) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { roadmap ->
                                    StandardRoadmapCard(
                                        roadmap = roadmap,
                                        roadmapPrimary = roadmapPrimary,
                                        onClick = { selectedRoadmap = roadmap },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showPersonalizationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonalizationSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Build your personalized roadmap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tell EduNova about your goals, current level, and time to learn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuestionBlock(
                    title = "What’s your goal?",
                    options = listOf("Get a job", "Prepare for an interview", "Learn a new skill", "Build projects", "Prepare for an exam"),
                    selected = selectedGoal,
                    roadmapPrimary = roadmapPrimary,
                    onSelect = { selectedGoal = it }
                )

                QuestionBlock(
                    title = "What’s your current level?",
                    options = listOf("Beginner", "Intermediate", "Advanced"),
                    selected = selectedLevel,
                    roadmapPrimary = roadmapPrimary,
                    onSelect = { selectedLevel = it }
                )

                QuestionBlock(
                    title = "How much time can you study?",
                    options = listOf("30 min/day", "1 hour/day", "2 hours/day", "3+ hours/day"),
                    selected = selectedStudyTime,
                    roadmapPrimary = roadmapPrimary,
                    onSelect = { selectedStudyTime = it }
                )

                QuestionBlock(
                    title = "What do you want to learn?",
                    options = listOf("Web Development", "AI / ML", "Data Science", "Android", "Cloud"),
                    selected = selectedInterest,
                    roadmapPrimary = roadmapPrimary,
                    onSelect = { selectedInterest = it }
                )

                Button(
                    onClick = {
                        viewModel.generatePersonalizedRoadmap(
                            goal = selectedGoal,
                            level = selectedLevel,
                            studyTime = selectedStudyTime,
                            interest = selectedInterest
                        )
                        showPersonalizationSheet = false
                    },
                    enabled = !uiState.isGenerating,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                ) {
                    Text("Generate roadmap")
                }
            }
        }
    }

    selectedRoadmap?.let { roadmap ->
        RoadmapDetailDialog(
            roadmap = roadmap,
            roadmapPrimary = roadmapPrimary,
            onDismiss = { selectedRoadmap = null }
        )
    }
}

@Composable
private fun GeneratedRoadmapCard(
    generated: GeneratedRoadmapPreview,
    roadmapPrimary: Color,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your roadmap is ready!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Goal: ${generated.goal}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Level: ${generated.level}   •   Study time: ${generated.studyTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Estimated journey: ${generated.duration}",
                style = MaterialTheme.typography.bodyMedium,
                color = roadmapPrimary
            )

            generated.stages.forEachIndexed { index, stage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(index + 1),
                            color = roadmapPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                ) {
                    Text("Start Roadmap")
                }
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Adjust Goals")
                }
            }
        }
    }
}

@Composable
private fun QuestionBlock(
    title: String,
    options: List<String>,
    selected: String,
    roadmapPrimary: Color,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                EduNovaFilterChip(
                    text = option,
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    selectedContainerColor = roadmapPrimary
                )
            }
        }
    }
}

@Composable
private fun FeaturedRoadmapCard(
    roadmap: RoadmapOption,
    roadmapPrimary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = roadmap.accentTheme.toColor()
    Card(
        modifier = modifier
            .heightIn(min = 180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconResolver.resolve(roadmap.icon),
                    contentDescription = "${roadmap.title} icon",
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = roadmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = roadmap.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Text(
                text = "${roadmap.level} • ${roadmap.stages} stages",
                style = MaterialTheme.typography.labelMedium,
                color = roadmapPrimary,
                fontWeight = FontWeight.SemiBold
            )
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
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                Text(
                    text = roadmap.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = roadmap.skills.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = roadmap.level,
                    style = MaterialTheme.typography.labelMedium,
                    color = roadmapPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${roadmap.stages} stages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Roadmap",
                    color = roadmapPrimary,
                    fontWeight = FontWeight.SemiBold
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
                Text("Start Roadmap")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
