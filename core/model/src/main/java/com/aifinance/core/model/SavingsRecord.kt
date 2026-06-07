package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SavingsRecord(
    val id: UUID,
    val savingsGoalId: UUID,
    val amount: BigDecimal,
    val date: LocalDate,
    val note: String?,
    val periodIndex: Int,
    val createdAt: Instant,
)
