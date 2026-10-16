package com.offline_First.ui.screens.roadmap

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.data.local.WebDevRoadmapDataProvider
import com.offline_First.domain.model.*
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSuccess
import kotlinx.coroutines.launch

private val NodeBorderColor = Color(0xFF1E1E1E)
private val CoreNodeYellow = Color(0xFFFFE600)
private val TopicNodeCream = Color(0xFFFFF6E5)
private val PurpleCheckColor = Color(0xFF7C3AED)
private val GreenCheckColor = Color(0xFF10B981)
private val GreyCheckColor = Color(0xFF6B7280)
private val BlueBoxColor = Color(0xFF2563EB)

// Mobile-first geometry matching target design (Screenshot 1):
// Total Canvas Width = 372.dp fits directly onto standard 360-400dp mobile screens at 1.0x scale!
// Left Track: 104.dp | Left Connector: 16.dp | Center Spine: 100.dp (center x = 170.dp) | Right Connector: 16.dp | Right Track: 136.dp
private val CanvasTotalWidth = 372.dp
private val LeftTrackWidth = 104.dp
private val RightTrackWidth = 136.dp
private val ConnectorWidth = 16.dp
private val CoreNodeWidth = 100.dp
private val TopicItemHeight = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDevRoadmapScreen(
    onBack: () -> Unit,
    viewModel: WebDevRoadmapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.celebrationMessage) {
        uiState.celebrationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCelebration()
        }
    }

    Scaffold(
        containerColor = Color(0xFFFBFBFB),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Front-end Roadmap",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                EduNovaBadge("roadmap.sh format", containerColor = Color(0xFFEDE9FE), contentColor = PurpleCheckColor)
                            }
                            Text(
                                text = "${uiState.completedCount}/${uiState.totalNodes} Nodes Completed • ${uiState.progressPercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }

                    // Compact Legend Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendBadgeItem(color = PurpleCheckColor, label = "Personal Recommendation")
                        LegendBadgeItem(color = GreenCheckColor, label = "Alternative Option")
                        LegendBadgeItem(color = GreyCheckColor, label = "Order not strict")
                    }
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val screenWidth = maxWidth
            val density = LocalDensity.current

            // Initial scale chosen so the usable 372dp roadmap fits the mobile screen width edge-to-edge
            // On a 390dp phone: fitScale = 390 / 372 = 1.05. Zero clipping, immediate visual clarity!
            val fitScale = (screenWidth.value / CanvasTotalWidth.value).coerceIn(0.85f, 1.25f)
            var scale by remember(screenWidth) { mutableFloatStateOf(fitScale) }

            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            var contentHeightPx by remember { mutableIntStateOf(0) }
            val contentHeightDp = with(density) { contentHeightPx.toDp() }

            val isFitOrNarrow = (CanvasTotalWidth * scale) <= (screenWidth + 2.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Pinch to zoom: handles 2-finger zoom cleanly while leaving 1-finger vertical scroll smooth!
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
                // Main scrollable canvas container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState, enabled = !isFitOrNarrow),
                    contentAlignment = if (isFitOrNarrow) Alignment.TopCenter else Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .width(CanvasTotalWidth * scale)
                            .then(
                                if (contentHeightDp > 0.dp) Modifier.height(contentHeightDp * scale)
                                else Modifier.wrapContentHeight()
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(CanvasTotalWidth)
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
                            ConnectedRoadmapMobileCanvas(
                                uiState = uiState,
                                onNodeClick = { viewModel.selectNode(it) }
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

    // Interactive Topic Detail Modal Bottom Sheet
    uiState.selectedNode?.let { node ->
        WebDevNodeDetailBottomSheet(
            node = node,
            isCompleted = uiState.isCompleted(node.id),
            isInProgress = uiState.isInProgress(node.id),
            onDismiss = { viewModel.selectNode(null) },
            onMarkCompleted = { viewModel.markNodeCompleted(node.id) },
            onMarkInProgress = { viewModel.markNodeInProgress(node.id) },
            onReset = { viewModel.resetNode(node.id) }
        )
    }
}

@Composable
private fun LegendBadgeItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
    }
}

