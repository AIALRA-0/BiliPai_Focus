package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.graphics.Color

data class TabletVideoLayoutPolicy(
    val primaryRatio: Float,
    val playerMaxWidthDp: Int,
    val infoMaxWidthDp: Int
)

data class TabletCinemaLayoutPolicy(
    val curtainPeekWidthDp: Int,
    val curtainOpenWidthDp: Int,
    val horizontalPaddingDp: Int,
    val playerMaxWidthDp: Int
)

internal enum class CinemaMetaPanelBlock {
    ACTIONS,
    UP_INFO,
    INTRO,
    COLLECTION,
    PAGES
}

internal enum class TabletSideCurtainState {
    HIDDEN,
    PEEK,
    OPEN
}

internal enum class TabletSecondaryPaneMode {
    EXPANDED,
    COMPACT,
    COLLAPSED
}

internal fun nextTabletSecondaryPaneMode(
    current: TabletSecondaryPaneMode
): TabletSecondaryPaneMode {
    return when (current) {
        TabletSecondaryPaneMode.EXPANDED -> TabletSecondaryPaneMode.COMPACT
        TabletSecondaryPaneMode.COMPACT -> TabletSecondaryPaneMode.COLLAPSED
        TabletSecondaryPaneMode.COLLAPSED -> TabletSecondaryPaneMode.EXPANDED
    }
}

internal fun resolveTabletPrimaryRatio(
    basePrimaryRatio: Float,
    secondaryPaneMode: TabletSecondaryPaneMode
): Float {
    return when (secondaryPaneMode) {
        TabletSecondaryPaneMode.EXPANDED -> basePrimaryRatio
        TabletSecondaryPaneMode.COMPACT -> (basePrimaryRatio + 0.08f).coerceAtMost(0.80f)
        TabletSecondaryPaneMode.COLLAPSED -> (basePrimaryRatio + 0.14f).coerceAtMost(0.86f)
    }
}

internal fun resolveTabletPrimaryRatio(
    basePrimaryRatio: Float,
    secondaryCollapsed: Boolean
): Float {
    return resolveTabletPrimaryRatio(
        basePrimaryRatio = basePrimaryRatio,
        secondaryPaneMode = if (secondaryCollapsed) {
            TabletSecondaryPaneMode.COLLAPSED
        } else {
            TabletSecondaryPaneMode.EXPANDED
        }
    )
}

fun resolveTabletVideoLayoutPolicy(
    widthDp: Int
): TabletVideoLayoutPolicy {
    return when {
        widthDp >= 1600 -> TabletVideoLayoutPolicy(
            primaryRatio = 0.66f,
            playerMaxWidthDp = 1240,
            infoMaxWidthDp = 1160
        )
        else -> TabletVideoLayoutPolicy(
            primaryRatio = 0.72f,
            playerMaxWidthDp = 1080,
            infoMaxWidthDp = 1000
        )
    }
}

fun resolveTabletCinemaLayoutPolicy(
    widthDp: Int
): TabletCinemaLayoutPolicy {
    val normalizedWidth = widthDp.coerceIn(960, 1800)
    val curtainOpenWidthDp = interpolateByWidth(
        widthDp = normalizedWidth,
        minWidthDp = 960,
        maxWidthDp = 1800,
        minValue = 320,
        maxValue = 480
    )
    val curtainPeekWidthDp = interpolateByWidth(
        widthDp = normalizedWidth,
        minWidthDp = 960,
        maxWidthDp = 1800,
        minValue = 56,
        maxValue = 74
    )
    val horizontalPaddingDp = interpolateByWidth(
        widthDp = normalizedWidth,
        minWidthDp = 960,
        maxWidthDp = 1800,
        minValue = 12,
        maxValue = 24
    )
    val playerMaxWidthDp = interpolateByWidth(
        widthDp = normalizedWidth,
        minWidthDp = 960,
        maxWidthDp = 1800,
        minValue = 980,
        maxValue = 1280
    )

    return TabletCinemaLayoutPolicy(
        curtainPeekWidthDp = curtainPeekWidthDp,
        curtainOpenWidthDp = curtainOpenWidthDp,
        horizontalPaddingDp = horizontalPaddingDp,
        playerMaxWidthDp = playerMaxWidthDp
    )
}

internal fun resolveCurtainWidthDp(
    state: TabletSideCurtainState,
    policy: TabletCinemaLayoutPolicy
): Int {
    return when (state) {
        TabletSideCurtainState.HIDDEN -> 0
        TabletSideCurtainState.PEEK -> policy.curtainPeekWidthDp
        TabletSideCurtainState.OPEN -> policy.curtainOpenWidthDp
    }
}

