package com.aifinance.feature.savings_goal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsMethod
import com.aifinance.core.model.SavingsRecord
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private enum class SavingsRecordViewMode {
    CHECK_IN,
    HEAT_MAP,
}

@Composable
fun SavingsRecordSection(
    goal: SavingsGoal,
    records: List<SavingsRecord>,
    onPeriodClick: (periodIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (goal.savingsMethod != SavingsMethod.DAILY_365 && goal.savingsMethod != SavingsMethod.WEEKLY_52) {
        return
    }

    val totalPeriods = when (goal.savingsMethod) {
        SavingsMethod.DAILY_365 -> 365
        SavingsMethod.WEEKLY_52 -> 52
        else -> return
    }
    val completedPeriods = remember(records) { records.map { it.periodIndex }.toSet() }
    val completedCount = (1..totalPeriods).count { it in completedPeriods }

    var viewMode by rememberSaveable { mutableStateOf(SavingsRecordViewMode.CHECK_IN) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "存钱记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                    )
                }
                OutlinedButton(
                    onClick = {
                        viewMode = when (viewMode) {
                            SavingsRecordViewMode.CHECK_IN -> SavingsRecordViewMode.HEAT_MAP
                            SavingsRecordViewMode.HEAT_MAP -> SavingsRecordViewMode.CHECK_IN
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (viewMode == SavingsRecordViewMode.CHECK_IN) "日历视图" else "列表视图",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)),
                    )
                    Text(
                        text = "$completedCount / $totalPeriods",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            when (viewMode) {
                SavingsRecordViewMode.CHECK_IN -> {
                    CheckInCardListView(
                        goal = goal,
                        totalPeriods = totalPeriods,
                        completedPeriods = completedPeriods,
                        onPeriodClick = onPeriodClick,
                    )
                }
                SavingsRecordViewMode.HEAT_MAP -> {
                    YearHeatMapView(
                        goal = goal,
                        totalPeriods = totalPeriods,
                        completedPeriods = completedPeriods,
                        onPeriodClick = onPeriodClick,
                    )
                }
            }
        }
    }
}

/**
 * 打卡卡片列表视图
 * 使用非懒加载 Column + Row 布局，避免与外层 verticalScroll 产生嵌套滚动冲突
 */
