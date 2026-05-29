package com.aifinance.feature.savings_goal

/**
 * 与 HTML 参考一致的行定义：left 方块 + gap 空白 + right 方块，在 maxWidth 列网格中居中。
 */
data class RowDef(
    val left: Int,
    val gap: Int = 0,
    val right: Int = 0,
    val isEndRow: Boolean = false,
)

enum class SavingsColoringShape {
    HEART,
    /** 365 天：小狗 | 52 周：小木屋 */
    ALT,
}

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

private enum class EndStyle {
    /** 365 爱心：365 个编号格 + 独立 end 行 */
    SEPARATE_END_ROW,
    /** 52 周：共 N 格，最后一格为第 N 期且显示 end */
    MARK_LAST_BLOCK,
    /** 365 小狗：共 N+1 格，前 N 格编号，最后一格为 end */
    TRAILING_END_CELL,
}

object SavingsPixelShapes {

    private const val HEART_365_MAX_WIDTH = 25
    private const val DOG_365_MAX_WIDTH = 26
    private const val SHAPE_52_MAX_WIDTH = 10

    fun layoutFor(shape: SavingsColoringShape, totalPeriods: Int): PixelShapeLayout {
        require(totalPeriods > 0)
        return when (shape) {
            SavingsColoringShape.HEART -> when (totalPeriods) {
                52 -> buildFromHtmlRows(
                    rowDefs = heart52Rows(),
                    maxWidth = SHAPE_52_MAX_WIDTH,
                    totalPeriods = totalPeriods,
                    endStyle = EndStyle.MARK_LAST_BLOCK,
                )
                else -> buildFromHtmlRows(
                    rowDefs = heart365Rows(),
                    maxWidth = HEART_365_MAX_WIDTH,
                    totalPeriods = totalPeriods,
                    endStyle = EndStyle.SEPARATE_END_ROW,
                )
            }
            SavingsColoringShape.ALT -> when (totalPeriods) {
                52 -> buildFromHtmlRows(
                    rowDefs = cabin52Rows(),
                    maxWidth = SHAPE_52_MAX_WIDTH,
                    totalPeriods = totalPeriods,
                    endStyle = EndStyle.MARK_LAST_BLOCK,
                )
                else -> buildFromHtmlRows(
                    rowDefs = dog365Rows(),
                    maxWidth = DOG_365_MAX_WIDTH,
                    totalPeriods = totalPeriods,
                    endStyle = EndStyle.TRAILING_END_CELL,
                )
            }
        }
    }

    /** 366 方块爱心（HTML 原版结构） */
    private fun heart365Rows(): List<RowDef> = listOf(
        RowDef(left = 6, gap = 7, right = 6),
        RowDef(left = 8, gap = 5, right = 8),
        RowDef(left = 10, gap = 3, right = 10),
        RowDef(left = 12, gap = 1, right = 12),
        RowDef(left = 25),
        RowDef(left = 25),
        RowDef(left = 25),
        RowDef(left = 25),
        RowDef(left = 25),
        RowDef(left = 25),
        RowDef(left = 23),
        RowDef(left = 21),
        RowDef(left = 19),
        RowDef(left = 17),
        RowDef(left = 15),
        RowDef(left = 13),
        RowDef(left = 11),
        RowDef(left = 9),
        RowDef(left = 7),
        RowDef(left = 5),
        RowDef(left = 3),
        RowDef(left = 1, isEndRow = true),
    )

    /** 366 方块小狗 */
    private fun dog365Rows(): List<RowDef> = listOf(
        RowDef(left = 4, gap = 10, right = 4),
        RowDef(left = 6, gap = 8, right = 6),
        RowDef(left = 7, gap = 6, right = 7),
        RowDef(left = 8, gap = 4, right = 8),
        RowDef(left = 22),
        RowDef(left = 24),
        RowDef(left = 26),
        RowDef(left = 24),
        RowDef(left = 22),
        RowDef(left = 20),
        RowDef(left = 16),
        RowDef(left = 14),
        RowDef(left = 18),
        RowDef(left = 20),
        RowDef(left = 22),
        RowDef(left = 24),
        RowDef(left = 24),
        RowDef(left = 24),
        RowDef(left = 8, gap = 8, right = 8),
    )

    /** 52 周迷你爱心 */
    private fun heart52Rows(): List<RowDef> = listOf(
        RowDef(left = 2, gap = 2, right = 2),
        RowDef(left = 4, right = 4),
        RowDef(left = 10),
        RowDef(left = 10),
        RowDef(left = 8),
        RowDef(left = 6),
        RowDef(left = 4),
        RowDef(left = 2),
    )

    /** 52 周温馨小木屋 */
    private fun cabin52Rows(): List<RowDef> = listOf(
        RowDef(left = 2),
        RowDef(left = 4),
        RowDef(left = 6),
        RowDef(left = 8),
        RowDef(left = 10),
        RowDef(left = 6),
        RowDef(left = 6),
        RowDef(left = 6),
        RowDef(left = 4),
    )

    private fun buildFromHtmlRows(
        rowDefs: List<RowDef>,
        maxWidth: Int,
        totalPeriods: Int,
        endStyle: EndStyle,
    ): PixelShapeLayout {
        val blockSlots = mutableListOf<Pair<Int, Int>>()
        var endRowSlot: Pair<Int, Int>? = null

        rowDefs.forEachIndexed { row, def ->
            if (def.isEndRow) {
                val indent = (maxWidth - def.left) / 2
                endRowSlot = row to (indent)
                return@forEachIndexed
            }
            val rowWidth = def.left + def.gap + def.right
            val indent = (maxWidth - rowWidth) / 2
            var col = indent
            repeat(def.left) {
                blockSlots += row to col
                col++
            }
            col += def.gap
            repeat(def.right) {
                blockSlots += row to col
                col++
            }
        }

        val cells = mutableListOf<PixelCell>()

        when (endStyle) {
            EndStyle.SEPARATE_END_ROW -> {
                require(blockSlots.size >= totalPeriods) {
                    "Shape has ${blockSlots.size} blocks, need $totalPeriods"
                }
                blockSlots.take(totalPeriods).forEachIndexed { index, (row, col) ->
                    cells += PixelCell(row = row, col = col, periodIndex = index + 1)
                }
                endRowSlot?.let { (row, col) ->
                    cells += PixelCell(row = row, col = col, periodIndex = 0, isEndMarker = true)
                }
            }
            EndStyle.MARK_LAST_BLOCK -> {
                require(blockSlots.size == totalPeriods) {
                    "Shape has ${blockSlots.size} blocks, expected $totalPeriods"
                }
                blockSlots.forEachIndexed { index, (row, col) ->
                    val period = index + 1
                    val isLast = index == blockSlots.lastIndex
                    cells += PixelCell(
                        row = row,
                        col = col,
                        periodIndex = period,
                        isEndMarker = isLast,
                    )
                }
            }
            EndStyle.TRAILING_END_CELL -> {
                require(blockSlots.size == totalPeriods + 1) {
                    "Shape has ${blockSlots.size} blocks, expected ${totalPeriods + 1}"
                }
                blockSlots.take(totalPeriods).forEachIndexed { index, (row, col) ->
                    cells += PixelCell(row = row, col = col, periodIndex = index + 1)
                }
                val (endRow, endCol) = blockSlots[totalPeriods]
                cells += PixelCell(row = endRow, col = endCol, periodIndex = 0, isEndMarker = true)
            }
        }

        return PixelShapeLayout(
            width = maxWidth,
            height = cells.maxOf { it.row } + 1,
            cells = cells,
        )
    }
}
