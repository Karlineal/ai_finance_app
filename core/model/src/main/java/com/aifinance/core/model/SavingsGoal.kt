package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 攒钱计划状态枚举
 */
enum class SavingsGoalStatus {
    ACTIVE, // 进行中
    COMPLETED, // 已达成
    FAILED, // 未达成（过期或放弃）
}

enum class SavingsMethod {
    WEEKLY_52, // 52周存钱法
    DAILY_365, // 365天存钱法
    MONTHLY_12, // 12月月存
    FIXED_AMOUNT, // 定额存钱
    FLEXIBLE, // 灵活存钱
}

enum class SavingsFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
}

/**
 * 攒钱计划领域模型
 */
data class SavingsGoal(
    val id: UUID,
    val accountId: UUID,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: SavingsGoalStatus,
    val notes: String?,
    val savingsMethod: SavingsMethod,
    val fixedAmount: BigDecimal?,
    val frequency: SavingsFrequency?,
    val baseAmount: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class SavingsTransactionPayload(
    val savingsGoalId: UUID, // 💡 建议改为 UUID 类型，与项目整体 ID 体系保持一致
    val notes: String? = null, // 额外备注
)
