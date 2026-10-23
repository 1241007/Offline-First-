package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.domain.model.PersonalizedRoadmapDetail
import com.offline_First.domain.model.PersonalizedRoadmapTopic
import com.offline_First.domain.model.PersonalizedTopicStatus
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive 2D Navigable Topic Graph Canvas matching roadmap.sh reference.
 * Features:
 * - Prerequisite DAG with branching paths and converging milestones
 * - Smooth pan and pinch-to-zoom (0.4x to 2.5x)
 * - Zoom in, zoom out, and fit-to-view controls
 * - Node selection and visible active state
 * - Clear status badges (Available, Locked, In-Progress, Completed, Skipped)
 * - Lightweight native Canvas rendering optimized for low-end Android devices
 */
@Composable
fun PersonalizedNavigableGraphView(
    roadmap: PersonalizedRoadmapDetail,
    selectedTopicId: String?,
    onTopicSelected: (PersonalizedRoadmapTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")

    val topics = roadmap.topics
    val tagToTopic = remember(topics) { topics.associateBy { it.id } }

    // Map each topic to layout grid tier (tier Y, column X)
    val layoutCoordinates = remember(topics) {
        computeTopologicalLayout(topics)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 2.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        // --- 1. Zoomable & Pannable Graph Content ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = offsetX
                    translationY = offsetY
                }
        ) {
            // --- Prerequisite Connector Edges Canvas ---
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                val nodeWidthPx = 180.dp.toPx()
                val nodeHeightPx = 80.dp.toPx()

                for (topic in topics) {
                    val toPos = layoutCoordinates[topic.id] ?: continue
                    val toCenterX = toPos.first * 260.dp.toPx() + (nodeWidthPx / 2) + 200.dp.toPx()
                    val toCenterY = toPos.second * 140.dp.toPx() + (nodeHeightPx / 2) + 60.dp.toPx()

                    for (prereqId in topic.prerequisiteTopicIds) {
                        val fromPos = layoutCoordinates[prereqId] ?: continue
                        val fromCenterX = fromPos.first * 260.dp.toPx() + (nodeWidthPx / 2) + 200.dp.toPx()
                        val fromCenterY = fromPos.second * 140.dp.toPx() + (nodeHeightPx / 2) + 60.dp.toPx()

                        val isPrereqCompleted = tagToTopic[prereqId]?.status == PersonalizedTopicStatus.COMPLETED
                        val isPrereqSkipped = tagToTopic[prereqId]?.status == PersonalizedTopicStatus.SKIPPED
                        val isCurrentActive = topic.status == PersonalizedTopicStatus.IN_PROGRESS || topic.id == selectedTopicId

                        val edgeColor = when {
                            isCurrentActive -> EduNovaAccent
                            isPrereqCompleted -> EduNovaSuccess
                            isPrereqSkipped -> Color.Gray
                            topic.status == PersonalizedTopicStatus.LOCKED -> Color.LightGray.copy(alpha = 0.7f)
                            else -> EduNovaPrimary.copy(alpha = 0.8f)
                        }

                        val strokeWidth = if (isCurrentActive) 3.5.dp.toPx() else 2.dp.toPx()
                        val pathEffect = if (isPrereqSkipped || topic.status == PersonalizedTopicStatus.LOCKED) {
                            PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        } else null

                        // Draw curved orthogonal connector
                        val path = Path().apply {
                            moveTo(fromCenterX, fromCenterY + (nodeHeightPx / 2))
                            cubicTo(
                                fromCenterX, fromCenterY + (nodeHeightPx / 2) + 40.dp.toPx(),
                                toCenterX, toCenterY - (nodeHeightPx / 2) - 40.dp.toPx(),
                                toCenterX, toCenterY - (nodeHeightPx / 2)
                            )
                        }

                        drawPath(
                            path = path,
                            color = edgeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, pathEffect = pathEffect)
                        )

                        // Draw directional arrow tip
                        drawCircle(
                            color = edgeColor,
                            radius = 4.dp.toPx(),
                            center = Offset(toCenterX, toCenterY - (nodeHeightPx / 2))
                        )
                    }
                }
            }

            // --- Topic Node Cards ---
            Box(modifier = Modifier.padding(32.dp)) {
                for (topic in topics) {
                    val pos = layoutCoordinates[topic.id] ?: Pair(0, 0)
                    val leftOffset = (pos.first * 260).dp + 200.dp
                    val topOffset = (pos.second * 140).dp + 60.dp

                    Box(
                        modifier = Modifier
                            .offset(x = leftOffset, y = topOffset)
                            .width(180.dp)
                            .height(80.dp)
                    ) {
                        GraphNodeCard(
                            topic = topic,
                            isSelected = topic.id == selectedTopicId,
                            onClick = { onTopicSelected(topic) }
                        )
                    }
                }
            }
        }

        // --- 2. Floating Zoom & Navigation Controls ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { scale = (scale + 0.2f).coerceAtMost(2.5f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { scale = (scale - 0.2f).coerceAtLeast(0.4f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.primary)
                }

                VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))

                IconButton(
                    onClick = {
                        scale = 1.0f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit to View", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // --- 3. Accessible Status Legend ---
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = EduNovaSuccess, label = "Completed")
                LegendItem(color = EduNovaAccent, label = "In-Progress")
                LegendItem(color = EduNovaPrimary, label = "Available")
                LegendItem(color = Color.Gray, label = "Skipped")
                LegendItem(color = Color.LightGray, label = "Locked")
            }
        }
    }
}

