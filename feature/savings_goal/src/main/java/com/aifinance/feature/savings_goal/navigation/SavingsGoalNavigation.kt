package com.aifinance.feature.savings_goal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

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
        // TODO: Member #4 to implement SavingsGoalListScreen
    }

    composable(SAVINGS_GOAL_CREATE_ROUTE) {
        // TODO: Member #4 to implement SavingsGoalCreateEditScreen (Create Mode)
    }

    composable(SAVINGS_GOAL_DETAIL_ROUTE) {
        // TODO: Member #4 to implement SavingsGoalDetailScreen
    }

    composable(SAVINGS_GOAL_EDIT_ROUTE) {
        // TODO: Member #4 to implement SavingsGoalCreateEditScreen (Edit Mode)
    }
}
