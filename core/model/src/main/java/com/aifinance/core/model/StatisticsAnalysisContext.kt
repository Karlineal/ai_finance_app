package com.aifinance.core.model

data class AnalysisCategorySummary(
    val name: String,
    val amount: String,
    val count: Int,
)

data class StatisticsAnalysisContext(
    val periodKey: String,
    val periodLabel: String,
    val anchorDateIso: String,
    val expense: String,
    val income: String,
    val balance: String,
    val transactionCount: Int,
    val topCategories: List<AnalysisCategorySummary>,
    val suggestions: List<String>,
)
