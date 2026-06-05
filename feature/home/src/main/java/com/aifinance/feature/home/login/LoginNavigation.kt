package com.aifinance.feature.home.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val LOGIN_ROUTE = "login"
const val ENTER_CODE_ROUTE = "enter_code"

fun NavController.navigateToLogin() {
    navigate(LOGIN_ROUTE)
}

fun NavController.navigateToEnterCode(phoneNumber: String) {
    navigate("$ENTER_CODE_ROUTE/$phoneNumber")
}

fun NavGraphBuilder.loginScreen(
    onNavigateToEnterCode: (String) -> Unit,
    onNavigateToEmailAuth: () -> Unit,
    onBack: () -> Unit
) {
    composable(route = LOGIN_ROUTE) {
        LoginScreen(
            onNavigateToEnterCode = onNavigateToEnterCode,
            onNavigateToEmailAuth = onNavigateToEmailAuth,
            onBack = onBack
        )
    }
}

fun NavGraphBuilder.enterCodeScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    composable(
        route = "$ENTER_CODE_ROUTE/{phoneNumber}",
        arguments = listOf(
            navArgument("phoneNumber") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
        EnterCodeScreen(
            phoneNumber = phoneNumber,
            onLoginSuccess = onLoginSuccess,
            onBack = onBack
        )
    }
}
