package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.BottomBarScrollHideIntent
import com.android.purebilibili.core.ui.BottomBarScrollHideState
import com.android.purebilibili.core.ui.BottomBarScrollHideTopRevealPx
import com.android.purebilibili.core.ui.reduceBottomBarScrollHideDelta
import com.android.purebilibili.core.ui.shouldAutoHideBottomBarOnScroll

internal val DynamicBottomBarTopRevealPx = BottomBarScrollHideTopRevealPx

internal typealias DynamicBottomBarScrollState = BottomBarScrollHideState

internal typealias DynamicBottomBarScrollIntent = BottomBarScrollHideIntent

/**
 * 向下浏览时隐藏：由子页面用滚动增量驱动，瀑布流/列表一并生效。
 */
internal fun shouldAutoCollapseDynamicBottomBar(
    visibilityMode: SettingsManager.BottomBarVisibilityMode,
): Boolean {
    return shouldAutoHideBottomBarOnScroll(visibilityMode)
}

/**
 * 用 nested-scroll 增量推断底栏显隐。
 *
 * 瀑布流首个可见 item 会在 lane 间切换，不能用 index 判断方向；
 * available.y / consumed.y 与布局锚点无关，平板与折叠屏多列下同样稳定。
 * 正 y 表示向上滚回顶部，负 y 表示向下浏览。
 */
internal fun reduceDynamicBottomBarScrollDelta(
    previousState: DynamicBottomBarScrollState,
    deltaY: Float,
    isAtTop: Boolean,
    thresholdPx: Float = com.android.purebilibili.core.ui.BottomBarScrollHideDirectionThresholdPx,
): com.android.purebilibili.core.ui.BottomBarScrollHideUpdate {
    return reduceBottomBarScrollHideDelta(
        previousState = previousState,
        deltaY = deltaY,
        isAtTop = isAtTop,
        thresholdPx = thresholdPx,
    )
}
