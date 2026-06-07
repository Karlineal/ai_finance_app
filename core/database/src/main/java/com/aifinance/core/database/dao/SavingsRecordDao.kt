package com.aifinance.core.database.dao

import androidx.room.*
import com.aifinance.core.database.entity.SavingsRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SavingsRecordDao {
    @Query("SELECT * FROM savings_records WHERE savingsGoalId = :goalId ORDER BY createdAt DESC")
    fun getRecordsByGoalId(goalId: UUID): Flow<List<SavingsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SavingsRecordEntity)

    @Delete
    suspend fun deleteRecord(record: SavingsRecordEntity)

    @Query("SELECT COUNT(*) FROM savings_records WHERE savingsGoalId = :goalId")
    suspend fun getRecordCountByGoalId(goalId: UUID): Int

    @Query("SELECT * FROM savings_records WHERE savingsGoalId = :goalId AND date = :date AND amount = :amount LIMIT 1")
    suspend fun getRecordByGoalDateAmount(
        goalId: UUID,
        date: java.time.LocalDate,
        amount: java.math.BigDecimal,
    ): SavingsRecordEntity?
}
