package com.aifinance.feature.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.aifinance.feature.transactions.TransactionsScreen

const val TRANSACTIONS_ROUTE = "transactions"

fun NavController.navigateToTransactions(navOptions: NavOptions? = null) {
    navigate(TRANSACTIONS_ROUTE, navOptions)
}

fun NavGraphBuilder.transactionsScreen() {
    composable(TRANSACTIONS_ROUTE) {
        TransactionsScreen()
    }
}
