package com.aifinance.feature.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.component.DayActivity
import com.aifinance.feature.home.component.RecordHeatMap
import java.time.YearMonth

@Composable
fun HomeSidebarDrawerContent(
    onNavigateToLogin: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateStatistics: () -> Unit,
    onNavigateTransactions: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAssetManagement: () -> Unit,
    onNavigateCategoryManagement: () -> Unit,
    onNavigateScheduledTransaction: () -> Unit = {},
    onNavigateToAllRecords: (java.time.LocalDate) -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    onNavigateToBillImport: () -> Unit = {},
    onNavigateToSavingsGoal: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recordStats by viewModel.recordStats.collectAsStateWithLifecycle()
    val transactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val avatarUri by viewModel.avatarUri.collectAsStateWithLifecycle()
    val currentMonth = remember { YearMonth.now() }
    val monthRecords = remember(transactions, currentMonth) {
        transactions.filter {
            it.date.year == currentMonth.year && it.date.monthValue == currentMonth.monthValue && !it.isPending
        }
    }

    val dayMap = remember(monthRecords) {
        monthRecords.groupBy { it.date.dayOfMonth }
    }

    val monthRecordedDays = remember(dayMap) { dayMap.keys.size }
    val monthTotalRecords = remember(monthRecords) { monthRecords.size }
    val monthActivity = remember(dayMap) {
        dayMap.mapValues { (_, txs) ->
            val hasIncome = txs.any { it.type == TransactionType.INCOME }
            val hasExpense = txs.any { it.type == TransactionType.EXPENSE }
            when {
                hasIncome -> DayActivity.WithIncome
                hasExpense -> DayActivity.ExpenseOnly
                else -> DayActivity.None
            }
        }
    }

    Surface(
        modifier = modifier
            .width(328.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoginHeader(
                isLoggedIn = isLoggedIn,
                avatarUri = avatarUri,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToUserProfile = onNavigateToUserProfile,
            )

            HeatmapCard(
                currentMonth = currentMonth,
                monthActivity = monthActivity,
                monthRecordedDays = monthRecordedDays,
                monthTotalRecords = monthTotalRecords,
                currentStreak = recordStats.currentStreak,
                onDateClick = { day ->
                    val date = java.time.LocalDate.of(currentMonth.year, currentMonth.monthValue, day)
                    onNavigateToAllRecords(date)
                },
            )

            FunctionGridCard(
                onNavigateStatistics = onNavigateStatistics,
                onNavigateTransactions = onNavigateTransactions,
                onNavigateAssetManagement = onNavigateAssetManagement,
                onNavigateCategoryManagement = onNavigateCategoryManagement,
                onNavigateScheduledTransaction = onNavigateScheduledTransaction,
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToBillImport = onNavigateToBillImport,
                onNavigateToSavingsGoal = onNavigateToSavingsGoal,
            )

            SettingEntryCard(onNavigateSettings = onNavigateSettings)
        }
    }
}

@Composable
private fun LoginHeader(
    isLoggedIn: Boolean,
    avatarUri: String,
    onNavigateToLogin: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isLoggedIn) {
                    onNavigateToUserProfile()
                } else {
                    onNavigateToLogin()
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumAvatar(isLoggedIn = isLoggedIn, avatarUri = avatarUri)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isLoggedIn) "用户已登录" else "点击登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isLoggedIn) {
                    Text(
                        text = "账号已绑定",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PremiumAvatar(
    isLoggedIn: Boolean,
    avatarUri: String,
    modifier: Modifier = Modifier,
) {
    val ringGradient = if (isLoggedIn) {
        Brush.linearGradient(listOf(Color(0xFF2E5FE6), Color(0xFF6A8EFF), Color(0xFF9BC0FF)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1F2A44), Color(0xFF324766), Color(0xFF4D6282)))
    }
    val coreGradient = if (isLoggedIn) {
        Brush.radialGradient(listOf(Color(0xFFEEF4FF), Color(0xFFD8E6FF), Color(0xFFB9D0FF)))
    } else {
        Brush.radialGradient(listOf(Color(0xFFE9EEF7), Color(0xFFC5D1E4), Color(0xFFA7B5CD)))
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .background(ringGradient, CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(coreGradient),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoggedIn && avatarUri.isNotEmpty()) {
                AsyncImage(
                    model = Uri.parse(avatarUri),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.26f), CircleShape),
                )
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun HeatmapCard(
    currentMonth: YearMonth,
    monthActivity: Map<Int, DayActivity>,
    monthRecordedDays: Int,
    monthTotalRecords: Int,
    currentStreak: Int,
    onDateClick: (Int) -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecordHeatMap(
                currentMonth = currentMonth,
                monthActivity = monthActivity,
                onDateClick = onDateClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeatmapStatItem("${monthRecordedDays}天", "坚持记录")
                HeatmapStatItem("${monthTotalRecords}条", "总记录")
                HeatmapStatItem("${currentStreak}天", "连续记录")
            }
        }
    }
}

@Composable
private fun HeatmapStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FunctionGridCard(
    onNavigateStatistics: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onNavigateTransactions: () -> Unit,
    onNavigateAssetManagement: () -> Unit,
    onNavigateCategoryManagement: () -> Unit,
    onNavigateScheduledTransaction: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToBillImport: () -> Unit,
    onNavigateToSavingsGoal: () -> Unit,
) {
    val defaultIconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val items = listOf(
        DrawerFunctionItem("图表统计", Icons.Default.PieChart, onNavigateStatistics, defaultIconTint),
        DrawerFunctionItem("资产管理", Icons.Default.CreditCard, onNavigateAssetManagement, defaultIconTint),
        DrawerFunctionItem("预算管理", Icons.Default.AutoGraph, onNavigateToBudget, defaultIconTint),
        DrawerFunctionItem("攒钱计划", Icons.Default.Savings, onNavigateToSavingsGoal, defaultIconTint),
        DrawerFunctionItem("分类管理", Icons.Default.Category, onNavigateCategoryManagement, defaultIconTint),
        DrawerFunctionItem("定时记账", Icons.Default.CalendarMonth, onNavigateScheduledTransaction, defaultIconTint),
        DrawerFunctionItem("账单导入", Icons.Default.UploadFile, onNavigateToBillImport, defaultIconTint),
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "常用功能", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            items.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = item.onClick),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(imageVector = item.icon, contentDescription = null, tint = item.iconTint, modifier = Modifier.size(24.dp))
                            Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingEntryCard(onNavigateSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateSettings),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private data class DrawerFunctionItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val iconTint: Color,
)
