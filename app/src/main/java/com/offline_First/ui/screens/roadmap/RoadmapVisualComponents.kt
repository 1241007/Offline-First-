package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import com.offline_First.data.staging.VisualRoadmapNode
import com.offline_First.data.staging.PythonRoadmapStagingData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.LearningPathItem
import com.offline_First.data.staging.RoadmapFaqItem
import com.offline_First.data.staging.RoadmapLessonTopic
import com.offline_First.data.staging.LessonPackItem
import com.offline_First.data.staging.RoadmapModuleData
import com.offline_First.data.staging.RoadmapProjectItem
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.*
import com.offline_First.ui.utils.IconResolver

/**
 * Search Bar styled in the EduNova warm theme, matching Screenshot 1.
 */
@Composable
fun RoadmapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = "What do you want to learn today?",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EduNovaShapes.card),
        color = EduNovaSurface,
        border = BorderStroke(1.dp, EduNovaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = EduNovaTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EduNovaTextSecondary
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = EduNovaTextPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = EduNovaTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card for each of the 7 Learning Paths (Screenshot 1).
 */
@Composable
fun LearningPathCard(
    path: LearningPathItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EduNovaShapes.card))
            .border(1.dp, if (path.isImplemented) EduNovaPrimary.copy(alpha = 0.5f) else EduNovaBorder, RoundedCornerShape(EduNovaShapes.card))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(EduNovaShapes.card),
        color = EduNovaSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (path.isImplemented) EduNovaPrimaryContainer else EduNovaSurfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconResolver.resolve(path.iconKey),
                        contentDescription = "${path.title} icon",
                        tint = if (path.isImplemented) EduNovaPrimary else EduNovaSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = path.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${path.category} • ${path.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextSecondary
                    )
                }
                if (path.isImplemented) {
                    EduNovaBadge(
                        text = "Full Track",
                        containerColor = EduNovaPrimaryContainer,
                        contentColor = EduNovaPrimary
                    )
                } else {
                    EduNovaBadge(
                        text = "Track Overview",
                        containerColor = EduNovaSurfaceVariant.copy(alpha = 0.6f),
                        contentColor = EduNovaTextSecondary
                    )
                }
            }

            Text(
                text = path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = EduNovaTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${path.totalModules} modules • ${path.totalProjects} projects",
                    style = MaterialTheme.typography.labelSmall,
                    color = EduNovaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (path.isImplemented) "Explore Roadmap" else "View Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (path.isImplemented) EduNovaPrimary else EduNovaSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (path.isImplemented) EduNovaPrimary else EduNovaSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Expandable "What is a Python developer?" card matching Screenshot 2.
 */
@Composable
fun WhatIsPythonBanner(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EduNovaShapes.card))
            .border(1.dp, EduNovaBorder, RoundedCornerShape(EduNovaShapes.card))
            .clickable { expanded = !expanded },
        color = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "What is a Python developer?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = EduNovaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "A Python developer writes software, builds server-side web applications and REST APIs (FastAPI, Django), develops automation scripts, and engineers data pipelines. Python's versatility makes it a primary language for backend systems, machine learning, cloud infrastructure, and automation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Key areas in this roadmap:\n• Core Syntax & Object-Oriented Design\n• Data Structures & Algorithms\n• File I/O & JSON persistence\n• Concurrency & Modern Async/Await\n• High-performance APIs with FastAPI",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = EduNovaTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Vertical Connected Timeline Node for each Python Module (Screenshot 3).
 * Visually communicates the step-by-step learning sequence inspired by Roadmap.sh.
 */
