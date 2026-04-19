package com.aifinance.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.UUID
import com.aifinance.core.model.UserRole
import kotlinx.coroutines.launch

private val ErrorRed = Color(0xFFFF4D4F)
private val PrimaryBlue = Color(0xFF2E5FE6)
private val SavingsYellow = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetWizardScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val viewModel: BudgetWizardViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            navigationIcon = {
                if (uiState.stepIndex > 1) {
                    TextButton(onClick = { viewModel.goToPreviousStep() }) {
                        Text("返回", color = PrimaryBlue)
                    }
                } else {
                    TextButton(onClick = onBack) { Text("关闭", color = PrimaryBlue) }
                }
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (uiState.stepIndex) {
                    1 -> RoleSelectionStep(
                        selectedRole = uiState.role,
                        onSelect = { role ->
                            viewModel.setRole(role)
                            viewModel.goToNextStep()
                        },
                    )
                    2 -> IncomeInputStep(
                        totalIncomeText = uiState.totalIncomeText,
                        totalIncome = uiState.totalIncome,
                        onTextChange = { viewModel.setTotalIncomeText(it) },
                        onQuickIncome = { value -> viewModel.setTotalIncomeText(value) },
                    )
                    3 -> FixedExpenseStep(
                        fixedExpensesMonthly = uiState.fixedExpensesMonthly,
                        fixedExpensesDrafts = uiState.fixedExpensesDrafts,
                        onAddTemplate = { name -> viewModel.addFixedExpenseTemplate(name) },
                        onAmountChange = { id, text -> viewModel.setFixedExpenseAmount(id, text) },
                        onRemove = { id -> viewModel.removeFixedExpense(id) },
                    )
                    4 -> RatioStep(
                        budgetRatio = uiState.budgetRatio,
                        totalBudget = uiState.totalBudget,
                        savingsAllocation = uiState.savingsAllocation,
                        disposableFund = uiState.disposableFund,
                        onRatioChange = { viewModel.setBudgetRatio(it) },
                    )
                    5 -> CategoryBreakdownStep(
                        totalBudget = uiState.totalBudget,
                        unallocatedPool = uiState.unallocatedPool,
                        allocations = uiState.categoryAllocations,
                        errorMessage = uiState.errorMessage,
                        onMinusPlus = { categoryId, delta ->
                            viewModel.adjustCategoryAmount(categoryId, delta)
                        },
                        onReset = { viewModel.resetCategoryToRecommendation() },
                        onAverage = { viewModel.allocateAverage() },
                        onAddCategory = { categoryId ->
                            viewModel.addCategoryFromCatalog(categoryId)
                        },
                    )
                }
            }

            BottomActions(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                stepIndex = uiState.stepIndex,
                totalBudget = uiState.totalBudget,
                role = uiState.role,
                totalIncomeText = uiState.totalIncomeText,
                onNext = {
                    if (uiState.stepIndex == 4) {
                        if (uiState.totalBudget <= BigDecimal.ZERO) {
                            viewModel.confirmAndComplete { ok ->
                                if (ok) onCompleted()
                            }
                            return@BottomActions
                        }
                    }

                    if (uiState.stepIndex == 5) {
                        viewModel.confirmAndComplete { ok ->
                            if (ok) onCompleted()
                        }
                        return@BottomActions
                    }

                    viewModel.goToNextStep()
                },
            )
        }
    }
}

@Composable
private fun RoleSelectionStep(
    selectedRole: UserRole?,
    onSelect: (UserRole) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "你目前的身份是？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        RoleOption(label = "学生", role = UserRole.STUDENT, selectedRole = selectedRole, onSelect = onSelect)
        RoleOption(label = "上班族", role = UserRole.EMPLOYEE, selectedRole = selectedRole, onSelect = onSelect)
        RoleOption(label = "自由职业者", role = UserRole.FREELANCER, selectedRole = selectedRole, onSelect = onSelect)
        RoleOption(label = "其他", role = UserRole.OTHER, selectedRole = selectedRole, onSelect = onSelect)
    }
}

@Composable
private fun RoleOption(
    label: String,
    role: UserRole,
    selectedRole: UserRole?,
    onSelect: (UserRole) -> Unit,
) {
    val selected = selectedRole == role
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(role) },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RadioButton(
                selected = selected,
                onClick = { onSelect(role) },
            )
        }
    }
}

