package com.offline_First.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.offline_First.R

private val Primary = Color(0xFF2563EB)
private val PrimaryDark = Color(0xFF0F172A)
private val Accent = Color(0xFF14B8A6)
private val Ink = Color(0xFF0F172A)
private val MutedInk = Color(0xFF64748B)
private val PageBackground = Color(0xFFF8FAFC)
private val Border = Color(0xFFE2E8F0)
private val SoftPrimary = Color(0xFFEFF6FF)

private data class Course(
    val name: String,
    val description: String,
    val icon: String,
    val color: Color
)

private val courses = listOf(
    Course("Python", "Build a strong programming foundation.", "Py", Color(0xFF3776AB)),
    Course("Java", "Learn practical object-oriented programming.", "J", Color(0xFFE76F00)),
    Course("C++", "Strengthen logic with powerful fundamentals.", "C+", Color(0xFF2563EB)),
    Course("Data Science", "Turn data into useful insights.", "DS", Color(0xFF7C3AED)),
    Course("DSA", "Master problem solving and algorithms.", "⌘", Color(0xFF0F766E)),
    Course("Full Stack", "Create complete modern web experiences.", "</>", Color(0xFFDB2777))
)

@Composable
fun LandingScreen(
    onLogin: () -> Unit = {},
    isLoggedIn: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val catalogueRequester = remember { BringIntoViewRequester() }
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        containerColor = PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(bottom = 84.dp)
                    .navigationBarsPadding()
            ) {
                TopBar(
                    onSearch = { showMessage("Course search will be available soon.") },
                    onLogin = onLogin,
                    isLoggedIn = isLoggedIn,
                    onProfile = { showMessage("Your profile will be available soon.") }
                )
                FeaturedCourseSection(
                    onCourseClick = { showMessage("Course content will be available soon.") }
                )
                QuickActions(
                    onCourses = { scope.launch { catalogueRequester.bringIntoView() } },
                    onRoadmap = { showMessage("Your personalized roadmap will be available soon.") },
                    onChatbot = { showMessage("Your AI learning assistant will be available soon.") }
                )
                CourseCatalogue(
                    onCourseClick = { showMessage("Course content will be available soon.") },
                    modifier = Modifier.bringIntoViewRequester(catalogueRequester)
                )
                Button(
                    onClick = { showMessage("Your learning journey will be ready soon.") },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 22.dp)
                        .semantics { contentDescription = "Start learning" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Start Learning", fontWeight = FontWeight.Bold)
                }
                SupportSection(onSupportAction = showMessage)
            }
            Button(
                onClick = { showMessage("Your AI learning assistant will be available soon.") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 18.dp)
                    .semantics { contentDescription = "Ask AI" },
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("✦  Ask AI", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TopBar(
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    isLoggedIn: Boolean,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark()
        Spacer(Modifier.width(9.dp))
        Text("EduNova", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onSearch,
            shape = RoundedCornerShape(11.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier
                .size(42.dp)
                .semantics { contentDescription = "Search courses" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                SearchIcon()
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isLoggedIn) {
            Surface(
                onClick = onProfile,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Border),
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
                border = BorderStroke(1.dp, Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                modifier = Modifier
                    .height(42.dp)
                    .semantics { contentDescription = "Log in to EduNova" }
            ) {
                Text("Login", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchIcon() {
    Canvas(modifier = Modifier.size(19.dp)) {
        drawCircle(
            color = PrimaryDark,
            radius = size.minDimension * .32f,
            center = androidx.compose.ui.geometry.Offset(size.width * .4f, size.height * .4f),
            style = Stroke(width = 2.2f)
        )
        drawLine(
            color = PrimaryDark,
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
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Text("E", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
    }
}

@Composable
private fun FeaturedCourseSection(onCourseClick: () -> Unit) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val featured = listOf(courses[0], courses[4], courses[5])
    val course = featured[selectedIndex]

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Top Courses", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(
            onClick = onCourseClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = course.color)
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
            Text("‹", color = MutedInk, fontSize = 26.sp, modifier = Modifier.clickable {
                selectedIndex = (selectedIndex - 1 + featured.size) % featured.size
            })
            Spacer(Modifier.width(12.dp))
            featured.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == selectedIndex) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (index == selectedIndex) Primary else Border)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("›", color = MutedInk, fontSize = 26.sp, modifier = Modifier.clickable {
                selectedIndex = (selectedIndex + 1) % featured.size
            })
        }
    }
}

@Composable
private fun CourseCatalogue(
    onCourseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Explore Courses", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Build skills that move you forward.", color = MutedInk, fontSize = 14.sp)
        Spacer(Modifier.height(11.dp))
        courses.chunked(2).forEach { rowCourses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCourses.forEach { course ->
                    CourseCard(course, Modifier.weight(1f), onCourseClick)
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            CourseIcon(course)
            Text(course.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(course.description, color = MutedInk, fontSize = 12.sp, lineHeight = 17.sp)
            Text("View course  →", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CourseIcon(course: Course, large: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (large) 70.dp else 42.dp)
            .clip(RoundedCornerShape(if (large) 20.dp else 12.dp))
            .background(Color.White.copy(alpha = if (large) .2f else .12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(course.icon, color = if (large) Color.White else course.color, fontSize = if (large) 20.sp else 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuickActions(
    onCourses: () -> Unit,
    onRoadmap: () -> Unit,
    onChatbot: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("Courses", Modifier.weight(1f), onCourses)
        ActionButton("Roadmap", Modifier.weight(1f), onRoadmap)
        ActionButton("Chatbot", Modifier.weight(1f), onChatbot)
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
    modifier = modifier
        .height(46.dp)
        .semantics { contentDescription = "Open $label" },
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SupportSection(onSupportAction: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Help & Support", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Need a hand? We're here to help.", color = MutedInk, fontSize = 14.sp)
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
        border = BorderStroke(1.dp, Border)
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
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = MutedInk, fontSize = 12.sp, lineHeight = 16.sp)
            }
            ChevronRight()
        }
    }
}

private enum class SupportIcon(val containerColor: Color) {
    Help(SoftPrimary),
    Contact(Color(0xFFE6FFFB)),
    About(Color(0xFFF1F5F9))
}

@Composable
private fun SupportIcon(icon: SupportIcon) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            SupportIcon.Help -> {
                drawArc(Primary, 205f, 230f, false, style = stroke)
                drawCircle(Primary, 1.2.dp.toPx(), center.copy(y = size.height * .78f))
                drawLine(Primary, center.copy(y = size.height * .43f), center.copy(y = size.height * .54f), stroke.width)
            }
            SupportIcon.Contact -> {
                drawRoundRect(
                    color = Accent,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .24f),
                    size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = stroke
                )
                drawLine(Accent, androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .3f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f), stroke.width)
                drawLine(Accent, androidx.compose.ui.geometry.Offset(size.width * .84f, size.height * .3f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .56f), stroke.width)
            }
            SupportIcon.About -> {
                drawCircle(MutedInk, size.minDimension * .36f, style = stroke)
                drawCircle(MutedInk, 1.2.dp.toPx(), center.copy(y = size.height * .36f))
                drawLine(MutedInk, center.copy(y = size.height * .49f), center.copy(y = size.height * .7f), stroke.width)
            }
        }
    }
}

@Composable
private fun ChevronRight() {
    Canvas(Modifier.size(20.dp)) {
        drawLine(MutedInk, androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(MutedInk, androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .8f), 2.dp.toPx(), StrokeCap.Round)
    }
}
