package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
fun InteractivePyqExamTool(
    initialChapter: String = "",
    onBack: () -> Unit
) {
    var chapterQuery by remember { mutableStateOf(initialChapter) }
    var isLoading by remember { mutableStateOf(false) }
    var activeChapterData by remember { mutableStateOf<PyqChapter?>(null) }
    val revealedAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    fun loadChapter(chapter: String) {
        val target = chapter.ifBlank { "Physics: Electricity & Circuits" }
        isLoading = true
        activeChapterData = null
        revealedAnswers.clear()

        coroutineScope.launch {
            delay(1000)
            activeChapterData = DemoPyqProvider.getPyqsForChapter(target)
            isLoading = false
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
                            color = EduNovaPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.School, null, tint = EduNovaPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("CBSE PYQ & Exam Vault", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeChapterData != null) activeChapterData!!.subject else "Solved Past Year Questions",
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
                    if (activeChapterData != null) {
                        IconButton(onClick = { activeChapterData = null }) {
                            Icon(Icons.Default.Refresh, "Switch Chapter", tint = EduNovaTextPrimary)
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
            // Chapter Selector Card
            if (activeChapterData == null && !isLoading) {
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
                            Icon(Icons.Default.Verified, null, tint = EduNovaPrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CBSE Past 10-Year Question Bank",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "Official board questions with step-by-step marking schemes, mark distributions, and examiner tips.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        OutlinedTextField(
                            value = chapterQuery,
                            onValueChange = { chapterQuery = it },
                            placeholder = { Text("e.g. Electricity, Light, Chemical Reactions...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduNovaPrimary,
                                unfocusedBorderColor = EduNovaBorder
                            )
                        )

                        Text("Select chapter:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoPyqProvider.sampleChapters.forEach { ch ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (chapterQuery == ch) EduNovaPrimaryContainer else EduNovaBackground,
                                    border = BorderStroke(1.dp, if (chapterQuery == ch) EduNovaPrimary else EduNovaBorder),
                                    modifier = Modifier.clickable {
                                        chapterQuery = ch
                                        loadChapter(ch)
                                    }
                                ) {
                                    Text(
                                        text = ch,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (chapterQuery == ch) EduNovaPrimary else EduNovaTextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { loadChapter(chapterQuery) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Chapter PYQs", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Loading state
            if (isLoading) {
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
                            "Retrieving Board Questions...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Filtering official CBSE past year papers, marking schemes, and step allocations.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Chapter PYQ List
            activeChapterData?.let { chapter ->
                // Hero Chapter Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaPrimary),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = "CBSE EXAM VAULT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = chapter.totalWeightage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = chapter.chapterTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Practice questions strictly matched with official CBSE Board answer evaluations.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Text("Frequently Asked Board Questions", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EduNovaTextPrimary)

                chapter.questions.forEach { pyq ->
                    val isRevealed = revealedAnswers[pyq.id] == true

                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduNovaSecondaryContainer
                                ) {
                                    Text(
                                        text = pyq.year,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduNovaSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EduNovaPrimaryContainer
                                ) {
                                    Text(
                                        text = "${pyq.marks} Marks",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduNovaPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = pyq.questionText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduNovaTextPrimary,
                                lineHeight = 21.sp
                            )

                            // Solution Drawer
                            AnimatedVisibility(visible = isRevealed, enter = fadeIn() + slideInVertically()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HorizontalDivider(color = EduNovaBorder, thickness = 1.dp)

                                    Text("Official Marking Scheme & Step Breakdown:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EduNovaTextPrimary)

                                    pyq.markingSchemeSteps.forEach { step ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EduNovaBackground,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = step,
                                                fontSize = 12.sp,
                                                color = EduNovaTextPrimary,
                                                lineHeight = 17.sp,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("Final Answer:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                                            Text(pyq.finalAnswer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { revealedAnswers[pyq.id] = !isRevealed },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isRevealed) "Hide Marking Scheme" else "View Solved Marking Scheme")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