@Composable
private fun CheckInCardListView(
    goal: SavingsGoal,
    totalPeriods: Int,
    completedPeriods: Set<Int>,
    onPeriodClick: (Int) -> Unit,
) {
    val periods = (1..totalPeriods).toList()
    val chunked = periods.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { period ->
                    Box(modifier = Modifier.weight(1f)) {
                        CheckInPeriodCard(
                            amountText = periodAmountDisplay(goal, period),
                            dateText = periodDateLabel(goal, period),
                            isCompleted = period in completedPeriods,
                            onClick = { onPeriodClick(period) },
                        )
                    }
                }
                // 奇数个时填充空白
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CheckInPeriodCard(amountText: String, dateText: String, isCompleted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * GitHub 风格年度热力图视图
 *
 * DAILY_365: 行=星期(一~日), 列=周数, 类似 GitHub Contribution Graph
 * WEEKLY_52: 简单网格布局, 每格代表一周, 显示周序号
 */
@Composable
private fun YearHeatMapView(
    goal: SavingsGoal,
    totalPeriods: Int,
    completedPeriods: Set<Int>,
    onPeriodClick: (Int) -> Unit,
) {
    if (goal.savingsMethod == SavingsMethod.WEEKLY_52) {
        Weekly52HeatMapView(
            totalPeriods = totalPeriods,
            completedPeriods = completedPeriods,
            onPeriodClick = onPeriodClick,
        )
    } else {
        Daily365HeatMapView(
            goal = goal,
            totalPeriods = totalPeriods,
            completedPeriods = completedPeriods,
            onPeriodClick = onPeriodClick,
        )
    }
}

/**
 * 52周存钱法热力图
 * 简单网格布局, 7行 x 8列 = 56格 (52周 + 4空白)
 * 每格显示周序号, 已打卡的格子高亮
 */
@Composable
private fun Weekly52HeatMapView(totalPeriods: Int, completedPeriods: Set<Int>, onPeriodClick: (Int) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val columns = 8
    val rows = (totalPeriods + columns - 1) / columns // ceil(52/8) = 7

    val cellSize = 36.dp
    val gap = 4.dp
    val emptyColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFEBEDF0)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                for (col in 0 until columns) {
                    val period = row * columns + col + 1
                    if (period <= totalPeriods) {
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (period in completedPeriods) {
                                        primaryColor
                                    } else {
                                        emptyColor
                                    },
                                )
                                .clickable { onPeriodClick(period) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$period",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.sp,
                                fontWeight = if (period in completedPeriods) FontWeight.Bold else FontWeight.Normal,
                                color = if (period in completedPeriods) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        // 空白占位
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

/**
 * 365天存钱法热力图
 * GitHub 风格: 行=星期(一~日), 列=周数
 */
@Composable
private fun Daily365HeatMapView(
    goal: SavingsGoal,
    totalPeriods: Int,
    completedPeriods: Set<Int>,
    onPeriodClick: (Int) -> Unit,
) {
    val startDate = goal.startDate
    val isDark = isSystemInDarkTheme()

    // --- 网格布局计算 ---
    // 将 startDate 对齐到所在周的周一作为网格起点
    val gridStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endDate = periodDate(goal, totalPeriods)
    // 将 endDate 对齐到所在周的周日作为网格终点
    val gridEnd = endDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val totalGridDays = (ChronoUnit.DAYS.between(gridStart, gridEnd) + 1).toInt()
    val numWeeks = (totalGridDays + 6) / 7

    // 构建 (周索引, 星期索引) -> periodIndex 的映射
    // 星期索引: 0=周一, 1=周二, ..., 6=周日
    val periodMap = remember(totalPeriods, startDate) {
        buildMap {
            for (p in 1..totalPeriods) {
                val date = periodDate(goal, p)
                val offset = ChronoUnit.DAYS.between(gridStart, date).toInt()
                val weekIdx = offset / 7
                val dayIdx = offset % 7
                put(weekIdx to dayIdx, p)
            }
        }
    }

    // --- 尺寸计算 ---
    val cellSize = 12.dp
    val gap = 3.dp
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val stepPx = cellSizePx + gapPx
    val canvasWidthDp = cellSize * numWeeks + gap * (numWeeks - 1).coerceAtLeast(0)
    val canvasHeightDp = cellSize * 7 + gap * 6

    // --- 颜色 ---
    val emptyColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFEBEDF0)
    val primaryColor = MaterialTheme.colorScheme.primary

    // --- 月份标签 ---
    val monthLabels = remember(totalPeriods, startDate, gridStart) {
        buildList {
            var lastMonth = -1
            for (w in 0 until numWeeks) {
                val weekStartDate = gridStart.plusDays((w * 7).toLong())
                val month = weekStartDate.monthValue
                if (month != lastMonth) {
                    add(w to "${month}月")
                    lastMonth = month
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 月份标签行
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            val labelPaint = remember(isDark) {
                android.graphics.Paint().apply {
                    textSize = 24f
                    color = if (isDark) {
                        android.graphics.Color.parseColor("#AAAAAA")
                    } else {
                        android.graphics.Color.parseColor("#888888")
                    }
                    isAntiAlias = true
                }
            }
            Canvas(
                modifier = Modifier
                    .width(canvasWidthDp)
                    .height(14.dp),
            ) {
                monthLabels.forEach { (weekIdx, label) ->
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        weekIdx * stepPx,
                        12f,
                        labelPaint,
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // 星期标签列 (只显示 一、三、五)
            Column(
                modifier = Modifier.width(20.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                val dayLabels = listOf("一", "", "三", "", "五", "", "")
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(cellSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 热力图网格 (横向滚动)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA))
                    .padding(4.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .width(canvasWidthDp)
                        .height(canvasHeightDp)
                        .pointerInput(completedPeriods, periodMap) {
                            detectTapGestures { offset ->
                                val col = (offset.x / stepPx).toInt()
                                val row = (offset.y / stepPx).toInt()
                                if (col in 0 until numWeeks && row in 0 until 7) {
                                    periodMap[col to row]?.let { onPeriodClick(it) }
                                }
                            }
                        },
                ) {
                    for (w in 0 until numWeeks) {
                        for (d in 0 until 7) {
                            val period = periodMap[w to d] ?: continue
                            val x = w * stepPx
                            val y = d * stepPx
                            drawRoundRect(
                                color = if (period in completedPeriods) primaryColor else emptyColor,
                                topLeft = Offset(x, y),
                                size = Size(cellSizePx, cellSizePx),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                }
            }
        }

        // 图例
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(emptyColor),
                )
                Text(
                    text = "未打卡",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primaryColor),
                )
                Text(
                    text = "已打卡",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun periodAmountDisplay(goal: SavingsGoal, period: Int): String {
    return "¥${formatMoney(periodAmount(goal, period))}"
}

private fun periodAmount(goal: SavingsGoal, period: Int): BigDecimal {
    val base = goal.baseAmount ?: BigDecimal.ONE
    return when (goal.savingsMethod) {
        SavingsMethod.DAILY_365 -> SavingsGoalCalculator.calculateDay365Amount(period, base)
        SavingsMethod.WEEKLY_52 -> SavingsGoalCalculator.calculateWeek52Amount(period, base)
        else -> BigDecimal.ZERO
    }
}

private fun periodDateLabel(goal: SavingsGoal, period: Int): String {
    val date = periodDate(goal, period)
    return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
}

private fun periodDate(goal: SavingsGoal, period: Int): LocalDate {
    return when (goal.savingsMethod) {
        SavingsMethod.DAILY_365 -> goal.startDate.plusDays((period - 1).toLong())
        SavingsMethod.WEEKLY_52 -> goal.startDate.plusWeeks((period - 1).toLong())
        else -> goal.startDate
    }
}
