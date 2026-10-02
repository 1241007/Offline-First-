package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.offline_First.ui.theme.EduNovaBackground
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.theme.EduNovaSurface
import com.offline_First.ui.theme.EduNovaSurfaceVariant
import com.offline_First.ui.theme.EduNovaTextPrimary

private enum class BuilderSpeaker { MENTOR, STUDENT }

private data class BuilderMessage(
    val speaker: BuilderSpeaker,
    val text: String,
    val options: List<String> = emptyList(),
    val quiz: Boolean = false
)

private data class BuilderStage(val title: String, val detail: String)

private val RoadmapPrimary = EduNovaPrimary
private val RoadmapPrimarySoft = EduNovaPrimaryContainer
private val RoadmapSurface = EduNovaSurface
private val RoadmapSurfaceWarm = EduNovaBackground
private val RoadmapBorder = EduNovaBorder
private val RoadmapSuccess = EduNovaSuccess
private val RoadmapText = EduNovaTextPrimary

private val builderStages = listOf(
    BuilderStage("Foundations", "Build the core concepts and vocabulary."),
    BuilderStage("Guided practice", "Complete short exercises with mentor feedback."),
    BuilderStage("Applied project", "Turn your new skills into a portfolio project."),
    BuilderStage("Review and next steps", "Measure progress and keep momentum going.")
)

@Composable
fun RoadmapBuilderScreen(onBack: () -> Unit = {}) {
    val messages = remember {
        mutableStateListOf(
            BuilderMessage(
                BuilderSpeaker.MENTOR,
                "Hi! I’m your EduNova learning mentor. I’ll ask a few questions, check what you already know, and shape a roadmap around your goals."
            ),
            BuilderMessage(
                BuilderSpeaker.MENTOR,
                "What would you like to achieve next?",
                options = listOf("Get a job", "Build a project", "Prepare for an exam")
            )
        )
    }
    var step by remember { mutableStateOf(1) }
    var isTyping by remember { mutableStateOf(false) }
    var currentOptions by remember { mutableStateOf(messages.last().options) }
    var draft by remember { mutableStateOf("") }
    var roadmapExpanded by remember { mutableStateOf(true) }
    var completed by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf("Get a job") }
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var selectedTime by remember { mutableStateOf("1 hour/day") }

    fun answer(value: String) {
        if (isTyping || completed) return
        selectedGoal = if (step == 1) value else selectedGoal
        selectedLevel = if (step == 2) value else selectedLevel
        selectedTime = if (step == 3) value else selectedTime
        messages.add(BuilderMessage(BuilderSpeaker.STUDENT, value))
        currentOptions = emptyList()
        isTyping = true
    }

    LaunchedEffect(isTyping) {
        if (!isTyping) return@LaunchedEffect
        delay(650)
        when (step) {
            1 -> {
                messages.add(
                    BuilderMessage(
                        BuilderSpeaker.MENTOR,
                        "Great. What best describes your current level?",
                        options = listOf("Beginner", "Some experience", "Advanced")
                    )
                )
                step = 2
            }
            2 -> {
                messages.add(
                    BuilderMessage(
                        BuilderSpeaker.MENTOR,
                        "How much time can you make available for learning most days?",
                        options = listOf("30 minutes", "1 hour", "2+ hours")
                    )
                )
                step = 3
            }
            3 -> {
                messages.add(
                    BuilderMessage(
                        BuilderSpeaker.MENTOR,
                        "Quick knowledge check: which practice usually helps you retain a new concept best?",
                        options = listOf("Explain it in my own words", "Read it once", "Skip practice"),
                        quiz = true
                    )
                )
                step = 4
            }
            4 -> {
                messages.add(
                    BuilderMessage(
                        BuilderSpeaker.MENTOR,
                        "Nice work. I’ll use your current roadmap as context and prioritize practical, bite-sized progress.",
                        options = listOf("Create my roadmap")
                    )
                )
                step = 5
            }
            else -> {
                completed = true
                messages.add(
                    BuilderMessage(
                        BuilderSpeaker.MENTOR,
                        "Your personalized roadmap is ready. You can adjust it any time as your goals change."
                    )
                )
                step = 6
            }
        }
        currentOptions = messages.last().options
        isTyping = false
    }

    fun sendDraft() {
        val value = draft.trim()
        if (value.isNotEmpty()) {
            answer(value)
            draft = ""
        }
    }

    Scaffold(
        containerColor = RoadmapSurfaceWarm,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 4.dp, end = 16.dp, bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to roadmap"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Build My Roadmap",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Personalized learning mentor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Step $step of 6",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoadmapPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                LinearProgressIndicator(
                    progress = { step / 6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, top = 6.dp),
                    color = RoadmapPrimary,
                    trackColor = RoadmapPrimarySoft
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RoadmapPrimarySoft
                ),
                border = BorderStroke(1.dp, RoadmapBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roadmapExpanded = !roadmapExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Roadmap", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            "Android Developer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            if (roadmapExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            "Toggle current roadmap",
                            tint = RoadmapPrimary
                        )
                    }
                    if (roadmapExpanded) {
                        Text(
                            "Kotlin • Compose • UI fundamentals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                if (isTyping) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = RoadmapPrimary
                            )
                            Text(
                                "EduNova is thinking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                if (completed) {
                    item {
                        RoadmapResult(
                            goal = selectedGoal,
                            level = selectedLevel,
                            time = selectedTime
                        )
                    }
                }
            }

            if (currentOptions.isNotEmpty() && !isTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentOptions.forEach { option ->
                        OutlinedButton(
                            onClick = { answer(option) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RoadmapBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RoadmapPrimary
                            )
                        ) {
                            Text(option, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tell your mentor anything...") },
                    singleLine = true,
                    enabled = !isTyping && !completed,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoadmapPrimary,
                        unfocusedBorderColor = RoadmapBorder,
                        cursorColor = RoadmapPrimary
                    )
                )
                IconButton(
                    onClick = ::sendDraft,
                    enabled = draft.isNotBlank() && !isTyping && !completed
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = RoadmapPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: BuilderMessage) {
    val mentor = message.speaker == BuilderSpeaker.MENTOR
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mentor) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mentor) 4.dp else 18.dp,
                bottomEnd = if (mentor) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (mentor) RoadmapSurface else RoadmapPrimarySoft
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    if (mentor) "EduNova Mentor" else "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoadmapPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message.text,
                    color = RoadmapText,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (message.quiz) {
                    Text(
                        "Knowledge check",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mentor) MaterialTheme.colorScheme.onSurfaceVariant else RoadmapPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoadmapResult(goal: String, level: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = RoadmapSurfaceWarm),
        border = BorderStroke(1.dp, RoadmapBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Your personalized roadmap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "$goal • $level • $time",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadmapPrimary,
                fontWeight = FontWeight.SemiBold
            )
            builderStages.forEachIndexed { index, stage ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(RoadmapSuccess.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = RoadmapSuccess, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(stage.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            stage.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoadmapPrimary)
            ) {
                Text("Start learning")
            }
            TextButton(onClick = {}) {
                Text("Adjust my answers")
            }
        }
    }
}