@Composable
private fun GraphNodeCard(
    topic: PersonalizedRoadmapTopic,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = topic.status == PersonalizedTopicStatus.COMPLETED
    val isInProgress = topic.status == PersonalizedTopicStatus.IN_PROGRESS
    val isLocked = topic.status == PersonalizedTopicStatus.LOCKED
    val isSkipped = topic.status == PersonalizedTopicStatus.SKIPPED

    val borderColor = when {
        isSelected -> EduNovaAccent
        isCompleted -> EduNovaSuccess
        isInProgress -> EduNovaAccent
        isSkipped -> Color.Gray
        isLocked -> Color.LightGray
        else -> EduNovaPrimary
    }

    val borderWidth = if (isSelected) 2.5.dp else 1.5.dp
    val containerColor = when {
        isSelected -> EduNovaAccent.copy(alpha = 0.12f)
        isCompleted -> EduNovaSuccess.copy(alpha = 0.08f)
        isInProgress -> EduNovaAccent.copy(alpha = 0.08f)
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> EduNovaSuccess
                            isInProgress -> EduNovaAccent
                            isSkipped -> Color.Gray
                            isLocked -> Color.LightGray
                            else -> EduNovaPrimary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Check
                        isInProgress -> Icons.Default.Schedule
                        isSkipped -> Icons.Default.FastForward
                        isLocked -> Icons.Default.Lock
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isCompleted || isInProgress || isSkipped) Color.White else EduNovaPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Text(
                    text = "${topic.estimatedMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Computes grid positions (Column X, Tier Y) for topic nodes in the DAG.
 * Handles linear progressions, branching specialization tracks, and convergence nodes.
 */
private fun computeTopologicalLayout(topics: List<PersonalizedRoadmapTopic>): Map<String, Pair<Int, Int>> {
    val layout = mutableMapOf<String, Pair<Int, Int>>()
    val idToTopic = topics.associateBy { it.id }

    var currentTier = 0
    val assigned = mutableSetOf<String>()

    while (assigned.size < topics.size) {
        // Find nodes whose prerequisites are all assigned
        val tierNodes = topics.filter { t ->
            t.id !in assigned && t.prerequisiteTopicIds.all { it in assigned }
        }

        if (tierNodes.isEmpty()) {
            // Break cycle if any malformed edge exists
            val remaining = topics.filter { it.id !in assigned }
            remaining.forEachIndexed { idx, t ->
                layout[t.id] = Pair(idx, currentTier)
                assigned.add(t.id)
            }
            break
        }

        // Branching logic: distribute nodes across columns centered at 0
        val count = tierNodes.size
        tierNodes.forEachIndexed { index, node ->
            val col = when {
                count == 1 -> 0
                count == 2 -> if (index == 0) -1 else 1
                else -> index - (count / 2)
            }
            layout[node.id] = Pair(col, currentTier)
            assigned.add(node.id)
        }

        currentTier += 1
    }

    return layout
}
