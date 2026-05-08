package com.aifinance.feature.importer

import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BatchImportProcessorTest {

    @Test
    fun `process removes existing and in-file duplicates`() {
        val accountId = UUID.randomUUID()
        val existing = Transaction(
            accountId = accountId,
            categoryId = CategoryCatalog.Ids.ExpenseFood,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("28.50"),
            currency = "CNY",
            title = "\u661f\u5df4\u514b\u5496\u5561",
            description = "\u62ff\u94c1",
            date = LocalDate.of(2026, 5, 8),
        )
        val rows = listOf(
            bill("\u661f\u5df4\u514b\u5496\u5561", "\u62ff\u94c1", "28.50"),
            bill("\u661f\u5df4\u514b\u5496\u5561", "\u62ff\u94c1", "28.50"),
            bill("\u5730\u94c1\u4e8c\u53f7\u7ebf", "\u4ea4\u901a\u51fa\u884c", "4.00"),
        )

        val result = BatchImportProcessor.process(rows, listOf(existing))

        assertEquals(3, result.rows.size)
        assertEquals(2, result.duplicateCount)
        assertEquals(1, result.importableRows.size)
        assertEquals(CategoryCatalog.Ids.ExpenseTransport, result.importableRows.single().categoryId)
    }

    @Test
    fun `suggestCategory handles common mainstream bookkeeping categories`() {
        val salary = bill(
            title = "\u4e94\u6708\u5de5\u8d44",
            note = "\u516c\u53f8\u4ee3\u53d1\u85aa\u8d44",
            amount = "12000.00",
            type = TransactionType.INCOME,
        )
        val shopping = bill(
            title = "\u4eac\u4e1c\u6570\u7801",
            note = "\u8033\u673a",
            amount = "399.00",
        )

        val salarySuggestion = BatchImportProcessor.suggestCategory(salary)
        val shoppingSuggestion = BatchImportProcessor.suggestCategory(shopping)

        assertEquals(CategoryCatalog.Ids.IncomeCareer, salarySuggestion.categoryId)
        assertTrue(salarySuggestion.confidence >= 0.9f)
        assertEquals(CategoryCatalog.Ids.ExpenseShopping, shoppingSuggestion.categoryId)
        assertTrue(shoppingSuggestion.confidence >= 0.9f)
    }

    @Test
    fun `unknown row falls back to other category with review confidence`() {
        val unknown = bill("\u672a\u77e5\u5546\u6237", "\u5907\u6ce8", "19.90")

        val suggestion = BatchImportProcessor.suggestCategory(unknown)

        assertEquals(CategoryCatalog.Ids.ExpenseOther, suggestion.categoryId)
        assertFalse(suggestion.confidence >= 0.72f)
    }

    @Test
    fun `mock bank statement simulates dedupe and auto categorization`() {
        val bytes = javaClass.classLoader
            ?.getResourceAsStream("mock_bank_statement.csv")
            ?.use { it.readBytes() }
            ?: error("missing mock bank statement")

        val rows = BankStatementParser.parseCsvBytes(bytes)
        val result = BatchImportProcessor.process(rows, emptyList())

        assertEquals(5, rows.size)
        assertEquals(1, result.duplicateCount)
        assertEquals(4, result.importableRows.size)
        assertTrue(result.importableRows.any { it.categoryId == CategoryCatalog.Ids.ExpenseFood })
        assertTrue(result.importableRows.any { it.categoryId == CategoryCatalog.Ids.ExpenseTransport })
        assertTrue(result.importableRows.any { it.categoryId == CategoryCatalog.Ids.ExpenseShopping })
        assertTrue(result.importableRows.any { it.categoryId == CategoryCatalog.Ids.IncomeCareer })
    }

    private fun bill(
        title: String,
        note: String,
        amount: String,
        type: TransactionType = TransactionType.EXPENSE,
    ): ParsedBankBill {
        return ParsedBankBill(
            dateTime = LocalDateTime.of(2026, 5, 8, 12, 0),
            amount = BigDecimal(amount),
            type = type,
            title = title,
            note = note,
        )
    }
}
