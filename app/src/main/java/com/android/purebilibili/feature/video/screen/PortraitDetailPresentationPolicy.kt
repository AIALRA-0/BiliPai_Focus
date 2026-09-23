package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class PortraitInlinePlayerLayoutSpec(
    val widthDp: Float,
    val heightDp: Float
)

internal data class StandalonePortraitPagerMotionSpec(
    val enterDurationMillis: Int,
    val exitDurationMillis: Int,
    val exitScaleTarget: Float,
    val exitTranslateUpFraction: Float,
    val inlineReturnDurationMillis: Int,
    val inlineReturnInitialScale: Float
)

internal enum class PortraitFullscreenButtonAction {
    ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseOfficialInlinePortraitDetailExperience(
    useTabletLayout: Boolean,
    isVerticalVideo: Boolean,
    portraitExperienceEnabled: Boolean,
    directPortraitEntry: Boolean = false
): Boolean {
    // 「竖屏直达」走 card→全屏 morph，不能先落官方内联详情再二次跳进 pager。
    if (directPortraitEntry) return false
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
        enterDurationMillis = 0,
        exitDurationMillis = 220,
        exitScaleTarget = 0.96f,
        exitTranslateUpFraction = 0.08f,
        inlineReturnDurationMillis = 240,
        inlineReturnInitialScale = 0.985f
    )
}

internal fun shouldEnableInlinePortraitScrollTransform(
    collapseMode: PortraitPlayerCollapseMode,
    selectedTabIndex: Int,
    isVerticalVideo: Boolean = true,
    isPlaybackPaused: Boolean = false
): Boolean {
    if (!collapseMode.enablesVideoOrientation(isVerticalVideo)) return false
    if (collapseMode == PortraitPlayerCollapseMode.PAUSED_ONLY && !isPlaybackPaused) return false
    return when (selectedTabIndex) {
        0 -> collapseMode.enablesIntro
        1 -> collapseMode.enablesComment
        else -> true
    }
}

/**
 * Whether the standalone portrait pager should cross-fade on entry.
 *
 * The detail player and fullscreen pager share one player. Cross-fading the old inline
 * viewport with the new pager makes swipe-to-fullscreen visibly pause in a centered frame.
 * Keep entry direct for every source; exit still owns its independent soft transition.
 */
internal fun shouldAnimateStandalonePortraitPager(
    useSharedPlayer: Boolean,
    directPortraitEntry: Boolean = false
): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = useSharedPlayer
    @Suppress("UNUSED_PARAMETER")
    val ignoredDirectEntry = directPortraitEntry
    return false
}

/**
 * When true, keep the phone detail body fully suppressed from the first frame of a
 * direct-portrait morph so only the full-bleed shell + entry cover are visible.
 */
internal fun shouldSuppressPhoneDetailBodyForDirectPortraitEntry(
    directPortraitEntry: Boolean,
    isPortraitFullscreen: Boolean
): Boolean {
    return directPortraitEntry && isPortraitFullscreen
}

/**
 * Standalone portrait pager covers the phone detail body. Suppress that body so inline
 * VideoPlayerSection does not dual-host the shared player under the pager.
 */
internal fun shouldSuppressPhoneDetailBodyUnderStandalonePortraitPager(
    portraitExperienceEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    hasPlayableState: Boolean,
): Boolean {
    return portraitExperienceEnabled &&
        isPortraitFullscreen &&
        hasPlayableState
}

