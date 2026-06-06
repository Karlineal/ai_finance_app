package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDetailScreen(
    goalId: String,
    onNavigateToEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val goal = remember(goals, goalId) {
        runCatching { UUID.fromString(goalId) }.getOrNull()?.let { id ->
            goals.firstOrNull { it.id == id }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("计划详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (goal != null) {
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (goal == null) {
            MissingGoal(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            val records by viewModel.getRecordsForGoal(goal.id).collectAsStateWithLifecycle(emptyList())
            GoalDetailContent(
                goal = goal,
                records = records,
                accounts = accounts,
                onDelete = {
                    viewModel.deleteGoal(goal)
                    onBack()
                },
                onMarkCompleted = { viewModel.updateStatus(goal.id, SavingsGoalStatus.COMPLETED) },
                onMarkFailed = { viewModel.updateStatus(goal.id, SavingsGoalStatus.FAILED) },
                onReactivate = { viewModel.updateStatus(goal.id, SavingsGoalStatus.ACTIVE) },
                onAddRecord = { record, sourceId -> viewModel.addRecord(record, sourceId) },
                onDeleteRecord = { viewModel.deleteRecord(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun GoalDetailContent(
    goal: SavingsGoal,
    records: List<SavingsRecord>,
    accounts: List<Account>,
    onDelete: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkFailed: () -> Unit,
    onReactivate: () -> Unit,
    onAddRecord: (SavingsRecord, UUID?) -> Unit,
    onDeleteRecord: (SavingsRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }
    var checkInPeriod by remember { mutableStateOf<Int?>(null) }
    
    val progress = SavingsGoalCalculator.calculateProgress(goal.currentAmount, goal.targetAmount)
    val remaining = goal.targetAmount.subtract(goal.currentAmount).coerceAtLeast(BigDecimal.ZERO)
    val daysRemaining = SavingsGoalCalculator.calculateDaysRemaining(goal.endDate)
    val statusColor = when {
        goal.status == SavingsGoalStatus.COMPLETED -> Color(0xFF22C55E)
        goal.status == SavingsGoalStatus.FAILED || SavingsGoalCalculator.isOverdue(goal.endDate, goal.status) -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    val methodText = when (goal.savingsMethod) {
        SavingsMethod.WEEKLY_52 -> "52周存钱"
        SavingsMethod.DAILY_365 -> "365天存钱"
        SavingsMethod.MONTHLY_12 -> "12月存单"
        SavingsMethod.FIXED_AMOUNT -> "定额存钱"
        SavingsMethod.FLEXIBLE -> "灵活存钱"
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularSavingsProgress(
                    progress = progress,
                    centerText = "${(progress * 100).toInt()}%",
                    color = statusColor,
                    modifier = Modifier.size(132.dp),
                )
                Text(goal.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "已存 ¥${formatMoney(goal.currentAmount)} / ¥${formatMoney(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$methodText · 进度 ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DetailStatCard(
                label = "剩余金额",
                value = "¥${formatMoney(remaining)}",
                modifier = Modifier.weight(1f),
            )
            DetailStatCard(
                label = "剩余天数",
                value = "${daysRemaining}天",
                modifier = Modifier.weight(1f),
            )
        }

        if (goal.status == SavingsGoalStatus.ACTIVE) {
            Button(
                onClick = {
                    checkInPeriod = null
                    showCheckInDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("打卡存入", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("计划操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(onClick = onMarkCompleted, modifier = Modifier.weight(1f)) {
                            Text("标记完成")
                        }
                        OutlinedButton(onClick = onMarkFailed, modifier = Modifier.weight(1f)) {
                            Text("放弃计划")
                        }
                    }
                }
            }
        } else {
            Button(onClick = onReactivate, modifier = Modifier.fillMaxWidth()) {
                Text("重新启用")
            }
        }

        if ((goal.savingsMethod == SavingsMethod.DAILY_365 || goal.savingsMethod == SavingsMethod.WEEKLY_52)
            && goal.status == SavingsGoalStatus.ACTIVE) {
            SavingsRecordSection(
                goal = goal,
                records = records,
                onPeriodClick = { period ->
                    checkInPeriod = period
                    showCheckInDialog = true
                },
            )
        }

        if (records.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("存入记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    records.forEach { record ->
                        RecordItem(record = record, onDelete = { onDeleteRecord(record) })
                    }
                }
            }
        }

        val notes = goal.notes
        if (!notes.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("备注", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("删除计划")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除攒钱计划") },
            text = { Text("删除后无法恢复，确定要删除「${goal.name}」吗？") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    if (showCheckInDialog) {
        CheckInDialog(
            goal = goal,
            accounts = accounts,
            initialPeriod = checkInPeriod,
            completedPeriods = remember(records) { records.map { it.periodIndex }.toSet() },
            onDismiss = {
                showCheckInDialog = false
                checkInPeriod = null
            },
            onConfirm = { date, amount, note, period, sourceAccountId ->
                val record = SavingsRecord(
                    id = UUID.randomUUID(),
                    savingsGoalId = goal.id,
                    amount = amount,
                    date = date,
                    note = note,
                    periodIndex = period,
                    createdAt = Instant.now()
                )
                onAddRecord(record, sourceAccountId)
                showCheckInDialog = false
                checkInPeriod = null
            }
        )
    }
}

@Composable
private fun RecordItem(record: SavingsRecord, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "第${record.periodIndex}期 - ${record.date}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            record.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("+¥${formatMoney(record.amount)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF22C55E))
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(Icons.Default.Delete, contentDescription = "删除记录", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除存入记录") },
            text = { Text("确定要删除这笔 ¥${formatMoney(record.amount)} 的存入记录吗？这也会同时扣除计划里的已存金额。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInDialog(
    goal: SavingsGoal,
    accounts: List<Account>,
    initialPeriod: Int? = null,
    completedPeriods: Set<Int> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, BigDecimal, String?, Int, UUID?) -> Unit,
) {
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var noteText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val availableAccounts = accounts.filter { it.id != goal.accountId && (it.type == AccountType.BANK || it.type == AccountType.DIGITAL_WALLET || it.type == AccountType.CASH || it.type == AccountType.INVESTMENT) }
    var selectedAccountId by remember { mutableStateOf(availableAccounts.firstOrNull()?.id) }
    val selectedAccount = availableAccounts.find { it.id == selectedAccountId }

    val currentPeriod = remember(dateText, initialPeriod) {
        initialPeriod ?: SavingsGoalCalculator.getCurrentPeriodIndex(goal.startDate, goal.savingsMethod, goal.frequency)
    }

    val periodAlreadySaved = currentPeriod in completedPeriods
    
    val defaultAmount = remember(currentPeriod, goal) {
        when (goal.savingsMethod) {
            SavingsMethod.WEEKLY_52 -> SavingsGoalCalculator.calculateWeek52Amount(currentPeriod, goal.baseAmount ?: BigDecimal(10))
            SavingsMethod.DAILY_365 -> SavingsGoalCalculator.calculateDay365Amount(currentPeriod, goal.baseAmount ?: BigDecimal(1))
            SavingsMethod.MONTHLY_12 -> SavingsGoalCalculator.calculateMonth12Amount(currentPeriod, goal.baseAmount ?: BigDecimal(100))
            SavingsMethod.FIXED_AMOUNT -> goal.fixedAmount ?: BigDecimal.ZERO
            SavingsMethod.FLEXIBLE -> BigDecimal.ZERO
        }
    }
    
    var amountText by remember(currentPeriod) { mutableStateOf(if (defaultAmount > BigDecimal.ZERO) defaultAmount.stripTrailingZeros().toPlainString() else "") }
    
    val amount = amountText.toBigDecimalOrNull()
    val amountError = amountText.isNotBlank() && (amount == null || amount <= BigDecimal.ZERO)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打卡存入 (第${currentPeriod}期)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (periodAlreadySaved) {
                    Text(
                        "该期已存入，重复存入将追加一笔记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (initialPeriod != null && (goal.savingsMethod == SavingsMethod.DAILY_365 || goal.savingsMethod == SavingsMethod.WEEKLY_52)) {
                    Text(
                        "本期应存 ¥${formatMoney(defaultAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("打卡日期 (支持补签)") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "无可用账户",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("付款账户") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (availableAccounts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            availableAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.icon} ${acc.name}") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filterAmountInput() },
                    label = { Text("存入金额") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) { { Text("请输入大于 0 的金额") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("备注 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > BigDecimal.ZERO,
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: LocalDate.now()
                    onConfirm(date, amount!!.setScale(2, RoundingMode.HALF_UP), noteText.ifBlank { null }, currentPeriod, selectedAccountId)
                },
            ) { Text("确认存入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
    
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching { LocalDate.parse(dateText) }.getOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dateText = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state)
        }
    }
}

@Composable
private fun DetailStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.filterAmountInput(): String {
    val builder = StringBuilder()
    var hasDot = false
    forEach { char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '.' && !hasDot -> {
                builder.append(char)
                hasDot = true
            }
        }
    }
    return builder.toString()
}

@Composable
private fun MissingGoal(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(42.dp))
            Text("没有找到这个计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AssistChip(onClick = onBack, label = { Text("返回列表") })
        }
    }
}
