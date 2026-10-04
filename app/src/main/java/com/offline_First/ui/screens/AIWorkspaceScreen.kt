package com.offline_First.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.ChatMessage
import com.offline_First.domain.model.ChatSession
import com.offline_First.domain.model.ConnectionMode
import com.offline_First.domain.model.ExplanationMode
import com.offline_First.domain.model.OfflineAIStatus
import com.offline_First.ui.screens.ai.AIScreen
import com.offline_First.ui.screens.ai.AIViewModel
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaBackground
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSecondaryContainer
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.theme.EduNovaSurface
import com.offline_First.ui.theme.EduNovaTextPrimary
import com.offline_First.ui.theme.EduNovaTextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkspaceScreen(
    isClass912Student: Boolean = true,
    onBack: () -> Unit = {},
    openToolsOnStart: Boolean = false,
    viewModel: AIViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val coroutineScope = rememberCoroutineScope()

    val handleExitToHome: () -> Unit = {
        if (uiState.currentScreen == AIScreen.DOWNLOAD_STATE || uiState.currentScreen == AIScreen.SETTINGS) {
            viewModel.setScreen(if (uiState.currentSession?.messages?.isNotEmpty() == true) AIScreen.CHAT else AIScreen.LANDING)
        }
        onBack()
    }

    LaunchedEffect(Unit) {
        if (uiState.currentScreen == AIScreen.DOWNLOAD_STATE) {
            viewModel.setScreen(if (uiState.currentSession?.messages?.isNotEmpty() == true) AIScreen.CHAT else AIScreen.LANDING)
        }
    }

    LaunchedEffect(openToolsOnStart) {
        if (openToolsOnStart) {
            viewModel.setToolsSheetVisible(true)
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != uiState.isDrawerOpen) {
            viewModel.setDrawerOpen(drawerState.isOpen)
        }
    }

    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && !drawerState.isOpen) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    BackHandler {
        when {
            drawerState.isOpen -> {
                coroutineScope.launch { drawerState.close() }
                viewModel.setDrawerOpen(false)
            }
            uiState.showDeleteConfirmDialog -> viewModel.showDeleteConfirm(false)
            uiState.renameTargetSessionId != null -> viewModel.cancelRenameChat()
            uiState.showToolsSheet -> viewModel.setToolsSheetVisible(false)
            uiState.showAttachmentsSheet -> viewModel.setAttachmentsSheetVisible(false)
            uiState.activeToolName != null -> viewModel.closeActiveTool()
            uiState.currentScreen == AIScreen.DOWNLOAD_STATE -> viewModel.setScreen(AIScreen.SETTINGS)
            uiState.currentScreen == AIScreen.SETTINGS -> {
                val destination = if (uiState.previousScreen == AIScreen.CHAT) AIScreen.CHAT else AIScreen.LANDING
                viewModel.setScreen(destination)
            }
            uiState.currentScreen == AIScreen.CHAT -> handleExitToHome()
            else -> handleExitToHome()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.currentScreen == AIScreen.CHAT,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = EduNovaSurface,
                modifier = Modifier.width(320.dp)
            ) {
                ChatHistoryDrawerContent(
                    sessions = uiState.sessions,
                    currentSessionId = uiState.currentSession?.id,
                    onNewChat = {
                        viewModel.createNewChat()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSelectChat = { id ->
                        viewModel.selectChat(id)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onRenameChat = { id, title -> viewModel.startRenameChat(id, title) },
                    onDeleteChat = { id -> viewModel.deleteChat(id) },
                    onBackToHome = {
                        coroutineScope.launch { drawerState.close() }
                        handleExitToHome()
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EduNovaBackground)
        ) {
            // If an individual tool is opened (preserving tools)
            if (uiState.activeToolName != null) {
                ActiveToolScreen(
                    toolName = uiState.activeToolName!!,
                    isClass912 = isClass912Student,
                    onBack = { viewModel.closeActiveTool() }
                )
            } else {
                when (uiState.currentScreen) {
                    AIScreen.LANDING -> {
                        AILandingScreenView(
                            draft = uiState.draftMessage,
                            onDraftChanged = { viewModel.onDraftMessageChanged(it) },
                            onSend = { prompt ->
                                viewModel.sendMessage(prompt)
                            },
                            onOpenTools = { viewModel.setToolsSheetVisible(true) },
                            onStartChat = { viewModel.setScreen(AIScreen.CHAT) },
                            onOpenTool = { toolName -> viewModel.openTool(toolName) },
                            onOpenAttachments = { viewModel.setAttachmentsSheetVisible(true) },
                            onOpenSettings = { viewModel.setScreen(AIScreen.SETTINGS) },
                            onBack = handleExitToHome
                        )
                    }
                    AIScreen.CHAT -> {
                        CleanChatScreenView(
                            session = uiState.currentSession,
                            explanationMode = uiState.explanationMode,
                            isGenerating = uiState.isGeneratingResponse,
                            draft = uiState.draftMessage,
                            onDraftChanged = { viewModel.onDraftMessageChanged(it) },
                            onSendMessage = { viewModel.sendMessage() },
                            onOpenHistory = {
                                coroutineScope.launch { drawerState.open() }
                                viewModel.setDrawerOpen(true)
                            },
                            onOpenSettings = { viewModel.setScreen(AIScreen.SETTINGS) },
                            onOpenAttachments = { viewModel.setAttachmentsSheetVisible(true) },
                            onBack = handleExitToHome
                        )
                    }
                    AIScreen.SETTINGS -> {
                        AISettingsScreenView(
                            connectionMode = uiState.connectionMode,
                            explanationMode = uiState.explanationMode,
                            offlineStatus = uiState.offlineAIStatus,
                            downloadProgress = uiState.downloadProgress,
                            downloadSize = uiState.downloadSize,
                            estimatedTimeRemaining = uiState.estimatedTimeRemaining,
                            onBack = {
                                val destination = if (uiState.previousScreen == AIScreen.CHAT) AIScreen.CHAT else AIScreen.LANDING
                                viewModel.setScreen(destination)
                            },
                            onSelectConnectionMode = { mode ->
                                viewModel.setConnectionMode(mode)
                                if (mode == ConnectionMode.OFFLINE) {
                                    if (uiState.offlineAIStatus != OfflineAIStatus.READY) {
                                        viewModel.startOfflineDownload(navigateToDownloadState = true)
                                    } else {
                                        viewModel.setScreen(AIScreen.DOWNLOAD_STATE)
                                    }
                                }
                            },
                            onSelectExplanationMode = { viewModel.setExplanationMode(it) },
                            onSetupOfflineAI = { viewModel.startOfflineDownload(navigateToDownloadState = true) },
                            onOpenDownloadState = { viewModel.setScreen(AIScreen.DOWNLOAD_STATE) },
                            onRequestDeleteOfflineAI = { viewModel.showDeleteConfirm(true) }
                        )
                    }
                    AIScreen.DOWNLOAD_STATE -> {
                        OfflineDownloadStateView(
                            downloadStage = uiState.downloadStage,
                            progress = uiState.downloadProgress,
                            downloadSize = uiState.downloadSize,
                            estimatedTime = uiState.estimatedTimeRemaining,
                            onBack = { viewModel.setScreen(AIScreen.SETTINGS) },
                            onStartOfflineChat = { viewModel.setScreen(AIScreen.CHAT) }
                        )
                    }
                }
            }

            // AI Tools bottom sheet
            if (uiState.showToolsSheet) {
                ToolsSheet(
                    isClass912Student = isClass912Student,
                    onDismiss = { viewModel.setToolsSheetVisible(false) },
                    onSelect = { toolName -> viewModel.openTool(toolName) }
                )
            }

            // Attachments bottom sheet
            if (uiState.showAttachmentsSheet) {
                AttachmentSheet(
                    onDismiss = { viewModel.setAttachmentsSheetVisible(false) },
                    onAction = { viewModel.setAttachmentsSheetVisible(false) }
                )
            }

            // Rename Chat Dialog
            if (uiState.renameTargetSessionId != null) {
                RenameChatDialog(
                    initialTitle = uiState.renameDraftText,
                    onTitleChange = { viewModel.onRenameDraftChanged(it) },
                    onConfirm = { viewModel.confirmRenameChat() },
                    onDismiss = { viewModel.cancelRenameChat() }
                )
            }

            // Delete Offline AI Confirmation Dialog inside Settings
            if (uiState.showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showDeleteConfirm(false) },
                    title = {
                        Text(
                            "Delete Offline AI?",
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )
                    },
                    text = {
                        Text(
                            "This will remove the downloaded AI and free up storage space.",
                            color = EduNovaTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmDeleteOfflineAI() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showDeleteConfirm(false) }) {
                            Text("Cancel", color = EduNovaTextSecondary)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}

/* =========================================================================
   SCREEN 1 — AI LANDING PAGE
   ========================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AILandingScreenView(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onOpenTools: () -> Unit,
    onStartChat: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EduNova AI",
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onOpenTools,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .semantics { contentDescription = "Open AI Tools" },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EduNovaSurface,
                            contentColor = EduNovaPrimary
                        )
                    ) {
                        Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Tools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics { contentDescription = "AI Settings" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "AI Settings",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduNovaBackground,
                    titleContentColor = EduNovaTextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(10.dp))

                // Lightbulb / AI Icon
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = EduNovaPrimary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "EduNova AI Icon",
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "EduNova AI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )

                Text(
                    text = "Learn smarter.\nUnderstand faster.",
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduNovaTextSecondary,
                    lineHeight = 24.sp
                )

                Text(
                    text = "Ask questions, understand difficult topics,\nrevise concepts, and study with AI tools.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = EduNovaTextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(8.dp))

                // Action cards
                val aiActions = listOf(
                    Triple("Chat with AI", "Ask anything, brainstorm & solve doubts", Icons.Default.ChatBubbleOutline),
                    Triple("Create a quiz", "Practice with rapid-fire questions", Icons.Default.Quiz),
                    Triple("Make flashcards", "Quickly review definitions & concepts", Icons.Default.Style),
                    Triple("Summarize notes", "Condense long topics into key bullets", Icons.AutoMirrored.Filled.MenuBook),
                    Triple("Formula explainer", "Step-by-step breakdown of equations", Icons.Default.AutoAwesome)
                )

                aiActions.forEach { (title, subtitle, icon) ->
                    Card(
                        onClick = {
                            when (title) {
                                "Chat with AI" -> onStartChat()
                                "Create a quiz" -> onOpenTool("Quiz")
                                "Make flashcards" -> onOpenTool("Flashcards")
                                "Summarize notes" -> {
                                    onDraftChanged("Summarize my study notes on: ")
                                    onStartChat()
                                }
                                "Formula explainer" -> {
                                    onDraftChanged("Explain the formula and steps for: ")
                                    onStartChat()
                                }
                                else -> onStartChat()
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EduNovaPrimary.copy(alpha = 0.10f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = EduNovaPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = EduNovaTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = subtitle,
                                    color = EduNovaTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // Bottom chat input style
            AIChatBottomBar(
                draft = draft,
                onDraftChanged = onDraftChanged,
                onSend = {
                    onSend(it)
                },
                onOpenAttachments = onOpenAttachments
            )
        }
    }
}

/* =========================================================================
   SCREEN 2 — AI SETTINGS
   ========================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AISettingsScreenView(
    connectionMode: ConnectionMode,
    explanationMode: ExplanationMode,
    offlineStatus: OfflineAIStatus,
    downloadProgress: Int,
    downloadSize: String,
    estimatedTimeRemaining: String,
    onBack: () -> Unit,
    onSelectConnectionMode: (ConnectionMode) -> Unit,
    onSelectExplanationMode: (ExplanationMode) -> Unit,
    onSetupOfflineAI: () -> Unit,
    onOpenDownloadState: () -> Unit,
    onRequestDeleteOfflineAI: () -> Unit
) {
    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Settings",
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduNovaBackground,
                    titleContentColor = EduNovaTextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // SECTION 1: Connection Mode
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Connection Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )

                // Online Card
                SelectableSettingCard(
                    title = "☁  Online",
                    description = "Uses online AI and requires internet.",
                    isSelected = connectionMode == ConnectionMode.ONLINE,
                    onClick = { onSelectConnectionMode(ConnectionMode.ONLINE) }
                )

                // Offline Card
                SelectableSettingCard(
                    title = "📱  Offline",
                    description = "Works without internet.",
                    isSelected = connectionMode == ConnectionMode.OFFLINE,
                    onClick = { onSelectConnectionMode(ConnectionMode.OFFLINE) }
                )
            }

            HorizontalDivider(color = EduNovaBorder)

            // SECTION 2: Explanation Mode
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Explanation Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )

                ExplanationMode.entries.forEach { mode ->
                    val titlePrefix = when (mode) {
                        ExplanationMode.TEACHER -> "👨‍🏫"
                        ExplanationMode.GENERAL -> "💬"
                        ExplanationMode.EXPLAINABLE -> "📖"
                    }
                    SelectableSettingCard(
                        title = "$titlePrefix  ${mode.displayName}",
                        description = mode.description,
                        isSelected = explanationMode == mode,
                        onClick = { onSelectExplanationMode(mode) }
                    )
                }
            }

            HorizontalDivider(color = EduNovaBorder)

            // SECTION 3: Offline AI
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Offline AI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (offlineStatus) {
                            OfflineAIStatus.NOT_DOWNLOADED -> {
                                Text(
                                    text = "Offline AI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduNovaTextPrimary
                                )
                                Text(
                                    text = "Not downloaded",
                                    fontSize = 13.sp,
                                    color = EduNovaTextSecondary
                                )
                                Button(
                                    onClick = onSetupOfflineAI,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Set up Offline AI", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            OfflineAIStatus.DOWNLOADING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Offline AI",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EduNovaTextPrimary
                                        )
                                        Text(
                                            text = "Downloading...",
                                            fontSize = 13.sp,
                                            color = EduNovaPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    TextButton(onClick = onOpenDownloadState) {
                                        Text("View details", color = EduNovaPrimary, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(downloadSize, fontSize = 13.sp, color = EduNovaTextSecondary)
                                    Text("$downloadProgress%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EduNovaPrimary)
                                }

                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = EduNovaPrimary,
                                    trackColor = EduNovaBorder
                                )

                                Text(
                                    text = estimatedTimeRemaining,
                                    fontSize = 12.sp,
                                    color = EduNovaTextSecondary
                                )
                            }
                            OfflineAIStatus.READY -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EduNovaSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Offline AI ready",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduNovaTextPrimary
                                    )
                                }

                                Text(
                                    text = "$downloadSize downloaded",
                                    fontSize = 13.sp,
                                    color = EduNovaTextSecondary
                                )

                                OutlinedButton(
                                    onClick = onRequestDeleteOfflineAI,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Delete Offline AI", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableSettingCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EduNovaPrimaryContainer.copy(alpha = 0.45f) else EduNovaSurface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EduNovaPrimary else EduNovaBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduNovaTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = EduNovaTextSecondary,
                    lineHeight = 17.sp
                )
            }
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = EduNovaPrimary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/* =========================================================================
   SCREEN 3 — OFFLINE AI DOWNLOAD STATE
   ========================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineDownloadStateView(
    downloadStage: String,
    progress: Int,
    downloadSize: String,
    estimatedTime: String,
    onBack: () -> Unit,
    onStartOfflineChat: () -> Unit
) {
    val isReady = progress >= 100 || downloadStage.equals("Ready", ignoreCase = true)

    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isReady) "Offline AI Ready" else "Offline AI Setup",
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduNovaBackground,
                    titleContentColor = EduNovaTextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isReady) {
                        Surface(
                            shape = CircleShape,
                            color = EduNovaSuccess.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduNovaSuccess,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Offline AI is ready",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "You can now ask questions, solve doubts, and learn completely offline without internet.",
                            fontSize = 14.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Surface(
                            color = EduNovaPrimaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, EduNovaPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$downloadSize downloaded",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = onStartOfflineChat,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Learning Offline", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = EduNovaPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = EduNovaPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Preparing Offline AI...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "The AI is being prepared for offline learning.",
                            fontSize = 14.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = downloadStage,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaPrimary
                            )
                            Text(
                                text = downloadSize,
                                fontSize = 13.sp,
                                color = EduNovaTextSecondary
                            )
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = EduNovaPrimary,
                            trackColor = EduNovaBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$progress%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextPrimary
                            )
                            Text(
                                text = estimatedTime,
                                fontSize = 12.sp,
                                color = EduNovaTextSecondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, EduNovaBorder)
                        ) {
                            Text("Continue in background", color = EduNovaTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================
   SCREEN 4 — CLEAN CHAT INTERFACE
   ========================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanChatScreenView(
    session: ChatSession?,
    explanationMode: ExplanationMode,
    isGenerating: Boolean,
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAttachments: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val messages = session?.messages ?: emptyList()

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "EduNova AI",
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            fontSize = 19.sp
                        )
                        // Small subtle chip for currently selected explanation mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduNovaPrimaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, EduNovaPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = explanationMode.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = EduNovaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back to Homepage" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Homepage",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.semantics { contentDescription = "Chat History" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Chat History",
                            tint = EduNovaTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics { contentDescription = "AI Settings" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "AI Settings",
                            tint = EduNovaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduNovaBackground,
                    titleContentColor = EduNovaTextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (messages.isEmpty()) {
                    item(key = "empty-chat-welcome") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                color = EduNovaPrimary.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = EduNovaPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Text(
                                text = "How can I help you today?",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextPrimary
                            )

                            Text(
                                text = "Ask questions, understand topics,\nsolve doubts, or get study help.",
                                textAlign = TextAlign.Center,
                                color = EduNovaTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            listOf(
                                "Explain Binary Search and its time complexity",
                                "How do I solve quadratic equations?",
                                "What is the difference between DFS and BFS?",
                                "Create a quick 2-question quiz"
                            ).forEach { suggestion ->
                                Surface(
                                    onClick = { onDraftChanged(suggestion) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = EduNovaSurface,
                                    border = BorderStroke(1.dp, EduNovaBorder),
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = EduNovaTextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        ChatBubbleItem(message = message)
                    }

                    if (isGenerating) {
                        item(key = "typing-indicator") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = EduNovaSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, EduNovaBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = EduNovaPrimary
                                        )
                                        Text(
                                            "Thinking...",
                                            fontSize = 12.sp,
                                            color = EduNovaTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Row
            AIChatBottomBar(
                draft = draft,
                onDraftChanged = onDraftChanged,
                onSend = { onSendMessage() },
                onOpenAttachments = onOpenAttachments
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) EduNovaPrimary else EduNovaSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 4.dp,
                bottomEnd = if (message.fromUser) 4.dp else 18.dp
            ),
            border = if (message.fromUser) null else BorderStroke(1.dp, EduNovaBorder),
            modifier = Modifier.fillMaxWidth(if (message.fromUser) 0.82f else 0.90f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If text contains code block, display neatly formatted
                val text = message.text
                if (text.contains("```")) {
                    FormattedMarkdownChatText(text = text, isFromUser = message.fromUser)
                } else {
                    Text(
                        text = text,
                        color = if (message.fromUser) Color.White else EduNovaTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattedMarkdownChatText(text: String, isFromUser: Boolean) {
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val cleanedCode = part.trim().removePrefix("kotlin").removePrefix("java").trim()
                Surface(
                    color = Color(0xFF1E1E24),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cleanedCode,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    color = if (isFromUser) Color.White else EduNovaTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun AIChatBottomBar(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onOpenAttachments: () -> Unit
) {
    Surface(
        color = EduNovaBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder)
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(onClick = onOpenAttachments) {
                        Icon(Icons.Default.Add, "Add attachment", tint = EduNovaTextSecondary)
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message EduNova AI...", color = EduNovaTextSecondary.copy(alpha = 0.7f), fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = EduNovaTextPrimary,
                            unfocusedTextColor = EduNovaTextPrimary
                        )
                    )
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Mic, "Voice input", tint = EduNovaTextSecondary)
                    }
                    IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onSend(draft.trim())
                            }
                        },
                        enabled = draft.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (draft.isNotBlank()) EduNovaPrimary else EduNovaTextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/* =========================================================================
   SCREEN 5 — CHAT HISTORY (Drawer Content)
   ========================================================================= */
@Composable
private fun ChatHistoryDrawerContent(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onBackToHome: () -> Unit = {}
) {
    val now = Calendar.getInstance()
    val todayStart = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - (24L * 60 * 60 * 1000)
    val sevenDaysAgo = todayStart - (7L * 24 * 60 * 60 * 1000)

    val todayChats = sessions.filter { it.lastUpdated >= todayStart }
    val yesterdayChats = sessions.filter { it.lastUpdated in yesterdayStart until todayStart }
    val last7DaysChats = sessions.filter { it.lastUpdated in sevenDaysAgo until yesterdayStart }
    val olderChats = sessions.filter { it.lastUpdated < sevenDaysAgo }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "EduNova AI",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduNovaTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onNewChat,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("+ New Chat", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Recent Chats",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = EduNovaTextSecondary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = EduNovaTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No previous chats yet.\nStart a new conversation!",
                            textAlign = TextAlign.Center,
                            color = EduNovaTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                if (todayChats.isNotEmpty()) {
                    item { HistorySectionHeader("Today") }
                items(todayChats, key = { it.id }) { session ->
                    HistoryItemRow(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onClick = { onSelectChat(session.id) },
                        onRename = { onRenameChat(session.id, session.title) },
                        onDelete = { onDeleteChat(session.id) }
                    )
                }
            }

            if (yesterdayChats.isNotEmpty()) {
                item { HistorySectionHeader("Yesterday") }
                items(yesterdayChats, key = { it.id }) { session ->
                    HistoryItemRow(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onClick = { onSelectChat(session.id) },
                        onRename = { onRenameChat(session.id, session.title) },
                        onDelete = { onDeleteChat(session.id) }
                    )
                }
            }

            if (last7DaysChats.isNotEmpty()) {
                item { HistorySectionHeader("Last 7 Days") }
                items(last7DaysChats, key = { it.id }) { session ->
                    HistoryItemRow(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onClick = { onSelectChat(session.id) },
                        onRename = { onRenameChat(session.id, session.title) },
                        onDelete = { onDeleteChat(session.id) }
                    )
                }
            }

            if (olderChats.isNotEmpty()) {
                item { HistorySectionHeader("Older") }
                items(olderChats, key = { it.id }) { session ->
                    HistoryItemRow(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onClick = { onSelectChat(session.id) },
                        onRename = { onRenameChat(session.id, session.title) },
                        onDelete = { onDeleteChat(session.id) }
                    )
                }
            }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = EduNovaBorder)
        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBackToHome,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EduNovaBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = EduNovaSurface,
                contentColor = EduNovaTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = EduNovaPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("Back to Homepage", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HistorySectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = EduNovaPrimary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun HistoryItemRow(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = if (isSelected) EduNovaPrimaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isSelected) EduNovaPrimary else EduNovaTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = session.title,
                maxLines = 1,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = EduNovaTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = EduNovaTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(EduNovaSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", fontSize = 13.sp, color = EduNovaTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/* =========================================================================
   SUPPORTING DIALOGS & EXISTING TOOLS INTEGRATION
   ========================================================================= */
@Composable
private fun RenameChatDialog(
    initialTitle: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Chat", fontWeight = FontWeight.Bold, color = EduNovaTextPrimary) },
        text = {
            OutlinedTextField(
                value = initialTitle,
                onValueChange = onTitleChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EduNovaTextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsSheet(
    isClass912Student: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Tools", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EduNovaTextPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close AI Tools") }
            }
            Text("Choose a focused way to learn.", color = EduNovaTextSecondary, fontSize = 13.sp)

            val tools = listOf(
                Pair("Quiz", Icons.Default.Quiz to EduNovaAccent),
                Pair("Flashcards", Icons.Default.Style to EduNovaAccent),
                Pair("Study Planner", Icons.Default.CalendarMonth to EduNovaAccent),
                Pair("Mind Map", Icons.Default.AccountTree to EduNovaSecondary),
                Pair("Image Analysis", Icons.Default.Image to EduNovaSecondary),
                Pair("AI Visual Video", Icons.Default.SmartDisplay to EduNovaSecondary),
                Pair("Reports", Icons.Default.Analytics to EduNovaAccent),
                Pair("Audio Learning", Icons.Default.Headphones to EduNovaSecondary)
            ) + if (isClass912Student) {
                listOf(
                    Pair("PYQ", Icons.AutoMirrored.Filled.MenuBook to EduNovaPrimary),
                    Pair("Exam Mode", Icons.Default.Timer to EduNovaPrimary),
                    Pair("Exam Review", Icons.AutoMirrored.Filled.FactCheck to EduNovaSecondary)
                )
            } else emptyList()

            tools.forEach { (name, iconAccent) ->
                Surface(
                    onClick = {
                        onDismiss()
                        onSelect(name)
                    },
                    color = EduNovaSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = iconAccent.second.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(iconAccent.first, null, tint = iconAccent.second, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(name, color = EduNovaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(onDismiss: () -> Unit, onAction: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Add to your question", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EduNovaTextPrimary)
            listOf(
                "Image" to Icons.Default.AddPhotoAlternate,
                "PDF" to Icons.Default.PictureAsPdf,
                "Document" to Icons.Default.Description,
                "Camera" to Icons.Default.CameraAlt,
                "Analyze Image" to Icons.Default.Image,
                "Summarize Document" to Icons.AutoMirrored.Filled.MenuBook
            ).forEach { (label, icon) ->
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(icon, null, tint = EduNovaPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = EduNovaTextPrimary, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveToolScreen(
    toolName: String,
    isClass912: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(toolName, fontWeight = FontWeight.Bold, color = EduNovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EduNovaTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EduNovaBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Interactive $toolName", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                    Text("This tool is ready to use with both Online and Offline AI modes.", fontSize = 13.sp, color = EduNovaTextSecondary)
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Enter topic or study materials...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run $toolName")
                    }
                }
            }
        }
    }
}
