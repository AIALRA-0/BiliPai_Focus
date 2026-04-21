package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class HomeTopTabGestureAction {
    NONE,
    COLLAPSE,
    EXPAND
}

internal fun resolveHomeTopTabGestureAction(
    dragDeltaPx: Float,
    isCollapsed: Boolean,
    thresholdPx: Float
): HomeTopTabGestureAction {
    if (thresholdPx <= 0f) return HomeTopTabGestureAction.NONE
    return when {
        !isCollapsed && dragDeltaPx >= thresholdPx -> HomeTopTabGestureAction.COLLAPSE
        isCollapsed && dragDeltaPx <= -thresholdPx -> HomeTopTabGestureAction.EXPAND
        else -> HomeTopTabGestureAction.NONE
    }
}

internal fun resolveHomeTopCollapsedHandleHeight(): Dp = 12.dp

internal fun resolveHomeTopTabsAutoCollapsed(
    currentHeaderOffsetPx: Float,
    isHeaderCollapseEnabled: Boolean,
    collapseThresholdPx: Float = 0.5f
): Boolean {
    if (!isHeaderCollapseEnabled) return false
    return currentHeaderOffsetPx <= -collapseThresholdPx
}

internal fun resolveHomeTopTabPresentationHeight(
    expandedHeight: Dp,
    isCollapsed: Boolean,
    collapsedHandleHeight: Dp = resolveHomeTopCollapsedHandleHeight()
): Dp {
    return if (isCollapsed) collapsedHandleHeight else expandedHeight
}
