package com.aifinance.feature.budget.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aifinance.feature.budget.BudgetDashboardScreen
import com.aifinance.feature.budget.BudgetEntryScreen
import com.aifinance.feature.budget.BudgetWizardScreen

const val BUDGET_ENTRY_ROUTE = "budget_entry"
const val BUDGET_WIZARD_ROUTE = "budget_wizard"
const val BUDGET_DASHBOARD_ROUTE = "budget_dashboard"

fun NavController.navigateToBudgetEntry() {
    navigate(BUDGET_ENTRY_ROUTE)
}

fun NavController.navigateToBudgetWizard() {
    navigate(BUDGET_WIZARD_ROUTE)
}

fun NavController.navigateToBudgetDashboard() {
    navigate(BUDGET_DASHBOARD_ROUTE)
}

fun NavGraphBuilder.budgetNavigation(
    onPopBudgetStack: () -> Unit,
    onNavigateToWizard: () -> Unit,
    onWizardCompletedNavigateToDashboard: () -> Unit,
) {
    composable(BUDGET_ENTRY_ROUTE) {
        BudgetEntryScreen(
            onNavigateBack = onPopBudgetStack,
            onOpenBudgetWizard = onNavigateToWizard,
        )
    }

    composable(BUDGET_WIZARD_ROUTE) {
        BudgetWizardScreen(
            onBack = onPopBudgetStack,
            onCompleted = onWizardCompletedNavigateToDashboard,
        )
    }

    composable(BUDGET_DASHBOARD_ROUTE) {
        BudgetDashboardScreen(
            onNavigateBack = onNavigateToWizard,
            onAdjustBudget = onNavigateToWizard,
            onNoActivePlanNavigateToSetup = onNavigateToWizard,
        )
    }
}

