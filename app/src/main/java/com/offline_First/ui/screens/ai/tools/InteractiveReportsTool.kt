package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveReportsTool(
    studentName: String = "Student",
    onBack: () -> Unit
) {
    var isGenerating by remember { mutableStateOf(false) }
    var activeReport by remember { mutableStateOf<StudentLearningReport?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Auto load or generate on launch
    LaunchedEffect(Unit) {
        isGenerating = true
        delay(800)
        activeReport = DemoReportsProvider.getStudentReport(studentName)
        isGenerating = false
    }

    fun refreshReport() {
        isGenerating = true
        activeReport = null
        coroutineScope.launch {
            delay(1000)
            activeReport = DemoReportsProvider.getStudentReport(studentName)
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
                                Icon(Icons.Default.Analytics, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Learning Report", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeReport != null) activeReport!!.academicPeriod else "Performance & Diagnostics",
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
                    IconButton(onClick = { refreshReport() }) {
                        Icon(Icons.Default.Refresh, "Refresh Report", tint = EduNovaTextPrimary)
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
            // Generating / Loading State
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
                            "Synthesizing Learning Analytics...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Analyzing quiz scores, retention curves, and concept gaps.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            activeReport?.let { report ->
                // Hero Overall Score Card
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
                            Column {
                                Text(
                                    text = "LEARNER PROFILE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = report.studentName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${report.overallScore}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "INDEX",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Study Hours", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("${report.studyHoursTotal} hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Quizzes Done", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("${report.quizzesCompleted}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Cards Mastered", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("${report.flashcardsMastered}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // 2x2 Key Diagnostic Metrics Grid
                Text("Key Performance Metrics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduNovaTextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    report.metrics.chunked(2).forEach { rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowMetrics.forEach { metric ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, EduNovaBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(metric.label, fontSize = 12.sp, color = EduNovaTextSecondary)
                                        Text(metric.value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EduNovaTextPrimary)
                                        Text(
                                            text = metric.change,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (metric.isPositive) EduNovaSuccess else EduNovaAccent
                                        )
                                    }
                                }
                            }
                            if (rowMetrics.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Subject-by-Subject Mastery Breakdown
                Text("Subject Diagnostic Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduNovaTextPrimary)

                report.subjectMastery.forEach { mastery ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(16.dp),
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
                                    text = mastery.subject,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = EduNovaTextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduNovaPrimaryContainer
                                ) {
                                    Text(
                                        text = "${mastery.grade} • ${mastery.scorePercentage}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EduNovaPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { mastery.scorePercentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (mastery.scorePercentage >= 80) EduNovaPrimary else EduNovaAccent,
                                trackColor = EduNovaBorder
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Strong Area", fontSize = 11.sp, color = EduNovaTextSecondary)
                                    Text(mastery.strongArea, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextPrimary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Focus Needed", fontSize = 11.sp, color = EduNovaTextSecondary)
                                    Text(mastery.focusArea, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaAccent)
                                }
                            }
                        }
                    }
                }

                // AI Actionable Recommendations
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = EduNovaPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Study Recommendations", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduNovaTextPrimary)
                        }

                        report.aiRecommendations.forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                                Text(rec, fontSize = 13.sp, color = EduNovaTextPrimary, lineHeight = 18.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