/**
 * Mobile-fitted 5-zone symmetrical cluster row matching target Screenshot 1.
 * Center spine is rigidly locked at x = 170.dp.
 */
@Composable
private fun RoadmapClusterRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    leftContent: (@Composable () -> Unit)? = null,
    leftConnector: (@Composable () -> Unit)? = null,
    centerContent: @Composable () -> Unit,
    rightConnector: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.width(CanvasTotalWidth),
        verticalAlignment = verticalAlignment
    ) {
        // Left Track (104.dp)
        Box(
            modifier = Modifier.width(LeftTrackWidth),
            contentAlignment = Alignment.CenterEnd
        ) {
            leftContent?.invoke()
        }

        // Left Connector (16.dp)
        Box(
            modifier = Modifier.width(ConnectorWidth),
            contentAlignment = Alignment.Center
        ) {
            leftConnector?.invoke()
        }

        // Center Spine (100.dp, centered at x = 170.dp)
        Box(
            modifier = Modifier.width(CoreNodeWidth),
            contentAlignment = Alignment.Center
        ) {
            centerContent()
        }

        // Right Connector (16.dp)
        Box(
            modifier = Modifier.width(ConnectorWidth),
            contentAlignment = Alignment.Center
        ) {
            rightConnector?.invoke()
        }

        // Right Track (136.dp)
        Box(
            modifier = Modifier.width(RightTrackWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            rightContent?.invoke()
        }
    }
}

/**
 * Connected mobile canvas faithfully replicating Screenshot 1 (target design).
 */
