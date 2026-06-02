package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsMethod
import com.aifinance.core.model.SavingsRecord
import java.math.BigDecimal
import java.time.LocalDate

private enum class SavingsRecordViewMode {
    CHECK_IN,
    COLORING,
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
    var coloringShape by rememberSaveable { mutableStateOf(SavingsColoringShape.HEART) }

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
                            SavingsRecordViewMode.CHECK_IN -> SavingsRecordViewMode.COLORING
                            SavingsRecordViewMode.COLORING -> SavingsRecordViewMode.CHECK_IN
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
                        text = if (viewMode == SavingsRecordViewMode.CHECK_IN) "涂色视图" else "打卡视图",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                if (viewMode == SavingsRecordViewMode.COLORING) {
                    OutlinedButton(
                        onClick = {
                            coloringShape = when (coloringShape) {
                                SavingsColoringShape.HEART -> SavingsColoringShape.ALT
                                SavingsColoringShape.ALT -> SavingsColoringShape.HEART
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("换个图", style = MaterialTheme.typography.labelMedium)
                    }
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
                SavingsRecordViewMode.COLORING -> {
                    ColoringShapeView(
                        goal = goal,
                        totalPeriods = totalPeriods,
                        shape = coloringShape,
                        completedPeriods = completedPeriods,
                        onPeriodClick = onPeriodClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInCardListView(
    goal: SavingsGoal,
    totalPeriods: Int,
    completedPeriods: Set<Int>,
    onPeriodClick: (Int) -> Unit,
) {
    val periods = (1..totalPeriods).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 520.dp, max = 4000.dp)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(periods, key = { it }) { period ->
            CheckInPeriodCard(
                amountText = periodAmountDisplay(goal, period),
                dateText = periodDateLabel(goal, period),
                isCompleted = period in completedPeriods,
                onClick = { onPeriodClick(period) },
            )
        }
    }
}

@Composable
private fun CheckInPeriodCard(
    amountText: String,
    dateText: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
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

@Composable
private fun ColoringShapeView(
    goal: SavingsGoal,
    totalPeriods: Int,
    shape: SavingsColoringShape,
    completedPeriods: Set<Int>,
    onPeriodClick: (Int) -> Unit,
) {
    val layout = remember(shape, totalPeriods) { SavingsPixelShapes.layoutFor(shape, totalPeriods) }
    val gap = 2.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // Parent Column padding(16dp) + Card internal padding(16dp) = 32dp × 2 sides = 64dp
    val horizontalPadding = 64.dp
    val availableWidth = screenWidth - horizontalPadding

    // Compute cell size to fit within available width (min 12dp for readability)
    val computedCellSize = (availableWidth - gap * (layout.width - 1)) / layout.width
    val cellSize = computedCellSize.coerceIn(12.dp, 28.dp)

    // Grid dimensions at chosen cell size
    val gridWidthDp = cellSize * layout.width + gap * (layout.width - 1)
    val gridHeightDp = cellSize * layout.height + gap * (layout.height - 1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            // Grid wrapper — centered in container
            Box(
                modifier = Modifier.size(
                    width = gridWidthDp,
                    height = gridHeightDp,
                ),
            ) {
                layout.cells.forEach { cell ->
                    if (cell.isEndMarker) {
                        ColoringEndTile(
                            isCompleted = cell.periodIndex > 0 && cell.periodIndex in completedPeriods,
                            onClick = if (cell.periodIndex > 0) {
                                { onPeriodClick(cell.periodIndex) }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .size(cellSize)
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (cellSize + gap) * cell.col,
                                    y = (cellSize + gap) * cell.row,
                                ),
                        )
                    } else {
                        ColoringPeriodTile(
                            isCompleted = cell.periodIndex in completedPeriods,
                            onClick = { onPeriodClick(cell.periodIndex) },
                            modifier = Modifier
                                .size(cellSize)
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (cellSize + gap) * cell.col,
                                    y = (cellSize + gap) * cell.row,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColoringEndTile(
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        onClick != null -> MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.1f)
    }
    val textColor = when {
        isCompleted -> Color.White
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "end",
            color = textColor,
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ColoringPeriodTile(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCompleted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.18f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
    )
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