@Composable
fun ModuleTimelineNode(
    module: RoadmapModuleData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    completedTopics: Set<String>,
    onTopicClick: (RoadmapLessonTopic) -> Unit,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val completedCount = module.topics.count { completedTopics.contains(it.id) }
    val totalCount = module.topics.size
    val isModuleFullyCompleted = totalCount > 0 && completedCount == totalCount
    val isModuleInProgress = completedCount in 1 until totalCount

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Step Indicator with Vertical Connecting Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Milestone Node Icon / Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isModuleFullyCompleted -> EduNovaSuccess
                            isModuleInProgress -> EduNovaPrimary
                            else -> EduNovaSurface
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when {
                            isModuleFullyCompleted -> EduNovaSuccess
                            isModuleInProgress -> EduNovaPrimary
                            else -> EduNovaBorder
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isModuleFullyCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isModuleInProgress) {
                    Text(
                        text = "${module.number}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "${module.number}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextSecondary
                    )
                }
            }

            // Connecting Vertical Guideline
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (isExpanded) 40.dp + (module.topics.size * 54).dp else 48.dp)
                        .background(if (isModuleFullyCompleted) EduNovaSuccess.copy(alpha = 0.5f) else EduNovaBorder)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Module Header Card and Expandable Topics
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(EduNovaShapes.card))
                .border(
                    width = 1.dp,
                    color = when {
                        isModuleFullyCompleted -> EduNovaSuccess.copy(alpha = 0.5f)
                        isExpanded -> EduNovaPrimary.copy(alpha = 0.6f)
                        else -> EduNovaBorder
                    },
                    shape = RoundedCornerShape(EduNovaShapes.card)
                )
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing)),
            color = EduNovaSurface,
            shape = RoundedCornerShape(EduNovaShapes.card)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Module ${module.number}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isModuleFullyCompleted) EduNovaSuccess else EduNovaPrimary
                            )
                            Text(
                                text = "•  ${module.estimatedWeeks}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EduNovaTextSecondary
                            )
                        }
                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )
                    }

                    // Status / Progress indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isModuleFullyCompleted) EduNovaSuccess.copy(alpha = 0.15f)
                                else EduNovaPrimaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$completedCount/$totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isModuleFullyCompleted) EduNovaSuccess else EduNovaPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = EduNovaTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = EduNovaTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Expanded Topics List
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = EduNovaBorder.copy(alpha = 0.6f))
                        module.topics.forEach { topic ->
                            val isTopicDone = completedTopics.contains(topic.id)
                            TopicRowItem(
                                topic = topic,
                                isCompleted = isTopicDone,
                                onClick = { onTopicClick(topic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clickable row for each topic in an expanded module.
 */
@Composable
fun TopicRowItem(
    topic: RoadmapLessonTopic,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (isCompleted) EduNovaSuccess.copy(alpha = 0.4f) else EduNovaBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isCompleted) EduNovaSuccess.copy(alpha = 0.08f) else EduNovaBackground,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) EduNovaSuccess else EduNovaBorder),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Medium,
                    color = EduNovaTextPrimary
                )
                Text(
                    text = "${topic.estimatedMinutes} min read & exercise",
                    style = MaterialTheme.typography.labelSmall,
                    color = EduNovaTextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EduNovaPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Project card for the Python Projects section (Section 4).
 */
@Composable
fun RoadmapProjectCard(
    project: RoadmapProjectItem,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EduNovaShapes.card))
            .border(1.dp, if (isCompleted) EduNovaSuccess.copy(alpha = 0.5f) else EduNovaBorder, RoundedCornerShape(EduNovaShapes.card))
            .clickable(onClick = onClick),
        color = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (project.difficulty) {
                                "Beginner" -> EduNovaSuccess.copy(alpha = 0.2f)
                                "Intermediate" -> EduNovaPrimaryContainer
                                else -> EduNovaWarning.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = when (project.difficulty) {
                            "Beginner" -> EduNovaSuccess
                            "Intermediate" -> EduNovaPrimary
                            else -> EduNovaWarning
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary
                    )
                    Text(
                        text = "${project.difficulty} • ~${project.estimatedHours} hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextSecondary
                    )
                }
                if (isCompleted) {
                    EduNovaBadge(
                        text = "Completed",
                        containerColor = EduNovaSuccess.copy(alpha = 0.15f),
                        contentColor = EduNovaSuccess,
                        icon = Icons.Default.Check
                    )
                }
            }

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = EduNovaTextSecondary,
                lineHeight = 20.sp
            )

            // Skills Row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                project.relevantSkills.take(3).forEach { skill ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EduNovaBackground)
                            .border(1.dp, EduNovaBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.labelSmall,
                            color = EduNovaTextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Project Guide",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Expandable FAQ Item matching Screenshot 4.
 */
@Composable
fun FaqAccordionItem(
    faq: RoadmapFaqItem,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EduNovaShapes.card))
            .border(1.dp, EduNovaBorder, RoundedCornerShape(EduNovaShapes.card))
            .clickable { expanded = !expanded },
        color = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse answer" else "Expand answer",
                    tint = EduNovaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = EduNovaBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * Floating AI Tutor bar matching Screenshot 2.
 */
@Composable
fun AiTutorFloatingPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(EduNovaShapes.pill))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(EduNovaShapes.pill),
        color = EduNovaTextPrimary,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = EduNovaAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "AI Tutor",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Have a question? Ask here...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EduNovaAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Read-only / Guide dialog when tapping a Project.
 */
