package com.aifinance.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.BrandPrimaryLight
import com.aifinance.core.designsystem.theme.ExpenseDefault
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.IcokieTheme
import com.aifinance.core.designsystem.theme.IncomeDefault
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.model.AppDateTime
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionType
import androidx.compose.material.icons.filled.ReceiptLong
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordHomeContent(
    onNavigateToAssetManagement: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val spacing = IcokieTheme.spacing

    var showAddTransaction by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now()) }

    val recentTransactions = viewModel.recentTransactions.collectAsStateWithLifecycle()
    val totalBalance = viewModel.totalBalance.collectAsStateWithLifecycle()
    val accountsById = viewModel.accountsById.collectAsStateWithLifecycle()
    val filteredTransactions = remember(recentTransactions.value, selectedMonth) {
        recentTransactions.value.filter {
            it.date.year == selectedMonth.year && it.date.monthValue == selectedMonth.monthValue
        }
    }
    val displayIncome = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.INCOME && !it.isPending }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    }
    val displayExpense = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.EXPENSE && !it.isPending }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    }
    val displayChange = remember(recentTransactions.value, selectedMonth) {
        calculateChangeForMonth(recentTransactions.value, selectedMonth)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransaction = true },
                containerColor = BrandPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "记一笔",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = spacing.pagePadding,
                vertical = spacing.pagePadding
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
        ) {
            item {
                BalanceCard(
                    balance = totalBalance.value.balance,
                    selectedMonth = selectedMonth,
                    assets = totalBalance.value.assets,
                    liabilities = totalBalance.value.liabilities,
                    income = displayIncome,
                    expense = displayExpense,
                    percentageChange = displayChange,
                    onAssetManageClick = onNavigateToAssetManagement,
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyRecentTransactions()
                }
            } else {
                val groupedByDate = filteredTransactions.groupBy { it.date }
                groupedByDate.toSortedMap(compareByDescending<LocalDate> { it }).forEach { (date, dayTransactions) ->
                    item(key = "home-day-$date") {
                        DaySectionHeader(
                            date = date,
                            dayTransactions = dayTransactions,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        )
                    }
                    items(
                        items = dayTransactions.sortedByDescending { it.time },
                        key = { it.id },
                    ) { transaction ->
                        TimelineTransactionRecord(
                            transaction = transaction,
                            accountName = accountsById.value[transaction.accountId]?.name,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
            }
        }

        if (showAddTransaction) {
            AddTransactionBottomSheet(
                onDismiss = { showAddTransaction = false },
                onSuccess = {
                    showAddTransaction = false
                },
                onNavigateToAssetManagement = onNavigateToAssetManagement,
            )
        }
    }
}

@Composable
private fun AIAlertCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    elevation: androidx.compose.ui.unit.Dp,
    alertLevel: AlertLevel = AlertLevel.NORMAL,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    val backgroundColor = when (alertLevel) {
        AlertLevel.NORMAL -> BrandPrimary
        AlertLevel.WARNING -> Color(0xFFF59E0B)
        AlertLevel.CRITICAL -> ExpenseDefault
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OnPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "AI Alert",
                        tint = OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = IcokieTextStyles.titleMedium,
                        color = OnPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.elementSpacing))
                    Text(
                        text = subtitle,
                        style = IcokieTextStyles.labelMedium,
                        color = OnPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "查看详情",
                tint = OnPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BalanceCard(
    balance: BigDecimal,
    selectedMonth: LocalDate,
    assets: BigDecimal,
    liabilities: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    percentageChange: PercentageChange,
    onAssetManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing
    val elevation = IcokieTheme.elevation
    var hideAmount by remember { mutableStateOf(false) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

    fun secureMoney(value: BigDecimal): String {
        val amountText = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        return if (hideAmount) "******" else "¥$amountText"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = SurfacePrimary
        ),
        shape = RoundedCornerShape(26.dp),
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val glassGradient = if (page == 0) {
                Brush.linearGradient(listOf(Color(0xFFF5E6A1), Color(0xFFF0D773), Color(0xFFEBCB5D)))
            } else {
                Brush.linearGradient(listOf(Color(0xFF3E6DFF), Color(0xFF5684FF), Color(0xFF6EA6FF)))
            }
            val titleColor = if (page == 0) Color(0xFF6E560A) else Color(0xFFEAF2FF)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glassGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                    .padding(spacing.cardPadding),
                horizontalAlignment = Alignment.Start,
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
                        Text(
                            text = if (page == 0) "净资产" else "${selectedMonth.monthValue}月支出",
                            style = IcokieTextStyles.labelMedium,
                            color = titleColor,
                        )
                        Text(
                            text = if (hideAmount) "🙈" else "👁️",
                            modifier = Modifier.clickable { hideAmount = !hideAmount },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.26f))
                            .clickable(onClick = onAssetManageClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = "💳", style = IcokieTextStyles.labelSmall)
                        Text(text = "资产管理", style = IcokieTextStyles.labelSmall, color = titleColor)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.itemSpacing))

                if (page == 0) {
                    Text(
                        text = secureMoney(balance),
                        style = IcokieTextStyles.headlineLarge,
                        color = titleColor,
                    )
                    Spacer(modifier = Modifier.height(spacing.cardSpacing))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "资产", style = IcokieTextStyles.labelSmall, color = titleColor.copy(alpha = 0.7f))
                            Text(text = secureMoney(assets), style = IcokieTextStyles.titleMedium, color = titleColor)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "负债", style = IcokieTextStyles.labelSmall, color = titleColor.copy(alpha = 0.7f))
                            Text(text = secureMoney(liabilities), style = IcokieTextStyles.titleMedium, color = titleColor)
                        }
                    }
                } else {
                    val monthlyBalance = income - expense
                    Text(
                        text = secureMoney(expense),
                        style = IcokieTextStyles.headlineLarge,
                        color = titleColor,
                    )
                    Spacer(modifier = Modifier.height(spacing.cardSpacing))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "收入", style = IcokieTextStyles.labelSmall, color = titleColor.copy(alpha = 0.8f))
                            Text(text = secureMoney(income), style = IcokieTextStyles.titleMedium, color = titleColor)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "结余", style = IcokieTextStyles.labelSmall, color = titleColor.copy(alpha = 0.8f))
                            Text(text = secureMoney(monthlyBalance), style = IcokieTextStyles.titleMedium, color = titleColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = if (page == 0) 0.30f else 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (percentageChange.isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Trend",
                            tint = if (percentageChange.isPositive) IncomeDefault else ExpenseDefault,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${if (percentageChange.isPositive) "+" else ""}${percentageChange.percentage.setScale(1, RoundingMode.HALF_UP)}% 较上月",
                            style = IcokieTextStyles.labelMedium,
                            color = if (page == 0) Color(0xFF2B6A34) else Color.White,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = 16.dp, height = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (pagerState.currentPage == index) OnSurfacePrimary else OnSurfaceTertiary.copy(alpha = 0.4f)),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "记一笔",
                style = IcokieTextStyles.labelMedium
            )
        }

        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OnSurfacePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "导入账单",
                style = IcokieTextStyles.labelMedium
            )
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    income: BigDecimal,
    expense: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing
    val elevation = IcokieTheme.elevation

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
        ) {
            Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = SurfacePrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(IncomeDefault)
                    )
                    Text(
                        text = "本月收入",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.itemSpacing))
                Text(
                    text = "+$currency ${income.toPlainString()}",
                    style = IcokieTextStyles.titleLarge,
                    color = IncomeDefault
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = SurfacePrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ExpenseDefault)
                    )
                    Text(
                        text = "本月支出",
                        style = IcokieTextStyles.labelSmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.itemSpacing))
                Text(
                    text = "-$currency ${expense.toPlainString()}",
                    style = IcokieTextStyles.titleLarge,
                    color = ExpenseDefault
                )
            }
        }
    }
}

