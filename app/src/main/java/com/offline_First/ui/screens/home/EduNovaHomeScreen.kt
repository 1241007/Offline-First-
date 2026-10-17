package com.offline_First.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import com.offline_First.data.AppContainer
import com.offline_First.data.sync.SyncState
import com.offline_First.data.sync.SyncStatus
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.R
import com.offline_First.ui.components.EduNovaBottomNavItem
import com.offline_First.ui.components.EduNovaBottomNavigation
import com.offline_First.ui.navigation.AppDestination
import com.offline_First.ui.theme.EduNovaBackground
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.theme.EduNovaSurface
import com.offline_First.ui.theme.EduNovaTextPrimary
import com.offline_First.ui.theme.EduNovaTextSecondary
import kotlinx.coroutines.launch

/**
 * EduNova Landing Page (Home Screen).
 * Implements the approved EduNova landing-page design with:
 * - Header: Branding, Search, Notifications, Profile (without School/General toggle).
 * - Hero Card: "Ask EduNova AI", subtitle, robot mascot, and query input bar.
 * - Offline Learning Banner: "Learn anytime, anywhere" and "Offline ready" indicator.
 * - 6 Compact Feature Cards: Balanced 2-column grid.
 * - Persistent 5-tab EduNova bottom navigation bar.
 */
