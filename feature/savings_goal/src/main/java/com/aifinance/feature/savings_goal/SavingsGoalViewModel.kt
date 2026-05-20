package com.aifinance.feature.savings_goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.SavingsGoalRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.createGoal(goal)
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.deleteGoal(goal)
        }
    }

    fun adjustSavedAmount(id: UUID, delta: BigDecimal) {
        viewModelScope.launch {
            savingsGoalRepository.adjustSavedAmount(id, delta)
        }
    }

    fun updateStatus(id: UUID, status: SavingsGoalStatus) {
        viewModelScope.launch {
            savingsGoalRepository.updateStatus(id, status)
        }
    }
}
