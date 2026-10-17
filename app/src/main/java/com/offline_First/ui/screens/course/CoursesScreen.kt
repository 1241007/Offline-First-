package com.offline_First.ui.screens.course

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.R
import com.offline_First.domain.model.Course
import com.offline_First.domain.model.CourseAccent
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
import com.offline_First.ui.utils.IconResolver
import kotlinx.coroutines.launch

private fun CourseAccent.toColor(): Color = when (this) {
    CourseAccent.PRIMARY -> EduNovaPrimary
    CourseAccent.SECONDARY -> EduNovaSecondary
    CourseAccent.ACCENT -> EduNovaAccent
}

/**
 * Standalone Courses screen displaying the complete EduNova Courses experience.
 * Includes top bar, featured course carousel, course catalogue, and bottom navigation.
 */
@Composable
fun CoursesScreen(
    onNavigate: (EduNovaBottomNavItem) -> Unit = {},
    onCourseClick: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenMyLearning: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    isLoggedIn: Boolean = false,
    viewModel: LandingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            EduNovaBottomNavigation(
                selectedItem = EduNovaBottomNavItem.COURSES,
                isSchoolMode = false,
                onNavigate = onNavigate
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            CoursesTopBar(
                onSearch = { showMessage("Search will be available soon.") },
                onLogin = onLogin,
                isLoggedIn = isLoggedIn,
                onProfile = onOpenProfile,
                onOpenMyLearning = onOpenMyLearning,
                onOpenSettings = onOpenSettings,
                onLanguageSelected = onLanguageSelected,
                onLogout = onLogout,
                onToggleMode = {
                    viewModel.toggleEducationMode()
                    showMessage("Switched to CBSE School Mode")
                }
            )

            CoursesContent(
                uiState = uiState,
                onCourseClick = { course -> onCourseClick(course.id) },
                onRetryCourses = { viewModel.retryCourses() },
                onStartLearning = {
                    val targetCourse = uiState.exploreCourses.firstOrNull() ?: uiState.featuredCourses.firstOrNull()
                    if (targetCourse != null) {
                        onCourseClick(targetCourse.id)
                    } else {
                        showMessage("Your learning journey will be ready soon.")
                    }
                },
                onSupportAction = showMessage
            )
        }
    }
}

/**
 * Overloaded CoursesScreen that accepts explicit LandingUiState for flexible composition.
 */
@Composable
fun CoursesScreen(
    uiState: LandingUiState,
    onCourseClick: (String) -> Unit,
    onRetryCourses: () -> Unit = {},
    onStartLearning: () -> Unit = {},
    onSupportAction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        CoursesContent(
            uiState = uiState,
            onCourseClick = { course -> onCourseClick(course.id) },
            onRetryCourses = onRetryCourses,
            onStartLearning = onStartLearning,
            onSupportAction = onSupportAction
        )
    }
}

@Composable
private fun CoursesTopBar(
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    isLoggedIn: Boolean,
    onProfile: () -> Unit,
    onOpenMyLearning: () -> Unit,
    onOpenSettings: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onToggleMode: () -> Unit = {}
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

/**
 * Complete Courses Content composable (extracted from GeneralHomeContent in LandingScreen).
 * Renders loading, error retry card, empty state, featured courses, course catalogue,
 * and help & support section.
 */
@Composable
fun CoursesContent(
    uiState: LandingUiState,
    onCourseClick: (Course) -> Unit,
    onRetryCourses: () -> Unit = {},
    onStartLearning: () -> Unit,
    onSupportAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        when {
            uiState.isCoursesLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Loading courses...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            uiState.coursesError != null -> {
                val isNetworkErr = uiState.isOffline ||
                    uiState.coursesError.contains("internet", ignoreCase = true) ||
                    uiState.coursesError.contains("offline", ignoreCase = true) ||
                    uiState.coursesError.contains("connect", ignoreCase = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isNetworkErr) Icons.Default.CloudOff else Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (isNetworkErr) "No courses saved for offline learning" else "Course Service Error",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = if (isNetworkErr) {
                                "Connect to the internet and save a course to access it offline."
                            } else {
                                uiState.coursesError
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = onRetryCourses,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            !uiState.isCoursesLoading && uiState.coursesError == null && uiState.exploreCourses.isEmpty() -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, EduNovaBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isOffline) Icons.Default.CloudOff else Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (uiState.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (uiState.isOffline) "No courses saved for offline learning" else "No courses available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = if (uiState.isOffline) {
                                "Connect to the internet and save a course to access it offline."
                            } else {
                                "Check back soon for new courses and learning paths."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            else -> {
                if (uiState.featuredCourses.isNotEmpty()) {
                    FeaturedCourseSection(
                        featured = uiState.featuredCourses,
                        onCourseClick = onCourseClick
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
            }
        }

        SupportSection(onSupportAction = onSupportAction)
    }
}

/**
 * Backward-compatible alias for GeneralHomeContent.
 */
@Composable
fun GeneralHomeContent(
    uiState: LandingUiState,
    onCourseClick: (Course) -> Unit,
    onRetryCourses: () -> Unit = {},
    onStartLearning: () -> Unit,
    onSupportAction: (String) -> Unit
) {
    CoursesContent(
        uiState = uiState,
        onCourseClick = onCourseClick,
        onRetryCourses = onRetryCourses,
        onStartLearning = onStartLearning,
        onSupportAction = onSupportAction
    )
}

@Composable
fun FeaturedCourseSection(
    featured: List<Course>,
    onCourseClick: (Course) -> Unit
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
            onClick = { onCourseClick(course) },
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
                        course.name,
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
fun CourseCatalogue(
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
                if (rowCourses.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, modifier: Modifier, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CourseIcon(course)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = course.level.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedInk,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = course.name,
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.description,
                    color = mutedInk,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (course.totalLessons > 0 && course.completedLessons > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (course.progress >= 1f) "Completed" else "${course.completedLessons}/${course.totalLessons} lessons",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (course.progress >= 1f) EduNovaSecondary else primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(course.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedInk,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { course.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (course.progress >= 1f) EduNovaSecondary else primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (course.totalLessons > 0) "${course.totalLessons} lessons" else "View course",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseIcon(course: Course, large: Boolean = false) {
    val courseColor = course.accent.toColor()
    Box(
        modifier = Modifier
            .size(if (large) 70.dp else 38.dp)
            .clip(RoundedCornerShape(if (large) 20.dp else 10.dp))
            .background(EduNovaPrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = IconResolver.resolve(course.icon),
            contentDescription = "${course.name} course",
            tint = courseColor,
            modifier = Modifier.size(if (large) 34.dp else 20.dp)
        )
    }
}

@Composable
fun SupportSection(onSupportAction: (String) -> Unit) {
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
fun SupportRow(
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

enum class SupportIcon(val containerColor: Color) {
    Help(EduNovaPrimaryContainer),
    Contact(EduNovaSurface),
    About(EduNovaSurface)
}

@Composable
fun SupportIcon(icon: SupportIcon) {
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
fun ChevronRight() {
    Canvas(Modifier.size(20.dp)) {
        drawLine(EduNovaBorder, androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(EduNovaBorder, androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .8f), 2.dp.toPx(), StrokeCap.Round)
    }
}
