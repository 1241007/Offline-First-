package com.offline_First

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun landingLoginOpensLoginAndRegisterLinkOpensRegister() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()

        composeRule.onNodeWithText("Create an account").performClick()
        composeRule.onNodeWithText("Create your account").assertIsDisplayed()
    }

    @Test
    fun forgotPasswordValidatesContactAndReturnsToLogin() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Forgot password?").performClick()
        composeRule.onNodeWithText("Forgot password?").assertIsDisplayed()

        composeRule.onNodeWithText("Send reset link").performClick()
        composeRule.onNodeWithText("Please enter your email or mobile number.").assertIsDisplayed()

        composeRule.onNodeWithText("Back to Login").performClick()
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    @Test
    fun landingExposesOnlyLoginForAuthentication() {
        composeRule.onNodeWithText("Login").assertIsDisplayed()
        composeRule.onNodeWithText("Register").assertDoesNotExist()
    }

    @Test
    fun landingSupportHubShowsInteractiveOptions() {
        composeRule.onNodeWithText("Need a hand? We're here to help.").assertIsDisplayed()
        composeRule.onNodeWithText("Find answers to common learning questions.").assertIsDisplayed()
        composeRule.onNodeWithText("Need help? Get in touch with our support team.").assertIsDisplayed()
        composeRule.onNodeWithText("Learn more about EduNova and its learning experience.").assertIsDisplayed()

        composeRule.onNodeWithText("Help Center").performClick()
        composeRule.onNodeWithText("Help resources are being prepared.").assertIsDisplayed()
    }

    @Test
    fun backFromRegisterReturnsToLogin() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Create an account").performClick()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    @Test
    fun loginShowsRequiredValidationAndPasswordCanBeShown() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Show").performClick()
        composeRule.onNodeWithText("Hide").assertIsDisplayed()
        composeRule.onNodeWithText("Login", useUnmergedTree = true).performClick()

        composeRule.onNodeWithText("Please enter your email or mobile number.").assertIsDisplayed()
        composeRule.onNodeWithText("Password cannot be empty.").assertIsDisplayed()
    }

    @Test
    fun registerRequiresTermsAndMatchingFields() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Create an account").performClick()
        composeRule.onNodeWithText("Create Account").performClick()

        composeRule.onNodeWithText("Please accept the Terms & Conditions.").assertIsDisplayed()
        composeRule.onNodeWithText("Passwords do not match.").assertDoesNotExist()
    }

    @Test
    fun loginFormSurvivesActivityRecreation() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onAllNodesWithText("Email or mobile number")
            .filter(hasSetTextAction())
            .performTextInput("learner@example.com")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("learner@example.com").assertIsDisplayed()
    }

    @Test
    fun registerFormSurvivesActivityRecreation() {
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Create an account").performClick()
        composeRule.onNodeWithText("Full name").performTextInput("Asha Learner")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Asha Learner").assertIsDisplayed()
    }
}
