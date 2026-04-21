package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.util.FormatUtils

internal fun resolveUpStatsText(
    followerCount: Int?,
    videoCount: Int?
): String? {
    val parts = mutableListOf<String>()
    val safeFollowerCount = followerCount?.takeIf { it > 0 }
    val safeVideoCount = videoCount?.takeIf { it > 0 }

    if (safeFollowerCount != null) {
        parts += "粉丝 ${FormatUtils.formatStat(safeFollowerCount.toLong())}"
    }
    if (safeVideoCount != null) {
        parts += "视频 ${FormatUtils.formatStat(safeVideoCount.toLong())}"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun shouldRenderUpBadgeTrailingSlot(
    hasTrailingContent: Boolean,
    reserveTrailingSlot: Boolean
): Boolean {
    return hasTrailingContent || reserveTrailingSlot
}

internal fun shouldRenderUserUpBadge(showUpBadge: Boolean): Boolean {
    return showUpBadge
}
