package com.aifinance.feature.home.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val EMAIL_AUTH_ROUTE = "email_auth"

fun NavController.navigateToEmailAuth() {
    navigate(EMAIL_AUTH_ROUTE)
}

fun NavGraphBuilder.emailAuthScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    composable(route = EMAIL_AUTH_ROUTE) {
        EmailAuthScreen(
            onLoginSuccess = onLoginSuccess,
            onBack = onBack
        )
    }
}
