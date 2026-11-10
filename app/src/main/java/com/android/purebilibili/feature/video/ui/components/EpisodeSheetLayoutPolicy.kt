package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

internal fun resolveEpisodeSheetHeightDp(availableHeightDp: Int, landscape: Boolean): Int {
    val height = availableHeightDp.coerceAtLeast(0)
    val fraction = if (landscape) 0.85f else 0.6f
    return minOf((height * fraction).roundToInt(), if (landscape) 480 else 560)
}

/** 使用当前窗口和安全区，适配旋转、分屏与大屏，不读取设备固定屏幕尺寸。 */
@Composable
internal fun episodeSheetMaxHeight(): Dp {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val insets = WindowInsets.safeDrawing
    val availableHeight = (window.height - insets.getTop(density) - insets.getBottom(density)).coerceAtLeast(0)
    return with(density) {
        Dp(resolveEpisodeSheetHeightDp(availableHeight.toDp().value.toInt(), window.width > window.height).toFloat())
    }
}
