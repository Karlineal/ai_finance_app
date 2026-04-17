package com.aifinance.feature.scheduled

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.OnSurfaceTertiary
import com.aifinance.core.designsystem.theme.SurfacePrimary
import com.aifinance.core.model.Category
import com.aifinance.core.model.CategoryCatalog
import com.aifinance.core.model.ScheduledRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTransactionListScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: ScheduledTransactionViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPermissionBanner by remember {
        mutableStateOf(ExactAlarmPermissionHelper.shouldShowPermissionGuidance(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showPermissionBanner = ExactAlarmPermissionHelper.shouldShowPermissionGuidance(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = SurfacePrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "定时记账",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OnSurfaceSecondary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAdd) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加定时记账",
                            tint = BrandPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePrimary,
                    titleContentColor = OnSurfacePrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(padding),
        ) {
            if (rules.isEmpty()) {
                ScheduledEmptyState(onAddClick = onNavigateToAdd)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showPermissionBanner) {
                        item {
                            ExactAlarmPermissionBanner(
                                onOpenSettings = { ExactAlarmPermissionHelper.openAlarmSettings(context) }
                            )
                        }
                    }
                    items(rules, key = { it.id }) { rule ->
                        val category = resolveCategory(rule, allCategories)
                        ScheduledRuleListCard(
                            rule = rule,
                            category = category,
                            onToggleEnabled = {
                                viewModel.setRuleEnabled(rule, !rule.enabled)
                            },
                            onDelete = { viewModel.deleteRule(rule.id) },
                        )
                    }
                    item {
                        Text(
                            text = "- 到底了 -",
                            style = IcokieTextStyles.labelSmall,
                            color = OnSurfaceTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun resolveCategory(rule: ScheduledRule, all: List<Category>): Category? {
    val id = rule.categoryId ?: return null
    return all.find { it.id == id }
        ?: CategoryCatalog.findById(id)?.asCategory()
}

@Composable
private fun ExactAlarmPermissionBanner(
    onOpenSettings: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3CD)
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "⚠️ 权限提醒",
                style = IcokieTextStyles.bodyLarge,
                color = Color(0xFF856404),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "定时记账需要精确闹钟权限才能在设定时间准时触发。当前权限未开启，定时记账可能会延迟或无法触发。",
                style = IcokieTextStyles.bodyMedium,
                color = Color(0xFF856404),
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF856404),
                ),
            ) {
                Text(
                    text = "去设置开启权限",
                    color = Color.White,
                    style = IcokieTextStyles.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ScheduledEmptyState(
    onAddClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "工资、住房…每月重复记账很麻烦？",
            style = IcokieTextStyles.bodyLarge,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "固定周期的收支，自动帮你记～",
            style = IcokieTextStyles.bodyMedium,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = OnPrimary,
                )
                Text(
                    text = "添加",
                    color = OnPrimary,
                    style = IcokieTextStyles.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
