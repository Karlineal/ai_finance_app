package com.aifinance.feature.savings_goal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aifinance.feature.savings_goal.SavingsGoalCreateEditScreen
import com.aifinance.feature.savings_goal.SavingsGoalDetailScreen
import com.aifinance.feature.savings_goal.SavingsGoalListScreen

const val SAVINGS_GOAL_LIST_ROUTE = "savings_goal_list"
const val SAVINGS_GOAL_CREATE_ROUTE = "savings_goal_create"
const val SAVINGS_GOAL_DETAIL_ROUTE = "savings_goal_detail/{goalId}"
const val SAVINGS_GOAL_EDIT_ROUTE = "savings_goal_edit/{goalId}"

fun NavController.navigateToSavingsGoalList() {
    navigate(SAVINGS_GOAL_LIST_ROUTE)
}

fun NavController.navigateToSavingsGoalCreate() {
    navigate(SAVINGS_GOAL_CREATE_ROUTE)
}

fun NavController.navigateToSavingsGoalDetail(goalId: String) {
    navigate("savings_goal_detail/$goalId")
}

fun NavController.navigateToSavingsGoalEdit(goalId: String) {
    navigate("savings_goal_edit/$goalId")
}

fun NavGraphBuilder.savingsGoalNavigation(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    composable(SAVINGS_GOAL_LIST_ROUTE) {
        SavingsGoalListScreen(
            onNavigateToCreate = onNavigateToCreate,
            onNavigateToDetail = onNavigateToDetail,
            onBack = onBack,
        )
    }

    composable(SAVINGS_GOAL_CREATE_ROUTE) {
        SavingsGoalCreateEditScreen(
            goalId = null,
            onSaved = onBack,
            onBack = onBack,
        )
    }

    composable(
        route = SAVINGS_GOAL_DETAIL_ROUTE,
        arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val goalId = backStackEntry.arguments?.getString("goalId").orEmpty()
        SavingsGoalDetailScreen(
            goalId = goalId,
            onNavigateToEdit = { onNavigateToEdit(goalId) },
            onBack = onBack,
        )
    }

    composable(
        route = SAVINGS_GOAL_EDIT_ROUTE,
        arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
    ) { backStackEntry ->
        SavingsGoalCreateEditScreen(
            goalId = backStackEntry.arguments?.getString("goalId"),
            onSaved = onBack,
            onBack = onBack,
        )
    }
}
