package com.aifinance.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val AssistantPlaceholderReply = "这是一个占位回复，后续接入真实 AI 模型。"

@HiltViewModel
class AssistantViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun onSuggestionClick(question: String) {
        _uiState.value = _uiState.value.copy(inputText = question)
    }

    fun rotateSuggestionGroup(groupCount: Int) {
        if (groupCount <= 0) return
        val nextIndex = (_uiState.value.suggestionGroupIndex + 1) % groupCount
        _uiState.value = _uiState.value.copy(suggestionGroupIndex = nextIndex)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val updatedMessages = _uiState.value.messages + listOf(
            AssistantMessage(role = AssistantRole.USER, content = text),
            AssistantMessage(role = AssistantRole.ASSISTANT, content = AssistantPlaceholderReply),
        )

        _uiState.value = _uiState.value.copy(
            inputText = "",
            messages = updatedMessages,
        )
    }
}
