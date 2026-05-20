package com.aifinance.feature.savings_goal

import androidx.lifecycle.ViewModel
import com.aifinance.core.model.Account
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor() : ViewModel() {
    val goals: StateFlow<List<SavingsGoal>> = MutableStateFlow(emptyList())
    val accounts: StateFlow<List<Account>> = MutableStateFlow(emptyList())

    fun createGoal(goal: SavingsGoal) {}
    fun updateGoal(goal: SavingsGoal) {}
    fun deleteGoal(goal: SavingsGoal) {}
    fun adjustSavedAmount(id: UUID, amount: BigDecimal) {}
    fun updateStatus(id: UUID, status: SavingsGoalStatus) {}
    fun getGoalById(id: String): SavingsGoal? = null
}
