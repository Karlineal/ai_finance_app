package com.aifinance.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.Category
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val accounts: StateFlow<List<Account>> =
        accountRepository.getActiveAccounts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val categories: List<Category> = listOf(
        Category(
            id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            name = "餐饮",
            icon = "🍜",
            color = 0xFF4F7BFF.toInt(),
        ),
        Category(
            id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            name = "购物",
            icon = "🛍️",
            color = 0xFF4F7BFF.toInt(),
        ),
        Category(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            name = "交通",
            icon = "🚗",
            color = 0xFF4F7BFF.toInt(),
        ),
        Category(
            id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
            name = "收入",
            icon = "📦",
            color = 0xFFE39A3A.toInt(),
        ),
        Category(
            id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
            name = "其他",
            icon = "📦",
            color = 0xFF9CA3AF.toInt(),
        ),
    )

    fun updateTransactionCategory(transaction: Transaction, categoryId: UUID) {
        val category = categories.firstOrNull { it.id == categoryId } ?: return
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    categoryId = category.id,
                    title = category.name,
                )
            )
        }
    }

    fun updateTransactionDetail(
        transaction: Transaction,
        amount: BigDecimal,
        accountId: UUID,
        date: LocalDate,
        type: TransactionType,
        includeInExpense: Boolean,
    ) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(
                    amount = amount,
                    accountId = accountId,
                    date = date,
                    type = type,
                    isPending = !includeInExpense,
                )
            )
        }
    }
}
