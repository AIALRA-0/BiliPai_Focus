package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle

internal data class HomeTopSearchRowMetrics(
    val height: Dp,
    val tabsSpacing: Dp,
)

/** Resolve reserved search-row space from the same visibility rules used by HomeHeader. */
internal fun resolveHomeTopSearchRowMetrics(
    configuredHeight: Dp,
    configuredTabsSpacing: Dp,
    bottomBarSearchEnabled: Boolean,
    hideTopTabs: Boolean,
    keepTopSearch: Boolean = false,
): HomeTopSearchRowMetrics {
    // Hiding the search pill must not remove avatar/settings when there is no tab row
    // to host them. Both native and glass chrome reserve the same controls-only row.
    val searchLivesInTopChrome = !bottomBarSearchEnabled || keepTopSearch
    return if (searchLivesInTopChrome) {
        HomeTopSearchRowMetrics(configuredHeight, configuredTabsSpacing)
    } else if (hideTopTabs) {
        HomeTopSearchRowMetrics(configuredHeight, 0.dp)
    } else {
        HomeTopSearchRowMetrics(0.dp, 0.dp)
    }
}

/** 首页与顶栏共用实际搜索行的收起距离，隐藏分类或迁移到底栏后不预留不存在的间距。 */
internal fun resolveHomeTopSearchScrollDistance(
    searchBarHeight: Dp,
    searchToTabsSpacing: Dp,
    extraSpacing: Dp,
    hideTopTabs: Boolean,
): Dp = if (searchBarHeight > 0.dp) {
    searchBarHeight + (if (hideTopTabs) 0.dp else searchToTabsSpacing) + extraSpacing
} else {
    0.dp
}

/**
 * 顶 Tab 与首个内容的紧凑收紧量。
 *
 * 顶部 Dock 的槽位沿用底栏 Dock 的 64dp 壳高,而 MD3 非玻璃下的可见下划线行明显矮于
 * 该槽位,导致 Tab 下划线与内容之间出现额外的空隙。MD3 非玻璃时从预留 padding 中
 * 收紧这一差值;玻璃 Dock 与 MIUIX 的可见壳高与槽位一致,不收紧。
 */
internal fun resolveHomeTabsToContentTighteningDp(
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean,
): Dp = if (uiStyle == AppUiStyle.MATERIAL3 && !liquidGlassEnabled) {
    12.dp
} else {
    0.dp
}
