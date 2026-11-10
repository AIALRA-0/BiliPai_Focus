package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppEntryIconPalette

/**
 * 设置页同级条目的多彩色板，与偏好分组内的入口彩色分配共用同一份定义。
 *
 * Miuix 主题沿用这套七彩入口色；Material 3 下同级图标统一请求中性色，
 * 由偏好渲染层按语义色解析。
 */
internal val SettingsSiblingIconPalette: List<Color> = AppEntryIconPalette

/**
 * @param colorful Miuix 传 true 取七彩入口色，其余主题传 false 请求中性色。
 */
internal fun resolveSettingsSiblingIconTint(
    siblingIndex: Int,
    paletteOffset: Int = 0,
    colorful: Boolean = false,
): Color {
    if (!colorful) return Color.Unspecified
    val paletteIndex = Math.floorMod(siblingIndex + paletteOffset, SettingsSiblingIconPalette.size)
    return SettingsSiblingIconPalette[paletteIndex]
}

internal fun resolveSettingsSiblingIconTints(
    siblingCount: Int,
    paletteOffset: Int = 0,
    colorful: Boolean = false,
): List<Color> {
    require(siblingCount >= 0) { "Sibling count must not be negative" }
    return List(siblingCount) { index ->
        resolveSettingsSiblingIconTint(index, paletteOffset, colorful)
    }
}

/** 按当前界面风格解析同级图标色：Miuix 保留七彩入口色，Material 3 使用中性色。 */
@Composable
internal fun rememberSettingsSiblingIconTints(
    siblingCount: Int,
    paletteOffset: Int = 0,
): List<Color> {
    val colorful = LocalAppUiStyle.current == AppUiStyle.MIUIX
    return remember(siblingCount, paletteOffset, colorful) {
        resolveSettingsSiblingIconTints(siblingCount, paletteOffset, colorful)
    }
}
