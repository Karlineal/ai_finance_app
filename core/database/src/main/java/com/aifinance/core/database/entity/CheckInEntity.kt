package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "check_ins",
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
data class CheckInEntity(
    @PrimaryKey val id: UUID,
    val savingsGoalId: UUID,
    val date: LocalDate,
    val amount: BigDecimal,
    val note: String?,
    val createdAt: Instant,
)
