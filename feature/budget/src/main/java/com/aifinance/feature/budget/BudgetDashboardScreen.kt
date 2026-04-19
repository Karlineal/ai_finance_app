package com.aifinance.feature.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.delay

private val ColorLow = Color(0xFF22C55E) // green
private val ColorMedium = Color(0xFFF59E0B) // amber
private val ColorHigh = Color(0xFFEF4444) // red

private val CalendarCellHeight = 34.dp
private val CalendarGridSpacing = 6.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetDashboardScreen(
    onNavigateBack: () -> Unit,
    onAdjustBudget: () -> Unit,
    /** 独立 `budget_dashboard` 路由：确认无方案时跳转初始化（入口内嵌仪表盘传 null） */
    onNoActivePlanNavigateToSetup: (() -> Unit)? = null,
    /** 点击某一天，用于外部打开记账/详情页面（可选） */
    onDayClick: ((LocalDate) -> Unit)? = null,
    /** 长按某一天，用于单独调整该日预算（可选） */
    onDayLongClick: ((LocalDate) -> Unit)? = null,
) {
    val viewModel: BudgetDashboardViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showRecordSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 不在 LaunchedEffect 里自动 navigate：重组帧内跳转会取消协程并损坏 Composer 栈（exitGroup / Stack.pop）。
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("预算管理", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                !uiState.isHydrated -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.plan == null -> {
                    val navigateToSetup = onNoActivePlanNavigateToSetup
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (navigateToSetup != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("暂无预算方案，请先完成初始化向导。")
                                Button(onClick = navigateToSetup) { Text("前往初始化向导") }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("暂无预算方案，请先完成初始化向导。")
                                Button(onClick = onAdjustBudget) { Text("开始初始化") }
                            }
                        }
                    }
                }
                else -> {
                    val plan = requireNotNull(uiState.plan)
                    val monthId = plan.monthId
                    val today = uiState.nowDate
                    val todayInThisMonth = today.year == monthId.year && today.monthValue == monthId.monthValue
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryCard(
                            totalBudget = uiState.totalBudget,
                            spentToDate = uiState.spentToDate,
                            remainingBudget = uiState.remainingBudget,
                            onReopenWizard = onAdjustBudget,
                        )
                        DailyBudgetCard(todayBudget = uiState.todayBudget)
                        CalendarGrid(
                            monthId = monthId,
                            highlightDay = if (todayInThisMonth) today.dayOfMonth else null,
                            onDayClick = { date ->
                                selectedDate = date
                                showRecordSheet = true
                                onDayClick?.invoke(date)
                            },
                            onDayLongClick = { date ->
                                onDayLongClick?.invoke(date)
                            },
                        )
                        CategoryUsageList(categoriesUsage = uiState.categoriesUsage)
                    }
                }
            }
        }

        if (showRecordSheet && selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = { showRecordSheet = false },
                sheetState = sheetState,
            ) {
                DailyRecordSheet(
                    date = selectedDate!!,
                    onClose = { showRecordSheet = false },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalBudget: BigDecimal,
    spentToDate: BigDecimal,
    remainingBudget: BigDecimal,
    onReopenWizard: () -> Unit,
) {
    val progress = if (totalBudget <= BigDecimal.ZERO) 0f else (spentToDate.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f)
    val deficitText = if (totalBudget <= BigDecimal.ZERO && spentToDate > BigDecimal.ZERO) "本月预算为 0（可能由固定支出超出收入导致）" else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("本月总预算", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${formatAmount(totalBudget)}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已用金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${formatAmount(spentToDate)}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("剩余金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${formatAmount(remainingBudget)}", fontWeight = FontWeight.Bold, color = if (remainingBudget < BigDecimal.ZERO) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface)
            }

            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = Color(0xFF2E5FE6))

            deficitText?.let { t ->
                Text(t, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onReopenWizard,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("调整预算")
            }
        }
    }
}

