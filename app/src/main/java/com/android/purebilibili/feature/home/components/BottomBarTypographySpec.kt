package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

internal fun resolveBottomBarSkinDockLabelFontSize(): TextUnit = 12.sp

internal fun resolveBottomBarSkinDockLabelLineHeight(): TextUnit = 18.sp

/** 图标+文字：胶囊内当 caption，保持 labelSmall。 */
internal fun resolveFloatingDockIconAndTextLabelFontSize(): TextUnit = 11.sp

/**
 * 液态玻璃仅文字：指示器是 52dp 胶囊，labelSmall 会显得空。
 * 15sp 能填满两字标签，四字如「插件中心」仍进得了 68dp 槽。
 */
internal fun resolveFloatingDockTextOnlyLabelFontSize(): TextUnit = 15.sp

internal fun resolveFloatingDockLabelFontSize(
    showIcon: Boolean,
    showText: Boolean,
): TextUnit {
    if (!showText) return 0.sp
    return if (showIcon) {
        resolveFloatingDockIconAndTextLabelFontSize()
    } else {
        resolveFloatingDockTextOnlyLabelFontSize()
    }
}
