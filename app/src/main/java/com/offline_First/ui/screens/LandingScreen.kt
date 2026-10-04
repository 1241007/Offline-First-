package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.R
import com.offline_First.domain.model.ChapterItem
import com.offline_First.domain.model.ChapterStatus
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
import com.offline_First.domain.model.UpcomingExam
import com.offline_First.ui.components.EduNovaBottomNavItem
import com.offline_First.ui.components.EduNovaBottomNavigation
import com.offline_First.ui.screens.landing.LandingUiState
import com.offline_First.ui.screens.landing.LandingViewModel
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSurface
import kotlinx.coroutines.launch

private fun CourseAccent.toColor(): Color = when (this) {
    CourseAccent.PRIMARY -> EduNovaPrimary
    CourseAccent.SECONDARY -> EduNovaSecondary
    CourseAccent.ACCENT -> EduNovaAccent
}

@Composable
private fun EduNovaBrandName() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Edu",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = (-0.3).sp
        )
        Text(
            "Nova",
            color = EduNovaPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
fun LandingScreen(
    onLogin: () -> Unit = {},
    isLoggedIn: Boolean = false,
    onAskAI: () -> Unit = {},
    onOpenAITools: () -> Unit = onAskAI,
    onRoadmap: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenMyLearning: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: LandingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isSchoolMode = uiState.educationMode == EducationMode.SCHOOL
    var selectedSubjectForDetails by remember { mutableStateOf<Subject?>(null) }
    var initialSubjectTab by remember { mutableIntStateOf(0) }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (isSchoolMode && selectedSubjectForDetails != null) {
                // Interactive Chapter & Practice Detail views matching Screens 2 & 3 in the design
                SchoolSubjectDetailScreen(
                    subject = selectedSubjectForDetails!!,
                    initialTab = initialSubjectTab,
                    onBack = { selectedSubjectForDetails = null },
                    onAskAI = onAskAI,
                    showMessage = showMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(bottom = 72.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(padding)
                        .padding(bottom = 80.dp)
                ) {
                    if (isSchoolMode) {
                        SchoolTopBar(
                            onSearch = { showMessage("Search will be available soon.") },
                            onNotifications = { showMessage("No new notifications") },
                            onProfile = onOpenProfile
                        )
                    } else {
                        TopBar(
                            onSearch = { showMessage("Search will be available soon.") },
                            onLogin = onLogin,
                            isLoggedIn = isLoggedIn,
                            onProfile = onOpenProfile,
                            onOpenMyLearning = onOpenMyLearning,
                            onOpenSettings = onOpenSettings,
                            onLanguageSelected = onLanguageSelected,
                            onLogout = onLogout
                        )
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = primaryColor)
                        }
                    } else {
                        when (uiState.educationMode) {
                            EducationMode.SCHOOL -> {
                                SchoolHomeContent(
                                    uiState = uiState,
                                    onAskAI = onAskAI,
                                    onContinueLearning = {
                                        selectedSubjectForDetails = uiState.subjects.firstOrNull()
                                        initialSubjectTab = 0
                                    },
                                    onSubjectClick = { subject ->
                                        selectedSubjectForDetails = subject
                                        initialSubjectTab = 0
                                    },
                                    onViewPlan = onRoadmap,
                                    onSeeAllSubjects = {
                                        selectedSubjectForDetails = uiState.subjects.firstOrNull()
                                        initialSubjectTab = 0
                                    },
                                    onSeeAllExams = {
                                        showMessage("All exams will be listed soon.")
                                    }
                                )
                            }
                            EducationMode.GENERAL -> {
                                GeneralHomeContent(
                                    uiState = uiState,
                                    onCourseClick = { showMessage("Course content will be available soon.") },
                                    onStartLearning = { showMessage("Your learning journey will be ready soon.") },
                                    onSupportAction = showMessage
                                )
                            }
                        }
                    }
                }
            }

            // Ask AI Floating Action Button: sits cleanly above bottom navigation bar in General mode
            if (!isSchoolMode) {
                Button(
                    onClick = onAskAI,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 80.dp)
                        .semantics { contentDescription = "Ask AI" },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("✦  Ask AI", fontWeight = FontWeight.Bold)
                }
            }

            // EduNova Persistent Bottom Navigation Bar
            EduNovaBottomNavigation(
                selectedItem = if (selectedSubjectForDetails != null) {
                    if (initialSubjectTab == 2) EduNovaBottomNavItem.PRACTICE else EduNovaBottomNavItem.SUBJECTS
                } else {
                    EduNovaBottomNavItem.HOME
                },
                isSchoolMode = isSchoolMode,
                onNavigate = { item ->
                    when (item) {
                        EduNovaBottomNavItem.HOME -> {
                            selectedSubjectForDetails = null
                            scope.launch { scrollState.animateScrollTo(0) }
                        }
                        EduNovaBottomNavItem.SUBJECTS -> {
                            selectedSubjectForDetails = uiState.subjects.firstOrNull()
                            initialSubjectTab = 0
                        }
                        EduNovaBottomNavItem.PRACTICE -> {
                            selectedSubjectForDetails = uiState.subjects.firstOrNull()
                            initialSubjectTab = 2
                        }
                        EduNovaBottomNavItem.COURSES -> onOpenMyLearning()
                        EduNovaBottomNavItem.ROADMAP -> onRoadmap()
                        EduNovaBottomNavItem.CHAT -> onAskAI()
                        EduNovaBottomNavItem.TOOLS -> onOpenAITools()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * School Mode (Class 9-12):
 * Prioritizes Greeting -> Continue Learning -> My Subjects -> Your Next Step.
 * Academic school subjects, zero dashboard overload, no duplicate courses.
 */
/**
 * Top Bar for School Mode (matches design):
 * - Left: "EduNova" in bold teal font
 * - Right: Search icon, Notification bell with badge dot, Profile avatar ("A")
 */
@Composable
private fun SchoolTopBar(
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(8.dp))
            EduNovaBrandName()
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSearch,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onNotifications,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Small unread notification indicator dot
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5722))
                )
            }

            Surface(
                onClick = onProfile,
                shape = CircleShape,
                color = EduNovaPrimary,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * School Mode Homepage:
 * - Greeting removed as specifically requested ("remove good morining asha cbse class10")
 * - Hero Card: "Ask EduNova AI" with 3D cute AI Robot illustration and search pill
 * - "Today's Learning": Quadratic Equations card with math parabola graph graphic
 * - "Your Subjects": 4 horizontal subject cards (Math, Physics, Chemistry, Biology) with progress
 * - "Upcoming Exams": Mathematics Unit Test with purple calendar
 */
@Composable
private fun SchoolHomeContent(
    uiState: LandingUiState,
    onAskAI: () -> Unit,
    onContinueLearning: () -> Unit,
    onSubjectClick: (Subject) -> Unit,
    onViewPlan: () -> Unit,
    onSeeAllSubjects: () -> Unit,
    onSeeAllExams: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Card: "Ask EduNova AI"
        SchoolAskEduNovaAICard(onAskAI = onAskAI)

        // 2. Today's Learning Section
        SchoolTodaysLearningSection(
            item = uiState.continueLearning,
            onContinue = onContinueLearning,
            onViewPlan = onViewPlan
        )

        // 3. Your Subjects Section
        SchoolYourSubjectsSection(
            subjects = uiState.subjects,
            onSubjectClick = onSubjectClick,
            onSeeAll = onSeeAllSubjects
        )

        // 4. Upcoming Exams Section
        SchoolUpcomingExamsSection(
            exams = uiState.upcomingExams,
            onSeeAll = onSeeAllExams,
            onExamClick = onContinueLearning
        )
    }
}

