package com.aifinance.feature.home.profile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val USER_PROFILE_ROUTE = "user_profile"

fun NavController.navigateToUserProfile() {
    navigate(USER_PROFILE_ROUTE)
}

fun NavGraphBuilder.userProfileScreen(
    onBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    composable(route = USER_PROFILE_ROUTE) {
        UserProfileScreen(
            onBack = onBack,
            onLogoutSuccess = onLogoutSuccess
        )
    }
}