@Composable
private fun ConnectedRoadmapMobileCanvas(
    uiState: WebDevRoadmapUiState,
    onNodeClick: (WebDevNode) -> Unit
) {
    Column(
        modifier = Modifier.width(CanvasTotalWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Front-end Title
        Text(
            text = "Front-end",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        DottedVerticalSpine(height = 14.dp)

        // 1. TOP SECTION: Internet, HTML, CSS, JavaScript in center,
        // Left: Legend & Beginner Project Card,
        // Right: 6 Internet subtopic nodes connected with curved fans.
        RoadmapClusterRow(
            verticalAlignment = Alignment.Top,
            leftContent = {
                Column(
                    modifier = Modifier.width(LeftTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Legend / Info Box (matching Screenshot 1)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            LegendMicroItem(color = PurpleCheckColor, text = "Personal Recommendation")
                            LegendMicroItem(color = GreenCheckColor, text = "Alternative Option")
                            LegendMicroItem(color = GreyCheckColor, text = "Order not strict on roadmap")

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Visit Beginner Friendly Version",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                                )
                            }

                            Text(
                                text = "Related Roadmaps",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            listOf("JavaScript Roadmap", "React Roadmap", "TypeScript Roadmap", "Node.js Roadmap").forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(8.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(item, fontSize = 7.sp, color = Color(0xFF374151))
                                }
                            }
                        }
                    }

                    // Beginner Callout Card on Left
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "HTML, CSS and JavaScript are the backbone of web development. Practice by building projects.",
                                fontSize = 7.sp,
                                lineHeight = 9.sp,
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE5E7EB),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Beginner Project Ideas",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 2.5.dp)
                                )
                            }
                        }
                    }
                }
            },
            centerContent = {
                Column(
                    modifier = Modifier.width(CoreNodeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_internet")!!,
                        isCompleted = uiState.isCompleted("core_internet"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 10.dp)

                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_html")!!,
                        isCompleted = uiState.isCompleted("core_html"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 8.dp)

                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_css")!!,
                        isCompleted = uiState.isCompleted("core_css"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 8.dp)

                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_javascript")!!,
                        isCompleted = uiState.isCompleted("core_javascript"),
                        onClick = onNodeClick
                    )
                }
            },
            rightConnector = {
                DottedBranchFan(
                    isRightSide = true,
                    count = 6,
                    itemHeight = TopicItemHeight,
                    startY = 15.dp
                )
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(RightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val internetTopics = listOf(
                        "branch_how_internet_works",
                        "branch_what_is_http",
                        "branch_domain_name",
                        "branch_what_is_hosting",
                        "branch_dns_how_works",
                        "branch_browsers_how_work"
                    )
                    internetTopics.forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(
                                node = node,
                                isCompleted = uiState.isCompleted(node.id),
                                width = RightTrackWidth,
                                onClick = { onNodeClick(node) }
                            )
                        }
                    }
                }
            }
        )

        DottedVerticalSpine(height = 16.dp)

        // 2. VERSION CONTROL & VCS HOSTING + LEFT BRANCHES (Git, GitHub, GitLab) + PACKAGE MANAGERS (Right)
        RoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(LeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("branch_git", "branch_github", "branch_gitlab").forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = LeftTrackWidth, onClick = { onNodeClick(node) })
                        }
                    }
                }
            },
            leftConnector = {
                DottedBranchFan(isRightSide = false, count = 3, itemHeight = TopicItemHeight)
            },
            centerContent = {
                Column(
                    modifier = Modifier.width(CoreNodeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_version_control")!!,
                        isCompleted = uiState.isCompleted("core_version_control"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 8.dp)

                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_vcs_hosting")!!,
                        isCompleted = uiState.isCompleted("core_vcs_hosting"),
                        onClick = onNodeClick
                    )
                }
            },
            rightConnector = {
                DottedHorizontalConnector()
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(RightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 2x2 Grid for npm, yarn, pnpm, Bun
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WebDevRoadmapDataProvider.getNodeById("branch_npm")?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                RoadmapMicroBox(node = it, isCompleted = uiState.isCompleted(it.id), onClick = { onNodeClick(it) })
                            }
                        }
                        WebDevRoadmapDataProvider.getNodeById("branch_yarn")?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                RoadmapMicroBox(node = it, isCompleted = uiState.isCompleted(it.id), onClick = { onNodeClick(it) })
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WebDevRoadmapDataProvider.getNodeById("branch_pnpm")?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                RoadmapMicroBox(node = it, isCompleted = uiState.isCompleted(it.id), onClick = { onNodeClick(it) })
                            }
                        }
                        WebDevRoadmapDataProvider.getNodeById("branch_bun")?.let {
                            Box(modifier = Modifier.weight(1f)) {
                                RoadmapMicroBox(node = it, isCompleted = uiState.isCompleted(it.id), onClick = { onNodeClick(it) })
                            }
                        }
                    }

                    // Package Managers yellow box
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_package_managers")!!,
                        isCompleted = uiState.isCompleted("core_package_managers"),
                        onClick = onNodeClick,
                        width = RightTrackWidth
                    )
                }
            }
        )

        DottedVerticalSpine(height = 14.dp)

        // 3. CSS FRAMEWORKS + TAILWIND (Right) + Intermediate Callout Card (Center/Left)
        RoadmapClusterRow(
            leftContent = {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "At this point, you should be able to build modern vanilla JS frontend applications.",
                            fontSize = 7.sp,
                            lineHeight = 9.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE5E7EB),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Intermediate Project Ideas",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.5.dp)
                            )
                        }
                    }
                }
            },
            centerContent = {
                RoadmapCoreBox(
                    node = WebDevRoadmapDataProvider.getNodeById("core_css_frameworks")!!,
                    isCompleted = uiState.isCompleted("core_css_frameworks"),
                    onClick = onNodeClick
                )
            },
            rightConnector = {
                DottedHorizontalConnector()
            },
            rightContent = {
                WebDevRoadmapDataProvider.getNodeById("branch_tailwind")?.let { node ->
                    RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = RightTrackWidth, onClick = { onNodeClick(node) })
                }
            }
        )

        DottedVerticalSpine(height = 16.dp)

        // 4. LEARN A FRAMEWORK + LEFT BRANCHES (React, Vue.js, Angular, Svelte, Solid JS) + AI IN DEVELOPMENT (Right)
        RoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(LeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("branch_react", "branch_vue", "branch_angular", "branch_svelte", "branch_solid").forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = LeftTrackWidth, onClick = { onNodeClick(node) })
                        }
                    }
                }
            },
            leftConnector = {
                DottedBranchFan(isRightSide = false, count = 5, itemHeight = TopicItemHeight)
            },
            centerContent = {
                RoadmapCoreBox(
                    node = WebDevRoadmapDataProvider.getNodeById("core_framework")!!,
                    isCompleted = uiState.isCompleted("core_framework"),
                    onClick = onNodeClick
                )
            },
            rightConnector = {
                DottedHorizontalConnector()
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(RightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("AI in Development", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CoreNodeYellow,
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Learn the Basics",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                    listOf("How LLMs work", "AI vs Traditional Coding", "Applications", "Code Reviews", "Refactoring", "Docs Generation").forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TopicNodeCream,
                            border = BorderStroke(1.dp, NodeBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                label,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.5.dp)
                            )
                        }
                    }
                }
            }
        )

        DottedVerticalSpine(height = 16.dp)

        // 5. AI ASSISTED CODING + LEFT BRANCHES (Claude Code, Cursor, Copilot, Antigravity) + Blue Badges
        RoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(LeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("branch_claude_code", "branch_cursor", "branch_copilot", "branch_antigravity").forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = LeftTrackWidth, onClick = { onNodeClick(node) })
                        }
                    }
                    // Blue Roadmap badges matching Screenshot 1
                    RoadmapBlueBadge("Prompt Engineering")
                    RoadmapBlueBadge("AI Agents Roadmap")
                }
            },
            leftConnector = {
                DottedBranchFan(isRightSide = false, count = 4, itemHeight = TopicItemHeight)
            },
            centerContent = {
                RoadmapCoreBox(
                    node = WebDevRoadmapDataProvider.getNodeById("core_ai_assisted")!!,
                    isCompleted = uiState.isCompleted("core_ai_assisted"),
                    onClick = onNodeClick
                )
            }
        )

        DottedVerticalSpine(height = 14.dp)

        // 6. PROMPTING TECHNIQUES -> AGENTS -> MCP -> SKILLS -> IMPLEMENTING AI
        RoadmapClusterRow(
            centerContent = {
                Column(
                    modifier = Modifier.width(CoreNodeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_prompting")!!,
                        isCompleted = uiState.isCompleted("core_prompting"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 8.dp)

                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_agents_mcp")!!,
                        isCompleted = uiState.isCompleted("core_agents_mcp"),
                        onClick = onNodeClick
                    )

                    DottedVerticalSpine(height = 8.dp)

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = CoreNodeYellow,
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        modifier = Modifier.width(CoreNodeWidth)
                    ) {
                        Text("MCP", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 5.dp))
                    }

                    DottedVerticalSpine(height = 6.dp)

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = CoreNodeYellow,
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        modifier = Modifier.width(CoreNodeWidth)
                    ) {
                        Text("Skills", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 5.dp))
                    }
                }
            }
        )

        DottedVerticalSpine(height = 14.dp)

        // 7. IMPLEMENTING AI + LEFT BRANCHES (Gemini, OpenAI, Anthropic)
        RoadmapClusterRow(
            leftContent = {
                Column(
                    modifier = Modifier.width(LeftTrackWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("branch_gemini", "branch_openai", "branch_anthropic").forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = LeftTrackWidth, onClick = { onNodeClick(node) })
                        }
                    }
                }
            },
            leftConnector = {
                DottedBranchFan(isRightSide = false, count = 3, itemHeight = TopicItemHeight)
            },
            centerContent = {
                RoadmapCoreBox(
                    node = WebDevRoadmapDataProvider.getNodeById("core_implementing_ai")!!,
                    isCompleted = uiState.isCompleted("core_implementing_ai"),
                    onClick = onNodeClick
                )
            }
        )

        DottedVerticalSpine(height = 16.dp)

        // 8. ADVANCED FRONTEND: Auth Strategies (Left), Module Bundlers (Center), Linters & Formatters (Right)
        Text("Advanced Frontend", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(4.dp))

        RoadmapClusterRow(
            leftContent = {
                Column(modifier = Modifier.width(LeftTrackWidth), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = CoreNodeYellow,
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auth Strategies", fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 5.dp))
                    }
                }
            },
            centerContent = {
                Column(
                    modifier = Modifier.width(CoreNodeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_bundlers")!!,
                        isCompleted = uiState.isCompleted("core_bundlers"),
                        onClick = onNodeClick
                    )

                    // Bundler sub-boxes (Vite, SWC, esbuild, Rollup, Parcel)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MicroTopicCard("Vite", Modifier.weight(1f))
                        MicroTopicCard("SWC", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MicroTopicCard("esbuild", Modifier.weight(1f))
                        MicroTopicCard("Rollup", Modifier.weight(1f))
                    }
                    MicroTopicCard("Parcel", Modifier.fillMaxWidth())
                }
            },
            rightContent = {
                Column(
                    modifier = Modifier.width(RightTrackWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoadmapCoreBox(
                        node = WebDevRoadmapDataProvider.getNodeById("core_linters")!!,
                        isCompleted = uiState.isCompleted("core_linters"),
                        onClick = onNodeClick,
                        width = RightTrackWidth
                    )

                    listOf("branch_biome", "branch_prettier", "branch_eslint").forEach { id ->
                        WebDevRoadmapDataProvider.getNodeById(id)?.let { node ->
                            RoadmapTopicBox(node = node, isCompleted = uiState.isCompleted(node.id), width = RightTrackWidth, onClick = { onNodeClick(node) })
                        }
                    }
                }
            }
        )

        DottedVerticalSpine(height = 16.dp)

        // 9. TESTING (Left) + WEB SECURITY (Right)
        RoadmapClusterRow(
            leftContent = {
                Column(modifier = Modifier.width(LeftTrackWidth), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = CoreNodeYellow,
                        border = BorderStroke(1.2.dp, NodeBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Testing", fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    listOf("Vitest", "Playwright", "Cypress").forEach { tool ->
                        MicroTopicCard(tool, Modifier.fillMaxWidth())
                    }
                }
            },
            centerContent = {
                DottedVerticalSpine(height = 40.dp)
            },
            rightContent = {
                Column(modifier = Modifier.width(RightTrackWidth), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("CORS", "HTTPS", "CSP", "OWASP Risks").forEach { sec ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TopicNodeCream,
                            border = BorderStroke(1.dp, NodeBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                sec,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun LegendMicroItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 6.5.sp,
            color = Color(0xFF374151),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RoadmapBlueBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = BlueBoxColor,
        modifier = Modifier.width(LeftTrackWidth)
    ) {
        Text(
            text = label,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 3.5.dp, horizontal = 4.dp)
        )
    }
}

@Composable
private fun MicroTopicCard(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = TopicNodeCream,
        border = BorderStroke(1.dp, NodeBorderColor),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 2.5.dp, horizontal = 2.dp)
        )
    }
}

