package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveStudyPlannerTool(
    initialGoal: String = "",
    onBack: () -> Unit
) {
    var goalInput by remember { mutableStateOf(initialGoal) }
    var isGenerating by remember { mutableStateOf(false) }
    var activePlan by remember { mutableStateOf<StudyPlanData?>(null) }
    val taskCompletionState = remember { mutableStateMapOf<String, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    fun generatePlan(goal: String) {
        val targetGoal = goal.ifBlank { "CBSE Class 10 Board Science (7-Day Sprint)" }
        isGenerating = true
        activePlan = null
        taskCompletionState.clear()

        coroutineScope.launch {
            delay(1200)
            val plan = DemoStudyPlannerProvider.getPlanForGoal(targetGoal)
            activePlan = plan
            // Initialize completed tasks
            plan.days.flatMap { it.tasks }.forEach { task ->
                taskCompletionState[task.id] = task.isDone
            }
            isGenerating = false
        }
    }

    Scaffold(
        containerColor = EduNovaBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EduNovaAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Study Planner", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activePlan != null) activePlan!!.targetExam else "Adaptive Daily Timetable",
                                fontSize = 12.sp,
                                color = EduNovaTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EduNovaTextPrimary)
                    }
                },
                actions = {
                    if (activePlan != null) {
                        IconButton(onClick = { activePlan = null }) {
                            Icon(Icons.Default.Refresh, "New Plan", tint = EduNovaTextPrimary)
                        }
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Goal Card
            if (activePlan == null && !isGenerating) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = EduNovaPrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Generate Personalized Study Plan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "Tell AI your target exam or learning goal. Get an optimized day-by-day timetable with theory, practice, and mock tests.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            placeholder = { Text("e.g. CBSE Class 10 Science, Python Backend, Physics...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduNovaPrimary,
                                unfocusedBorderColor = EduNovaBorder
                            )
                        )

                        Text("Sample study goals:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoStudyPlannerProvider.sampleGoals.forEach { goal ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (goalInput == goal) EduNovaPrimaryContainer else EduNovaBackground,
                                    border = BorderStroke(1.dp, if (goalInput == goal) EduNovaPrimary else EduNovaBorder),
                                    modifier = Modifier.clickable {
                                        goalInput = goal
                                        generatePlan(goal)
                                    }
                                ) {
                                    Text(
                                        text = goal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (goalInput == goal) EduNovaPrimary else EduNovaTextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { generatePlan(goalInput) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate AI Study Timetable", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Generating Loading State
            if (isGenerating) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = EduNovaPrimary, strokeWidth = 3.dp)
                        Text(
                            "AI is building your study timetable...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Balancing cognitive load, revision intervals, and daily hour budgets.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Active Plan UI
            activePlan?.let { plan ->
                val allTasks = plan.days.flatMap { it.tasks }
                val completedCount = allTasks.count { taskCompletionState[it.id] == true }
                val totalCount = allTasks.size
                val currentPercentage = if (totalCount > 0) ((completedCount.toFloat() / totalCount) * 100).toInt() else 0

                // Hero Plan Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaPrimary),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${plan.totalDays}-DAY SPRINT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "${plan.dailyHours} hrs / day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = plan.goalTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "“${plan.motivationalQuote}”",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

                        // Overall Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Milestones Done: $completedCount of $totalCount",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$currentPercentage%",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { currentPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Day-by-Day Plan Cards
                Text("Your Day-by-Day Schedule", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EduNovaTextPrimary)

                plan.days.forEach { day ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
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
                                    text = day.dayLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = EduNovaTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduNovaPrimaryContainer
                                ) {
                                    Text(
                                        text = "${day.tasks.count { taskCompletionState[it.id] == true }}/${day.tasks.size} Done",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduNovaPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Focus: ${day.focusArea}",
                                fontSize = 12.sp,
                                color = EduNovaTextSecondary
                            )

                            HorizontalDivider(color = EduNovaBorder, thickness = 0.8.dp)

                            // Task Checklist
                            day.tasks.forEach { task ->
                                val isDone = taskCompletionState[task.id] == true

                                Surface(
                                    onClick = {
                                        taskCompletionState[task.id] = !isDone
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDone) Color(0xFFE8F5E9) else EduNovaBackground,
                                    border = BorderStroke(1.dp, if (isDone) Color(0xFF4CAF50) else EduNovaBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isDone,
                                            onCheckedChange = { taskCompletionState[task.id] = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF4CAF50),
                                                uncheckedColor = EduNovaTextSecondary
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDone) Color(0xFF2E7D32) else EduNovaTextPrimary
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = task.timeSlot,
                                                    fontSize = 11.sp,
                                                    color = EduNovaTextSecondary
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (task.taskType) {
                                                        "Quiz" -> EduNovaAccent.copy(alpha = 0.15f)
                                                        "Practice" -> EduNovaSecondary.copy(alpha = 0.15f)
                                                        "PYQ" -> EduNovaPrimary.copy(alpha = 0.15f)
                                                        else -> EduNovaPrimaryContainer
                                                    }
                                                ) {
                                                    Text(
                                                        text = task.taskType,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EduNovaTextPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
