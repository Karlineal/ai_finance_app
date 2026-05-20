package com.aifinance.feature.savings_goal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

fun NavController.navigateToSavingsGoalList() {}
fun NavController.navigateToSavingsGoalDetail(id: String) {}
fun NavController.navigateToSavingsGoalCreate() {}
fun NavController.navigateToSavingsGoalEdit(id: String) {}

fun NavGraphBuilder.savingsGoalNavigation(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onBack: () -> Unit
) {}