/**
 * Solid Core Yellow Milestone Box. Sized to match Screenshot 1 (width = 100.dp).
 */
@Composable
private fun RoadmapCoreBox(
    node: WebDevNode,
    isCompleted: Boolean,
    onClick: (WebDevNode) -> Unit,
    width: Dp = CoreNodeWidth
) {
    Surface(
        onClick = { onClick(node) },
        shape = RoundedCornerShape(5.dp),
        color = if (isCompleted) Color(0xFFE6F4EA) else CoreNodeYellow,
        border = BorderStroke(1.4.dp, NodeBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.width(width)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EduNovaSuccess,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = node.label,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Topic Box with recommendation badge. Sized to match Screenshot 1.
 */
@Composable
private fun RoadmapTopicBox(
    node: WebDevNode,
    isCompleted: Boolean,
    onClick: () -> Unit,
    width: Dp = RightTrackWidth
) {
    val checkColor = when (node.recommendation) {
        RecommendationType.PERSONAL_RECOMMENDATION -> PurpleCheckColor
        RecommendationType.ALTERNATIVE_OPTION -> GreenCheckColor
        else -> GreyCheckColor
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        color = if (isCompleted) Color(0xFFDCFCE7) else TopicNodeCream,
        border = BorderStroke(1.2.dp, NodeBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.width(width)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = node.label,
                fontSize = 9.sp,
                lineHeight = 11.5.sp,
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
                    .background(if (isCompleted) EduNovaSuccess else checkColor, CircleShape),
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
private fun RoadmapMicroBox(
    node: WebDevNode,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val checkColor = when (node.recommendation) {
        RecommendationType.PERSONAL_RECOMMENDATION -> PurpleCheckColor
        RecommendationType.ALTERNATIVE_OPTION -> GreenCheckColor
        else -> GreyCheckColor
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isCompleted) Color(0xFFDCFCE7) else TopicNodeCream,
        border = BorderStroke(1.dp, NodeBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = node.label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(if (isCompleted) EduNovaSuccess else checkColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(7.dp))
            }
        }
    }
}

@Composable
private fun DottedVerticalSpine(height: Dp) {
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .height(height)
    ) {
        drawLine(
            color = NodeBorderColor,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
    }
}

@Composable
private fun DottedHorizontalConnector() {
    Canvas(
        modifier = Modifier
            .width(ConnectorWidth)
            .height(2.dp)
    ) {
        drawLine(
            color = NodeBorderColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
    }
}

/**
 * Curved dotted fan branching from a core node out to multiple branch nodes.
 */
@Composable
private fun DottedBranchFan(
    isRightSide: Boolean,
    count: Int,
    itemHeight: Dp,
    startY: Dp? = null
) {
    val totalHeight = itemHeight * count
    val density = LocalDensity.current

    Canvas(
        modifier = Modifier
            .width(ConnectorWidth)
            .height(totalHeight)
    ) {
        val startX = if (isRightSide) 0f else size.width
        val endX = if (isRightSide) size.width else 0f
        val originY = startY?.let { with(density) { it.toPx() } } ?: (size.height / 2f)

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
                color = NodeBorderColor,
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            )
        }
    }
}

/**
 * Topic details modal bottom sheet for Web Development nodes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDevNodeDetailBottomSheet(
    node: WebDevNode,
    isCompleted: Boolean,
    isInProgress: Boolean,
    onDismiss: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkInProgress: () -> Unit,
    onReset: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (node.nodeType == WebDevNodeType.CORE_MILESTONE) "Core Milestone Path" else "Recommended Frontend Skill",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Quick Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isCompleted) {
                    Button(
                        onClick = onMarkCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Done", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Progress")
                    }
                }

                if (!isInProgress && !isCompleted) {
                    FilledTonalButton(
                        onClick = onMarkInProgress,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Topic", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Overview
            if (node.overview.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(node.overview, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    }
                }
            }

            // Learning Objectives
            if (node.learningObjectives.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Key Learning Objectives", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        node.learningObjectives.forEach { obj ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp), color = EduNovaPrimary)
                                Text(obj, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Subtopics
            if (node.subtopics.isNotEmpty()) {
                Text("Core Concepts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                node.subtopics.forEach { sub ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(sub.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(sub.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Code Examples
            if (node.codeExamples.isNotEmpty()) {
                Text("Code Example", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                node.codeExamples.forEach { ex ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ex.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E1E2E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ex.code,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCDD6F4),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            if (ex.expectedOutput.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF11111B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Output: " + ex.expectedOutput,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFA6E3A1),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Practice Questions
            if (node.practiceQuestions.isNotEmpty()) {
                Text("Practice Quiz", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                node.practiceQuestions.forEach { q ->
                    var selectedOption by remember { mutableStateOf<Int?>(null) }
                    val isAnswered = selectedOption != null

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(q.question, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            q.options.forEachIndexed { optIdx, optText ->
                                val isCorrect = optIdx == q.correctIndex
                                val isSelected = selectedOption == optIdx
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        isCorrect -> EduNovaSuccess.copy(alpha = 0.15f)
                                        isSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    },
                                    border = BorderStroke(1.dp, if (isAnswered && isCorrect) EduNovaSuccess else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isAnswered) { selectedOption = optIdx }
                                ) {
                                    Text(
                                        text = "${('A'.code + optIdx).toChar()}. $optText",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
