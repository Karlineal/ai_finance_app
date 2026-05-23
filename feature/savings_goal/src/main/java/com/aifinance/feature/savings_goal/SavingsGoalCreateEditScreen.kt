package com.aifinance.feature.savings_goal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aifinance.core.designsystem.theme.SavingsDashedLine
import com.aifinance.core.designsystem.theme.SavingsDashedLineDark
import com.aifinance.core.designsystem.theme.SavingsPigBg
import com.aifinance.core.designsystem.theme.SavingsPigBgDark
import com.aifinance.core.designsystem.theme.SavingsPigBorder
import com.aifinance.core.designsystem.theme.SavingsPigBorderDark
import com.aifinance.core.designsystem.theme.SavingsSelectedBg
import com.aifinance.core.designsystem.theme.SavingsSelectedBgDark
import com.aifinance.core.designsystem.theme.SavingsSummaryBg
import com.aifinance.core.designsystem.theme.SavingsSummaryBgDark
import com.aifinance.core.designsystem.theme.SavingsSwitchTrack
import com.aifinance.core.designsystem.theme.SavingsSwitchTrackDark
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.data.repository.SavingsGoalCalculator
import com.aifinance.core.model.SavingsFrequency
import com.aifinance.core.model.SavingsGoal
import com.aifinance.core.model.SavingsGoalStatus
import com.aifinance.core.model.SavingsMethod
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalCreateEditScreen(
    goalId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val editingGoal = remember(goals, goalId) {
        goalId?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let { id ->
            goals.firstOrNull { it.id == id }
        }
    }
    val isEditing = goalId != null

    var method by rememberSaveable { mutableStateOf(SavingsMethod.DAILY_365) }
    var baseAmount by rememberSaveable { mutableStateOf("1") }
    var fixedAmount by rememberSaveable { mutableStateOf("100") }
    var frequency by rememberSaveable { mutableStateOf(SavingsFrequency.MONTHLY) }
    var periods by rememberSaveable { mutableStateOf("12") }

    var name by rememberSaveable { mutableStateOf("") }
    var targetAmount by rememberSaveable { mutableStateOf("") }
    var currentAmount by rememberSaveable { mutableStateOf("0") }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf<UUID?>(null) }
    var didLoad by rememberSaveable(goalId) { mutableStateOf(false) }

    var showDetails by rememberSaveable { mutableStateOf(isEditing) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showBaseAmountDialog by remember { mutableStateOf(false) }
    var isAutoDeduct by rememberSaveable { mutableStateOf(true) }
    var agreedToTerms by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editingGoal?.id) {
        if (editingGoal != null && !didLoad) {
            method = editingGoal.savingsMethod
            baseAmount = editingGoal.baseAmount?.stripTrailingZeros()?.toPlainString() ?: ""
            fixedAmount = editingGoal.fixedAmount?.stripTrailingZeros()?.toPlainString() ?: ""
            frequency = editingGoal.frequency ?: SavingsFrequency.MONTHLY
            name = editingGoal.name
            targetAmount = editingGoal.targetAmount.stripTrailingZeros().toPlainString()
            currentAmount = editingGoal.currentAmount.stripTrailingZeros().toPlainString()
            startDate = editingGoal.startDate.toString()
            endDate = editingGoal.endDate.toString()
            notes = editingGoal.notes.orEmpty()
            accountId = editingGoal.accountId
            didLoad = true
            showDetails = true
        }
    }

    LaunchedEffect(method, baseAmount, fixedAmount, frequency, periods, startDate) {
        if (isEditing) return@LaunchedEffect
        val start = startDate.toLocalDateOrNull() ?: LocalDate.now()
        val defaultNames = setOf("", "52周存钱计划", "365天存钱计划", "12存单法", "定额存钱计划", "灵活存钱计划")
        val canUpdateName = name.trim() in defaultNames
        when (method) {
            SavingsMethod.WEEKLY_52 -> {
                val base = baseAmount.toBigDecimalOrNull() ?: BigDecimal(10)
                targetAmount = SavingsGoalCalculator.calculateTotalTarget(method, base, null, null).toPlainString()
                endDate = start.plusWeeks(52).toString()
                if (canUpdateName) name = "52周存钱计划"
            }
            SavingsMethod.DAILY_365 -> {
                val base = baseAmount.toBigDecimalOrNull() ?: BigDecimal(1)
                targetAmount = SavingsGoalCalculator.calculateTotalTarget(method, base, null, null).toPlainString()
                endDate = start.plusDays(365).toString()
                if (canUpdateName) name = "365天存钱计划"
            }
            SavingsMethod.MONTHLY_12 -> {
                val base = baseAmount.toBigDecimalOrNull() ?: BigDecimal(100)
                targetAmount = SavingsGoalCalculator.calculateTotalTarget(method, base, null, null).toPlainString()
                endDate = start.plusMonths(12).toString()
                if (canUpdateName) name = "12存单法"
            }
            SavingsMethod.FIXED_AMOUNT -> {
                val fix = fixedAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val p = periods.toIntOrNull() ?: 0
                targetAmount = SavingsGoalCalculator.calculateTotalTarget(method, null, fix, p).toPlainString()
                endDate = when (frequency) {
                    SavingsFrequency.DAILY -> start.plusDays(p.toLong())
                    SavingsFrequency.WEEKLY -> start.plusWeeks(p.toLong())
                    SavingsFrequency.MONTHLY -> start.plusMonths(p.toLong())
                }.toString()
                if (canUpdateName) name = "定额存钱计划"
            }
            SavingsMethod.FLEXIBLE -> {
                if (canUpdateName) name = "灵活存钱计划"
            }
        }
    }

    AnimatedContent(
        targetState = showDetails,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(tween(300)) { it } + fadeIn()).togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut())
            } else {
                (slideInHorizontally(tween(300)) { -it } + fadeIn()).togetherWith(slideOutHorizontally(tween(300)) { it } + fadeOut())
            }
        },
        label = "ScreenTransition"
    ) { showingDetails ->
        if (!showingDetails) {
            // STEP 1: Method Selection (Poster style)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val imageHeight = screenWidth * (1844f / 853f)

                Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_savings_header),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // Dark overlay for dark theme
                    if (isSystemInDarkTheme()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xCC0F172A))
                        )
                    }

                    // Invisible clickable back button over the image's back icon
                    Box(
                        modifier = Modifier
                            .padding(top = 42.dp, start = 8.dp)
                            .size(56.dp)
                            .clickable(onClick = onBack)
                    )

                    // Method Selector Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = imageHeight * 0.53f) // Align with blank space
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MethodSelector(selected = method, onSelect = { method = it })

                        Spacer(Modifier.height(16.dp))

                        val exampleText = when (method) {
                            SavingsMethod.DAILY_365 -> "💰 首日存${baseAmount}元，每日递增${baseAmount}元，一年后约存${targetAmount}元"
                            SavingsMethod.WEEKLY_52 -> "💰 首周存${baseAmount}元，每周递增${baseAmount}元，一年后约存${targetAmount}元"
                            SavingsMethod.MONTHLY_12 -> "💰 每月固定存入${baseAmount}元，一年后约存${targetAmount}元"
                            SavingsMethod.FIXED_AMOUNT -> "💰 每期存入固定金额，达成特定目标"
                            SavingsMethod.FLEXIBLE -> "💰 随心所欲，灵活存钱，积少成多"
                        }
                        Text(
                            text = exampleText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // "去存钱" button overlay
                    Button(
                        onClick = { showDetails = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = imageHeight * 0.06f)
                            .fillMaxWidth(0.85f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("去存钱", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // STEP 2: Configuration Form
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(if (isEditing) "编辑攒钱计划" else "添加攒钱计划", fontWeight = FontWeight.Bold, fontSize = 18.sp) 
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isEditing) onBack() else showDetails = false
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // The card
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                                Spacer(modifier = Modifier.height(20.dp))

                                // Title Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (name.isEmpty()) {
                                                Text("请输入计划名称", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.width(IntrinsicSize.Min)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                // Dashed line
                                val dashedLineColor = if (isSystemInDarkTheme()) SavingsDashedLineDark else SavingsDashedLine
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                    drawLine(
                                        color = dashedLineColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // Rows
                                FormRow(
                                    label = "存钱方式",
                                    value = when(method) {
                                        SavingsMethod.DAILY_365 -> "365天存钱"
                                        SavingsMethod.WEEKLY_52 -> "52周存钱"
                                        SavingsMethod.MONTHLY_12 -> "12月存单"
                                        SavingsMethod.FIXED_AMOUNT -> "定额存钱"
                                        SavingsMethod.FLEXIBLE -> "灵活存钱"
                                    },
                                    onClick = { if (!isEditing) showDetails = false }
                                )

                                FormRow(
                                    label = "初始金额",
                                    value = "${baseAmount.ifEmpty { "0" }}元",
                                    onClick = { showBaseAmountDialog = true }
                                )

                                FormRow(
                                    label = "递增金额",
                                    value = "${baseAmount.ifEmpty { "0" }}元",
                                    onClick = { showBaseAmountDialog = true }
                                )

                                val startLocal = startDate.toLocalDateOrNull() ?: LocalDate.now()
                                FormRow(
                                    label = "开始时间",
                                    value = "今天${startLocal.monthValue}月${startLocal.dayOfMonth}日开始",
                                    onClick = { showStartDatePicker = true }
                                )

                                // Auto Deduct Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("自动扣款", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = isAutoDeduct,
                                        onCheckedChange = { isAutoDeduct = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = if (isSystemInDarkTheme()) SavingsSwitchTrackDark else SavingsSwitchTrack)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Summary Box
                                val exampleText = when (method) {
                                    SavingsMethod.DAILY_365 -> "💰 首日存${baseAmount}元，一年后大约可存${targetAmount}元"
                                    SavingsMethod.WEEKLY_52 -> "💰 首周存${baseAmount}元，一年后大约可存${targetAmount}元"
                                    SavingsMethod.MONTHLY_12 -> "💰 每月固定存入${baseAmount}元，一年后大约可存${targetAmount}元"
                                    else -> "💰 开启您的攒钱之旅"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSystemInDarkTheme()) SavingsSummaryBgDark else SavingsSummaryBg, RoundedCornerShape(12.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(exampleText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Pig Icon overlapping the card
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(56.dp)
                                .background(if (isSystemInDarkTheme()) SavingsPigBgDark else SavingsPigBg, CircleShape)
                                .border(1.dp, if (isSystemInDarkTheme()) SavingsPigBorderDark else SavingsPigBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐷", fontSize = 28.sp)
                        }
                    }

                    // Bottom section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { agreedToTerms = !agreedToTerms }.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (agreedToTerms) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (agreedToTerms) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("我已阅读并同意", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("《攒钱计划服务协议》", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                val target = targetAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val current = currentAmount.ifBlank { "0" }.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val start = startDate.toLocalDateOrNull() ?: LocalDate.now()
                                val end = endDate.toLocalDateOrNull() ?: LocalDate.now()

                                val base = baseAmount.toBigDecimalOrNull()
                                val fixed = fixedAmount.toBigDecimalOrNull()

                                val goal = SavingsGoal(
                                    id = editingGoal?.id ?: UUID.randomUUID(),
                                    accountId = accountId ?: UUID.randomUUID(),
                                    name = name.trim(),
                                    targetAmount = target.setScale(2, RoundingMode.HALF_UP),
                                    currentAmount = current.setScale(2, RoundingMode.HALF_UP),
                                    startDate = start,
                                    endDate = end,
                                    status = editingGoal?.status ?: SavingsGoalStatus.ACTIVE,
                                    notes = notes.trim().ifBlank { null },
                                    savingsMethod = method,
                                    fixedAmount = if (method == SavingsMethod.FIXED_AMOUNT) fixed else null,
                                    frequency = if (method == SavingsMethod.FIXED_AMOUNT) frequency else null,
                                    baseAmount = if (method in listOf(SavingsMethod.WEEKLY_52, SavingsMethod.DAILY_365, SavingsMethod.MONTHLY_12)) base else null,
                                    createdAt = editingGoal?.createdAt ?: Instant.now(),
                                    updatedAt = Instant.now(),
                                )
                                if (isEditing) {
                                    viewModel.updateGoal(goal)
                                } else {
                                    viewModel.createGoal(goal, isAutoDeduct)
                                }
                                onSaved()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            enabled = name.isNotBlank() && targetAmount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && agreedToTerms
                        ) {
                            Text("确认", fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialogWrapper(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { startDate = it }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialogWrapper(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { endDate = it }
        )
    }

    if (showBaseAmountDialog) {
        AlertDialog(
            onDismissRequest = { showBaseAmountDialog = false },
            title = { Text("设置金额") },
            text = {
                OutlinedTextField(
                    value = baseAmount,
                    onValueChange = { baseAmount = it.filterAmountInput() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = { TextButton(onClick = { showBaseAmountDialog = false }) { Text("确定") } }
        )
    }
}

@Composable
private fun FormRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MethodSelector(selected: SavingsMethod, onSelect: (SavingsMethod) -> Unit) {
    val methods = listOf(
        MethodInfo(SavingsMethod.DAILY_365, "365天存钱", "每日小积累"),
        MethodInfo(SavingsMethod.WEEKLY_52, "52周存钱", "周周递增存"),
        MethodInfo(SavingsMethod.MONTHLY_12, "12月存单", "月月稳步存"),
        MethodInfo(SavingsMethod.FIXED_AMOUNT, "定额存钱", "固定金额存"),
        MethodInfo(SavingsMethod.FLEXIBLE, "灵活存钱", "自由随意存")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val chunked = methods.chunked(2)
        chunked.forEach { rowMethods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMethods.forEach { info ->
                    MethodCard(
                        info = info,
                        isSelected = selected == info.method,
                        onClick = { onSelect(info.method) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowMethods.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MethodInfo(val method: SavingsMethod, val title: String, val subtitle: String)

@Composable
private fun MethodCard(
    info: MethodInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected)
        if (isSystemInDarkTheme()) SavingsSelectedBgDark else SavingsSelectedBg
    else
        MaterialTheme.colorScheme.surface
    val titleColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.5.dp, borderColor),
        tonalElevation = if (isSelected) 0.dp else 2.dp,
        shadowElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = info.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun MethodConfiguration(
    method: SavingsMethod,
    baseAmount: String,
    onBaseAmountChange: (String) -> Unit,
    fixedAmount: String,
    onFixedAmountChange: (String) -> Unit,
    frequency: SavingsFrequency,
    onFrequencyChange: (SavingsFrequency) -> Unit,
    periods: String,
    onPeriodsChange: (String) -> Unit,
) {
    AnimatedVisibility(visible = method in listOf(SavingsMethod.WEEKLY_52, SavingsMethod.DAILY_365, SavingsMethod.MONTHLY_12)) {
        OutlinedTextField(
            value = baseAmount,
            onValueChange = onBaseAmountChange,
            label = { Text(if (method == SavingsMethod.MONTHLY_12) "基础月存金额" else "基础金额") },
            prefix = { Text("¥") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    AnimatedVisibility(visible = method == SavingsMethod.FIXED_AMOUNT) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = fixedAmount,
                onValueChange = onFixedAmountChange,
                label = { Text("每期金额") },
                prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SavingsFrequency.DAILY to "每日", SavingsFrequency.WEEKLY to "每周", SavingsFrequency.MONTHLY to "每月").forEach { (freq, label) ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { onFrequencyChange(freq) },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedTextField(
                value = periods,
                onValueChange = onPeriodsChange,
                label = { Text("存入期数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toLocalDateOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    ) {
        DatePicker(state)
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = try { LocalDate.parse(this) } catch (_: DateTimeParseException) { null }

private fun String.filterAmountInput(): String {
    val b = StringBuilder()
    var d = false
    forEach {
        if (it.isDigit()) b.append(it)
        else if (it == '.' && !d) { b.append(it); d = true }
    }
    return b.toString()
}

private fun String.filterDigitInput() = filter { it.isDigit() }
