package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.util.UUID

@Composable
fun SavingsGoalDetailScreen(
    goalId: String,
    onNavigateToEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
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
            GoalDetailContent(
                goal = goal,
                onBack = onBack,
                onDelete = {
                    viewModel.deleteGoal(goal)
                    onBack()
                },
                onMarkCompleted = { viewModel.updateStatus(goal.id, SavingsGoalStatus.COMPLETED) },
                onMarkFailed = { viewModel.updateStatus(goal.id, SavingsGoalStatus.FAILED) },
                onReactivate = { viewModel.updateStatus(goal.id, SavingsGoalStatus.ACTIVE) },
                onAdjust = { viewModel.adjustSavedAmount(goal.id, it) },
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
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkFailed: () -> Unit,
    onReactivate: () -> Unit,
    onAdjust: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val progress = SavingsGoalCalculator.calculateProgress(goal.currentAmount, goal.targetAmount)
    val remaining = goal.targetAmount.subtract(goal.currentAmount).coerceAtLeast(BigDecimal.ZERO)
    val daysRemaining = SavingsGoalCalculator.calculateDaysRemaining(goal.endDate)
    val dailySuggestion = SavingsGoalCalculator.calculateDailySuggestion(goal.currentAmount, goal.targetAmount, goal.endDate)
    val weeklySuggestion = SavingsGoalCalculator.calculateWeeklySuggestion(goal.currentAmount, goal.targetAmount, goal.endDate)
    val statusColor = when {
        goal.status == SavingsGoalStatus.COMPLETED -> Color(0xFF22C55E)
        goal.status == SavingsGoalStatus.FAILED || SavingsGoalCalculator.isOverdue(goal.endDate, goal.status) -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("存款建议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionRow("每天建议", "¥${formatMoney(dailySuggestion)}")
                SuggestionRow("每周建议", "¥${formatMoney(weeklySuggestion)}")
                SuggestionRow("计划周期", "${goal.startDate} 至 ${goal.endDate}")
            }
        }

        if (goal.status == SavingsGoalStatus.ACTIVE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("快捷存入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("10", "50", "100", "500").forEach { amount ->
                            AssistChip(onClick = { onAdjust(BigDecimal(amount)) }, label = { Text("+¥$amount") })
                        }
                    }
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

@Composable
private fun SuggestionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
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
