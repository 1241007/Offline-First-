package com.offline_First

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.offline_First.ui.screens.LandingScreen
import com.offline_First.ui.screens.auth.LoginScreen
import com.offline_First.ui.screens.auth.RegisterScreen
import com.offline_First.ui.theme.OfflineFirstTheme

private enum class AppDestination {
    LANDING,
    LOGIN,
    REGISTER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineFirstTheme {
                var destination by rememberSaveable { mutableStateOf(AppDestination.LANDING) }
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = destination != AppDestination.LANDING) {
                    destination = when (destination) {
                        AppDestination.REGISTER -> AppDestination.LOGIN
                        AppDestination.LOGIN -> AppDestination.LANDING
                        AppDestination.LANDING -> AppDestination.LANDING
                    }
                }

                when (destination) {
                    AppDestination.LANDING -> LandingScreen(
                        onLogin = { destination = AppDestination.LOGIN },
                        isLoggedIn = isLoggedIn
                    )
                    AppDestination.LOGIN -> LoginScreen(
                        onRegister = { destination = AppDestination.REGISTER },
                        onLoginSuccess = {
                            isLoggedIn = true
                            destination = AppDestination.LANDING
                        }
                    )
                    AppDestination.REGISTER -> RegisterScreen(
                        onLogin = { destination = AppDestination.LOGIN }
                    )
                }
            }
        }
    }
}