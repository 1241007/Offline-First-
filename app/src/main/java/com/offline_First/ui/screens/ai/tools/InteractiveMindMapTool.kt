package com.offline_First.ui.screens.ai.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveMindMapTool(
    initialTopic: String = "",
    onBack: () -> Unit
) {
    var topicInput by remember { mutableStateOf(initialTopic) }
    var isGenerating by remember { mutableStateOf(false) }
    var activeMindMap by remember { mutableStateOf<MindMapData?>(null) }
    val expandedBranches = remember { mutableStateMapOf<Int, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    fun startGenerating(topic: String) {
        val targetTopic = topic.ifBlank { "Photosynthesis Process" }
        isGenerating = true
        activeMindMap = null
        expandedBranches.clear()

        coroutineScope.launch {
            delay(1200)
            val map = DemoMindMapProvider.getMindMapForTopic(targetTopic)
            activeMindMap = map
            // Auto-expand all branches by default for a rich visual layout
            map.branches.indices.forEach { expandedBranches[it] = true }
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
                            color = EduNovaSecondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountTree, null, tint = EduNovaSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Mind Map", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduNovaTextPrimary)
                            Text(
                                if (activeMindMap != null) activeMindMap!!.subject else "Visual Concept Tree",
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
            // Topic Input Card
            if (activeMindMap == null && !isGenerating) {
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
                                "Generate Concept Mind Map",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EduNovaTextPrimary
                            )
                        }
                        Text(
                            "Visualize complex chapters, mechanisms, or system architectures as an interconnected tree.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary
                        )

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            placeholder = { Text("e.g. Photosynthesis, Newton's Laws, Machine Learning...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduNovaPrimary,
                                unfocusedBorderColor = EduNovaBorder
                            )
                        )

                        Text("Popular map topics:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EduNovaTextSecondary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoMindMapProvider.sampleTopics.forEach { topic ->
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
                            Icon(Icons.Default.Hub, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Mind Map", fontWeight = FontWeight.SemiBold)
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
                        CircularProgressIndicator(color = EduNovaSecondary, strokeWidth = 3.dp)
                        Text(
                            "AI is organizing conceptual nodes...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EduNovaTextPrimary
                        )
                        Text(
                            "Extracting hierarchical dependencies, mechanisms, and key formulas.",
                            fontSize = 13.sp,
                            color = EduNovaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Active Mind Map Tree
            activeMindMap?.let { map ->
                // Central Core Topic Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = EduNovaPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    text = "CORE CONCEPT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    activeMindMap = null
                                }
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Topic", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = map.centralTopic,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = map.summary,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Visual Connector Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFFB0BEC5),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Branches List
                map.branches.forEachIndexed { branchIndex, branch ->
                    val isExpanded = expandedBranches[branchIndex] ?: true
                    val branchColor = Color(branch.colorHex)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = EduNovaSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, branchColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Branch Header (Clickable to Expand/Collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedBranches[branchIndex] = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = branchColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Hub, null, tint = branchColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = branch.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = EduNovaTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { expandedBranches[branchIndex] = !isExpanded },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle",
                                        tint = EduNovaTextSecondary
                                    )
                                }
                            }

                            // Subnodes (Rendered when expanded)
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    branch.subNodes.forEach { subNode ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = EduNovaBackground,
                                            border = BorderStroke(1.dp, EduNovaBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(branchColor)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = subNode.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = EduNovaTextPrimary
                                                    )
                                                }
                                                Text(
                                                    text = subNode.description,
                                                    fontSize = 12.sp,
                                                    color = EduNovaTextSecondary,
                                                    lineHeight = 16.sp
                                                )

                                                Spacer(Modifier.height(4.dp))
                                                // Key Takeaway Tags
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    subNode.keyPoints.forEach { point ->
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = branchColor.copy(alpha = 0.08f)
                                                        ) {
                                                            Text(
                                                                text = point,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = branchColor,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                    }
                }
            }
        }
    }
}
