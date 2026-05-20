package com.aifinance.core.data.repository

import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.time.LocalDate

object SavingsGoalCalculator {
    fun calculateProgress(current: BigDecimal, target: BigDecimal): Float = 0f
    fun isOverdue(endDate: LocalDate, status: SavingsGoalStatus): Boolean = false
    fun calculateDaysRemaining(endDate: LocalDate): Long = 0
    fun calculateDailySuggestion(current: BigDecimal, target: BigDecimal, endDate: LocalDate): BigDecimal = BigDecimal.ZERO
    fun calculateWeeklySuggestion(current: BigDecimal, target: BigDecimal, endDate: LocalDate): BigDecimal = BigDecimal.ZERO
}
