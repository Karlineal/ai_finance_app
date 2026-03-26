package com.aifinance.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import com.aifinance.feature.add_transaction.navigation.ADD_TRANSACTION_ROUTE
import com.aifinance.feature.add_transaction.navigation.addTransactionScreen
import com.aifinance.feature.home.ASSET_MANAGEMENT_ROUTE
import com.aifinance.feature.home.ADD_ASSET_ACCOUNT_ROUTE
import com.aifinance.feature.home.ADD_ASSET_DETAIL_ROUTE
import com.aifinance.feature.home.AddAssetAccountScreen
import com.aifinance.feature.home.AddAssetDetailScreen
import com.aifinance.feature.home.AssetManagementScreen
import com.aifinance.feature.home.addAssetDetailRoute
import com.aifinance.feature.home.navigation.HOME_ROUTE
import com.aifinance.feature.home.navigation.homeScreen
import com.aifinance.feature.transactions.navigation.TRANSACTIONS_ROUTE
import com.aifinance.feature.transactions.navigation.transactionsScreen

@Composable
fun AiFinanceNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        homeScreen(
            onNavigateToTransactions = {
                navController.navigate(TRANSACTIONS_ROUTE)
            },
            onNavigateToAddTransaction = {
                navController.navigate(ADD_TRANSACTION_ROUTE)
            },
            onNavigateToAssetManagement = {
                navController.navigate(ASSET_MANAGEMENT_ROUTE)
            }
        )

        composable(ASSET_MANAGEMENT_ROUTE) {
            AssetManagementScreen(
                onBack = { navController.popBackStack() },
                onAddAccount = { navController.navigate(ADD_ASSET_ACCOUNT_ROUTE) }
            )
        }

        composable(ADD_ASSET_ACCOUNT_ROUTE) {
            AddAssetAccountScreen(
                onBack = { navController.popBackStack() },
                onPresetClick = { presetKey ->
                    navController.navigate(addAssetDetailRoute(presetKey))
                }
            )
        }

        composable(
            route = ADD_ASSET_DETAIL_ROUTE,
            arguments = listOf(navArgument("presetKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val presetKey = backStackEntry.arguments?.getString("presetKey") ?: "custom_asset"
            AddAssetDetailScreen(
                presetKey = presetKey,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(ASSET_MANAGEMENT_ROUTE, inclusive = false)
                }
            )
        }

        transactionsScreen()
        addTransactionScreen(
            onBack = { navController.popBackStack() },
            onSuccess = { navController.popBackStack() }
        )
    }
}
