# Savings Goal 日历视图问题清单

**发现日期**: 2026-06-07  
**模块**: `feature/savings_goal`  
**关联分支**: `5.29-saving-reviewed`

---

## 一、严重问题

### 1. 双轨数据架构（核心架构问题）
**状态**: 🔴 待处理

项目同时存在两套完全独立的打卡数据表，但实际功能只使用了其中一套：

| 数据路径 | 表名 | Entity | DAO | Repository | ViewModel |
|----------|------|--------|-----|------------|-----------|
| Path A (未使用) | `check_ins` | `CheckInEntity` | `CheckInDao` | `SavingsGoalCheckInRepository` | `CheckInCalendarViewModel` |
| Path B (使用中) | `savings_records` | `SavingsRecordEntity` | `SavingsRecordDao` | `SavingsGoalRepository` | `SavingsGoalViewModel` |

**影响**: 
- `check_ins` 表及所有相关代码是死代码，浪费维护成本
- 数据库迁移添加了 `check_ins` 表，但从未被读写

### 2. 死代码文件
**状态**: 🔴 待删除

以下文件完全未被任何 UI 流程使用：
- `feature/savings_goal/CheckInCalendarViewModel.kt`
- `feature/savings_goal/CheckInCalendarUiState.kt`
- `core/data/repository/SavingsGoalCheckInRepository.kt`
- `core/data/repository/SavingsGoalCheckInRepositoryImpl.kt`
- `core/database/dao/CheckInDao.kt`
- `core/database/entity/CheckInEntity.kt`
- `core/model/CheckIn.kt`

**相关 DI 注册**: 
- `RepositoryModule.kt` 中的 `SavingsGoalCheckInRepository` 提供
- `DatabaseModule.kt` 中的 `CheckInDao` 提供
- `AiFinanceDatabase.kt` 中的 `checkInDao()` 抽象方法

### 3. 热力图日期范围错误
**位置**: `CheckInCalendarScreen.kt:249-250`

```kotlin
val endDate = remember { LocalDate.now() }  // ❌ 错误：应该用 goal.endDate
val startDate = remember { goal?.startDate ?: endDate.minusMonths(12) }
```

**问题**:
- 已完成的计划会显示超出计划范围的空白区域
- 未开始的计划会显示反转的日期范围（startDate > endDate）
- 进度计算使用 `goal.endDate`，但热力图使用 `LocalDate.now()`，两者不一致

**修复建议**:
```kotlin
val endDate = remember(goal) { 
    goal?.endDate?.let { if (it.isAfter(LocalDate.now())) LocalDate.now() else it } 
        ?: LocalDate.now()
}
```

---

## 二、高优先级问题

### 4. 52周/365天计划未区分渲染
**位置**: `CheckInCalendarScreen.kt` 整体

**问题**:
- 52周计划应按"周"打卡（共52次），但热力图仍按"天"渲染（显示364个格子）
- 导致52周计划的热力图非常稀疏（只有约14%的格子可能被点亮）
- `SavingsRecordViews.kt` 中的 `YearHeatMapView` 对52周计划使用 `periodDate` 做了适配，但 `CheckInCalendarScreen` 没有

**修复建议**:
- 52周计划：热力图按周显示，每周一个格子
- 365天计划：热力图按天显示

### 5. totalDays 计算错误
**位置**: `CheckInCalendarScreen.kt:104-108`

```kotlin
val totalDays = remember(goal) {
    goal?.let {
        ChronoUnit.DAYS.between(it.startDate, it.endDate).toInt().coerceIn(1, 365)
    } ?: 365
}
```

**问题**:
- `ChronoUnit.DAYS.between()` 返回的是两个日期之间的天数，**不包含 endDate**
- 52周计划：应为52个周期（52次打卡），而非364天
- 365天计划：实际返回364天，导致进度条显示99%而非100%
- `coerceIn(1, 365)` 硬编码上限，不支持更长的灵活计划

**修复建议**:
```kotlin
val totalPeriods = remember(goal) {
    goal?.let {
        when (it.savingsMethod) {
            SavingsMethod.WEEKLY_52 -> 52
            SavingsMethod.DAILY_365 -> 365
            else -> ChronoUnit.DAYS.between(it.startDate, it.endDate).toInt() + 1
        }
    } ?: 365
}
```

### 6. 缺少唯一约束
**位置**: 
- `CheckInEntity.kt` - `check_ins` 表
- `SavingsRecordEntity.kt` - `savings_records` 表

**问题**:
- 两个表都缺少 `(savingsGoalId, date)` 的唯一索引
- 使用 `OnConflictStrategy.REPLACE` 配合 UUID 主键，无法防止重复插入
- 用户快速双击可能导致同一天创建两条记录

**修复建议**:
```kotlin
@Entity(
    tableName = "savings_records",
    indices = [
        Index(value = ["savingsGoalId"]),
        Index(value = ["savingsGoalId", "date"], unique = true)  // 添加唯一约束
    ]
)
```

---

## 三、中等问题

### 7. CheckInCalendarScreen 未使用自己的 ViewModel
**位置**: `CheckInCalendarScreen.kt:92-93`

```kotlin
viewModel: SavingsGoalViewModel = hiltViewModel(),  // ❌ 应该用 CheckInCalendarViewModel
```

