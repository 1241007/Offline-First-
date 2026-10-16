package com.offline_First

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.offline_First.data.AppContainer
import com.offline_First.data.AuthState
import com.offline_First.ui.navigation.AppDestination
import com.offline_First.ui.screens.AIWorkspaceScreen
import com.offline_First.ui.screens.LandingScreen
import com.offline_First.ui.screens.MyLearningScreen
import com.offline_First.ui.screens.ProfileScreen
import com.offline_First.ui.screens.RoadmapBuilderScreen
import com.offline_First.ui.screens.RoadmapScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offline_First.ui.screens.roadmap.PersonalizedAssessmentScreen
import com.offline_First.ui.screens.roadmap.PersonalizedRoadmapScreen
import com.offline_First.ui.screens.roadmap.PersonalizedRoadmapViewModel
import com.offline_First.ui.screens.roadmap.PersonalizedTopicDetailScreen
import com.offline_First.ui.screens.SettingsScreen
import com.offline_First.ui.screens.auth.ForgotPasswordScreen
import com.offline_First.ui.screens.auth.LoginScreen
import com.offline_First.ui.screens.auth.RegisterScreen
import com.offline_First.ui.screens.course.CourseDetailScreen
import com.offline_First.ui.screens.course.LessonViewerScreen
import com.offline_First.ui.screens.course.RevisionListScreen
import com.offline_First.ui.theme.EduNovaBackground
import com.offline_First.ui.theme.EduNovaPrimary
import com.offline_First.ui.theme.OfflineFirstTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            OfflineFirstTheme {
                val coroutineScope = rememberCoroutineScope()
                val sessionManager = remember { AppContainer.sessionManager }
                val authRepository = remember { AppContainer.authRepository }
                val authState by sessionManager.authState.collectAsState()

                var destination by rememberSaveable { mutableStateOf(AppDestination.LOADING) }
                val navigationStack = rememberSaveable { mutableStateListOf<AppDestination>() }
                var openWorkspaceTools by rememberSaveable { mutableStateOf(false) }
                var selectedLanguage by rememberSaveable { mutableStateOf("English") }
                var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
                val profileRepository = remember { AppContainer.profileRepository }


                val profile by profileRepository.observeUserProfile().collectAsState(
                    initial = com.offline_First.data.local.LocalProfileRepository.defaultProfile()
                )
                val personalizedRoadmapViewModel: PersonalizedRoadmapViewModel = viewModel()

                // Restore session on startup
                LaunchedEffect(Unit) {
                    authRepository.restoreSession()
                }

                // React to authState transitions
                LaunchedEffect(authState) {
                    when (authState) {
                        AuthState.Initializing -> {
                            if (destination != AppDestination.LOADING) {
                                destination = AppDestination.LOADING
                            }
                        }
                        is AuthState.Authenticated -> {
                            if (destination == AppDestination.LOADING || destination == AppDestination.LOGIN) {
                                destination = AppDestination.LANDING
                            }
                        }
                        AuthState.SessionExpired -> {
                            destination = AppDestination.LOGIN
                        }
                        AuthState.Unauthenticated -> {
                            if (destination == AppDestination.LOADING) {
                                destination = AppDestination.LANDING
                            }
                        }
                        AuthState.Authenticating, AuthState.Refreshing -> Unit
                    }
                }

                val isLoggedIn = authState is AuthState.Authenticated

                val pushDestination: (AppDestination) -> Unit = { next ->
                    if (destination != next) {
                        navigationStack.add(destination)
                        destination = next
                    }
                }

                val navigateBack: () -> Unit = {
                    val previous = navigationStack.lastOrNull()
                    if (previous != null) {
                        navigationStack.removeAt(navigationStack.lastIndex)
                        destination = previous
                    } else if (destination != AppDestination.LANDING) {
                        destination = AppDestination.LANDING
                    }
                }

                BackHandler(enabled = destination != AppDestination.LANDING && destination != AppDestination.LOADING) {
                    navigateBack()
                }

                when (destination) {
                    AppDestination.LOADING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(EduNovaBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EduNovaPrimary)
                        }
                    }
                    AppDestination.LANDING -> LandingScreen(
                        onLogin = { pushDestination(AppDestination.LOGIN) },
                        isLoggedIn = isLoggedIn,
                        onAskAI = {
                            openWorkspaceTools = false
                            pushDestination(AppDestination.AI_WORKSPACE)
                        },
                        onOpenAITools = {
                            openWorkspaceTools = true
                            pushDestination(AppDestination.AI_WORKSPACE)
                        },
                        onRoadmap = { pushDestination(AppDestination.ROADMAP) },
                        onOpenProfile = { pushDestination(AppDestination.PROFILE) },
                        onOpenMyLearning = { pushDestination(AppDestination.MY_LEARNING) },
                        onOpenSettings = { pushDestination(AppDestination.SETTINGS) },
                        onLanguageSelected = { selectedLanguage = it },
                        onLogout = {
                            coroutineScope.launch {
                                val currentRt = sessionManager.currentRefreshToken()
                                authRepository.logout(currentRt)
                                pushDestination(AppDestination.LOGIN)
                            }
                        },
                        onCourseClick = { courseId ->
                            selectedCourseId = courseId
                            pushDestination(AppDestination.COURSE_DETAIL)
                        }
                    )

                    AppDestination.LOGIN -> LoginScreen(
                        onRegister = { pushDestination(AppDestination.REGISTER) },
                        onForgotPassword = { pushDestination(AppDestination.FORGOT_PASSWORD) },
                        onLoginSuccess = {
                            pushDestination(AppDestination.LANDING)
                        }
                    )
                    AppDestination.REGISTER -> RegisterScreen(
                        onLogin = { pushDestination(AppDestination.LOGIN) }
                    )
                    AppDestination.FORGOT_PASSWORD -> ForgotPasswordScreen(
                        onLogin = { pushDestination(AppDestination.LOGIN) }
                    )
                    AppDestination.AI_WORKSPACE -> AIWorkspaceScreen(
                        isClass912Student = (profile?.educationMode == com.offline_First.domain.model.EducationMode.SCHOOL),
                        onBack = { navigateBack() },
                        openToolsOnStart = openWorkspaceTools
                    )
                    AppDestination.ROADMAP -> RoadmapScreen(
                        onBack = navigateBack,
                        onBuildRoadmap = { pushDestination(AppDestination.ROADMAP_BUILDER) },
                        onOpenPersonalizedRoadmap = { pushDestination(AppDestination.PERSONALIZED_ROADMAP) },
                        onOpenSavedRoadmap = { roadmap ->
                            personalizedRoadmapViewModel.openRoadmap(roadmap)
                            pushDestination(AppDestination.PERSONALIZED_ROADMAP)
                        },
                        onStartAssessmentForCourse = { courseName ->
                            personalizedRoadmapViewModel.updateCourseInput(courseName)
                            personalizedRoadmapViewModel.validateAndStartAssessment(courseName)
                            pushDestination(AppDestination.ROADMAP_BUILDER)
                        },
                        personalizedViewModel = personalizedRoadmapViewModel
                    )
                    AppDestination.ROADMAP_BUILDER -> PersonalizedAssessmentScreen(
                        onBack = { navigateBack() },
                        onRoadmapReady = { pushDestination(AppDestination.PERSONALIZED_ROADMAP) },
                        viewModel = personalizedRoadmapViewModel
                    )
                    AppDestination.PERSONALIZED_ROADMAP -> PersonalizedRoadmapScreen(
                        onBack = { navigateBack() },
                        onRetakeAssessment = { pushDestination(AppDestination.ROADMAP_BUILDER) },
                        onOpenTopicDetail = { pushDestination(AppDestination.TOPIC_DETAIL) },
                        viewModel = personalizedRoadmapViewModel
                    )
                    AppDestination.TOPIC_DETAIL -> PersonalizedTopicDetailScreen(
                        onBack = { navigateBack() },
                        viewModel = personalizedRoadmapViewModel
                    )
                    AppDestination.MY_LEARNING -> MyLearningScreen(
                        onBack = navigateBack
                    )
                    AppDestination.PROFILE -> ProfileScreen(
                        onBack = navigateBack,
                        onLanguage = { pushDestination(AppDestination.SETTINGS) }
                    )
                    AppDestination.SETTINGS -> SettingsScreen(
                        onBack = navigateBack,
                        onOpenProfile = { pushDestination(AppDestination.PROFILE) },
                        language = selectedLanguage,
                        onLanguageSelected = { selectedLanguage = it }
                    )
                    AppDestination.COURSE_DETAIL -> CourseDetailScreen(
                        courseId = selectedCourseId ?: "c-py",
                        onBack = navigateBack,
                        onLessonClick = { lessonId ->
                            selectedLessonId = lessonId
                            pushDestination(AppDestination.LESSON_VIEWER)
                        },
                        onOpenRevisionList = {
                            pushDestination(AppDestination.REVISION_LIST)
                        }
                    )
                    AppDestination.LESSON_VIEWER -> LessonViewerScreen(
                        courseId = selectedCourseId ?: "c-py",
                        lessonId = selectedLessonId ?: "",
                        onBack = navigateBack,
                        onNavigateToLesson = { nextLessonId ->
                            selectedLessonId = nextLessonId
                        },
                        onOpenRevisionList = {
                            pushDestination(AppDestination.REVISION_LIST)
                        }
                    )
                    AppDestination.REVISION_LIST -> RevisionListScreen(
                        onBack = navigateBack,
                        onOpenLesson = { courseId, lessonId ->
                            selectedCourseId = courseId
                            selectedLessonId = lessonId
                            pushDestination(AppDestination.LESSON_VIEWER)
                        }
                    )
                }


            }
        }
    }
}