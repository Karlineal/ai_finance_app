package com.aifinance.core.model

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class SavingsGoalStatus {
    ACTIVE, COMPLETED, FAILED
}

data class SavingsGoal(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val targetAmount: BigDecimal = BigDecimal.ZERO,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusMonths(1),
    val status: SavingsGoalStatus = SavingsGoalStatus.ACTIVE,
    val notes: String? = null,
    val accountId: UUID? = null,
    val createdAt: java.time.Instant = java.time.Instant.now(),
    val updatedAt: java.time.Instant = java.time.Instant.now()
)
