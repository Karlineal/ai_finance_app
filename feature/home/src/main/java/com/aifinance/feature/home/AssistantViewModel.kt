package com.aifinance.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.ai.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun onSuggestionClick(question: String) {
        _uiState.value = _uiState.value.copy(inputText = question)
        sendMessage()
    }

    fun rotateSuggestionGroup(groupCount: Int) {
        if (groupCount <= 0) return
        val nextIndex = (_uiState.value.suggestionGroupIndex + 1) % groupCount
        _uiState.value = _uiState.value.copy(suggestionGroupIndex = nextIndex)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = AssistantMessage(role = AssistantRole.USER, content = text)
        _uiState.value = _uiState.value.copy(
            inputText = "",
            messages = _uiState.value.messages + userMessage,
            isLoading = true
        )

        viewModelScope.launch {
            aiRepository.sendMessage(text)
                .onSuccess { response ->
                    val assistantMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = response
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    val errorMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = "抱歉，发生了错误：${error.message}"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + errorMessage,
                        isLoading = false
                    )
                }
        }
    }

    fun sendImageForOCR(imageUri: Uri, file: File) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            aiRepository.recognizeImage(file)
                .onSuccess { ocrText ->
                    val prompt = "我上传了一张账单图片，OCR识别结果如下：\n\n$ocrText\n\n请帮我提取关键信息（金额、日期、商家、分类等），并建议如何记录这笔交易。"
                    aiRepository.sendMessage(prompt)
                        .onSuccess { response ->
                            val assistantMessage = AssistantMessage(
                                role = AssistantRole.ASSISTANT,
                                content = response
                            )
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + assistantMessage,
                                isLoading = false
                            )
                        }
                        .onFailure { error ->
                            val errorMessage = AssistantMessage(
                                role = AssistantRole.ASSISTANT,
                                content = "OCR识别成功，但AI分析失败：${error.message}"
                            )
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + errorMessage,
                                isLoading = false
                            )
                        }
                }
                .onFailure { error ->
                    val errorMessage = AssistantMessage(
                        role = AssistantRole.ASSISTANT,
                        content = "OCR识别失败：${error.message}"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + errorMessage,
                        isLoading = false
                    )
                }
        }
    }

    fun clearConversation() {
        aiRepository.clearConversation()
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            inputText = ""
        )
    }
}
