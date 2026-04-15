package com.android.purebilibili.feature.video.screen

import kotlin.math.max

internal data class PortraitInlinePlayerLayoutSpec(
    val widthDp: Float,
    val heightDp: Float
)

internal data class StandalonePortraitPagerMotionSpec(
    val enterDurationMillis: Int,
    val exitDurationMillis: Int,
    val exitScaleTarget: Float
)

internal enum class PortraitFullscreenButtonAction {
    ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseOfficialInlinePortraitDetailExperience(
    useTabletLayout: Boolean,
    isVerticalVideo: Boolean,
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled && !useTabletLayout && isVerticalVideo
}

internal fun shouldUseSharedPlayerForPortraitFullscreen(): Boolean {
    return true
}

internal fun shouldShowStandalonePortraitPager(
    portraitExperienceEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    useOfficialInlinePortraitDetailExperience: Boolean,
    hasPlayableState: Boolean
): Boolean {
    return portraitExperienceEnabled &&
        isPortraitFullscreen &&
        hasPlayableState
}

internal fun shouldActivatePortraitFullscreenState(
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled
}

internal fun resolveStandalonePortraitPagerMotionSpec(): StandalonePortraitPagerMotionSpec {
    return StandalonePortraitPagerMotionSpec(
        enterDurationMillis = 220,
        exitDurationMillis = 180,
        exitScaleTarget = 0.98f
    )
}

internal fun shouldEnableInlinePortraitScrollTransform(
    swipeHidePlayerEnabled: Boolean,
    useOfficialInlinePortraitDetailExperience: Boolean
): Boolean {
    return swipeHidePlayerEnabled
}

internal fun shouldAnimateStandalonePortraitPager(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun resolvePortraitFullscreenButtonAction(
    useOfficialInlinePortraitDetailExperience: Boolean
): PortraitFullscreenButtonAction {
    return PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseCompactInlinePortraitPlayerForCommentTab(
    useOfficialInlinePortraitDetailExperience: Boolean,
    selectedTabIndex: Int,
    isPortraitFullscreen: Boolean
): Boolean {
    return useOfficialInlinePortraitDetailExperience &&
        selectedTabIndex == 1 &&
        !isPortraitFullscreen
}

internal fun resolveInlinePortraitPlayerCollapseProgress(
    manualCollapseProgress: Float,
    compactForCommentTabProgress: Float
): Float {
    return manualCollapseProgress
        .coerceIn(0f, 1f)
        .coerceAtLeast(compactForCommentTabProgress.coerceIn(0f, 1f))
}

internal fun resolvePortraitInlinePlayerLayoutSpec(
    screenWidthDp: Float,
    screenHeightDp: Float,
    isCollapsed: Boolean
): PortraitInlinePlayerLayoutSpec {
    val width = screenWidthDp
    val collapsedHeight = screenWidthDp * 9f / 16f
    if (isCollapsed) {
        return PortraitInlinePlayerLayoutSpec(
            widthDp = width,
            heightDp = collapsedHeight
        )
    }

    val expandedHeight = max(screenHeightDp * 0.65f, screenWidthDp)
    return PortraitInlinePlayerLayoutSpec(
        widthDp = width,
        heightDp = expandedHeight
    )
}
