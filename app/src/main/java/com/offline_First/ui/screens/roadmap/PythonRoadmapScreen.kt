package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.*
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

enum class PythonViewMode {
    ROADMAP_VIEW,
    STUDY_GUIDE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonRoadmapScreen(
    onBack: () -> Unit,
    viewModel: PythonRoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchActive by remember { mutableStateOf(false) }
    var viewMode by rememberSaveable { mutableStateOf(PythonViewMode.ROADMAP_VIEW) }

    LaunchedEffect(uiState.celebrationMessage) {
        uiState.celebrationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearCelebration()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    text = "Python Programming",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                EduNovaBadge(
                                    if (viewMode == PythonViewMode.ROADMAP_VIEW) "Visual Roadmap" else "Study Guide",
                                    containerColor = EduNovaPrimary.copy(alpha = 0.12f),
                                    contentColor = EduNovaPrimary
                                )
                            }
                            Text(
                                text = "${uiState.completedCount}/${uiState.totalTopicsCount} Topics Completed • ${uiState.overallPercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Switch between Connected Visual Roadmap and Study Guide
                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == PythonViewMode.ROADMAP_VIEW) {
                                    PythonViewMode.STUDY_GUIDE
                                } else {
                                    PythonViewMode.ROADMAP_VIEW
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == PythonViewMode.ROADMAP_VIEW) Icons.Default.ViewAgenda else Icons.Default.AccountTree,
                                contentDescription = "Toggle View Mode",
                                tint = primaryColor
                            )
                        }

                        if (viewMode == PythonViewMode.STUDY_GUIDE) {
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchActive) primaryColor else onSurface
                                )
                            }
                        }
                    }

                    // Search field when toggled in study guide mode
                    if (viewMode == PythonViewMode.STUDY_GUIDE) {
                        AnimatedVisibility(visible = isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search topics (e.g. loops, classes, lambda)...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        }

                        // Quick Module Navigation Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.modules.forEachIndexed { index, module ->
                                val progress = uiState.getModuleProgress(module.id)
                                val isCompleted = progress >= 100
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCompleted) EduNovaSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCompleted) EduNovaSuccess.copy(alpha = 0.4f) else outlineColor.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem((index + 1).coerceAtMost(uiState.modules.size))
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${module.stageNumber}. ${module.title.substringBefore('&').trim()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCompleted) EduNovaSuccess else onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$progress%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCompleted) EduNovaSuccess else primaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (viewMode == PythonViewMode.ROADMAP_VIEW) {
            ConnectedPythonRoadmapMobileView(
                uiState = uiState,
                paddingValues = paddingValues,
                onTopicClick = { topic -> viewModel.selectTopic(topic) }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Dashboard Progress Header
                item(key = "python-roadmap-header") {
                    RoadmapSummaryHeader(
                        uiState = uiState,
                        onExpandAll = { viewModel.expandAllModules() },
                        onCollapseAll = { viewModel.collapseAllModules() },
                        onStatusFilterChange = { viewModel.setStatusFilter(it) }
                    )
                }

                // Learning Modules with Connected Nodes
                itemsIndexed(
                    items = uiState.modules,
                    key = { _, mod -> mod.id }
                ) { index, module ->
                    val isExpanded = uiState.expandedModuleIds.contains(module.id)
                    val isLastModule = index == uiState.modules.size - 1

                    ModuleLearningNodeItem(
                        module = module,
                        isExpanded = isExpanded,
                        isLastModule = isLastModule,
                        progress = uiState.progress,
                        moduleProgress = uiState.getModuleProgress(module.id),
                        searchQuery = uiState.searchQuery,
                        statusFilter = uiState.statusFilter,
                        onToggleExpand = { viewModel.toggleModuleExpansion(module.id) },
                        onTopicClick = { topic -> viewModel.selectTopic(topic) },
                        onQuickToggleTopic = { topicId ->
                            val currentStatus = uiState.progress.getStatus(topicId)
                            if (currentStatus == TopicLearningStatus.COMPLETED) {
                                viewModel.resetTopicStatus(topicId)
                            } else {
                                viewModel.markTopicCompleted(topicId)
                            }
                        }
                    )
                }
            }
        }
    }

    // Interactive Topic Details Bottom Sheet
    uiState.selectedTopic?.let { topic ->
        val currentStatus = uiState.progress.getStatus(topic.id)
        TopicDetailBottomSheet(
            topic = topic,
            status = currentStatus,
            onDismiss = { viewModel.selectTopic(null) },
            onMarkCompleted = {
                viewModel.markTopicCompleted(topic.id)
            },
            onMarkInProgress = {
                viewModel.markTopicInProgress(topic.id)
            },
            onSkip = {
                viewModel.skipTopic(topic.id)
            },
            onReset = {
                viewModel.resetTopicStatus(topic.id)
            }
        )
    }
}

