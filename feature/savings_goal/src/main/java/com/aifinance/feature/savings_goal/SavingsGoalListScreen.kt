package com.aifinance.feature.savings_goal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import java.math.BigDecimal
import java.text.DecimalFormat

private val QuickAmounts = listOf("10", "50", "100", "500")
private val MoneyFormat = DecimalFormat("#,##0.00")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    if (goals.isEmpty()) {
        SavingsGoalCreateEditScreen(
            goalId = null,
            onSaved = { /* It will automatically transition when goal is saved */ },
            onBack = onBack
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("攒钱计划", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToAddAccount) {
                            Icon(Icons.Default.Add, contentDescription = "添加账户")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.size(4.dp)) }
                
                item {
                    SavingsEntryBanner(onClick = onNavigateToCreate)
                }

                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onClick = { onNavigateToDetail(goal.id.toString()) },
                        onQuickAmount = { amount ->
                            viewModel.adjustSavedAmount(goal.id, BigDecimal(amount))
                        },
                    )
                }
                item { Spacer(Modifier.size(88.dp)) }
            }
        }
    }
}

@Composable
private fun SavingsEntryBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("💰", style = MaterialTheme.typography.headlineSmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "新建攒钱计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "开启攒钱计划，快乐攒钱",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit,
    onQuickAmount: (String) -> Unit,
) {
    val progress = SavingsGoalCalculator.calculateProgress(goal.currentAmount, goal.targetAmount)
    val isOverdue = SavingsGoalCalculator.isOverdue(goal.endDate, goal.status)
    val accentColor = when {
        goal.status == SavingsGoalStatus.COMPLETED -> Color(0xFF22C55E)
        goal.status == SavingsGoalStatus.FAILED || isOverdue -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    val methodText = when (goal.savingsMethod) {
        com.aifinance.core.model.SavingsMethod.WEEKLY_52 -> "52周存钱"
        com.aifinance.core.model.SavingsMethod.DAILY_365 -> "365天存钱"
        com.aifinance.core.model.SavingsMethod.MONTHLY_12 -> "12月存单"
        com.aifinance.core.model.SavingsMethod.FIXED_AMOUNT -> "定额存钱"
        com.aifinance.core.model.SavingsMethod.FLEXIBLE -> "灵活存钱"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularSavingsProgress(
                    progress = progress,
                    centerText = "¥${formatMoney(goal.currentAmount)}",
                    color = accentColor,
                    modifier = Modifier.size(84.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        StatusIcon(goal.status, isOverdue)
                    }
                    Text(
                        "$methodText · 目标 ¥${formatMoney(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        statusText(goal.status, isOverdue, progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                    )
                    Text(
                        "截止 ${goal.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (goal.status == SavingsGoalStatus.ACTIVE) {
                if (goal.savingsMethod == com.aifinance.core.model.SavingsMethod.FLEXIBLE) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickAmounts.forEach { amount ->
                            AssistChip(
                                onClick = { onQuickAmount(amount) },
                                label = { Text("+¥$amount") },
                            )
                        }
                    }
                } else {
                    androidx.compose.material3.Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("去打卡")
                    }
                }
            }
        }
    }
}

@Composable
internal fun CircularSavingsProgress(
    progress: Float,
    centerText: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusIcon(status: SavingsGoalStatus, isOverdue: Boolean) {
    when {
        status == SavingsGoalStatus.COMPLETED -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(18.dp),
        )
        status == SavingsGoalStatus.FAILED || isOverdue -> Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(18.dp),
        )
    }
}

internal fun formatMoney(value: BigDecimal): String = MoneyFormat.format(value)

private fun statusText(status: SavingsGoalStatus, isOverdue: Boolean, progress: Float): String = when {
    status == SavingsGoalStatus.COMPLETED -> "已完成"
    status == SavingsGoalStatus.FAILED -> "已放弃"
    isOverdue -> "已逾期"
    else -> "进度 ${(progress * 100).toInt()}%"
}
