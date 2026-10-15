package com.offline_First.ui.screens.course

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseCertificate
import com.offline_First.domain.model.CourseDetail
import com.offline_First.domain.model.CourseModule
import com.offline_First.domain.model.LessonItem
import com.offline_First.domain.model.UiState
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import com.offline_First.ui.utils.IconResolver

private fun CourseAccent.toColor(): Color = when (this) {
    CourseAccent.PRIMARY -> EduNovaPrimary
    CourseAccent.SECONDARY -> EduNovaSecondary
    CourseAccent.ACCENT -> EduNovaAccent
}

private fun getSkillsForCourse(course: CourseDetail): List<String> {
    val name = course.name.lowercase()
    return when {
        name.contains("python") -> listOf("Python Syntax", "OOP Concepts", "Data Structures", "Functions & Modules", "Problem Solving", "Scripting")
        name.contains("c++") || name.contains("cpp") -> listOf("C++17/20", "Memory Management", "Pointers & References", "OOP", "STL Containers", "Systems Programming")
        name.contains("dsa") || name.contains("algorithm") -> listOf("Data Structures", "Algorithm Design", "Big-O Analysis", "Recursion & DP", "Graph Theory", "Searching & Sorting")
        name.contains("data science") -> listOf("NumPy & Pandas", "Exploratory Data Analysis", "Statistical Testing", "Matplotlib & Seaborn", "Feature Engineering", "Data Wrangling")
        name.contains("machine learning") || name.contains("deep learning") -> listOf("Supervised Learning", "Neural Networks", "Model Evaluation", "Scikit-Learn", "Feature Scaling", "Loss Optimization")
        name.contains("java") -> listOf("Java Core", "OOP Principles", "Java Collections", "Exception Handling", "Multithreading", "JVM Internals")
        name.contains("javascript") -> listOf("Modern ES6+", "DOM Manipulation", "Asynchronous JS", "Promises & Fetch", "Event Loop", "Closures")
        name.contains("full stack") || name.contains("web development") -> listOf("Semantic HTML/CSS", "RESTful APIs", "Full Stack Architecture", "Database Design", "Node.js & Express", "State Management")
        name.contains("android") || name.contains("kotlin") -> listOf("Kotlin", "Jetpack Compose", "Coroutines & Flow", "Android Architecture", "Room DB", "Material 3")
        name.contains("system design") || name.contains("sde") -> listOf("System Scalability", "Clean Architecture", "Design Patterns", "Microservices", "Reliability Engineering", "Caching Strategies")
        else -> listOf("Core Foundations", "Hands-on Practice", "Problem Solving", "Industry Best Practices", "Real-world Projects")
    }
}

private fun getLearningOutcomesForCourse(course: CourseDetail): List<String> {
    val name = course.name.lowercase()
    return when {
        name.contains("python") -> listOf(
            "Master Python fundamentals, control flow, functions, and standard libraries",
            "Build robust object-oriented programs using classes, inheritance, and encapsulation",
            "Work with practical data structures including lists, dictionaries, sets, and tuples",
            "Test and validate your knowledge with quizzes and hands-on coding exercises"
        )
        name.contains("c++") || name.contains("cpp") -> listOf(
            "Understand modern C++ syntax, type safety, references, and memory allocation",
            "Design object-oriented systems with polymorphism, encapsulation, and templates",
            "Leverage the Standard Template Library (STL) for high-performance applications",
            "Solve real-world systems programming problems with industry best practices"
        )
        name.contains("dsa") || name.contains("algorithm") -> listOf(
            "Implement fundamental linear and non-linear data structures from scratch",
            "Analyze time and space complexity with asymptotic Big-O notation",
            "Solve advanced algorithmic problems using recursion, trees, and dynamic programming",
            "Prepare for technical interviews with structured practice problems"
        )
        name.contains("data science") -> listOf(
            "Clean, transform, and analyze real-world datasets with NumPy and Pandas",
            "Generate insightful charts and statistical visualizations with Matplotlib & Seaborn",
            "Formulate and test data-driven hypotheses using statistical workflows",
            "Build foundation pipelines for modern machine learning workflows"
        )
        name.contains("machine learning") || name.contains("deep learning") -> listOf(
            "Understand mathematical foundations of regression, classification, and clustering",
            "Train, evaluate, and tune predictive machine learning models with Scikit-Learn",
            "Prevent overfitting using cross-validation, regularization, and feature selection",
            "Implement multi-layer neural network concepts for modern AI problems"
        )
        else -> listOf(
            "Master core concepts and foundational principles through hands-on practice",
            "Build real-world projects and problem-solving skills step by step",
            "Test your knowledge with interactive module quizzes and knowledge checks",
            "Earn an EduNova Course Completion Certificate to showcase your achievements"
        )
    }
}