**问题**: 
- `CheckInCalendarViewModel` 已实现但从未被实例化
- 实际使用的是 `SavingsGoalViewModel`，操作的是 `savings_records` 表

### 8. 点击操作使用固定 baseAmount
**位置**: `CheckInCalendarScreen.kt:149`

```kotlin
val amount = currentGoal.baseAmount ?: BigDecimal.ONE  // ❌ 递增计划应使用 periodAmount
```

**问题**:
- 52周/365天递增计划中，每期金额应递增
- 但这里始终使用基础金额，导致打卡金额错误

**修复建议**:
```kotlin
val periodIndex = SavingsGoalCalculator.getCurrentPeriodIndex(
    goal.startDate, goal.savingsMethod, goal.frequency
)
val amount = SavingsGoalCalculator.periodAmount(goal, periodIndex)
```

### 9. periodIndex 固定为0
**位置**: `CheckInCalendarScreen.kt:153`

```kotlin
periodIndex = 0,  // ❌ 应该根据日期计算实际期数
```

**问题**: 导致所有打卡记录的期数都是0，无法区分是哪一期的打卡

### 10. 图例"少/多"与实际不匹配
**位置**: `CheckInCalendarScreen.kt:404-420`

**问题**:
- 图例显示"少"和"多"两端，暗示有多个强度级别（类似 GitHub 四级热力图）
- 但实际只有两个级别：`EMPTY` 和 `CHECKED`
- "少/多"的文案具有误导性

**修复建议**: 改为"未打卡"和"已打卡"

### 11. SavingsRecordSection 仅在 ACTIVE 状态显示
**位置**: `SavingsGoalDetailScreen.kt:226-227`

```kotlin
if ((goal.savingsMethod == SavingsMethod.DAILY_365 || goal.savingsMethod == SavingsMethod.WEEKLY_52)
    && goal.status == SavingsGoalStatus.ACTIVE) {
```

**问题**: 当计划状态为 `COMPLETED` 或 `FAILED` 时，日历视图完全不显示，用户无法回顾历史打卡记录

### 12. LazyVerticalGrid 嵌套滚动问题
**位置**: 
- `SavingsGoalDetailScreen.kt:138-139` - 外层 `Column.verticalScroll`
- `SavingsRecordViews.kt:193-211` - `CheckInCardListView` 中的 `LazyVerticalGrid`

**问题**:
- `LazyVerticalGrid` 被设置 `heightIn(min = 520.dp, max = 4000.dp)` 来解决嵌套滚动
- 对于365天计划，520dp最小高度可能不够显示所有卡片
- 嵌套滚动容器在 Compose 中默认不支持跨容器滚动传递

---

## 四、低优先级问题

### 13. MonthHeader 可见性判断
**位置**: `CheckInCalendarScreen.kt:372`

```kotlin
if (calendarMonth.weekDays.first().first().date <= endDate) {
```

**问题**: 如果目标的 `startDate` 在未来，月份头部可能不会正确显示

### 14. daySize 固定 18.dp
**位置**: `CheckInCalendarScreen.kt:240`

**问题**: 
- 小屏手机（320dp宽）：52周 x 18dp = 936dp，远超屏幕宽度
- 大屏平板：日历可能显得过于稀疏
- 缺乏响应式适配

### 15. CheckInCalendarUiState.totalDays 默认值
**位置**: `CheckInCalendarUiState.kt:9`

```kotlin
val totalDays: Int = 365,  // ❌ 52周计划应为52
```

**问题**: 默认值硬编码为365，不适用于52周计划

### 16. SavingsRecordEntity 缺少 periodIndex 字段
**位置**: `CheckInEntity.kt`

**问题**: `CheckInEntity` 没有 `periodIndex` 字段，如果未来切换到 `check_ins` 表，会丢失期数信息

---

## 五、架构建议

### 短期（清理死代码）
1. 删除 `CheckInCalendarViewModel.kt`、`CheckInCalendarUiState.kt`
2. 删除 `SavingsGoalCheckInRepository.kt`、`SavingsGoalCheckInRepositoryImpl.kt`
3. 删除 `CheckInDao.kt`、`CheckInEntity.kt`、`CheckIn.kt`
4. 清理 DI 模块中的相关注册
5. 考虑添加数据库迁移删除 `check_ins` 表（或保留但标记为废弃）

### 中期（修复热力图）
1. 修正日期范围计算（使用 `goal.endDate`）
2. 修正 `totalDays` 计算（按周期类型区分）
3. 添加 `(savingsGoalId, date)` 唯一约束
4. 52周/365天计划使用不同的热力图渲染逻辑

### 长期（架构统一）
- 如果需要更精确的打卡记录，考虑统一到 `check_ins` 表
- 为 `check_ins` 添加 `periodIndex` 字段
- 实现真正的打卡语义（不含金额、不创建转账交易）

---

## 六、测试用例建议

### 需要验证的场景
1. **52周计划**：创建52周存钱计划，验证热力图显示52个格子
2. **365天计划**：创建365天存钱计划，验证热力图显示365个格子
3. **已完成计划**：标记计划为完成，验证日历仍可查看历史
4. **快速双击**：快速双击同一天，验证不会创建重复记录
5. **递增金额**：验证打卡金额随期数递增
6. **跨年计划**：验证跨年的日期范围计算正确

---

**文档维护**: 此文档由 Claude Code 自动生成，基于代码静态分析。实际修复前请验证问题是否存在。
