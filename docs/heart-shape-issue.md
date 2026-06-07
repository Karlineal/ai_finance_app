# 心形涂色打卡 - 问题总结

## 问题描述

攒钱计划的涂色打卡功能中，365天心形图案显示不正常，不像经典的心形。

## 当前实现

使用**二维布尔矩阵**定义心形形状：
- 文件：`feature/savings_goal/src/main/java/.../SavingsPixelShapes.kt`
- 格式：`Array<BooleanArray>`，每行是一个布尔数组
- 渲染：遍历矩阵，将 `true` 的位置渲染为格子

## 当前矩阵定义

```kotlin
private val HEART_365_MATRIX = arrayOf(
    // 26 列 × 21 行，共 365 格
    booleanArrayOf(F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F),  // row 0
    booleanArrayOf(F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F),  // row 1
    booleanArrayOf(F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F),  // row 2
    booleanArrayOf(F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F),  // row 3
    // ... 中间 8 行都是 24 格
    booleanArrayOf(F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F),  // row 11
    booleanArrayOf(F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F),  // row 12
    booleanArrayOf(F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F),  // row 13
    booleanArrayOf(F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F),  // row 14
    booleanArrayOf(F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F),  // row 15
    booleanArrayOf(F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F),  // row 16
    booleanArrayOf(F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F),  // row 17
    booleanArrayOf(F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F),  // row 18
    booleanArrayOf(F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F,F),  // row 19
    booleanArrayOf(F,F,F,F,F,F,F,F,F,F,F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F),  // row 20
)
```

## 问题分析

### 1. 形状不像心形
当前形状更像是：
- 顶部是平的（第 0 行只有 10 格）
- 中间是矩形（第 3-10 行都是 24 格）
- 底部逐渐收窄到一个点

**经典心形应该是：**
- 顶部有两个弧形凸起
- 中间最宽
- 底部逐渐收窄到一个点

### 2. 顶部弧形缺失
经典心形的顶部应该有类似 `♥` 的两个弧形，但当前实现是平的。

### 3. 格子数验证
- 目标：365 格
- 实际：365 格 ✓（格子数正确）

## 解决方案方向

### 方案 A：使用数学公式生成心形
使用心形的参数方程：
```
x = 16 sin³(t)
y = 13 cos(t) - 5 cos(2t) - 2 cos(3t) - cos(4t)
```
在网格上采样，生成更精确的心形。

### 方案 B：使用经典像素心形模板
找一个已知好看的像素心形模板，扩展到 365 格。

### 方案 C：手动调整矩阵
手动设计一个更像心形的矩阵，确保：
1. 顶部有两个弧形
2. 中间最宽
3. 底部逐渐收窄

## 技术约束

1. **Android 应用**：不能使用 CSS，只能用 Jetpack Compose
2. **像素风格**：需要是像素网格，不能是矢量图形
3. **格子数固定**：必须恰好 365 格（365 天）
4. **对称性**：心形应该左右对称
5. **可点击**：每个格子需要可以点击打卡

## 相关文件

- `feature/savings_goal/src/main/java/.../SavingsPixelShapes.kt` - 形状定义
- `feature/savings_goal/src/main/java/.../SavingsRecordViews.kt` - 渲染逻辑

## 当前效果

![当前心形效果](../screenshots/current-heart.png)

## 期望效果

应该是一个经典的心形 `♥`，顶部有两个弧形，底部收窄到一个点。

---

*创建时间：2026-06-05*
*状态：待修复*
