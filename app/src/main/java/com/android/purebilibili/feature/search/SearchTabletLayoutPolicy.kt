package com.android.purebilibili.feature.search

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import com.android.purebilibili.core.util.WindowWidthSizeClass
import com.android.purebilibili.feature.home.resolveHomeFeedGridColumns

fun resolveSearchMaxContentWidth(): Dp = 1280.dp

fun resolveSearchSingleColumnResultMaxWidth(): Dp = 840.dp

fun resolveSearchContentWidth(
    isExpandedScreen: Boolean,
    widthDp: Dp
): Dp = if (isExpandedScreen) {
    minOf(widthDp, resolveSearchMaxContentWidth())
} else {
    widthDp
}

internal fun resolveSearchVideoGridColumns(
    singleColumn: Boolean,
    contentWidthDp: Int,
    fixedColumnCount: Int = 0,
    cardWidthPreset: HomeFeedCardWidthPreset = HomeFeedCardWidthPreset.AUTO,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Medium
): Int {
    if (singleColumn) {
        return 1
    }
    return resolveHomeFeedGridColumns(
        contentWidthDp = contentWidthDp,
        displayMode = 0,
        fixedColumnCount = fixedColumnCount,
        cardWidthPreset = cardWidthPreset,
        widthSizeClass = widthSizeClass
    )
}

internal fun resolveSearchGridCardWidthDp(
    availableWidthDp: Float,
    minItemWidthDp: Float,
    horizontalPaddingDp: Float,
    spacingDp: Float,
    fixedColumnCount: Int? = null,
): Float {
    val contentWidth = (availableWidthDp - 2 * horizontalPaddingDp).coerceAtLeast(0f)
    val columns = fixedColumnCount?.coerceAtLeast(1) ?: ((contentWidth + spacingDp) / (minItemWidthDp + spacingDp))
        .toInt().coerceAtLeast(1)
    return ((contentWidth - spacingDp * (columns - 1)) / columns).coerceAtLeast(0f)
}

data class SearchLayoutPolicy(
    val resultGridMinItemWidthDp: Int,
    val resultGridSpacingDp: Int,
    val resultHorizontalPaddingDp: Int,
    val splitOuterPaddingDp: Int,
    val splitInnerGapDp: Int,
    val leftPaneWeight: Float,
    val rightPaneWeight: Float,
    val hotSearchColumns: Int
)

fun shouldUseSearchSplitLayout(
    widthDp: Int
): Boolean = widthDp >= 840

fun resolveSearchLayoutPolicy(
    widthDp: Int
): SearchLayoutPolicy {
    return when {
        widthDp >= 1600 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 260,
            resultGridSpacingDp = 16,
            resultHorizontalPaddingDp = 24,
            splitOuterPaddingDp = 32,
            splitInnerGapDp = 16,
            leftPaneWeight = 1.15f,
            rightPaneWeight = 0.85f,
            hotSearchColumns = 4
        )
        widthDp >= 840 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 220,
            resultGridSpacingDp = 12,
            resultHorizontalPaddingDp = 20,
            splitOuterPaddingDp = 24,
            splitInnerGapDp = 12,
            leftPaneWeight = 1.05f,
            rightPaneWeight = 0.95f,
            hotSearchColumns = 3
        )
        widthDp >= 600 -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 200,
            resultGridSpacingDp = 12,
            resultHorizontalPaddingDp = 16,
            splitOuterPaddingDp = 20,
            splitInnerGapDp = 12,
            leftPaneWeight = 1f,
            rightPaneWeight = 1f,
            hotSearchColumns = 2
        )
        else -> SearchLayoutPolicy(
            resultGridMinItemWidthDp = 160,
            resultGridSpacingDp = 8,
            resultHorizontalPaddingDp = 8,
            splitOuterPaddingDp = 16,
            splitInnerGapDp = 8,
            leftPaneWeight = 1f,
            rightPaneWeight = 1f,
            hotSearchColumns = 2
        )
    }
}
