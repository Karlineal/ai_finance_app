package com.aifinance.core.data.repository

import com.aifinance.core.model.StatisticsAnalysisContext
import kotlinx.coroutines.flow.StateFlow

interface StatisticsAnalysisBridge {
    val openAiTabRequest: StateFlow<Boolean>

    fun navigateToAiWithContext(context: StatisticsAnalysisContext)
    fun consumePendingContext(): StatisticsAnalysisContext?
    fun consumeOpenAiTabRequest()
}
