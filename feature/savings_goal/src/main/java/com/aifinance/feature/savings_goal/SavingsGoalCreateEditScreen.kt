package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalCreateEditScreen(
    goalId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val editingGoal = remember(goals, goalId) {
        goalId?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let { id ->
            goals.firstOrNull { it.id == id }
        }
    }
    val isEditing = goalId != null

    var name by rememberSaveable { mutableStateOf("") }
    var targetAmount by rememberSaveable { mutableStateOf("") }
    var currentAmount by rememberSaveable { mutableStateOf("0") }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var didLoad by rememberSaveable(goalId) { mutableStateOf(false) }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editingGoal?.id) {
        if (editingGoal != null && !didLoad) {
            name = editingGoal.name
            targetAmount = editingGoal.targetAmount.stripTrailingZeros().toPlainString()
            currentAmount = editingGoal.currentAmount.stripTrailingZeros().toPlainString()
            startDate = editingGoal.startDate.toString()
            endDate = editingGoal.endDate.toString()
            notes = editingGoal.notes.orEmpty()
            didLoad = true
        }
    }

    val validation = validateForm(
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = startDate,
        endDate = endDate,
        hasAccount = editingGoal != null || accounts.isNotEmpty(),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑计划" else "新建计划", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("计划名称") },
                        singleLine = true,
                        isError = submitAttempted && validation.nameError != null,
                        supportingText = errorText(submitAttempted, validation.nameError),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it.filterAmountInput() },
                        label = { Text("目标金额") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = submitAttempted && validation.targetError != null,
                        supportingText = errorText(submitAttempted, validation.targetError),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = currentAmount,
                        onValueChange = { currentAmount = it.filterAmountInput() },
                        label = { Text("已存金额") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = submitAttempted && validation.currentError != null,
                        supportingText = errorText(submitAttempted, validation.currentError),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("开始日期") },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            singleLine = true,
                            isError = submitAttempted && validation.startDateError != null,
                            supportingText = errorText(submitAttempted, validation.startDateError),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("截止日期") },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            singleLine = true,
                            isError = submitAttempted && validation.endDateError != null,
                            supportingText = errorText(submitAttempted, validation.endDateError),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "日期格式：yyyy-MM-dd",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    validation.accountError?.takeIf { submitAttempted }?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = {
                    submitAttempted = true
                    if (validation.isValid) {
                        val now = Instant.now()
                        val accountId = editingGoal?.accountId ?: accounts.first().id
                        val goal = SavingsGoal(
                            id = editingGoal?.id ?: UUID.randomUUID(),
                            accountId = accountId,
                            name = name.trim(),
                            targetAmount = targetAmount.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                            currentAmount = currentAmount.ifBlank { "0" }.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                            startDate = LocalDate.parse(startDate),
                            endDate = LocalDate.parse(endDate),
                            status = editingGoal?.status ?: SavingsGoalStatus.ACTIVE,
                            notes = notes.trim().ifBlank { null },
                            createdAt = editingGoal?.createdAt ?: now,
                            updatedAt = now,
                        )
                        if (isEditing) {
                            viewModel.updateGoal(goal)
                        } else {
                            viewModel.createGoal(goal)
                        }
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditing) "保存修改" else "创建计划")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun errorText(show: Boolean, message: String?): (@Composable () -> Unit)? {
    return if (show && message != null) {
        { Text(message) }
    } else {
        null
    }
}

private data class SavingsGoalFormValidation(
    val nameError: String? = null,
    val targetError: String? = null,
    val currentError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val accountError: String? = null,
) {
    val isValid: Boolean
        get() = listOf(nameError, targetError, currentError, startDateError, endDateError, accountError).all { it == null }
}

private fun validateForm(
    name: String,
    targetAmount: String,
    currentAmount: String,
    startDate: String,
    endDate: String,
    hasAccount: Boolean,
): SavingsGoalFormValidation {
    val target = targetAmount.toBigDecimalOrNull()
    val current = currentAmount.ifBlank { "0" }.toBigDecimalOrNull()
    val start = startDate.toLocalDateOrNull()
    val end = endDate.toLocalDateOrNull()

    return SavingsGoalFormValidation(
        nameError = if (name.isBlank()) "请输入计划名称" else null,
        targetError = when {
            target == null -> "请输入有效金额"
            target <= BigDecimal.ZERO -> "目标金额必须大于 0"
            else -> null
        },
        currentError = when {
            current == null -> "请输入有效金额"
            current < BigDecimal.ZERO -> "已存金额不能小于 0"
            else -> null
        },
        startDateError = if (start == null) "请输入有效日期" else null,
        endDateError = when {
            end == null -> "请输入有效日期"
            start != null && end.isBefore(start) -> "截止日期不能早于开始日期"
            else -> null
        },
        accountError = if (!hasAccount) "请先创建一个账户，再创建攒钱计划" else null,
    )
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
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
