package com.aifinance.core.data.repository

import com.aifinance.core.database.dao.SavingsGoalDao
import com.aifinance.core.database.dao.SavingsRecordDao
import com.aifinance.core.database.entity.toDomain
import com.aifinance.core.database.entity.toEntity
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import com.aifinance.core.model.SavingsRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepositoryImpl @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsRecordDao: SavingsRecordDao,
) : SavingsGoalRepository {

    override fun getAllGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllSavingsGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getGoalById(id: UUID): Flow<SavingsGoal?> {
        return getAllGoals().map { goals -> goals.find { it.id == id } }
    }

    override suspend fun createGoal(goal: SavingsGoal): SavingsGoal {
        val now = Instant.now()
        val toPersist = goal.copy(updatedAt = now)
        savingsGoalDao.insertSavingsGoal(toPersist.toEntity())
        return toPersist
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateSavingsGoal(
            goal.copy(updatedAt = Instant.now()).toEntity(),
        )
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        savingsGoalDao.deleteSavingsGoal(goal.toEntity())
    }

    override suspend fun adjustSavedAmount(id: UUID, delta: BigDecimal) {
        savingsGoalDao.updateCurrentAmountProgress(id, delta, Instant.now())
    }

    override suspend fun updateStatus(id: UUID, status: SavingsGoalStatus) {
        val entity = savingsGoalDao.getSavingsGoalById(id) ?: return
        savingsGoalDao.updateSavingsGoal(
            entity.copy(
                status = status.name,
                updatedAt = Instant.now(),
            ),
        )
    }

    override fun getRecordsByGoalId(goalId: UUID): Flow<List<SavingsRecord>> {
        return savingsRecordDao.getRecordsByGoalId(goalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addRecord(record: SavingsRecord) {
        savingsRecordDao.insertRecord(record.toEntity())
        savingsGoalDao.updateCurrentAmountProgress(record.savingsGoalId, record.amount, Instant.now())
    }

    override suspend fun deleteRecord(record: SavingsRecord) {
        savingsRecordDao.deleteRecord(record.toEntity())
        savingsGoalDao.updateCurrentAmountProgress(record.savingsGoalId, record.amount.negate(), Instant.now())
    }

    override suspend fun getGoalByAccountId(accountId: UUID): SavingsGoal? {
        return savingsGoalDao.getGoalByAccountId(accountId)?.toDomain()
    }

    override suspend fun deleteRecordByGoalDateAmount(
        savingsGoalId: UUID,
        date: java.time.LocalDate,
        amount: BigDecimal,
    ) {
        val record = savingsRecordDao.getRecordByGoalDateAmount(savingsGoalId, date, amount)
        if (record != null) {
            savingsRecordDao.deleteRecord(record)
            savingsGoalDao.updateCurrentAmountProgress(savingsGoalId, record.amount.negate(), Instant.now())
        }
    }
}
