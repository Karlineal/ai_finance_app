package com.aifinance.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.home.HomeContainerScreen

const val HOME_ROUTE = "home"

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(HOME_ROUTE, navOptions)
}

fun NavGraphBuilder.homeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToAssetManagement: () -> Unit = {},
) {
    composable(HOME_ROUTE) {
        HomeContainerScreen(
            onOpenDrawer = onNavigateToTransactions,
            onNavigateToAssetManagement = onNavigateToAssetManagement,
        )
    }
}
