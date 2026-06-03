package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal enum class VideoSharedTransitionPlaybackIntent {
    ImmediatePlayback,
    CoverFirst
}

internal enum class VideoSharedTransitionTargetMode {
    InlineCover,
    InlinePlayer,
    LandscapeFullscreen,
    PortraitFullscreen
}

internal const val VIDEO_SHARED_COVER_ASPECT_RATIO = 16f / 10f
private const val HOME_SOURCE_ROUTE = "home"
private const val HOME_SHARED_TRANSITION_DURATION_MILLIS = 360
internal const val FULLSCREEN_SHARED_TRANSITION_DURATION_MILLIS = 420
private const val HOME_DETAIL_REVEAL_DELAY_MILLIS = 40
private const val HOME_DETAIL_REVEAL_DURATION_MILLIS = 220
private const val HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP = 14
private const val HOME_DETAIL_REVEAL_INITIAL_SCALE = 0.985f
private const val HOME_SHARED_TRANSITION_CARD_CORNER_DP = 16
private const val HOME_SHARED_TRANSITION_PLAYER_CORNER_DP = 12
private const val DEFAULT_VIDEO_CARD_CORNER_DP = 12
private const val DEFAULT_VIDEO_PLAYER_CORNER_DP = 12
private const val DYNAMIC_VIDEO_CARD_CORNER_DP = 10
private const val WATCH_LATER_VIDEO_CARD_CORNER_DP = 8
private val VIDEO_CARD_IOS_LIKE_EASE_OUT = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
internal val VIDEO_RETURN_CROSSFADE_EASING: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
internal val VIDEO_RETURN_FADE_OUT_EASING: Easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

internal data class VideoSharedTransitionOwnership(
    val useCoverSharedBounds: Boolean,
    val useMetadataSharedBounds: Boolean
)

internal data class VideoSharedTransitionMotionSpec(
    val enabled: Boolean,
    val durationMillis: Int,
    val contentDelayMillis: Int,
    val contentDurationMillis: Int,
    val contentSlideOffsetDp: Int,
    val contentInitialScale: Float,
    val easing: Easing
)

internal data class VideoSharedCornerSpec(
    val enabled: Boolean,
    val startCornerDp: Int,
    val endCornerDp: Int
)

internal data class VideoSharedTransitionVisualSpec(
    val targetMode: VideoSharedTransitionTargetMode,
    val sourceCornerDp: Int,
    val targetCornerDp: Int,
    val fillTargetViewport: Boolean,
    val useCoverSharedBounds: Boolean,
    val suppressCoverFade: Boolean
)

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    return VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoCardSharedTransitionEasing(): Easing {
    return VIDEO_CARD_IOS_LIKE_EASE_OUT
}

internal fun resolveVideoSharedTransitionSourceCornerDp(
    sourceRoute: String?,
    fallbackCornerDp: Int = DEFAULT_VIDEO_CARD_CORNER_DP
): Int {
    return when (sourceRoute?.substringBefore("?")) {
        "dynamic",
        "dynamic_detail" -> DYNAMIC_VIDEO_CARD_CORNER_DP
        "watch_later" -> WATCH_LATER_VIDEO_CARD_CORNER_DP
        else -> fallbackCornerDp
    }.coerceAtLeast(0)
}

internal fun resolveVideoSharedTransitionVisualSpec(
    sourceRoute: String?,
    sourceCornerDp: Int = resolveVideoSharedTransitionSourceCornerDp(sourceRoute),
    playbackIntent: VideoSharedTransitionPlaybackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
    fullscreen: Boolean = false,
    autoPortrait: Boolean = false,
    initialVertical: Boolean = false,
    isVerticalVideo: Boolean = false,
    isReturning: Boolean = false,
    playerCornerDp: Int = DEFAULT_VIDEO_PLAYER_CORNER_DP
): VideoSharedTransitionVisualSpec {
    val normalizedSourceRoute = sourceRoute?.substringBefore("?")?.takeIf { it.isNotBlank() }
    val safeSourceCornerDp = sourceCornerDp.coerceAtLeast(0)
    val shouldPreferPortraitTarget = initialVertical || (autoPortrait && isVerticalVideo)
    val targetMode = when {
        isReturning -> VideoSharedTransitionTargetMode.InlineCover
        shouldPreferPortraitTarget -> VideoSharedTransitionTargetMode.PortraitFullscreen
        playbackIntent == VideoSharedTransitionPlaybackIntent.CoverFirst ->
            VideoSharedTransitionTargetMode.InlineCover
        fullscreen -> VideoSharedTransitionTargetMode.LandscapeFullscreen
        else -> VideoSharedTransitionTargetMode.InlinePlayer
    }
    val targetCornerDp = when {
        isReturning -> safeSourceCornerDp
        targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen -> 0
        targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen -> 0
        else -> playerCornerDp.coerceAtLeast(0)
    }

    return VideoSharedTransitionVisualSpec(
        targetMode = targetMode,
        sourceCornerDp = safeSourceCornerDp,
        targetCornerDp = targetCornerDp,
        fillTargetViewport = targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen ||
            targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen,
        useCoverSharedBounds = normalizedSourceRoute != null,
        suppressCoverFade = isReturning
    )
}

