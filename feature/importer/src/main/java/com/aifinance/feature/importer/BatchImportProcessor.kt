package com.aifinance.feature.importer

import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

data class ProcessedImportRow(
    val row: ParsedBankBill,
    val categoryId: UUID,
    val confidence: Float,
    val duplicateReason: String? = null,
) {
    val isDuplicate: Boolean
        get() = duplicateReason != null
}

data class BatchImportResult(
    val rows: List<ProcessedImportRow>,
    val confidentCategoryThreshold: Float = 0.72f,
) {
    val importableRows: List<ProcessedImportRow>
        get() = rows.filterNot { it.isDuplicate }

    val duplicateCount: Int
        get() = rows.count { it.isDuplicate }

    val categorizedCount: Int
        get() = rows.count { it.confidence >= confidentCategoryThreshold }
}

data class BatchImportConfig(
    val incomeRules: List<CategoryRule> = BatchImportRuleCatalog.defaultIncomeRules,
    val expenseRules: List<CategoryRule> = BatchImportRuleCatalog.defaultExpenseRules,
    val confidentCategoryThreshold: Float = 0.72f,
)

object BatchImportRuleCatalog {
    val defaultIncomeRules = listOf(
        CategoryRule(
            CategoryCatalog.Ids.IncomeCareer,
            0.95f,
            "\u5de5\u8d44",
            "\u85aa\u8d44",
            "\u85aa\u6c34",
            "\u5956\u91d1",
            "\u52b3\u52a1",
        ),
        CategoryRule(
            CategoryCatalog.Ids.IncomeBusiness,
            0.9f,
            "\u7ecf\u8425",
            "\u8d27\u6b3e",
            "\u9500\u552e",
            "\u6536\u6b3e",
        ),
        CategoryRule(
            CategoryCatalog.Ids.IncomeInsurance,
            0.88f,
            "\u7406\u8d54",
            "\u4fdd\u9669",
            "\u7406\u8d22",
            "\u5229\u606f",
            "\u80a1\u606f",
        ),
        CategoryRule(CategoryCatalog.Ids.IncomeSecondHand, 0.86f, "\u4e8c\u624b", "\u95f2\u9c7c", "\u8f6c\u5356"),
        CategoryRule(CategoryCatalog.Ids.IncomeLucky, 0.84f, "\u7ea2\u5305", "\u8d60\u4e0e", "\u793c\u91d1"),
        CategoryRule(CategoryCatalog.Ids.IncomeLiving, 0.82f, "\u751f\u6d3b\u8d39", "\u8865\u8d34", "\u62a5\u9500"),
    )

    val defaultExpenseRules = listOf(
        CategoryRule(CategoryCatalog.Ids.ExpenseFood, 0.95f, "\u9910", "\u996d", "\u5496\u5561", "\u5976\u8336", "\u5916\u5356", "\u8d85\u5e02", "mcdonald", "kfc", "starbucks"),
        CategoryRule(CategoryCatalog.Ids.ExpenseTransport, 0.92f, "\u5730\u94c1", "\u516c\u4ea4", "\u6253\u8f66", "\u6ef4\u6ef4", "\u9ad8\u94c1", "\u673a\u7968", "\u52a0\u6cb9", "\u505c\u8f66"),
        CategoryRule(CategoryCatalog.Ids.ExpenseShopping, 0.9f, "\u6dd8\u5b9d", "\u4eac\u4e1c", "\u62fc\u591a\u591a", "\u5546\u573a", "\u8d2d\u7269", "\u670d\u9970", "\u6570\u7801"),
        CategoryRule(
            CategoryCatalog.Ids.ExpenseHousing,
            0.9f,
            "\u623f\u79df",
            "\u7269\u4e1a",
            "\u6c34\u7535",
            "\u71c3\u6c14",
            "\u5bbd\u5e26",
        ),
        CategoryRule(
            CategoryCatalog.Ids.ExpenseCommunication,
            0.86f,
            "\u8bdd\u8d39",
            "\u6d41\u91cf",
            "\u79fb\u52a8",
            "\u8054\u901a",
            "\u7535\u4fe1",
        ),
        CategoryRule(
            CategoryCatalog.Ids.ExpenseMedical,
            0.9f,
            "\u533b\u9662",
            "\u836f",
            "\u95e8\u8bca",
            "\u4f53\u68c0",
            "\u533b\u7597",
        ),
        CategoryRule(
            CategoryCatalog.Ids.ExpenseEducation,
            0.88f,
            "\u5b66\u8d39",
            "\u8bfe\u7a0b",
            "\u6559\u80b2",
            "\u4e66",
            "\u57f9\u8bad",
        ),
        CategoryRule(
            CategoryCatalog.Ids.ExpenseEntertainment,
            0.86f,
            "\u7535\u5f71",
            "\u6e38\u620f",
            "\u97f3\u4e50",
            "\u89c6\u9891",
            "\u4f1a\u5458",
            "\u65c5\u6e38",
        ),
    )
}

