package com.aifinance.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.BudgetRepository
import com.aifinance.core.model.MonthlyBudgetPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BudgetEntryUiState(
    val isHydrated: Boolean = false,
    val activePlan: MonthlyBudgetPlan? = null,
)

@HiltViewModel
class BudgetEntryViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    val uiState: StateFlow<BudgetEntryUiState> =
        budgetRepository.getActivePlan()
            .map { plan -> BudgetEntryUiState(isHydrated = true, activePlan = plan) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BudgetEntryUiState(isHydrated = false, activePlan = null),
            )
}