@Composable
private fun IncomeInputStep(
    totalIncomeText: String,
    totalIncome: BigDecimal,
    onTextChange: (String) -> Unit,
    onQuickIncome: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "你每月的总收入/生活费是多少？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        // 中央大金额展示（解析后的实际金额）
        Text(
            text = "¥${formatAmount(totalIncome)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
        )
        OutlinedTextField(
            value = totalIncomeText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("金额（元）") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { onQuickIncome("3000") }) { Text("3000") }
            TextButton(onClick = { onQuickIncome("5000") }) { Text("5000") }
            TextButton(onClick = { onQuickIncome("8000") }) { Text("8000") }
        }

        // 简化版刻度滑动：0 ~ 20,000 区间，步长约 100
        val maxIncome = 20_000f
        val sliderValue = (totalIncome.coerceAtLeast(BigDecimal.ZERO)
            .min(BigDecimal(maxIncome.toInt())))
            .toFloat()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    val rounded = (newValue / 100f).toInt() * 100
                    onTextChange(rounded.toString())
                },
                valueRange = 0f..maxIncome,
            )
            Text(
                text = "左右滑动刻度快速调整金额",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedExpenseStep(
    fixedExpensesMonthly: BigDecimal,
    fixedExpensesDrafts: List<FixedExpenseDraft>,
    onAddTemplate: (String) -> Unit,
    onAmountChange: (UUID, String) -> Unit,
    onRemove: (UUID) -> Unit,
) {
    var showTemplateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "你有些什么固定支出？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = "固定支出合计：¥${formatAmount(fixedExpensesMonthly)}", style = MaterialTheme.typography.bodyLarge)

        Button(onClick = { showTemplateSheet = true }) {
            Text("＋ 添加固定支出")
        }

        if (fixedExpensesDrafts.isEmpty()) {
            Text(text = "暂无固定支出，可继续下一步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            fixedExpensesDrafts.forEach { draft ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = draft.name, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onRemove(draft.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除固定支出")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // 微调按钮：-100 / +100 元
                            TextButton(onClick = {
                                val current = draft.amountText.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val next = (current - BigDecimal("100")).coerceAtLeast(BigDecimal.ZERO)
                                onAmountChange(draft.id, next.toPlainString())
                            }) {
                                Text("-100")
                            }
                            OutlinedTextField(
                                value = draft.amountText,
                                onValueChange = { onAmountChange(draft.id, it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("金额（元）") },
                            )
                            TextButton(onClick = {
                                val current = draft.amountText.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val next = current + BigDecimal("100")
                                onAmountChange(draft.id, next.toPlainString())
                            }) {
                                Text("+100")
                            }
                        }
                    }
                }
            }
        }

        if (showTemplateSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTemplateSheet = false },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "选择一个常见固定支出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    val templates = listOf("房租/房贷", "车贷", "水电/通讯", "保险")
                    templates.forEach { name ->
                        TextButton(
                            onClick = {
                                onAddTemplate(name)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showTemplateSheet = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = name)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RatioStep(
    budgetRatio: Float,
    totalBudget: BigDecimal,
    savingsAllocation: BigDecimal,
    disposableFund: BigDecimal,
    onRatioChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "为你智能推荐预算方案", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = "依据 5/3/2 或 80/20 等比例，为你拆分预算与储蓄", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(text = "可支配金额：¥${formatAmount(disposableFund)}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(6.dp))

        // 双向配色进度条：左侧预算（蓝）、右侧储蓄（黄）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .weight(budgetRatio.coerceIn(0f, 1f).takeIf { it > 0f } ?: 0.001f)
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(color = PrimaryBlue, shape = RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)),
            )
            Box(
                modifier = Modifier
                    .weight((1f - budgetRatio).coerceIn(0f, 1f).takeIf { it > 0f } ?: 0.001f)
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(color = SavingsYellow, shape = RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)),
            )
        }
        val budgetPercent = (budgetRatio * 100).toInt().coerceIn(0, 100)
        val savingsPercent = 100 - budgetPercent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "预算 $budgetPercent%", style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
            Text(text = "储蓄 $savingsPercent%", style = MaterialTheme.typography.bodySmall, color = SavingsYellow)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = budgetRatio,
            onValueChange = onRatioChange,
            valueRange = 0f..1f,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "预算：¥${formatAmount(totalBudget)}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
            Text(text = "储蓄：¥${formatAmount(savingsAllocation)}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
        }

        if (disposableFund > BigDecimal.ZERO) {
            Text(
                text = "固定支出 ¥${formatAmount(disposableFund - (totalBudget + savingsAllocation))} 已优先预留，不计入滑块分配。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { onRatioChange(0.8f) }) { Text("80/20") }
            TextButton(onClick = { onRatioChange(0.5f) }) { Text("50/50") }
            TextButton(onClick = { onRatioChange(0.7f) }) { Text("70/30") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBreakdownStep(
    totalBudget: BigDecimal,
    unallocatedPool: BigDecimal,
    allocations: List<com.aifinance.core.model.BudgetCategoryAllocation>,
    errorMessage: String?,
    onMinusPlus: (UUID, BigDecimal) -> Unit,
    onReset: () -> Unit,
    onAverage: () -> Unit,
    onAddCategory: (UUID) -> Unit,
) {
    var showCategorySheet by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "月度分类预算推荐", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = "TotalBudget：¥${formatAmount(totalBudget)}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "未分配池：¥${formatAmount(unallocatedPool)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (totalBudget <= BigDecimal.ZERO) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("本月可变预算为 0，将进入赤字模式（或仅做记录）。", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(onClick = { showCategorySheet = true }) {
                Text("＋ 添加分类预算")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onReset) { Text("重置为推荐") }
                Button(onClick = onAverage) { Text("平均分配") }
            }

            allocations.forEach { cat ->
                val canMinus = cat.amount > BigDecimal.ZERO
                val canPlus = unallocatedPool >= BigDecimal("1.00")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(text = cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "预算：¥${formatAmount(cat.amount)}", color = PrimaryBlue)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMinusPlus(cat.categoryId, BigDecimal("-50.00")) },
                            enabled = canMinus,
                        ) {
                            Text(text = "-50", fontWeight = FontWeight.Bold, color = if (canMinus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { onMinusPlus(cat.categoryId, BigDecimal("50.00")) },
                            enabled = canPlus,
                        ) {
                            Text(text = "+50", fontWeight = FontWeight.Bold, color = if (canPlus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(text = errorMessage, color = ErrorRed, fontWeight = FontWeight.Medium)
            }
        }

        if (showCategorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showCategorySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "添加一个可变支出分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Divider()
                    // 与 ViewModel 中推荐分类保持一致
                    val candidates = listOf(
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseFood, "餐饮"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseShopping, "购物"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseTransport, "交通"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseEntertainment, "娱乐"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseEducation, "教育"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseMedical, "医疗"),
                        CategoryCandidate(com.aifinance.core.model.CategoryCatalog.Ids.ExpenseOther, "其他支出"),
                    )
                    candidates.forEach { candidate ->
                        // 已存在的分类不重复添加
                        val exists = allocations.any { it.categoryId == candidate.id }
                        TextButton(
                            onClick = {
                                if (!exists) {
                                    onAddCategory(candidate.id)
                                }
                                showCategorySheet = false
                            },
                            enabled = !exists,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = candidate.name)
                                if (exists) {
                                    Text(text = "已添加", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private data class CategoryCandidate(
    val id: UUID,
    val name: String,
)

@Composable
private fun BottomActions(
    modifier: Modifier = Modifier,
    stepIndex: Int,
    totalBudget: BigDecimal,
    role: UserRole?,
    totalIncomeText: String,
    onNext: () -> Unit,
) {
    val enabled = when (stepIndex) {
        1 -> role != null
        2 -> totalIncomeText.isNotBlank()
        3 -> true
        4 -> true
        5 -> true
        else -> false
    }
    Button(
        onClick = onNext,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = when (stepIndex) {
                5 -> "确认使用"
                else -> if (stepIndex == 4 && totalBudget <= BigDecimal.ZERO) "确认使用" else "下一步"
            },
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatAmount(amount: BigDecimal): String {
    val df = DecimalFormat("#,##0.00")
    return df.format(amount)
}

