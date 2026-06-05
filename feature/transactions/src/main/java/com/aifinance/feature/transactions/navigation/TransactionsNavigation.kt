package com.aifinance.feature.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aifinance.feature.transactions.CalendarTransactionsScreen
import com.aifinance.feature.transactions.TransactionsScreen
import java.time.LocalDate
import java.util.UUID

const val TRANSACTIONS_ROUTE = "transactions"
const val TRANSACTION_DETAIL_ROUTE = "transaction_detail/{transactionId}"
const val ALL_RECORDS_ROUTE = "all_records/{dateString}"

fun transactionDetailRoute(transactionId: UUID): String = "transaction_detail/$transactionId"
fun allRecordsRoute(date: LocalDate): String = "all_records/${date}"

fun NavController.navigateToTransactions(navOptions: NavOptions? = null) {
    navigate(TRANSACTIONS_ROUTE, navOptions)
}

fun NavController.navigateToTransactionDetail(transactionId: UUID, navOptions: NavOptions? = null) {
    navigate(transactionDetailRoute(transactionId), navOptions)
}

fun NavController.navigateToAllRecords(date: LocalDate, navOptions: NavOptions? = null) {
    navigate(allRecordsRoute(date), navOptions)
}

fun NavGraphBuilder.transactionsScreen(
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
) {
    composable(TRANSACTIONS_ROUTE) {
        TransactionsScreen(
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
        )
    }
}

fun NavGraphBuilder.calendarTransactionsScreen(
    onBack: () -> Unit,
    onNavigateToTransactionDetail: (UUID) -> Unit = {},
) {
    composable(
        route = ALL_RECORDS_ROUTE,
        arguments = listOf(navArgument("dateString") { type = NavType.StringType }),
    ) { backStackEntry ->
        val dateString = backStackEntry.arguments?.getString("dateString")
        val initialDate = try {
            dateString?.let { LocalDate.parse(it) } ?: LocalDate.now()
        } catch (_: Exception) {
            LocalDate.now()
        }
        CalendarTransactionsScreen(
            initialDate = initialDate,
            onBack = onBack,
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
        )
    }
}

fun NavGraphBuilder.transactionDetailScreen(
    onBack: () -> Unit,
) {
    composable(
        route = TRANSACTION_DETAIL_ROUTE,
        arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val transactionIdStr = backStackEntry.arguments?.getString("transactionId")

        // 💡 修复点 1：将 String 安全转换为 UUID 类型
        val transactionId = try {
            transactionIdStr?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        } catch (_: Exception) {
            UUID.randomUUID()
        }

        // 💡 修复点 2：严格对应 TransactionDetailRoute 的形参名称
        com.aifinance.feature.transactions.TransactionDetailRoute(
            transactionIdArg = transactionIdStr, 
            onBack = onBack,
            onSaved = onBack // Default behavior to go back on save
        )
    }
}
