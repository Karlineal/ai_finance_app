package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE // 账户删除时级联删除对应的攒钱计划
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class SavingsGoalEntity(
    @PrimaryKey
    val id: UUID,
    val accountId: UUID,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String, // 存储枚举的 String 表达形式形式
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * 数据库实体转换为 Domain 模型的扩展函数
 */
fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    accountId = accountId,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    startDate = startDate,
    endDate = endDate,
    status = SavingsGoalStatus.valueOf(status),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Domain 模型转换为数据库实体的扩展函数
 */
fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    accountId = accountId,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    startDate = startDate,
    endDate = endDate,
    status = status.name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)