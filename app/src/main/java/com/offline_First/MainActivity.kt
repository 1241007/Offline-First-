package com.offline_First

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.offline_First.data.local.LocalProfileRepository
import com.offline_First.domain.model.EducationMode
import com.offline_First.domain.model.UserProfile
import com.offline_First.ui.navigation.AppDestination
import com.offline_First.ui.screens.AIWorkspaceScreen
import com.offline_First.ui.screens.LandingScreen
import com.offline_First.ui.screens.MyLearningScreen
import com.offline_First.ui.screens.ProfileScreen
import com.offline_First.ui.screens.RoadmapBuilderScreen
import com.offline_First.ui.screens.RoadmapScreen
import com.offline_First.ui.screens.SettingsScreen
import com.offline_First.ui.screens.auth.ForgotPasswordScreen
import com.offline_First.ui.screens.auth.LoginScreen
import com.offline_First.ui.screens.auth.RegisterScreen
import com.offline_First.ui.theme.OfflineFirstTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineFirstTheme {
                var destination by rememberSaveable { mutableStateOf(AppDestination.LANDING) }
                val navigationStack = rememberSaveable { mutableStateListOf<AppDestination>() }
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var openWorkspaceTools by rememberSaveable { mutableStateOf(false) }
                var selectedLanguage by rememberSaveable { mutableStateOf("English") }
                val profileRepository = remember { LocalProfileRepository() }
                val profile by profileRepository.observeUserProfile().collectAsState(
                    initial = UserProfile(
                        fullName = "Asha Learner",
                        email = "asha@example.com",
                        mobile = "+91 98765 43210",
                        interests = "Android, UI design",
                        level = "Intermediate",
                        educationMode = EducationMode.SCHOOL
                    )
                )

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
                            isLoggedIn = false
                            pushDestination(AppDestination.LOGIN)
                        }
                    )
                    AppDestination.LOGIN -> LoginScreen(
                        onRegister = { pushDestination(AppDestination.REGISTER) },
                        onForgotPassword = { pushDestination(AppDestination.FORGOT_PASSWORD) },
                        onLoginSuccess = {
                            isLoggedIn = true
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
                        isClass912Student = (profile.educationMode == EducationMode.SCHOOL),
                        onBack = { navigateBack() },
                        openToolsOnStart = openWorkspaceTools
                    )
                    AppDestination.ROADMAP -> RoadmapScreen(
                        onBack = navigateBack,
                        onBuildRoadmap = { pushDestination(AppDestination.ROADMAP_BUILDER) }
                    )
                    AppDestination.ROADMAP_BUILDER -> RoadmapBuilderScreen(
                        onBack = { navigateBack() }
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