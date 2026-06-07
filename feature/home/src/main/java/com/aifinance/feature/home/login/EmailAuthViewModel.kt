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
    private val auth: FirebaseAuth,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmailAuthState>(EmailAuthState.Idle)
    val uiState: StateFlow<EmailAuthState> = _uiState.asStateFlow()

    fun authenticate(email: String, password: String, isLogin: Boolean) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _uiState.value = EmailAuthState.Error("请输入邮箱和密码")
                return@launch
            }
            if (!isValidEmail(email)) {
                _uiState.value = EmailAuthState.Error("请输入有效的邮箱地址")
                return@launch
            }
            _uiState.value = EmailAuthState.Loading
            try {
                if (isLogin) {
                    auth.signInWithEmailAndPassword(email, password).await()
                } else {
                    auth.createUserWithEmailAndPassword(email, password).await()
                }
                val user = auth.currentUser
                val userEmail = user?.email ?: ""
                userPreferencesRepository.setEmail(userEmail)
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
            if (!isValidEmail(email)) {
                _uiState.value = EmailAuthState.Error("请输入有效的邮箱地址")
                return@launch
            }
            _uiState.value = EmailAuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = EmailAuthState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = EmailAuthState.Error(e.message ?: "发送重置邮件失败")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is EmailAuthState.Error) {
            _uiState.value = EmailAuthState.Idle
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

sealed interface EmailAuthState {
    data object Idle : EmailAuthState
    data object Loading : EmailAuthState
    data object Success : EmailAuthState
    data object PasswordResetSent : EmailAuthState
    data class Error(val message: String) : EmailAuthState
}
