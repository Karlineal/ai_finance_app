package com.aifinance.feature.savings_goal


data class PixelCell(
    val row: Int,
    val col: Int,
    val periodIndex: Int,
    val isEndMarker: Boolean = false,
)

data class PixelShapeLayout(
    val width: Int,
    val height: Int,
    val cells: List<PixelCell>,
)

enum class SavingsColoringShape {
    HEART,
    /** 52 周：小木屋 */
    CABIN,
}

object SavingsPixelShapes {

    fun layoutFor(shape: SavingsColoringShape, totalPeriods: Int): PixelShapeLayout {
        require(totalPeriods > 0)
        return when (shape) {
            SavingsColoringShape.HEART -> when (totalPeriods) {
                52 -> buildFromMatrix(CABIN_52_MATRIX, totalPeriods)
                else -> buildHeart(totalPeriods)
            }
            SavingsColoringShape.CABIN -> when (totalPeriods) {
                52 -> buildFromMatrix(CABIN_52_MATRIX, totalPeriods)
                else -> buildHeart(totalPeriods)
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  心形算法生成
    //  使用经典心形曲线方程 (x²+y²-1)³ ≤ x²·y³
    //  通过二分搜索自动调整缩放，精确生成 totalPeriods 个格子
    // ─────────────────────────────────────────────────

    private fun buildHeart(totalPeriods: Int): PixelShapeLayout {
        // 365 天：25 列 × 22 行 = 550 个候选格子，心形约占 66%
        val width = 25
        val height = 22
        val cells = findHeartCells(width, height, totalPeriods)
        return PixelShapeLayout(width = width, height = height, cells = cells)
    }

    /**
     * 用二分搜索找到合适的缩放因子，使心形恰好包含 [targetCells] 个格子。
     * 缩放越大 → 心形越小 → 格子越少。
     */
    private fun findHeartCells(
        width: Int,
        height: Int,
        targetCells: Int,
    ): List<PixelCell> {
        var lo = 0.5f
        var hi = 3.0f
        var result = heartCellsAtScale(width, height, targetCells, lo)

        repeat(10) {
            val mid = (lo + hi) / 2f
            val count = countHeartCells(width, height, mid)
            if (count >= targetCells) {
                result = heartCellsAtScale(width, height, targetCells, mid)
                hi = mid
            } else {
                lo = mid
            }
        }
        return result
    }

    private fun countHeartCells(width: Int, height: Int, scale: Float): Int {
        val xScale = width.toFloat() / (2f * scale)
        val yScale = height.toFloat() / (2f * scale)
        var count = 0
        for (row in 0 until height) {
            val ny = 1f - 2f * row / (height - 1)
            for (col in 0 until width) {
                val nx = 2f * col / (width - 1) - 1f
                val x = nx * xScale
                val y = ny * yScale
                val xx = x * x
                val yy = y * y
                val a = xx + yy - 1f
                if (a * a * a <= xx * y * y * y) count++
            }
        }
        return count
    }

    private fun heartCellsAtScale(
        width: Int,
        height: Int,
        targetCells: Int,
        scale: Float,
    ): List<PixelCell> {
        val xScale = width.toFloat() / (2f * scale)
        val yScale = height.toFloat() / (2f * scale)
        val slots = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until height) {
            val ny = 1f - 2f * row / (height - 1)
            for (col in 0 until width) {
                val nx = 2f * col / (width - 1) - 1f
                val x = nx * xScale
                val y = ny * yScale
                val xx = x * x
                val yy = y * y
                val a = xx + yy - 1f
                if (a * a * a <= xx * y * y * y) {
                    slots += row to col
                }
            }
        }
        return slots.take(targetCells).mapIndexed { index, (row, col) ->
            PixelCell(row = row, col = col, periodIndex = index + 1)
        }
    }

    // ─────────────────────────────────────────────────
    //  52 周小木屋 — 10 列 × 8 行，共 52 格
    // ─────────────────────────────────────────────────

    private fun buildFromMatrix(matrix: Array<BooleanArray>, totalPeriods: Int): PixelShapeLayout {
        val height = matrix.size
        val width = matrix[0].size

        val slots = mutableListOf<Pair<Int, Int>>()
        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                if (matrix[row][col]) {
                    slots += row to col
                }
            }
        }

        require(slots.size >= totalPeriods) {
            "Shape has ${slots.size} blocks, need $totalPeriods"
        }

        val cells = mutableListOf<PixelCell>()
        slots.take(totalPeriods).forEachIndexed { index, (row, col) ->
            cells += PixelCell(row = row, col = col, periodIndex = index + 1)
        }
        // End marker 放在矩阵最后一行的第一个 true 位置
        val lastRow = matrix.last()
        val endCol = lastRow.indexOf(true)
        if (endCol >= 0) {
            cells += PixelCell(row = height - 1, col = endCol, periodIndex = 0, isEndMarker = true)
        }

        return PixelShapeLayout(width = width, height = height, cells = cells)
    }

    private val CABIN_52_MATRIX = arrayOf(
        //  0  1  2  3  4  5  6  7  8  9
        booleanArrayOf(F,F,F,F,T,T,F,F,F,F),  // row 0 — 烟囱
        booleanArrayOf(F,F,F,T,T,T,T,F,F,F),  // row 1
        booleanArrayOf(F,F,T,T,T,T,T,T,F,F),  // row 2
        booleanArrayOf(F,T,T,T,T,T,T,T,T,F),  // row 3
        booleanArrayOf(T,T,T,T,T,T,T,T,T,T),  // row 4 — 屋顶底
        booleanArrayOf(T,T,T,T,T,T,T,T,T,T),  // row 5 — 墙壁
        booleanArrayOf(T,T,T,T,T,T,T,T,T,T),  // row 6
        booleanArrayOf(F,F,F,F,T,T,F,F,F,F),  // row 7 — 门
    )
}

private const val T = true
private const val F = false
