package com.aifinance.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.BudgetRepository
import com.aifinance.core.model.MonthlyBudgetPlan
import com.aifinance.core.model.TodayBudgetResult
import com.aifinance.core.model.calcTodayBudget
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import com.aifinance.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.math.RoundingMode
import javax.inject.Inject

enum class BudgetUsageStatus { LOW, MEDIUM, HIGH }

data class CategoryUsageUi(
    val categoryId: java.util.UUID,
    val name: String,
    val budgetAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val usageRatio: BigDecimal,
    val status: BudgetUsageStatus,
)

data class BudgetDashboardUiState(
    /** false：尚未收到 DataStore/交易合并后的首帧，避免把 initialValue 误当成「无方案」 */
    val isHydrated: Boolean = false,
    val plan: MonthlyBudgetPlan? = null,
    val totalBudget: BigDecimal = BigDecimal.ZERO,
    val spentToDate: BigDecimal = BigDecimal.ZERO,
    val remainingBudget: BigDecimal = BigDecimal.ZERO,
    val todayBudget: TodayBudgetResult = TodayBudgetResult(
        todayBudget = BigDecimal.ZERO,
        remainingBudget = BigDecimal.ZERO,
        deficit = BigDecimal.ZERO,
    ),
    val categoriesUsage: List<CategoryUsageUi> = emptyList(),
    val nowDate: LocalDate = LocalDate.now(),
)

@HiltViewModel
class BudgetDashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val _nowDate = MutableStateFlow(LocalDate.now())
    private val nowDate: StateFlow<LocalDate> = _nowDate

    init {
        // 保证每天 0:00 刷新（按设备本地时区）
        viewModelScope.launch {
            while (true) {
                val now = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val nextMidnight = now
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val currentMillis = System.currentTimeMillis()
                val delayMs = (nextMidnight - currentMillis).coerceAtLeast(0L)
                delay(delayMs)
                _nowDate.value = LocalDate.now()
            }
        }
    }

    val uiState: StateFlow<BudgetDashboardUiState> =
        combine(
            budgetRepository.getActivePlan(),
            transactionRepository.getAllTransactions(),
            nowDate,
        ) { plan, transactions, now ->
            Triple(plan, transactions, now)
        }
            .mapLatest { (plan, transactions, now) ->
                // 交易筛选/分组/求和在大数据量时会明显拖慢主线程，这里强制切到后台线程计算
                withContext(Dispatchers.Default) {
                    computeUiState(plan, transactions, now).copy(isHydrated = true)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BudgetDashboardUiState(isHydrated = false),
            )

    private fun computeUiState(
        plan: MonthlyBudgetPlan?,
        transactions: List<com.aifinance.core.model.Transaction>,
        now: LocalDate,
    ): BudgetDashboardUiState {
        if (plan == null) return BudgetDashboardUiState(nowDate = now)

        val monthId = plan.monthId
        val daysInMonth = monthId.lengthOfMonth()
        val todayIndex = if (now.year == monthId.year && now.monthValue == monthId.monthValue) {
            now.dayOfMonth
        } else {
            // 非当前月份时，RemainingDaysInclusive 会在 calcTodayBudget 中把今日预算置 0
            daysInMonth + 1
        }

        val totalBudget = plan.totalBudget

        val monthExpense = transactions.filter {
            it.type == TransactionType.EXPENSE && !it.isPending &&
                it.date.year == monthId.year && it.date.monthValue == monthId.monthValue
        }

        val spentToDate = monthExpense
            .filter { it.date <= now }
            .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }
            .setScale(2, RoundingMode.HALF_UP)

        val remainingBudget = totalBudget.subtract(spentToDate)

        val pendingHolds = BigDecimal.ZERO
        val remainingDaysInclusive = daysInMonth - todayIndex + 1
        val todayBudget = calcTodayBudget(
            totalBudget = totalBudget,
            spentToDate = spentToDate,
            pendingHolds = pendingHolds,
            remainingDaysInclusive = remainingDaysInclusive,
        )

        val categorySpentById: Map<java.util.UUID, BigDecimal> =
            monthExpense
                .filter { it.date <= now && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) ->
                    list.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.setScale(2, RoundingMode.HALF_UP)
                }

        val categoriesUsage = plan.categories.map { c ->
            val spent = categorySpentById[c.categoryId] ?: BigDecimal.ZERO
            val ratio = if (c.amount.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else spent.divide(c.amount, 4, java.math.RoundingMode.HALF_UP)
            val status = when {
                ratio < BigDecimal("0.6") -> BudgetUsageStatus.LOW
                ratio <= BigDecimal("0.8") -> BudgetUsageStatus.MEDIUM
                else -> BudgetUsageStatus.HIGH
            }
            CategoryUsageUi(
                categoryId = c.categoryId,
                name = c.name,
                budgetAmount = c.amount,
                spentAmount = spent,
                usageRatio = ratio,
                status = status,
            )
        }

        return BudgetDashboardUiState(
            plan = plan.copy(
                spentToDate = spentToDate,
                pendingHolds = pendingHolds,
            ),
            totalBudget = totalBudget,
            spentToDate = spentToDate,
            remainingBudget = remainingBudget,
            todayBudget = todayBudget,
            categoriesUsage = categoriesUsage,
            nowDate = now,
        )
    }
}

