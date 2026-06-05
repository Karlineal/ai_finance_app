package com.aifinance.feature.home.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EmailAuthViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow<EmailAuthState>(EmailAuthState.Idle)
    val uiState: StateFlow<EmailAuthState> = _uiState.asStateFlow()

    fun authenticate(email: String, password: String, isLogin: Boolean) {
        viewModelScope.launch {
            _uiState.value = EmailAuthState.Loading
            try {
                if (isLogin) {
                    auth.signInWithEmailAndPassword(email, password).await()
                } else {
                    auth.createUserWithEmailAndPassword(email, password).await()
                }
                userPreferencesRepository.setLoggedIn(true)
                _uiState.value = EmailAuthState.Success
            } catch (e: Exception) {
                _uiState.value = EmailAuthState.Error(e.message ?: "操作失败")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _uiState.value = EmailAuthState.Error("请先输入邮箱地址")
                return@launch
            }
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = EmailAuthState.Error("密码重置邮件已发送至您的邮箱")
            } catch (e: Exception) {
                _uiState.value = EmailAuthState.Error(e.message ?: "发送失败")
            }
        }
    }
    
    fun clearError() {
        if (_uiState.value is EmailAuthState.Error) {
            _uiState.value = EmailAuthState.Idle
        }
    }
}

sealed interface EmailAuthState {
    data object Idle : EmailAuthState
    data object Loading : EmailAuthState
    data object Success : EmailAuthState
    data class Error(val message: String) : EmailAuthState
}
