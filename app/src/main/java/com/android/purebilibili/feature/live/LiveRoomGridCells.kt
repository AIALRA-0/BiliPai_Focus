package com.android.purebilibili.feature.live

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/** 根据网格实际获得的空间计算列宽，侧栏、分屏与窗口内边距均由布局阶段扣除。 */
internal data class LiveRoomGridCells(val maxCardWidth: Dp = 240.dp) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val width = availableSize.coerceAtLeast(0)
        val columns = ceil(
            (width - spacing).coerceAtLeast(1).toDouble() /
                (maxCardWidth.roundToPx().coerceAtLeast(1) + spacing),
        ).toInt().coerceAtLeast(1)
        val usable = (width - spacing * (columns - 1)).coerceAtLeast(0)
        return List(columns) { usable / columns + if (it < usable % columns) 1 else 0 }
    }
}
