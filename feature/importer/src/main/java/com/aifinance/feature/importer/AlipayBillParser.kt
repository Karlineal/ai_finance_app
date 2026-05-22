package com.aifinance.feature.importer

import android.content.Context
import android.net.Uri
import com.aifinance.core.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object AlipayBillParser {

    private val dateTimePatterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
    )

    fun parse(context: Context, uri: Uri): List<ParsedBankBill> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
        return parseBytes(bytes)
    }

    fun parseBytes(bytes: ByteArray): List<ParsedBankBill> {
        if (bytes.isEmpty()) return emptyList()

        val lines = decodeCsvLines(bytes)
        if (lines.isEmpty()) return emptyList()

        var headerIndex = -1
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("交易时间") && line.contains("收/支")) {
                headerIndex = i
                break
            }
        }
        if (headerIndex == -1) return emptyList()

        val result = mutableListOf<ParsedBankBill>()
        for (i in (headerIndex + 1) until lines.size) {
            val rawLine = lines[i].trim()
            if (rawLine.isEmpty() || rawLine.startsWith("-") || rawLine.startsWith("#")) continue

            val parts = splitCsvLine(rawLine).map {
                it.trim()
                    .removeSurrounding("\"")
                    .replace("\t", "")
                    .trim()
            }
            if (parts.size < 9) continue

            val dateTime = parseDateTime(parts[0]) ?: continue
            val counterparty = parts[2].ifBlank { "未知对象" }
            val product = parts[4]
            val typeText = parts[5]
            val tradeType = parts[1]
            val rawAmount = parts[6]
            val amount = parseAmount(parts[6]) ?: continue
            val status = parts[8]
            val includeInExpense = typeText != "不计支出" && typeText != "不计收支"
            val type = when (typeText) {
                "收入" -> TransactionType.INCOME
                "支出" -> TransactionType.EXPENSE
                else -> resolveTypeForExcluded(
                    tradeType = tradeType,
                    product = product,
                    counterparty = counterparty,
                    status = status,
                    rawAmount = rawAmount,
                    amount = amount,
                )
            }
            result += ParsedBankBill(
                dateTime = dateTime,
                amount = amount.abs(),
                type = type,
                title = product.ifBlank { counterparty },
                note = buildString {
                    append(counterparty)
                    if (status.isNotBlank() && status != "/") append(" $status")
                }.trim().ifBlank { null },
                includeInExpense = includeInExpense,
            )
        }
        return result
    }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val normalized = raw.trim()
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", " ")
            .replace("T", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return null

        dateTimePatterns.forEach { pattern ->
            runCatching {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern(pattern))
            }
        }

        return runCatching {
            val date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            LocalDateTime.of(date, LocalTime.of(1, 0))
        }.getOrNull()
    }

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw.trim()
            .replace(",", "")
            .replace("￥", "")
            .replace("¥", "")
            .replace("人民币", "")
            .replace(Regex("[^\\d+\\-.]"), "")
        if (cleaned.isBlank() || cleaned == "-" || cleaned == "." || cleaned == "+") return null
        return runCatching { BigDecimal(cleaned) }.getOrNull()
    }

    private fun resolveTypeForExcluded(
        tradeType: String,
        product: String,
        counterparty: String,
        status: String,
        rawAmount: String,
        amount: BigDecimal,
    ): TransactionType {
        val context = "$tradeType $product $counterparty $status".lowercase()

        if (rawAmount.trim().startsWith("+")) return TransactionType.INCOME
        if (rawAmount.trim().startsWith("-")) return TransactionType.EXPENSE

        val expenseKeywords = listOf("消费", "付款", "支付", "买", "扣款", "还款", "转出", "提现", "充值", "缴费")
        if (expenseKeywords.any { context.contains(it) }) return TransactionType.EXPENSE

        val incomeKeywords = listOf("收款", "收入", "退款", "返还", "报销", "工资", "奖金", "转入", "到账", "红包")
        if (incomeKeywords.any { context.contains(it) }) return TransactionType.INCOME

        return if (amount.signum() >= 0) TransactionType.INCOME else TransactionType.EXPENSE
    }

    private fun decodeCsvLines(bytes: ByteArray): List<String> {
        val candidates = listOf("UTF-8", "GB18030", "GBK").map { java.nio.charset.Charset.forName(it) }
        candidates.forEach { charset ->
            val text = runCatching { String(bytes, charset) }.getOrNull() ?: return@forEach
            if (text.isBlank()) return@forEach
            val lines = text.lines().filter { it.trim().isNotBlank() }
            if (lines.size < 2) return@forEach
            val preview = lines.take(10).joinToString(" ")
            if (preview.contains("交易时间") || preview.contains("收/支") || preview.contains("支付宝")) {
                return lines
            }
        }
        return String(bytes, Charsets.UTF_8).lines().filter { it.trim().isNotBlank() }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    currentToken.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString().trim().removePrefix("\t"))
                currentToken.setLength(0)
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim().removePrefix("\t"))
        return result
    }
}
