package com.aifinance.feature.budget

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.aifinance.core.model.BudgetCategoryAllocation
import com.aifinance.core.model.BudgetSource
import com.aifinance.core.model.allocateByWeights
import com.aifinance.core.model.CategoryWeightInput
import com.aifinance.core.model.FixedExpenseItem
import com.aifinance.core.model.FixedExpenseRecurrence
import com.aifinance.core.model.MonthlyBudgetPlan
import com.aifinance.core.model.UserRole
import com.aifinance.core.model.BudgetWizardState
import com.aifinance.core.model.BudgetCategoryAllocation as CoreBudgetCategoryAllocation
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.data.repository.BudgetRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class FixedExpenseDraft(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val amountText: String = "",
    val recurrence: FixedExpenseRecurrence = FixedExpenseRecurrence.MONTHLY,
) {
    fun toFixedExpenseItem(month: YearMonth): FixedExpenseItem {
        val amount = amountText.toMoneyOrZero()
        return FixedExpenseItem(
            id = id,
            name = name,
            amount = amount,
            recurrence = recurrence,
            prorateToMonthly = true,
            startMonth = month,
        )
    }
}

data class BudgetWizardUiState(
    val stepIndex: Int = 1,

    val role: UserRole? = null,

    val totalIncomeText: String = "",
    val totalIncome: BigDecimal = BigDecimal.ZERO,

    val fixedExpensesDrafts: List<FixedExpenseDraft> = emptyList(),
    val fixedExpensesMonthly: BigDecimal = BigDecimal.ZERO,

    val budgetRatio: Float = 0.8f,
    val disposableFund: BigDecimal = BigDecimal.ZERO,
    val totalBudget: BigDecimal = BigDecimal.ZERO,
    val savingsAllocation: BigDecimal = BigDecimal.ZERO,

    val categoryAllocations: List<CoreBudgetCategoryAllocation> = emptyList(),
    val unallocatedPool: BigDecimal = BigDecimal.ZERO,
    val categoryEdited: Boolean = false,

    val errorMessage: String? = null,
)

