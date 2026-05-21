package com.aifinance.core.data.repository

import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 攒钱计划进度与存款建议计算工具。
 */
object SavingsGoalCalculator {

    fun calculateProgress(saved: BigDecimal, target: BigDecimal): Float {
        if (target.signum() == 0) return 0f
        val ratio = saved
            .divide(target, 4, RoundingMode.HALF_UP)
            .toDouble()
            .coerceIn(0.0, 1.0)
        return ratio.toFloat()
    }

    fun calculateDaysRemaining(deadline: LocalDate): Long {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), deadline)
        return days.coerceAtLeast(0)
    }

    fun calculateDailySuggestion(
        saved: BigDecimal,
        target: BigDecimal,
        deadline: LocalDate,
    ): BigDecimal {
        val remaining = remainingAmount(saved, target)
        if (remaining.signum() == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

        if (LocalDate.now().isAfter(deadline)) {
            return remaining
        }

        val days = calculateDaysRemaining(deadline)
        if (days == 0L) {
            return remaining
        }

        return remaining
            .divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
    }

    fun calculateWeeklySuggestion(
        saved: BigDecimal,
        target: BigDecimal,
        deadline: LocalDate,
    ): BigDecimal {
        val remaining = remainingAmount(saved, target)
        if (remaining.signum() == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

        if (LocalDate.now().isAfter(deadline)) {
            return remaining
        }

        val days = calculateDaysRemaining(deadline)
        if (days == 0L) {
            return remaining
        }

        val weeks = (days + 6) / 7
        return remaining
            .divide(BigDecimal(weeks), 2, RoundingMode.HALF_UP)
    }

    fun isOverdue(deadline: LocalDate, status: SavingsGoalStatus): Boolean {
        return status == SavingsGoalStatus.ACTIVE && LocalDate.now().isAfter(deadline)
    }

    private fun remainingAmount(saved: BigDecimal, target: BigDecimal): BigDecimal {
        return target.subtract(saved).coerceAtLeast(BigDecimal.ZERO)
    }
}
