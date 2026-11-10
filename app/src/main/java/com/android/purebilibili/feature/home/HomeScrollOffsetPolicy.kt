package com.android.purebilibili.feature.home

import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

@Suppress("UNUSED_PARAMETER")
internal fun resolveNextHomeGlobalScrollOffset(
    currentOffset: Float,
    scrollDeltaY: Float,
    liquidGlassEnabled: Boolean,
    minUpdateDeltaPx: Float = 0.5f
): Float? {
    // Bottom dock motion also consumes this offset without liquid glass.
    if (abs(scrollDeltaY) < minUpdateDeltaPx) return null
    return currentOffset - scrollDeltaY
}

/**
 * 把 nested-scroll 增量连续累计到 [LocalHomeScrollOffset]。
 * 动态/历史/收藏等页若只上报 firstVisibleItem/offset，滑过第 0 项后偏移会饱和，
 * 底栏搜索胶囊的 24dp 阈值就再也凑不满。
 */
internal fun createContinuousScrollOffsetConnection(
    offsetState: MutableFloatState,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val next = resolveNextHomeGlobalScrollOffset(
            currentOffset = offsetState.floatValue,
            scrollDeltaY = available.y,
            liquidGlassEnabled = false,
        )
        if (next != null) {
            offsetState.floatValue = next
        }
        return Offset.Zero
    }
}
