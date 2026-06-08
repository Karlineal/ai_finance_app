package com.aifinance.core.data.repository

import com.aifinance.core.model.StatisticsAnalysisContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsAnalysisBridge @Inject constructor() {
    private val _openAiTabRequest = MutableStateFlow(false)
    val openAiTabRequest: StateFlow<Boolean> = _openAiTabRequest.asStateFlow()

    private var pendingContext: StatisticsAnalysisContext? = null

    @Synchronized
    fun navigateToAiWithContext(context: StatisticsAnalysisContext) {
        pendingContext = context
        _openAiTabRequest.value = true
    }

    @Synchronized
    fun consumePendingContext(): StatisticsAnalysisContext? {
        val context = pendingContext
        pendingContext = null
        return context
    }

    fun consumeOpenAiTabRequest() {
        _openAiTabRequest.value = false
    }
}
