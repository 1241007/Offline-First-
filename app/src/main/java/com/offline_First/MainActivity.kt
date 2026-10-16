package com.offline_First

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.offline_First.data.AppContainer
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
import com.offline_First.ui.theme.OfflineFirstTheme
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            OfflineFirstTheme {
                var destination by rememberSaveable { mutableStateOf(AppDestination.LANDING) }
                val navigationStack = rememberSaveable { mutableStateListOf<AppDestination>() }
                var openWorkspaceTools by rememberSaveable { mutableStateOf(false) }
                var selectedLanguage by rememberSaveable { mutableStateOf("English") }
                val coroutineScope = rememberCoroutineScope()
                val authRepository = remember { AppContainer.authRepository }
                val isLoggedIn by authRepository.observeSession().collectAsState(initial = false)
                val profileRepository = remember { AppContainer.profileRepository }
                val profile by profileRepository.observeUserProfile().collectAsState(initial = null)
                val personalizedRoadmapViewModel: PersonalizedRoadmapViewModel = viewModel()

                LaunchedEffect(authRepository) {
                    authRepository.restoreSession().onFailure { error ->
                        Log.e(TAG, "Could not restore the Supabase session.", error)
                    }
                }

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

                BackHandler(enabled = destination != AppDestination.LANDING) {
                    navigateBack()
                }

                when (destination) {
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
                                authRepository.signOut().onFailure { error ->
                                    Log.e(TAG, "Could not revoke the Supabase session.", error)
                                }
                                pushDestination(AppDestination.LOGIN)
                            }
                        }
                    )
                    AppDestination.LOGIN -> LoginScreen(
                        onRegister = { pushDestination(AppDestination.REGISTER) },
                        onForgotPassword = { pushDestination(AppDestination.FORGOT_PASSWORD) },
                        onLoginSuccess = { pushDestination(AppDestination.LANDING) }
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
                }
            }
        }
    }
}