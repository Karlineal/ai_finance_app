package com.aifinance.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aifinance.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, time DESC")
    fun getTransactionsByAccount(accountId: UUID): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE linkedTransactionId = :linkedId AND id != :excludeId LIMIT 1")
    suspend fun getTransactionByLinkedId(linkedId: UUID, excludeId: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE type = 'TRANSFER' AND date = :date AND amount = :amount AND description = :description AND id != :excludeId LIMIT 1")
    suspend fun findLinkedTransfer(date: LocalDate, amount: String, description: String?, excludeId: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE type = 'TRANSFER' AND date = :date AND description = :description")
    suspend fun getTransfersByDescription(date: LocalDate, description: String?): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userConfirmed = 0 ORDER BY date DESC")
    fun getPendingConfirmations(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