@Composable
private fun DailyBudgetCard(
    todayBudget: com.aifinance.core.model.TodayBudgetResult,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("动态日预算（0:00 刷新）", fontWeight = FontWeight.Bold)
            Text("今日可用预算：¥${formatAmount(todayBudget.todayBudget)}", fontWeight = FontWeight.SemiBold)
            if (todayBudget.deficit > BigDecimal.ZERO) {
                Text("本月已透支 ¥${formatAmount(todayBudget.deficit)}", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            } else {
                Text("剩余金额：¥${formatAmount(todayBudget.remainingBudget)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarGrid(
    monthId: YearMonth,
    highlightDay: Int?,
    onDayClick: ((LocalDate) -> Unit)?,
    onDayLongClick: ((LocalDate) -> Unit)?,
) {
    val daysInMonth = monthId.lengthOfMonth()
    val first = LocalDate.of(monthId.year, monthId.monthValue, 1)
    val offset = ((first.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7 // 以周一为一周起点
    val totalCells = ((offset + daysInMonth + 6) / 7) * 7
    val rowCount = totalCells / 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${monthId.year}年${monthId.monthValue}月", fontWeight = FontWeight.Bold)

            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { d ->
                    Text(d, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }

            // 不可在无限高度 Column 内使用 LazyVerticalGrid，否则会触发布局崩溃（完成向导进入本页即闪退）
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CalendarGridSpacing),
            ) {
                repeat(rowCount) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CalendarGridSpacing),
                    ) {
                        repeat(7) { col ->
                            val idx = row * 7 + col
                            val day = idx - offset + 1
                            val isValid = day in 1..daysInMonth
                            val isHighlight = highlightDay != null && isValid && day == highlightDay
                            val date = if (isValid) {
                                LocalDate.of(monthId.year, monthId.monthValue, day)
                            } else {
                                null
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(CalendarCellHeight)
                                    .background(
                                        color = if (isHighlight) Color(0xFF2E5FE6) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .combinedClickable(
                                        enabled = isValid,
                                        onClick = {
                                            date?.let { d -> onDayClick?.invoke(d) }
                                        },
                                        onLongClick = {
                                            date?.let { d -> onDayLongClick?.invoke(d) }
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isValid) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isHighlight) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRecordSheet(
    date: LocalDate,
    onClose: () -> Unit,
) {
    var tabIndex by remember { mutableStateOf(0) } // 0: 手动记账, 1: AI 记录
    var manualText by remember { mutableStateOf("") }
    var aiInput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiTag by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            delay(1000)
            // 简单示例：始终打上“想法”标签
            aiTag = "想法"
            isAnalyzing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${date.monthValue} 月 ${date.dayOfMonth} 日记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onClose) {
                Text("关闭")
            }
        }

        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("手动记账") },
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("AI 记录") },
            )
        }

        when (tabIndex) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("简单记录当日收支或备注：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("请输入记账内容") },
                    )
                    Button(
                        onClick = { onClose() },
                        enabled = manualText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("保存记录")
                    }
                }
            }
            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("用一句话描述你的花费或心情，交给 AI 来理解和打标签。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("例如：好吃") },
                    )
                    Button(
                        onClick = {
                            if (aiInput.isNotBlank()) {
                                isAnalyzing = true
                                aiTag = null
                            }
                        },
                        enabled = aiInput.isNotBlank() && !isAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (isAnalyzing) "分析中..." else "发送")
                    }

                    if (isAnalyzing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                            Text("AI 分析中，请稍候...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    aiTag?.let { tag ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(tag, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "已根据你的输入自动识别为「$tag」类型记录。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryUsageList(
    categoriesUsage: List<CategoryUsageUi>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("分类预算监控", fontWeight = FontWeight.Bold)

            if (categoriesUsage.isEmpty()) {
                Text("当前 TotalBudget 为 0，暂无分类预算。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                categoriesUsage.forEach { c ->
                    val progress = if (c.budgetAmount <= BigDecimal.ZERO) 0f else c.spentAmount.divide(c.budgetAmount, 4, java.math.RoundingMode.HALF_UP).toFloat()
                    val color = when (c.status) {
                        BudgetUsageStatus.LOW -> ColorLow
                        BudgetUsageStatus.MEDIUM -> ColorMedium
                        BudgetUsageStatus.HIGH -> ColorHigh
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "¥${formatAmount(c.spentAmount)} / ¥${formatAmount(c.budgetAmount)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

private fun formatAmount(amount: BigDecimal): String {
    return DecimalFormat("#,##0.00").format(amount)
}