object BatchImportProcessor {
    fun process(
        rows: List<ParsedBankBill>,
        existingTransactions: List<Transaction>,
        config: BatchImportConfig = BatchImportConfig(),
    ): BatchImportResult {
        val seenInBatch = mutableSetOf<String>()
        val existingKeys = existingTransactions.map { it.toStrictDuplicateKey() }.toSet()

        val processed = rows.map { row ->
            val duplicateKey = row.toStrictDuplicateKey()
            val duplicateReason = when {
                duplicateKey in existingKeys -> "\u5df2\u5b58\u5728\u76f8\u540c\u4ea4\u6613"
                !seenInBatch.add(duplicateKey) -> "\u672c\u6b21\u5bfc\u5165\u6587\u4ef6\u5185\u91cd\u590d"
                else -> null
            }
            val suggestion = suggestCategory(row, config)
            ProcessedImportRow(
                row = row,
                categoryId = suggestion.categoryId,
                confidence = suggestion.confidence,
                duplicateReason = duplicateReason,
            )
        }
        return BatchImportResult(
            rows = processed,
            confidentCategoryThreshold = config.confidentCategoryThreshold,
        )
    }

    fun suggestCategory(row: ParsedBankBill, config: BatchImportConfig = BatchImportConfig()): CategorySuggestion {
        val rules = if (row.type == TransactionType.INCOME) config.incomeRules else config.expenseRules
        val text = "${row.title} ${row.note.orEmpty()}".normalizeForMatch()
        val matched = rules.firstOrNull { rule -> rule.keywords.any { text.contains(it.normalizeForMatch()) } }
        if (matched != null) {
            return CategorySuggestion(matched.categoryId, matched.confidence)
        }
        val fallback = if (row.type == TransactionType.INCOME) {
            CategoryCatalog.Ids.IncomeOther
        } else {
            CategoryCatalog.Ids.ExpenseOther
        }
        return CategorySuggestion(fallback, 0.45f)
    }

    private fun Transaction.toStrictDuplicateKey(): String {
        return listOf(
            date.toString(),
            type.name,
            amount.normalizedAmount(),
            title.normalizeForMatch(),
        ).joinToString("|")
    }

    private fun ParsedBankBill.toStrictDuplicateKey(): String {
        return listOf(
            dateTime.toLocalDate().toString(),
            type.name,
            amount.normalizedAmount(),
            title.normalizeForMatch(),
        ).joinToString("|")
    }

    private fun BigDecimal.normalizedAmount(): String {
        return stripTrailingZeros().toPlainString()
    }

    private fun String.normalizeForMatch(): String {
        return lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), "")
            .replace(Regex("[\\p{Punct}\u3000-\u303f\uff00-\uffef]"), "")
    }
}

data class CategorySuggestion(
    val categoryId: UUID,
    val confidence: Float,
)

data class CategoryRule(
    val categoryId: UUID,
    val confidence: Float,
    val keywords: List<String>,
) {
    constructor(categoryId: UUID, confidence: Float, vararg keywords: String) : this(
        categoryId = categoryId,
        confidence = confidence,
        keywords = keywords.toList(),
    )
}
