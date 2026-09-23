package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.util.WindowWidthSizeClass

/** Information-density decisions kept separate from the selected visual card style. */
internal data class HomeFeedCardDensityPolicy(
    val compactMetadata: Boolean,
    val titleMinLines: Int,
    val titleMaxLines: Int,
    val compactStatsOnCover: Boolean,
)

internal fun resolveHomeFeedCardDensityPolicy(
    style: HomeFeedCardStyle,
    gridColumns: Int,
    widthSizeClass: WindowWidthSizeClass,
): HomeFeedCardDensityPolicy {
    val denseLargeGrid = widthSizeClass >= WindowWidthSizeClass.Expanded && gridColumns >= 6
    val styleUsesCompactMetadata = style != HomeFeedCardStyle.CURRENT

    return HomeFeedCardDensityPolicy(
        compactMetadata = styleUsesCompactMetadata || denseLargeGrid,
        // Do not reserve a blank second line for short titles on dense tablet grids.
        titleMinLines = if (denseLargeGrid) 1 else 2,
        titleMaxLines = 2,
        // Counts remain available on the cover while the redundant metadata row is removed.
        compactStatsOnCover = denseLargeGrid,
    )
}
