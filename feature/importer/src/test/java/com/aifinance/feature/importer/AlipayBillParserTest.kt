package com.aifinance.feature.importer

import com.aifinance.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class AlipayBillParserTest {

    @Test
    fun `parse valid alipay csv returns correct transactions`() {
        val csv = """
            支付宝账单明细
            交易时间,交易分类,交易对方,对方账号,商品名称,收/支,金额,收/付款方式,交易状态
            2024-03-15 14:30:00,转账,张三,,咖啡,支出,-35.50,余额支付,成功
            2024-03-16 09:00:00,收入,公司,,工资,收入,+8000.00,银行卡,成功
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        val list = AlipayBillParser.parseBytes(bytes)
        assertEquals(2, list.size)

        val expense = list[0]
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertTrue(expense.amount.compareTo(BigDecimal("35.50")) == 0)
        assertEquals("咖啡", expense.title)

        val income = list[1]
        assertEquals(TransactionType.INCOME, income.type)
        assertTrue(income.amount.compareTo(BigDecimal("8000.00")) == 0)
    }

    @Test
    fun `parse empty file returns empty list`() {
        assertTrue(AlipayBillParser.parseBytes(ByteArray(0)).isEmpty())
    }

    @Test
    fun `parse file without header returns empty list`() {
        val csv = """
            col1,col2,col3
            a,b,c
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        assertTrue(AlipayBillParser.parseBytes(bytes).isEmpty())
    }
}
