package com.aifinance.core.data.repository

import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import com.aifinance.core.model.SavingsRecord
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

interface SavingsGoalRepository {
    fun getAllGoals(): Flow<List<SavingsGoal>>
    fun getGoalById(id: UUID): Flow<SavingsGoal?>
    suspend fun createGoal(goal: SavingsGoal): SavingsGoal
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goal: SavingsGoal)
    suspend fun adjustSavedAmount(id: UUID, delta: BigDecimal)
    suspend fun updateStatus(id: UUID, status: SavingsGoalStatus)
    
    fun getRecordsByGoalId(goalId: UUID): Flow<List<SavingsRecord>>
    suspend fun addRecord(record: SavingsRecord)
    suspend fun deleteRecord(record: SavingsRecord)
    suspend fun getGoalByAccountId(accountId: UUID): SavingsGoal?
    suspend fun deleteRecordByGoalDateAmount(savingsGoalId: UUID, date: java.time.LocalDate, amount: BigDecimal)
}
