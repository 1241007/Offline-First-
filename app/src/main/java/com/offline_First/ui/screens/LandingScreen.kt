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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.R
import com.offline_First.domain.model.ContinueLearningItem
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.StudyFocusItem
import com.offline_First.domain.model.Subject
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

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(bottom = 96.dp)
                    .navigationBarsPadding()
            ) {
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
                                onContinueLearning = onOpenMyLearning,
                                onSubjectClick = { showMessage("${it.name} materials will be available soon.") },
                                onNextStepClick = onOpenMyLearning
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

            // Ask AI Floating Action Button: sits cleanly above bottom navigation bar
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

            // EduNova Persistent Bottom Navigation Bar
            EduNovaBottomNavigation(
                selectedItem = EduNovaBottomNavItem.HOME,
                onNavigate = { item ->
                    when (item) {
                        EduNovaBottomNavItem.HOME -> {
                            scope.launch { scrollState.animateScrollTo(0) }
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
@Composable
private fun SchoolHomeContent(
    uiState: LandingUiState,
    onContinueLearning: () -> Unit,
    onSubjectClick: (Subject) -> Unit,
    onNextStepClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Personalized Greeting
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Good morning,",
                color = mutedInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${uiState.greetingName} 👋",
                color = ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's continue your learning.",
                color = mutedInk,
                fontSize = 14.sp
            )
        }

        // 2. Primary Hero Card: Continue Learning
        uiState.continueLearning?.let { item ->
            SchoolContinueLearningCard(
                item = item,
                onContinue = onContinueLearning
            )
        }

        // 3. My Subjects: Academic Subjects Grid
        SchoolSubjectsSection(
            subjects = uiState.subjects,
            onSubjectClick = onSubjectClick
        )

        // 4. Your Next Step
        uiState.studyFocus?.let { nextStep ->
            SchoolNextStepCard(
                item = nextStep,
                onContinue = onNextStepClick
            )
        }
    }
}

@Composable
private fun SchoolContinueLearningCard(
    item: ContinueLearningItem,
    onContinue: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EduNovaPrimaryContainer),
        border = BorderStroke(1.dp, EduNovaBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CONTINUE LEARNING",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.subjectName,
                    color = ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.topicName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.lessonInfo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(item.progress * 100).toInt()}%",
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = primaryColor,
                trackColor = Color.White
            )
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Continue  →", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SchoolSubjectsSection(
    subjects: List<Subject>,
    onSubjectClick: (Subject) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Subjects", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "See All",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { }
            )
        }

        val rows = remember(subjects) { subjects.chunked(2) }
        rows.forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SchoolSubjectCard(
                        subject = subject,
                        modifier = Modifier.weight(1f),
                        onClick = { onSubjectClick(subject) }
                    )
                }
                if (rowSubjects.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SchoolSubjectCard(
    subject: Subject,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EduNovaPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(subject.icon, fontSize = 22.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = subject.name,
                    color = ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (subject.progress != null) {
                    Text(
                        text = "${(subject.progress * 100).toInt()}% completed",
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "${subject.totalTopics} topics",
                        color = mutedInk,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolNextStepCard(
    item: StudyFocusItem,
    onContinue: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your Next Step", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, EduNovaBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(item.subjectName, color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(item.actionTitle, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(item.lessonInfo, color = mutedInk, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("Continue  →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark()
            Spacer(Modifier.width(9.dp))
            Text("EduNova", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Top Courses", color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(
            onClick = onCourseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = courseColor)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FEATURED COURSE", color = Color.White.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${course.name} Programming", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(course.description, color = Color.White.copy(alpha = .88f), fontSize = 13.sp, lineHeight = 19.sp)
                    Text("Start Learning  →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Explore Courses", color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Build skills that move you forward.", color = mutedInk, fontSize = 14.sp)
        Spacer(Modifier.height(11.dp))
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
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            CourseIcon(course)
            Text(course.name, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(course.description, color = mutedInk, fontSize = 12.sp, lineHeight = 17.sp)
            Text("View course  →", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            .background(Color.White.copy(alpha = if (large) .2f else .12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(course.icon, color = if (large) Color.White else courseColor, fontSize = if (large) 20.sp else 13.sp, fontWeight = FontWeight.Black)
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
