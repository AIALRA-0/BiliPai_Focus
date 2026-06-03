package com.android.purebilibili.feature.home.components.cards

internal enum class HomeVideoBadgeStyle {
    GLASS,
    PLAIN
}

internal data class HomeVideoGlassBadgeStylePolicy(
    val coverStyle: HomeVideoBadgeStyle,
    val infoStyle: HomeVideoBadgeStyle
)

internal fun resolveHomeVideoGlassBadgeStylePolicy(
    showCoverGlassBadges: Boolean,
    showInfoGlassBadges: Boolean
): HomeVideoGlassBadgeStylePolicy = HomeVideoGlassBadgeStylePolicy(
    // 玻璃标签已退役：旧偏好仍可能存在，但不再影响首页、搜索、历史等卡片标签样式。
    coverStyle = HomeVideoBadgeStyle.PLAIN,
    infoStyle = HomeVideoBadgeStyle.PLAIN
)
