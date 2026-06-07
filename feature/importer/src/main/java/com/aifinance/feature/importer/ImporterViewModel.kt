package com.aifinance.feature.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

enum class ImportChannel(val label: String) {
    WECHAT("\u5fae\u4fe1\u8d26\u5355"),
    ALIPAY("\u652f\u4ed8\u5b9d\u8d26\u5355"),
    BANK("\u94f6\u884c\u8d26\u5355"),
}

data class ImporterUiState(
    val selectedChannel: ImportChannel = ImportChannel.BANK,
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val categorizedCount: Int = 0,
    val lastBatchId: UUID? = null,
    val message: String? = null,
)

@HiltViewModel
class ImporterViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImporterUiState())
    val uiState: StateFlow<ImporterUiState> = _uiState.asStateFlow()

    fun selectChannel(channel: ImportChannel) {
        _uiState.value = _uiState.value.copy(selectedChannel = channel, message = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun importBankStatement(context: Context, uri: Uri) {
        importWithParser(
            parser = { BankStatementParser.parse(context, uri) },
            sourceType = TransactionSourceType.IMPORTED_BANK,
            successLabel = "\u94f6\u884c\u8d26\u5355",
        )
    }

    fun importSelectedStatement(context: Context, uri: Uri) {
        when (_uiState.value.selectedChannel) {
            ImportChannel.WECHAT -> importWithParser(
                parser = { WechatBillParser.parse(context, uri) },
                sourceType = TransactionSourceType.IMPORTED_WECHAT,
                successLabel = "\u5fae\u4fe1\u8d26\u5355",
            )

            ImportChannel.ALIPAY -> importWithParser(
                parser = { AlipayBillParser.parse(context, uri) },
                sourceType = TransactionSourceType.IMPORTED_ALIPAY,
                successLabel = "\u652f\u4ed8\u5b9d\u8d26\u5355",
            )

            ImportChannel.BANK -> importBankStatement(context, uri)
        }
    }

    private fun importWithParser(
        parser: () -> List<ParsedBankBill>,
        sourceType: TransactionSourceType,
        successLabel: String,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                duplicateCount = 0,
                categorizedCount = 0,
                message = null,
            )
            runCatching {
                val rows = parser()
                val accountId = accountRepository.getDefaultIncomeExpenseAccount()?.id
                    ?: accountRepository.getFirstActiveAccount()?.id
                    ?: error("\u672a\u627e\u5230\u53ef\u7528\u8d26\u6237\uff0c\u8bf7\u5148\u521b\u5efa\u8d26\u6237")
                val existingTransactions = transactionRepository.getAllTransactions().first()
                val batchId = UUID.randomUUID()
                val zoneId = ZoneId.systemDefault()
                val result = BatchImportProcessor.process(
                    rows = rows,
                    existingTransactions = existingTransactions,
                )

                var imported = 0
                result.importableRows.forEach { processed ->
                    val row = processed.row
                    val transaction = Transaction(
                        accountId = accountId,
                        categoryId = processed.categoryId,
                        type = row.type,
                        amount = row.amount,
                        currency = "CNY",
                        title = row.title,
                        description = row.note,
                        date = row.dateTime.toLocalDate(),
                        time = row.dateTime.atZone(zoneId).toInstant(),
                        sourceType = sourceType,
                        importBatchId = batchId,
                        rawText = row.note,
                        aiCategory = processed.categoryId,
                        aiConfidence = processed.confidence,
                        userConfirmed = processed.confidence >= result.confidentCategoryThreshold,
                        isPending = !row.includeInExpense,
                    )
                    transactionRepository.insertTransaction(transaction)
                    imported++
                }
                ImportSummary(
                    batchId = batchId,
                    imported = imported,
                    duplicates = result.duplicateCount,
                    categorized = result.categorizedCount,
                    total = rows.size,
                )
            }.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importedCount = summary.imported,
                    duplicateCount = summary.duplicates,
                    categorizedCount = summary.categorized,
                    lastBatchId = summary.batchId,
                    message = buildSuccessMessage(summary, successLabel),
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "\u5bfc\u5165\u5931\u8d25\uff1a${throwable.message ?: "\u672a\u77e5\u9519\u8bef"}",
                )
            }
        }
    }

    private fun buildSuccessMessage(summary: ImportSummary, successLabel: String): String {
        if (summary.total == 0) return "\u672a\u8bc6\u522b\u5230\u53ef\u5bfc\u5165\u7684\u8bb0\u5f55"
        return "\u5bfc\u5165\u6210\u529f\uff1a$successLabel ${summary.imported} \u6761\uff0c\u667a\u80fd\u53bb\u91cd ${summary.duplicates} \u6761\uff0c\u81ea\u52a8\u5206\u7c7b ${summary.categorized} \u6761"
    }
}

private data class ImportSummary(
    val batchId: UUID,
    val imported: Int,
    val duplicates: Int,
    val categorized: Int,
    val total: Int,
)

data class ParsedBankBill(
    val dateTime: java.time.LocalDateTime,
    val amount: BigDecimal,
    val type: TransactionType,
    val title: String,
    val note: String?,
    val includeInExpense: Boolean = true,
)
