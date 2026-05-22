package com.aifinance.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.ScheduledRuleRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.schedule.ScheduleOccurrenceCalculator
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal

class ScheduledTransactionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScheduledWorkerEntryPoint::class.java,
        )
        val scheduledRuleRepository = entryPoint.scheduledRuleRepository()
        val transactionRepository = entryPoint.transactionRepository()
        val categoryRepository = entryPoint.categoryRepository()
        val scheduledRuleScheduler = entryPoint.scheduledRuleScheduler()

        val ruleIdStr = inputData.getString(KEY_RULE_ID) ?: return Result.failure()
        val ruleId = try {
            UUID.fromString(ruleIdStr)
        } catch (_: IllegalArgumentException) {
            return Result.failure()
        }

        Log.d("ScheduledWorker", "开始执行定时记账任务: ruleId=$ruleId")

        val rule = scheduledRuleRepository.getById(ruleId) ?: run {
            Log.w("ScheduledWorker", "规则不存在: ruleId=$ruleId")
            return Result.success()
        }
        if (!rule.enabled) {
            Log.d("ScheduledWorker", "规则已禁用: ruleId=$ruleId")
            return Result.success()
        }

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zone)

        if (!canFire(rule, today)) {
            scheduledRuleScheduler.cancelRule(ruleId)
            return Result.success()
        }

        return try {
            if (rule.title.startsWith("SAVINGS_GOAL_")) {
                val goalIdStr = rule.title.removePrefix("SAVINGS_GOAL_")
                val goalId = UUID.fromString(goalIdStr)
                val goal = entryPoint.savingsGoalRepository().getGoalById(goalId).firstOrNull()
                if (goal == null || goal.status != com.aifinance.core.model.SavingsGoalStatus.ACTIVE) {
                    scheduledRuleScheduler.cancelRule(ruleId)
                    return Result.success()
                }

                val periodIndex = com.aifinance.core.data.repository.SavingsGoalCalculator.getCurrentPeriodIndex(goal.startDate, goal.savingsMethod, goal.frequency)
                
                val amountToSave = when (goal.savingsMethod) {
                    com.aifinance.core.model.SavingsMethod.DAILY_365 -> 
                        com.aifinance.core.data.repository.SavingsGoalCalculator.calculateDay365Amount(periodIndex, goal.baseAmount ?: BigDecimal.ONE)
                    com.aifinance.core.model.SavingsMethod.WEEKLY_52 -> 
                        com.aifinance.core.data.repository.SavingsGoalCalculator.calculateWeek52Amount(periodIndex, goal.baseAmount ?: BigDecimal(10))
                    com.aifinance.core.model.SavingsMethod.MONTHLY_12 -> 
                        com.aifinance.core.data.repository.SavingsGoalCalculator.calculateMonth12Amount(periodIndex, goal.baseAmount ?: BigDecimal(100))
                    com.aifinance.core.model.SavingsMethod.FIXED_AMOUNT -> 
                        goal.fixedAmount ?: BigDecimal.ZERO
                    com.aifinance.core.model.SavingsMethod.FLEXIBLE -> 
                        BigDecimal.ZERO
                }

                if (amountToSave > BigDecimal.ZERO) {
                    val record = com.aifinance.core.model.SavingsRecord(
                        id = UUID.randomUUID(),
                        savingsGoalId = goalId,
                        amount = amountToSave,
                        date = today,
                        note = "自动打卡",
                        periodIndex = periodIndex,
                        createdAt = now
                    )
                    entryPoint.savingsGoalRepository().addRecord(record)
                    
                    val txOut = Transaction(
                        id = UUID.randomUUID(),
                        accountId = rule.accountId,
                        categoryId = rule.categoryId,
                        type = rule.transactionType,
                        amount = amountToSave,
                        currency = "CNY",
                        title = "转出-攒钱小荷包",
                        description = "攒钱计划自动扣款：${goal.name}",
                        date = today,
                        time = now,
                        sourceType = TransactionSourceType.SCHEDULED,
                        userConfirmed = true
                    )
                    
                    val txIn = Transaction(
                        id = UUID.randomUUID(),
                        accountId = goal.accountId,
                        categoryId = rule.categoryId,
                        type = rule.transactionType,
                        amount = amountToSave,
                        currency = "CNY",
                        title = "转入-攒钱小荷包",
                        description = "攒钱计划自动扣款：${goal.name}",
                        date = today,
                        time = now,
                        sourceType = TransactionSourceType.SCHEDULED,
                        userConfirmed = true
                    )
                    
                    transactionRepository.insertTransaction(txOut)
                    transactionRepository.insertTransaction(txIn)
                    Log.i("ScheduledWorker", "自动攒钱已执行: ${goal.name}, 金额=$amountToSave, 日期=$today")
                }
            } else {
                val categoryName = rule.categoryId?.let { categoryRepository.getCategoryById(it)?.name }
                val transaction = Transaction(
                    id = UUID.randomUUID(),
                    accountId = rule.accountId,
                    categoryId = rule.categoryId,
                    type = rule.transactionType,
                    amount = rule.amount,
                    currency = rule.currency,
                    title = rule.title.ifBlank { "定时记账" },
                    description = categoryName,
                    date = today,
                    time = now,
                    sourceType = TransactionSourceType.SCHEDULED,
                    userConfirmed = true,
                )
                transactionRepository.insertTransaction(transaction)
                Log.i("ScheduledWorker", "定时记账已创建: ${rule.title}, 金额=${rule.amount}, 日期=$today")
            }

            val newCount = rule.firedCount + 1
            val fireLocal = LocalDateTime.of(today, LocalTime.of(rule.startHour, rule.startMinute))
            val nextBase = ScheduleOccurrenceCalculator.advance(fireLocal, rule.recurrence)
            val nextLocal = ScheduleOccurrenceCalculator.firstLocalDateTimeOnOrAfter(
                start = nextBase,
                recurrence = rule.recurrence,
                zone = zone,
                now = now,
            )
            val nextOccurrenceDate = nextLocal.toLocalDate()

            val maxOcc = rule.maxOccurrences
            val endD = rule.endDate
            val finished = when (rule.endMode) {
                ScheduledEndMode.AFTER_COUNT ->
                    maxOcc != null && newCount >= maxOcc
                ScheduledEndMode.END_DATE ->
                    endD != null && nextOccurrenceDate.isAfter(endD)
                ScheduledEndMode.NEVER -> false
            }

            val nextInstant = if (finished) {
                null
            } else {
                nextLocal.atZone(zone).toInstant()
            }

            val updatedRule = rule.copy(
                firedCount = newCount,
                lastFiredAt = now,
                nextRunAt = nextInstant,
                updatedAt = Instant.now(),
            )
            scheduledRuleRepository.update(updatedRule)

            if (finished || nextInstant == null) {
                scheduledRuleScheduler.cancelRule(ruleId)
                Log.d("ScheduledWorker", "规则已完成: ${rule.title}")
            } else {
                scheduledRuleScheduler.enqueueKnownNext(ruleId, nextInstant)
                Log.d("ScheduledWorker", "已调度下一次执行: ${rule.title}, 下次=$nextInstant")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduledWorker", "定时记账执行失败: ruleId=$ruleId", e)
            Result.retry()
        }
    }

    private fun canFire(rule: ScheduledRule, today: LocalDate): Boolean {
        val endD = rule.endDate
        if (rule.endMode == ScheduledEndMode.END_DATE && endD != null && today.isAfter(endD)) {
            return false
        }
        val maxOcc = rule.maxOccurrences
        if (rule.endMode == ScheduledEndMode.AFTER_COUNT &&
            maxOcc != null &&
            rule.firedCount >= maxOcc
        ) {
            return false
        }
        return true
    }

    companion object {
        const val KEY_RULE_ID = "scheduled_rule_id"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduledWorkerEntryPoint {
    fun scheduledRuleRepository(): ScheduledRuleRepository
    fun transactionRepository(): TransactionRepository
    fun categoryRepository(): CategoryRepository
    fun scheduledRuleScheduler(): ScheduledRuleScheduler
    fun savingsGoalRepository(): com.aifinance.core.data.repository.SavingsGoalRepository
}