@Composable
private fun DaySectionHeader(
    date: LocalDate,
    dayTransactions: List<Transaction>,
    modifier: Modifier = Modifier,
) {
    val expense = dayTransactions
        .filter { it.type == TransactionType.EXPENSE && !it.isPending }
        .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    val income = dayTransactions
        .filter { it.type == TransactionType.INCOME && !it.isPending }
        .fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("M月d日（E）", Locale.CHINA)),
            style = IcokieTextStyles.titleMedium,
            color = OnSurfacePrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "支出¥${expense.pretty()} | 收入¥${income.pretty()}",
            style = IcokieTextStyles.labelMedium,
            color = OnSurfaceSecondary,
        )
    }
}

@Composable
private fun TimelineTransactionRecord(
    transaction: Transaction,
    accountName: String?,
    modifier: Modifier = Modifier,
) {
    val visual = transaction.resolveCategoryVisual()
    val remark = transaction.description?.takeIf { it.isNotBlank() } ?: transaction.title

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = transaction.time.toClockText(),
            style = IcokieTextStyles.labelMedium,
            color = OnSurfaceSecondary,
            modifier = Modifier
                .width(42.dp)
                .padding(top = 2.dp),
        )

        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFD5DEEC)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(visual.amountColor.copy(alpha = 0.45f)),
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(visual.chipBackground)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = visual.label, style = IcokieTextStyles.labelMedium, color = visual.chipText)
                    Text(text = visual.emoji)
                }

                Text(
                    text = transaction.prettyAmount(),
                    style = IcokieTextStyles.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = visual.amountColor,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F4F8))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = remark,
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = accountName ?: "未选择账户",
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = OnSurfaceSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "系统定位",
                    style = IcokieTextStyles.labelMedium,
                    color = OnSurfaceSecondary,
                )
            }
        }
    }
}

