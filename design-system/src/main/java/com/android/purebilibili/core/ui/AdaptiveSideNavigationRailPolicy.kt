package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle

enum class AdaptiveSideNavigationRailRenderer {
    MATERIAL3,
    MIUIX
}

fun resolveAdaptiveSideNavigationRailRenderer(
    uiStyle: AppUiStyle,
): AdaptiveSideNavigationRailRenderer = when (uiStyle) {
    AppUiStyle.MIUIX -> AdaptiveSideNavigationRailRenderer.MIUIX
    AppUiStyle.MATERIAL3 -> AdaptiveSideNavigationRailRenderer.MATERIAL3
}

fun shouldUseExpandableMiuixNavigationRail(isExpandedWidthClass: Boolean): Boolean =
    isExpandedWidthClass