/**
 * Hero Card: Ask EduNova AI
 * Features delicate soft teal gradient, robot mascot illustration, and pill input box
 */
@Composable
private fun SchoolAskEduNovaAICard(onAskAI: () -> Unit) {
    Card(
        onClick = onAskAI,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFB2DFDB).copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE8F7F5),
                            Color(0xFFE3F2FD),
                            Color(0xFFEFFBF9)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ask EduNova AI",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Explain, solve, summarize or get help with any topic.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Cute 3D AI Robot illustration
                    CuteAIRobotIllustration()
                }

                // White prompt pill box
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ask anything... (text, image, voice, file)",
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = EduNovaPrimary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ask",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D-styled AI Robot Mascot Illustration
 */
@Composable
private fun CuteAIRobotIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height

            // Glow / aura
            drawCircle(
                color = Color(0xFF80DEEA).copy(alpha = 0.35f),
                radius = w * 0.46f,
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f)
            )

            // Antenna
            drawLine(
                color = Color(0xFF00897B),
                start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.22f),
                end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.08f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF26A69A),
                radius = 4.5f,
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.07f)
            )

            // Robot head
            drawRoundRect(
                color = Color(0xFFE0F7FA),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )

            // Robot screen face
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.30f),
                size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.40f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )

            // Glowing cyan eyes
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 5.5f,
                center = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.48f)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 5.5f,
                center = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.48f)
            )

            // Smile
            val smilePath = Path().apply {
                moveTo(w * 0.44f, h * 0.58f)
                quadraticTo(w * 0.5f, h * 0.63f, w * 0.56f, h * 0.58f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Ears/side sensors
            drawCircle(
                color = Color(0xFF00897B),
                radius = 3.5f,
                center = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.50f)
            )
            drawCircle(
                color = Color(0xFF00897B),
                radius = 3.5f,
                center = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.50f)
            )
        }
    }
}