@Composable
private fun RoadmapSummaryHeader(
    uiState: PythonRoadmapUiState,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onStatusFilterChange: (TopicLearningStatus?) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
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
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(listOf(EduNovaPrimary, EduNovaSecondary)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Python Learning Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "8 Modules • 20 Topics • ~${uiState.remainingMinutes / 60}h remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${uiState.overallPercentage}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.overallPercentage >= 100) EduNovaSuccess else primaryColor
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (uiState.overallPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (uiState.overallPercentage >= 100) EduNovaSuccess else primaryColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Status Counter Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusCounterItem(
                    label = "Completed",
                    count = uiState.completedCount,
                    color = EduNovaSuccess,
                    icon = Icons.Default.CheckCircle,
                    selected = uiState.statusFilter == TopicLearningStatus.COMPLETED,
                    onClick = {
                        onStatusFilterChange(
                            if (uiState.statusFilter == TopicLearningStatus.COMPLETED) null else TopicLearningStatus.COMPLETED
                        )
                    }
                )
                StatusCounterItem(
                    label = "In Progress",
                    count = uiState.inProgressCount,
                    color = EduNovaAccent,
                    icon = Icons.Default.PlayCircle,
                    selected = uiState.statusFilter == TopicLearningStatus.IN_PROGRESS,
                    onClick = {
                        onStatusFilterChange(
                            if (uiState.statusFilter == TopicLearningStatus.IN_PROGRESS) null else TopicLearningStatus.IN_PROGRESS
                        )
                    }
                )
                StatusCounterItem(
                    label = "Pending",
                    count = uiState.totalTopicsCount - uiState.completedCount - uiState.inProgressCount - uiState.skippedCount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Default.RadioButtonUnchecked,
                    selected = uiState.statusFilter == TopicLearningStatus.NOT_STARTED,
                    onClick = {
                        onStatusFilterChange(
                            if (uiState.statusFilter == TopicLearningStatus.NOT_STARTED) null else TopicLearningStatus.NOT_STARTED
                        )
                    }
                )
                StatusCounterItem(
                    label = "Skipped",
                    count = uiState.skippedCount,
                    color = MaterialTheme.colorScheme.outline,
                    icon = Icons.Default.RemoveCircleOutline,
                    selected = uiState.statusFilter == TopicLearningStatus.SKIPPED,
                    onClick = {
                        onStatusFilterChange(
                            if (uiState.statusFilter == TopicLearningStatus.SKIPPED) null else TopicLearningStatus.SKIPPED
                        )
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any node to view concepts & code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onExpandAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Expand All", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onCollapseAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Collapse All", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCounterItem(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, color) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ModuleLearningNodeItem(
    module: PythonModule,
    isExpanded: Boolean,
    isLastModule: Boolean,
    progress: PythonRoadmapProgress,
    moduleProgress: Int,
    searchQuery: String,
    statusFilter: TopicLearningStatus?,
    onToggleExpand: () -> Unit,
    onTopicClick: (PythonTopic) -> Unit,
    onQuickToggleTopic: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isModuleDone = moduleProgress >= 100

    val filteredTopics = remember(module.topics, searchQuery, statusFilter, progress) {
        module.topics.filter { topic ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                topic.title.contains(searchQuery, ignoreCase = true) ||
                        topic.shortDescription.contains(searchQuery, ignoreCase = true) ||
                        topic.subtopics.any { it.title.contains(searchQuery, ignoreCase = true) }
            }
            val status = progress.getStatus(topic.id)
            val matchesFilter = statusFilter == null || status == statusFilter
            matchesSearch && matchesFilter
        }
    }

    // Hide module if search/filter produces 0 matching topics
    if (filteredTopics.isEmpty() && (searchQuery.isNotBlank() || statusFilter != null)) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Module Milestone Node Card with Connecting Spine Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Milestone Node Icon / Connector Dot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isModuleDone) EduNovaSuccess.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isModuleDone) "✓" else "${module.stageNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isModuleDone) EduNovaSuccess else primaryColor,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Main Module Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleExpand),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isModuleDone) {
                        EduNovaSuccess.copy(alpha = 0.05f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isModuleDone) EduNovaSuccess.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "STAGE ${module.stageNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isModuleDone) EduNovaSuccess else primaryColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            EduNovaBadge(
                                "${module.topics.size} Topics",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = module.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Progress Pill and Expand/Collapse Icon
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isModuleDone) EduNovaSuccess.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "$moduleProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isModuleDone) EduNovaSuccess else primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Branching Topic Nodes (Rendered when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                filteredTopics.forEachIndexed { tIdx, topic ->
                    val isLastTopic = tIdx == filteredTopics.size - 1
                    ConnectedTopicNodeRow(
                        topic = topic,
                        status = progress.getStatus(topic.id),
                        isLastTopic = isLastTopic && isLastModule,
                        onClick = { onTopicClick(topic) },
                        onQuickToggle = { onQuickToggleTopic(topic.id) }
                    )
                }
            }
        }

        // Connecting vertical trunk spine between modules
        if (!isLastModule) {
            Box(
                modifier = Modifier
                    .padding(start = 37.dp)
                    .width(2.dp)
                    .height(20.dp)
                    .background(
                        if (isModuleDone) EduNovaSuccess.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

@Composable
private fun ConnectedTopicNodeRow(
    topic: PythonTopic,
    status: TopicLearningStatus,
    isLastTopic: Boolean,
    onClick: () -> Unit,
    onQuickToggle: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val statusColor = when (status) {
        TopicLearningStatus.COMPLETED -> EduNovaSuccess
        TopicLearningStatus.IN_PROGRESS -> EduNovaAccent
        TopicLearningStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        TopicLearningStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val statusText = when (status) {
        TopicLearningStatus.COMPLETED -> "Completed"
        TopicLearningStatus.IN_PROGRESS -> "In Progress"
        TopicLearningStatus.SKIPPED -> "Skipped"
        TopicLearningStatus.NOT_STARTED -> "Not Started"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Visual Bezier Branch Line Connecting central spine to topic node
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spineX = size.width / 2f
                val branchEndY = size.height / 2f
                val branchEndX = size.width

                // Vertical trunk spine
                drawLine(
                    color = statusColor.copy(alpha = 0.4f),
                    start = Offset(spineX, 0f),
                    end = Offset(spineX, size.height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Smooth curved branching connector to card
                val branchPath = Path().apply {
                    moveTo(spineX, branchEndY)
                    cubicTo(
                        spineX + 10.dp.toPx(), branchEndY,
                        branchEndX - 10.dp.toPx(), branchEndY,
                        branchEndX, branchEndY
                    )
                }
                drawPath(
                    path = branchPath,
                    color = statusColor.copy(alpha = 0.6f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Connector dot on spine
                drawCircle(
                    color = statusColor,
                    radius = 4.dp.toPx(),
                    center = Offset(spineX, branchEndY)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Topic Node Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (status) {
                    TopicLearningStatus.COMPLETED -> EduNovaSuccess.copy(alpha = 0.04f)
                    TopicLearningStatus.IN_PROGRESS -> primaryColor.copy(alpha = 0.06f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = BorderStroke(
                1.dp,
                when (status) {
                    TopicLearningStatus.COMPLETED -> EduNovaSuccess.copy(alpha = 0.35f)
                    TopicLearningStatus.IN_PROGRESS -> primaryColor.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon toggle button
                IconButton(
                    onClick = onQuickToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = when (status) {
                            TopicLearningStatus.COMPLETED -> Icons.Default.CheckCircle
                            TopicLearningStatus.IN_PROGRESS -> Icons.Default.PlayCircle
                            TopicLearningStatus.SKIPPED -> Icons.Default.RemoveCircleOutline
                            TopicLearningStatus.NOT_STARTED -> Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = "Status: $statusText",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (status == TopicLearningStatus.COMPLETED) FontWeight.SemiBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = topic.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Time estimate pill
                EduNovaBadge(
                    "${topic.estimatedMinutes}m",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailBottomSheet(
    topic: PythonTopic,
    status: TopicLearningStatus,
    onDismiss: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkInProgress: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var activeTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Concepts", "Code Example", "Practice Quiz")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topic.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Start Learning / Mark Completed / Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status != TopicLearningStatus.COMPLETED) {
                    Button(
                        onClick = onMarkCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Status", fontSize = 13.sp)
                    }
                }

                if (status != TopicLearningStatus.IN_PROGRESS && status != TopicLearningStatus.COMPLETED) {
                    FilledTonalButton(
                        onClick = onMarkInProgress,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = primaryColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = primaryColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Topic", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (status != TopicLearningStatus.SKIPPED && status != TopicLearningStatus.COMPLETED) {
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = primaryColor
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (activeTab) {
                    0 -> ConceptOverviewSection(topic = topic)
                    1 -> CodeExamplesSection(topic = topic)
                    2 -> PracticeQuizSection(topic = topic)
                }
            }
        }
    }
}

@Composable
private fun ConceptOverviewSection(topic: PythonTopic) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Overview Card
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = topic.overview,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }

    // Learning Objectives
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Learning Objectives",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            topic.learningObjectives.forEach { objective ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = primaryColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = objective,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Subtopics
    Text(
        text = "Key Subtopics & Explanations",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    topic.subtopics.forEach { sub ->
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = sub.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sub.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sub.keyPoints.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        sub.keyPoints.forEach { point ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("–", color = EduNovaPrimary, modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeExamplesSection(topic: PythonTopic) {
    if (topic.codeExamples.isEmpty()) {
        Text("No code examples available for this topic yet.")
        return
    }

    topic.codeExamples.forEach { example ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = EduNovaPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = example.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Code Box (Dark Terminal Theme)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1E2E), // Modern code editor dark theme
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("python", color = Color(0xFF89B4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("UTF-8", color = Color(0xFF6C7086), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = example.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFCDD6F4),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Expected Output Box
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Expected Output:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaSuccess
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF11111B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = example.expectedOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFA6E3A1), // Soft terminal green
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Explanation Note
                Text(
                    text = "💡 ${example.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PracticeQuizSection(topic: PythonTopic) {
    if (topic.practiceQuestions.isEmpty()) {
        Text("No practice quiz available for this topic.")
        return
    }

    Text(
        text = "Test Your Understanding",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    topic.practiceQuestions.forEachIndexed { qIdx, question ->
        var selectedOption by remember { mutableStateOf<Int?>(null) }
        val isAnswered = selectedOption != null
        val isCorrect = selectedOption == question.correctIndex

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Q${qIdx + 1}: ${question.question}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Options list
                question.options.forEachIndexed { optIdx, optionText ->
                    val isThisOptionSelected = selectedOption == optIdx
                    val isThisOptionCorrect = optIdx == question.correctIndex

                    val optContainerColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        isThisOptionCorrect -> EduNovaSuccess.copy(alpha = 0.15f)
                        isThisOptionSelected && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    }

                    val optBorderColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        isThisOptionCorrect -> EduNovaSuccess
                        isThisOptionSelected && !isCorrect -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = optContainerColor,
                        border = BorderStroke(1.dp, optBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswered) {
                                selectedOption = optIdx
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A'.code + optIdx).toChar()}.",
                                fontWeight = FontWeight.Bold,
                                color = if (isAnswered && isThisOptionCorrect) EduNovaSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAnswered) {
                                if (isThisOptionCorrect) {
                                    Icon(Icons.Default.Check, contentDescription = "Correct", tint = EduNovaSuccess, modifier = Modifier.size(16.dp))
                                } else if (isThisOptionSelected) {
                                    Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Explanation after answer
                if (isAnswered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCorrect) EduNovaSuccess.copy(alpha = 0.08f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(if (isCorrect) "✓" else "✕", color = if (isCorrect) EduNovaSuccess else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private val PyCanvasTotalWidth = 372.dp
private val PyLeftTrackWidth = 104.dp
private val PyRightTrackWidth = 136.dp
private val PyConnectorWidth = 16.dp
private val PyCoreNodeWidth = 100.dp
private val PyTopicItemHeight = 28.dp

private val PyNodeBorderColor = Color(0xFF1E1E1E)
private val PyCoreNodeYellow = Color(0xFFFFE600)
private val PyTopicNodeCream = Color(0xFFFFF6E5)

/**
 * Mobile-first connected roadmap diagram view for Python matching Screenshot 1 target design.
 */
@Composable
private fun ConnectedPythonRoadmapMobileView(
    uiState: PythonRoadmapUiState,
    paddingValues: PaddingValues,
    onTopicClick: (PythonTopic) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val screenWidth = maxWidth
        val density = LocalDensity.current

        // Initial scale chosen so the usable 372dp roadmap fits the mobile screen width edge-to-edge
        val fitScale = (screenWidth.value / PyCanvasTotalWidth.value).coerceIn(0.85f, 1.25f)
        var scale by remember(screenWidth) { mutableFloatStateOf(fitScale) }

        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        var contentHeightPx by remember { mutableIntStateOf(0) }
        val contentHeightDp = with(density) { contentHeightPx.toDp() }

        val isFitOrNarrow = (PyCanvasTotalWidth * scale) <= (screenWidth + 2.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1.0f) {
                                    scale = (scale * zoomChange).coerceIn(fitScale * 0.75f, 2.2f)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState, enabled = !isFitOrNarrow),
                contentAlignment = if (isFitOrNarrow) Alignment.TopCenter else Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .width(PyCanvasTotalWidth * scale)
                        .then(
                            if (contentHeightDp > 0.dp) Modifier.height(contentHeightDp * scale)
                            else Modifier.wrapContentHeight()
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .width(PyCanvasTotalWidth)
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) {
                                    contentHeightPx = coordinates.size.height
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .padding(horizontal = 2.dp, vertical = 12.dp)
                    ) {
                        PythonRoadmapDiagramContent(
                            uiState = uiState,
                            onTopicClick = onTopicClick
                        )
                    }
                }
            }

            // Floating Controls Overlay (Zoom In, Zoom Out, Fit Screen)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale + 0.15f).coerceAtMost(2.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { scale = (scale - 0.15f).coerceAtLeast(fitScale * 0.70f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            scale = fitScale
                            coroutineScope.launch {
                                horizontalScrollState.scrollTo(0)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = "Fit Screen", tint = EduNovaPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PythonRoadmapDiagramContent(
    uiState: PythonRoadmapUiState,
    onTopicClick: (PythonTopic) -> Unit
) {
    Column(
        modifier = Modifier.width(PyCanvasTotalWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Python Programming Title
        Text(
            text = "Python Programming",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        PyDottedVerticalSpine(height = 14.dp)

        // Stage 1: Fundamentals (Center) -> Topics on Right
        val mod1 = uiState.modules.getOrNull(0)
        PyRoadmapClusterRow(
            verticalAlignment = Alignment.Top,
            leftContent = {
                // Info / Legend Card on Left
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.2.dp, PyNodeBorderColor),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PyLegendMicroItem(color = Color(0xFF7C3AED), text = "Core Track")
                        PyLegendMicroItem(color = Color(0xFF10B981), text = "Recommended")
                        PyLegendMicroItem(color = Color(0xFF6B7280), text = "Self Paced")

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Python 3.11+ Roadmap",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            },
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod1?.title?.substringBefore('&')?.trim() ?: "Fundamentals",
                    stageNumber = 1,
                    isDone = uiState.getModuleProgress(mod1?.id ?: "") >= 100
                )
            },
            rightConnector = {
                PyDottedBranchFan(isRightSide = true, count = mod1?.topics?.size ?: 3, itemHeight = PyTopicItemHeight)
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(PyRightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod1?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, onClick = { onTopicClick(topic) })
                    }
                }
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 2: Control Flow (Center) -> Topics on Left
        val mod2 = uiState.modules.getOrNull(1)
        PyRoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(PyLeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod2?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, width = PyLeftTrackWidth, onClick = { onTopicClick(topic) })
                    }
                }
            },
            leftConnector = {
                PyDottedBranchFan(isRightSide = false, count = mod2?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod2?.title?.substringBefore('&')?.trim() ?: "Control Flow",
                    stageNumber = 2,
                    isDone = uiState.getModuleProgress(mod2?.id ?: "") >= 100
                )
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 3: Data Structures (Center) -> Topics on Right
        val mod3 = uiState.modules.getOrNull(2)
        PyRoadmapClusterRow(
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod3?.title?.substringBefore('&')?.trim() ?: "Data Structures",
                    stageNumber = 3,
                    isDone = uiState.getModuleProgress(mod3?.id ?: "") >= 100
                )
            },
            rightConnector = {
                PyDottedBranchFan(isRightSide = true, count = mod3?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(PyRightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod3?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, onClick = { onTopicClick(topic) })
                    }
                }
            }
        )

        PyDottedVerticalSpine(height = 14.dp)

        // Practice Callout Card (Center)
        Surface(
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
            color = Color.White,
            modifier = Modifier.width(260.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Master data structures by practicing list comprehensions, dict lookups, and set operations.",
                    fontSize = 7.5.sp,
                    lineHeight = 10.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE5E7EB),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Data Processing Exercises",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
            }
        }

        PyDottedVerticalSpine(height = 14.dp)

        // Stage 4: Functions & Scope (Center) -> Topics on Left
        val mod4 = uiState.modules.getOrNull(3)
        PyRoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(PyLeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod4?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, width = PyLeftTrackWidth, onClick = { onTopicClick(topic) })
                    }
                }
            },
            leftConnector = {
                PyDottedBranchFan(isRightSide = false, count = mod4?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod4?.title?.substringBefore('&')?.trim() ?: "Functions",
                    stageNumber = 4,
                    isDone = uiState.getModuleProgress(mod4?.id ?: "") >= 100
                )
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 5: OOP Concepts (Center) -> Topics on Right
        val mod5 = uiState.modules.getOrNull(4)
        PyRoadmapClusterRow(
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod5?.title?.substringBefore('&')?.trim() ?: "OOP in Python",
                    stageNumber = 5,
                    isDone = uiState.getModuleProgress(mod5?.id ?: "") >= 100
                )
            },
            rightConnector = {
                PyDottedBranchFan(isRightSide = true, count = mod5?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(PyRightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod5?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, onClick = { onTopicClick(topic) })
                    }
                }
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 6: Error Handling & I/O (Center) -> Topics on Left
        val mod6 = uiState.modules.getOrNull(5)
        PyRoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(PyLeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod6?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, width = PyLeftTrackWidth, onClick = { onTopicClick(topic) })
                    }
                }
            },
            leftConnector = {
                PyDottedBranchFan(isRightSide = false, count = mod6?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod6?.title?.substringBefore('&')?.trim() ?: "Error Handling",
                    stageNumber = 6,
                    isDone = uiState.getModuleProgress(mod6?.id ?: "") >= 100
                )
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 7: Advanced Python (Center) -> Topics on Right
        val mod7 = uiState.modules.getOrNull(6)
        PyRoadmapClusterRow(
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod7?.title?.substringBefore('&')?.trim() ?: "Advanced Python",
                    stageNumber = 7,
                    isDone = uiState.getModuleProgress(mod7?.id ?: "") >= 100
                )
            },
            rightConnector = {
                PyDottedBranchFan(isRightSide = true, count = mod7?.topics?.size ?: 2, itemHeight = PyTopicItemHeight)
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(PyRightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod7?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, onClick = { onTopicClick(topic) })
                    }
                }
            }
        )

        PyDottedVerticalSpine(height = 16.dp)

        // Stage 8: Practical Projects (Center) -> Topics on Left
        val mod8 = uiState.modules.getOrNull(7)
        PyRoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(PyLeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mod8?.topics?.forEach { topic ->
                        val isDone = uiState.progress.getStatus(topic.id) == TopicLearningStatus.COMPLETED
                        PyTopicBox(topic = topic, isCompleted = isDone, width = PyLeftTrackWidth, onClick = { onTopicClick(topic) })
                    }
                }
            },
            leftConnector = {
                PyDottedBranchFan(isRightSide = false, count = mod8?.topics?.size ?: 1, itemHeight = PyTopicItemHeight)
            },
            centerContent = {
                PyRoadmapMilestoneBox(
                    label = mod8?.title?.substringBefore('&')?.trim() ?: "Projects",
                    stageNumber = 8,
                    isDone = uiState.getModuleProgress(mod8?.id ?: "") >= 100
                )
            }
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun PyRoadmapClusterRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    leftContent: (@Composable () -> Unit)? = null,
    leftConnector: (@Composable () -> Unit)? = null,
    centerContent: @Composable () -> Unit,
    rightConnector: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.width(PyCanvasTotalWidth),
        verticalAlignment = verticalAlignment
    ) {
        // Left Track (104.dp)
        Box(modifier = Modifier.width(PyLeftTrackWidth), contentAlignment = Alignment.CenterEnd) {
            leftContent?.invoke()
        }
        // Left Connector (16.dp)
        Box(modifier = Modifier.width(PyConnectorWidth), contentAlignment = Alignment.Center) {
            leftConnector?.invoke()
        }
        // Center Spine (100.dp, centered at x = 170.dp)
        Box(modifier = Modifier.width(PyCoreNodeWidth), contentAlignment = Alignment.Center) {
            centerContent()
        }
        // Right Connector (16.dp)
        Box(modifier = Modifier.width(PyConnectorWidth), contentAlignment = Alignment.Center) {
            rightConnector?.invoke()
        }
        // Right Track (136.dp)
        Box(modifier = Modifier.width(PyRightTrackWidth), contentAlignment = Alignment.CenterStart) {
            rightContent?.invoke()
        }
    }
}

@Composable
private fun PyRoadmapMilestoneBox(
    label: String,
    stageNumber: Int,
    isDone: Boolean
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (isDone) Color(0xFFE6F4EA) else PyCoreNodeYellow,
        border = BorderStroke(1.4.dp, PyNodeBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.width(PyCoreNodeWidth)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isDone) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EduNovaSuccess,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = "$stageNumber. $label",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun PyTopicBox(
    topic: PythonTopic,
    isCompleted: Boolean,
    onClick: () -> Unit,
    width: Dp = PyRightTrackWidth
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        color = if (isCompleted) Color(0xFFDCFCE7) else PyTopicNodeCream,
        border = BorderStroke(1.2.dp, PyNodeBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.width(width)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = topic.title,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(13.dp)
                    .background(if (isCompleted) EduNovaSuccess else Color(0xFF7C3AED), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun PyLegendMicroItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = text, fontSize = 6.5.sp, color = Color(0xFF374151), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PyDottedVerticalSpine(height: Dp) {
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .height(height)
    ) {
        drawLine(
            color = PyNodeBorderColor,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
    }
}

@Composable
private fun PyDottedBranchFan(
    isRightSide: Boolean,
    count: Int,
    itemHeight: Dp
) {
    val totalHeight = itemHeight * count
    Canvas(
        modifier = Modifier
            .width(PyConnectorWidth)
            .height(totalHeight)
    ) {
        val startX = if (isRightSide) 0f else size.width
        val endX = if (isRightSide) size.width else 0f
        val originY = size.height / 2f

        for (i in 0 until count) {
            val targetY = (i * (size.height / count)) + (size.height / count / 2f)
            val path = Path().apply {
                moveTo(startX, originY)
                cubicTo(
                    (startX + endX) / 2f, originY,
                    (startX + endX) / 2f, targetY,
                    endX, targetY
                )
            }
            drawPath(
                path = path,
                color = PyNodeBorderColor,
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            )
        }
    }
}
