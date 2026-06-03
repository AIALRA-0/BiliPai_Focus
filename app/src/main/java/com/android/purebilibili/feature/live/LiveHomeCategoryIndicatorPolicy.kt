package com.android.purebilibili.feature.live

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.resolveCompactCapsuleChromeSpec

internal data class LiveCategorySegmentedControlSpec(
    val itemWidthDp: Int?,
    val heightDp: Int,
    val indicatorHeightDp: Int,
    val labelFontSizeSp: Int,
    val containerHorizontalPaddingDp: Int,
    val containerVerticalPaddingDp: Int
)

internal fun resolveLiveHomeCategorySelectedIndex(
    selectedAreaId: Int,
    areaIds: List<Int>
): Int {
    if (selectedAreaId == 0) return 0
    val areaIndex = areaIds.indexOf(selectedAreaId)
    return if (areaIndex >= 0) areaIndex + 1 else 0
}

internal fun resolveLiveHomeCategorySegmentedControlSpec(): LiveCategorySegmentedControlSpec {
    val compactChrome = resolveCompactCapsuleChromeSpec(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    return LiveCategorySegmentedControlSpec(
        itemWidthDp = 82,
        heightDp = compactChrome.primaryHeightDp,
        indicatorHeightDp = 30,
        labelFontSizeSp = 14,
        containerHorizontalPaddingDp = 4,
        containerVerticalPaddingDp = 4
    )
}

internal fun resolveLiveHomeCategoryFollowScrollTarget(
    indicatorPosition: Float,
    itemWidthPx: Float,
    itemCount: Int,
    viewportWidthPx: Float,
    currentScrollPx: Float,
    maxScrollPx: Float,
    edgeBufferPx: Float
): Int {
    if (itemWidthPx <= 0f || itemCount <= 0 || viewportWidthPx <= 0f || maxScrollPx <= 0f) {
        return currentScrollPx.toInt().coerceIn(0, maxScrollPx.toInt().coerceAtLeast(0))
    }

    val clampedPosition = indicatorPosition.coerceIn(0f, (itemCount - 1).toFloat())
    val itemStartPx = clampedPosition * itemWidthPx
    val itemEndPx = itemStartPx + itemWidthPx
    val viewportEndPx = currentScrollPx + viewportWidthPx
    val rawTarget = when {
        itemStartPx - edgeBufferPx < currentScrollPx -> itemStartPx - edgeBufferPx
        itemEndPx + edgeBufferPx > viewportEndPx -> itemEndPx + edgeBufferPx - viewportWidthPx
        else -> currentScrollPx
    }

    return rawTarget.toInt().coerceIn(0, maxScrollPx.toInt())
}

internal fun resolveLiveAreaParentSegmentedControlSpec(): LiveCategorySegmentedControlSpec {
    val compactChrome = resolveCompactCapsuleChromeSpec(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    return LiveCategorySegmentedControlSpec(
        itemWidthDp = 112,
        heightDp = compactChrome.primaryHeightDp,
        indicatorHeightDp = 30,
        labelFontSizeSp = 16,
        containerHorizontalPaddingDp = 4,
        containerVerticalPaddingDp = 4
    )
}
