package com.offline_First.ui.screens.course

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.domain.model.CourseAccent
import com.offline_First.domain.model.CourseQuiz
import com.offline_First.domain.model.UiState
import com.offline_First.ui.components.EduNovaBadge
import com.offline_First.ui.theme.EduNovaAccent
import com.offline_First.ui.theme.EduNovaBorder
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.EduNovaPrimaryContainer
import com.offline_First.ui.theme.EduNovaSecondary
import com.offline_First.ui.theme.EduNovaSuccess
import kotlinx.coroutines.launch

private fun CourseAccent.toColor(): Color = when (this) {
    CourseAccent.PRIMARY -> EduNovaPrimary
    CourseAccent.SECONDARY -> EduNovaSecondary
    CourseAccent.ACCENT -> EduNovaAccent
}

/**
 * Validates and safely launches an external learning resource URL via ACTION_VIEW intent.
 * Permits only http and https schemes.
 */
fun launchExternalResource(context: Context, url: String?): Result<Unit> {
    if (url.isNullOrBlank()) {
        return Result.failure(IllegalArgumentException("No resource URL provided for this lesson."))
    }
    val trimmed = url.trim()
    val uri = try {
        Uri.parse(trimmed)
    } catch (e: Exception) {
        return Result.failure(IllegalArgumentException("Invalid resource URL format."))
    }
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
        return Result.failure(IllegalArgumentException("Unsupported URL scheme: $scheme. Only http and https are permitted."))
    }
    return try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Result.success(Unit)
    } catch (e: ActivityNotFoundException) {
        Result.failure(IllegalStateException("No browser or application found to open this resource."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@Composable
fun LessonViewerScreen(
    courseId: String,
    lessonId: String,
    onBack: () -> Unit,
    onNavigateToLesson: (lessonId: String) -> Unit = {},
    onOpenRevisionList: () -> Unit = {},
    viewModel: LessonViewerViewModel = viewModel(
        key = "$courseId-$lessonId",
        factory = LessonViewerViewModel.Factory(courseId, lessonId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LessonViewerTopBar(
                onBack = onBack,
                courseName = when (val s = uiState.state) {
                    is UiState.Success -> s.data.course.name
                    else -> "Lesson Viewer"
                },
                moduleTitle = when (val s = uiState.state) {
                    is UiState.Success -> s.data.module.title
                    else -> null
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
            when (val state = uiState.state) {
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
                            "Loading lesson content...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                is UiState.Error -> {
                    LessonErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadLesson(lessonId) },
                        onBack = onBack
                    )
                }
                is UiState.Empty -> {
                    LessonErrorState(
                        message = uiState.errorMessage ?: "We could not find the requested lesson.",
                        onRetry = { viewModel.loadLesson(lessonId) },
                        onBack = onBack
                    )
                }
                is UiState.Success -> {
                    val data = state.data
                    val lesson = data.lesson

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Lesson Overview Card
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
                                                listOf(
                                                    EduNovaPrimaryContainer.copy(alpha = 0.45f),
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        )
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Progress counter
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LESSON ${data.currentLessonIndex} OF ${data.totalLessonsInCourse}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                        if (uiState.isCompleted) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Completed",
                                                    tint = EduNovaSuccess,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Completed",
                                                    color = EduNovaSuccess,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Lesson Title
                                    Text(
                                        text = lesson.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    // Lesson Description
                                    lesson.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    // Metadata Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val contentTypeIcon = when (lesson.contentType.lowercase()) {
                                            "quiz" -> Icons.Default.Quiz
                                            "article" -> Icons.AutoMirrored.Filled.Article
                                            else -> Icons.Default.PlayCircleOutline
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(EduNovaPrimaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = contentTypeIcon,
                                                contentDescription = null,
                                                tint = EduNovaPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = lesson.contentType.replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EduNovaPrimary
                                            )
                                        }

                                        lesson.durationMinutes?.let { mins ->
                                            EduNovaBadge(
                                                text = "$mins min",
                                                icon = Icons.Default.Timer,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        lesson.resourceProvider?.let { provider ->
                                            EduNovaBadge(
                                                text = provider,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Learning Resource Cards (Reading / Documentation and Video Walkthrough)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, EduNovaBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reading & Documentation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        lesson.resourceProvider?.let { provider ->
                                            EduNovaBadge(
                                                text = provider,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Explore full, curated lesson materials from ${lesson.resourceProvider ?: "the official provider"}. External content will open safely in your web browser.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )

                                    if (!lesson.resourceUrl.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                val launchResult = launchExternalResource(context, lesson.resourceUrl)
                                                launchResult.onFailure { err ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            err.localizedMessage ?: "Failed to open resource link."
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .semantics { contentDescription = "Open learning resource" },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Open Reading Resource",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Reading resource currently unavailable for this lesson.",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Video Tutorial Resource Card (Rendered only when valid videoUrl exists)
                        if (!lesson.videoUrl.isNullOrBlank()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, EduNovaBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Video Lesson / Tutorial",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            EduNovaBadge(
                                                text = lesson.videoProvider ?: "YouTube",
                                                icon = Icons.Default.PlayCircleOutline,
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Text(
                                            text = "Watch the companion video lesson from ${lesson.videoProvider ?: "YouTube"}. Opens safely in your YouTube app or web browser.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                        Button(
                                            onClick = {
                                                val launchResult = launchExternalResource(context, lesson.videoUrl)
                                                launchResult.onFailure { err ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            err.localizedMessage ?: "Failed to open video link."
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .semantics { contentDescription = "Watch video lesson" },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircleOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Watch Video Lesson",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Lesson Actions: Completion & Revision
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, EduNovaBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Lesson Actions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Mark Complete Button
                                        Button(
                                            onClick = {
                                                viewModel.toggleLessonCompleted()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (!uiState.isCompleted) "Lesson marked as complete."
                                                        else "Lesson marked as incomplete."
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (uiState.isCompleted) EduNovaSuccess
                                                else MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isCompleted) Icons.Default.CheckCircle
                                                else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (uiState.isCompleted) "Completed" else "Mark Complete",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Mark for Revision Button
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.toggleMarkForRevision()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (!uiState.isMarkedForRevision) "Marked for revision"
                                                        else "Removed from revision"
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (uiState.isMarkedForRevision) MaterialTheme.colorScheme.primary else EduNovaBorder
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isMarkedForRevision) Icons.Default.Bookmark
                                                else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = if (uiState.isMarkedForRevision) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (uiState.isMarkedForRevision) "For Revision" else "Mark Revision",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = if (uiState.isMarkedForRevision) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = EduNovaSuccess
                                        )
                                        Text(
                                            text = "Completions & revisions saved locally",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Interactive Practice Quiz Section (Step 4)
                        if (lesson.quiz != null) {
                            item {
                                LessonInteractiveQuizCard(
                                    quiz = lesson.quiz,
                                    quizState = uiState.quizState,
                                    onSelectOption = viewModel::selectQuizOption,
                                    onSubmitAnswer = viewModel::submitQuizAnswer,
                                    onNextQuestion = viewModel::nextQuizQuestion,
                                    onRetryQuiz = viewModel::retryQuiz
                                )
                            }
                        }

                        // Reserved Learn with AI Card (Step 5)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, EduNovaBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
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
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Learn with AI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Interactive tutoring with lesson context (Available in Step 5)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    EduNovaBadge(
                                        text = "Step 5",
                                        containerColor = EduNovaPrimaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Bottom Navigation: Previous and Next Lesson Controls
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        data.previousLesson?.let { prev ->
                                            onNavigateToLesson(prev.id)
                                        }
                                    },
                                    enabled = data.previousLesson != null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Previous",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        data.nextLesson?.let { next ->
                                            onNavigateToLesson(next.id)
                                        }
                                    },
                                    enabled = data.nextLesson != null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Next",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
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

@Composable
private fun LessonViewerTopBar(
    onBack: () -> Unit,
    courseName: String,
    moduleTitle: String?,
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
                    contentDescription = "Back to course syllabus",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = courseName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                moduleTitle?.let { mod ->
                    Text(
                        text = mod,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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

@Composable
private fun LessonErrorState(
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
            text = "Unable to load lesson",
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
                Text("Back to Course")
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

@Composable
fun LessonInteractiveQuizCard(
    quiz: CourseQuiz,
    quizState: CourseQuizState,
    onSelectOption: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRetryQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EduNovaBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduNovaPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = EduNovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Lesson Practice Quiz",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (quiz.questions.isEmpty()) {
                Text(
                    text = "No questions available for this practice quiz yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (quizState.isQuizFinished) {
                // Quiz Completed / Score Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EduNovaPrimaryContainer,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = EduNovaPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Text(
                        text = "Quiz Completed!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val percentage = (quizState.score.toFloat() / quiz.questions.size * 100).toInt()
                    Text(
                        text = "You scored ${quizState.score} out of ${quiz.questions.size} ($percentage%)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onRetryQuiz,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Quiz", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                val currentQuestion = quiz.questions[quizState.currentQuestionIndex]
                val totalQuestions = quiz.questions.size

                // Progress Bar & Counter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${quizState.currentQuestionIndex + 1} of $totalQuestions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Score: ${quizState.score}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (quizState.currentQuestionIndex + 1).toFloat() / totalQuestions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = EduNovaBorder
                    )
                }

                // Question Text
                Text(
                    text = currentQuestion.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )

                // Options
                currentQuestion.options.forEachIndexed { index, optionText ->
                    val isSelected = quizState.selectedOptionIndex == index
                    val isCorrect = index == currentQuestion.correctIndex

                    val containerColor = when {
                        quizState.isAnswerSubmitted && isCorrect -> Color(0xFFE8F5E9)
                        quizState.isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        isSelected -> EduNovaPrimaryContainer
                        else -> MaterialTheme.colorScheme.background
                    }

                    val borderColor = when {
                        quizState.isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
                        quizState.isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFE57373)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> EduNovaBorder
                    }

                    Surface(
                        onClick = {
                            if (!quizState.isAnswerSubmitted) {
                                onSelectOption(index)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    quizState.isAnswerSubmitted && isCorrect -> Color(0xFF4CAF50)
                                    quizState.isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFE57373)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                },
                                border = if (!isSelected && (!quizState.isAnswerSubmitted || !isCorrect)) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                                } else null,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (quizState.isAnswerSubmitted && isCorrect) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    } else if (quizState.isAnswerSubmitted && isSelected && !isCorrect) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    } else if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = optionText,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Explanation Box when submitted
                AnimatedVisibility(
                    visible = quizState.isAnswerSubmitted,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EduNovaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = EduNovaAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Explanation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Action Button: Submit or Next
                if (!quizState.isAnswerSubmitted) {
                    Button(
                        onClick = onSubmitAnswer,
                        enabled = quizState.selectedOptionIndex != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Answer", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (quizState.currentQuestionIndex < totalQuestions - 1) "Next Question" else "View Results",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
