package com.aifinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aifinance.core.model.BudgetCategoryAllocation
import com.aifinance.core.model.BudgetWizardState
import com.aifinance.core.model.MonthlyBudgetPlan
import com.aifinance.core.model.BudgetSource
import com.aifinance.core.model.UserRole
import com.aifinance.core.model.CurrencyCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

private val ACTIVE_PLAN_JSON_KEY = stringPreferencesKey("budget_active_plan_json")

class BudgetRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : BudgetRepository {

    override fun getActivePlan(): Flow<MonthlyBudgetPlan?> {
        // 禁止在 dataStore.data 的 collect/transform 路径里同步调用 dataStore.edit：
        // 会触发 DataStore 重入/非法状态，表现为一进预算页立刻崩溃。
        return dataStore.data
            .catch {
                emit(emptyPreferences())
            }
            .map { prefs ->
                val json = prefs[ACTIVE_PLAN_JSON_KEY] ?: return@map null
                runCatching {
                    Json.decodeFromString<MonthlyBudgetPlanDto>(json).toDomain()
                }.getOrNull()
            }
    }

    override suspend fun upsertActivePlan(plan: MonthlyBudgetPlan) {
        val dto = plan.toDto()
        dataStore.edit { prefs ->
            prefs[ACTIVE_PLAN_JSON_KEY] = Json.encodeToString(dto)
        }
    }

    override suspend fun clearActivePlan() {
        dataStore.edit { prefs ->
            prefs.remove(ACTIVE_PLAN_JSON_KEY)
        }
    }
}

@Serializable
private data class MonthlyBudgetPlanDto(
    val monthId: String, // YYYY-MM
    val currency: CurrencyCode,
    val userRole: UserRole,

    val totalIncome: String,
    val fixedExpensesMonthly: String,
    val disposableFund: String,

    val budgetRatio: Float,
    val totalBudget: String,
    val savingsAllocation: String,

    val categories: List<BudgetCategoryAllocationDto>,
    val unallocatedPool: String,

    val spentToDate: String,
    val pendingHolds: String,

    val createdAt: String? = null,
    val updatedAt: String? = null,
    val source: BudgetSource = BudgetSource.AI_RECOMMENDATION,
    val version: Int = 1,
    val wizardState: BudgetWizardStateDto? = null,
)

@Serializable
private data class BudgetWizardStateDto(
    val stepIndex: Int,
    val completedSteps: List<Int>,
)

@Serializable
private data class BudgetCategoryAllocationDto(
    val categoryId: String,
    val name: String,
    val weight: Float,
    val amount: String,
    val spentToDate: String,
    val allowOverspend: Boolean,
    val editable: Boolean,
)

private fun MonthlyBudgetPlan.toDto(): MonthlyBudgetPlanDto {
    return MonthlyBudgetPlanDto(
        monthId = this.monthId.toString(),
        currency = this.currency,
        userRole = this.userRole,

        totalIncome = this.totalIncome.toPlainString(),
        fixedExpensesMonthly = this.fixedExpensesMonthly.toPlainString(),
        disposableFund = this.disposableFund.toPlainString(),

        budgetRatio = this.budgetRatio,
        totalBudget = this.totalBudget.toPlainString(),
        savingsAllocation = this.savingsAllocation.toPlainString(),

        categories = this.categories.map {
            BudgetCategoryAllocationDto(
                categoryId = it.categoryId.toString(),
                name = it.name,
                weight = it.weight,
                amount = it.amount.toPlainString(),
                spentToDate = it.spentToDate.toPlainString(),
                allowOverspend = it.allowOverspend,
                editable = it.editable,
            )
        },
        unallocatedPool = this.unallocatedPool.toPlainString(),

        spentToDate = this.spentToDate.toPlainString(),
        pendingHolds = this.pendingHolds.toPlainString(),

        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
        source = this.source,
        version = this.version,
        wizardState = this.wizardState?.let {
            BudgetWizardStateDto(
                stepIndex = it.stepIndex,
                completedSteps = it.completedSteps,
            )
        },
    )
}

private fun MonthlyBudgetPlanDto.toDomain(): MonthlyBudgetPlan {
    fun bd(v: String): BigDecimal = runCatching { BigDecimal(v) }.getOrElse { BigDecimal.ZERO }

    return MonthlyBudgetPlan(
        userId = "local_user",
        monthId = YearMonth.parse(this.monthId),
        currency = this.currency,

        userRole = this.userRole,

        totalIncome = bd(totalIncome),
        fixedExpensesMonthly = bd(fixedExpensesMonthly),
        disposableFund = bd(disposableFund),

        budgetRatio = budgetRatio,
        totalBudget = bd(totalBudget),
        savingsAllocation = bd(savingsAllocation),

        categories = categories.map {
            BudgetCategoryAllocation(
                categoryId = UUID.fromString(it.categoryId),
                name = it.name,
                weight = it.weight,
                amount = bd(it.amount),
                spentToDate = bd(it.spentToDate),
                allowOverspend = it.allowOverspend,
                editable = it.editable,
            )
        },
        unallocatedPool = bd(unallocatedPool),

        spentToDate = bd(spentToDate),
        pendingHolds = bd(pendingHolds),

        createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.now(),
        updatedAt = updatedAt?.let { Instant.parse(it) } ?: Instant.now(),
        source = this.source,
        version = this.version,

        wizardState = wizardState?.let { w ->
            BudgetWizardState(stepIndex = w.stepIndex, completedSteps = w.completedSteps)
        },
    )
}

