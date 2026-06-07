# 涂色打卡图形显示不完整 - 问题分析与修复清单

## 问题描述

攒钱计划的涂色打卡界面（小狗图形）在某些设备上没有完整显示，图形底部可能被截断。

## 截图分析

从截图可以看到：
- 显示 "1 / 365" 进度和 "换个图" 按钮
- 小狗形状的像素网格显示在浅蓝色背景容器中
- 图形底部区域似乎显示不完整

---

## 代码位置分析

### 核心文件

| 文件 | 作用 | 关键行 |
|------|------|--------|
| `feature/savings_goal/src/main/java/.../SavingsPixelShapes.kt` | 像素形状定义 | L41-231 |
| `feature/savings_goal/src/main/java/.../SavingsRecordViews.kt` | 涂色视图渲染 | L293-370 |
| `feature/savings_goal/src/main/java/.../SavingsGoalDetailScreen.kt` | 详情页容器 | L226-236 |

### 问题定位

#### 1. 网格尺寸计算 (`SavingsRecordViews.kt:293-314`)

```kotlin
@Composable
private fun ColoringShapeView(...) {
    val layout = remember(shape, totalPeriods) { SavingsPixelShapes.layoutFor(shape, totalPeriods) }
    val gap = 2.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val horizontalPadding = 64.dp  // 问题点：固定 64dp 可能不准确
    val availableWidth = screenWidth - horizontalPadding

    val computedCellSize = (availableWidth - gap * (layout.width - 1)) / layout.width
    val cellSize = computedCellSize.coerceIn(12.dp, 28.dp)  // 问题点：最大 28dp 可能太小

    val gridWidthDp = cellSize * layout.width + gap * (layout.width - 1)
    val gridHeightDp = cellSize * layout.height + gap * (layout.height - 1)
}
```

**潜在问题：**
- `horizontalPadding = 64.dp` 是硬编码值，可能与实际布局不匹配
- `cellSize` 最大限制为 28dp，在大屏设备上可能过于保守
- 没有考虑顶部工具栏、状态栏等额外高度

