package com.aifinance.feature.importer

import com.aifinance.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class BankStatementParserTest {

    @Test
    fun `parse valid bank csv returns correct transactions`() {
        val csv = """
            交易时间,金额,摘要
            2024-04-01 11:00:00,-88.00,餐饮消费
            2024-04-02 15:30:00,5000.00,工资
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        val list = BankStatementParser.parseCsvBytes(bytes)
        assertEquals(2, list.size)

        val expense = list[0]
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertTrue(expense.amount.compareTo(BigDecimal("88.00")) == 0)

        val income = list[1]
        assertEquals(TransactionType.INCOME, income.type)
        assertTrue(income.amount.compareTo(BigDecimal("5000.00")) == 0)
    }

    @Test
    fun `parse empty file returns empty list`() {
        assertTrue(BankStatementParser.parseCsvBytes(ByteArray(0)).isEmpty())
    }

    @Test
    fun `parse file without recognizable header returns empty list`() {
        val csv = """
            a,b
            hello,world
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        assertTrue(BankStatementParser.parseCsvBytes(bytes).isEmpty())
    }
}
