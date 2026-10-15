package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.RoadmapFaqItem
import com.offline_First.data.staging.RoadmapProjectItem
import com.offline_First.data.staging.VisualRoadmapNode
import com.offline_First.ui.theme.*

// --- EDUNOVA ROADMAP UNIFIED COLOR HIERARCHY ---
val EduNovaSupportingBoxBg = Color(0xFFE8F5F1)
val EduNovaSupportingBoxBorder = Color(0xFF7CB8B0)
val EduNovaMilestoneBoxBg = Color(0xFF24777B)
val EduNovaMilestoneBoxBorder = Color(0xFF195558)
val EduNovaPrimaryConnectorColor = Color(0xFF24777B)
val EduNovaSecondaryConnectorColor = Color(0xFF4FA098)

/**
 * Reusable zoomable & pannable canvas box for EduNova roadmaps.
 * Features dedicated top toolbar controls (+, -, %, reset) that never overlap the diagram,
 * plus 2D touch gestures for smooth dragging and pinch-to-zoom.
 */
@Composable
fun EduNovaZoomableCanvasBox(
    modifier: Modifier = Modifier,
    initialScale: Float = 0.85f,
    viewportHeight: Int = 680,
    content: @Composable (scale: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(initialScale) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val minScale = 0.45f
    val maxScale = 2.2f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dedicated Top Controls Bar (Never overlaps the diagram nodes)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = EduNovaPrimaryConnectorColor
                        )
                        Text(
                            text = "Drag to explore • Pinch to zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Zoom in, zoom out, scale indicator, and reset view controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { scale = (scale - 0.15f).coerceIn(minScale, maxScale) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { scale = (scale + 0.15f).coerceIn(minScale, maxScale) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                scale = initialScale
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset View", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Scrollable/Pannable Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewportHeight.dp)
                    .clipToBounds()
                    .background(EduNovaBackground)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(minScale, maxScale)
                            val boundX = 650f * scale
                            val boundY = 2200f * scale
                            offsetX = (offsetX + pan.x).coerceIn(-boundX, boundX)
                            offsetY = (offsetY + pan.y).coerceIn(-boundY, boundY)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .padding(vertical = 16.dp)
                ) {
                    content(scale)
                }
            }
        }
    }
}

/**
 * Compact rectangular node component strictly styled according to EduNova's theme:
 * - Supporting topics: Light mint green background with soft teal border and dark ink text.
 * - Major milestones: Darker teal background with deep teal border and high-contrast white text.
 * - Selected: Deep teal focus border.
 * - Completed: Success tint and checkmark icon.
 */