private fun resolveVideoSharedTransitionProfile(sourceRoute: String?): VideoSharedTransitionProfile {
    return if (sourceRoute?.substringBefore("?") == HOME_SOURCE_ROUTE) {
        VideoSharedTransitionProfile.COVER_ONLY
    } else {
        VideoSharedTransitionProfile.COVER_AND_METADATA
    }
}

internal fun shouldEnableVideoCoverSharedTransition(
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun shouldEnableVideoMetadataSharedTransition(
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    useCardContainerSharedBounds: Boolean = false,
    profile: VideoSharedTransitionProfile = resolveVideoSharedTransitionProfile()
): Boolean {
    if (!coverSharedEnabled) return false
    // 卡片容器已经承载整体放大/回收时，标题、UP、统计等不要再各自抢独立 sharedBounds。
    if (useCardContainerSharedBounds) return false
    // Keep metadata linked during quick return to avoid cover-only snapback.
    if (isQuickReturnLimited && profile == VideoSharedTransitionProfile.COVER_ONLY) return false
    // Home 源也启用 metadata sharedBounds，标题/头像/UP名独立共享
    return true
}

internal fun resolveVideoSharedTransitionOwnership(
    sourceRoute: String?,
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean
): VideoSharedTransitionOwnership {
    if (!coverSharedEnabled) {
        return VideoSharedTransitionOwnership(
            useCoverSharedBounds = false,
            useMetadataSharedBounds = false
        )
    }

    return VideoSharedTransitionOwnership(
        useCoverSharedBounds = true,
        useMetadataSharedBounds = shouldEnableVideoMetadataSharedTransition(
            coverSharedEnabled = true,
            isQuickReturnLimited = isQuickReturnLimited,
            profile = resolveVideoSharedTransitionProfile(sourceRoute)
        )
    )
}

internal fun resolveVideoCardSharedTransitionMotionSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoSharedTransitionMotionSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    if (!enabled) {
        return VideoSharedTransitionMotionSpec(
            enabled = false,
            durationMillis = 0,
            contentDelayMillis = 0,
            contentDurationMillis = 0,
            contentSlideOffsetDp = 0,
            contentInitialScale = 1f,
            easing = VIDEO_CARD_IOS_LIKE_EASE_OUT
        )
    }

    return VideoSharedTransitionMotionSpec(
        enabled = true,
        durationMillis = HOME_SHARED_TRANSITION_DURATION_MILLIS,
        contentDelayMillis = HOME_DETAIL_REVEAL_DELAY_MILLIS,
        contentDurationMillis = HOME_DETAIL_REVEAL_DURATION_MILLIS,
        contentSlideOffsetDp = HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP,
        contentInitialScale = HOME_DETAIL_REVEAL_INITIAL_SCALE,
        easing = VIDEO_CARD_IOS_LIKE_EASE_OUT
    )
}

internal fun resolveHomeVideoSharedTransitionCornerSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoSharedCornerSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    return if (enabled) {
        VideoSharedCornerSpec(
            enabled = true,
            startCornerDp = HOME_SHARED_TRANSITION_CARD_CORNER_DP,
            endCornerDp = HOME_SHARED_TRANSITION_PLAYER_CORNER_DP
        )
    } else {
        VideoSharedCornerSpec(
            enabled = false,
            startCornerDp = 0,
            endCornerDp = 0
        )
    }
}
