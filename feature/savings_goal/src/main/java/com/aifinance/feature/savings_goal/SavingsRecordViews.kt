package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsMethod
import com.aifinance.core.model.SavingsRecord
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.min

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
    val completedPeriods = records.map { it.periodIndex }.toSet()
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
                            .background(Color(0xFFFF9800), RoundedCornerShape(4.dp)),
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
            .height(520.dp),
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
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
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
                        color = if (isCompleted) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
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
    val layout = SavingsPixelShapes.layoutFor(shape, totalPeriods)
    val cellSize = 30.dp
    val gap = 2.dp
    val gridWidth = cellSize * layout.width + gap * (layout.width - 1)
    val gridHeight = cellSize * layout.height + gap * (layout.height - 1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9)),
        ) {
            val density = LocalDensity.current
            val gridWidthPx = with(density) { gridWidth.toPx() }
            val gridHeightPx = with(density) { gridHeight.toPx() }
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }

            val fitScale = min(
                containerWidthPx / gridWidthPx,
                containerHeightPx / gridHeightPx,
            ).coerceIn(0.08f, 1f)

            var scale by rememberSaveable(shape, totalPeriods) { mutableFloatStateOf(fitScale) }
            var offsetX by rememberSaveable(shape, totalPeriods) { mutableFloatStateOf(0f) }
            var offsetY by rememberSaveable(shape, totalPeriods) { mutableFloatStateOf(0f) }

            fun centerAtScale(targetScale: Float) {
                scale = targetScale
                offsetX = (containerWidthPx - gridWidthPx * targetScale) / 2f
                offsetY = (containerHeightPx - gridHeightPx * targetScale) / 2f
            }

            LaunchedEffect(shape, totalPeriods, fitScale, containerWidthPx, containerHeightPx) {
                centerAtScale(fitScale)
            }

            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(0.08f, 3f)
                offsetX += panChange.x
                offsetY += panChange.y
            }

            Column(modifier = Modifier.fillMaxSize()) {
                ZoomControlBar(
                    scale = scale,
                    onZoomIn = {
                        val newScale = (scale * 1.25f).coerceAtMost(3f)
                        centerAtScale(newScale)
                    },
                    onZoomOut = {
                        val newScale = (scale / 1.25f).coerceAtLeast(0.08f)
                        centerAtScale(newScale)
                    },
                    onFitAll = { centerAtScale(fitScale) },
                    onReset = { centerAtScale(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformState),
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .size(gridWidth, gridHeight),
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
                                    amountLabel = periodAmountLabel(goal, cell.periodIndex),
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
    }
}

@Composable
private fun ZoomControlBar(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitAll: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "双指可缩放拖动",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End,
            )
            OutlinedButton(
                onClick = onZoomOut,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Remove, contentDescription = "缩小", modifier = Modifier.size(16.dp))
            }
            OutlinedButton(
                onClick = onFitAll,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text("全图", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onZoomIn,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "放大", modifier = Modifier.size(16.dp))
            }
            OutlinedButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text("1:1", style = MaterialTheme.typography.labelMedium)
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
        onClick != null -> Color.White
        else -> Color(0xFFE8EEF7)
    }
    val textColor = when {
        isCompleted -> Color.White
        onClick != null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .border(0.5.dp, Color(0xFFDCE3ED), RoundedCornerShape(5.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "end",
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ColoringPeriodTile(
    amountLabel: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCompleted) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    val textColor = if (isCompleted) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .border(0.5.dp, Color(0xFFDCE3ED), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = amountLabel,
            color = textColor,
            fontSize = when {
                amountLabel.length > 3 -> 9.sp
                amountLabel.length > 2 -> 10.sp
                else -> 11.sp
            },
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun periodAmountDisplay(goal: SavingsGoal, period: Int): String {
    return "¥${formatMoney(periodAmount(goal, period))}"
}

private fun periodAmountLabel(goal: SavingsGoal, period: Int): String {
    return periodAmount(goal, period).stripTrailingZeros().toPlainString()
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
