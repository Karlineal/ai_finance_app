package com.aifinance.feature.home

import androidx.lifecycle.ViewModel
import com.aifinance.core.data.repository.StatisticsAnalysisBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeContainerViewModel @Inject constructor(
    private val analysisBridge: StatisticsAnalysisBridge,
) : ViewModel() {

    val openAiTabRequest: StateFlow<Boolean> = analysisBridge.openAiTabRequest

    fun consumeOpenAiTabRequest() {
        analysisBridge.consumeOpenAiTabRequest()
    }

    fun consumePendingStatisticsContext() = analysisBridge.consumePendingContext()
}
