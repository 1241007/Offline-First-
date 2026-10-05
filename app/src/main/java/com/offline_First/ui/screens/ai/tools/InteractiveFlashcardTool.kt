package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveFlashcardTool(
    initialTopic: String = "",
    onBack: () -> Unit
) {
    var topicInput by remember { mutableStateOf(initialTopic) }
    var isGenerating by remember { mutableStateOf(false) }
    var activeDeck by remember { mutableStateOf<FlashcardDeck?>(null) }

    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var masteredCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flashcard_flip"
    )

    fun startGenerating(topic: String) {
        val targetTopic = topic.ifBlank { "Physics Core Laws" }
        isGenerating = true
        activeDeck = null
        currentCardIndex = 0
        isFlipped = false
        showHint = false
        masteredCount = 0

        coroutineScope.launch {
            delay(1200)
            activeDeck = DemoFlashcardProvider.getDeckForTopic(targetTopic)
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
                                Icon(Icons.Default.Style, null, tint = EduNovaAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Flashcard Deck", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeDeck != null) activeDeck!!.subject else "Active Recall",
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
            // Deck Generator Input
            if (activeDeck == null && !isGenerating) {
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
                                "Generate Flashcards with AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "Create active-recall flashcard decks tailored to key concepts, formulas, or terminology.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            placeholder = { Text("e.g. Physics Core Laws, Python, Calculus...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduNovaPrimary,
                                unfocusedBorderColor = EduNovaBorder
                            )
                        )

                        Text("Quick deck topics:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoFlashcardProvider.sampleTopics.forEach { topic ->
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
                            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Flashcard Deck", fontWeight = FontWeight.SemiBold)
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
                            "AI is crafting flashcards...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Extracting high-yield concepts, definitions, and active-recall cues.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Active Flashcard Deck View
            activeDeck?.let { deck ->
                val card = deck.cards[currentCardIndex]
                val totalCards = deck.cards.size

                // Progress Bar & Counter Card
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
                                    text = "Card ${currentCardIndex + 1} of $totalCards",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EduNovaPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = EduNovaSuccess, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Mastered: $masteredCount",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = EduNovaTextSecondary
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (currentCardIndex + 1).toFloat() / totalCards },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EduNovaPrimary,
                            trackColor = EduNovaBorder
                        )
                    }
                }

                // Interactive Flippable 3D Flashcard
                Card(
                    onClick = {
                        isFlipped = !isFlipped
                        showHint = false
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) Color(0xFFF1F8E9) else EduNovaSurface // Subtle light green on answer
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, if (isFlipped) EduNovaSuccess.copy(alpha = 0.5f) else EduNovaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer {
                                // Prevent mirrored text when flipped
                                if (rotation > 90f) {
                                    rotationY = 180f
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Category Tag & Flip Hint
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isFlipped) EduNovaSuccess.copy(alpha = 0.15f) else EduNovaSecondaryContainer
                                ) {
                                    Text(
                                        text = if (isFlipped) "ANSWER • ${card.category}" else "QUESTION • ${card.category}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFlipped) EduNovaSuccess else EduNovaSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TouchApp, null, tint = EduNovaTextSecondary, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tap to flip", fontSize = 11.sp, color = EduNovaTextSecondary)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Main Content (Question or Answer)
                            Text(
                                text = if (isFlipped) card.answer else card.question,
                                fontSize = 17.sp,
                                fontWeight = if (isFlipped) FontWeight.Medium else FontWeight.Bold,
                                color = EduNovaTextPrimary,
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Start
                            )

                            // Optional Hint (Front side only)
                            if (!isFlipped) {
                                AnimatedVisibility(visible = showHint, enter = fadeIn()) {
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
                                            Icon(Icons.Default.Lightbulb, null, tint = EduNovaAccent, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Hint: ${card.hint}",
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

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFlipped) {
                        TextButton(
                            onClick = { showHint = !showHint }
                        ) {
                            Icon(Icons.Default.Lightbulb, null, tint = EduNovaAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (showHint) "Hide Hint" else "Show Hint", color = EduNovaTextPrimary, fontSize = 13.sp)
                        }
                    } else {
                        // Mark as mastered button when flipped
                        OutlinedButton(
                            onClick = {
                                masteredCount += 1
                                if (currentCardIndex < totalCards - 1) {
                                    currentCardIndex += 1
                                    isFlipped = false
                                    showHint = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EduNovaSuccess)
                        ) {
                            Icon(Icons.Default.Check, null, tint = EduNovaSuccess, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark Mastered", color = EduNovaSuccess, fontSize = 13.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                if (currentCardIndex > 0) {
                                    currentCardIndex -= 1
                                    isFlipped = false
                                    showHint = false
                                }
                            },
                            enabled = currentCardIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Card")
                        }

                        Button(
                            onClick = {
                                if (currentCardIndex < totalCards - 1) {
                                    currentCardIndex += 1
                                    isFlipped = false
                                    showHint = false
                                } else {
                                    // Finished deck
                                    currentCardIndex = 0
                                    isFlipped = false
                                    showHint = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary)
                        ) {
                            Text(
                                if (currentCardIndex < totalCards - 1) "Next Card →" else "Restart Deck 🔄",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
