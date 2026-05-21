package com.aifinance.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aifinance.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: UUID): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getSavingsGoalsByAccount(accountId: UUID): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    /**
     * 💡 P0 级核心逻辑：存入或取出金额的原子操作（利用 Room 内置事务）
     * @param id 攒钱计划 ID
     * @param amount 变更金额（存入为正数，取出为负数）
     * @param updatedAt 更新时间戳
     */
    @Transaction
    suspend fun updateCurrentAmountProgress(id: UUID, amount: BigDecimal, updatedAt: Instant) {
        val currentGoal = getSavingsGoalById(id) ?: return
        val newAmount = currentGoal.currentAmount.add(amount)

        // 确保金额不会变成负数
        val finalAmount = if (newAmount < BigDecimal.ZERO) BigDecimal.ZERO else newAmount

        val updatedGoal = currentGoal.copy(
            currentAmount = finalAmount,
            updatedAt = updatedAt
        )
        updateSavingsGoal(updatedGoal)
    }
}