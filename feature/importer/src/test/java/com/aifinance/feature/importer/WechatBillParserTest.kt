package com.aifinance.feature.importer

import com.aifinance.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class WechatBillParserTest {

    @Test
    fun `parse valid wechat csv returns correct transactions`() {
        val csv = """
            微信支付账单明细
            交易时间,收/支,交易金额,交易类型,商品
            2024-02-10 18:20:00,支出,¥42.00,商户消费,便利店
            2024-02-11 08:00:00,收入,100.00,转账,退款到账
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        val list = WechatBillParser.parseCsvBytes(bytes)
        assertEquals(2, list.size)

        val expense = list[0]
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertTrue(expense.amount.compareTo(BigDecimal("42.00")) == 0)

        val income = list[1]
        assertEquals(TransactionType.INCOME, income.type)
        assertTrue(income.amount.compareTo(BigDecimal("100.00")) == 0)
    }

    @Test
    fun `parse empty file returns empty list`() {
        assertTrue(WechatBillParser.parseCsvBytes(ByteArray(0)).isEmpty())
    }

    @Test
    fun `parse file without header returns empty list`() {
        val csv = """
            foo,bar,baz
            1,2,3
        """.trimIndent()
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        assertTrue(WechatBillParser.parseCsvBytes(bytes).isEmpty())
    }
}
