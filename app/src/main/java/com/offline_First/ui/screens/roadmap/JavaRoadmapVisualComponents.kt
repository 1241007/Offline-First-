package com.offline_First.ui.screens.roadmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.JavaRoadmapStagingData
import com.offline_First.data.staging.VisualRoadmapNode
import com.offline_First.ui.theme.*

// --- EDUNOVA ROADMAP COLOR HIERARCHY ---
private val SupportingBoxBg = Color(0xFFE8F5F1)
private val SupportingBoxBorder = Color(0xFF7CB8B0)
private val MilestoneBoxBg = Color(0xFF24777B)
private val MilestoneBoxBorder = Color(0xFF195558)
private val PrimaryConnectorColor = Color(0xFF24777B)
private val SecondaryConnectorColor = Color(0xFF4FA098)

/**
 * Zoomable container providing pinch-to-zoom, two-dimensional panning,
 * dedicated non-overlapping zoom controls, and a reset view action for the Java Roadmap.
 */
@Composable
fun JavaZoomableCanvasBox(
    modifier: Modifier = Modifier,
    content: @Composable (scale: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(0.85f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val minScale = 0.5f
    val maxScale = 2.2f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dedicated Top Controls Bar (Never overlaps the diagram nodes!)
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
                            tint = PrimaryConnectorColor
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

                        VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 4.dp))

                        IconButton(
                            onClick = {
                                scale = 0.85f
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
                    .height(680.dp)
                    .clipToBounds()
                    .background(EduNovaBackground)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(minScale, maxScale)
                            val boundX = 500f * scale
                            val boundY = 1600f * scale
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
 * Compact node component strictly styled according to EduNova's color hierarchy:
 * - Supporting topics: Light mint green background with soft teal border and dark ink text.
 * - Major milestones: Darker teal background with deep teal border and high-contrast white text.
 * - Selected: Stronger teal/green border.
 * - Completed: Success tint and checkmark icon.
 */
@Composable
fun JavaRoadmapNodeBox(
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
        node.isMajor -> MilestoneBoxBorder
        else -> SupportingBoxBorder
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
        node.isMajor -> MilestoneBoxBg
        else -> SupportingBoxBg
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
                    modifier = Modifier.size(13.dp).padding(end = 4.dp)
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
 * Connected visual Java Developer Roadmap Diagram matching Roadmap.sh flow.
 * All connections use STRICT ORTHOGONAL (horizontal and vertical) routing.
 * No diagonal slashes. Every connection connects directly to actual node bounds.
 */
@Composable
fun JavaVisualRoadmapDiagram(
    selectedNodeId: String?,
    completedTopicIds: Set<String>,
    onSelectNode: (VisualRoadmapNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryConnector = PrimaryConnectorColor
    val secondaryConnector = SecondaryConnectorColor
    val secondaryDashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)

    // Optimized canvas width for mobile viewport fit
    val canvasWidth = 640.dp

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val nodeCoords = remember { mutableStateMapOf<String, LayoutCoordinates>() }

    Box(
        modifier = modifier
            .width(canvasWidth)
            .padding(horizontal = 12.dp)
            .onGloballyPositioned { rootCoordinates = it }
    ) {
        // --- 1. RECTILINEAR CONNECTOR CANVAS DRAWN BEHIND NODES ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val root = rootCoordinates ?: return@Canvas
            if (!root.isAttached) return@Canvas

            fun getBounds(id: String): Rect? {
                val coords = nodeCoords[id] ?: return null
                if (!coords.isAttached) return null
                val pos = root.localPositionOf(coords, Offset.Zero)
                return Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
            }

            fun drawStraightLine(start: Offset, end: Offset, isDashed: Boolean = false) {
                if (isDashed) {
                    drawLine(
                        color = secondaryConnector,
                        start = start,
                        end = end,
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = secondaryDashEffect
                    )
                } else {
                    drawLine(
                        color = primaryConnector,
                        start = start,
                        end = end,
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            }

            // Strictly orthogonal (rectilinear) path: only horizontal and vertical segments!
            fun drawOrthogonalPath(start: Offset, end: Offset, isDashed: Boolean = false) {
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
                        color = secondaryConnector,
                        style = Stroke(width = 1.8.dp.toPx(), pathEffect = secondaryDashEffect)
                    )
                } else {
                    drawPath(
                        path = path,
                        color = primaryConnector,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            fun drawFanningDottedCurve(start: Offset, end: Offset, isLeft: Boolean) {
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    val controlOffsetX = 28.dp.toPx() * (if (isLeft) -1f else 1f)
                    cubicTo(
                        start.x + controlOffsetX, start.y,
                        end.x - controlOffsetX, end.y,
                        end.x, end.y
                    )
                }
                drawPath(
                    path = path,
                    color = secondaryConnector,
                    style = Stroke(width = 1.8.dp.toPx(), pathEffect = secondaryDashEffect)
                )
            }

            // --- A. Top Incoming Guide to Java ---
            getBounds(JavaRoadmapStagingData.NODE_JAVA.id)?.let { rJava ->
                drawStraightLine(
                    start = Offset(rJava.center.x, rJava.top - 18.dp.toPx()),
                    end = Offset(rJava.center.x, rJava.top),
                    isDashed = true
                )
            }

            // --- B. Java -> Learn the Basics (Straight Vertical) ---
            val rJava = getBounds(JavaRoadmapStagingData.NODE_JAVA.id)
            val rBasics = getBounds(JavaRoadmapStagingData.NODE_LEARN_BASICS.id)
            if (rJava != null && rBasics != null) {
                drawStraightLine(
                    start = Offset(rJava.center.x, rJava.bottom),
                    end = Offset(rJava.center.x, rBasics.top)
                )
            }

            // --- C. Learn the Basics -> Left Fundamentals Wing ---
            if (rBasics != null) {
                val leftStart = Offset(rBasics.left, rBasics.center.y)
                listOf(
                    JavaRoadmapStagingData.NODE_BASIC_SYNTAX.id,
                    JavaRoadmapStagingData.NODE_LIFECYCLE.id,
                    JavaRoadmapStagingData.NODE_DATA_TYPES.id,
                    JavaRoadmapStagingData.NODE_VARIABLES_SCOPES.id,
                    JavaRoadmapStagingData.NODE_TYPE_CASTING.id
                ).forEach { id ->
                    getBounds(id)?.let { rNode ->
                        drawFanningDottedCurve(start = leftStart, end = Offset(rNode.right, rNode.center.y), isLeft = true)
                    }
                }

                // --- D. Learn the Basics -> Right Fundamentals Wing ---
                val rightStart = Offset(rBasics.right, rBasics.center.y)
                listOf(
                    JavaRoadmapStagingData.NODE_STRINGS_METHODS.id,
                    JavaRoadmapStagingData.NODE_MATH_OPERATIONS.id,
                    JavaRoadmapStagingData.NODE_ARRAYS.id,
                    JavaRoadmapStagingData.NODE_CONDITIONALS.id,
                    JavaRoadmapStagingData.NODE_LOOPS.id,
                    JavaRoadmapStagingData.NODE_BASICS_OF_OOP_START.id
                ).forEach { id ->
                    getBounds(id)?.let { rNode ->
                        drawFanningDottedCurve(start = rightStart, end = Offset(rNode.left, rNode.center.y), isLeft = false)
                    }
                }
            }

            // --- E. Learn the Basics -> Object Oriented Programming (Straight Vertical) ---
            val rOop = getBounds(JavaRoadmapStagingData.NODE_OOP.id)
            if (rBasics != null && rOop != null) {
                drawStraightLine(
                    start = Offset(rBasics.center.x, rBasics.bottom),
                    end = Offset(rBasics.center.x, rOop.top)
                )
            }

            // --- F. Object Oriented Programming Bus Bar (Rectilinear) ---
            val rBasicsOopContainer = getBounds("container_basics_of_oop")
            val rMoreOopContainer = getBounds("container_more_about_oop")
            val rExceptionHandling = getBounds(JavaRoadmapStagingData.NODE_EXCEPTION_HANDLING.id)
            if (rOop != null && rBasicsOopContainer != null && rMoreOopContainer != null && rExceptionHandling != null) {
                val busY = (rOop.bottom + minOf(rBasicsOopContainer.top, rMoreOopContainer.top, rExceptionHandling.top)) / 2f
                val leftX = rBasicsOopContainer.center.x
                val centerX = rOop.center.x
                val rightX = rExceptionHandling.center.x

                // Vertical drop from OOP to horizontal bus
                drawStraightLine(Offset(centerX, rOop.bottom), Offset(centerX, busY))
                // Horizontal crossbar across the 3 branches
                drawStraightLine(Offset(leftX, busY), Offset(rightX, busY))
                // Vertical drops into each of the 3 containers/nodes
                drawStraightLine(Offset(leftX, busY), Offset(leftX, rBasicsOopContainer.top))
                drawStraightLine(Offset(centerX, busY), Offset(centerX, rMoreOopContainer.top))
                drawStraightLine(Offset(rightX, busY), Offset(rightX, rExceptionHandling.top))
            }

            // --- G. Right Spine: Straight Vertical Language Features Progression ---
            val rightProgression = listOf(
                JavaRoadmapStagingData.NODE_EXCEPTION_HANDLING.id,
                JavaRoadmapStagingData.NODE_LAMBDAS.id,
                JavaRoadmapStagingData.NODE_ANNOTATIONS.id,
                JavaRoadmapStagingData.NODE_MODULES.id,
                JavaRoadmapStagingData.NODE_OPTIONALS.id,
                JavaRoadmapStagingData.NODE_GENERICS.id,
                JavaRoadmapStagingData.NODE_REFLECTION.id,
                JavaRoadmapStagingData.NODE_DEPENDENCY_INJECTION.id,
                JavaRoadmapStagingData.NODE_IO_OPERATIONS.id,
                JavaRoadmapStagingData.NODE_FILE_OPERATIONS.id
            )
            for (i in 0 until rightProgression.lastIndex) {
                val rA = getBounds(rightProgression[i])
                val rB = getBounds(rightProgression[i + 1])
                if (rA != null && rB != null) {
                    drawStraightLine(Offset(rA.center.x, rA.bottom), Offset(rA.center.x, rB.top))
                }
            }

            // --- H. Left Spine: Straight Vertical Cryptography -> Regex ---
            val rCrypto = getBounds(JavaRoadmapStagingData.NODE_CRYPTOGRAPHY.id)
            if (rBasicsOopContainer != null && rCrypto != null) {
                drawStraightLine(
                    Offset(rBasicsOopContainer.center.x, rBasicsOopContainer.bottom),
                    Offset(rBasicsOopContainer.center.x, rCrypto.top)
                )
            }
            val leftProgression = listOf(
                JavaRoadmapStagingData.NODE_CRYPTOGRAPHY.id,
                JavaRoadmapStagingData.NODE_DATE_TIME.id,
                JavaRoadmapStagingData.NODE_NETWORKING.id,
                JavaRoadmapStagingData.NODE_REGEX.id
            )
            for (i in 0 until leftProgression.lastIndex) {
                val rA = getBounds(leftProgression[i])
                val rB = getBounds(leftProgression[i + 1])
                if (rA != null && rB != null) {
                    drawStraightLine(Offset(rA.center.x, rA.bottom), Offset(rA.center.x, rB.top))
                }
            }
            val rRegex = getBounds(JavaRoadmapStagingData.NODE_REGEX.id)
            val rFunctionalContainer = getBounds("container_functional_programming")
            if (rRegex != null && rFunctionalContainer != null) {
                drawStraightLine(
                    Offset(rRegex.center.x, rRegex.bottom),
                    Offset(rRegex.center.x, rFunctionalContainer.top)
                )
            }

            // --- I. Concurrency & Collections (Orthogonal Connections) ---
            val rThreadPools = getBounds(JavaRoadmapStagingData.NODE_THREAD_POOLS.id)
            val rConcurrency = getBounds(JavaRoadmapStagingData.NODE_CONCURRENCY.id)
            val rCollectionsContainer = getBounds("container_collections")
            val rDi = getBounds(JavaRoadmapStagingData.NODE_DEPENDENCY_INJECTION.id)

            if (rThreadPools != null && rConcurrency != null) {
                drawStraightLine(
                    Offset(rConcurrency.center.x, rThreadPools.bottom),
                    Offset(rConcurrency.center.x, rConcurrency.top),
                    isDashed = true
                )
            }

            // Regex horizontally into Concurrency
            if (rRegex != null && rConcurrency != null) {
                drawStraightLine(
                    Offset(rRegex.right, rConcurrency.center.y),
                    Offset(rConcurrency.left, rConcurrency.center.y)
                )
            }

            // Concurrency horizontally into Collections
            if (rConcurrency != null && rCollectionsContainer != null) {
                drawStraightLine(
                    Offset(rConcurrency.right, rConcurrency.center.y),
                    Offset(rCollectionsContainer.left, rConcurrency.center.y)
                )
            }

            // Collections horizontally into Right Spine (DI)
            if (rCollectionsContainer != null && rDi != null) {
                drawStraightLine(
                    Offset(rCollectionsContainer.right, rCollectionsContainer.center.y),
                    Offset(rDi.center.x, rCollectionsContainer.center.y)
                )
            }

            // --- J. Build Tools, Web Frameworks, Database Access (Horizontal Bus) ---
            val rBuildTools = getBounds(JavaRoadmapStagingData.NODE_BUILD_TOOLS.id)
            val rWebFrameworks = getBounds(JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS.id)
            val rDatabaseAccess = getBounds(JavaRoadmapStagingData.NODE_DATABASE_ACCESS.id)

            if (rFunctionalContainer != null && rBuildTools != null) {
                drawStraightLine(
                    Offset(rFunctionalContainer.right, rBuildTools.center.y),
                    Offset(rBuildTools.left, rBuildTools.center.y)
                )
            }

            // Flat horizontal bus across the 3 milestones
            if (rBuildTools != null && rWebFrameworks != null) {
                drawStraightLine(
                    Offset(rBuildTools.right, rWebFrameworks.center.y),
                    Offset(rWebFrameworks.left, rWebFrameworks.center.y)
                )
            }
            if (rWebFrameworks != null && rDatabaseAccess != null) {
                drawStraightLine(
                    Offset(rWebFrameworks.right, rWebFrameworks.center.y),
                    Offset(rDatabaseAccess.left, rWebFrameworks.center.y)
                )
            }

            // Drops from stacks into milestones
            val rBazel = getBounds(JavaRoadmapStagingData.NODE_BAZEL.id)
            if (rBazel != null && rBuildTools != null) {
                drawStraightLine(Offset(rBuildTools.center.x, rBazel.bottom), Offset(rBuildTools.center.x, rBuildTools.top), isDashed = true)
            }

            val rPlay = getBounds(JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK.id)
            if (rPlay != null && rWebFrameworks != null) {
                drawStraightLine(Offset(rWebFrameworks.center.x, rPlay.bottom), Offset(rWebFrameworks.center.x, rWebFrameworks.top))
            }

            val rSpringData = getBounds(JavaRoadmapStagingData.NODE_SPRING_DATA_JPA.id)
            if (rSpringData != null && rDatabaseAccess != null) {
                drawStraightLine(Offset(rDatabaseAccess.center.x, rSpringData.bottom), Offset(rDatabaseAccess.center.x, rDatabaseAccess.top))
            }

            // --- K. Documentation, Logging, Testing & Backend Roadmap (Strictly Straight!) ---
            val rJavadoc = getBounds(JavaRoadmapStagingData.NODE_JAVADOC.id)
            val rDocumentation = getBounds(JavaRoadmapStagingData.NODE_DOCUMENTATION.id)
            val rLogging = getBounds(JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS.id)
            val rLogback = getBounds(JavaRoadmapStagingData.NODE_LOGBACK.id)
            val rTesting = getBounds(JavaRoadmapStagingData.NODE_TESTING.id)
            val rTestingContainer = getBounds("container_testing")
            val rBackendCard = getBounds("container_backend_roadmap")

            // Web Frameworks STRAIGHT DOWN to Documentation
            if (rWebFrameworks != null && rDocumentation != null) {
                drawStraightLine(
                    Offset(rDocumentation.center.x, rWebFrameworks.bottom),
                    Offset(rDocumentation.center.x, rDocumentation.top)
                )
            }

            // Javadoc dotted to Documentation (Horizontal)
            if (rJavadoc != null && rDocumentation != null) {
                drawStraightLine(
                    Offset(rJavadoc.right, rDocumentation.center.y),
                    Offset(rDocumentation.left, rDocumentation.center.y),
                    isDashed = true
                )
            }

            // Documentation solid to Logging Frameworks (Horizontal)
            if (rDocumentation != null && rLogging != null) {
                drawStraightLine(
                    Offset(rDocumentation.right, rDocumentation.center.y),
                    Offset(rLogging.left, rDocumentation.center.y)
                )
            }

            // Logging Frameworks dotted down to logging grid (Vertical)
            if (rLogging != null && rLogback != null) {
                drawStraightLine(
                    Offset(rLogging.center.x, rLogging.bottom),
                    Offset(rLogging.center.x, rLogback.top),
                    isDashed = true
                )
            }

            // Documentation STRAIGHT DOWN to Testing (Straight Vertical!)
            if (rDocumentation != null && rTesting != null) {
                drawStraightLine(
                    Offset(rTesting.center.x, rDocumentation.bottom),
                    Offset(rTesting.center.x, rTesting.top)
                )
            }

            // Testing Container dotted to Testing (Horizontal)
            if (rTesting != null && rTestingContainer != null) {
                drawStraightLine(
                    Offset(rTestingContainer.right, rTesting.center.y),
                    Offset(rTesting.left, rTesting.center.y),
                    isDashed = true
                )
            }

            // Testing STRAIGHT DOWN to Backend Roadmap Card (Straight Vertical!)
            if (rTesting != null && rBackendCard != null) {
                drawStraightLine(
                    Offset(rTesting.center.x, rTesting.bottom),
                    Offset(rTesting.center.x, rBackendCard.top)
                )
                // Bottom terminating dotted guide line
                drawStraightLine(
                    Offset(rBackendCard.center.x, rBackendCard.bottom),
                    Offset(rBackendCard.center.x, rBackendCard.bottom + 22.dp.toPx()),
                    isDashed = true
                )
            }
        }

        // --- 2. FOREGROUND NODES & BALANCED LAYOUT ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- TOP CARDS (Roadmap note & roadmap.sh) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaSurface,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.width(170.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Java specific roadmap skipping backend details.",
                            fontSize = 8.5.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SupportingBoxBg,
                            border = BorderStroke(1.dp, SupportingBoxBorder)
                        ) {
                            Text(
                                text = "Visit Backend Roadmap",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MilestoneBoxBg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduNovaSurface,
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.width(170.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Find detailed versions of other roadmaps.",
                            fontSize = 8.5.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SupportingBoxBg,
                            border = BorderStroke(1.dp, SupportingBoxBorder)
                        ) {
                            Text(
                                text = "roadmap.sh",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MilestoneBoxBg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // --- JAVA ROOT NODE ---
            JavaRoadmapNodeBox(
                node = JavaRoadmapStagingData.NODE_JAVA,
                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_JAVA.id,
                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_JAVA.id),
                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_JAVA) },
                modifier = Modifier
                    .width(150.dp)
                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_JAVA.id] = it }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- FUNDAMENTALS SECTION (Left Wing | Learn the Basics | Right Wing) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Wing: Basic Syntax, Lifecycle, Data Types, Variables & Scopes, Type Casting
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val leftNodes = listOf(
                        JavaRoadmapStagingData.NODE_BASIC_SYNTAX,
                        JavaRoadmapStagingData.NODE_LIFECYCLE,
                        JavaRoadmapStagingData.NODE_DATA_TYPES,
                        JavaRoadmapStagingData.NODE_VARIABLES_SCOPES,
                        JavaRoadmapStagingData.NODE_TYPE_CASTING
                    )
                    leftNodes.forEach { node ->
                        JavaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it },
                            compact = true
                        )
                    }
                }

                Spacer(modifier = Modifier.width(48.dp))

                // Center Milestone: Learn the Basics
                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_LEARN_BASICS,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_LEARN_BASICS.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_LEARN_BASICS.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_LEARN_BASICS) },
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_LEARN_BASICS.id] = it }
                )

                Spacer(modifier = Modifier.width(48.dp))

                // Right Wing: Strings, Math, Arrays, Conditionals, Loops, Basics of OOP
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val rightNodes = listOf(
                        JavaRoadmapStagingData.NODE_STRINGS_METHODS,
                        JavaRoadmapStagingData.NODE_MATH_OPERATIONS,
                        JavaRoadmapStagingData.NODE_ARRAYS,
                        JavaRoadmapStagingData.NODE_CONDITIONALS,
                        JavaRoadmapStagingData.NODE_LOOPS,
                        JavaRoadmapStagingData.NODE_BASICS_OF_OOP_START
                    )
                    rightNodes.forEach { node ->
                        JavaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it },
                            compact = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- OBJECT ORIENTED PROGRAMMING MILESTONE ---
            JavaRoadmapNodeBox(
                node = JavaRoadmapStagingData.NODE_OOP,
                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_OOP.id,
                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_OOP.id),
                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_OOP) },
                modifier = Modifier
                    .width(240.dp)
                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_OOP.id] = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- OOP SUB-BRANCHES: Basics of OOP | More about OOP | Language Features ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Basics of OOP Container
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, SupportingBoxBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords["container_basics_of_oop"] = it }
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Basics of OOP",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val oopLeft = listOf(
                            JavaRoadmapStagingData.NODE_CLASSES_OBJECTS,
                            JavaRoadmapStagingData.NODE_ATTRIBUTES_METHODS,
                            JavaRoadmapStagingData.NODE_ACCESS_SPECIFIERS,
                            JavaRoadmapStagingData.NODE_STATIC_KEYWORD,
                            JavaRoadmapStagingData.NODE_FINAL_KEYWORD,
                            JavaRoadmapStagingData.NODE_NESTED_CLASSES,
                            JavaRoadmapStagingData.NODE_PACKAGES
                        )
                        oopLeft.forEach { n ->
                            JavaRoadmapNodeBox(
                                node = n,
                                isSelected = selectedNodeId == n.id,
                                isCompleted = completedTopicIds.contains(n.id),
                                onClick = { onSelectNode(n) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[n.id] = it },
                                compact = true
                            )
                        }
                    }
                }

                // Center: More about OOP Container
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, SupportingBoxBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(290.dp)
                        .onGloballyPositioned { nodeCoords["container_more_about_oop"] = it }
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "More about OOP",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                JavaRoadmapStagingData.NODE_OBJECT_LIFECYCLE,
                                JavaRoadmapStagingData.NODE_INHERITANCE,
                                JavaRoadmapStagingData.NODE_ABSTRACTION
                            ).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                JavaRoadmapStagingData.NODE_METHOD_CHAINING,
                                JavaRoadmapStagingData.NODE_ENCAPSULATION,
                                JavaRoadmapStagingData.NODE_INTERFACES
                            ).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                JavaRoadmapStagingData.NODE_ENUMS,
                                JavaRoadmapStagingData.NODE_RECORD,
                                JavaRoadmapStagingData.NODE_METHOD_OVERLOAD_OVERRIDE
                            ).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                JavaRoadmapStagingData.NODE_INITIALIZER_BLOCK,
                                JavaRoadmapStagingData.NODE_STATIC_DYNAMIC_BINDING
                            ).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        JavaRoadmapNodeBox(
                            node = JavaRoadmapStagingData.NODE_PASS_VALUE_REFERENCE,
                            isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_PASS_VALUE_REFERENCE.id,
                            isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_PASS_VALUE_REFERENCE.id),
                            onClick = { onSelectNode(JavaRoadmapStagingData.NODE_PASS_VALUE_REFERENCE) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_PASS_VALUE_REFERENCE.id] = it },
                            compact = true
                        )
                    }
                }

                // Right: Language Features Progression (Aligned directly above Dependency Injection)
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_EXCEPTION_HANDLING,
                        JavaRoadmapStagingData.NODE_LAMBDAS,
                        JavaRoadmapStagingData.NODE_ANNOTATIONS,
                        JavaRoadmapStagingData.NODE_MODULES,
                        JavaRoadmapStagingData.NODE_OPTIONALS,
                        JavaRoadmapStagingData.NODE_GENERICS,
                        JavaRoadmapStagingData.NODE_REFLECTION
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- CONCURRENCY, COLLECTIONS & CORE APIS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Cryptography, Date/Time, Networking, Regex
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_CRYPTOGRAPHY,
                        JavaRoadmapStagingData.NODE_DATE_TIME,
                        JavaRoadmapStagingData.NODE_NETWORKING,
                        JavaRoadmapStagingData.NODE_REGEX
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }
                }

                // Center-Left: Concurrency Supporting Stack & Concurrency Milestone
                Column(
                    modifier = Modifier.width(140.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_VOLATILE,
                        JavaRoadmapStagingData.NODE_JVM_MEMORY,
                        JavaRoadmapStagingData.NODE_VIRTUAL_THREADS,
                        JavaRoadmapStagingData.NODE_THREADS,
                        JavaRoadmapStagingData.NODE_SYNCHRONIZATION,
                        JavaRoadmapStagingData.NODE_EXECUTORS,
                        JavaRoadmapStagingData.NODE_THREAD_POOLS
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    JavaRoadmapNodeBox(
                        node = JavaRoadmapStagingData.NODE_CONCURRENCY,
                        isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_CONCURRENCY.id,
                        isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_CONCURRENCY.id),
                        onClick = { onSelectNode(JavaRoadmapStagingData.NODE_CONCURRENCY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_CONCURRENCY.id] = it }
                    )
                }

                // Center-Right: Collections Container Box
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, SupportingBoxBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords["container_collections"] = it }
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Collections",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        JavaRoadmapNodeBox(
                            node = JavaRoadmapStagingData.NODE_COLLECTIONS_FRAMEWORK,
                            isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_COLLECTIONS_FRAMEWORK.id,
                            isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_COLLECTIONS_FRAMEWORK.id),
                            onClick = { onSelectNode(JavaRoadmapStagingData.NODE_COLLECTIONS_FRAMEWORK) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_COLLECTIONS_FRAMEWORK.id] = it },
                            compact = true
                        )
                        JavaRoadmapNodeBox(
                            node = JavaRoadmapStagingData.NODE_ARRAY_VS_ARRAYLIST,
                            isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_ARRAY_VS_ARRAYLIST.id,
                            isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_ARRAY_VS_ARRAYLIST.id),
                            onClick = { onSelectNode(JavaRoadmapStagingData.NODE_ARRAY_VS_ARRAYLIST) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_ARRAY_VS_ARRAYLIST.id] = it },
                            compact = true
                        )
                        JavaRoadmapNodeBox(
                            node = JavaRoadmapStagingData.NODE_LINKED_LIST,
                            isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_LINKED_LIST.id,
                            isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_LINKED_LIST.id),
                            onClick = { onSelectNode(JavaRoadmapStagingData.NODE_LINKED_LIST) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_LINKED_LIST.id] = it },
                            compact = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(JavaRoadmapStagingData.NODE_SET, JavaRoadmapStagingData.NODE_MAP).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(JavaRoadmapStagingData.NODE_QUEUE, JavaRoadmapStagingData.NODE_DEQUE).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(JavaRoadmapStagingData.NODE_STACK, JavaRoadmapStagingData.NODE_ITERATOR).forEach { n ->
                                JavaRoadmapNodeBox(
                                    node = n,
                                    isSelected = selectedNodeId == n.id,
                                    isCompleted = completedTopicIds.contains(n.id),
                                    onClick = { onSelectNode(n) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[n.id] = it },
                                    compact = true
                                )
                            }
                        }
                        JavaRoadmapNodeBox(
                            node = JavaRoadmapStagingData.NODE_GENERIC_COLLECTIONS,
                            isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_GENERIC_COLLECTIONS.id,
                            isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_GENERIC_COLLECTIONS.id),
                            onClick = { onSelectNode(JavaRoadmapStagingData.NODE_GENERIC_COLLECTIONS) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_GENERIC_COLLECTIONS.id] = it },
                            compact = true
                        )
                    }
                }

                // Far-Right: Dependency Injection, I/O Operations, File Operations (Direct Continuation of Spine)
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_DEPENDENCY_INJECTION,
                        JavaRoadmapStagingData.NODE_IO_OPERATIONS,
                        JavaRoadmapStagingData.NODE_FILE_OPERATIONS
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- FUNCTIONAL, BUILD TOOLS, FRAMEWORKS, DATABASE ---
            // A. Top supporting stacks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Functional Programming Container
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, SupportingBoxBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords["container_functional_programming"] = it }
                ) {
                    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "Functional Programming",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        listOf(
                            JavaRoadmapStagingData.NODE_HIGH_ORDER_FUNCTIONS,
                            JavaRoadmapStagingData.NODE_FUNCTIONAL_INTERFACES,
                            JavaRoadmapStagingData.NODE_FUNCTIONAL_COMPOSITION,
                            JavaRoadmapStagingData.NODE_STREAM_API
                        ).forEach { n ->
                            JavaRoadmapNodeBox(
                                node = n,
                                isSelected = selectedNodeId == n.id,
                                isCompleted = completedTopicIds.contains(n.id),
                                onClick = { onSelectNode(n) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[n.id] = it },
                                compact = true
                            )
                        }
                    }
                }

                // Center-Left Stack: Maven, Gradle, Bazel
                Column(
                    modifier = Modifier.width(140.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_MAVEN,
                        JavaRoadmapStagingData.NODE_GRADLE,
                        JavaRoadmapStagingData.NODE_BAZEL
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }
                }

                // Center Stack: Spring Boot, Quarkus/Javalin, Play Framework
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Spring Boot is recommended ▾",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MilestoneBoxBg
                    )

                    JavaRoadmapNodeBox(
                        node = JavaRoadmapStagingData.NODE_SPRING_BOOT,
                        isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_SPRING_BOOT.id,
                        isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_SPRING_BOOT.id),
                        onClick = { onSelectNode(JavaRoadmapStagingData.NODE_SPRING_BOOT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_SPRING_BOOT.id] = it },
                        compact = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(JavaRoadmapStagingData.NODE_QUARKUS, JavaRoadmapStagingData.NODE_JAVALIN).forEach { n ->
                            JavaRoadmapNodeBox(
                                node = n,
                                isSelected = selectedNodeId == n.id,
                                isCompleted = completedTopicIds.contains(n.id),
                                onClick = { onSelectNode(n) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[n.id] = it },
                                compact = true
                            )
                        }
                    }

                    JavaRoadmapNodeBox(
                        node = JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK,
                        isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK.id,
                        isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK.id),
                        onClick = { onSelectNode(JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_PLAY_FRAMEWORK.id] = it },
                        compact = true
                    )
                }

                // Right Stack: Database Access topics
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    listOf(
                        JavaRoadmapStagingData.NODE_JDBC,
                        JavaRoadmapStagingData.NODE_JPA,
                        JavaRoadmapStagingData.NODE_EBEAN,
                        JavaRoadmapStagingData.NODE_HIBERNATE,
                        JavaRoadmapStagingData.NODE_SPRING_DATA_JPA
                    ).forEach { n ->
                        JavaRoadmapNodeBox(
                            node = n,
                            isSelected = selectedNodeId == n.id,
                            isCompleted = completedTopicIds.contains(n.id),
                            onClick = { onSelectNode(n) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[n.id] = it },
                            compact = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // B. Single Flat Horizontal Milestone Row (Build Tools ── Web Frameworks ── Database Access)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(155.dp)) // Aligns under Functional Programming

                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_BUILD_TOOLS,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_BUILD_TOOLS.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_BUILD_TOOLS.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_BUILD_TOOLS) },
                    modifier = Modifier
                        .width(140.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_BUILD_TOOLS.id] = it }
                )

                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_WEB_FRAMEWORKS.id] = it }
                )

                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_DATABASE_ACCESS,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_DATABASE_ACCESS.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_DATABASE_ACCESS.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_DATABASE_ACCESS) },
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_DATABASE_ACCESS.id] = it }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- DOCUMENTATION & LOGGING (Directly under Web Frameworks on Centerline) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Javadoc (on the left)
                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_JAVADOC,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_JAVADOC.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_JAVADOC.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_JAVADOC) },
                    modifier = Modifier
                        .width(85.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_JAVADOC.id] = it },
                    compact = true
                )

                Spacer(modifier = Modifier.width(36.dp))

                // Documentation Milestone (Centered directly beneath Web Frameworks!)
                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_DOCUMENTATION,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_DOCUMENTATION.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_DOCUMENTATION.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_DOCUMENTATION) },
                    modifier = Modifier
                        .width(150.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_DOCUMENTATION.id] = it }
                )

                Spacer(modifier = Modifier.width(36.dp))

                // Logging Frameworks Milestone (on the right)
                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS) },
                    modifier = Modifier
                        .width(155.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_LOGGING_FRAMEWORKS.id] = it }
                )
            }

            // Logging Stack (under Logging Frameworks on the right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(JavaRoadmapStagingData.NODE_LOGBACK, JavaRoadmapStagingData.NODE_LOG4J2).forEach { n ->
                            JavaRoadmapNodeBox(
                                node = n,
                                isSelected = selectedNodeId == n.id,
                                isCompleted = completedTopicIds.contains(n.id),
                                onClick = { onSelectNode(n) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[n.id] = it },
                                compact = true
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(JavaRoadmapStagingData.NODE_SLF4J, JavaRoadmapStagingData.NODE_TINYLOG).forEach { n ->
                            JavaRoadmapNodeBox(
                                node = n,
                                isSelected = selectedNodeId == n.id,
                                isCompleted = completedTopicIds.contains(n.id),
                                onClick = { onSelectNode(n) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[n.id] = it },
                                compact = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- TESTING (Testing Milestone Centered Directly Under Documentation!) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Testing Categories Container Box (on the left)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, SupportingBoxBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(360.dp)
                        .onGloballyPositioned { nodeCoords["container_testing"] = it }
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_JUNIT,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_JUNIT.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_JUNIT.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_JUNIT) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_JUNIT.id] = it },
                                compact = true
                            )
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_TESTNG,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_TESTNG.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_TESTNG.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_TESTNG) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_TESTNG.id] = it },
                                compact = true
                            )
                            Text(
                                text = "Unit Testing",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaTextSecondary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_REST_ASSURED,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_REST_ASSURED.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_REST_ASSURED.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_REST_ASSURED) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_REST_ASSURED.id] = it },
                                compact = true
                            )
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_JMETER,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_JMETER.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_JMETER.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_JMETER) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_JMETER.id] = it },
                                compact = true
                            )
                            Text(
                                text = "Integration Testing",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaTextSecondary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_BEHAVIOR_TESTING,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_BEHAVIOR_TESTING.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_BEHAVIOR_TESTING.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_BEHAVIOR_TESTING) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_BEHAVIOR_TESTING.id] = it },
                                compact = true
                            )
                            Text(
                                text = "Behavior Testing",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaTextSecondary
                            )
                            JavaRoadmapNodeBox(
                                node = JavaRoadmapStagingData.NODE_MOCKING_MOCKITO,
                                isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_MOCKING_MOCKITO.id,
                                isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_MOCKING_MOCKITO.id),
                                onClick = { onSelectNode(JavaRoadmapStagingData.NODE_MOCKING_MOCKITO) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_MOCKING_MOCKITO.id] = it },
                                compact = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(36.dp))

                // Major Milestone: Testing (Straight on the Center Spine!)
                JavaRoadmapNodeBox(
                    node = JavaRoadmapStagingData.NODE_TESTING,
                    isSelected = selectedNodeId == JavaRoadmapStagingData.NODE_TESTING.id,
                    isCompleted = completedTopicIds.contains(JavaRoadmapStagingData.NODE_TESTING.id),
                    onClick = { onSelectNode(JavaRoadmapStagingData.NODE_TESTING) },
                    modifier = Modifier
                        .width(150.dp)
                        .onGloballyPositioned { nodeCoords[JavaRoadmapStagingData.NODE_TESTING.id] = it }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- BOTTOM RECOMMENDATION: BACKEND ROADMAP (Straight on the Center Spine!) ---
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduNovaSurface,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .width(300.dp)
                    .onGloballyPositioned { nodeCoords["container_backend_roadmap"] = it }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Visit Backend path and see what you missed",
                        style = MaterialTheme.typography.bodySmall,
                        color = EduNovaTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { /* Navigate to Backend Roadmap */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MilestoneBoxBg)
                    ) {
                        Text(
                            text = "Backend Roadmap",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
