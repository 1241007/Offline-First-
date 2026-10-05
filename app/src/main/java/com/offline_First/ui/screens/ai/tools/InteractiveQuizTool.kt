package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
fun InteractiveQuizTool(
    initialTopic: String = "",
    onBack: () -> Unit
) {
    var topicInput by remember { mutableStateOf(initialTopic) }
    var isGenerating by remember { mutableStateOf(false) }
    var activeQuiz by remember { mutableStateOf<QuizTopic?>(null) }
    
    // Quiz play state
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun startGenerating(topic: String) {
        val targetTopic = topic.ifBlank { "Newton's Laws of Motion" }
        isGenerating = true
        activeQuiz = null
        quizCompleted = false
        currentQuestionIndex = 0
        selectedOptionIndex = null
        isAnswerSubmitted = false
        score = 0
        
        coroutineScope.launch {
            // Realistic AI generation delay for demo presentation
            delay(1200)
            activeQuiz = DemoQuizProvider.getQuizForTopic(targetTopic)
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
                                Icon(Icons.Default.Quiz, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Quiz Generator", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeQuiz != null) activeQuiz!!.subject else "Interactive Assessment",
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
            // Generation / Topic Input Card (collapsible or shown when no quiz is running)
            if (activeQuiz == null && !isGenerating) {
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
                                "Generate Custom Quiz",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "Type any chapter, concept, or curriculum topic to test your knowledge.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            placeholder = { Text("e.g. Newton's Laws, Photosynthesis, Python...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduNovaPrimary,
                                unfocusedBorderColor = EduNovaBorder
                            )
                        )

                        Text("Popular quick topics:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoQuizProvider.sampleTopics.forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (topicInput == topic) EduNovaPrimaryContainer else EduNovaBackground,
                                    border = BorderStroke(1.dp, if (topicInput == topic) EduNovaPrimary else EduNovaBorder),
                                    modifier = Modifier.clickable {
                                        topicInput = topic
                                        startGenerating(topic)
                                    }
                                ) {
                                    Text(
                                        text = topic,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (topicInput == topic) EduNovaPrimary else EduNovaTextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { startGenerating(topicInput) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Quiz with AI", fontWeight = FontWeight.SemiBold)
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
                            "AI is building your quiz...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Synthesizing questions, multi-choice options, and pedagogical explanations.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Active Quiz Flow
            activeQuiz?.let { quiz ->
                if (!quizCompleted) {
                    val currentQ = quiz.questions[currentQuestionIndex]
                    val totalQ = quiz.questions.size

                    // Progress & Header
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
                                Surface(
                                    color = EduNovaPrimaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Question ${currentQuestionIndex + 1} of $totalQ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EduNovaPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "Score: $score",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = EduNovaTextSecondary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / totalQ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EduNovaPrimary,
                                trackColor = EduNovaBorder
                            )
                        }
                    }

                    // Question Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentQ.question,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextPrimary,
                                lineHeight = 24.sp
                            )

                            // 4 Options
                            currentQ.options.forEachIndexed { index, optionText ->
                                val isSelected = selectedOptionIndex == index
                                val isCorrect = index == currentQ.correctIndex

                                val containerColor = when {
                                    isAnswerSubmitted && isCorrect -> Color(0xFFE8F5E9) // Light green
                                    isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE) // Light red
                                    isSelected -> EduNovaPrimaryContainer
                                    else -> EduNovaBackground
                                }

                                val borderColor = when {
                                    isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
                                    isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFE57373)
                                    isSelected -> EduNovaPrimary
                                    else -> EduNovaBorder
                                }

                                Surface(
                                    onClick = {
                                        if (!isAnswerSubmitted) {
                                            selectedOptionIndex = index
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = containerColor,
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
                                                isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFE57373)
                                                isSelected -> EduNovaPrimary
                                                else -> Color.Transparent
                                            },
                                            border = if (!isSelected && (!isAnswerSubmitted || !isCorrect)) BorderStroke(1.5.dp, EduNovaTextSecondary) else null,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isAnswerSubmitted && isCorrect) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                } else if (isAnswerSubmitted && isSelected && !isCorrect) {
                                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                } else if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = optionText,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = EduNovaTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Explanation Card when answered
                            AnimatedVisibility(
                                visible = isAnswerSubmitted,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = EduNovaBackground),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, EduNovaBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Lightbulb, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("Explanation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EduNovaTextPrimary)
                                            Spacer(Modifier.height(2.dp))
                                            Text(currentQ.explanation, fontSize = 12.sp, color = EduNovaTextSecondary, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }

                            // Submit or Next Button
                            if (!isAnswerSubmitted) {
                                Button(
                                    onClick = {
                                        if (selectedOptionIndex != null) {
                                            isAnswerSubmitted = true
                                            if (selectedOptionIndex == currentQ.correctIndex) {
                                                score += 1
                                            }
                                        }
                                    },
                                    enabled = selectedOptionIndex != null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit Answer", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (currentQuestionIndex < totalQ - 1) {
                                            currentQuestionIndex += 1
                                            selectedOptionIndex = null
                                            isAnswerSubmitted = false
                                        } else {
                                            quizCompleted = true
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (currentQuestionIndex < totalQ - 1) "Next Question →" else "View Results 🎉",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Quiz Result Screen
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EduNovaSuccess.copy(alpha = 0.15f),
                                modifier = Modifier.size(70.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = EduNovaSuccess, modifier = Modifier.size(38.dp))
                                }
                            }

                            Text(
                                text = "Quiz Completed!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextPrimary
                            )

                            val percentage = ((score.toFloat() / quiz.questions.size) * 100).toInt()
                            Text(
                                text = "You scored $score out of ${quiz.questions.size} ($percentage%)",
                                fontSize = 15.sp,
                                color = EduNovaTextSecondary
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduNovaBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = when {
                                            percentage >= 80 -> "🌟 Outstanding mastery! You're ready for exam questions."
                                            percentage >= 50 -> "👍 Good effort! Review the explanations to solidify concepts."
                                            else -> "📚 Keep practicing! Re-read the chapter summary and try again."
                                        },
                                        fontSize = 13.sp,
                                        color = EduNovaTextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        currentQuestionIndex = 0
                                        selectedOptionIndex = null
                                        isAnswerSubmitted = false
                                        score = 0
                                        quizCompleted = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }

                                Button(
                                    onClick = {
                                        activeQuiz = null
                                        quizCompleted = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("New Quiz")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
