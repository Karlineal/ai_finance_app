package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 打卡记录领域模型
 */
data class CheckIn(
    val id: UUID,
    val savingsGoalId: UUID,
    val date: LocalDate,
    val amount: BigDecimal,
    val note: String?,
    val createdAt: Instant,
)
