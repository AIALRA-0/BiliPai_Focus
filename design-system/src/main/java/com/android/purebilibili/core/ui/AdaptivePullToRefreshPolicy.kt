package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle

fun resolveAdaptivePullToRefreshRenderer(
    uiStyle: AppUiStyle
): PresetPrimitiveRenderer {
    return resolvePresetPrimitiveRenderer(uiStyle)
}

fun resolveMiuixPullToRefreshTexts(): List<String> = listOf(
    "下拉刷新...",
    "松手刷新",
    "正在刷新...",
    "刷新完成",
)

/**
 * Top inset (dp) for the pull-to-refresh indicator relative to the
 * [AdaptivePullToRefreshBox] top edge.
 *
 * Use the **height of chrome that overlays the refresh box** (status bar + floating
 * top bar / home header). When the box is already laid out *below* a Scaffold
 * topBar (body already padded), pass **0**.
 *
 * Do **not** reuse home [listTopPadding] on other screens — each surface has its
 * own chrome stack.
 */
fun resolvePullRefreshIndicatorTopInsetDp(overlayChromeHeightDp: Float): Float {
    return overlayChromeHeightDp.coerceAtLeast(0f)
}

/**
 * Scaffold body already receives topBar padding → indicator sits at the box top.
 */
fun resolveScaffoldedPullRefreshIndicatorTopInsetDp(): Float = 0f
