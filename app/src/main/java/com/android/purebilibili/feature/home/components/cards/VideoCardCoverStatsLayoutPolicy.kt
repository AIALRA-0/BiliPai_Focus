package com.android.purebilibili.feature.home.components.cards

private const val COMPACT_STAT_BADGE_SPACING_DP = 6f
private const val COMPACT_DURATION_BADGE_SPACING_DP = 6f
private const val COMPACT_ONLINE_BADGE_MIN_WIDTH_DP = 52f
private const val HISTORY_PROGRESS_BAR_HEIGHT_DP = 2f
private const val HISTORY_PROGRESS_BAR_CLEARANCE_DP = 2f
private const val COMPACT_STATS_BASE_BOTTOM_PADDING_DP = 6f
private const val FLOATING_DURATION_BASE_BOTTOM_PADDING_DP = 10f

internal data class VideoCardCompactCoverStatsLayout(
    val primaryMinWidthDp: Float,
    val secondaryMinWidthDp: Float,
    val showSecondaryStat: Boolean,
    val showOnlineCount: Boolean,
    val statsEndPaddingDp: Float
)

internal data class VideoCardCoverOverlayBottomLayout(
    val historyProgressBarHeightDp: Float,
    val compactStatsBottomPaddingDp: Float,
    val floatingDurationBottomPaddingDp: Float
)

internal fun resolveVideoCardPrimaryStatBadgeMinWidthDp(
    statText: String
): Float {
    val normalizedLength = statText.trim().length.coerceAtLeast(3)
    return (34f + normalizedLength * 6f).coerceIn(52f, 72f)
}

internal fun resolveVideoCardSecondaryStatBadgeMinWidthDp(
    statText: String
): Float {
    val normalizedLength = statText.trim().length.coerceAtLeast(3)
    return (40f + normalizedLength * 6f).coerceIn(58f, 76f)
}

internal fun resolveVideoCardCompactCoverStatsLayout(
    availableWidthDp: Float,
    primaryStatText: String,
    secondaryStatText: String?,
    hasOnlineCount: Boolean,
    durationBadgeMinWidthDp: Float = 0f
): VideoCardCompactCoverStatsLayout {
    val primaryMinWidth = resolveVideoCardPrimaryStatBadgeMinWidthDp(primaryStatText)
    val showSecondary = !secondaryStatText.isNullOrBlank()
    val secondaryMinWidth = if (showSecondary) {
        resolveVideoCardSecondaryStatBadgeMinWidthDp(secondaryStatText.orEmpty())
    } else {
        0f
    }
    val durationReserveWidth = if (durationBadgeMinWidthDp > 0f) {
        durationBadgeMinWidthDp + COMPACT_DURATION_BADGE_SPACING_DP
    } else {
        0f
    }
    val statsAvailableWidth = (availableWidthDp - durationReserveWidth).coerceAtLeast(0f)
    val requiredForPrimaryAndSecondary = primaryMinWidth +
        if (showSecondary) COMPACT_STAT_BADGE_SPACING_DP + secondaryMinWidth else 0f
    val requiredWithOnline = requiredForPrimaryAndSecondary +
        if (hasOnlineCount) COMPACT_STAT_BADGE_SPACING_DP + COMPACT_ONLINE_BADGE_MIN_WIDTH_DP else 0f

    return VideoCardCompactCoverStatsLayout(
        primaryMinWidthDp = primaryMinWidth,
        secondaryMinWidthDp = secondaryMinWidth,
        showSecondaryStat = showSecondary,
        // 在线人数是补充信息；空间不足时先让位给播放量和弹幕/评论数。
        showOnlineCount = hasOnlineCount && statsAvailableWidth >= requiredWithOnline,
        statsEndPaddingDp = durationReserveWidth
    )
}

internal fun resolveVideoCardCoverOverlayBottomLayout(
    showHistoryProgressBar: Boolean
): VideoCardCoverOverlayBottomLayout {
    val progressReserve = if (showHistoryProgressBar) {
        HISTORY_PROGRESS_BAR_HEIGHT_DP + HISTORY_PROGRESS_BAR_CLEARANCE_DP
    } else {
        0f
    }
    return VideoCardCoverOverlayBottomLayout(
        historyProgressBarHeightDp = HISTORY_PROGRESS_BAR_HEIGHT_DP,
        compactStatsBottomPaddingDp = COMPACT_STATS_BASE_BOTTOM_PADDING_DP + progressReserve,
        floatingDurationBottomPaddingDp = FLOATING_DURATION_BASE_BOTTOM_PADDING_DP + progressReserve
    )
}
