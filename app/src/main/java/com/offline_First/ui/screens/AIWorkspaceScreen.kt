package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PageBackground = Color(0xFFF8FAFC)
private val Ink = Color(0xFF0F172A)
private val MutedInk = Color(0xFF64748B)
private val Primary = Color(0xFF2563EB)
private val Border = Color(0xFFE2E8F0)

private enum class AITool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
) {
    CHAT("AI Chat", "Ask anything and learn with AI", Icons.Default.ChatBubbleOutline, Primary),
    PLANNER("Study Planner", "Build a smarter learning routine", Icons.Default.CalendarMonth, Color(0xFF0F766E)),
    MIND_MAP("Mind Map", "Turn a topic into a visual structure", Icons.Default.AccountTree, Color(0xFF7C3AED)),
    IMAGE_ANALYSIS("Image Analysis", "Understand diagrams, notes, and images", Icons.Default.Image, Color(0xFFEA580C)),
    QUIZ("Quiz", "Test your understanding", Icons.Default.Quiz, Color(0xFFD97706)),
    FLASHCARDS("Flashcards", "Revise concepts quickly", Icons.Default.Style, Color(0xFF16A34A)),
    VIDEO("AI Visual Video", "Turn a topic into a visual explanation", Icons.Default.SmartDisplay, Color(0xFFDB2777)),
    REPORTS("Reports", "Track your learning progress", Icons.Default.Analytics, Color(0xFF0891B2)),
    ONLINE_LEARNING("Online Learning", "Continue with curated learning", Icons.Default.School, Color(0xFF2563EB)),
    AUDIO_LEARNING("Audio Learning", "Learn while listening", Icons.Default.Headphones, Color(0xFF4F46E5)),
    PYQ("PYQ", "Practice previous year questions", Icons.Default.MenuBook, Color(0xFF1D4ED8)),
    EXAM_MODE("Exam Mode", "Practice under exam conditions", Icons.Default.Timer, Color(0xFFDC2626)),
    EXAM_REVIEW("Exam Review", "Review performance in depth", Icons.Default.FactCheck, Color(0xFF9333EA))
}

private data class ChatMessage(val text: String, val fromUser: Boolean)

private data class ToolGroup(val title: String, val tools: List<AITool>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkspaceScreen(
    isClass912Student: Boolean = true,
    onBack: () -> Unit = {},
    openToolsOnStart: Boolean = false
) {
    var selectedTool by rememberSaveable { mutableStateOf(AITool.CHAT.name) }
    var showTools by rememberSaveable { mutableStateOf(openToolsOnStart) }
    var showAttachments by rememberSaveable { mutableStateOf(false) }
    var previewAsClass912 by rememberSaveable { mutableStateOf(isClass912Student) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val currentTool = AITool.valueOf(selectedTool)
    val selectTool: (AITool) -> Unit = {
        selectedTool = it.name
        showTools = false
    }

    BackHandler {
        when {
            showAttachments -> showAttachments = false
            showTools -> showTools = false
            currentTool != AITool.CHAT -> selectedTool = AITool.CHAT.name
            else -> onBack()
        }
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentTool == AITool.CHAT) "EduNova AI" else "EduNova AI · ${currentTool.title}",
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentTool == AITool.CHAT) onBack()
                            else selectedTool = AITool.CHAT.name
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showTools = true },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .semantics { contentDescription = "Open AI Tools" },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Tools", fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Ink,
                    navigationIconContentColor = Ink,
                    actionIconContentColor = Ink
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTool) {
                AITool.CHAT -> ChatWorkspace(
                    messages = messages,
                    onSend = { text ->
                        messages += ChatMessage(text, true)
                        messages += ChatMessage("Here is a helpful starting point for \"$text\". I can break it down, quiz you, or make flashcards next.", false)
                    },
                    onNewChat = { messages.clear() },
                    onOpenAttachments = { showAttachments = true }
                )
                AITool.VIDEO -> VideoTool()
                AITool.PLANNER -> FormTool("Study Planner", "Plan goals, subjects, revision, and weekly learning.", "What would you like to plan?")
                AITool.MIND_MAP -> FormTool("Mind Map", "Turn any topic into a visual structure.", "Enter a topic, for example Operating Systems")
                AITool.IMAGE_ANALYSIS -> FormTool("Image Analysis", "Understand images with AI.", "Upload an image or choose a study action")
                AITool.QUIZ -> FormTool("Create Quiz", "Test your understanding with a quick practice set.", "Enter a topic")
                AITool.FLASHCARDS -> FlashcardsTool()
                AITool.REPORTS -> ReportsTool(isClass912Student = previewAsClass912)
                AITool.ONLINE_LEARNING -> SimpleTool("Continue Learning", "Pick up a guided lesson whenever you are ready.", Icons.Default.School)
                AITool.AUDIO_LEARNING -> AudioLearningTool()
                AITool.PYQ -> FormTool("Previous Year Questions", "Practice real exam-style question sets.", "Select subject, chapter, and year")
                AITool.EXAM_MODE -> FormTool("Exam Mode", "Practice under timed, exam-focused conditions.", "Physics · 60 minutes · 30 questions")
                AITool.EXAM_REVIEW -> ReviewTool()
            }
        }
    }

    if (showTools) {
        ToolsSheet(
            selectedTool = currentTool,
            isClass912Student = previewAsClass912,
            onDismiss = { showTools = false },
            onSelect = selectTool,
            onToggleStudentType = {
                previewAsClass912 = it
                if (!it && currentTool in setOf(AITool.PYQ, AITool.EXAM_MODE, AITool.EXAM_REVIEW)) {
                    selectedTool = AITool.CHAT.name
                }
            }
        )
    }

    if (showAttachments) {
        AttachmentSheet(
            onDismiss = { showAttachments = false },
            onAction = { showAttachments = false }
        )
    }
}

