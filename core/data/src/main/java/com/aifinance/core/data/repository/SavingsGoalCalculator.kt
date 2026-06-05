package com.aifinance.core.data.repository

import com.aifinance.core.model.SavingsFrequency
import com.aifinance.core.model.SavingsGoalStatus
import com.aifinance.core.model.SavingsMethod
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SavingsGoalCalculator {

    fun calculateProgress(saved: BigDecimal, target: BigDecimal): Float {
        if (target <= BigDecimal.ZERO) return 0f
        val progress = saved.divide(target, 4, RoundingMode.HALF_UP).toFloat()
        return progress.coerceIn(0f, 1f)
    }

    fun calculateDaysRemaining(deadline: LocalDate): Long {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), deadline)
        return maxOf(days, 0)
    }

    fun calculateDailySuggestion(
        saved: BigDecimal,
        target: BigDecimal,
        deadline: LocalDate
    ): BigDecimal {
        val remaining = target.subtract(saved).max(BigDecimal.ZERO)
        val daysLeft = calculateDaysRemaining(deadline)
        if (daysLeft <= 0) return remaining
        return remaining.divide(BigDecimal(daysLeft), 2, RoundingMode.CEILING)
    }

    fun calculateWeeklySuggestion(
        saved: BigDecimal,
        target: BigDecimal,
        deadline: LocalDate
    ): BigDecimal {
        val remaining = target.subtract(saved).max(BigDecimal.ZERO)
        val daysLeft = calculateDaysRemaining(deadline)
        if (daysLeft <= 0) return remaining
        val weeksLeft = maxOf(daysLeft / 7, 1)
        return remaining.divide(BigDecimal(weeksLeft), 2, RoundingMode.CEILING)
    }

    fun isOverdue(deadline: LocalDate, status: SavingsGoalStatus): Boolean {
        return status == SavingsGoalStatus.ACTIVE && LocalDate.now().isAfter(deadline)
    }

    fun calculateWeek52Amount(weekIndex: Int, baseAmount: BigDecimal): BigDecimal {
        return baseAmount.multiply(BigDecimal(weekIndex))
    }

    fun calculateDay365Amount(dayIndex: Int, baseAmount: BigDecimal): BigDecimal {
        return baseAmount.multiply(BigDecimal(dayIndex))
    }

    fun calculateMonth12Amount(monthIndex: Int, baseAmount: BigDecimal): BigDecimal {
        return baseAmount.multiply(BigDecimal(monthIndex))
    }

    fun calculateTotalTarget(method: SavingsMethod, baseAmount: BigDecimal?, fixedAmount: BigDecimal?, periods: Int?): BigDecimal {
        return when (method) {
            SavingsMethod.WEEKLY_52 -> {
                val base = baseAmount ?: BigDecimal(10)
                base.multiply(BigDecimal(1378))
            }
            SavingsMethod.DAILY_365 -> {
                val base = baseAmount ?: BigDecimal(1)
                base.multiply(BigDecimal(66795))
            }
            SavingsMethod.MONTHLY_12 -> {
                val base = baseAmount ?: BigDecimal(100)
                base.multiply(BigDecimal(78))
            }
            SavingsMethod.FIXED_AMOUNT -> {
                val amount = fixedAmount ?: BigDecimal.ZERO
                val count = periods ?: 0
                amount.multiply(BigDecimal(count))
            }
            SavingsMethod.FLEXIBLE -> BigDecimal.ZERO
        }
    }

    fun getCurrentPeriodIndex(startDate: LocalDate, method: SavingsMethod, frequency: SavingsFrequency?): Int {
        val now = LocalDate.now()
        if (now.isBefore(startDate)) return 1
        
        return when (method) {
            SavingsMethod.WEEKLY_52 -> {
                val weeks = ChronoUnit.WEEKS.between(startDate, now)
                (weeks + 1).toInt().coerceIn(1, 52)
            }
            SavingsMethod.DAILY_365 -> {
                val days = ChronoUnit.DAYS.between(startDate, now)
                (days + 1).toInt().coerceIn(1, 365)
            }
            SavingsMethod.MONTHLY_12 -> {
                val months = ChronoUnit.MONTHS.between(startDate, now)
                (months + 1).toInt().coerceIn(1, 12)
            }
            SavingsMethod.FIXED_AMOUNT -> {
                when (frequency) {
                    SavingsFrequency.DAILY -> {
                        val days = ChronoUnit.DAYS.between(startDate, now)
                        (days + 1).toInt()
                    }
                    SavingsFrequency.WEEKLY -> {
                        val weeks = ChronoUnit.WEEKS.between(startDate, now)
                        (weeks + 1).toInt()
                    }
                    SavingsFrequency.MONTHLY -> {
                        val months = ChronoUnit.MONTHS.between(startDate, now)
                        (months + 1).toInt()
                    }
                    null -> 1
                }
            }
            SavingsMethod.FLEXIBLE -> 1
        }
    }
}
