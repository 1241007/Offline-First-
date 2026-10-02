package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoadmapPageBackground = Color(0xFFF8FAFC)
private val RoadmapCard = Color.White
private val RoadmapBorder = Color(0xFFE2E8F0)
private val RoadmapMuted = Color(0xFF64748B)
private val RoadmapInk = Color(0xFF0F172A)
private val RoadmapPrimary = Color(0xFF2563EB)
private val RoadmapPrimarySoft = Color(0xFFEFF6FF)
private val RoadmapAccent = Color(0xFF14B8A6)
private val RoadmapSuccess = Color(0xFF22C55E)
private val RoadmapWarning = Color(0xFFF59E0B)
private val RoadmapGhost = Color(0xFFF1F5F9)

private data class RoadmapSummary(
    val label: String,
    val value: String,
    val tone: Color = RoadmapPrimary
)

private data class RoadmapStage(
    val number: Int,
    val title: String,
    val description: String,
    val topics: List<String>,
    val duration: String,
    val status: StageStatus,
    val isCurrent: Boolean = false
)

private enum class StageStatus {
    COMPLETED,
    IN_PROGRESS,
    UPCOMING,
    LOCKED
}

@Composable
fun RoadmapScreen(onBack: () -> Unit = {}) {
    val summary = listOf(
        RoadmapSummary("Goal", "Data Scientist", RoadmapPrimary),
        RoadmapSummary("Level", "Intermediate", RoadmapAccent),
        RoadmapSummary("Study time", "1–2 hrs/day", RoadmapWarning),
        RoadmapSummary("Focus", "Python • SQL", RoadmapPrimary)
    )

    val stages = listOf(
        RoadmapStage(
            number = 1,
            title = "Foundation",
            description = "Build your core concepts and confidence in Python and logic.",
            topics = listOf("Python basics", "Variables", "Functions", "OOP"),
            duration = "2 weeks",
            status = StageStatus.COMPLETED
        ),
        RoadmapStage(
            number = 2,
            title = "Core Skills",
            description = "Practice data handling, SQL queries, and analytical thinking.",
            topics = listOf("SQL", "Pandas", "Data cleaning", "Analysis"),
            duration = "3 weeks",
            status = StageStatus.IN_PROGRESS,
            isCurrent = true
        ),
        RoadmapStage(
            number = 3,
            title = "Projects",
            description = "Apply your knowledge to complete guided mini-projects.",
            topics = listOf("Dashboards", "Case studies", "Presentations"),
            duration = "4 weeks",
            status = StageStatus.UPCOMING
        ),
        RoadmapStage(
            number = 4,
            title = "Advanced",
            description = "Prepare for machine learning, modeling, and deployment workflows.",
            topics = listOf("ML basics", "Model eval", "Feature engineering"),
            duration = "5 weeks",
            status = StageStatus.LOCKED
        ),
        RoadmapStage(
            number = 5,
            title = "Career Ready",
            description = "Package your portfolio, interview prep, and job-market readiness.",
            topics = listOf("Portfolio", "Git", "Technical stories"),
            duration = "2 weeks",
            status = StageStatus.LOCKED
        )
    )

    Scaffold(
        containerColor = RoadmapPageBackground,
        topBar = {
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RoadmapInk
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Roadmap",
                        color = RoadmapInk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Learning Roadmap",
                        color = RoadmapInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Text(
                        text = "Built around your goals, pace, and current level.",
                        color = RoadmapMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Surface(
                    color = RoadmapPrimarySoft,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, RoadmapPrimary.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = "Personalized for you",
                        color = RoadmapPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RoadmapCard),
                border = BorderStroke(1.dp, RoadmapBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(RoadmapPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your roadmap is ready",
                                color = RoadmapInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Based on your goals and current progress.",
                                color = RoadmapMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        text = "I’ve mapped your path around your current level, study rhythm, and target career. We’ll keep refining it as you finish each milestone.",
                        color = RoadmapInk,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoadmapPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Create my roadmap", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoadmapPrimarySoft),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Update my goals", color = RoadmapPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, RoadmapBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profile snapshot",
                        color = RoadmapInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        summary.forEach { item ->
                            SummaryChip(item, Modifier.weight(1f))
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RoadmapCard),
                border = BorderStroke(1.dp, RoadmapBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your journey",
                            color = RoadmapInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "4 of 12 milestones done",
                            color = RoadmapPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Foundation", "Core Skills", "Projects", "Advanced", "Goal").forEachIndexed { index, item ->
                            Surface(
                                color = if (index <= 2) RoadmapPrimarySoft else RoadmapGhost,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, if (index <= 2) RoadmapPrimary.copy(alpha = 0.18f) else RoadmapBorder)
                            ) {
                                Text(
                                    text = item,
                                    color = if (index <= 2) RoadmapPrimary else RoadmapMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (index <= 2) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MiniStat("Today", "2 lessons", Modifier.weight(1f))
                        MiniStat("ETA", "~10 weeks", Modifier.weight(1f))
                        MiniStat("Current", "Core Skills", Modifier.weight(1f))
                    }
                }
            }

            Text(
                text = "Roadmap timeline",
                color = RoadmapInk,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                stages.forEachIndexed { index, stage ->
                    RoadmapStageCard(stage = stage, isLast = index == stages.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(summary: RoadmapSummary, modifier: Modifier = Modifier) {
    Surface(
        color = RoadmapPrimarySoft,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RoadmapPrimary.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = summary.label,
                color = RoadmapMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = summary.value,
                color = RoadmapInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = RoadmapGhost,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RoadmapBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = label, color = RoadmapMuted, fontSize = 10.sp)
            Text(text = value, color = RoadmapInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RoadmapStageCard(stage: RoadmapStage, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(stageStatusColor(stage.status)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stage.number.toString().padStart(2, '0'),
                    color = stageStatusTextColor(stage.status),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            if (!isLast) {
                Divider(
                    color = RoadmapBorder,
                    modifier = Modifier
                        .width(2.dp)
                        .height(72.dp)
                        .padding(top = 6.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, RoadmapBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stage.title, color = RoadmapInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    StageBadge(stage.status, stage.isCurrent)
                }

                Text(
                    text = stage.description,
                    color = RoadmapMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = stage.topics.joinToString(" • "),
                    color = RoadmapInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Estimated time", color = RoadmapMuted, fontSize = 11.sp)
                    Text(text = stage.duration, color = RoadmapInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StageBadge(status: StageStatus, isCurrent: Boolean) {
    val (text, bg, textColor) = when (status) {
        StageStatus.COMPLETED -> Triple("Completed", RoadmapPrimarySoft, RoadmapPrimary)
        StageStatus.IN_PROGRESS -> Triple("In progress", RoadmapAccent.copy(alpha = 0.12f), RoadmapAccent)
        StageStatus.UPCOMING -> Triple("Upcoming", RoadmapGhost, RoadmapMuted)
        StageStatus.LOCKED -> Triple("Locked", Color(0xFFF1F5F9), RoadmapMuted)
    }

    val displayText = if (isCurrent && status == StageStatus.IN_PROGRESS) "Current" else text
    val displayBg = if (isCurrent && status == StageStatus.IN_PROGRESS) RoadmapPrimarySoft else bg
    val displayTextColor = if (isCurrent && status == StageStatus.IN_PROGRESS) RoadmapPrimary else textColor

    Surface(
        color = displayBg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, displayTextColor.copy(alpha = 0.12f))
    ) {
        Text(
            text = displayText,
            color = displayTextColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

private fun stageStatusColor(status: StageStatus): Color = when (status) {
    StageStatus.COMPLETED -> RoadmapPrimary
    StageStatus.IN_PROGRESS -> RoadmapAccent
    StageStatus.UPCOMING -> RoadmapGhost
    StageStatus.LOCKED -> Color(0xFFE2E8F0)
}

private fun stageStatusTextColor(status: StageStatus): Color = when (status) {
    StageStatus.COMPLETED -> Color.White
    StageStatus.IN_PROGRESS -> Color.White
    StageStatus.UPCOMING -> RoadmapMuted
    StageStatus.LOCKED -> RoadmapMuted
}