@Composable
fun EduNovaHomeScreen(
    isSchoolMode: Boolean = false,
    onNavigate: (EduNovaBottomNavItem) -> Unit,
    onOpenProfile: () -> Unit,
    onAskAI: (String?) -> Unit,
    onOpenAITools: () -> Unit = {},
    onRoadmap: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenMyLearning: () -> Unit = {},
    onPractice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var aiQueryText by remember { mutableStateOf("") }
    var showOfflineInfoDialog by remember { mutableStateOf(false) }

    val syncManager = AppContainer.syncManager
    val syncStatus by syncManager?.syncStatus?.collectAsState() ?: remember { mutableStateOf(null) }
    val networkMonitor = AppContainer.networkMonitor
    val isOnline by networkMonitor?.isOnline?.collectAsState() ?: remember { mutableStateOf(true) }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        containerColor = EduNovaBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            EduNovaBottomNavigation(
                selectedItem = EduNovaBottomNavItem.HOME,
                isSchoolMode = isSchoolMode,
                onNavigate = onNavigate
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header (Logo, Search, Notifications, Profile avatar)
            HomeHeader(
                onSearch = { showMessage("Search will be available soon.") },
                onNotifications = { showMessage("No new notifications") },
                onProfile = onOpenProfile
            )

            // 2. Hero Card: Ask EduNova AI
            AskEduNovaAIHeroCard(
                query = aiQueryText,
                onQueryChange = { aiQueryText = it },
                onSend = {
                    val q = aiQueryText.trim()
                    aiQueryText = ""
                    onAskAI(if (q.isNotEmpty()) q else null)
                },
                onAttach = { showMessage("Attachment support active in AI Workspace") },
                onVoice = { showMessage("Voice input active in AI Workspace") }
            )

            // 3. Offline Learning Banner with sync and offline indicators
            OfflineLearningBanner(
                syncStatus = syncStatus,
                isOnline = isOnline,
                onRetrySync = { scope.launch { syncManager?.syncNow() } },
                onClick = { showOfflineInfoDialog = true }
            )

            // 4. Six Compact Feature Cards in 2x3 Grid (non-clickable showcase cards)
            FeatureGridSection()
        }
    }

    if (showOfflineInfoDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) EduNovaPrimary else EduNovaTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Offline-First Status", fontWeight = FontWeight.Bold, color = EduNovaTextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Connection: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            color = if (isOnline) EduNovaSuccess else EduNovaTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    syncStatus?.let { status ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sync State: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = status.state.name,
                                color = when (status.state) {
                                    SyncState.SUCCESS -> EduNovaSuccess
                                    SyncState.SYNCING -> EduNovaPrimary
                                    SyncState.FAILED -> MaterialTheme.colorScheme.error
                                    else -> EduNovaTextSecondary
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (status.pendingOperationsCount > 0) {
                            Text(
                                text = "Pending queued items: ${status.pendingOperationsCount}",
                                fontSize = 12.5.sp,
                                color = EduNovaTextSecondary
                            )
                        }

                        if (status.lastError != null) {
                            Text(
                                text = "Last error: ${status.lastError}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Local Syllabus & Notes: Instant access offline\n• Offline AI Models: Supported on device\n• Sync on Reconnect: Progress saves locally and syncs automatically",
                        fontSize = 12.5.sp,
                        color = EduNovaTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isOnline && (syncStatus?.pendingOperationsCount ?: 0) > 0) {
                        TextButton(onClick = {
                            scope.launch { syncManager?.syncNow() }
                            showOfflineInfoDialog = false
                        }) {
                            Text("Sync Now", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showOfflineInfoDialog = false }) {
                        Text("Close", color = EduNovaTextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

/**
 * Top Header:
 * - Brand logo ("Edu" in navy, "Nova" in brand teal).
 * - Search icon.
 * - Notifications bell icon.
 * - Profile avatar.
 * Note: School/General mode toggle is deliberately omitted from this header.
 */
@Composable
private fun HomeHeader(
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EduNovaPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("E", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Edu",
                    color = EduNovaTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Nova",
                    color = EduNovaPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Button
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Search" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = EduNovaTextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Notification Button
            Surface(
                onClick = onNotifications,
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Notifications" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = EduNovaTextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    // Notification indicator dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EduNovaPrimary)
                    )
                }
            }

            // Profile Avatar
            Surface(
                onClick = onProfile,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Open profile" }
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar_placeholder),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }
}

/**
 * Hero Card: Ask EduNova AI
 * Mint-tinted card with robot mascot, heading, subtitle, and input box.
 */
@Composable
private fun AskEduNovaAIHeroCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EduNovaPrimaryContainer),
        border = BorderStroke(1.dp, EduNovaBorder.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Upper row: Text and Robot illustration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ask EduNova AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Explain, solve, summarize or get help with any topic.",
                        fontSize = 12.5.sp,
                        color = EduNovaTextSecondary,
                        lineHeight = 17.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Friendly EduNova Robot Illustration
                EduNovaRobotIllustration(modifier = Modifier.size(64.dp))
            }

            // Input Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onAttach,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach file",
                            tint = EduNovaTextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Ask anything...",
                                color = EduNovaTextSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = EduNovaTextPrimary,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(EduNovaPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = onVoice,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = EduNovaTextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Send Arrow Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EduNovaPrimary)
                            .clickable(onClick = onSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Send question",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Friendly Vector Robot Mascot Illustration composable.
 */
@Composable
private fun EduNovaRobotIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Antenna
        drawLine(
            color = EduNovaSecondary,
            start = Offset(w * 0.5f, h * 0.22f),
            end = Offset(w * 0.5f, h * 0.10f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = EduNovaPrimary,
            radius = 3.5.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.08f)
        )

        // Robot Head
        drawRoundRect(
            color = Color(0xFFE2F3F0),
            topLeft = Offset(w * 0.16f, h * 0.22f),
            size = Size(w * 0.68f, h * 0.54f),
            cornerRadius = CornerRadius(14.dp.toPx())
        )
        drawRoundRect(
            color = EduNovaPrimary,
            topLeft = Offset(w * 0.16f, h * 0.22f),
            size = Size(w * 0.68f, h * 0.54f),
            cornerRadius = CornerRadius(14.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Robot Visor / Face Screen
        drawRoundRect(
            color = EduNovaTextPrimary,
            topLeft = Offset(w * 0.24f, h * 0.32f),
            size = Size(w * 0.52f, h * 0.30f),
            cornerRadius = CornerRadius(8.dp.toPx())
        )

        // Glowing Friendly Eyes
        drawCircle(
            color = Color(0xFF2DD4BF),
            radius = 3.5.dp.toPx(),
            center = Offset(w * 0.38f, h * 0.44f)
        )
        drawCircle(
            color = Color(0xFF2DD4BF),
            radius = 3.5.dp.toPx(),
            center = Offset(w * 0.62f, h * 0.44f)
        )

        // Cute Smile
        val smilePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.44f, h * 0.53f)
            quadraticTo(w * 0.50f, h * 0.57f, w * 0.56f, h * 0.53f)
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF2DD4BF),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Ear / Headphone details
        drawRoundRect(
            color = EduNovaSecondary,
            topLeft = Offset(w * 0.10f, h * 0.36f),
            size = Size(w * 0.08f, h * 0.24f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = EduNovaSecondary,
            topLeft = Offset(w * 0.82f, h * 0.36f),
            size = Size(w * 0.08f, h * 0.24f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // Shoulders
        drawRoundRect(
            color = EduNovaPrimaryContainer,
            topLeft = Offset(w * 0.25f, h * 0.80f),
            size = Size(w * 0.50f, h * 0.18f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
    }
}

/**
 * Offline Learning Banner:
 * "Learn anytime, anywhere" with description and "Offline ready" indicator.
 */
@Composable
private fun OfflineLearningBanner(
    onClick: () -> Unit,
    syncStatus: SyncStatus? = null,
    isOnline: Boolean = true,
    onRetrySync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pendingCount = syncStatus?.pendingOperationsCount ?: 0
    val isSyncing = syncStatus?.state == SyncState.SYNCING
    val isFailed = syncStatus?.state == SyncState.FAILED

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
        border = BorderStroke(1.dp, if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isFailed -> MaterialTheme.colorScheme.errorContainer
                            isSyncing -> EduNovaPrimaryContainer
                            !isOnline -> Color(0xFFF1F5F9)
                            else -> EduNovaPrimaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isFailed -> Icons.Default.Warning
                        isSyncing -> Icons.Default.Sync
                        !isOnline -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudDone
                    },
                    contentDescription = null,
                    tint = when {
                        isFailed -> MaterialTheme.colorScheme.error
                        isSyncing -> EduNovaPrimary
                        !isOnline -> EduNovaTextSecondary
                        else -> EduNovaPrimary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Learn anytime, anywhere",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary
                    )

                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            isSyncing -> EduNovaPrimary.copy(alpha = 0.16f)
                            !isOnline -> Color(0xFFE2E8F0)
                            else -> EduNovaSuccess.copy(alpha = 0.16f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isFailed -> Icons.Default.Warning
                                    isSyncing -> Icons.Default.Sync
                                    !isOnline -> Icons.Default.CloudOff
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = when {
                                    isFailed -> MaterialTheme.colorScheme.error
                                    isSyncing -> EduNovaPrimary
                                    !isOnline -> EduNovaTextSecondary
                                    else -> EduNovaSuccess
                                },
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = when {
                                    isFailed -> "Sync failed"
                                    isSyncing -> "Syncing..."
                                    !isOnline && pendingCount > 0 -> "$pendingCount queued"
                                    !isOnline -> "Offline"
                                    else -> "Offline ready"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isFailed -> MaterialTheme.colorScheme.error
                                    isSyncing -> EduNovaPrimary
                                    !isOnline -> EduNovaTextSecondary
                                    else -> EduNovaSuccess
                                }
                            )
                        }
                    }
                }

                Text(
                    text = when {
                        isFailed -> "Some changes could not be synced. Tap to view details or retry."
                        isSyncing -> "Synchronizing your offline progress with the cloud..."
                        !isOnline && pendingCount > 0 -> "You're offline. $pendingCount item(s) will sync when connection returns."
                        !isOnline -> "You're offline. Access your downloaded courses and materials."
                        else -> "Access your downloaded courses and offline study materials without internet."
                    },
                    fontSize = 12.sp,
                    color = EduNovaTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Six Compact Feature Cards in 2-column balanced grid.
 * Rendered as non-clickable showcase cards.
 */
@Composable
private fun FeatureGridSection(
    modifier: Modifier = Modifier
) {
    val items = listOf(
        FeatureItem(
            title = "Personalized Learning",
            description = "Tailored guidance for learning goals",
            icon = Icons.Default.AccountTree
        ),
        FeatureItem(
            title = "Quizzes & Practice",
            description = "Test your understanding with practice",
            icon = Icons.AutoMirrored.Filled.Assignment
        ),
        FeatureItem(
            title = "Mind Maps",
            description = "Visualize concepts and connections",
            icon = Icons.Default.Hub
        ),
        FeatureItem(
            title = "Study Resources",
            description = "Access learning materials and courses",
            icon = Icons.AutoMirrored.Filled.MenuBook
        ),
        FeatureItem(
            title = "Track Progress",
            description = "Monitor your progress and streaks",
            icon = Icons.Default.Analytics
        ),
        FeatureItem(
            title = "Works Offline",
            description = "Access supported downloaded content",
            icon = Icons.Default.CheckCircle
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    FeatureCard(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun FeatureCard(
    item: FeatureItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EduNovaPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = EduNovaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = EduNovaTextSecondary,
                    lineHeight = 14.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
