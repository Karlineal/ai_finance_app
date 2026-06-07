package com.aifinance.feature.importer

import android.content.Context
import android.net.Uri
import com.aifinance.core.model.TransactionType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object WechatBillParser {

    private val dateKeywords = listOf("交易时间", "交易日期", "记账日期", "入账日期", "日期", "时间")
    private val amountKeywords = listOf("交易金额", "金额", "发生额", "发生金额", "金额(元)")
    private val directionKeywords = listOf("收/支", "收支类型", "收支")
    private val tradeTypeKeywords = listOf("交易类型", "交易分类", "业务类型")
    private val titleKeywords = listOf("摘要", "交易摘要", "交易名称", "用途", "商户名称", "对方户名", "备注")
    private val counterpartyKeywords = listOf("交易对方", "对方户名", "对方帐号", "对方姓名", "对方名字")
    private val productKeywords = listOf("商品", "商品名称", "交易名称", "业务描述")
    private val noteKeywords = listOf("备注", "附言", "说明", "用途", "交易摘要")

    private val dateTimePatterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm",
    )

    private val dateOnlyPatterns = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyyMMdd",
    )

    fun parse(context: Context, uri: Uri): List<ParsedBankBill> {
        val lowerName = (uri.lastPathSegment ?: "").lowercase(Locale.ROOT)
        return if (lowerName.endsWith(".csv")) {
            parseCsv(context, uri)
        } else {
            runCatching { parseExcel(context, uri) }.getOrElse { parseCsv(context, uri) }
        }
    }

    private fun parseExcel(context: Context, uri: Uri): List<ParsedBankBill> {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val workbook = WorkbookFactory.create(input)
            workbook.use { wb ->
                val sheet = wb.getSheetAt(0)
                if (sheet.physicalNumberOfRows <= 1) return emptyList()

                val rows = sheet.rowIterator().asSequence().toList()
                val rowValues = rows.map { row ->
                    (0..row.lastCellNum.toInt().coerceAtLeast(20)).map { col ->
                        row.getCell(col)?.asDisplayText().orEmpty()
                    }
                }

                val header = detectHeader(rowValues.take(60))
                if (!header.isUseful) return emptyList()

                return rows.drop(header.headerRowIndex + 1).mapNotNull { row ->
                    parseDataRow(
                        getValue = { idx -> row.getCell(idx)?.asDisplayText().orEmpty() },
                        getCell = { idx -> row.getCell(idx) },
                        header = header,
                    )
                }
            }
        }
        return emptyList()
    }

    private fun parseCsv(context: Context, uri: Uri): List<ParsedBankBill> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
        return parseCsvBytes(bytes)
    }

    fun parseCsvBytes(bytes: ByteArray): List<ParsedBankBill> {
        if (bytes.isEmpty()) return emptyList()

        val rows = decodeCsvLines(bytes).map { splitCsvLine(it) }
        if (rows.isEmpty()) return emptyList()

        val header = detectHeader(rows.take(40))
        if (!header.isUseful) return emptyList()

        return rows.drop(header.headerRowIndex + 1).mapNotNull { values ->
            parseDataRow(
                getValue = { idx -> values.getOrNull(idx).orEmpty() },
                getCell = { null },
                header = header,
            )
        }
    }

    private fun parseDataRow(
        getValue: (Int) -> String,
        getCell: (Int) -> Cell?,
        header: WechatHeaderInfo,
    ): ParsedBankBill? {
        val dateText = header.dateIndex?.let(getValue).orEmpty()
        val dateTime = parseDateTime(dateText, header.dateIndex?.let(getCell)) ?: return null
        val amountText = header.amountIndex?.let(getValue).orEmpty()
        val amount = parseAmount(amountText) ?: return null
        if (amount == BigDecimal.ZERO) return null

        val direction = header.directionIndex?.let(getValue).orEmpty()
        val tradeType = header.tradeTypeIndex?.let(getValue).orEmpty()
        val includeInExpense = !isExcludedFromIncomeExpense(direction)
        val title = listOf(
            header.productIndex?.let(getValue).orEmpty(),
            header.titleIndex?.let(getValue).orEmpty(),
            header.counterpartyIndex?.let(getValue).orEmpty(),
        ).firstOrNull { it.isNotBlank() && it != "/" } ?: "微信账单导入"
        val note = header.noteIndex?.let(getValue)?.takeIf { it.isNotBlank() && it != "/" }

        val type = resolveType(
            direction = direction,
            tradeType = tradeType,
            rawAmount = amountText,
            amount = amount,
            context = "$title ${note.orEmpty()}",
        ) ?: return null
        return ParsedBankBill(
            dateTime = dateTime,
            amount = amount.abs(),
            type = type,
            title = cleanTitle(title),
            note = note,
            includeInExpense = includeInExpense,
        )
    }

    private fun resolveType(
        direction: String,
        tradeType: String,
        rawAmount: String,
        amount: BigDecimal,
        context: String,
    ): TransactionType? {
        val directionText = direction.lowercase(Locale.ROOT)
        val tradeTypeText = tradeType.lowercase(Locale.ROOT)
        val text = "$tradeTypeText $context".lowercase(Locale.ROOT)
        if (directionText.contains("收入") || directionText.contains("贷") || directionText.contains("入")) {
            return TransactionType.INCOME
        }
        if (directionText.contains("支出") || directionText.contains("借") || directionText.contains("出")) {
            return TransactionType.EXPENSE
        }

        if (rawAmount.trim().startsWith("+")) return TransactionType.INCOME
        if (rawAmount.trim().startsWith("-")) return TransactionType.EXPENSE

        if (listOf(
                "消费",
                "付款",
                "取现",
                "提现",
                "转出",
                "充值",
                "缴费",
                "购买",
            ).any { directionText.contains(it) || text.contains(it) }
        ) {
            return TransactionType.EXPENSE
        }
        if (listOf("存入", "存款", "工资", "转入", "收款", "退款", "报销", "红包", "到账", "返还").any {
                directionText.contains(it) || text.contains(it)
            }
        ) {
            return TransactionType.INCOME
        }
        if (amount.signum() > 0) return TransactionType.INCOME
        if (amount.signum() < 0) return TransactionType.EXPENSE
        return null
    }

    private fun cleanTitle(title: String): String {
        return title.removePrefix("\t")
            .removePrefix("支付-")
            .removePrefix("转账-")
            .removePrefix("扫码支付-")
            .removePrefix("付款-")
            .trim()
            .ifBlank { "微信账单导入" }
    }

    private fun detectHeader(rows: List<List<String>>): WechatHeaderInfo {
        var best = WechatHeaderInfo()
        rows.forEachIndexed { rowIndex, row ->
            if (row.all { it.isBlank() }) return@forEachIndexed
            val info = WechatHeaderInfo(
                headerRowIndex = rowIndex,
                dateIndex = findColumn(row, dateKeywords),
                amountIndex = findColumn(row, amountKeywords),
                directionIndex = findColumn(row, directionKeywords),
                tradeTypeIndex = findColumn(row, tradeTypeKeywords),
                titleIndex = findColumn(row, titleKeywords),
                counterpartyIndex = findColumn(row, counterpartyKeywords),
                productIndex = findColumn(row, productKeywords),
                noteIndex = findColumn(row, noteKeywords),
            )
            if (info.score > best.score) best = info
        }
        return best
    }

    private fun findColumn(row: List<String>, keywords: List<String>): Int? {
        return row.indexOfFirst { value ->
            val text = value.replace(Regex("\\s+"), "").replace("\u00A0", "")
            keywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }.takeIf { it >= 0 }
    }

    private fun parseDateTime(raw: String, cell: Cell?): LocalDateTime? {
        if (cell != null && cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return runCatching {
                val date = cell.dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                val hasTime = containsTimeToken(cell.cellStyle?.dataFormatString)
                if (hasTime) date else LocalDateTime.of(date.toLocalDate(), LocalTime.of(1, 0))
            }.getOrNull()
        }

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
        dateOnlyPatterns.forEach { pattern ->
            runCatching {
                val date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern))
                return LocalDateTime.of(date, LocalTime.of(1, 0))
            }
        }
        return null
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

    private fun decodeCsvLines(bytes: ByteArray): List<String> {
        val candidates = listOf("UTF-8", "GB18030", "GBK").map { java.nio.charset.Charset.forName(it) }
        candidates.forEach { charset ->
            val text = runCatching { String(bytes, charset) }.getOrNull() ?: return@forEach
            if (text.isBlank()) return@forEach
            val lines = text.lines().filter { it.trim().isNotBlank() }
            if (lines.size < 2) return@forEach
            val preview = lines.take(10).joinToString(" ")
            if (preview.contains("微信") || preview.contains("交易时间") || preview.contains("收/支")) {
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

    private fun containsTimeToken(dataFormat: String?): Boolean {
        val value = dataFormat?.lowercase(Locale.ROOT).orEmpty()
        return value.contains("h") || value.contains("m") || value.contains("s")
    }

    private fun isExcludedFromIncomeExpense(direction: String): Boolean {
        val normalized = direction.trim()
        if (normalized == "/") return true
        if (normalized == "不计收支" || normalized == "不计支出") return true
        return false
    }

    private fun Cell.asDisplayText(): String {
        return when (cellType) {
            CellType.STRING -> stringCellValue.orEmpty().trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(this)) {
                    runCatching {
                        dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
                    }.getOrElse { numericCellValue.toString() }
                } else {
                    numericCellValue.toBigDecimal().stripTrailingZeros().toPlainString()
                }
            }
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> runCatching {
                when (cachedFormulaResultType) {
                    CellType.STRING -> stringCellValue.orEmpty().trim()
                    CellType.NUMERIC -> numericCellValue.toBigDecimal().stripTrailingZeros().toPlainString()
                    else -> toString().trim()
                }
            }.getOrElse { toString().trim() }
            else -> toString().trim()
        }
    }
}

private data class WechatHeaderInfo(
    val headerRowIndex: Int = 0,
    val dateIndex: Int? = null,
    val amountIndex: Int? = null,
    val directionIndex: Int? = null,
    val tradeTypeIndex: Int? = null,
    val titleIndex: Int? = null,
    val counterpartyIndex: Int? = null,
    val productIndex: Int? = null,
    val noteIndex: Int? = null,
) {
    val score: Int
        get() {
            var s = 0
            if (dateIndex != null) s += 3
            if (amountIndex != null) s += 2
            if (directionIndex != null) s += 2
            if (tradeTypeIndex != null) s += 1
            if (titleIndex != null || productIndex != null || counterpartyIndex != null) s += 1
            return s
        }

    val isUseful: Boolean
        get() = dateIndex != null && amountIndex != null
}
