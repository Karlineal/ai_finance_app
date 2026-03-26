package com.aifinance.feature.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Category
import com.aifinance.core.model.CurrencyCode
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: String = "",
    val title: String = "",
    val categoryId: UUID? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    val categories = listOf(
        Category(
            id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            name = "餐饮",
            icon = "🍔",
            color = 0xFFEF4444.toInt()
        ),
        Category(
            id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            name = "交通",
            icon = "🚗",
            color = 0xFF3B82F6.toInt()
        ),
        Category(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            name = "购物",
            icon = "🛒",
            color = 0xFF10B981.toInt()
        ),
        Category(
            id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
            name = "工资",
            icon = "💰",
            color = 0xFFF59E0B.toInt()
        ),
        Category(
            id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
            name = "住房",
            icon = "🏠",
            color = 0xFF8B5CF6.toInt()
        ),
        Category(
            id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
            name = "通讯",
            icon = "📱",
            color = 0xFF14B8A6.toInt()
        ),
        Category(
            id = UUID.fromString("77777777-7777-7777-7777-777777777777"),
            name = "其他",
            icon = "📦",
            color = 0xFF6B7280.toInt()
        )
    )

    init {
        _uiState.value = AddTransactionUiState(
            categoryId = categories.first().id
        )
    }

    fun onAmountChanged(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val finalAmount = if (decimalCount > 1) {
            val firstDecimal = filtered.indexOf('.')
            filtered.filterIndexed { index, c ->
                c != '.' || index == firstDecimal
            }
        } else {
            filtered
        }
        _uiState.value = _uiState.value.copy(
            amount = finalAmount,
            amountError = null
        )
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onCategorySelected(categoryId: UUID) {
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId,
            categoryError = null
        )
    }

    fun onTypeChanged(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        var hasError = false
        var newState = currentState

        val amountValue = try {
            BigDecimal(currentState.amount.ifEmpty { "0" })
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }

        if (amountValue <= BigDecimal.ZERO) {
            newState = newState.copy(amountError = "金额必须大于0")
            hasError = true
        }

        if (currentState.categoryId == null) {
            newState = newState.copy(categoryError = "请选择分类")
            hasError = true
        }

        if (hasError) {
            _uiState.value = newState
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val defaultAccountId = accountRepository.getDefaultIncomeExpenseAccount()?.id
                    ?: accountRepository.getFirstActiveAccount()?.id
                    ?: UUID.randomUUID()

                val selectedCategory = categories.find { it.id == currentState.categoryId }
                val autoTitle = currentState.title.takeIf { it.isNotBlank() }
                    ?: selectedCategory?.name
                    ?: "未分类"

                val transaction = Transaction(
                    accountId = defaultAccountId,
                    categoryId = currentState.categoryId,
                    type = currentState.type,
                    amount = amountValue,
                    currency = "CNY",
                    title = autoTitle,
                    description = currentState.note.takeIf { it.isNotBlank() },
                    date = currentState.date,
                    sourceType = TransactionSourceType.MANUAL,
                    userConfirmed = true
                )

                transactionRepository.insertTransaction(transaction)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    amountError = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AddTransactionUiState(
            categoryId = categories.first().id
        )
    }
}
