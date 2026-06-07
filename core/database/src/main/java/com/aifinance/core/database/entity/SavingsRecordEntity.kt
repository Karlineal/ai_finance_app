package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aifinance.core.model.SavingsRecord
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "savings_records",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsGoalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["savingsGoalId"])],
)
data class SavingsRecordEntity(
    @PrimaryKey val id: UUID,
    val savingsGoalId: UUID,
    val amount: BigDecimal,
    val date: LocalDate,
    val note: String?,
    val periodIndex: Int,
    val createdAt: Instant,
)

fun SavingsRecordEntity.toDomain() = SavingsRecord(
    id = id,
    savingsGoalId = savingsGoalId,
    amount = amount,
    date = date,
    note = note,
    periodIndex = periodIndex,
    createdAt = createdAt,
)

fun SavingsRecord.toEntity() = SavingsRecordEntity(
    id = id,
    savingsGoalId = savingsGoalId,
    amount = amount,
    date = date,
    note = note,
    periodIndex = periodIndex,
    createdAt = createdAt,
)