/**
 * Today's Learning Section
 */
@Composable
private fun SchoolTodaysLearningSection(
    item: ContinueLearningItem?,
    onContinue: () -> Unit,
    onViewPlan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Learning",
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View Plan",
                color = EduNovaPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewPlan() }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parabola Math Graph Canvas
                MathGraphParabolaGraphic()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item?.subjectName ?: "Mathematics",
                        color = EduNovaPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item?.topicName ?: "Quadratic Equations",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item?.lessonInfo ?: "Lesson 8 of 12",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { item?.progress ?: 0.8f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EduNovaPrimary,
                            trackColor = Color(0xFFE0F2F1)
                        )
                        Text(
                            text = "${((item?.progress ?: 0.8f) * 100).toInt()}%",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EduNovaPrimary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Continue  →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Coordinate grid parabola graph graphic for Mathematics Quadratic Equations
 */
@Composable
private fun MathGraphParabolaGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0F2F1)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height
            val tealColor = Color(0xFF00796B)
            val gridColor = Color(0xFF80CBC4).copy(alpha = 0.5f)

            // Coordinate axes
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.1f),
                end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.9f),
                strokeWidth = 1.5f
            )
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.75f),
                end = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.75f),
                strokeWidth = 1.5f
            )

            // Parabola path: y = y0 - a * (x - x0)^2
            val path = Path()
            val x0 = w * 0.5f
            val y0 = h * 0.75f
            val a = 0.045f

            var first = true
            for (step in 0..40) {
                val x = w * 0.2f + (w * 0.6f * step / 40f)
                val dx = x - x0
                val y = y0 - (a * dx * dx)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = tealColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * "Your Subjects" Section:
 * 4 horizontal cards (Math, Physics, Chemistry, Biology) with progress
 */
@Composable
private fun SchoolYourSubjectsSection(
    subjects: List<Subject>,
    onSubjectClick: (Subject) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Subjects",
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See All",
                color = EduNovaPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            subjects.take(4).forEach { subject ->
                SchoolSubjectCardHorizontal(
                    subject = subject,
                    modifier = Modifier.weight(1f),
                    onClick = { onSubjectClick(subject) }
                )
            }
        }
    }
}

