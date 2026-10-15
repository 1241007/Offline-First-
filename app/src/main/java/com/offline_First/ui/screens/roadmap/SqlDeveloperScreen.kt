package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.SqlRoadmapStagingData
import com.offline_First.data.staging.VisualRoadmapNode
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.components.EduNovaFilterChip
import com.offline_First.ui.theme.*

/**
 * Dedicated SQL Roadmap Screen.
 * Conforms to Screenshot 5 EduNova Java UI standard with zoom/pan controls,
 * mint/teal styling, FAQs, Projects tab, and AI Tutor integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlDeveloperCourseScreen(
    uiState: RoadmapUiState,
    viewModel: RoadmapViewModel,
    onBack: () -> Unit
) {
    val progress = viewModel.calculateSqlProgress()
    val totalTopics = SqlRoadmapStagingData.ALL_VISUAL_NODES.size
    val completedCount = uiState.completedSqlTopicIds.size
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
                // Header & Title section
                item(key = "sql-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SQL Roadmap",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Step by step guide to learning SQL in 2026",
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
                item(key = "sql-tabs") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf("Roadmap", "Projects", "AI Tutor")
                        tabs.forEach { tab ->
                            val isSelected = uiState.sqlCourseTab == tab
                            EduNovaFilterChip(
                                text = tab,
                                selected = isSelected,
                                onClick = { viewModel.setSqlCourseTab(tab) },
                                selectedContainerColor = roadmapPrimary
                            )
                        }
                    }
                }

                // Tab Content Switch
                when (uiState.sqlCourseTab) {
                    "Roadmap" -> {
                        // "What is SQL?" Accordion
                        item(key = "what-is-sql-accordion") {
                            WhatIsSQLBanner()
                        }

                        // Connected Branching Visual Roadmap Diagram inside Zoomable Canvas
                        item(key = "sql-visual-diagram") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Interactive Learning Diagram",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EduNovaBadge("${SqlRoadmapStagingData.ALL_VISUAL_NODES.size} Connected Topics")
                                }

                                Text(
                                    text = "Pinch to zoom in/out or drag to explore. Tap any node to inspect details.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                EduNovaZoomableCanvasBox(
                                    modifier = Modifier.fillMaxWidth()
                                ) { _ ->
                                    SqlVisualRoadmapDiagram(
                                        selectedNodeId = selectedVisualNode?.id,
                                        completedTopicIds = uiState.completedSqlTopicIds,
                                        onSelectNode = { node ->
                                            selectedVisualNode = node
                                        }
                                    )
                                }
                            }
                        }

                        // Frequently Asked Questions Header
                        item(key = "sql-faqs-header") {
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
                                    EduNovaBadge("${SqlRoadmapStagingData.SQL_FAQS.size} FAQs")
                                }
                                Text(
                                    text = "Common questions about mastering SQL queries, indexing, and transactional isolation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SqlRoadmapStagingData.SQL_FAQS.forEach { faq ->
                            item(key = "sql_faq_${faq.id}") {
                                EduNovaFaqAccordionItem(faq = faq)
                            }
                        }
                    }

                    "Projects" -> {
                        item(key = "sql-projects-header") {
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
                                    EduNovaBadge("${SqlRoadmapStagingData.SQL_PROJECTS.size} Projects")
                                }
                                Text(
                                    text = "Real-world database schemas and queries to cement your database design skills",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SqlRoadmapStagingData.SQL_PROJECTS.forEach { project ->
                            item(key = "sql_project_${project.id}") {
                                EduNovaProjectCard(
                                    project = project,
                                    onAskAi = { prompt ->
                                        viewModel.setAiTutorPrompt(prompt)
                                        viewModel.setSqlCourseTab("AI Tutor")
                                    }
                                )
                            }
                        }
                    }

                    "AI Tutor" -> {
                        item(key = "sql-ai-tutor-container") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = EduNovaAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "EduNova SQL AI Tutor",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Specialized mentorship for Relational Databases & SQL",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Ask any question about complex JOINs, Window Functions, ACID transactions, EXPLAIN ANALYZE query plans, or indexing strategies. Your tutor has full context of this roadmap.",
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

                                    val sqlPrompts = listOf(
                                        "How do ROW_NUMBER(), RANK(), and DENSE_RANK() differ in SQL?",
                                        "Explain the four ACID properties and how PostgreSQL ensures Isolation",
                                        "How do I optimize a slow JOIN query using EXPLAIN ANALYZE and B-Tree indexes?",
                                        "What is the difference between a Correlated Subquery and a Common Table Expression (CTE)?"
                                    )

                                    sqlPrompts.forEach { prompt ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setAiTutorPrompt(prompt) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = prompt,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
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
                                                    text = uiState.aiTutorTopicPrompt,
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

            // Floating AI Tutor pill bar at the bottom
            AiTutorFloatingPill(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                onClick = { viewModel.setSqlCourseTab("AI Tutor") }
            )

            // Topic Detail Bottom Sheet
            selectedVisualNode?.let { node ->
                EduNovaTopicDetailBottomSheet(
                    node = node,
                    isCompleted = uiState.completedSqlTopicIds.contains(node.id),
                    onToggleComplete = { viewModel.toggleSqlTopicCompletion(node.id) },
                    onAskAiTutor = { prompt ->
                        viewModel.setAiTutorPrompt(prompt)
                        viewModel.setSqlCourseTab("AI Tutor")
                        selectedVisualNode = null
                    },
                    onDismiss = { selectedVisualNode = null },
                    languageLabel = "SQL Example:"
                )
            }
        }
    }
}

@Composable
private fun WhatIsSQLBanner() {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What is SQL?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "SQL (Structured Query Language) is the domain-specific declarative language used in programming and designed for managing data held in relational database management systems (RDBMS). It enables developers and data engineers to define tables (DDL), query and update data (DML), enforce relational constraints, manage indexes, and execute atomic transactions with ACID guarantees.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