#### 2. 网格容器 (`SavingsRecordViews.kt:321-368`)

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()  // 问题点：高度自适应可能不够
        .clip(RoundedCornerShape(12.dp))
        .background(...),
    contentAlignment = Alignment.Center,
) {
    Box(
        modifier = Modifier.size(
            width = gridWidthDp,
            height = gridHeightDp,  // 显式设置高度
        ),
    ) {
        layout.cells.forEach { cell ->
            // 使用 offset 定位每个 cell
            ColoringPeriodTile(
                modifier = Modifier
                    .size(cellSize)
                    .align(Alignment.TopStart)
                    .offset(
                        x = (cellSize + gap) * cell.col,
                        y = (cellSize + gap) * cell.row,  // 问题点：offset 可能导致溢出
                    ),
            )
        }
    }
}
```

**潜在问题：**
- 内部 `Box` 使用 `Modifier.size()` 设置固定尺寸
- 每个 tile 使用 `offset()` 定位，如果计算有误可能导致溢出
- 外层 `Box` 的 `wrapContentHeight()` 可能无法正确包裹所有 offset 内容

#### 3. 小狗形状定义 (`SavingsPixelShapes.kt:108-128`)

```kotlin
private fun dog365Rows(): List<RowDef> = listOf(
    RowDef(left = 4, gap = 10, right = 4),   // Row 0: 耳朵
    RowDef(left = 6, gap = 8, right = 6),    // Row 1
    RowDef(left = 7, gap = 6, right = 7),    // Row 2
    RowDef(left = 8, gap = 4, right = 8),    // Row 3
    RowDef(left = 22),                        // Row 4: 头部
    RowDef(left = 24),                        // Row 5
    RowDef(left = 26),                        // Row 6: 最宽处
    RowDef(left = 24),                        // Row 7
    RowDef(left = 22),                        // Row 8
    RowDef(left = 20),                        // Row 9: 脖子
    RowDef(left = 16),                        // Row 10
    RowDef(left = 14),                        // Row 11
    RowDef(left = 18),                        // Row 12: 身体
    RowDef(left = 20),                        // Row 13
    RowDef(left = 22),                        // Row 14
    RowDef(left = 24),                        // Row 15
    RowDef(left = 24),                        // Row 16
    RowDef(left = 24),                        // Row 17
    RowDef(left = 8, gap = 8, right = 8),    // Row 18: 腿部
)
```

**形状特点：**
- 总行数：19 行
- 最大宽度：26 列
- 总格子数：366（365 + 1 end marker）

#### 4. 屏幕高度计算

假设典型设备参数：
- 屏幕宽度：360dp
- 可用宽度：360 - 64 = 296dp
- Cell 大小：(296 - 2*(26-1)) / 26 ≈ 9.3dp → 被限制为 12dp
- 网格宽度：12 * 26 + 2 * 25 = 362dp（超出屏幕！）
- 网格高度：12 * 19 + 2 * 18 = 264dp

**关键发现：** 网格宽度 (362dp) 可能超出屏幕宽度 (360dp)，导致水平溢出！

---

## 工作清单

### Phase 1: 诊断与验证

- [ ] **1.1 添加调试日志**
  - 在 `ColoringShapeView` 中添加 Log 输出
  - 记录 `layout.width`, `layout.height`, `cellSize`, `gridWidthDp`, `gridHeightDp`
  - 记录 `screenWidth`, `availableWidth`
  - 文件：`SavingsRecordViews.kt`

- [ ] **1.2 多设备测试**
  - 在不同尺寸设备上测试（小屏 320dp、中屏 360dp、大屏 400dp+）
  - 记录每个设备的网格显示情况
  - 截图对比

- [ ] **1.3 检查实际 padding 值**
  - 追踪从 `SavingsGoalDetailScreen` 到 `ColoringShapeView` 的完整 padding 链
  - 验证 `horizontalPadding = 64.dp` 是否准确
  - 文件：`SavingsGoalDetailScreen.kt`, `SavingsRecordViews.kt`

### Phase 2: 修复方案 A - 优化尺寸计算

- [ ] **2.1 动态计算 horizontalPadding**
  ```kotlin
  // 替换硬编码值
  val horizontalPadding = remember(screenWidth) {
      // 根据实际布局层级计算
      16.dp * 2 + 16.dp * 2  // Column padding + Card padding
  }
  ```
  - 文件：`SavingsRecordViews.kt:304-305`

- [ ] **2.2 调整 cellSize 上限**
  ```kotlin
  // 根据屏幕宽度动态调整上限
  val maxCellSize = if (screenWidth < 360.dp) 20.dp else 28.dp
  val cellSize = computedCellSize.coerceIn(12.dp, maxCellSize)
  ```
  - 文件：`SavingsRecordViews.kt:309-310`

- [ ] **2.3 添加宽度约束检查**
  ```kotlin
  // 确保网格不超出屏幕
  val constrainedGridWidth = gridWidthDp.coerceAtMost(availableWidth)
  ```
  - 文件：`SavingsRecordViews.kt:313`

### Phase 3: 修复方案 B - 使用 LazyVerticalGrid

- [ ] **3.1 重构为 LazyVerticalGrid（推荐）**
  - 将手动 offset 定位改为使用 `LazyVerticalGrid`
  - 利用 Compose 的内置滚动和布局能力
  - 示例结构：
  ```kotlin
  LazyVerticalGrid(
      columns = GridCells.Fixed(layout.width),
      modifier = Modifier.size(gridWidthDp, gridHeightDp),
  ) {
      items(layout.cells) { cell ->
          ColoringPeriodTile(...)
      }
  }
  ```
  - 文件：`SavingsRecordViews.kt:330-367`

- [ ] **3.2 处理空单元格**
  - 为非 shape 区域的 grid cell 提供空白占位
  - 文件：`SavingsRecordViews.kt`

### Phase 4: 修复方案 C - 改进容器布局

- [ ] **4.1 移除固定 size，改用 wrapContent**
  ```kotlin
  Box(
      modifier = Modifier
          .wrapContentWidth()
          .wrapContentHeight()
          .clip(RoundedCornerShape(12.dp))
          .background(...),
  ) {
      // 内容自适应
  }
  ```
  - 文件：`SavingsRecordViews.kt:330-335`

- [ ] **4.2 添加水平滚动支持**
  ```kotlin
  HorizontalScroll(
      modifier = Modifier.fillMaxWidth(),
      state = rememberScrollState(),
  ) {
      Box(modifier = Modifier.size(gridWidthDp, gridHeightDp)) {
          // cells...
      }
  }
  ```
  - 文件：`SavingsRecordViews.kt:321-368`

### Phase 5: 修复方案 D - 优化小狗形状

- [ ] **5.1 重新设计小狗形状**
  - 调整行定义使其更紧凑
  - 减少最大宽度需求
  - 文件：`SavingsPixelShapes.kt:108-128`

- [ ] **5.2 添加形状缩放逻辑**
  ```kotlin
  // 根据可用宽度动态缩放形状
  fun layoutFor(shape: SavingsColoringShape, totalPeriods: Int, availableWidth: Dp): PixelShapeLayout
  ```
  - 文件：`SavingsPixelShapes.kt`

### Phase 6: 测试与验证

- [ ] **6.1 单元测试**
  - 测试 `SavingsPixelShapes.layoutFor()` 在不同参数下的输出
  - 测试尺寸计算逻辑
  - 文件：新建 `SavingsPixelShapesTest.kt`

- [ ] **6.2 UI 测试**
  - 测试 `ColoringShapeView` 在不同屏幕尺寸下的渲染
  - 验证所有 shape（心形、小狗、小木屋）的显示
  - 文件：新建 `ColoringShapeViewTest.kt`

- [ ] **6.3 集成测试**
  - 完整流程：创建计划 → 查看详情 → 切换涂色视图 → 换个图
  - 验证打卡功能正常
  - 文件：相关测试文件

### Phase 7: 代码质量

- [ ] **7.1 提取常量**
  ```kotlin
  companion object {
      const val MIN_CELL_SIZE_DP = 12
      const val MAX_CELL_SIZE_DP = 28
      const val CELL_GAP_DP = 2
      const val HORIZONTAL_PADDING_DP = 64
  }
  ```
  - 文件：`SavingsRecordViews.kt`

- [ ] **7.2 添加文档注释**
  - 为 `ColoringShapeView` 添加 KDoc
  - 说明布局计算逻辑
  - 文件：`SavingsRecordViews.kt:293`

- [ ] **7.3 性能优化**
  - 确保 `layout` 的 remember key 正确
  - 避免不必要的重组
  - 文件：`SavingsRecordViews.kt:301`

---

## 推荐修复优先级

| 优先级 | 方案 | 复杂度 | 效果 |
|--------|------|--------|------|
| 🔴 高 | 2.1 动态计算 padding | 低 | 解决根因 |
| 🔴 高 | 2.2 调整 cellSize 上限 | 低 | 改善显示 |
| 🟡 中 | 3.1 重构为 LazyVerticalGrid | 中 | 长期方案 |
| 🟡 中 | 4.2 添加水平滚动 | 低 | 兜底方案 |
| 🟢 低 | 5.1 优化形状定义 | 高 | 美化效果 |

---

## 验收标准

- [ ] 小狗图形在 320dp~420dp 屏幕宽度设备上完整显示
- [ ] 心形和小木屋图形正常显示
- [ ] 切换图形（"换个图"按钮）功能正常
- [ ] 打卡点击功能正常
- [ ] 深色模式下颜色正确
- [ ] 无水平/垂直滚动条（除非必要）

---

## 参考资源

- Compose Layout 官方文档：https://developer.android.com/jetpack/compose/layouts
- LazyVerticalGrid 文档：https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/package-summary
- 像素艺术网格实现参考：https://github.com/nicholasgasior/gdx-pixel

---

*创建时间：2026-06-05*
*关联 Commit：a9e5f55 fix(savings_goal): review fixes — active-status guard, dynamic grid height, theme colors*
