package com.aifinance.feature.savings_goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsMethod
import com.aifinance.core.model.SavingsRecord
import com.kizitonwose.calendar.compose.CalendarLayoutInfo
import com.kizitonwose.calendar.compose.HeatMapCalendar
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapCalendarState
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapWeek
import com.kizitonwose.calendar.compose.heatmapcalendar.rememberHeatMapCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

// ─────────────────────────────────────────────────
//  HeatMap 级别
// ─────────────────────────────────────────────────

private enum class Level {
    EMPTY,
    CHECKED,
    ;

    @Composable
    fun color(): Color = when (this) {
        EMPTY -> if (isSystemInDarkTheme()) Color(0xFF161B22) else Color(0xFFEBEDF0)
        CHECKED -> if (isSystemInDarkTheme()) Color(0xFF39D353) else Color(0xFF40C463)
    }
}

// ─────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInCalendarScreen(
    savingsGoalId: UUID,
    onNavigateBack: () -> Unit,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val goal = remember(goals, savingsGoalId) {
        goals.firstOrNull { it.id == savingsGoalId }
    }

    val records by (
        goal?.let { viewModel.getRecordsForGoal(it.id) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
        )
        .collectAsStateWithLifecycle(emptyList())

    val checkIns = remember(records) { records.associateBy { it.date } }
    val totalDays = remember(goal) {
        goal?.let {
            when (it.savingsMethod) {
                SavingsMethod.WEEKLY_52 -> 52
                SavingsMethod.DAILY_365 -> 365
                SavingsMethod.MONTHLY_12 -> 12
                SavingsMethod.FIXED_AMOUNT -> {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(it.startDate, it.endDate).toInt()
                    days.coerceIn(1, 365 * 5)
                }
                SavingsMethod.FLEXIBLE -> {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(it.startDate, it.endDate).toInt()
                    days.coerceIn(1, 365 * 5)
                }
            }
        } ?: 365
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("存钱记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressSection(
                checkInCount = checkIns.size,
                totalDays = totalDays,
            )

            HeatMapCalendarSection(
                checkIns = checkIns,
                goal = goal,
                onDayClick = { date ->
                    goal?.let { currentGoal ->
                        val existingRecord = checkIns[date]
                        if (existingRecord != null) {
                            viewModel.deleteRecord(existingRecord)
                        } else {
                            val amount = currentGoal.baseAmount ?: BigDecimal.ONE
                            val periodIndex = SavingsGoalCalculator.getCurrentPeriodIndex(
                                startDate = currentGoal.startDate,
                                method = currentGoal.savingsMethod,
                                frequency = currentGoal.frequency,
                            )
                            val record = SavingsRecord(
                                id = UUID.randomUUID(),
                                savingsGoalId = currentGoal.id,
                                amount = amount,
                                date = date,
                                note = null,
                                periodIndex = periodIndex,
                                createdAt = Instant.now(),
                            )
                            viewModel.addRecord(record, null)
                        }
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────
//  Progress Section
// ─────────────────────────────────────────────────

@Composable
private fun ProgressSection(checkInCount: Int, totalDays: Int, modifier: Modifier = Modifier) {
    val progress by remember(checkInCount, totalDays) {
        derivedStateOf {
            if (totalDays > 0) checkInCount.toFloat() / totalDays else 0f
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                        text = "$checkInCount / $totalDays",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────
//  HeatMap Calendar Section
// ─────────────────────────────────────────────────

private val daySize = 18.dp

@Composable
private fun HeatMapCalendarSection(
    checkIns: Map<LocalDate, SavingsRecord>,
    goal: SavingsGoal?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val endDate = remember(goal) {
        goal?.let {
            if (it.status == com.aifinance.core.model.SavingsGoalStatus.COMPLETED) {
                it.endDate
            } else {
                if (today.isBefore(it.endDate)) today else it.endDate
            }
        } ?: today
    }
    val startDate = remember { goal?.startDate ?: endDate.minusMonths(12) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "打卡日历",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            val state = rememberHeatMapCalendarState(
                startMonth = startDate.yearMonth,
                endMonth = endDate.yearMonth,
                firstVisibleMonth = endDate.yearMonth,
                firstDayOfWeek = firstDayOfWeekFromLocale(),
            )

            HeatMapCalendar(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                contentPadding = PaddingValues(end = 6.dp),
                dayContent = { day, week ->
                    HeatMapDay(
                        day = day,
                        startDate = startDate,
                        endDate = endDate,
                        week = week,
                        isChecked = checkIns.containsKey(day.date),
                        onClick = onDayClick,
                    )
                },
                weekHeader = { WeekHeader(it) },
                monthHeader = { MonthHeader(it, endDate, state) },
            )

            HeatMapLegend()
        }
    }
}

// ─────────────────────────────────────────────────
//  Day Cell
// ─────────────────────────────────────────────────

@Composable
private fun HeatMapDay(
    day: CalendarDay,
    startDate: LocalDate,
    endDate: LocalDate,
    week: HeatMapWeek,
    isChecked: Boolean,
    onClick: (LocalDate) -> Unit,
) {
    val weekDates = remember(week) { week.days.map { it.date } }
    if (day.date in startDate..endDate) {
        val level = if (isChecked) Level.CHECKED else Level.EMPTY
        Box(
            modifier = Modifier
                .size(daySize)
                .padding(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(level.color())
                .clickable { onClick(day.date) },
        )
    } else if (weekDates.contains(startDate)) {
        Box(modifier = Modifier.size(daySize))
    }
}

// ─────────────────────────────────────────────────
//  Week Header (一, 三, 五)
// ─────────────────────────────────────────────────

@Composable
private fun WeekHeader(dayOfWeek: DayOfWeek) {
    val label = remember(dayOfWeek) {
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.FRIDAY -> "五"
            else -> ""
        }
    }
    if (label.isNotEmpty()) {
        Box(
            modifier = Modifier
                .height(daySize)
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Spacer(Modifier.height(daySize))
    }
}

// ─────────────────────────────────────────────────
//  Month Header
// ─────────────────────────────────────────────────

@Composable
private fun MonthHeader(calendarMonth: CalendarMonth, endDate: LocalDate, state: HeatMapCalendarState) {
    val density = LocalDensity.current
    val firstFullyVisibleMonth by remember {
        derivedStateOf { getFirstFullyVisibleMonth(state.layoutInfo, daySize, density) }
    }
    if (calendarMonth.weekDays.first().first().date <= endDate) {
        val month = calendarMonth.yearMonth
        val title = if (month == firstFullyVisibleMonth) {
            "${month.year}年${month.monthValue}月"
        } else {
            "${month.monthValue}月"
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp, start = 2.dp),
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────
//  Legend
// ─────────────────────────────────────────────────

@Composable
private fun HeatMapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "未打卡",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Level.entries.forEach { level ->
            Box(
                modifier = Modifier
                    .size(daySize)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(level.color()),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "已打卡",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────

private fun getFirstFullyVisibleMonth(layoutInfo: CalendarLayoutInfo, daySize: Dp, density: Density): YearMonth? {
    val visibleItemsInfo = layoutInfo.visibleMonthsInfo
    return when {
        visibleItemsInfo.isEmpty() -> null
        visibleItemsInfo.count() == 1 -> visibleItemsInfo.first().month.yearMonth
        else -> {
            val firstItem = visibleItemsInfo.first()
            val daySizePx = with(density) { daySize.toPx() }
            if (
                firstItem.size < daySizePx * 3 ||
                (
                    firstItem.offset < layoutInfo.viewportStartOffset &&
                        layoutInfo.viewportStartOffset - firstItem.offset > daySizePx
                    )
            ) {
                visibleItemsInfo[1].month.yearMonth
            } else {
                visibleItemsInfo.first().month.yearMonth
            }
        }
    }
}
