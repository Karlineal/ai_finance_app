package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

/**
 * 月度预算方案快照，用于配合向导与主页展示。
 *
 * 说明：
 * - 金额字段统一使用 BigDecimal，语义上等同于「货币最小单位 * scale」，具体格式化交给 UI。
 * - 不改动现有 BudgetPlan，避免与原有按周期预算模型发生冲突。
 */
data class MonthlyBudgetPlan(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val monthId: YearMonth,
    val currency: CurrencyCode,

    val userRole: UserRole,

    val totalIncome: BigDecimal,
    val fixedExpensesMonthly: BigDecimal,
    val disposableFund: BigDecimal,

    /** 预算比例 BudgetRatio ∈ [0,1]，表示可支配资金中分配给「可变支出预算」的比例。 */
    val budgetRatio: Float,
    /** 可变支出总预算（TotalBudget）。 */
    val totalBudget: BigDecimal,
    /** 储蓄或结余金额（SavingsAllocation）。 */
    val savingsAllocation: BigDecimal,

    /** 分类预算拆解，仅包含「可变支出」类别。 */
    val categories: List<BudgetCategoryAllocation>,

    /** 未分配池，用于前端微调时临时缓冲。 */
    val unallocatedPool: BigDecimal,

    /** 执行期指标：本月累计支出、待入账等。 */
    val spentToDate: BigDecimal,
    val pendingHolds: BigDecimal,

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val source: BudgetSource = BudgetSource.AI_RECOMMENDATION,
    val version: Int = 1,

    /** 引导向导状态快照，便于从中断处恢复或版本对比。 */
    val wizardState: BudgetWizardState? = null,
)

/**
 * 单个分类的预算分配，面向「可变支出」类目。
 */
data class BudgetCategoryAllocation(
    val categoryId: UUID,
    val name: String,
    /** 推荐权重（0~1），用于根据 TotalBudget 自动计算初始金额。 */
    val weight: Float,
    /** 本方案中该分类的预算金额。 */
    val amount: BigDecimal,
    /** 已用金额，便于主页展示使用率。 */
    val spentToDate: BigDecimal = BigDecimal.ZERO,
    /** 是否允许超支。 */
    val allowOverspend: Boolean = true,
    /** 是否允许在向导或设置页中编辑该分类预算。 */
    val editable: Boolean = true,
)

/**
 * 固定支出条目定义，对应「房租/房贷、通讯、车贷」等。
 */
data class FixedExpenseItem(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val amount: BigDecimal,
    val recurrence: FixedExpenseRecurrence = FixedExpenseRecurrence.MONTHLY,
    /** 是否按月摊销（适用于年费/季费）。 */
    val prorateToMonthly: Boolean = true,
    /** 起始日期，可用于只在某些月份生效或做精细摊销。 */
    val startMonth: YearMonth? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class FixedExpenseRecurrence {
    MONTHLY,
    QUARTERLY,
    YEARLY,
}

/**
 * 向导状态，便于记录完成到第几步、哪些步骤已确认等。
 */
data class BudgetWizardState(
    val stepIndex: Int,
    val completedSteps: List<Int> = emptyList(),
)

/**
 * 预算来源信息，便于后续做版本对比和算法优化。
 */
enum class BudgetSource {
    AI_RECOMMENDATION,
    USER_ADJUSTED,
}

/**
 * 用户角色，用于选择默认权重组合。
 */
enum class UserRole {
    STUDENT,
    EMPLOYEE,
    FREELANCER,
    OTHER,
}

