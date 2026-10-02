package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess

private data class RoadmapOption(
    val title: String,
    val category: String,
    val description: String,
    val skills: List<String>,
    val level: String,
    val duration: String,
    val stages: Int,
    val icon: String,
    val accent: Color
)

private data class GeneratedRoadmapPreview(
    val goal: String,
    val level: String,
    val studyTime: String,
    val duration: String,
    val stages: List<String>
)

private val roadmapCategories = listOf(
    "All",
    "Development",
    "Data & AI",
    "Mobile",
    "Cloud & DevOps",
    "Security"
)

private val roadmapPrimary = EduNovaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    onBack: () -> Unit = {},
    onBuildRoadmap: () -> Unit = {}
) {
    val roadmapOptions = remember {
        listOf(
            RoadmapOption(
                title = "Frontend Developer",
                category = "Development",
                description = "Build polished interfaces and production-ready web apps.",
                skills = listOf("HTML", "CSS", "JavaScript", "React"),
                level = "Beginner",
                duration = "8–10 weeks",
                stages = 6,
                icon = "FE",
                accent = EduNovaPrimary
            ),
            RoadmapOption(
                title = "Backend Developer",
                category = "Development",
                description = "Create APIs, databases, and backend systems students can ship.",
                skills = listOf("Node.js", "APIs", "Databases", "Security"),
                level = "Intermediate",
                duration = "10–12 weeks",
                stages = 7,
                icon = "BE",
                accent = EduNovaSecondary
            ),
            RoadmapOption(
                title = "Full Stack Developer",
                category = "Development",
                description = "Move from UI work to complete product development workflows.",
                skills = listOf("React", "Node", "Databases", "DevOps"),
                level = "Intermediate",
                duration = "12–14 weeks",
                stages = 8,
                icon = "FS",
                accent = EduNovaSuccess
            ),
            RoadmapOption(
                title = "Data Scientist",
                category = "Data & AI",
                description = "Learn data analysis, modeling, and decision-making with real datasets.",
                skills = listOf("Python", "SQL", "Statistics", "ML"),
                level = "Beginner",
                duration = "10–12 weeks",
                stages = 7,
                icon = "DS",
                accent = EduNovaSecondary
            ),
            RoadmapOption(
                title = "Machine Learning Engineer",
                category = "Data & AI",
                description = "Master model training, evaluation, and deployment for AI products.",
                skills = listOf("Python", "ML", "PyTorch", "Math"),
                level = "Intermediate",
                duration = "12–16 weeks",
                stages = 9,
                icon = "ML",
                accent = EduNovaAccent
            ),
            RoadmapOption(
                title = "Android Developer",
                category = "Mobile",
                description = "Build user-friendly mobile apps with Kotlin and Jetpack Compose.",
                skills = listOf("Kotlin", "Compose", "UI", "Testing"),
                level = "Beginner",
                duration = "8–10 weeks",
                stages = 6,
                icon = "AD",
                accent = EduNovaSecondary
            ),
            RoadmapOption(
                title = "DevOps Engineer",
                category = "Cloud & DevOps",
                description = "Learn automation, deployment flow, and cloud-first engineering habits.",
                skills = listOf("Linux", "CI/CD", "Cloud", "Containers"),
                level = "Intermediate",
                duration = "10–12 weeks",
                stages = 7,
                icon = "DO",
                accent = EduNovaAccent
            ),
            RoadmapOption(
                title = "Data Analyst",
                category = "Data & AI",
                description = "Turn messy numbers into trends, dashboards, and smart business decisions.",
                skills = listOf("Excel", "SQL", "Tableau", "Insights"),
                level = "Beginner",
                duration = "6–8 weeks",
                stages = 5,
                icon = "DA",
                accent = EduNovaSecondary
            ),
            RoadmapOption(
                title = "Cybersecurity",
                category = "Security",
                description = "Build the fundamentals of secure systems, networks, and threat awareness.",
                skills = listOf("Networking", "Security", "Ethical Hacking", "Monitoring"),
                level = "Intermediate",
                duration = "10–12 weeks",
                stages = 7,
                icon = "CY",
                accent = EduNovaPrimary
            )
        )
    }

    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedRoadmap by remember { mutableStateOf<RoadmapOption?>(null) }
    var showPersonalizationSheet by rememberSaveable { mutableStateOf(false) }
    var isGenerating by rememberSaveable { mutableStateOf(false) }
    var generatedRoadmap by remember { mutableStateOf<GeneratedRoadmapPreview?>(null) }

    var selectedGoal by rememberSaveable { mutableStateOf("Get a job") }
    var selectedLevel by rememberSaveable { mutableStateOf("Beginner") }
    var selectedStudyTime by rememberSaveable { mutableStateOf("1 hour/day") }
    var selectedInterest by rememberSaveable { mutableStateOf("AI / ML") }

    val filteredRoadmaps = remember(selectedCategory, roadmapOptions) {
        if (selectedCategory == "All") roadmapOptions else roadmapOptions.filter { it.category == selectedCategory }
    }

    val featureRoadmaps = filteredRoadmaps.take(2)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            delay(1200L)
            generatedRoadmap = GeneratedRoadmapPreview(
                goal = selectedGoal,
                level = selectedLevel,
                studyTime = selectedStudyTime,
                duration = "10–12 weeks",
                stages = listOf(
                    "Python Foundations",
                    "Math for ML",
                    "Data Processing",
                    "Machine Learning",
                    "Deep Learning",
                    "Real-world Projects"
                )
            )
            showPersonalizationSheet = false
            isGenerating = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
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
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Roadmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Developer Roadmaps",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Follow a structured path, learn the right skills, and build your way toward your goal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = roadmapPrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, roadmapPrimary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(roadmapPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✦",
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chat then Build",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tell EduNova about your goals, current level, and available study time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PersonalizationTag("Your goal")
                            PersonalizationTag("Current level")
                            PersonalizationTag("Study time")
                            PersonalizationTag("Interests")
                        }

                        Button(
                            onClick = onBuildRoadmap,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chat & Build")
                        }
                    }
                }
            }

            if (generatedRoadmap != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Your roadmap is ready!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Goal: ${generatedRoadmap!!.goal}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Level: ${generatedRoadmap!!.level}   •   Study time: ${generatedRoadmap!!.studyTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Estimated journey: ${generatedRoadmap!!.duration}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = roadmapPrimary
                            )

                            generatedRoadmap!!.stages.forEachIndexed { index, stage ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(roadmapPrimary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "%02d".format(index + 1),
                                            color = roadmapPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                                ) {
                                    Text("Start Roadmap")
                                }
                                OutlinedButton(
                                    onClick = { generatedRoadmap = null },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Adjust Goals")
                                }
                            }
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(roadmapPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AI",
                                    color = roadmapPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Building your roadmap...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "EduNova is creating a learning path based on your goals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Explore by interest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        roadmapCategories.forEach { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) roadmapPrimary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedCategory = category }
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Popular this week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        featureRoadmaps.forEach { roadmap ->
                            FeaturedRoadmapCard(
                                roadmap = roadmap,
                                onClick = { selectedRoadmap = roadmap },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Standard Roadmaps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a structured path and start learning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredRoadmaps.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No roadmaps found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try another category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val rows = filteredRoadmaps.chunked(2)
                items(rows.size) { index ->
                    val row = rows[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { roadmap ->
                            StandardRoadmapCard(
                                roadmap = roadmap,
                                onClick = { selectedRoadmap = roadmap },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showPersonalizationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonalizationSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Build your personalized roadmap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tell EduNova about your goals, current level, and time to learn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuestionBlock(
                    title = "What’s your goal?",
                    options = listOf("Get a job", "Prepare for an interview", "Learn a new skill", "Build projects", "Prepare for an exam"),
                    selected = selectedGoal,
                    onSelect = { selectedGoal = it }
                )

                QuestionBlock(
                    title = "What’s your current level?",
                    options = listOf("Beginner", "Intermediate", "Advanced"),
                    selected = selectedLevel,
                    onSelect = { selectedLevel = it }
                )

                QuestionBlock(
                    title = "How much time can you study?",
                    options = listOf("30 min/day", "1 hour/day", "2 hours/day", "3+ hours/day"),
                    selected = selectedStudyTime,
                    onSelect = { selectedStudyTime = it }
                )

                QuestionBlock(
                    title = "What do you want to learn?",
                    options = listOf("Web Development", "AI / ML", "Data Science", "Android", "Cloud"),
                    selected = selectedInterest,
                    onSelect = { selectedInterest = it }
                )

                Button(
                    onClick = { isGenerating = true },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                ) {
                    Text("Generate roadmap")
                }
            }
        }
    }

    selectedRoadmap?.let { roadmap ->
        AlertDialog(
            onDismissRequest = { selectedRoadmap = null },
            title = {
                Text(
                    text = roadmap.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = roadmap.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Difficulty: ${roadmap.level}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Duration: ${roadmap.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Stages: ${roadmap.stages}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Skills covered:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        roadmap.skills.forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(roadmapPrimary.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = skill,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = roadmapPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedRoadmap = null },
                    colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary)
                ) {
                    Text("Start Roadmap")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRoadmap = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun PersonalizationTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun QuestionBlock(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) roadmapPrimary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(option) }
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedRoadmapCard(
    roadmap: RoadmapOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(roadmap.accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = roadmap.icon,
                    color = roadmap.accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = roadmap.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = roadmap.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Text(
                text = "${roadmap.level} • ${roadmap.stages} stages",
                style = MaterialTheme.typography.labelMedium,
                color = roadmapPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StandardRoadmapCard(
    roadmap: RoadmapOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(roadmap.accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = roadmap.icon,
                        fontWeight = FontWeight.Bold,
                        color = roadmap.accent,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = roadmap.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = roadmap.skills.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = roadmap.level,
                    style = MaterialTheme.typography.labelMedium,
                    color = roadmapPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${roadmap.stages} stages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = roadmapPrimary.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Roadmap",
                    color = roadmapPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
