package com.android.purebilibili.feature.video.ui.components

internal data class PlayerSettingsMenuLayoutPolicy(
    val maxWidthDp: Int,
    val maxHeightDp: Int,
)

/** 玻璃列表保留紧凑宽度并优先利用高度；弹幕编辑面板与原生面板沿用自身尺寸。 */
internal fun resolvePlayerSettingsMenuLayoutPolicy(
    screenWidthDp: Int,
    screenHeightDp: Int,
): PlayerSettingsMenuLayoutPolicy {
    val landscape = screenWidthDp > screenHeightDp
    return PlayerSettingsMenuLayoutPolicy(
        maxWidthDp = minOf(if (landscape) 280 else 264, screenWidthDp - 32).coerceAtLeast(1),
        // 只限制玻璃菜单：优先利用窗口高度，实际安全区由覆盖层再收紧。
        maxHeightDp = (screenHeightDp - 32).coerceAtLeast(1),
    )
}