@Composable
private fun ChatWorkspace(
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenAttachments: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val suggestions = listOf("Explain a topic", "Help me understand this concept", "Create a quiz", "Make flashcards")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(66.dp),
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.11f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lightbulb, null, tint = Primary, modifier = Modifier.size(30.dp))
                            }
                        }
                        Text("EduNova AI", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Learn smarter.\nUnderstand faster.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 17.sp, color = MutedInk)
                        Text(
                            "Ask questions, understand difficult topics,\nrevise concepts, and study with AI tools.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MutedInk,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.size(10.dp))
                        suggestions.forEach { suggestion ->
                            SuggestionChip(text = suggestion, onClick = { draft = suggestion })
                        }
                    }
                }
            } else {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(onClick = onOpenAttachments) {
                        Icon(Icons.Default.Add, "Add attachment", tint = MutedInk)
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message EduNova AI...", color = MutedInk) },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Mic, "Voice input", tint = MutedInk)
                    }
                    IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onSend(draft.trim())
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            "Send message",
                            tint = if (draft.isNotBlank()) Primary else MutedInk.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
        if (messages.isNotEmpty()) {
            TextButton(onClick = onNewChat, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Chat")
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(0.82f)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Ink, fontSize = 14.sp)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) Primary else Color.White,
            shape = RoundedCornerShape(18.dp),
            border = if (message.fromUser) null else BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(14.dp),
                color = if (message.fromUser) Color.White else Ink,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsSheet(
    selectedTool: AITool,
    isClass912Student: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AITool) -> Unit,
    onToggleStudentType: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groups = listOf(
        ToolGroup("LEARN WITH AI", AITool.entries.take(8)),
        ToolGroup("LEARNING", listOf(AITool.ONLINE_LEARNING, AITool.AUDIO_LEARNING))
    ) + if (isClass912Student) {
        listOf(ToolGroup("EXAM PREPARATION", listOf(AITool.PYQ, AITool.EXAM_MODE, AITool.EXAM_REVIEW)))
    } else {
        emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { },
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Tools", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close AI Tools") }
            }
            Text("Choose a focused way to learn.", color = MutedInk, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preview user type", color = MutedInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = !isClass912Student,
                    onClick = { onToggleStudentType(false) },
                    label = { Text("General") }
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = isClass912Student,
                    onClick = { onToggleStudentType(true) },
                    label = { Text("Class 9–12") }
                )
            }
            groups.forEach { group ->
                Text(group.title, color = MutedInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                group.tools.forEach { tool ->
                    ToolMenuItem(
                        tool = tool,
                        selected = tool == selectedTool,
                        onClick = { onSelect(tool) }
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ToolMenuItem(tool: AITool, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Primary.copy(alpha = 0.07f) else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) Primary.copy(alpha = 0.45f) else Border),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open ${tool.title}" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = tool.accent.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(tool.icon, null, tint = tool.accent, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tool.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(tool.description, color = MutedInk, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(onDismiss: () -> Unit, onAction: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add to your question", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            listOf(
                "Image" to Icons.Default.AddPhotoAlternate,
                "PDF" to Icons.Default.PictureAsPdf,
                "Document" to Icons.Default.Description,
                "Camera" to Icons.Default.CameraAlt,
                "Analyze Image" to Icons.Default.Image,
                "Summarize Document" to Icons.Default.MenuBook
            ).forEach { (label, icon) ->
                TextButton(onClick = onAction, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Icon(icon, null, tint = Primary)
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = Ink, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun VideoTool() {
    var topic by rememberSaveable { mutableStateOf("") }
    var isGenerating by rememberSaveable { mutableStateOf(false) }
    var isReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            delay(1400)
            isGenerating = false
            isReady = true
        }
    }
    ToolPage(title = "AI Visual Video", subtitle = "Turn a difficult topic into a visual explanation.") {
        OutlinedTextField(topic, { topic = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Topic or study material") }, placeholder = { Text("Explain the water cycle") }, minLines = 3)
        Text("Explanation level", color = MutedInk, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(true, {}, { Text("Simple") })
            FilterChip(false, {}, { Text("Detailed") })
            FilterChip(false, {}, { Text("Exam-focused") })
        }
        Button(onClick = { isGenerating = true; isReady = false }, enabled = topic.isNotBlank() && !isGenerating, modifier = Modifier.fillMaxWidth()) {
            Text(if (isGenerating) "Creating your visual lesson..." else "Create Visual Video")
        }
        if (isGenerating) MockProgress()
        if (isReady) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(topic, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("AI Visual Explanation", color = Color(0xFF9D174D))
                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().size(150.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play video preview", tint = Color.White, modifier = Modifier.size(42.dp)) }
                    }
                    Text("Water evaporates from Earth's surface and rises into the atmosphere...", color = MutedInk, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { isReady = false }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Regenerate") }
                        OutlinedButton(onClick = { }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockProgress() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Creating your visual lesson...", color = Ink, fontWeight = FontWeight.Bold)
            Text("✓ Understanding topic\n✓ Planning visual scenes\n✓ Preparing explanation\n○ Rendering video", color = MutedInk, fontSize = 13.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun FormTool(title: String, subtitle: String, prompt: String) {
    ToolPage(title, subtitle) {
        OutlinedTextField("", {}, modifier = Modifier.fillMaxWidth(), label = { Text(prompt) }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(true, {}, { Text("Simple") })
            FilterChip(false, {}, { Text("Detailed") })
            FilterChip(false, {}, { Text("Focused") })
        }
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text(if (title == "Quiz") "Generate Quiz" else "Create $title") }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Text("Your mock $title will appear here. This frontend preview is ready for a future AI connection.", Modifier.padding(16.dp), color = MutedInk, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FlashcardsTool() {
    ToolPage("Create Flashcards", "Revise key concepts quickly.") {
        OutlinedTextField("", {}, modifier = Modifier.fillMaxWidth(), label = { Text("Topic") }, placeholder = { Text("Operating Systems") })
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Create Flashcards") }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("What is a process?", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Tap to reveal answer", color = MutedInk, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    TextButton({}) { Text("Previous") }
                    Button({}) { Text("Flip") }
                    TextButton({}) { Text("Next") }
                }
            }
        }
    }
}

@Composable
private fun ReportsTool(isClass912Student: Boolean) {
    val progressLabel = if (isClass912Student) "Revision progress" else "Learning progress"
    ToolPage("Reports", "A calm snapshot of your learning progress.") {
        listOf(
            "Course completion" to "68%",
            "Topics studied" to if (isClass912Student) "24 chapters" else "18 topics",
            "Quiz activity" to "12 completed",
            progressLabel to "On track"
        ).forEach { (label, value) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = Ink, modifier = Modifier.weight(1f))
                    Text(value, color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AudioLearningTool() {
    ToolPage("Audio Learning", "Learn while listening.") {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Photosynthesis", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("A short audio lesson for your next revision session.", color = MutedInk)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = {}, shape = CircleShape, color = Primary, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play audio", tint = Color.White) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("━━━━━━──────  02:18", color = Primary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewTool() {
    ToolPage("Exam Performance Review", "Turn practice results into your next revision plan.") {
        listOf("Overall Performance" to "82%", "Strong Areas" to "Mechanics, algebra", "Needs Revision" to "Optics, electricity", "Question Analysis" to "4 concepts to revisit", "Recommended Revision" to "30 minutes today").forEach { (title, detail) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Ink, fontWeight = FontWeight.Bold)
                    Text(detail, color = MutedInk, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SimpleTool(title: String, subtitle: String, icon: ImageVector) {
    ToolPage(title, subtitle) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(42.dp))
                Text("Your next lesson is ready.", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("This learning entry point is a frontend preview and does not change the existing course system.", color = MutedInk, fontSize = 13.sp)
                Button({}) { Text("Explore learning") }
            }
        }
    }
}

@Composable
private fun ToolPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, color = MutedInk, fontSize = 14.sp)
            content()
        }
    )
}
