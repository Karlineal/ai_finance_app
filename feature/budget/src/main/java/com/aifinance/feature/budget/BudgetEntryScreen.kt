package com.aifinance.feature.budget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BudgetEntryScreen(
    /** 离开预算入口（向导 Step1「关闭」、仪表盘顶部返回） */
    onNavigateBack: () -> Unit,
    /** 打开独立向导路由以重新制定预算（完成后由向导导航到仪表盘） */
    onOpenBudgetWizard: () -> Unit,
) {
    val viewModel: BudgetEntryViewModel = hiltViewModel()
    val entry by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        !entry.isHydrated -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        entry.activePlan == null -> {
            BudgetWizardScreen(
                onBack = onNavigateBack,
                onCompleted = { },
            )
        }
        else -> {
            BudgetDashboardScreen(
                onNavigateBack = onNavigateBack,
                onAdjustBudget = onOpenBudgetWizard,
                onNoActivePlanNavigateToSetup = null,
            )
        }
    }
}