@Composable
fun ProjectDetailDialog(
    project: RoadmapProjectItem,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EduNovaBadge(
                        text = project.difficulty,
                        containerColor = EduNovaPrimaryContainer,
                        contentColor = EduNovaPrimary
                    )
                    Text(
                        text = "~${project.estimatedHours} hrs",
                        style = MaterialTheme.typography.labelSmall,
                        color = EduNovaTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduNovaTextSecondary
                )

                Text(
                    text = "Expected Deliverables:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )
                project.deliverables.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•  ", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = EduNovaTextSecondary
                        )
                    }
                }

                Text(
                    text = "Implementation Guide:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaBackground,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onToggleComplete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) EduNovaSuccess else EduNovaPrimary
                ),
                shape = RoundedCornerShape(EduNovaShapes.button)
            ) {
                Text(if (isCompleted) "Completed ✓" else "Mark Project Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EduNovaTextSecondary)
            }
        },
        containerColor = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    )
}

/**
 * Informative dialog when tapping non-Python learning paths.
 */
@Composable
fun UpcomingPathDialog(
    path: LearningPathItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EduNovaPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconResolver.resolve(path.iconKey),
                        contentDescription = null,
                        tint = EduNovaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = path.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EduNovaBadge(text = "${path.level} • ${path.duration}")
                Text(
                    text = path.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduNovaTextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaBackground,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Curriculum Highlights:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• ${path.totalModules} Core Learning Modules\n• ${path.totalProjects} Practical Portfolio Projects\n• Offline topic lessons & assessments coming in next release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EduNovaTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                shape = RoundedCornerShape(EduNovaShapes.button)
            ) {
                Text("Understood")
            }
        },
        containerColor = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    )
}

/**
 * Compact Course Card containing ONLY the course name and bookmark affordance.
 * Matching the exact visual style of Roadmap.sh adapted to EduNova's theme.
 */
@Composable
fun CompactCourseCard(
    courseName: String,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isBookmarked) EduNovaPrimary.copy(alpha = 0.5f) else EduNovaBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = EduNovaSurface,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = courseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = EduNovaTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleBookmark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                    tint = if (isBookmarked) EduNovaPrimary else EduNovaTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Top Tab Bar with Roadmaps, Lesson Packs (NEW), and Project Ideas.
 */
@Composable
fun RoadmapTopTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("My Roadmaps", "Roadmaps", "Lesson Packs", "Project Ideas")

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) EduNovaTextPrimary else EduNovaTextSecondary
                        )

                        if (tab == "Lesson Packs") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EduNovaAccent.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 1.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EduNovaAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Indicator line
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(if (isSelected) 36.dp else 0.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) EduNovaPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}

/**
 * Prominent "Learn with AI" Card (matching Screenshot 2).
 */
@Composable
fun LearnWithAiCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, EduNovaPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = EduNovaPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Learn with AI",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Dialog displaying course details for non-Python courses.
 */
@Composable
fun CourseOverviewDialog(
    courseName: String,
    category: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onOpenRoadmap: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) EduNovaPrimary else EduNovaTextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EduNovaBadge("$category Track")

                Text(
                    text = "A step-by-step roadmap to mastering $courseName, curated with modern industry standards and practical milestone projects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduNovaTextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EduNovaPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Staged & available offline in EduNova Catalog",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = EduNovaTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenRoadmap,
                colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                shape = RoundedCornerShape(EduNovaShapes.button)
            ) {
                Text("Open Track")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EduNovaTextSecondary)
            }
        },
        containerColor = EduNovaSurface,
        shape = RoundedCornerShape(EduNovaShapes.card)
    )
}

/**
 * Lesson Pack Card for Lesson Packs tab.
 */