internal fun resolveInitialCurtainState(widthDp: Int): TabletSideCurtainState {
    return if (widthDp >= 960) {
        TabletSideCurtainState.OPEN
    } else {
        TabletSideCurtainState.PEEK
    }
}

internal fun resolveCurtainStateAfterAutoBehavior(
    currentState: TabletSideCurtainState,
    isActivelyPlaying: Boolean
): TabletSideCurtainState {
    return when {
        isActivelyPlaying && currentState == TabletSideCurtainState.OPEN -> {
            TabletSideCurtainState.PEEK
        }
        !isActivelyPlaying && currentState == TabletSideCurtainState.HIDDEN -> {
            TabletSideCurtainState.PEEK
        }
        else -> currentState
    }
}

internal fun resolveCinemaSideCurtainSelectedTab(
    currentSelectedTab: Int,
    replyCount: Int,
    isRepliesLoading: Boolean,
    hasRelatedVideos: Boolean,
    showRelatedVideosSection: Boolean
): Int {
    if (!showRelatedVideosSection) {
        return 0
    }

    return if (
        currentSelectedTab == 0 &&
        replyCount == 0 &&
        !isRepliesLoading &&
        hasRelatedVideos
    ) {
        1
    } else {
        currentSelectedTab
    }
}

internal fun shouldShowVideoRelatedVideosSection(
    showRelatedVideosSection: Boolean,
    relatedVideoCount: Int
): Boolean {
    return showRelatedVideosSection && relatedVideoCount > 0
}

internal fun resolveTabletSecondaryDefaultTab(
    replyCount: Int,
    hasRelatedVideos: Boolean,
    showRelatedVideosSection: Boolean
): Int {
    return if (replyCount == 0 && hasRelatedVideos && showRelatedVideosSection) 1 else 0
}

internal fun resolveTabletSecondaryTabs(
    replyCount: Int,
    showRelatedVideosSection: Boolean
): List<String> {
    val commentsTabTitle = if (replyCount > 0) {
        "评论 ($replyCount)"
    } else {
        "评论"
    }
    return buildList {
        add(commentsTabTitle)
        if (showRelatedVideosSection) {
            add("相关推荐")
        }
    }
}

internal fun resolveVideoCommentEmptyStateHint(
    hasRelatedVideos: Boolean,
    showRelatedVideosSection: Boolean
): String? {
    return if (hasRelatedVideos && showRelatedVideosSection) {
        "先看看相关推荐"
    } else {
        null
    }
}

internal fun resolveVideoCommentEmptyStateActionLabel(
    hasRelatedVideos: Boolean,
    showRelatedVideosSection: Boolean
): String? {
    return if (hasRelatedVideos && showRelatedVideosSection) {
        "切换到相关推荐"
    } else {
        null
    }
}

internal fun resolveCinemaMetaPanelContainerColor(
    isDarkTheme: Boolean,
    surfaceColor: Color
): Color {
    return if (isDarkTheme) {
        surfaceColor.copy(alpha = 0.92f)
    } else {
        Color.White
    }
}

internal fun resolveCinemaIntroCardContainerColor(
    isDarkTheme: Boolean,
    surfaceContainerLowColor: Color
): Color {
    return if (isDarkTheme) {
        surfaceContainerLowColor.copy(alpha = 0.96f)
    } else {
        Color.White
    }
}

internal fun resolveCinemaMetaPanelBlocks(
    hasOwner: Boolean,
    hasCollection: Boolean,
    hasMultiplePages: Boolean
): List<CinemaMetaPanelBlock> {
    return buildList {
        add(CinemaMetaPanelBlock.ACTIONS)
        if (hasOwner) {
            add(CinemaMetaPanelBlock.UP_INFO)
        }
        add(CinemaMetaPanelBlock.INTRO)
        if (hasCollection) {
            add(CinemaMetaPanelBlock.COLLECTION)
        }
        if (hasMultiplePages) {
            add(CinemaMetaPanelBlock.PAGES)
        }
    }
}

private fun interpolateByWidth(
    widthDp: Int,
    minWidthDp: Int,
    maxWidthDp: Int,
    minValue: Int,
    maxValue: Int
): Int {
    if (widthDp <= minWidthDp) return minValue
    if (widthDp >= maxWidthDp) return maxValue
    val progress = (widthDp - minWidthDp).toFloat() / (maxWidthDp - minWidthDp).toFloat()
    return (minValue + (maxValue - minValue) * progress).toInt()
}