@Composable
fun EduNovaRoadmapNodeBox(
    node: VisualRoadmapNode,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val borderColor = when {
        isSelected -> Color(0xFF104A4D)
        isCompleted -> EduNovaSuccess
        node.isMajor -> EduNovaMilestoneBoxBorder
        else -> EduNovaSupportingBoxBorder
    }

    val borderWidth = when {
        isSelected -> 2.2.dp
        node.isMajor -> 1.8.dp
        isCompleted -> 1.6.dp
        else -> 1.dp
    }

    val bgColor = when {
        isSelected -> if (node.isMajor) Color(0xFF1B6064) else Color(0xFFCEEEE6)
        isCompleted -> if (node.isMajor) Color(0xFF2A6D5E) else Color(0xFFD6F0E4)
        node.isMajor -> EduNovaMilestoneBoxBg
        else -> EduNovaSupportingBoxBg
    }

    val textColor = when {
        node.isMajor -> Color.White
        else -> EduNovaTextPrimary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (node.isMajor) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 5.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = if (node.isMajor) Color(0xFFB5E8D5) else EduNovaSuccess,
                    modifier = Modifier
                        .size(13.dp)
                        .padding(end = 4.dp)
                )
            }

            Text(
                text = node.title,
                style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 11.5.sp)
                else if (node.isMajor) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.5.sp)
                else MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                fontWeight = if (node.isMajor || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            node.badge?.let { badge ->
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (node.isMajor) Color.White.copy(alpha = 0.2f) else EduNovaAccent.copy(alpha = 0.35f),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (node.isMajor) Color.White else EduNovaSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reusable modal bottom sheet to inspect topic details, objectives, code examples,
 * toggle completion, and ask the AI Tutor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduNovaTopicDetailBottomSheet(
    node: VisualRoadmapNode,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onAskAiTutor: (String) -> Unit,
    onDismiss: () -> Unit,
    languageLabel: String = "Code Example"
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Section tag and Topic Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.section.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Explanation
            if (node.explanation.isNotBlank()) {
                Text(
                    text = node.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            // Learning Objectives
            if (node.objectives.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Key Learning Objectives:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    node.objectives.forEach { obj ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = obj,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Code Snippet Example
            if (node.codeSnippet.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = languageLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = node.codeSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Actions: Mark as Complete & Ask AI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onAskAiTutor("Explain ${node.title} in detail with real-world examples and best practices") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI Tutor", fontSize = 12.sp)
                }

                Button(
                    onClick = onToggleComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) EduNovaSuccess else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) "Completed" else "Mark Complete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Expandable FAQ row with smooth animation for EduNova roadmaps.
 */
@Composable
fun EduNovaFaqAccordionItem(faq: RoadmapFaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

/**
 * Project card for Roadmap Projects tab.
 */
@Composable
fun EduNovaProjectCard(
    project: RoadmapProjectItem,
    onAskAi: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (project.difficulty) {
                        "Beginner" -> EduNovaSuccess.copy(alpha = 0.15f)
                        "Intermediate" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> EduNovaAccent.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = project.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (project.difficulty) {
                            "Beginner" -> EduNovaSuccess
                            "Intermediate" -> MaterialTheme.colorScheme.primary
                            else -> EduNovaAccent
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Relevant Skills Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                project.relevantSkills.take(3).forEach { skill ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = skill,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "~${project.estimatedHours} hours",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { onAskAi("How do I build the project: ${project.title}?") }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask AI Tutor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- CONNECTOR DRAWING HELPERS ---

fun getNodeBounds(
    nodeCoords: Map<String, LayoutCoordinates>,
    root: LayoutCoordinates?,
    id: String
): Rect? {
    val r = root ?: return null
    if (!r.isAttached) return null
    val coords = nodeCoords[id] ?: return null
    if (!coords.isAttached) return null
    val pos = r.localPositionOf(coords, Offset.Zero)
    return Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
}

fun DrawScope.drawStraightConnector(
    start: Offset,
    end: Offset,
    isDashed: Boolean = false,
    color: Color = EduNovaPrimaryConnectorColor,
    strokeWidthPx: Float = 2.5f * density,
    dashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
) {
    if (isDashed) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidthPx,
            pathEffect = dashEffect
        )
    } else {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidthPx
        )
    }
}

fun DrawScope.drawOrthogonalConnector(
    start: Offset,
    end: Offset,
    isDashed: Boolean = false,
    color: Color = EduNovaPrimaryConnectorColor,
    strokeWidthPx: Float = 2.5f * density,
    dashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val midY = (start.y + end.y) / 2f
        lineTo(start.x, midY)
        lineTo(end.x, midY)
        lineTo(end.x, end.y)
    }
    if (isDashed) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidthPx, pathEffect = dashEffect)
        )
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidthPx)
        )
    }
}

fun DrawScope.drawFanningDottedConnector(
    start: Offset,
    end: Offset,
    isLeft: Boolean,
    color: Color = EduNovaSecondaryConnectorColor,
    strokeWidthPx: Float = 1.8f * density,
    dashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val controlOffsetX = 28f * density * (if (isLeft) -1f else 1f)
        cubicTo(
            start.x + controlOffsetX, start.y,
            end.x - controlOffsetX, end.y,
            end.x, end.y
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, pathEffect = dashEffect)
    )
}