@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    onLessonClick: (lessonId: String) -> Unit = {},
    onOpenRevisionList: () -> Unit = {},
    viewModel: CourseDetailViewModel = viewModel(
        key = courseId,
        factory = CourseDetailViewModel.Factory(courseId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CourseDetailTopBar(
                onBack = onBack,
                title = when (val state = uiState.courseDetailState) {
                    is UiState.Success -> state.data.name
                    else -> "Course Overview"
                },
                onOpenRevisionList = onOpenRevisionList
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState.courseDetailState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading course syllabus...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                is UiState.Error -> {
                    CourseErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadCourseDetail() },
                        onBack = onBack
                    )
                }
                is UiState.Empty -> {
                    CourseErrorState(
                        message = "We couldn't find the requested course details.",
                        onRetry = { viewModel.loadCourseDetail() },
                        onBack = onBack
                    )
                }
                is UiState.Success -> {
                    CourseDetailContent(
                        course = state.data,
                        uiState = uiState,
                        onLessonClick = onLessonClick,
                        onToggleLessonCompleted = { lessonId ->
                            viewModel.toggleLessonCompleted(lessonId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseDetailTopBar(
    onBack: () -> Unit,
    title: String,
    onOpenRevisionList: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EduNovaBorder)
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
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (onOpenRevisionList != null) {
                IconButton(onClick = onOpenRevisionList) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Revision List",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseDetailContent(
    course: CourseDetail,
    uiState: CourseDetailUiState,
    onLessonClick: (lessonId: String) -> Unit,
    onToggleLessonCompleted: (lessonId: String) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = course.accent.toColor()
    val sortedModules = remember(course) { course.modules.sortedBy { it.displayOrder } }

    var expandedModuleIds by remember(course) {
        mutableStateOf(sortedModules.map { it.id }.toSet())
    }
    var showCertificateDialog by remember { mutableStateOf(false) }

    if (showCertificateDialog && uiState.certificate != null) {
        CertificateDialog(
            certificate = uiState.certificate,
            onDismiss = { showCertificateDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Coursera-style Hero Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(EduNovaPrimaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.surface)
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Organization / Category line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "EduNova Academy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("•", fontSize = 12.sp, color = mutedInk)
                        Text(
                            text = course.category.ifBlank { "Professional Certificate" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = mutedInk
                        )
                    }

                    // Title and Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = course.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = ink,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EduNovaPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconResolver.resolve(course.icon),
                                contentDescription = "${course.name} icon",
                                tint = accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Rating and Learner Stats bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "4.8",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ink
                        )
                        Text(
                            text = "(2,480 ratings)",
                            fontSize = 12.sp,
                            color = mutedInk
                        )
                        Text("•", fontSize = 12.sp, color = mutedInk)
                        Text(
                            text = "97% approval",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduNovaSuccess
                        )
                    }

                    // Course Description
                    Text(
                        text = course.description,
                        color = mutedInk,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    // Coursera Quick-Glance Badges with Theme Vector Icons
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EduNovaBadge(
                            text = "${course.level} Level",
                            icon = Icons.Default.School,
                            containerColor = EduNovaPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        course.durationHours?.let { hours ->
                            EduNovaBadge(
                                text = "Approx. $hours hours",
                                icon = Icons.Default.Timer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = mutedInk
                            )
                        }
                        EduNovaBadge(
                            text = "Flexible Schedule",
                            icon = Icons.Default.CalendarToday,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = mutedInk
                        )
                        EduNovaBadge(
                            text = "${course.modules.size} Modules (${course.totalLessonsCount} Lessons)",
                            icon = Icons.Default.MenuBook,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = mutedInk
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Primary Action CTA: Start / Continue Learning
                    val firstLessonId = course.modules.firstOrNull()?.lessons?.firstOrNull()?.id
                    Button(
                        onClick = {
                            firstLessonId?.let { onLessonClick(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (uiState.completedLessonsCount > 0) "Continue Learning" else "Start Learning",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "100% Free Curriculum  •  Self-Paced  •  Certificate on completion",
                        fontSize = 11.5.sp,
                        color = mutedInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Coursera-inspired Course Progress Tracking Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ink
                        )
                        val pct = (uiState.progressPercentage * 100).toInt()
                        Text(
                            text = "$pct% Complete",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (uiState.isCourseCompleted) EduNovaSuccess else MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { uiState.progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .semantics {
                                contentDescription = "Course progress: ${(uiState.progressPercentage * 100).toInt()} percent completed"
                            },
                        color = if (uiState.isCourseCompleted) EduNovaSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.completedLessonsCount} of ${uiState.totalLessonsCount} lessons completed",
                            fontSize = 12.sp,
                            color = mutedInk
                        )
                        if (uiState.isCourseCompleted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduNovaSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduNovaSuccess
                                )
                            }
                        }
                    }

                    // Certificate CTA banner when course is complete
                    if (uiState.isCourseCompleted && uiState.certificate != null) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showCertificateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduNovaSuccess)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "View EduNova Certificate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Skills you'll gain (Coursera Feature)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Skills you'll gain",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ink
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        getSkillsForCourse(course).forEach { skill ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduNovaPrimaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, EduNovaPrimary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = skill,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // What you will learn
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "What you will learn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ink
                    )
                    getLearningOutcomesForCourse(course).forEach { outcome ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EduNovaSuccess,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = outcome,
                                fontSize = 13.sp,
                                color = ink,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }

        // Prerequisites Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Prerequisites & Requirements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ink
                    )
                    if (course.prerequisites.isEmpty() ||
                        course.prerequisites.any { it.contains("None", ignoreCase = true) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EduNovaSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "No prior experience required — beginner friendly!",
                                fontSize = 13.sp,
                                color = mutedInk
                            )
                        }
                    } else {
                        course.prerequisites.forEach { req ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = req,
                                    fontSize = 13.sp,
                                    color = mutedInk,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shareable Certificate Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EduNovaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EduNovaPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Earn an EduNova Certificate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ink
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Complete all lessons and quizzes to earn a shareable EduNova course-completion certificate.",
                            fontSize = 12.sp,
                            color = mutedInk,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Syllabus Section Header with Expand/Collapse All
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Course Curriculum",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ink
                    )
                    Text(
                        text = "${course.modules.size} modules • ${course.totalLessonsCount} lessons",
                        fontSize = 12.sp,
                        color = mutedInk
                    )
                }
                TextButton(
                    onClick = {
                        expandedModuleIds = if (expandedModuleIds.size == sortedModules.size) {
                            emptySet()
                        } else {
                            sortedModules.map { it.id }.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (expandedModuleIds.size == sortedModules.size) "Collapse All" else "Expand All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Modules and Lessons List (Expandable Accordion)
        items(sortedModules.size, key = { sortedModules[it].id }) { index ->
            val module = sortedModules[index]
            val isExpanded = expandedModuleIds.contains(module.id)
            ModuleCard(
                module = module,
                moduleIndex = index + 1,
                courseAccent = course.accent,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedModuleIds = if (isExpanded) {
                        expandedModuleIds - module.id
                    } else {
                        expandedModuleIds + module.id
                    }
                },
                onLessonClick = onLessonClick,
                onToggleLessonCompleted = onToggleLessonCompleted
            )
        }
    }
}

@Composable
private fun ModuleCard(
    module: CourseModule,
    moduleIndex: Int,
    courseAccent: CourseAccent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLessonClick: (lessonId: String) -> Unit,
    onToggleLessonCompleted: (lessonId: String) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = courseAccent.toColor()
    val completedInModule = module.lessons.count { it.isCompleted }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Module Header (Clickable to Expand / Collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Module $moduleIndex",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        EduNovaBadge(
                            text = "$completedInModule/${module.lessons.size} completed",
                            containerColor = if (completedInModule == module.lessons.size && module.lessons.isNotEmpty()) {
                                EduNovaSuccess.copy(alpha = 0.15f)
                            } else {
                                EduNovaPrimaryContainer
                            },
                            contentColor = if (completedInModule == module.lessons.size && module.lessons.isNotEmpty()) {
                                EduNovaSuccess
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = module.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ink
                    )
                    module.description?.let { desc ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 12.sp,
                            color = mutedInk
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Lessons in module (Expandable)
            AnimatedVisibility(visible = isExpanded) {
                val sortedLessons = remember(module) { module.lessons.sortedBy { it.displayOrder } }
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedLessons.forEachIndexed { index, lesson ->
                        LessonRow(
                            lesson = lesson,
                            orderIndex = index + 1,
                            accentColor = accentColor,
                            onClick = { onLessonClick(lesson.id) },
                            onToggleCompleted = { onToggleLessonCompleted(lesson.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LessonItem,
    orderIndex: Int,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val isDone = lesson.isCompleted

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Lesson ${orderIndex}: ${lesson.title}. Status: ${if (isDone) "Completed" else "Incomplete"}" },
        shape = RoundedCornerShape(12.dp),
        color = if (isDone) EduNovaSuccess.copy(alpha = 0.05f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, if (isDone) EduNovaSuccess.copy(alpha = 0.35f) else EduNovaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Completion indicator button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDone) EduNovaSuccess.copy(alpha = 0.15f) else EduNovaPrimaryContainer)
                    .clickable(onClick = onToggleCompleted)
                    .semantics { contentDescription = if (isDone) "Mark incomplete" else "Mark complete" },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EduNovaSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    val icon = when (lesson.contentType.lowercase()) {
                        "quiz" -> Icons.Default.Quiz
                        "article" -> Icons.AutoMirrored.Filled.Article
                        else -> Icons.Default.PlayCircleOutline
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = lesson.contentType,
                        tint = EduNovaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${orderIndex}. ${lesson.title}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (isDone) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduNovaSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Completed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduNovaSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                lesson.description?.let { desc ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = mutedInk,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lesson.durationMinutes?.let { mins ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = mutedInk,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "$mins min",
                                fontSize = 11.sp,
                                color = mutedInk
                            )
                        }
                    }

                    lesson.resourceProvider?.let { provider ->
                        Text(
                            text = "•  $provider",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (lesson.quiz != null) {
                        Text(
                            text = "•  Quiz included",
                            fontSize = 11.sp,
                            color = EduNovaSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open lesson",
                tint = if (isDone) EduNovaSuccess else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CertificateDialog(
    certificate: CourseCertificate,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "EduNova Certificate of Completion\n" +
                            "This certifies that ${certificate.learnerName} has successfully completed all coursework for '${certificate.courseName}'.\n" +
                            "Completion Date: ${certificate.completedDate}\n" +
                            "Certificate ID: ${certificate.id}\n" +
                            "Platform: EduNova (Course Completion Record)"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share EduNova Certificate"))
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = EduNovaPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "EduNova Certificate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, EduNovaBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CERTIFICATE OF COMPLETION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "This certifies that",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = certificate.learnerName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "has successfully completed all required modules & lessons in",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = certificate.courseName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(certificate.completedDate, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Certificate ID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(certificate.id, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Text(
                    text = "EduNova course-completion certificate. Locally verified upon 100% completion of course lessons. Not an accredited university degree.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    )
}

@Composable
private fun CourseErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Unable to load course",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Back")
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Retry")
            }
        }
    }
}
