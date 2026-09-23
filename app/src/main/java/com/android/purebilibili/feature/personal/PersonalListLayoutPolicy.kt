package com.android.purebilibili.feature.personal

import com.android.purebilibili.feature.home.components.cards.HORIZONTAL_VIDEO_CARD_COVER_ASPECT_RATIO
import com.android.purebilibili.feature.home.components.cards.HORIZONTAL_VIDEO_CARD_COVER_WIDTH_DP
import kotlin.math.ceil
import kotlin.math.max

internal const val PERSONAL_LIST_MAX_ITEM_WIDTH_DP = 480f
internal const val PERSONAL_LIST_BASE_MIN_HEIGHT_DP = 90f
/** PiliPlus `Style.aspectRatio` for history/later horizontal covers. */
internal const val PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO =
    HORIZONTAL_VIDEO_CARD_COVER_ASPECT_RATIO
internal const val PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP = HORIZONTAL_VIDEO_CARD_COVER_WIDTH_DP
internal const val PERSONAL_LIST_POSTER_ASPECT_RATIO = 3f / 4f

/**
 * Mirrors BiliPai' max-cross-axis-extent behavior: secondary personal lists
 * add another column once the available width exceeds the 480dp item target.
 */
internal fun resolvePersonalListColumnCount(
    availableWidthDp: Float,
    horizontalSpacingDp: Float = 12f,
): Int {
    if (availableWidthDp <= 0f) return 1
    val safeSpacing = horizontalSpacingDp.coerceAtLeast(0f)
    return ceil(
        (availableWidthDp + safeSpacing) /
            (PERSONAL_LIST_MAX_ITEM_WIDTH_DP + safeSpacing)
    ).toInt().coerceAtLeast(1)
}

internal fun resolvePersonalListItemWidthDp(
    availableWidthDp: Float,
    columnCount: Int,
    horizontalSpacingDp: Float = 12f,
): Float {
    val safeColumns = columnCount.coerceAtLeast(1)
    val totalSpacing = horizontalSpacingDp.coerceAtLeast(0f) * (safeColumns - 1)
    return ((availableWidthDp - totalSpacing) / safeColumns).coerceAtLeast(0f)
}

internal fun resolvePersonalMediaCardMinHeightDp(fontScale: Float): Float =
    PERSONAL_LIST_BASE_MIN_HEIGHT_DP * max(1f, fontScale)
