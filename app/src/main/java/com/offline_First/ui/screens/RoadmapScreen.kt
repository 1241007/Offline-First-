package com.offline_First.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.offline_First.data.staging.*
import com.offline_First.domain.model.*
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.components.EduNovaFilterChip
import com.offline_First.ui.screens.roadmap.*
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.utils.IconResolver
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.ui.screens.roadmap.PersonalizedRoadmapViewModel

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
    onOpenPersonalizedRoadmap: () -> Unit = {},
    onOpenSavedRoadmap: (PersonalizedRoadmapDetail) -> Unit = {},
    onStartAssessmentForCourse: (String) -> Unit = {},
    viewModel: RoadmapViewModel = viewModel(),
    personalizedViewModel: PersonalizedRoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val personalizedState by personalizedViewModel.uiState.collectAsState()
    val savedRoadmaps = personalizedState.savedRoadmaps
    val roadmapPrimary = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Intercept back presses to stay within Roadmap sub-views when not on the root Catalog
    BackHandler(enabled = uiState.currentSubView != RoadmapSubView.CATALOG) {
        viewModel.navigateBackInRoadmap()
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyPersonalizedRoadmaps()
        viewModel.loadSavedRoadmaps()
    }

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

    // Filter Role-based and Skill-based courses by search query
    val filteredRoleBased = remember(uiState.catalogSearchQuery) {
        if (uiState.catalogSearchQuery.isBlank()) {
            PythonRoadmapStagingData.ROLE_BASED_COURSES
        } else {
            PythonRoadmapStagingData.ROLE_BASED_COURSES.filter {
                it.contains(uiState.catalogSearchQuery, ignoreCase = true)
            }
        }
    }

    val filteredSkillBased = remember(uiState.catalogSearchQuery) {
        if (uiState.catalogSearchQuery.isBlank()) {
            PythonRoadmapStagingData.SKILL_BASED_COURSES
        } else {
            PythonRoadmapStagingData.SKILL_BASED_COURSES.filter {
                it.contains(uiState.catalogSearchQuery, ignoreCase = true)
            }
        }
    }

    // Render SubViews: TOPIC_DETAIL -> PYTHON_DEVELOPER -> CATALOG
    when (uiState.currentSubView) {
        RoadmapSubView.TOPIC_DETAIL -> {
            uiState.selectedTopic?.let { topic ->
                TopicDetailContentScreen(
                    topic = topic,
                    isCompleted = uiState.completedTopicIds.contains(topic.id),
                    onToggleComplete = { viewModel.toggleTopicCompletion(topic.id) },
                    onBack = { viewModel.navigateBackInRoadmap() },
                    onNextTopic = { viewModel.navigateToNextTopic() },
                    onPrevTopic = { viewModel.navigateToPrevTopic() },
                    onAskAiAboutTopic = { prompt ->
                        viewModel.setAiTutorPrompt(prompt)
                        viewModel.setPythonCourseTab("AI Tutor")
                        viewModel.navigateBackInRoadmap()
                    }
                )
            }
        }

        RoadmapSubView.PYTHON_DEVELOPER -> {
            PythonDeveloperCourseScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { viewModel.navigateBackInRoadmap() }
            )
        }

        RoadmapSubView.JAVA_DEVELOPER -> {
            JavaDeveloperCourseScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { viewModel.navigateBackInRoadmap() }
            )
        }

        RoadmapSubView.MACHINE_LEARNING -> {
            MLDeveloperCourseScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { viewModel.navigateBackInRoadmap() }
            )
        }

        RoadmapSubView.SQL_ROADMAP -> {
            SqlDeveloperCourseScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { viewModel.navigateBackInRoadmap() }
            )
        }

        RoadmapSubView.CATALOG -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
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
                                    text = "Roadmaps",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                FilledTonalButton(
                                    onClick = {
                                        if (uiState.isOffline) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Creating custom roadmaps with AI requires an internet connection.")
                                            }
                                        } else {
                                            viewModel.startNewAssessment()
                                            onBuildRoadmap()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }

                            // Top tab bar with Roadmaps, Lesson Packs, and Project Ideas
                            RoadmapTopTabs(
                                selectedTab = uiState.roadmapMainTab,
                                onTabSelected = { viewModel.setRoadmapMainTab(it) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Bar
                    item(key = "roadmap-catalog-search") {
                        RoadmapSearchBar(
                            query = uiState.catalogSearchQuery,
                            onQueryChange = { viewModel.setCatalogSearchQuery(it) }
                        )
                    }

                    when (uiState.roadmapMainTab) {
                        "My Roadmaps" -> {
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
                                    val filterTabs = listOf("All", "In Progress", "Completed")
                                    filterTabs.forEach { tab ->
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
                                    key = { "pers_${it.id}" }
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
                                            onOpenSavedRoadmap(roadmap)
                                        },
                                        onViewProfile = {
                                            viewModel.selectPersonalizedRoadmap(roadmap)
                                            selectedPersonalizedRoadmap = roadmap
                                        },
                                        onRename = {
                                            roadmapToRename = roadmap
                                            renameDraftTitle = roadmap.title
                                        },
                                        onDelete = {
                                            roadmapToDelete = roadmap
                                        },
                                        isSaved = uiState.savedRoadmapIds.contains(roadmap.id),
                                        onToggleSave = { viewModel.toggleSaveRoadmap(roadmap.id) }
                                    )
                                }
                            }
                        }

                        "Roadmaps" -> {
                            if (uiState.isOffline) {
                                // Offline Notice Header
                                item(key = "offline-roadmap-notice") {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Offline Mode",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Showing roadmaps saved for offline learning.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = EduNovaSuccess.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Offline Ready",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EduNovaSuccess,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                val offlineSavedSystem = uiState.savedRoadmaps.filter { !it.isPersonalized }
                                val hasAnyOfflineRoadmaps = personalizedRoadmaps.isNotEmpty() || offlineSavedSystem.isNotEmpty()

                                if (!hasAnyOfflineRoadmaps) {
                                    item(key = "empty-offline-roadmaps") {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Text(
                                                    text = "No roadmaps saved for offline learning",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "Connect to the internet and save a roadmap to access it offline.",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 18.sp
                                                )
                                                Button(
                                                    onClick = {
                                                        viewModel.loadSavedRoadmaps()
                                                        viewModel.loadMyPersonalizedRoadmaps()
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary),
                                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Refresh", fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 1. Show user's custom / personalized roadmaps
                                    if (personalizedRoadmaps.isNotEmpty()) {
                                        item(key = "offline-custom-roadmaps-header") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "My Custom Roadmaps",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                EduNovaBadge("${personalizedRoadmaps.size} Available")
                                            }
                                        }

                                        items(
                                            items = personalizedRoadmaps,
                                            key = { "rec_${it.id}" }
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
                                                },
                                                isSaved = true,
                                                onToggleSave = { viewModel.toggleSaveRoadmap(roadmap.id) }
                                            )
                                        }
                                    }

                                    // 2. Show saved system roadmaps (e.g. Python Developer, Java Developer, SQL, ML)
                                    if (offlineSavedSystem.isNotEmpty()) {
                                        item(key = "offline-saved-system-header") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Saved Learning Paths",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                EduNovaBadge("${offlineSavedSystem.size} Saved")
                                            }
                                        }

                                        items(
                                            items = offlineSavedSystem,
                                            key = { "saved_sys_${it.id}" }
                                        ) { sysRoadmap ->
                                            Card(
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.selectCourseByName(sysRoadmap.title, sysRoadmap.category)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .background(roadmapPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = IconResolver.resolve(sysRoadmap.icon),
                                                            contentDescription = null,
                                                            tint = roadmapPrimary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = sysRoadmap.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = EduNovaSuccess.copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = "Offline Ready",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = EduNovaSuccess,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                            Text(
                                                                text = sysRoadmap.description.ifEmpty { "${sysRoadmap.category} • ${sysRoadmap.level}" },
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.toggleSaveRoadmap(sysRoadmap.id) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bookmark,
                                                            contentDescription = "Saved",
                                                            tint = roadmapPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ONLINE: Show standard online experience
                                // "Learn with AI" card (matching Screenshot 2)
                                item(key = "learn-with-ai-card") {
                                    LearnWithAiCard(
                                        onClick = {
                                            viewModel.startNewAssessment()
                                            onBuildRoadmap()
                                        }
                                    )
                                }

                                // Prominently show user's personalized roadmaps if any exist
                                if (personalizedRoadmaps.isNotEmpty()) {
                                    item(key = "my-custom-roadmaps-header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "My Custom Roadmaps",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            EduNovaBadge("${personalizedRoadmaps.size} Active")
                                        }
                                    }

                                    items(
                                        items = personalizedRoadmaps,
                                        key = { "rec_${it.id}" }
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
                                            },
                                            isSaved = uiState.savedRoadmapIds.contains(roadmap.id),
                                            onToggleSave = { viewModel.toggleSaveRoadmap(roadmap.id) }
                                        )
                                    }
                                }

                                // 1. Skill-based Section Header
                                item(key = "skill-based-header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Skill-based",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${filteredSkillBased.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Skill-based course cards in a clean, responsive 2-column grid
                                items(
                                    items = filteredSkillBased.chunked(2),
                                    key = { pair -> "skill_row_${pair.first()}" }
                                ) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pair.forEach { courseName ->
                                            CompactCourseCard(
                                                courseName = courseName,
                                                isBookmarked = uiState.bookmarkedCourseNames.contains(courseName),
                                                onClick = { viewModel.selectCourseByName(courseName, "Skill-based") },
                                                onToggleBookmark = { viewModel.toggleBookmarkCourse(courseName) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                // 2. Role-based Section Header
                                item(key = "role-based-header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Role-based",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${filteredRoleBased.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Role-based course cards in a clean, responsive 2-column grid
                                items(
                                    items = filteredRoleBased.chunked(2),
                                    key = { pair -> "role_row_${pair.first()}" }
                                ) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pair.forEach { courseName ->
                                            CompactCourseCard(
                                                courseName = courseName,
                                                isBookmarked = uiState.bookmarkedCourseNames.contains(courseName),
                                                onClick = { viewModel.selectCourseByName(courseName, "Role-based") },
                                                onToggleBookmark = { viewModel.toggleBookmarkCourse(courseName) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        "Lesson Packs" -> {
                            if (uiState.isOffline) {
                                item(key = "offline-lesson-packs") {
                                    OfflineTabNoticeCard(
                                        title = "Lesson Packs Unavailable Offline",
                                        message = "Connect to the internet to explore and download curated lesson packs."
                                    )
                                }
                            } else {
                                item(key = "lesson-packs-header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Curated Lesson Packs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        EduNovaBadge("${PythonRoadmapStagingData.LESSON_PACKS.size} Packs")
                                    }
                                }

                                items(
                                    items = PythonRoadmapStagingData.LESSON_PACKS,
                                    key = { it.id }
                                ) { pack ->
                                    LessonPackCard(
                                        pack = pack,
                                        onClick = {
                                            viewModel.selectCourseByName(pack.title, "Lesson Pack")
                                        }
                                    )
                                }
                            }
                        }

                        "Project Ideas" -> {
                            if (uiState.isOffline) {
                                item(key = "offline-project-ideas") {
                                    OfflineTabNoticeCard(
                                        title = "Project Ideas Unavailable Offline",
                                        message = "Connect to the internet to explore practical project ideas."
                                    )
                                }
                            } else {
                                item(key = "project-ideas-header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Practical Project Ideas",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        EduNovaBadge("${PythonRoadmapStagingData.PYTHON_PROJECTS.size} Projects")
                                    }
                                }

                                items(
                                    items = PythonRoadmapStagingData.PYTHON_PROJECTS,
                                    key = { it.id }
                                ) { project ->
                                    RoadmapProjectCard(
                                        project = project,
                                        isCompleted = uiState.completedProjectIds.contains(project.id),
                                        onClick = { viewModel.selectProject(project) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Upcoming Learning Path Dialog (for Paths 2..7)
    uiState.selectedUpcomingPath?.let { path ->
        UpcomingPathDialog(
            path = path,
            onDismiss = { viewModel.dismissUpcomingPathDialog() }
        )
    }

    // Practice Project Detail Dialog
    uiState.selectedProject?.let { project ->
        ProjectDetailDialog(
            project = project,
            isCompleted = uiState.completedProjectIds.contains(project.id),
            onToggleComplete = { viewModel.toggleProjectCompletion(project.id) },
            onDismiss = { viewModel.selectProject(null) }
        )
    }

    // Course Overview Dialog (for Roadmap.sh compact course cards)
    uiState.selectedCourseOverview?.let { (courseName, category) ->
        CourseOverviewDialog(
            courseName = courseName,
            category = category,
            isBookmarked = uiState.bookmarkedCourseNames.contains(courseName),
            onToggleBookmark = { viewModel.toggleBookmarkCourse(courseName) },
            onOpenRoadmap = {
                viewModel.dismissCourseOverviewDialog()
                if (courseName.contains("Python", ignoreCase = true)) {
                    val pythonPath = PythonRoadmapStagingData.LEARNING_PATHS.firstOrNull { it.id == "python-developer" }
                    if (pythonPath != null) {
                        viewModel.selectLearningPath(pythonPath)
                    }
                } else if (courseName.contains("Java", ignoreCase = true)) {
                    viewModel.openJavaDeveloperScreen()
                }
            },
            onDismiss = { viewModel.dismissCourseOverviewDialog() }
        )
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
            isSaved = uiState.savedRoadmapIds.contains(roadmap.id),
            onToggleSave = { viewModel.toggleSaveRoadmap(roadmap.id) },
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
            isSaved = uiState.savedRoadmapIds.contains(pRoadmap.id),
            onToggleSave = { viewModel.toggleSaveRoadmap(pRoadmap.id) },
            onDismiss = { selectedPersonalizedRoadmap = null }
        )
    }
}

@Composable
private fun OfflineTabNoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PythonDeveloperCourseScreen(
    uiState: RoadmapUiState,
    viewModel: RoadmapViewModel,
    onBack: () -> Unit
) {
    val progress = viewModel.calculatePythonProgress()
    val totalTopics = PythonRoadmapStagingData.ALL_VISUAL_NODES.size
    val completedCount = uiState.completedTopicIds.count { id ->
        PythonRoadmapStagingData.ALL_VISUAL_NODES.any { it.id == id }
    }
    val roadmapPrimary = MaterialTheme.colorScheme.primary
    var selectedVisualNode by remember { mutableStateOf<VisualRoadmapNode?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "All Roadmaps",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All Roadmaps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { /* Bookmark action */ }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { /* Share action */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)
            ) {
                // Header & Title section (Screenshot 2)
                item(key = "python-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Python Developer",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = "Step by step guide to becoming a Python developer in 2026",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stats & Progress Bar
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$completedCount of $totalTopics Topics Completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$progress%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = roadmapPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = roadmapPrimary,
                                    trackColor = roadmapPrimary.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }

                // Course Tabs: Roadmap, Projects, AI Tutor
                item(key = "python-tabs") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf("Roadmap", "Projects", "AI Tutor")
                        tabs.forEach { tab ->
                            val isSelected = uiState.pythonCourseTab == tab
                            EduNovaFilterChip(
                                text = tab,
                                selected = isSelected,
                                onClick = { viewModel.setPythonCourseTab(tab) },
                                selectedContainerColor = roadmapPrimary
                            )
                        }
                    }
                }

                // Tab Content Switch
                when (uiState.pythonCourseTab) {
                    "Roadmap" -> {
                        // Mark tip banner
                        item(key = "tracking-tip-banner") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduNovaAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EduNovaAccent.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TipsAndUpdates,
                                        contentDescription = null,
                                        tint = EduNovaAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tap any topic node below to study lesson content and track progress",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // What is Python Developer accordion
                        item(key = "what-is-python-banner") {
                            WhatIsPythonBanner()
                        }

                        // Visual Roadmap Headline
                        item(key = "visual-roadmap-title") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Interactive Learning Diagram",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EduNovaBadge("${PythonRoadmapStagingData.ALL_VISUAL_NODES.size} Connected Topics")
                                }
                                Text(
                                    text = "Pinch to zoom in/out or drag to explore. Tap any node to inspect details.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Connected Branching Visual Roadmap Diagram (Roadmap.sh Interactive Sequence inside Zoomable Canvas)
                        item(key = "python-visual-diagram") {
                            EduNovaZoomableCanvasBox(
                                modifier = Modifier.fillMaxWidth()
                            ) { _ ->
                                EduNovaPythonVisualRoadmapDiagram(
                                    selectedNodeId = selectedVisualNode?.id,
                                    completedTopicIds = uiState.completedTopicIds,
                                    onSelectNode = { node ->
                                        selectedVisualNode = node
                                    }
                                )
                            }
                        }

                        // FAQ Header & Accordions (Screen 4)
                        item(key = "faqs-header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Frequently Asked Questions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EduNovaBadge("FAQ")
                                }
                                Text(
                                    text = "Everything you need to know about the Python developer learning journey",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        PythonRoadmapStagingData.PYTHON_FAQS.forEach { faq ->
                            item(key = "faq_${faq.id}") {
                                FaqAccordionItem(faq = faq)
                            }
                        }
                    }

                    "Projects" -> {
                        item(key = "projects-header") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hands-on Practice Projects",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EduNovaBadge("${PythonRoadmapStagingData.PYTHON_PROJECTS.size} Projects")
                                }
                                Text(
                                    text = "Build real-world Python applications to cement your knowledge and build your portfolio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        PythonRoadmapStagingData.PYTHON_PROJECTS.forEach { project ->
                            val isCompleted = uiState.completedProjectIds.contains(project.id)
                            item(key = "proj_${project.id}") {
                                RoadmapProjectCard(
                                    project = project,
                                    isCompleted = isCompleted,
                                    onClick = { viewModel.selectProject(project) }
                                )
                            }
                        }
                    }

                    "AI Tutor" -> {
                        item(key = "ai-tutor-course-tab") {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = roadmapPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "EduNova Python AI Tutor",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Personalized 1-on-1 code mentorship",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Ask any question about Python syntax, debugging errors, object-oriented principles, or FastAPI endpoints. Your AI Tutor has full context of this roadmap.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                    Text(
                                        text = "Suggested Questions:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val prompts = listOf(
                                        "Explain list comprehensions vs generator expressions in Python",
                                        "How do Python decorators work under the hood?",
                                        "What is the difference between asyncio and multiprocessing?",
                                        "How to write clean unit tests with pytest for FastAPI?"
                                    )

                                    prompts.forEach { prompt ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setAiTutorPrompt(prompt)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.QuestionAnswer,
                                                    contentDescription = null,
                                                    tint = roadmapPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = prompt,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = null,
                                                    tint = roadmapPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!uiState.aiTutorTopicPrompt.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = EduNovaSuccess.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, EduNovaSuccess.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Active Query Queued:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EduNovaSuccess
                                                )
                                                Text(
                                                    text = uiState.aiTutorTopicPrompt ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
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

            // Floating AI Tutor pill bar at the bottom (matching Screenshot 2 & 3)
            AiTutorFloatingPill(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                onClick = { viewModel.setPythonCourseTab("AI Tutor") }
            )

            // Topic Detail Bottom Sheet
            selectedVisualNode?.let { node ->
                EduNovaTopicDetailBottomSheet(
                    node = node,
                    isCompleted = uiState.completedTopicIds.contains(node.id),
                    onToggleComplete = { viewModel.toggleTopicCompletion(node.id) },
                    onAskAiTutor = { prompt ->
                        viewModel.setAiTutorPrompt(prompt)
                        viewModel.setPythonCourseTab("AI Tutor")
                        selectedVisualNode = null
                    },
                    onDismiss = { selectedVisualNode = null },
                    languageLabel = "Python Example:"
                )
            }
        }
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
    onViewProfile: () -> Unit = {},
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
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
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onContinueLearning),
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
text = { Text("View Assessment Profile") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onViewProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Remove from Saved" else "Save for Offline") },
                            leadingIcon = {
                                Icon(
                                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isSaved) EduNovaSuccess else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleSave()
                            }
                        )
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
                if (isSaved) {
                    EduNovaBadge("Saved Offline", containerColor = EduNovaSuccess.copy(alpha = 0.12f), contentColor = EduNovaSuccess)
                }
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
    onDismiss: () -> Unit,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    onStartAssessment: (String) -> Unit = {}
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
                onClick = {
                    onStartAssessment(roadmap.title)
                },
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
            ) {
                Text("Close")
            }
        },
        dismissButton = if (onToggleSave != null) {
            {
                OutlinedButton(
                    onClick = onToggleSave,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSaved) EduNovaSuccess else roadmapPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save Offline")
                }
            }
        } else null
    )
}

@Composable
private fun PersonalizedRoadmapDetailDialog(
    roadmap: PersonalizedRoadmapDetail,
    completedMilestones: Set<String>,
    onToggleMilestone: (String) -> Unit,
    roadmapPrimary: Color,
    onDismiss: () -> Unit,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null
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
        },
        dismissButton = if (onToggleSave != null) {
            {
                OutlinedButton(
                    onClick = onToggleSave,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSaved) EduNovaSuccess else roadmapPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save Offline")
                }
            }
        } else null
    )
}