private fun String.toMoneyOrZero(): BigDecimal {
    val normalized = trim()
    if (normalized.isEmpty()) return BigDecimal.ZERO
    val parsed = normalized.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    // 限制非负；负值直接按 0 处理（向导校验会给出错误提示）
    return parsed.coerceAtLeast(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
}

@HiltViewModel
class BudgetWizardViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetWizardUiState())
    val uiState: StateFlow<BudgetWizardUiState> = _uiState

    private val currentMonth: YearMonth = YearMonth.now()
    private val currency: com.aifinance.core.model.CurrencyCode = "CNY"

    fun setRole(role: UserRole) {
        _uiState.update { s ->
            s.copy(
                role = role,
                categoryEdited = false,
                errorMessage = null,
            )
        }
        recomputeDerived()
    }

    fun setTotalIncomeText(text: String) {
        _uiState.update { s ->
            val parsed = text.toMoneyOrZero()
            s.copy(
                totalIncomeText = text,
                totalIncome = parsed,
                categoryEdited = false,
                errorMessage = null,
            )
        }
        recomputeDerived()
    }

    fun addFixedExpenseTemplate(name: String) {
        _uiState.update { s ->
            s.copy(
                fixedExpensesDrafts = s.fixedExpensesDrafts + FixedExpenseDraft(
                    name = name,
                    amountText = "",
                ),
                categoryEdited = false,
                errorMessage = null,
            )
        }
        recomputeDerived()
    }

    fun setFixedExpenseAmount(expenseId: UUID, amountText: String) {
        _uiState.update { s ->
            s.copy(
                fixedExpensesDrafts = s.fixedExpensesDrafts.map { draft ->
                    if (draft.id == expenseId) draft.copy(amountText = amountText) else draft
                },
                categoryEdited = false,
            )
        }
        recomputeDerived()
    }

    fun removeFixedExpense(expenseId: UUID) {
        _uiState.update { s ->
            s.copy(
                fixedExpensesDrafts = s.fixedExpensesDrafts.filterNot { it.id == expenseId },
                categoryEdited = false,
            )
        }
        recomputeDerived()
    }

    fun setBudgetRatio(newRatio: Float) {
        val clamped = newRatio.coerceIn(0f, 1f)
        _uiState.update { s ->
            s.copy(
                budgetRatio = clamped,
                categoryEdited = false,
                errorMessage = null,
            )
        }
        recomputeDerived()
    }

    fun goToNextStep() {
        _uiState.update { s ->
            val nextStep = (s.stepIndex + 1).coerceAtMost(5)
            s.copy(stepIndex = nextStep, errorMessage = null)
        }

        // 进入 Step 5 时，如果用户尚未手动编辑分类，则按推荐权重自动拆分
        _uiState.value.let { state ->
            if (state.stepIndex == 5 && !state.categoryEdited) {
                recomputeCategoryAllocationsFromRoleIfPossible(state)
            }
        }
    }

    fun goToPreviousStep() {
        _uiState.update { s ->
            s.copy(stepIndex = (s.stepIndex - 1).coerceAtLeast(1), errorMessage = null)
        }
    }

    fun resetCategoryToRecommendation() {
        _uiState.update { s ->
            val next = recomputeCategoryAllocationsFromRoleIfPossible(s)
            next.copy(categoryEdited = false, errorMessage = null)
        }
    }

    fun allocateAverage() {
        _uiState.update { s ->
            if (s.totalBudget <= BigDecimal.ZERO) {
                return@update s.copy(
                    categoryAllocations = emptyList(),
                    unallocatedPool = BigDecimal.ZERO,
                    errorMessage = null,
                    categoryEdited = false,
                )
            }
            val cats = s.categoryAllocations.ifEmpty { getDefaultVariableCategoryAllocations(s.totalBudget) }
            val n = cats.size.takeIf { it > 0 } ?: return@update s
            val totalCents = s.totalBudget
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
            val base = totalCents / n
            val rem = totalCents % n

            val updated = cats.mapIndexed { index, c ->
                val cents = base + if (index < rem.toInt()) 1L else 0L
                val amount = BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY)
                c.copy(amount = amount)
            }

            s.copy(
                categoryAllocations = updated,
                unallocatedPool = BigDecimal.ZERO,
                categoryEdited = true,
                errorMessage = null,
            )
        }
    }

    fun adjustCategoryAmount(categoryId: UUID, delta: BigDecimal) {
        _uiState.update { s ->
            if (s.totalBudget <= BigDecimal.ZERO) return@update s
            if (delta == BigDecimal.ZERO) return@update s
            val target = s.categoryAllocations.firstOrNull { it.categoryId == categoryId } ?: return@update s
            val newAmount = target.amount + delta
            if (newAmount < BigDecimal.ZERO) return@update s

            val newPool = s.totalBudget.subtract(
                s.categoryAllocations.fold(BigDecimal.ZERO) { acc, c ->
                    if (c.categoryId == categoryId) acc + newAmount else acc + c.amount
                },
            )

            // 默认策略：只允许从未分配池扣减（不允许超额）
            if (newPool < BigDecimal.ZERO) {
                return@update s.copy(errorMessage = "预算不足，不能超额分配。")
            }

            val updated = s.categoryAllocations.map { c ->
                if (c.categoryId == categoryId) c.copy(amount = newAmount) else c
            }

            s.copy(
                categoryAllocations = updated,
                unallocatedPool = newPool,
                categoryEdited = true,
                errorMessage = null,
            )
        }
    }

    fun confirmAndComplete(onDone: (Boolean) -> Unit) {
        val state = _uiState.value

        val role = state.role ?: run {
            onDone(false)
            return
        }

        val now = Instant.now()
        val plan = MonthlyBudgetPlan(
            userId = "local_user",
            monthId = currentMonth,
            currency = currency,
            userRole = role,
            totalIncome = state.totalIncome,
            fixedExpensesMonthly = state.fixedExpensesMonthly,
            disposableFund = state.disposableFund,

            budgetRatio = state.budgetRatio,
            totalBudget = state.totalBudget,
            savingsAllocation = state.savingsAllocation,

            categories = state.categoryAllocations,
            unallocatedPool = state.unallocatedPool,

            spentToDate = BigDecimal.ZERO,
            pendingHolds = BigDecimal.ZERO,

            createdAt = now,
            updatedAt = now,
            source = BudgetSource.AI_RECOMMENDATION,
            version = 1,

            wizardState = BudgetWizardState(
                stepIndex = state.stepIndex,
                completedSteps = listOf(1, 2, 3, 4).let { base ->
                    if (state.totalBudget > BigDecimal.ZERO) base + 5 else base
                },
            ),
        )

        viewModelScope.launch {
            budgetRepository.upsertActivePlan(plan)
            onDone(true)
        }
    }

    fun addCategoryFromCatalog(categoryId: UUID) {
        _uiState.update { s ->
            if (s.categoryAllocations.any { it.categoryId == categoryId }) return@update s

            val def = getDefaultVariableCategoryDefs().firstOrNull { it.categoryId == categoryId } ?: return@update s
            val newAllocation = def.toBudgetAllocation(amount = BigDecimal.ZERO)
            val newList = s.categoryAllocations + newAllocation
            val newUnallocated = s.totalBudget
                .subtract(newList.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount })
                .coerceAtLeast(BigDecimal.ZERO)

            s.copy(
                categoryAllocations = newList,
                unallocatedPool = newUnallocated,
                categoryEdited = true,
                errorMessage = null,
            )
        }
    }

    private fun recomputeDerived() {
        val state = _uiState.value
        val fixedMonthly = state.fixedExpensesDrafts
            .map { it.amountText.toMoneyOrZero() }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
            .coerceAtLeast(BigDecimal.ZERO)

        val disposable = state.totalIncome.subtract(fixedMonthly).coerceAtLeast(BigDecimal.ZERO)

        val rawBudget = disposable.multiply(BigDecimal.valueOf(state.budgetRatio.toDouble()))
        val totalBudget = if (disposable <= BigDecimal.ZERO) BigDecimal.ZERO else rawBudget.setScale(2, RoundingMode.HALF_UP)
        val savings = disposable.subtract(totalBudget).coerceAtLeast(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)

        _uiState.update { s ->
            val next = s.copy(
                fixedExpensesMonthly = fixedMonthly,
                disposableFund = disposable,
                totalBudget = totalBudget,
                savingsAllocation = savings,
            )
            // 只有当用户没有手动编辑时，才重算 Step 5 的推荐拆分
            if (next.stepIndex >= 5 && !next.categoryEdited) {
                recomputeCategoryAllocationsFromRoleIfPossible(next)
            } else {
                next.copy(errorMessage = null, unallocatedPool = next.totalBudget - next.categoryAllocations.sumOf { it.amount })
            }
        }
    }

    private fun recomputeCategoryAllocationsFromRoleIfPossible(state: BudgetWizardUiState): BudgetWizardUiState {
        val totalBudget = state.totalBudget
        if (totalBudget <= BigDecimal.ZERO) {
            return state.copy(categoryAllocations = emptyList(), unallocatedPool = BigDecimal.ZERO)
        }
        val role = state.role ?: return state

        val weights = roleWeightsToCategoryWeights(role)
        val allocationsMap = allocateByWeights(totalBudget, weights)

        val allocations = getDefaultVariableCategoryDefs().map { def ->
            val amount = allocationsMap[def.categoryId] ?: BigDecimal.ZERO
            def.toBudgetAllocation(amount = amount)
        }

        val sum = allocations.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
        return state.copy(
            categoryAllocations = allocations,
            unallocatedPool = totalBudget.subtract(sum).coerceAtLeast(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
            errorMessage = null,
        )
    }

    private fun getDefaultVariableCategoryDefs(): List<VariableCategoryDef> {
        return listOf(
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseFood, "餐饮"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseShopping, "购物"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseTransport, "交通"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseEntertainment, "娱乐"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseEducation, "教育"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseMedical, "医疗"),
            VariableCategoryDef(CategoryCatalog.Ids.ExpenseOther, "其他支出"),
        )
    }

    private data class VariableCategoryDef(
        val categoryId: UUID,
        val name: String,
    ) {
        fun toBudgetAllocation(amount: BigDecimal): CoreBudgetCategoryAllocation {
            // weight 仅用于展示/追踪，这里不影响已分配金额
            return CoreBudgetCategoryAllocation(
                categoryId = categoryId,
                name = name,
                weight = 0f,
                amount = amount,
                spentToDate = BigDecimal.ZERO,
                allowOverspend = false,
                editable = true,
            )
        }
    }

    private fun roleWeightsToCategoryWeights(role: UserRole): List<CategoryWeightInput> {
        // 权重使用「百分比」形式（总和 100），allocateByWeights 内部会做归一化
        val weightsPercent: Map<UUID, BigDecimal> = when (role) {
            UserRole.STUDENT -> mapOf(
                CategoryCatalog.Ids.ExpenseFood to BigDecimal(35),
                CategoryCatalog.Ids.ExpenseShopping to BigDecimal(23),
                CategoryCatalog.Ids.ExpenseTransport to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseEducation to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseEntertainment to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseMedical to BigDecimal(5),
                CategoryCatalog.Ids.ExpenseOther to BigDecimal(7),
            )
            UserRole.EMPLOYEE -> mapOf(
                CategoryCatalog.Ids.ExpenseFood to BigDecimal(25),
                CategoryCatalog.Ids.ExpenseShopping to BigDecimal(25),
                CategoryCatalog.Ids.ExpenseTransport to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseEducation to BigDecimal(7),
                CategoryCatalog.Ids.ExpenseEntertainment to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseMedical to BigDecimal(8),
                CategoryCatalog.Ids.ExpenseOther to BigDecimal(15),
            )
            UserRole.FREELANCER -> mapOf(
                CategoryCatalog.Ids.ExpenseFood to BigDecimal(22),
                CategoryCatalog.Ids.ExpenseShopping to BigDecimal(23),
                CategoryCatalog.Ids.ExpenseTransport to BigDecimal(8),
                CategoryCatalog.Ids.ExpenseEducation to BigDecimal(12),
                CategoryCatalog.Ids.ExpenseEntertainment to BigDecimal(8),
                CategoryCatalog.Ids.ExpenseMedical to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseOther to BigDecimal(17),
            )
            UserRole.OTHER -> mapOf(
                CategoryCatalog.Ids.ExpenseFood to BigDecimal(20),
                CategoryCatalog.Ids.ExpenseShopping to BigDecimal(20),
                CategoryCatalog.Ids.ExpenseTransport to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseEducation to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseEntertainment to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseMedical to BigDecimal(10),
                CategoryCatalog.Ids.ExpenseOther to BigDecimal(20),
            )
        }

        return getDefaultVariableCategoryDefs().map { def ->
            CategoryWeightInput(
                categoryId = def.categoryId,
                weight = weightsPercent[def.categoryId] ?: BigDecimal.ZERO,
            )
        }
    }

    private fun getDefaultVariableCategoryAllocations(totalBudget: BigDecimal): List<CoreBudgetCategoryAllocation> {
        val weights = roleWeightsToCategoryWeights(_uiState.value.role ?: UserRole.OTHER)
        val allocationsMap = allocateByWeights(totalBudget, weights)
        return getDefaultVariableCategoryDefs().map { def ->
            def.toBudgetAllocation(amount = allocationsMap[def.categoryId] ?: BigDecimal.ZERO)
        }
    }
}