@Composable
private fun SchoolSubjectCardHorizontal(
    subject: Subject,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Subject theme styling
    val (iconBg, iconTint, progressColor) = when (subject.name.lowercase()) {
        "math", "mathematics" -> Triple(Color(0xFFFFEBEE), Color(0xFFE53935), EduNovaPrimary)
        "physics" -> Triple(Color(0xFFE3F2FD), Color(0xFF1E88E5), Color(0xFFFB8C00))
        "chemistry" -> Triple(Color(0xFFFCE4EC), Color(0xFFE91E63), Color(0xFFE53935))
        "biology" -> Triple(Color(0xFFE8F5E9), Color(0xFF43A047), Color(0xFF43A047))
        else -> Triple(Color(0xFFE0F2F1), EduNovaPrimary, EduNovaPrimary)
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                when (subject.name.lowercase()) {
                    "math", "mathematics" -> Icon(Icons.Default.Calculate, null, tint = iconTint, modifier = Modifier.size(24.dp))
                    "physics" -> Icon(Icons.Default.Science, null, tint = iconTint, modifier = Modifier.size(24.dp))
                    "chemistry" -> Text("🧪", fontSize = 20.sp)
                    "biology" -> Icon(Icons.Default.Eco, null, tint = iconTint, modifier = Modifier.size(24.dp))
                    else -> Text(subject.icon, fontSize = 20.sp)
                }
            }

            Text(
                text = if (subject.name == "Mathematics") "Math" else subject.name,
                color = Color(0xFF0F172A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "${((subject.progress ?: 0.8f) * 100).toInt()}%",
                color = progressColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = { subject.progress ?: 0.8f },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = Color(0xFFF1F5F9)
            )
        }
    }
}

/**
 * Upcoming Exams Section
 */
@Composable
private fun SchoolUpcomingExamsSection(
    exams: List<UpcomingExam>,
    onSeeAll: () -> Unit,
    onExamClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Exams",
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See All",
                color = EduNovaPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }

        Card(
            onClick = onExamClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE7F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF7E57C2),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exams.firstOrNull()?.title ?: "Mathematics – Unit Test",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exams.firstOrNull()?.daysRemaining ?: 3} days remaining • ${exams.firstOrNull()?.className ?: "Class 10"}",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Interactive School Subject Details Screen (Screens 2 & 3 in the design):
 * - Header: Back button, red Math book icon, "Mathematics / Class 10 • CBSE", Bookmark, Overflow menu
 * - Tabs: Overview, Chapters, Practice, PYQs, Notes
 * - Overview Tab (Screen 2): Your Progress (80%), Continue Learning (Quadratic Equations), Chapter list with checks, Ask AI card
 * - Practice Tab (Screen 3): Topic Quiz, Mixed Quiz, PYQs, Mock Tests, Mind Maps, Notes
 */
