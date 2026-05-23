package com.aifinance.feature.savings_goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.AccountRepository
import com.aifinance.core.data.repository.SavingsGoalRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.model.Account
import com.aifinance.core.model.AccountType
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsMethod
import com.aifinance.core.model.SavingsFrequency
import com.aifinance.core.model.SavingsGoalStatus
import com.aifinance.core.model.SavingsRecord
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val scheduledRuleRepository: com.aifinance.core.data.repository.ScheduledRuleRepository,
    private val scheduledRuleScheduler: com.aifinance.core.data.scheduler.ScheduledRuleScheduler
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createGoal(goal: SavingsGoal, isAutoDeduct: Boolean = false) {
        viewModelScope.launch {
            val accountId = UUID.randomUUID()
            val newAccount = Account(
                id = accountId,
                name = "${goal.name}小荷包",
                type = AccountType.INVESTMENT,
                currency = "CNY",
                initialBalance = BigDecimal.ZERO,
                currentBalance = BigDecimal.ZERO,
                color = 0xFFEAB308.toInt(),
                icon = "💰",
                note = "攒钱计划专属账户",
                includeInTotalAssets = true,
                isDefaultIncomeExpense = false
            )
            accountRepository.insertAccount(newAccount)
            
            val goalWithAccount = goal.copy(accountId = accountId)
            savingsGoalRepository.createGoal(goalWithAccount)

            if (isAutoDeduct) {
                val defaultAccount = accountRepository.getActiveAccounts().firstOrNull()?.firstOrNull { it.isDefaultIncomeExpense }
                    ?: accountRepository.getActiveAccounts().firstOrNull()?.firstOrNull { it.type == AccountType.CASH || it.type == AccountType.BANK }
                
                if (defaultAccount != null) {
                    val recurrence = when (goal.savingsMethod) {
                        SavingsMethod.DAILY_365 -> com.aifinance.core.model.ScheduledRecurrence.DAILY
                        SavingsMethod.WEEKLY_52 -> com.aifinance.core.model.ScheduledRecurrence.WEEKLY
                        SavingsMethod.MONTHLY_12 -> com.aifinance.core.model.ScheduledRecurrence.MONTHLY
                        SavingsMethod.FIXED_AMOUNT -> when (goal.frequency) {
                            SavingsFrequency.DAILY -> com.aifinance.core.model.ScheduledRecurrence.DAILY
                            SavingsFrequency.WEEKLY -> com.aifinance.core.model.ScheduledRecurrence.WEEKLY
                            SavingsFrequency.MONTHLY -> com.aifinance.core.model.ScheduledRecurrence.MONTHLY
                            else -> com.aifinance.core.model.ScheduledRecurrence.MONTHLY
                        }
                        else -> com.aifinance.core.model.ScheduledRecurrence.MONTHLY
                    }

                    val rule = com.aifinance.core.model.ScheduledRule(
                        id = UUID.randomUUID(),
                        enabled = true,
                        title = "SAVINGS_GOAL_${goal.id}",
                        transactionType = TransactionType.TRANSFER,
                        categoryId = CategoryCatalog.Ids.TransferDefault,
                        accountId = defaultAccount.id,
                        amount = goal.baseAmount ?: BigDecimal.ONE,
                        currency = "CNY",
                        startDate = goal.startDate,
                        startHour = 10,
                        startMinute = 0,
                        recurrence = recurrence,
                        endMode = com.aifinance.core.model.ScheduledEndMode.END_DATE,
                        endDate = goal.endDate,
                        maxOccurrences = null
                    )
                    scheduledRuleRepository.insert(rule)
                    val nextBase = java.time.LocalDateTime.of(goal.startDate, java.time.LocalTime.of(10, 0))
                    val nextLocal = com.aifinance.core.data.schedule.ScheduleOccurrenceCalculator.firstLocalDateTimeOnOrAfter(
                        start = nextBase,
                        recurrence = recurrence,
                        zone = java.time.ZoneId.systemDefault(),
                        now = Instant.now()
                    )
                    scheduledRuleScheduler.enqueueKnownNext(rule.id, nextLocal.atZone(java.time.ZoneId.systemDefault()).toInstant())
                }
            }
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.deleteGoal(goal)
        }
    }

    fun adjustSavedAmount(id: UUID, delta: BigDecimal) {
        viewModelScope.launch {
            savingsGoalRepository.adjustSavedAmount(id, delta)
        }
    }

    fun quickSave(goalId: UUID, amount: BigDecimal) {
        viewModelScope.launch {
            val defaultAccount = accountRepository.getActiveAccounts().firstOrNull()
                ?.firstOrNull { it.isDefaultIncomeExpense }
                ?: accountRepository.getActiveAccounts().firstOrNull()
                ?.firstOrNull { it.type == AccountType.CASH || it.type == AccountType.BANK }

            val record = SavingsRecord(
                id = UUID.randomUUID(),
                savingsGoalId = goalId,
                amount = amount,
                date = LocalDate.now(),
                note = "快速存入",
                periodIndex = 0,
                createdAt = Instant.now()
            )

            addRecord(record, defaultAccount?.id)
        }
    }

    fun updateStatus(id: UUID, status: SavingsGoalStatus) {
        viewModelScope.launch {
            savingsGoalRepository.updateStatus(id, status)
        }
    }

    fun getRecordsForGoal(goalId: UUID): Flow<List<SavingsRecord>> {
        return savingsGoalRepository.getRecordsByGoalId(goalId)
    }

    fun addRecord(record: SavingsRecord, sourceAccountId: UUID? = null) {
        viewModelScope.launch {
            savingsGoalRepository.addRecord(record)
            
            if (sourceAccountId != null) {
                val goal = savingsGoalRepository.getGoalById(record.savingsGoalId).firstOrNull()
                if (goal != null) {
                    val targetAccountId = goal.accountId
                    val now = Instant.now()
                    val today = LocalDate.now()
                    
                    val pairId = UUID.randomUUID()

                    val txOut = Transaction(
                        id = UUID.randomUUID(),
                        accountId = sourceAccountId,
                        categoryId = CategoryCatalog.Ids.TransferDefault,
                        type = TransactionType.TRANSFER,
                        amount = record.amount,
                        currency = "CNY",
                        title = "转出-攒钱小荷包",
                        description = "攒钱计划：${goal.name} 打卡",
                        date = today,
                        time = now,
                        linkedTransactionId = pairId
                    )

                    val txIn = Transaction(
                        id = UUID.randomUUID(),
                        accountId = targetAccountId,
                        categoryId = CategoryCatalog.Ids.TransferDefault,
                        type = TransactionType.TRANSFER,
                        amount = record.amount,
                        currency = "CNY",
                        title = "转入-攒钱小荷包",
                        description = "攒钱计划：${goal.name} 打卡",
                        date = today,
                        time = now,
                        linkedTransactionId = pairId
                    )

                    transactionRepository.insertTransaction(txOut)
                    transactionRepository.insertTransaction(txIn)
                }
            }
        }
    }

    fun deleteRecord(record: SavingsRecord) {
        viewModelScope.launch {
            savingsGoalRepository.deleteRecord(record)

            val goal = savingsGoalRepository.getGoalById(record.savingsGoalId).firstOrNull()
            if (goal != null) {
                val description = "攒钱计划：${goal.name} 打卡"
                transactionRepository.deleteTransfersByDescription(record.date, description)
            }
        }
    }
}
