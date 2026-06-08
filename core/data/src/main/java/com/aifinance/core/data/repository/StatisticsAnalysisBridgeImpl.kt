package com.aifinance.core.data.repository

import com.aifinance.core.model.StatisticsAnalysisContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsAnalysisBridgeImpl @Inject constructor() : StatisticsAnalysisBridge {

    private val _openAiTabRequest = MutableStateFlow(false)
    override val openAiTabRequest: StateFlow<Boolean> = _openAiTabRequest.asStateFlow()

    @Volatile
    private var pendingContext: StatisticsAnalysisContext? = null

    @Synchronized
    override fun navigateToAiWithContext(context: StatisticsAnalysisContext) {
        pendingContext = context
        _openAiTabRequest.value = true
    }

    @Synchronized
    override fun consumePendingContext(): StatisticsAnalysisContext? {
        val context = pendingContext
        pendingContext = null
        return context
    }

    override fun consumeOpenAiTabRequest() {
        _openAiTabRequest.value = false
    }
}
