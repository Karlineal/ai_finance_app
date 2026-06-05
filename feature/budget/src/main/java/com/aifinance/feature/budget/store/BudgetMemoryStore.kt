package com.aifinance.feature.budget.store

import com.aifinance.core.model.MonthlyBudgetPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 向导完成后用于驱动 Dashboard 展示的「会话内」存储。
 *
 * 说明：
 * - 本轮不做数据库/持久化（避免引入 Room/DataStore 迁移冲突）。
 * - 后续你或他人可以用同接口替换为持久化实现。
 */
object BudgetMemoryStore {
    private val _activePlan = MutableStateFlow<MonthlyBudgetPlan?>(null)
    val activePlan: StateFlow<MonthlyBudgetPlan?> = _activePlan

    fun setActivePlan(plan: MonthlyBudgetPlan) {
        _activePlan.value = plan
    }

    fun clear() {
        _activePlan.value = null
    }
}