@Composable
private fun SchoolSubjectDetailScreen(
    subject: Subject,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onAskAI: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Overview", "Chapters", "Practice", "PYQs", "Notes")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }

            Spacer(Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (subject.name == "Mathematics") "Math" else subject.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subject.gradeLevel,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            IconButton(onClick = { showMessage("Saved to Bookmarks") }) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = Color(0xFF0F172A)
                )
            }

            IconButton(onClick = { showMessage("Options menu") }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFF0F172A)
                )
            }
        }

        // Tabs Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = EduNovaPrimary,
            edgePadding = 20.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EduNovaPrimary,
                        height = 3.dp
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tabTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = if (isSelected) EduNovaPrimary else Color(0xFF64748B)
                        )
                    }
                )
            }
        }

        // Tab Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                0, 1 -> {
                    // SCREEN 2: Overview / Chapters
                    // 1. Your Progress Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFFB2DFDB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE0F2F1), Color(0xFFE8F5E9))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { 0.80f },
                                        color = EduNovaPrimary,
                                        trackColor = Color.White,
                                        strokeWidth = 6.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "80%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Your Progress",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${subject.completedChapters} of ${subject.totalChapters} chapters completed",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Continue Learning
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Continue Learning",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MathGraphParabolaGraphic(Modifier.size(64.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EduNovaPrimary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "Quadratic Equations",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Lesson 8 of 12",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { 0.8f },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = EduNovaPrimary,
                                                trackColor = Color(0xFFE0F2F1)
                                            )
                                            Text(
                                                text = "80%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE0F2F1),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = EduNovaPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Timer, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                        Text("~10 min", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                        Text("5 practice questions", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }

                    // 3. Chapter List
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapter List",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "See All",
                                color = EduNovaPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val chapters = listOf(
                            Triple(1, "Introduction to Quadratic Equations", ChapterStatus.COMPLETED),
                            Triple(2, "Methods of Solving Quadratic Equations", ChapterStatus.COMPLETED),
                            Triple(3, "Nature of Roots", ChapterStatus.IN_PROGRESS),
                            Triple(4, "Word Problems", ChapterStatus.NOT_STARTED),
                            Triple(5, "Applications", ChapterStatus.NOT_STARTED)
                        )

                        chapters.forEach { (num, title, status) ->
                            Card(
                                onClick = { showMessage("Opening $title") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (status == ChapterStatus.IN_PROGRESS) Color(0xFFF0FDF4) else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (status == ChapterStatus.IN_PROGRESS) Color(0xFF80CBC4) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    when (status) {
                                        ChapterStatus.COMPLETED -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        ChapterStatus.IN_PROGRESS -> {
                                            Surface(
                                                shape = CircleShape,
                                                color = EduNovaPrimary,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "In progress",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        ChapterStatus.NOT_STARTED -> {
                                            Icon(
                                                imageVector = Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Not started",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$num. $title",
                                            fontWeight = if (status == ChapterStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (status == ChapterStatus.IN_PROGRESS) {
                                            Text(
                                                text = "In Progress • 80%",
                                                fontSize = 11.5.sp,
                                                color = EduNovaPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = if (status == ChapterStatus.IN_PROGRESS) EduNovaPrimary else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Need help with this chapter? Card (Ask EduNova AI)
                    Card(
                        onClick = onAskAI,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFB2DFDB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CuteAIRobotIllustration(Modifier.size(46.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Need help with this chapter?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Ask EduNova AI for explanations, examples or extra questions.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = EduNovaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                2 -> {
                    // SCREEN 3: Practice Tab
                    // 1. Practice Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Practice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Strengthen your understanding with questions.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Topic Quiz
                            Card(
                                onClick = { showMessage("Starting Topic Quiz...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFFF3E0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎯", fontSize = 20.sp)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }

                                    Text("Topic Quiz", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                                    Text("Practice by topic", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }

                            // Mixed Quiz
                            Card(
                                onClick = { showMessage("Generating AI Mixed Quiz...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFEDE7F6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Quiz, null, tint = Color(0xFF7E57C2), modifier = Modifier.size(20.dp))
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }

                                    Text("Mixed Quiz", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                                    Text("AI-generated questions", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }

                    // 2. Previous Year Questions
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Previous Year Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Solve chapter-wise PYQs with detailed solutions.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )

                        val pyqItems = listOf(
                            Triple("Chapter-wise PYQs", Color(0xFFFFEBEE), Color(0xFFE53935)),
                            Triple("Year-wise Papers", Color(0xFFEDE7F6), Color(0xFF7E57C2)),
                            Triple("Mock Test", Color(0xFFE8F5E9), Color(0xFF43A047))
                        )

                        pyqItems.forEach { (title, bg, tint) ->
                            Card(
                                onClick = { showMessage("Opening $title...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (title) {
                                            "Chapter-wise PYQs" -> Icon(Icons.Default.Description, null, tint = tint, modifier = Modifier.size(20.dp))
                                            "Year-wise Papers" -> Icon(Icons.Default.CalendarMonth, null, tint = tint, modifier = Modifier.size(20.dp))
                                            else -> Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = tint, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Study Material
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Study Material",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Revise concepts in different formats.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )

                        val materialItems = listOf(
                            Triple("Mind Map", Color(0xFFF3E5F5), Color(0xFF8E24AA)),
                            Triple("Study Notes", Color(0xFFFFF3E0), Color(0xFFFB8C00))
                        )

                        materialItems.forEach { (title, bg, tint) ->
                            Card(
                                onClick = { showMessage("Opening $title...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (title == "Mind Map") {
                                            Icon(Icons.Default.Hub, null, tint = tint, modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Default.Description, null, tint = tint, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Notes or PYQs placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tabs[selectedTab]} will be available soon.",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * General Mode (College & Beyond):
 * Preserves the approved Top Courses carousel, single Explore Courses grid,
 * Start Learning button, and Help & Support section.
 */
@Composable
private fun GeneralHomeContent(
    uiState: LandingUiState,
    onCourseClick: (Course) -> Unit,
    onStartLearning: () -> Unit,
    onSupportAction: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        if (uiState.featuredCourses.isNotEmpty()) {
            FeaturedCourseSection(
                featured = uiState.featuredCourses,
                onCourseClick = { onCourseClick(uiState.featuredCourses.first()) }
            )
        }

        CourseCatalogue(
            courses = uiState.exploreCourses,
            onCourseClick = onCourseClick
        )

        Button(
            onClick = onStartLearning,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 22.dp)
                .semantics { contentDescription = "Start learning" },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Start Learning", fontWeight = FontWeight.Bold)
        }

        SupportSection(onSupportAction = onSupportAction)
    }
}

@Composable
private fun TopBar(
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    isLoggedIn: Boolean,
    onProfile: () -> Unit,
    onOpenMyLearning: () -> Unit,
    onOpenSettings: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark()
            Spacer(Modifier.width(9.dp))
            EduNovaBrandName()
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                border = BorderStroke(1.dp, EduNovaBorder),
                modifier = Modifier
                    .size(42.dp)
                    .semantics { contentDescription = "Search courses" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SearchIcon(primaryDark = ink)
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.align(Alignment.Top)) {
                if (isLoggedIn) {
                    Surface(
                        onClick = { menuExpanded = true },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar_placeholder),
                            contentDescription = "Open profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onLogin,
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
                        modifier = Modifier
                            .height(42.dp)
                            .semantics { contentDescription = "Log in to EduNova" }
                    ) {
                        Text("Login", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded && isLoggedIn,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier
                .wrapContentWidth(Alignment.End)
                .align(Alignment.TopEnd),
            offset = androidx.compose.ui.unit.DpOffset(
                x = (-8).dp,
                y = 8.dp
            )
        ) {
            DropdownMenuItem(
                text = { Text("Profile") },
                onClick = { menuExpanded = false; onProfile() }
            )
            DropdownMenuItem(
                text = { Text("My Learning") },
                onClick = { menuExpanded = false; onOpenMyLearning() }
            )
            DropdownMenuItem(
                text = { Text("Setting") },
                onClick = { menuExpanded = false; onOpenSettings() }
            )
            DropdownMenuItem(
                text = { Text("Language") },
                onClick = { menuExpanded = false; languageDialogVisible = true }
            )
            DropdownMenuItem(
                text = { Text("Log out") },
                onClick = { menuExpanded = false; onLogout() }
            )
        }
    }
    if (languageDialogVisible) {
        AlertDialog(
            onDismissRequest = { languageDialogVisible = false },
            title = { Text("Choose language") },
            text = {
                Column {
                    listOf("English", "Hindi", "Marathi").forEach { language ->
                        TextButton(
                            onClick = {
                                onLanguageSelected(language)
                                languageDialogVisible = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(language, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun SearchIcon(primaryDark: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        drawCircle(
            color = primaryDark,
            radius = size.minDimension * .32f,
            center = androidx.compose.ui.geometry.Offset(size.width * .4f, size.height * .4f),
            style = Stroke(width = 2.2f)
        )
        drawLine(
            color = primaryDark,
            start = androidx.compose.ui.geometry.Offset(size.width * .63f, size.height * .63f),
            end = androidx.compose.ui.geometry.Offset(size.width * .9f, size.height * .9f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(EduNovaPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text("E", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
    }
}

@Composable
private fun FeaturedCourseSection(
    featured: List<Course>,
    onCourseClick: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val course = featured.getOrElse(selectedIndex) { featured.first() }
    val courseColor = course.accent.toColor()
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Top Courses", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Card(
            onClick = onCourseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, EduNovaBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(EduNovaPrimaryContainer, MaterialTheme.colorScheme.surface)
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "FEATURED COURSE",
                        color = courseColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "${course.name} Programming",
                        color = ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        course.description,
                        color = mutedInk,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        "Start Learning  →",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                CourseIcon(course, large = true)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = mutedInk, fontSize = 26.sp, modifier = Modifier.clickable {
                selectedIndex = (selectedIndex - 1 + featured.size) % featured.size
            })
            Spacer(Modifier.width(12.dp))
            featured.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == selectedIndex) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (index == selectedIndex) primaryColor else EduNovaBorder)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("›", color = mutedInk, fontSize = 26.sp, modifier = Modifier.clickable {
                selectedIndex = (selectedIndex + 1) % featured.size
            })
        }
    }
}

@Composable
private fun CourseCatalogue(
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val chunkedCourses = remember(courses) { courses.chunked(2) }

    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Explore Courses", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Build skills that move you forward.", color = mutedInk, fontSize = 13.sp)
        chunkedCourses.forEach { rowCourses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCourses.forEach { course ->
                    CourseCard(course, Modifier.weight(1f)) { onCourseClick(course) }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, modifier: Modifier, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CourseIcon(course)
            Text(course.name, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(course.description, color = mutedInk, fontSize = 12.sp, lineHeight = 17.sp)
            Text("View course  →", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CourseIcon(course: Course, large: Boolean = false) {
    val courseColor = course.accent.toColor()
    Box(
        modifier = Modifier
            .size(if (large) 70.dp else 42.dp)
            .clip(RoundedCornerShape(if (large) 20.dp else 12.dp))
            .background(EduNovaPrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val courseIcon = when (course.name.lowercase()) {
            "data science" -> Icons.Default.Analytics
            "dsa" -> Icons.Default.Hub
            "full stack" -> Icons.Default.Web
            else -> Icons.Default.Code
        }
        Icon(
            imageVector = courseIcon,
            contentDescription = "${course.name} course",
            tint = courseColor,
            modifier = Modifier.size(if (large) 34.dp else 24.dp)
        )
    }
}

@Composable
private fun SupportSection(onSupportAction: (String) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Help & Support", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Need a hand? We're here to help.", color = mutedInk, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        SupportRow(
            icon = SupportIcon.Help,
            title = "Help Center",
            description = "Find answers to common learning questions.",
            onClick = { onSupportAction("Help resources are being prepared.") }
        )
        SupportRow(
            icon = SupportIcon.Contact,
            title = "Contact Support",
            description = "Need help? Get in touch with our support team.",
            onClick = { onSupportAction("Support contact will be available soon.") }
        )
        SupportRow(
            icon = SupportIcon.About,
            title = "About EduNova",
            description = "Learn more about EduNova and its learning experience.",
            onClick = { onSupportAction("More about EduNova will be available soon.") }
        )
    }
}

@Composable
private fun SupportRow(
    icon: SupportIcon,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open $title" },
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, EduNovaBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(icon.containerColor),
                contentAlignment = Alignment.Center
            ) {
                SupportIcon(icon)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
            }
            ChevronRight()
        }
    }
}

private enum class SupportIcon(val containerColor: Color) {
    Help(EduNovaPrimaryContainer),
    Contact(EduNovaSurface),
    About(EduNovaSurface)
}

@Composable
private fun SupportIcon(icon: SupportIcon) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            SupportIcon.Help -> {
                val questionMark = Path().apply {
                    moveTo(size.width * .3f, size.height * .36f)
                    cubicTo(
                        size.width * .32f, size.height * .12f,
                        size.width * .7f, size.height * .12f,
                        size.width * .72f, size.height * .36f
                    )
                    cubicTo(
                        size.width * .73f, size.height * .56f,
                        size.width * .52f, size.height * .58f,
                        size.width * .5f, size.height * .75f
                    )
                }
                drawPath(questionMark, EduNovaPrimary, style = stroke)
                drawCircle(EduNovaPrimary, 1.2.dp.toPx(), center.copy(y = size.height * .9f))
            }
            SupportIcon.Contact -> {
                drawRoundRect(
                    color = EduNovaAccent,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .24f),
                    size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = stroke
                )
                drawLine(EduNovaAccent, androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .3f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f), stroke.width)
                drawLine(EduNovaAccent, androidx.compose.ui.geometry.Offset(size.width * .84f, size.height * .3f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f), stroke.width)
            }
            SupportIcon.About -> {
                drawCircle(EduNovaBorder, size.minDimension * .36f, style = stroke)
                drawCircle(EduNovaBorder, 1.2.dp.toPx(), center.copy(y = size.height * .36f))
                drawLine(EduNovaBorder, center.copy(y = size.height * .49f), center.copy(y = size.height * .7f), stroke.width)
            }
        }
    }
}

@Composable
private fun ChevronRight() {
    Canvas(Modifier.size(20.dp)) {
        drawLine(EduNovaBorder, androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(EduNovaBorder, androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .8f), 2.dp.toPx(), StrokeCap.Round)
    }
}