private data class TimelineCategoryVisual(
    val label: String,
    val emoji: String,
    val chipBackground: Color,
    val chipText: Color,
    val amountColor: Color,
)

private val CategoryFoodId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")
private val CategoryShoppingId = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")
private val CategoryTransportId = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333")
private val CategoryIncomeId = java.util.UUID.fromString("44444444-4444-4444-4444-444444444444")

private fun Transaction.resolveCategoryVisual(): TimelineCategoryVisual {
    return when {
        categoryId == CategoryFoodId -> TimelineCategoryVisual(
            label = "餐饮",
            emoji = "🍜",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryShoppingId -> TimelineCategoryVisual(
            label = "购物",
            emoji = "🛍️",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryTransportId -> TimelineCategoryVisual(
            label = "交通",
            emoji = "🚗",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )

        categoryId == CategoryIncomeId || type == TransactionType.INCOME -> TimelineCategoryVisual(
            label = "收入",
            emoji = "📦",
            chipBackground = Color(0xFFFCE8D4),
            chipText = Color(0xFFB85B06),
            amountColor = Color(0xFFB85B06),
        )

        type == TransactionType.TRANSFER -> TimelineCategoryVisual(
            label = "转账",
            emoji = "💸",
            chipBackground = Color(0xFFE9EEF8),
            chipText = Color(0xFF54627A),
            amountColor = Color(0xFF2B3345),
        )

        else -> TimelineCategoryVisual(
            label = "支出",
            emoji = "🧾",
            chipBackground = Color(0xFFE5EEFF),
            chipText = Color(0xFF2F67DE),
            amountColor = Color(0xFF2F67DE),
        )
    }
}

private fun Transaction.prettyAmount(): String {
    val value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    return when (type) {
        TransactionType.INCOME -> "+¥$value"
        TransactionType.EXPENSE -> "-¥$value"
        TransactionType.TRANSFER -> "¥$value"
    }
}

private fun BigDecimal.pretty(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun java.time.Instant.toClockText(): String {
    return atZone(AppDateTime.zoneId)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun calculateChangeForMonth(
    transactions: List<Transaction>,
    selectedMonth: LocalDate,
): PercentageChange {
    val currentMonthTotal = transactions
        .filter {
            it.date.year == selectedMonth.year &&
                it.date.monthValue == selectedMonth.monthValue &&
                !it.isPending
        }
        .fold(BigDecimal.ZERO) { sum, item ->
            when (item.type) {
                TransactionType.INCOME -> sum + item.amount
                TransactionType.EXPENSE -> sum - item.amount
                TransactionType.TRANSFER -> sum
            }
        }

    val previousMonth = selectedMonth.minusMonths(1)
    val previousMonthTotal = transactions
        .filter {
            it.date.year == previousMonth.year &&
                it.date.monthValue == previousMonth.monthValue &&
                !it.isPending
        }
        .fold(BigDecimal.ZERO) { sum, item ->
            when (item.type) {
                TransactionType.INCOME -> sum + item.amount
                TransactionType.EXPENSE -> sum - item.amount
                TransactionType.TRANSFER -> sum
            }
        }

    if (previousMonthTotal.compareTo(BigDecimal.ZERO) == 0) {
        return PercentageChange(BigDecimal.ZERO, isPositive = true)
    }

    val diff = currentMonthTotal - previousMonthTotal
    val pct = diff
        .divide(previousMonthTotal.abs(), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))

    return PercentageChange(percentage = pct, isPositive = pct >= BigDecimal.ZERO)
}

@Composable
private fun EmptyRecentTransactions(
    modifier: Modifier = Modifier
) {
    val spacing = IcokieTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sectionSpacing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = OnSurfaceTertiary
            )
            Text(
                text = "暂无交易记录",
                style = IcokieTextStyles.bodyLarge,
                color = OnSurfaceSecondary
            )
            Text(
                text = "点击「记一笔」添加第一笔交易",
                style = IcokieTextStyles.labelSmall,
                color = OnSurfaceTertiary
            )
        }
    }
}

private val InfoBackground = Color(0xFFDBEAFE)
private val InfoDefault = Color(0xFF3B82F6)
private val ExpenseBackground = Color(0xFFFEE2E2)
private val IncomeBackground = Color(0xFFD1FAE5)
private val SurfaceSecondary = Color(0xFFF8FAFC)

@Composable
private fun MonthSelector(
    modifier: Modifier = Modifier,
    selectedMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "上个月",
                tint = OnSurfacePrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${selectedMonth.monthValue}月",
                style = IcokieTextStyles.titleMedium,
                color = OnSurfacePrimary
            )
            Text(
                text = "${selectedMonth.year}年",
                style = IcokieTextStyles.labelSmall,
                color = OnSurfaceSecondary
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "下个月",
                tint = OnSurfacePrimary
            )
        }
    }
}