internal fun resolvePortraitFullscreenButtonAction(
    useOfficialInlinePortraitDetailExperience: Boolean
): PortraitFullscreenButtonAction {
    return PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseCompactInlinePortraitPlayerForCommentTab(
    useOfficialInlinePortraitDetailExperience: Boolean,
    selectedTabIndex: Int,
    isPortraitFullscreen: Boolean,
    isCommentThreadVisible: Boolean = false,
    collapseMode: PortraitPlayerCollapseMode = PortraitPlayerCollapseMode.BOTH,
    isVerticalVideo: Boolean = true,
    isPlaybackPaused: Boolean = false
): Boolean {
    // Switching between 简介/评论 must keep the inline portrait player visible. The player is
    // collapsed only by the detail list gesture, which also allows an upward drag to restore it.
    return false
}

internal fun shouldUseCompactInlinePortraitPlayerForIntroScroll(
    useOfficialInlinePortraitDetailExperience: Boolean,
    selectedTabIndex: Int,
    isPortraitFullscreen: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    collapseMode: PortraitPlayerCollapseMode = PortraitPlayerCollapseMode.BOTH,
    isVerticalVideo: Boolean = true,
    isPlaybackPaused: Boolean = false,
    introScrollThresholdPx: Int = 56
): Boolean {
    if (!useOfficialInlinePortraitDetailExperience || isPortraitFullscreen) return false
    if (!collapseMode.enablesVideoOrientation(isVerticalVideo)) return false
    if (collapseMode == PortraitPlayerCollapseMode.PAUSED_ONLY && !isPlaybackPaused) return false
    if (!collapseMode.enablesIntro) return false
    if (selectedTabIndex != 0) return false
    return isVideoDetailIntroScrollPastCollapseThreshold(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        thresholdPx = introScrollThresholdPx
    )
}

internal fun isVideoDetailIntroScrollPastCollapseThreshold(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    thresholdPx: Int = 56
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset >= thresholdPx
}

internal fun resolveInlinePortraitPlayerCollapseProgress(
    manualCollapseProgress: Float,
    compactForCommentTabProgress: Float,
    restoreRequested: Boolean = false
): Float {
    if (restoreRequested) return 0f
    // The list threshold drives the state holder to its compact offset. Rendering must follow
    // that manual offset afterwards so an upward drag can restore the player immediately.
    return manualCollapseProgress.coerceIn(0f, 1f)
}

internal fun resolveInlinePortraitPlayerCommentCollapseDurationMillis(
    tabSwitchAnimationSpec: VideoContentTabSwitchAnimationSpec
): Int {
    return tabSwitchAnimationSpec.durationMs
}

/**
 * Keep the collapsed portrait player on a full-width 16:9 canvas. The vertical media remains
 * centered inside the black canvas while the detail tabs move directly below it.
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolvePiliPlusCollapsedPlayerViewportHeightDp(
    standardCollapsedHeightDp: Float,
    collapseMode: PortraitPlayerCollapseMode,
    isPlaybackPaused: Boolean,
    toolbarHeightDp: Float = 56f,
    mediaPeekHeightDp: Float = 56f,
): Float {
    return standardCollapsedHeightDp.coerceAtLeast(0f)
}

/**
 * Inline portrait detail player size.
 *
 * BiliPai parity for phone: expanded ≈ max(longestSide * 0.65, shortestSide),
 * so vertical videos get a tall preview without becoming full-screen cards.
 * Wide foldable portrait windows stay capped so intro/comment remain reachable.
 */
internal fun resolvePortraitInlinePlayerLayoutSpec(
    screenWidthDp: Float,
    screenHeightDp: Float,
    isCollapsed: Boolean,
    isFoldableCoverWindow: Boolean = false,
): PortraitInlinePlayerLayoutSpec {
    val width = screenWidthDp
    val standardCollapsedHeight = screenWidthDp * 9f / 16f
    val collapsedHeight = if (isFoldableCoverWindow && screenHeightDp > 0f) {
        min(standardCollapsedHeight, screenHeightDp * FOLDABLE_COVER_COMPACT_PLAYER_HEIGHT_FRACTION)
    } else {
        standardCollapsedHeight
    }
    if (isCollapsed) {
        return PortraitInlinePlayerLayoutSpec(
            widthDp = width,
            heightDp = collapsedHeight
        )
    }

    val shortestSide = min(screenWidthDp, screenHeightDp)
    val longestSide = max(screenWidthDp, screenHeightDp)
    val isWidePortraitWindow = screenWidthDp >= 600f && screenHeightDp > screenWidthDp
    val expandedHeight = if (isWidePortraitWindow) {
        // 折叠屏内屏竖屏窗口不能按手机竖屏体验撑满首屏，否则详情区入口会被播放器挤出。
        min(max(screenHeightDp * 0.52f, collapsedHeight), screenWidthDp)
    } else {
        max(longestSide * 0.65f, shortestSide)
    }
    val coverExpandedHeightLimit = if (
        isFoldableCoverWindow &&
        screenHeightDp > 0f &&
        screenHeightDp < FOLDABLE_COVER_COMPACT_HEIGHT_MAX_DP
    ) {
        screenHeightDp * FOLDABLE_COVER_COMPACT_PLAYER_HEIGHT_FRACTION
    } else {
        Float.POSITIVE_INFINITY
    }
    return PortraitInlinePlayerLayoutSpec(
        widthDp = width,
        heightDp = min(expandedHeight, coverExpandedHeightLimit)
    )
}

/**
 * 横屏 16:9 详情播放器内容高度：按**真实布局宽度**算 9/16，
 * 避免 configuration.screenWidthDp 与窗口可用宽度不一致时左右黑边。
 */
internal fun resolveLandscapeDetailPlayerContentHeightPx(
    layoutWidthPx: Int,
): Int {
    val width = layoutWidthPx.coerceAtLeast(1)
    return ((width * 9f) / 16f).roundToInt().coerceAtLeast(1)
}
