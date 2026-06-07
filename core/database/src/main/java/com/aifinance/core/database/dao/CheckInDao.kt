package com.aifinance.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aifinance.core.database.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface CheckInDao {

    @Query("SELECT * FROM check_ins WHERE savingsGoalId = :savingsGoalId ORDER BY date DESC")
    fun getCheckInsByGoal(savingsGoalId: UUID): Flow<List<CheckInEntity>>

    @Query(
        "SELECT * FROM check_ins WHERE savingsGoalId = :savingsGoalId " +
            "AND date BETWEEN :startDate AND :endDate ORDER BY date ASC",
    )
    fun getCheckInsByDateRange(savingsGoalId: UUID, startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>>

    @Query("SELECT COUNT(*) FROM check_ins WHERE savingsGoalId = :savingsGoalId")
    fun getCheckInCount(savingsGoalId: UUID): Flow<Int>

    @Query("SELECT * FROM check_ins WHERE savingsGoalId = :savingsGoalId AND date = :date LIMIT 1")
    suspend fun getCheckInByDate(savingsGoalId: UUID, date: LocalDate): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckInEntity)

    @Delete
    suspend fun deleteCheckIn(checkIn: CheckInEntity)

    @Query("DELETE FROM check_ins WHERE savingsGoalId = :savingsGoalId AND date = :date")
    suspend fun deleteCheckInByDate(savingsGoalId: UUID, date: LocalDate)
}
