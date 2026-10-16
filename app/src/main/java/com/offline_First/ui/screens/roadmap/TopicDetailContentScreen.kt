    package com.offline_First.ui.screens.roadmap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.RoadmapLessonTopic
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.*

/**
 * Screen 4: Dedicated Topic Details and Lesson Content Screen.
 * Displays readable explanations, objectives, code examples, previous/next controls,
 * completion toggles, and AI Tutor trigger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailContentScreen(
    topic: RoadmapLessonTopic,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onBack: () -> Unit,
    onNextTopic: (() -> Unit)? = null,
    onPrevTopic: (() -> Unit)? = null,
    onAskAiAboutTopic: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to roadmap",
                            tint = EduNovaTextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Python Developer Roadmap",
                            style = MaterialTheme.typography.labelSmall,
                            color = EduNovaPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            onAskAiAboutTopic("Explain the concept of '${topic.title}' in Python with simple examples.")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI Tutor about this topic",
                            tint = EduNovaPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPrevTopic != null) {
                        OutlinedButton(
                            onClick = onPrevTopic,
                            shape = RoundedCornerShape(EduNovaShapes.button),
                            border = BorderStroke(1.dp, EduNovaBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Previous", color = EduNovaTextPrimary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    Button(
                        onClick = onToggleComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) EduNovaSuccess else EduNovaPrimary
                        ),
                        shape = RoundedCornerShape(EduNovaShapes.button),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Completed", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Mark as Complete", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onNextTopic != null) {
                        OutlinedButton(
                            onClick = onNextTopic,
                            shape = RoundedCornerShape(EduNovaShapes.button),
                            border = BorderStroke(1.dp, EduNovaPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EduNovaPrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info & Time Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EduNovaBadge(
                    text = "${topic.estimatedMinutes} Minutes",
                    containerColor = EduNovaPrimaryContainer,
                    contentColor = EduNovaPrimary,
                    icon = Icons.Default.Timer
                )
                if (isCompleted) {
                    EduNovaBadge(
                        text = "Topic Completed",
                        containerColor = EduNovaSuccess.copy(alpha = 0.15f),
                        contentColor = EduNovaSuccess,
                        icon = Icons.Default.Check
                    )
                }
            }

            Text(
                text = topic.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EduNovaTextPrimary
            )

            Text(
                text = topic.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = EduNovaTextSecondary,
                lineHeight = 24.sp
            )

            // Learning Objectives Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EduNovaSurface,
                shape = RoundedCornerShape(EduNovaShapes.card),
                border = BorderStroke(1.dp, EduNovaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What You'll Learn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    topic.objectives.forEach { obj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .padding(top = 6.dp)
                                    .clip(CircleShape)
                                    .background(EduNovaPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = obj,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EduNovaTextPrimary
                            )
                        }
                    }
                }
            }

            // In-Depth Explanation
            Text(
                text = "Concept Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EduNovaTextPrimary
            )

            Text(
                text = topic.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = EduNovaTextSecondary,
                lineHeight = 22.sp
            )

            // Code Example Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Python 3 Code Example",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EduNovaTextPrimary
                )
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Python Code", topic.codeSnippet)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(16.dp),
                        tint = EduNovaPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = EduNovaPrimary
                    )
                }
            }

            // Code Block Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E2E), // Modern code editor dark slate
                border = BorderStroke(1.dp, Color(0xFF313244))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = topic.codeSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFCDD6F4), // Clean syntax off-white
                        lineHeight = 20.sp
                    )
                }
            }

            // Key Takeaways Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EduNovaPrimaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(EduNovaShapes.card),
                border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Key Takeaways",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EduNovaPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    topic.keyTakeaways.forEach { takeaway ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("✓  ", color = EduNovaPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = takeaway,
                                style = MaterialTheme.typography.bodySmall,
                                color = EduNovaTextPrimary
                            )
                        }
                    }
                }
            }

            // AI Tutor Ask Action Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(EduNovaShapes.card))
                    .border(1.dp, EduNovaBorder, RoundedCornerShape(EduNovaShapes.card)),
                color = EduNovaSurface,
                shape = RoundedCornerShape(EduNovaShapes.card)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EduNovaPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confused about ${topic.title}?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            text = "Ask EduNova's AI Tutor for step-by-step guidance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EduNovaTextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            onAskAiAboutTopic("Explain the concept of '${topic.title}' in Python with step-by-step examples and common beginner gotchas.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Ask AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
