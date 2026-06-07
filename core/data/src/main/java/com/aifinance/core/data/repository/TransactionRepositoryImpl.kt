package com.aifinance.core.data.repository

import android.util.Log
import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.dao.TransactionDao
import com.aifinance.core.database.entity.TransactionEntity
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val savingsGoalRepository: SavingsGoalRepository,
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.mapNotNull { entity ->
                runCatching { entity.toDomain() }
                    .onFailure { Log.e("TransactionRepo", "skip corrupt row ${entity.id}", it) }
                    .getOrNull()
            }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.mapNotNull { entity ->
                runCatching { entity.toDomain() }.getOrNull()
            }
        }
    }

    override fun getTransactionsByAccount(accountId: UUID): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.mapNotNull { entity ->
                runCatching { entity.toDomain() }.getOrNull()
            }
        }
    }

    override suspend fun getTransactionById(id: UUID): Transaction? {
        return transactionDao.getTransactionById(id)?.let { entity ->
            runCatching { entity.toDomain() }.getOrNull()
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        applyBalanceImpact(transaction, direction = 1)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val oldTransaction = transactionDao.getTransactionById(transaction.id)?.toDomain()
        if (oldTransaction != null) {
            applyBalanceImpact(oldTransaction, direction = -1)
        }
        transactionDao.update(transaction.toEntity())
        applyBalanceImpact(transaction, direction = 1)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val existing = transactionDao.getTransactionById(transaction.id)?.toDomain() ?: transaction
        transactionDao.delete(existing.toEntity())
        applyBalanceImpact(existing, direction = -1)
    }

    override suspend fun deleteTransactionWithLinked(transaction: Transaction) {
        val existing = transactionDao.getTransactionById(transaction.id)?.toDomain() ?: transaction
        val linkedId = existing.linkedTransactionId

        // Detect savings-goal transfer before deletion for cascade cleanup
        val isSavingsGoalTransfer = existing.title.contains("攒钱小荷包") ||
            existing.description?.contains("攒钱计划") == true

        // Delete the primary transaction
        transactionDao.delete(existing.toEntity())
        applyBalanceImpact(existing, direction = -1)

        // Delete the linked paired transaction if exists
        val linkedTx = if (linkedId != null) {
            transactionDao.getTransactionByLinkedId(linkedId, existing.id)?.toDomain()
        } else {
            // Fallback for old data without linkedTransactionId: match by date/amount/description
            transactionDao.findLinkedTransfer(
                date = existing.date,
                amount = existing.amount.toPlainString(),
                description = existing.description,
                excludeId = existing.id,
            )?.toDomain()
        }

        if (linkedTx != null) {
            transactionDao.delete(linkedTx.toEntity())
            applyBalanceImpact(linkedTx, direction = -1)
        }

        // Reverse cascade: delete the corresponding SavingsRecord
        if (isSavingsGoalTransfer) {
            val goalAccountId = when {
                existing.title.startsWith("转入") -> existing.accountId
                linkedTx?.title?.startsWith("转入") == true -> linkedTx.accountId
                else -> existing.accountId
            }
            val goal = savingsGoalRepository.getGoalByAccountId(goalAccountId)
            if (goal != null) {
                savingsGoalRepository.deleteRecordByGoalDateAmount(
                    savingsGoalId = goal.id,
                    date = existing.date,
                    amount = existing.amount,
                )
            }
        }
    }

    override suspend fun deleteTransfersByDescription(date: java.time.LocalDate, description: String?) {
        val transfers = transactionDao.getTransfersByDescription(date, description)
        transfers.forEach { tx ->
            deleteTransaction(tx.toDomain())
        }
    }

    override suspend fun clearAllTransactionHistory() {
        transactionDao.deleteAllTransactions()
        accountDao.clearAllBalancesToZero()
    }

    private suspend fun applyBalanceImpact(transaction: Transaction, direction: Int) {
        val baseDelta = when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> transaction.amount.negate()
            TransactionType.TRANSFER -> when {
                transaction.title.startsWith("转出") -> transaction.amount.negate()
                transaction.title.startsWith("转入") -> transaction.amount
                else -> transaction.amount.negate()
            }
        }

        if (baseDelta.compareTo(BigDecimal.ZERO) != 0) {
            val signedDelta = baseDelta.multiply(BigDecimal.valueOf(direction.toLong()))
            Log.d(
                "TransactionRepo",
                "Adjusting balance for account ${transaction.accountId}: " +
                    "type=${transaction.type}, amount=${transaction.amount}, " +
                    "delta=$signedDelta, direction=$direction",
            )
            accountDao.adjustCurrentBalance(transaction.accountId, signedDelta)
        } else {
            Log.w(
                "TransactionRepo",
                "Skipping balance adjustment for transaction ${transaction.id}: delta is zero",
            )
        }
    }
}

private fun TransactionEntity.toDomain(): Transaction {
    val txType = runCatching { TransactionType.valueOf(type) }
        .getOrElse { TransactionType.EXPENSE }
    val srcType = runCatching { TransactionSourceType.valueOf(sourceType) }
        .getOrElse { TransactionSourceType.MANUAL }
    return Transaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = txType,
        amount = amount,
        currency = currency,
        title = title,
        description = description,
        date = date,
        time = time,
        isPending = isPending,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceType = srcType,
        importBatchId = importBatchId,
        rawText = rawText,
        aiCategory = aiCategory,
        aiConfidence = aiConfidence,
        userConfirmed = userConfirmed,
        ocrSourceId = ocrSourceId,
        paymentMethod = paymentMethod,
        paymentAccount = paymentAccount,
        linkedTransactionId = linkedTransactionId,
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = type.name,
        amount = amount,
        currency = currency,
        title = title,
        description = description,
        date = date,
        time = time,
        isPending = isPending,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceType = sourceType.name,
        importBatchId = importBatchId,
        rawText = rawText,
        aiCategory = aiCategory,
        aiConfidence = aiConfidence,
        userConfirmed = userConfirmed,
        ocrSourceId = ocrSourceId,
        paymentMethod = paymentMethod,
        paymentAccount = paymentAccount,
        linkedTransactionId = linkedTransactionId,
    )
}
