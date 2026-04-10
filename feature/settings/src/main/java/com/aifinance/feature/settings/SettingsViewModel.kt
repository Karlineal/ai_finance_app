package com.aifinance.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AppThemeMode
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val isClearingHistory: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.themeMode.collect { mode ->
                _uiState.update { state ->
                    state.copy(isDarkTheme = mode == AppThemeMode.DARK)
                }
            }
        }
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT)
        }
    }

    fun clearAllHistory() {
        if (_uiState.value.isClearingHistory) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingHistory = true) }
            try {
                transactionRepository.clearAllTransactionHistory()
            } finally {
                _uiState.update { it.copy(isClearingHistory = false) }
            }
        }
    }
}