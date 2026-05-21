package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 攒钱计划状态枚举
 */
enum class SavingsGoalStatus {
    ACTIVE,    // 进行中
    COMPLETED, // 已达成
    FAILED     // 未达成（过期或放弃）
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
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SavingsTransactionPayload(
    val savingsGoalId: UUID,         // 💡 建议改为 UUID 类型，与项目整体 ID 体系保持一致
    val notes: String? = null        // 额外备注
)