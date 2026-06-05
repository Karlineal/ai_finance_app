package com.aifinance.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * 预算相关的核心算法工具，保持为纯函数，方便在向导、首页、统计等模块复用。
 *
 * 说明：
 * - 金额按「分」维度进行精确分配，避免四舍五入导致总和不等于总预算。
 * - 对外仍以 BigDecimal 表示金额，通常约定 scale=2（两位小数）。
 */

/**
 * 权重输入数据，用于根据 TotalBudget 进行分类预算分配。
 */
data class CategoryWeightInput(
    val categoryId: UUID,
    val weight: BigDecimal,
)

/**
 * 按权重将总预算精确拆分到各分类，使用「最大余数补齐法」保证总和严格等于 totalBudget。
 *
 * @param totalBudget 总预算金额（约定为 scale=2，单位为货币）
 * @param weights 各分类权重，权重可以不预先归一化
 * @return categoryId -> 分配金额（BigDecimal, scale=2）
 */
fun allocateByWeights(
    totalBudget: BigDecimal,
    weights: List<CategoryWeightInput>,
): Map<UUID, BigDecimal> {
    if (totalBudget <= BigDecimal.ZERO || weights.isEmpty()) {
        return emptyMap()
    }

    // 统一转换为「分」，避免浮点误差
    val totalInCents = totalBudget
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    // 过滤掉权重 <= 0 的分类
    val positiveWeights = weights.filter { it.weight > BigDecimal.ZERO }
    if (positiveWeights.isEmpty()) {
        return emptyMap()
    }

    val totalWeight = positiveWeights.fold(BigDecimal.ZERO) { acc, item ->
        acc + item.weight
    }

    if (totalWeight <= BigDecimal.ZERO) {
        return emptyMap()
    }

    data class TempAlloc(
        val categoryId: UUID,
        val baseCents: Long,
        val fraction: BigDecimal,
    )

    val allocations = positiveWeights.map { item ->
        // rawCents = totalInCents * (weight / totalWeight)
        val ratio = item.weight.divide(totalWeight, 10, RoundingMode.HALF_UP)
        val rawCents = BigDecimal(totalInCents).multiply(ratio)

        val baseCents = rawCents.setScale(0, RoundingMode.DOWN).longValueExact()
        val fraction = rawCents.subtract(BigDecimal(baseCents))

        TempAlloc(
            categoryId = item.categoryId,
            baseCents = baseCents,
            fraction = fraction,
        )
    }

    var allocatedBase = allocations.sumOf { it.baseCents }
    var remaining = (totalInCents - allocatedBase).toInt().coerceAtLeast(0)

    // 按小数部分从大到小排序，依次补 1 分，直到和对齐
    val sortedByFraction = allocations.sortedByDescending { it.fraction }

    val finalCentsMap = mutableMapOf<UUID, Long>()

    sortedByFraction.forEach { alloc ->
        var cents = alloc.baseCents
        if (remaining > 0) {
            cents += 1
            remaining -= 1
            allocatedBase += 1
        }
        finalCentsMap[alloc.categoryId] = cents
    }

    // 理论上 allocatedBase == totalInCents，如有误差则做一次兜底校正
    if (allocatedBase != totalInCents && finalCentsMap.isNotEmpty()) {
        val diff = (totalInCents - allocatedBase).toInt()
        val firstKey = finalCentsMap.keys.first()
        finalCentsMap[firstKey] = finalCentsMap.getValue(firstKey) + diff
    }

    return finalCentsMap.mapValues { (_, cents) ->
        BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY)
    }
}

/**
 * 日预算计算结果。
 */
data class TodayBudgetResult(
    val todayBudget: BigDecimal,
    val remainingBudget: BigDecimal,
    val deficit: BigDecimal,
)

/**
 * 计算「今日可用预算」。
 *
 * @param totalBudget 本月可变支出总预算
 * @param spentToDate 本月累计支出
 * @param pendingHolds 待入账/冻结金额（可选，允许为 0）
 * @param remainingDaysInclusive 剩余天数（包含今天），必须 >= 1
 */
fun calcTodayBudget(
    totalBudget: BigDecimal,
    spentToDate: BigDecimal,
    pendingHolds: BigDecimal,
    remainingDaysInclusive: Int,
): TodayBudgetResult {
    if (remainingDaysInclusive <= 0) {
        return TodayBudgetResult(
            todayBudget = BigDecimal.ZERO,
            remainingBudget = BigDecimal.ZERO,
            deficit = BigDecimal.ZERO,
        )
    }

    val remainingBudget = totalBudget
        .subtract(spentToDate)
        .subtract(pendingHolds)

    return if (remainingBudget <= BigDecimal.ZERO) {
        TodayBudgetResult(
            todayBudget = BigDecimal.ZERO,
            remainingBudget = remainingBudget,
            deficit = remainingBudget.negate(),
        )
    } else {
        val days = BigDecimal(remainingDaysInclusive)
        val todayBudget = remainingBudget
            .divide(days, 2, RoundingMode.DOWN)

        TodayBudgetResult(
            todayBudget = todayBudget,
            remainingBudget = remainingBudget,
            deficit = BigDecimal.ZERO,
        )
    }
}

