package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
fun InteractiveImageAnalysisTool(
    initialQuery: String = "",
    onBack: () -> Unit
) {
    var selectedProblem by remember { mutableStateOf(initialQuery) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var activeResult by remember { mutableStateOf<ImageAnalysisResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun runAnalysis(problem: String) {
        val target = problem.ifBlank { "Physics: Pulley & Incline Mechanics" }
        isAnalyzing = true
        activeResult = null

        coroutineScope.launch {
            delay(1400) // Simulated multimodal OCR & reasoning latency
            activeResult = DemoImageAnalysisProvider.analyzeProblem(target)
            isAnalyzing = false
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
                            color = EduNovaSecondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, null, tint = EduNovaSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Image & Problem Solver", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeResult != null) activeResult!!.subject else "Multimodal Homework Diagnostics",
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
                    if (activeResult != null) {
                        IconButton(onClick = { activeResult = null }) {
                            Icon(Icons.Default.Refresh, "Scan Another", tint = EduNovaTextPrimary)
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
            // Scanner / Selection Card
            if (activeResult == null && !isAnalyzing) {
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
                            Icon(Icons.Default.DocumentScanner, null, tint = EduNovaSecondary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Scan Textbook Question or Diagram",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "EduNova Multimodal AI extracts mathematical formulas, parses physics diagrams, and produces complete step-by-step solutions.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        // Sample Document Scanner View
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EduNovaBackground,
                            border = BorderStroke(1.5.dp, EduNovaSecondary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clickable {
                                    runAnalysis(selectedProblem)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = EduNovaSecondary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tap to scan textbook diagram or upload image",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = EduNovaTextPrimary
                                    )
                                    Text(
                                        "Supports Mechanics, Circuits, Integrals & Chemistry",
                                        fontSize = 11.sp,
                                        color = EduNovaTextSecondary
                                    )
                                }
                            }
                        }

                        Text("Or select sample textbook question to test:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoImageAnalysisProvider.sampleProblems.forEach { problem ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selectedProblem == problem) EduNovaSecondaryContainer else EduNovaBackground,
                                    border = BorderStroke(1.dp, if (selectedProblem == problem) EduNovaSecondary else EduNovaBorder),
                                    modifier = Modifier.clickable {
                                        selectedProblem = problem
                                        runAnalysis(problem)
                                    }
                                ) {
                                    Text(
                                        text = problem,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedProblem == problem) EduNovaSecondary else EduNovaTextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { runAnalysis(selectedProblem) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze & Solve with AI", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Generating / Scanning State
            if (isAnalyzing) {
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
                        CircularProgressIndicator(color = EduNovaSecondary, strokeWidth = 3.dp)
                        Text(
                            "Multimodal Vision AI Processing...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Extracting vector geometry, symbolic equations, and constructing step-by-step mathematical proof.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Solved Result UI
            activeResult?.let { res ->
                // Hero Problem Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduNovaSecondaryContainer
                        ) {
                            Text(
                                text = "EXTRACTED PROBLEM • ${res.subject}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = res.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )

                        Text(
                            text = res.problemStatement,
                            fontSize = 14.sp,
                            color = EduNovaTextPrimary,
                            lineHeight = 20.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EduNovaBackground,
                            border = BorderStroke(1.dp, EduNovaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Functions, null, tint = EduNovaSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = res.extractedDiagramOrFormula,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EduNovaSecondary
                                )
                            }
                        }
                    }
                }

                // Step-by-Step Solution Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, EduNovaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, null, tint = EduNovaPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Step-by-Step Solution Derivation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EduNovaTextPrimary)
                        }

                        res.stepByStepSolution.forEach { stepText ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduNovaBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stepText,
                                    fontSize = 13.sp,
                                    color = EduNovaTextPrimary,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Final Answer Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaPrimary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FINAL ANSWER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = res.finalAnswer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Pro Tip Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EduNovaAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Concept Tip", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EduNovaTextPrimary)
                            Text(res.proTip, fontSize = 12.sp, color = EduNovaTextSecondary, lineHeight = 16.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