@Composable
fun LessonPackCard(
    pack: LessonPackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, EduNovaBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = EduNovaSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EduNovaBadge(pack.category)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${pack.lessonCount} Lessons • ${pack.durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = EduNovaTextSecondary
                )
            }

            Text(
                text = pack.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EduNovaTextPrimary
            )

            Text(
                text = pack.description,
                style = MaterialTheme.typography.bodySmall,
                color = EduNovaTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// =============================================================================
// CONNECTED BRANCHING ROADMAP DIAGRAM (Roadmap.sh Style Responsive Learning Map)
// =============================================================================

/**
 * An individual interactive node in the connected roadmap diagram.
 * Adapts between mobile-compact and tablet-expanded presentations.
 */
@Composable
fun RoadmapDiagramNode(
    node: VisualRoadmapNode,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMajor: Boolean = false,
    compact: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = if (compact) 10.sp else 12.sp,
    backgroundColorOverride: Color? = null
) {
    val backgroundColor = backgroundColorOverride ?: when {
        isCompleted -> EduNovaPrimaryContainer.copy(alpha = 0.55f)
        isMajor -> Color(0xFFFFF1C5) // Warm yellow accent matching reference screenshot
        else -> EduNovaSurface
    }
    val borderColor = when {
        isSelected -> EduNovaPrimary
        isCompleted -> EduNovaSuccess
        isMajor -> Color(0xFFD48B28) // Subtle gold/teal border for major milestones
        else -> EduNovaBorder
    }
    val borderWidth = if (isSelected) 2.dp else if (isMajor) 1.5.dp else 1.dp

    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(if (isSelected) 3.dp else 1.dp, RoundedCornerShape(if (isMajor) 8.dp else 6.dp)),
        shape = RoundedCornerShape(if (isMajor) 8.dp else 6.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 8.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = fontSize,
                    fontWeight = if (isMajor) FontWeight.Bold else FontWeight.Medium
                ),
                color = EduNovaTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isCompleted) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(if (compact) 12.dp else 15.dp)
                        .background(EduNovaSuccess, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(if (compact) 8.dp else 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draws curved bezier connector lines fanning out from the center milestone
 * to subtopic rows on the left or right side.
 */
@Composable
fun CurvedConnectorWing(
    count: Int,
    isLeft: Boolean,
    itemHeight: Dp = 32.dp,
    spacing: Dp = 6.dp,
    width: Dp = 16.dp,
    color: Color = EduNovaPrimary.copy(alpha = 0.55f),
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height((itemHeight + spacing) * count - spacing)
    ) {
        val totalH = size.height
        val centerY = totalH / 2f
        val w = size.width
        val stroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
        )

        val itemH = itemHeight.toPx()
        val spacePx = spacing.toPx()

        for (i in 0 until count) {
            val targetY = i * (itemH + spacePx) + itemH / 2f
            val path = Path()
            if (isLeft) {
                // Starts at right edge (centerY) and curves to left edge (targetY)
                path.moveTo(w, centerY)
                path.cubicTo(
                    w * 0.35f, centerY,
                    w * 0.65f, targetY,
                    0f, targetY
                )
            } else {
                // Starts at left edge (centerY) and curves to right edge (targetY)
                path.moveTo(0f, centerY)
                path.cubicTo(
                    w * 0.65f, centerY,
                    w * 0.35f, targetY,
                    w, targetY
                )
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

/**
 * Draws a straight vertical connecting line between milestones.
 */
@Composable
fun VerticalMilestoneConnector(
    height: Dp = 24.dp,
    width: Dp = 2.dp,
    color: Color = EduNovaPrimary.copy(alpha = 0.65f),
    isDotted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val stroke = if (isDotted) {
            Stroke(
                width = size.width,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
            )
        } else {
            Stroke(width = size.width)
        }
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = stroke.width,
            pathEffect = stroke.pathEffect
        )
    }
}

/**
 * The Master Visual Roadmap Diagram.
 * Responsive mobile-first layout faithfully reproducing the Roadmap.sh reference screenshots.
 */
@Composable
fun PythonVisualRoadmapDiagram(
    selectedNodeId: String?,
    completedTopicIds: Set<String>,
    onSelectNode: (VisualRoadmapNode) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isMobile = maxWidth < 600.dp
        val colWidth = if (isMobile) 98.dp else 150.dp
        val centerWidth = if (isMobile) 114.dp else 180.dp
        val connectorWidth = if (isMobile) 16.dp else 36.dp
        val nodeHeight = if (isMobile) 32.dp else 40.dp
        val nodeSpacing = if (isMobile) 6.dp else 8.dp
        val fontSize = if (isMobile) 10.sp else 12.sp
        val milestoneFontSize = if (isMobile) 11.sp else 14.sp

        Column(
            modifier = Modifier
                .widthIn(min = if (isMobile) 340.dp else 560.dp)
                .horizontalScroll(rememberScrollState(), enabled = maxWidth < 340.dp)
                .padding(horizontal = if (isMobile) 6.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Cards Row: Related Roadmaps (Left) and roadmap.sh box (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Related Roadmaps Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaSurface,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.width(if (isMobile) 110.dp else 160.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "Related Roadmaps",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isMobile) 8.sp else 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        listOf("Backend Roadmap", "DevOps Roadmap", "Django Roadmap", "Python for Data Analysis").forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduNovaPrimary,
                                    modifier = Modifier.size(if (isMobile) 8.dp else 10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = r,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isMobile) 7.sp else 9.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Center Vertical Line
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    VerticalMilestoneConnector(height = 20.dp, isDotted = true)
                    // Python Starting Point Node
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Python",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isMobile) 15.sp else 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = EduNovaTextPrimary
                        )
                    }
                    VerticalMilestoneConnector(height = 20.dp)
                }

                // Right: Find detailed version Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaSurface,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.width(if (isMobile) 110.dp else 160.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Find detailed version with other roadmaps",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isMobile) 7.sp else 9.sp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EduNovaBorder.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "roadmap.sh",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isMobile) 8.sp else 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(vertical = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // =================================================================
            // MILESTONE 1: Learn the Basics (Exact Screenshot Layout)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Wing: 6 Rows
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(nodeSpacing),
                    horizontalAlignment = Alignment.End
                ) {
                    // Row 0: Basic Syntax
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_SYNTAX,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SYNTAX.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SYNTAX.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SYNTAX) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 1: Variables and Data Types
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_VARIABLES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_VARIABLES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_VARIABLES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_VARIABLES) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 2: Operators + @
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_OPERATORS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_OPERATORS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_OPERATORS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_OPERATORS) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.weight(2.3f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_OPERATORS_AT,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_OPERATORS_AT.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_OPERATORS_AT.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_OPERATORS_AT) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    // Row 3: Working with Strings
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_STRINGS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_STRINGS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_STRINGS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_STRINGS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 4: Conditionals
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_CONDITIONALS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CONDITIONALS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CONDITIONALS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CONDITIONALS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 5: Loops
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_LOOPS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LOOPS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LOOPS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LOOPS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                }

                // Left Connector
                CurvedConnectorWing(
                    count = 6,
                    isLeft = true,
                    itemHeight = nodeHeight,
                    spacing = nodeSpacing,
                    width = connectorWidth
                )

                // Center Node: Learn the Basics
                RoadmapDiagramNode(
                    node = PythonRoadmapStagingData.BASICS_CENTER_NODE,
                    isSelected = selectedNodeId == PythonRoadmapStagingData.BASICS_CENTER_NODE.id,
                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.BASICS_CENTER_NODE.id),
                    onClick = { onSelectNode(PythonRoadmapStagingData.BASICS_CENTER_NODE) },
                    isMajor = true,
                    compact = isMobile,
                    fontSize = milestoneFontSize,
                    modifier = Modifier.width(centerWidth).height(nodeHeight + 6.dp)
                )

                // Right Connector
                CurvedConnectorWing(
                    count = 6,
                    isLeft = false,
                    itemHeight = nodeHeight,
                    spacing = nodeSpacing,
                    width = connectorWidth
                )

                // Right Wing: 6 Rows
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(nodeSpacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Row 0: Lists + Tuples + Sets
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_LISTS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LISTS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LISTS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LISTS) },
                            compact = true,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_TUPLES,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TUPLES.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TUPLES.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TUPLES) },
                            compact = true,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_SETS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SETS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SETS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SETS) },
                            compact = true,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    // Row 1: Dictionaries
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_DICTS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DICTS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DICTS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DICTS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 2: Type Casting
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_CASTING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CASTING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CASTING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CASTING) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 3: Functions, Builtin Functions
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_FUNCTIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FUNCTIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FUNCTIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FUNCTIONS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 4: Exceptions
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_EXCEPTIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_EXCEPTIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_EXCEPTIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_EXCEPTIONS) },
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // Row 5: Comments + Type Annot.
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_COMMENTS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_COMMENTS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_COMMENTS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_COMMENTS) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_SCOPE_ANNOT,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SCOPE_ANNOT) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            VerticalMilestoneConnector(height = 28.dp)

            // =================================================================
            // MILESTONE 2: Data Structures & Algorithms
            // =================================================================
            // Milestone 2 Center Node
            RoadmapDiagramNode(
                node = PythonRoadmapStagingData.DSA_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.DSA_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.DSA_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.DSA_CENTER_NODE) },
                isMajor = true,
                compact = isMobile,
                fontSize = milestoneFontSize,
                modifier = Modifier.width(centerWidth + 24.dp).height(nodeHeight + 6.dp)
            )

            // Row containing: Left Package Managers, Central Spine, Right Advanced Concepts
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // Left Branch: Package Managers (2x3 Grid + Highlighted Node)
                Column(
                    modifier = Modifier.width(colWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pkgPairs = listOf(
                        Pair(PythonRoadmapStagingData.NODE_PYPI, PythonRoadmapStagingData.NODE_PIP),
                        Pair(PythonRoadmapStagingData.NODE_CONDA, PythonRoadmapStagingData.NODE_UV),
                        Pair(PythonRoadmapStagingData.NODE_POETRY, PythonRoadmapStagingData.NODE_PDM)
                    )
                    pkgPairs.forEach { (n1, n2) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(nodeHeight - 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            RoadmapDiagramNode(
                                node = n1,
                                isSelected = selectedNodeId == n1.id,
                                isCompleted = completedTopicIds.contains(n1.id),
                                onClick = { onSelectNode(n1) },
                                compact = true,
                                fontSize = 9.sp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            RoadmapDiagramNode(
                                node = n2,
                                isSelected = selectedNodeId == n2.id,
                                isCompleted = completedTopicIds.contains(n2.id),
                                onClick = { onSelectNode(n2) },
                                compact = true,
                                fontSize = 9.sp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                    VerticalMilestoneConnector(height = 10.dp, isDotted = true)
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_PKG_MANAGERS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PKG_MANAGERS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PKG_MANAGERS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PKG_MANAGERS) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Center Spine: 6 DSA Topics
                Column(
                    modifier = Modifier.width(centerWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dsaSpine = listOf(
                        PythonRoadmapStagingData.NODE_ARRAYS_LISTS,
                        PythonRoadmapStagingData.NODE_HASH_MAPS,
                        PythonRoadmapStagingData.NODE_STACKS_QUEUES,
                        PythonRoadmapStagingData.NODE_BST,
                        PythonRoadmapStagingData.NODE_RECURSION,
                        PythonRoadmapStagingData.NODE_SORTING_SEARCHING
                    )
                    dsaSpine.forEach { node ->
                        VerticalMilestoneConnector(height = 8.dp, isDotted = true)
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Right Branch: Builtin/Custom + 7 Advanced Concepts
                Column(
                    modifier = Modifier.width(colWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Builtin + Custom Row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight - 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_BUILTIN,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_BUILTIN.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_BUILTIN.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_BUILTIN) },
                            compact = true,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_CUSTOM,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CUSTOM.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CUSTOM.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CUSTOM) },
                            compact = true,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    VerticalMilestoneConnector(height = 6.dp, isDotted = true)
                    val advList = listOf(
                        PythonRoadmapStagingData.NODE_MODULES,
                        PythonRoadmapStagingData.NODE_ERROR_HANDLING,
                        PythonRoadmapStagingData.NODE_LAMBDAS,
                        PythonRoadmapStagingData.NODE_DECORATORS,
                        PythonRoadmapStagingData.NODE_ITERATORS,
                        PythonRoadmapStagingData.NODE_REGEX,
                        PythonRoadmapStagingData.NODE_VARIABLE_SCOPE
                    )
                    advList.forEach { node ->
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            isMajor = true,
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight)
                        )
                    }
                }
            }

            VerticalMilestoneConnector(height = 28.dp)

            // =================================================================
            // MILESTONE 3: Object-Oriented Programming
            // =================================================================
            // Milestone 3 Center Node
            RoadmapDiagramNode(
                node = PythonRoadmapStagingData.OOP_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.OOP_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.OOP_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.OOP_CENTER_NODE) },
                isMajor = true,
                compact = isMobile,
                fontSize = milestoneFontSize,
                modifier = Modifier.width(centerWidth + 28.dp).height(nodeHeight + 6.dp)
            )

            // Row containing: Left Common Packages/Files, Center OOP Subnodes, Right Environments & Static Typing
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Common Packages, pyproject.toml, Comprehensions, Paradigms, Context Manager
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_COMMON_PACKAGES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_COMMON_PACKAGES) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    // pyproject.toml Card
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduNovaSurface,
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectNode(PythonRoadmapStagingData.NODE_PYPROJECT) }
                    ) {
                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("pyproject.toml", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            Text("Configuration", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary))
                        }
                    }
                    val leftOopNodes = listOf(
                        PythonRoadmapStagingData.NODE_COMPREHENSIONS,
                        PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS,
                        PythonRoadmapStagingData.NODE_PARADIGMS,
                        PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS
                    )
                    leftOopNodes.forEach { node ->
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            isMajor = true,
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Center: 2x2 Grid under OOP
                Column(
                    modifier = Modifier.width(centerWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VerticalMilestoneConnector(height = 10.dp, isDotted = true)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_CLASSES,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CLASSES.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CLASSES.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CLASSES) },
                            compact = true,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_METHODS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_METHODS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_METHODS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_METHODS) },
                            compact = true,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_INHERITANCE,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_INHERITANCE.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_INHERITANCE.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_INHERITANCE) },
                            compact = true,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        RoadmapDiagramNode(
                            node = PythonRoadmapStagingData.NODE_ENCAPSULATION,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ENCAPSULATION.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ENCAPSULATION.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ENCAPSULATION) },
                            compact = true,
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Right: Environments Box + Static Typing Stack
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Environments Box
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduNovaSurface,
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            listOf(
                                PythonRoadmapStagingData.NODE_PIPENV,
                                PythonRoadmapStagingData.NODE_VIRTUALENV,
                                PythonRoadmapStagingData.NODE_PYENV
                            ).forEach { env ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (completedTopicIds.contains(env.id)) EduNovaPrimaryContainer else EduNovaBorder.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth().clickable { onSelectNode(env) }
                                ) {
                                    Text(
                                        text = env.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            Text("Environments", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary))
                        }
                    }

                    // Static Typing Highlighted Node
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_STATIC_TYPING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_STATIC_TYPING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_STATIC_TYPING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_STATIC_TYPING) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )

                    // Typing subnodes
                    val typingNodes = listOf(
                        PythonRoadmapStagingData.NODE_TYPING,
                        PythonRoadmapStagingData.NODE_MYPY,
                        PythonRoadmapStagingData.NODE_PYRIGHT,
                        PythonRoadmapStagingData.NODE_PYRE,
                        PythonRoadmapStagingData.NODE_PYDANTIC
                    )
                    typingNodes.forEach { node ->
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            fontSize = 9.sp,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight - 4.dp)
                        )
                    }
                }
            }

            VerticalMilestoneConnector(height = 28.dp)

            // =================================================================
            // MILESTONE 4: Concurrency
            // =================================================================
            // Concurrency Center Node
            RoadmapDiagramNode(
                node = PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE) },
                isMajor = true,
                compact = isMobile,
                fontSize = milestoneFontSize,
                modifier = Modifier.width(centerWidth + 20.dp).height(nodeHeight + 6.dp)
            )

            // Row containing: Left Frameworks, Center Concurrency Subnodes, Right Code Formatting & Files
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Learn a Framework + Synchronous/Async sub-boxes
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )

                    // Frameworks Container Card
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduNovaSurface,
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            // Synchronous
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_PLOTLY_DASH, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PLOTLY_DASH.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PLOTLY_DASH.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PLOTLY_DASH) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_PYRAMID, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYRAMID.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYRAMID.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYRAMID) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                            }
                            Text("Synchronous", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary), modifier = Modifier.align(Alignment.CenterHorizontally))

                            // Asynchronous
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_GEVENT, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_GEVENT.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_GEVENT.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_GEVENT) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_AIOHTTP, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_AIOHTTP.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_AIOHTTP.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_AIOHTTP) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_TORNADO, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TORNADO.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TORNADO.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TORNADO) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_SANIC, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SANIC.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SANIC.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SANIC) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                            }
                            Text("Asynchronous", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary), modifier = Modifier.align(Alignment.CenterHorizontally))

                            // Sync + Async (FastAPI, Django, Flask)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_FASTAPI, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FASTAPI.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FASTAPI.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FASTAPI) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_DJANGO, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DJANGO.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DJANGO.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DJANGO) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                                RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_FLASK, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FLASK.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FLASK.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FLASK) }, compact = true, fontSize = 7.sp, modifier = Modifier.weight(1f).height(24.dp))
                            }
                            Text("Synchronous + Asynchronous", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary), modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Center: Multiprocessing, Asynchrony, GIL, Threading, glob
                Column(
                    modifier = Modifier.width(centerWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val concSpine = listOf(
                        PythonRoadmapStagingData.NODE_MULTIPROCESSING,
                        PythonRoadmapStagingData.NODE_ASYNCHRONY,
                        PythonRoadmapStagingData.NODE_GIL,
                        PythonRoadmapStagingData.NODE_THREADING,
                        PythonRoadmapStagingData.NODE_GLOB
                    )
                    concSpine.forEach { node ->
                        VerticalMilestoneConnector(height = 8.dp, isDotted = true)
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(connectorWidth))

                // Right: Code Formatting, File Handling, Documentation
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // yapf, black, ruff
                    Row(
                        modifier = Modifier.fillMaxWidth().height(nodeHeight - 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_YAPF, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_YAPF.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_YAPF.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_YAPF) }, compact = true, fontSize = 8.sp, modifier = Modifier.weight(1f).fillMaxHeight())
                        RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_BLACK, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_BLACK.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_BLACK.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_BLACK) }, compact = true, fontSize = 8.sp, modifier = Modifier.weight(1f).fillMaxHeight())
                        RoadmapDiagramNode(node = PythonRoadmapStagingData.NODE_RUFF, isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_RUFF.id, isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_RUFF.id), onClick = { onSelectNode(PythonRoadmapStagingData.NODE_RUFF) }, compact = true, fontSize = 8.sp, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    VerticalMilestoneConnector(height = 6.dp, isDotted = true)
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_CODE_FORMATTING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CODE_FORMATTING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CODE_FORMATTING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CODE_FORMATTING) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    VerticalMilestoneConnector(height = 6.dp)
                    RoadmapDiagramNode(
                        node = PythonRoadmapStagingData.NODE_FILE_HANDLING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FILE_HANDLING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FILE_HANDLING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FILE_HANDLING) },
                        isMajor = true,
                        compact = isMobile,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth().height(nodeHeight)
                    )
                    VerticalMilestoneConnector(height = 6.dp)
                    // Documentation (Sphinx) Box
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduNovaSurface,
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            RoadmapDiagramNode(
                                node = PythonRoadmapStagingData.NODE_SPHINX,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SPHINX.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SPHINX.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SPHINX) },
                                compact = true,
                                fontSize = 8.sp,
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                            )
                            Text("Documentation", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = EduNovaTextSecondary))
                        }
                    }
                }
            }

            VerticalMilestoneConnector(height = 28.dp)

            // =================================================================
            // MILESTONE 5: Testing
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Testing Center Milestone Node
                RoadmapDiagramNode(
                    node = PythonRoadmapStagingData.TESTING_CENTER_NODE,
                    isSelected = selectedNodeId == PythonRoadmapStagingData.TESTING_CENTER_NODE.id,
                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.TESTING_CENTER_NODE.id),
                    onClick = { onSelectNode(PythonRoadmapStagingData.TESTING_CENTER_NODE) },
                    isMajor = true,
                    compact = isMobile,
                    fontSize = milestoneFontSize,
                    modifier = Modifier.width(centerWidth).height(nodeHeight + 6.dp)
                )

                CurvedConnectorWing(
                    count = 4,
                    isLeft = false,
                    itemHeight = nodeHeight,
                    spacing = nodeSpacing,
                    width = connectorWidth
                )

                // Testing Subnodes Stack on Right
                Column(
                    modifier = Modifier.width(colWidth),
                    verticalArrangement = Arrangement.spacedBy(nodeSpacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    val testingNodes = listOf(
                        PythonRoadmapStagingData.NODE_TOX,
                        PythonRoadmapStagingData.NODE_UNITTEST,
                        PythonRoadmapStagingData.NODE_DOCTEST,
                        PythonRoadmapStagingData.NODE_PYTEST
                    )
                    testingNodes.forEach { node ->
                        RoadmapDiagramNode(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = isMobile,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth().height(nodeHeight)
                        )
                    }
                }
            }

            VerticalMilestoneConnector(height = 24.dp, isDotted = true)

            // =================================================================
            // RELATED ROADMAPS FOOTER (Matching Screenshot)
            // =================================================================
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EduNovaSurface,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Have a look at the following related roadmaps",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isMobile) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = EduNovaTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf("Backend", "Django", "Python for Data Analysis").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3843D0), // Deep blue/purple badge matching reference
                                modifier = Modifier.padding(horizontal = 3.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (isMobile) 8.sp else 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Topic Detail Bottom Sheet displayed whenever any node in the diagram is tapped.
 * Displays topic details, learning objectives, code snippet, completion toggle, and AI tutor link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailBottomSheet(
    node: VisualRoadmapNode,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onAskAiTutor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with section badge and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EduNovaBadge(node.section)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = EduNovaTextSecondary
                    )
                }
            }

            // Topic Title
            Text(
                text = node.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = EduNovaTextPrimary
            )

            // Mark as Complete button
            Button(
                onClick = onToggleComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) EduNovaSuccess else EduNovaPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Completed ✓ (Tap to Mark Incomplete)" else "Mark as Complete",
                    fontWeight = FontWeight.Bold
                )
            }

            // Summary / Explanation
            if (node.explanation.isNotBlank()) {
                Text(
                    text = node.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduNovaTextSecondary,
                    lineHeight = 22.sp
                )
            }

            // Key Learning Objectives / Subtopics
            if (node.objectives.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Key Learning Objectives:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary
                    )
                    node.objectives.forEach { obj ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                color = EduNovaPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = obj,
                                style = MaterialTheme.typography.bodySmall,
                                color = EduNovaTextPrimary
                            )
                        }
                    }
                }
            }

            // Example Code Snippet Card
            if (node.codeSnippet.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Example Code Snippet:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E1E2E), // Modern dark code card
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = node.codeSnippet,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCDD6F4)
                            )
                        )
                    }
                }
            }

            // Ask AI Tutor About This Topic
            OutlinedButton(
                onClick = {
                    onAskAiTutor("Explain ${node.title} in Python with real-world examples, common pitfalls, and best practices.")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, EduNovaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ask AI Tutor About This Topic",
                    color = EduNovaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
