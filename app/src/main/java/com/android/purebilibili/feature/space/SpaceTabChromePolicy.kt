package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.VideoSortOrder
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.resolveCompactCapsuleChromeSpec
import kotlin.math.roundToInt

internal data class SpaceSegmentedTabChromeSpec(
    val selectedIndex: Int,
    val heightDp: Int,
    val indicatorHeightDp: Int,
    val horizontalPaddingDp: Int,
    val itemWidthDp: Int?,
    val scrollable: Boolean,
    val liquidGlassEffectsEnabled: Boolean,
    val dragSelectionEnabled: Boolean
)

internal data class SpaceContributionToolbarSpec(
    val tabHeightDp: Int,
    val tabIndicatorHeightDp: Int,
    val collapsedTabWidthDp: Int,
    val expandedTabRailHeightDp: Int,
    val horizontalPaddingDp: Int,
    val showVideoActions: Boolean,
    val showTotalText: Boolean,
    val showPlayAllText: Boolean,
    val showSortText: Boolean,
    val collapseAfterTabSelection: Boolean
)

private const val SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP = 16
private const val SPACE_SCROLLABLE_CONTRIBUTION_ITEM_MIN_WIDTH_DP = 104
private const val SPACE_SCROLLABLE_CONTRIBUTION_ITEM_TEXT_PADDING_DP = 44
private const val SPACE_SCROLLABLE_CONTRIBUTION_CJK_CHAR_WIDTH_DP = 15
private const val SPACE_SCROLLABLE_CONTRIBUTION_ASCII_CHAR_WIDTH_DP = 8
private const val SPACE_CONTRIBUTION_TOOLBAR_COMPACT_WIDTH_DP = 430
private const val SPACE_CONTRIBUTION_TOOLBAR_ROOMY_WIDTH_DP = 480

internal fun resolveSpaceMainTabChromeSpec(
    tabs: List<SpaceMainTabItem>,
    selectedTab: SpaceMainTab
): SpaceSegmentedTabChromeSpec {
    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    val compactChrome = resolveCompactCapsuleChromeSpec(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    return SpaceSegmentedTabChromeSpec(
        selectedIndex = selectedIndex,
        heightDp = compactChrome.primaryHeightDp,
        indicatorHeightDp = 30,
        horizontalPaddingDp = SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP,
        itemWidthDp = null,
        scrollable = false,
        liquidGlassEffectsEnabled = true,
        dragSelectionEnabled = true
    )
}

internal fun resolveSpaceContributionTabChromeSpec(
    tabs: List<SpaceContributionTab>,
    selectedTabId: String,
    selectedSubTab: SpaceSubTab
): SpaceSegmentedTabChromeSpec {
    val selectedIndex = tabs.indexOfFirst { it.id == selectedTabId }
        .takeIf { it >= 0 }
        ?: tabs.indexOfFirst { it.subTab == selectedSubTab }.coerceAtLeast(0)
    val scrollable = shouldScrollSpaceContributionTabs(tabs)
    val compactChrome = resolveCompactCapsuleChromeSpec(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    return SpaceSegmentedTabChromeSpec(
        selectedIndex = selectedIndex,
        heightDp = compactChrome.primaryHeightDp,
        indicatorHeightDp = 30,
        horizontalPaddingDp = SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP,
        itemWidthDp = resolveSpaceContributionTabItemWidthDp(tabs),
        scrollable = scrollable,
        liquidGlassEffectsEnabled = true,
        dragSelectionEnabled = !scrollable
    )
}

internal fun resolveSpaceContributionToolbarSpec(
    widthDp: Int,
    selectedSubTab: SpaceSubTab,
    tabCount: Int,
    selectedTitle: String = ""
): SpaceContributionToolbarSpec {
    val showVideoActions = selectedSubTab == SpaceSubTab.VIDEO ||
        selectedSubTab == SpaceSubTab.CHARGING_VIDEO
    val compactActions = widthDp < SPACE_CONTRIBUTION_TOOLBAR_COMPACT_WIDTH_DP || tabCount > 2
    val roomy = widthDp >= SPACE_CONTRIBUTION_TOOLBAR_ROOMY_WIDTH_DP && tabCount <= 2
    return SpaceContributionToolbarSpec(
        tabHeightDp = 40,
        tabIndicatorHeightDp = 34,
        collapsedTabWidthDp = resolveSpaceContributionCollapsedTabWidthDp(selectedTitle, widthDp),
        expandedTabRailHeightDp = 40,
        horizontalPaddingDp = 12,
        showVideoActions = showVideoActions,
        showTotalText = showVideoActions && roomy,
        showPlayAllText = showVideoActions && !compactActions,
        showSortText = showVideoActions && !compactActions,
        collapseAfterTabSelection = true
    )
}

internal fun resolveSpaceContributionCollapsedTabWidthDp(title: String, widthDp: Int): Int {
    val normalizedTitle = title.trim()
    val minimumWidth = if (normalizedTitle.length <= 2) 88 else 104
    val maximumWidth = minOf(156, (widthDp * 0.45f).roundToInt().coerceAtLeast(minimumWidth))
    val containsWideText = normalizedTitle.any { it.code > 127 }
    val estimatedWidth = if (containsWideText) {
        estimateSpaceContributionTabTitleWidthDp(normalizedTitle)
    } else {
        minimumWidth
    }
    return estimatedWidth.coerceIn(minimumWidth, maximumWidth)
}

internal fun resolveSpaceVideoSortCompactLabel(order: VideoSortOrder): String {
    return when (order) {
        VideoSortOrder.PUBDATE -> "最新"
        VideoSortOrder.OLDEST_PUBDATE -> "最早"
        VideoSortOrder.CLICK -> "播放"
        VideoSortOrder.STOW -> "收藏"
    }
}

private fun shouldScrollSpaceContributionTabs(tabs: List<SpaceContributionTab>): Boolean {
    return tabs.size > 3
}

internal fun resolveSpaceContributionTabItemWidthDp(tabs: List<SpaceContributionTab>): Int {
    val widestTitle = tabs.maxOfOrNull { estimateSpaceContributionTabTitleWidthDp(it.title) } ?: 0
    return widestTitle.coerceAtLeast(SPACE_SCROLLABLE_CONTRIBUTION_ITEM_MIN_WIDTH_DP)
}

internal fun resolveSpaceContributionTabCenteredScrollOffsetPx(
    selectedIndex: Int,
    itemWidthPx: Float,
    viewportWidthPx: Float
): Int {
    if (selectedIndex <= 0 || itemWidthPx <= 0f || viewportWidthPx <= 0f) return 0
    val itemStartPx = selectedIndex * itemWidthPx
    return (itemStartPx - (viewportWidthPx - itemWidthPx) / 2f)
        .roundToInt()
        .coerceAtLeast(0)
}

private fun estimateSpaceContributionTabTitleWidthDp(title: String): Int {
    val textWidth = title.sumOf { char ->
        if (char.code in 0..127) {
            SPACE_SCROLLABLE_CONTRIBUTION_ASCII_CHAR_WIDTH_DP
        } else {
            SPACE_SCROLLABLE_CONTRIBUTION_CJK_CHAR_WIDTH_DP
        }
    }
    return textWidth + SPACE_SCROLLABLE_CONTRIBUTION_ITEM_TEXT_PADDING_DP
}
