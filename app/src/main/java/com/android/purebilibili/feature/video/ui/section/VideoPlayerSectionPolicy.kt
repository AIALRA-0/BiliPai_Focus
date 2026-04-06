package com.android.purebilibili.feature.video.ui.section

import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlin.math.abs

enum class VideoGestureMode { None, Brightness, Volume, Seek, SwipeToFullscreen }

private const val HI_RES_AUDIO_QUALITY_ID = 30251
private const val HI_RES_LONG_PRESS_SPEED_LIMIT = 1.5f
private const val PLAYER_DRAG_GESTURE_BOTTOM_EXCLUSION_BUFFER_DP = 12
private const val PLAYBACK_STALL_LOG_THRESHOLD_MS = 700L
private const val VIDEO_PLAYER_COVER_FADE_ENTER_DURATION_MILLIS = 200
private const val VIDEO_PLAYER_COVER_FADE_EXIT_DURATION_MILLIS = 300
private const val VIDEO_PLAYER_COVER_REVEAL_HOLD_DELAY_MILLIS = 96
private const val VIDEO_PLAYER_SURFACE_REVEAL_DURATION_MILLIS = 220
private const val VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE = 0.985f

internal const val LONG_PRESS_SPEED_LOCK_THRESHOLD_DP = 72
internal const val FOREGROUND_SURFACE_RECOVERY_DELAY_MS = 80L
internal const val FOREGROUND_SURFACE_RECOVERY_TIMEOUT_MS = 1200L

internal data class LongPressSpeedStartDecision(
    val originalPlaybackParameters: PlaybackParameters,
    val targetPlaybackParameters: PlaybackParameters,
    val clearExistingLock: Boolean
)

internal fun resolveGestureSeekableDurationMs(
    playbackDurationMs: Long,
    fallbackDurationMs: Long
): Long {
    return if (playbackDurationMs > 0L) {
        playbackDurationMs
    } else {
        fallbackDurationMs.coerceAtLeast(0L)
    }
}

internal fun resolveVideoPlayerBottomGestureExclusionHeightDp(
    controlBarBottomPaddingDp: Int,
    progressSpacingDp: Int,
    progressTouchHeightDp: Int,
    controlRowHeightDp: Int,
    extraBufferDp: Int = PLAYER_DRAG_GESTURE_BOTTOM_EXCLUSION_BUFFER_DP
): Int {
    return (
        controlBarBottomPaddingDp +
            progressSpacingDp +
            progressTouchHeightDp +
            controlRowHeightDp +
            extraBufferDp
        ).coerceAtLeast(0)
}

internal fun shouldIgnoreVideoPlayerDragStart(
    offsetY: Float,
    containerHeightPx: Float,
    edgeSafeZonePx: Float,
    bottomGestureExclusionPx: Float
): Boolean {
    if (containerHeightPx <= 0f) return false
    val clampedBottomExclusionPx = bottomGestureExclusionPx.coerceIn(0f, containerHeightPx)
    val inEdgeSafeZone = offsetY < edgeSafeZonePx || offsetY > (containerHeightPx - edgeSafeZonePx)
    val inBottomGestureExclusionZone = clampedBottomExclusionPx > 0f &&
        offsetY >= (containerHeightPx - clampedBottomExclusionPx)
    return inEdgeSafeZone || inBottomGestureExclusionZone
}

internal fun resolveEffectiveLongPressSpeed(
    requestedSpeed: Float,
    currentAudioQuality: Int,
    hiResSpeedLimit: Float = HI_RES_LONG_PRESS_SPEED_LIMIT
): Float {
    val normalized = requestedSpeed.coerceAtLeast(0.1f)
    return if (currentAudioQuality == HI_RES_AUDIO_QUALITY_ID) {
        normalized.coerceAtMost(hiResSpeedLimit)
    } else {
        normalized
    }
}

internal fun resolveLongPressPlaybackParameters(
    requestedSpeed: Float,
    currentAudioQuality: Int
): PlaybackParameters {
    return PlaybackParameters(
        resolveEffectiveLongPressSpeed(
            requestedSpeed = requestedSpeed,
            currentAudioQuality = currentAudioQuality
        ),
        1.0f
    )
}

internal fun resolveLongPressSpeedStartDecision(
    currentPlaybackParameters: PlaybackParameters,
    previousOriginalPlaybackParameters: PlaybackParameters,
    longPressSpeedLocked: Boolean,
    requestedSpeed: Float,
    currentAudioQuality: Int
): LongPressSpeedStartDecision {
    return LongPressSpeedStartDecision(
        originalPlaybackParameters = if (longPressSpeedLocked) {
            previousOriginalPlaybackParameters
        } else {
            currentPlaybackParameters
        },
        targetPlaybackParameters = resolveLongPressPlaybackParameters(
            requestedSpeed = requestedSpeed,
            currentAudioQuality = currentAudioQuality
        ),
        clearExistingLock = longPressSpeedLocked
    )
}

internal fun shouldShowHiResLongPressCompatHint(
    requestedSpeed: Float,
    effectiveSpeed: Float,
    hasShownHint: Boolean
): Boolean {
    if (hasShownHint) return false
    return requestedSpeed - effectiveSpeed > 0.001f
}

internal fun shouldEnableLongPressSpeedGesture(
    isScreenLocked: Boolean,
    scale: Float,
    isMultiTouchActive: Boolean
): Boolean {
    return !isScreenLocked && !isMultiTouchActive && scale <= 1.01f
}

internal fun shouldEnableViewportTransformGesture(
    isScreenLocked: Boolean
): Boolean {
    // Disable pinch-to-zoom/pan during playback to avoid accidental viewport
    // distortion while keeping aspect ratio changes inside the explicit menu.
    return false
}

internal fun shouldLockLongPressSpeedBySwipe(
    isLongPressing: Boolean,
    alreadyLocked: Boolean,
    totalDragDistanceY: Float,
    thresholdPx: Float
): Boolean {
    return isLongPressing &&
        !alreadyLocked &&
        thresholdPx > 0f &&
        totalDragDistanceY <= -thresholdPx
}

internal fun shouldRestorePlaybackParametersAfterLongPressRelease(
    wasLongPressing: Boolean,
    longPressSpeedLocked: Boolean,
    gestureEnded: Boolean
): Boolean {
    return gestureEnded && wasLongPressing && !longPressSpeedLocked
}

internal fun resolveVerticalGestureMode(
    isFullscreen: Boolean,
    isSwipeUp: Boolean,
    startX: Float,
    leftZoneEnd: Float,
    rightZoneStart: Float,
    portraitSwipeToFullscreenEnabled: Boolean,
    centerSwipeToFullscreenEnabled: Boolean,
    slideVolumeBrightnessEnabled: Boolean = true
): VideoGestureMode {
    if (!isFullscreen && portraitSwipeToFullscreenEnabled && isSwipeUp) {
        return VideoGestureMode.SwipeToFullscreen
    }
    if (!slideVolumeBrightnessEnabled && startX < leftZoneEnd) {
        return VideoGestureMode.None
    }
    if (!slideVolumeBrightnessEnabled && startX > rightZoneStart) {
        return VideoGestureMode.None
    }
    return when {
        startX < leftZoneEnd -> VideoGestureMode.Brightness
        startX > rightZoneStart -> VideoGestureMode.Volume
        else -> if (centerSwipeToFullscreenEnabled) {
            VideoGestureMode.SwipeToFullscreen
        } else {
            VideoGestureMode.None
        }
    }
}

internal fun shouldShowDanmakuLayers(
    isInPipMode: Boolean,
    danmakuEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    pipNoDanmakuEnabled: Boolean,
    hostLifecycleStarted: Boolean
): Boolean {
    if (!hostLifecycleStarted) return false
    if (!danmakuEnabled || isPortraitFullscreen) return false
    if (isInPipMode && pipNoDanmakuEnabled) return false
    return true
}

internal fun resolveDanmakuLayerTopOffsetPx(
    isFullscreen: Boolean,
    statusBarHeightPx: Int
): Int {
    return 0
}

internal fun resolveHorizontalSeekDeltaMs(
    isFullscreen: Boolean,
    fullscreenSwipeSeekEnabled: Boolean,
    totalDragDistanceX: Float,
    containerWidthPx: Float,
    fullscreenSwipeSeekSeconds: Int?,
    gestureSensitivity: Float
): Long? {
    if (isFullscreen && fullscreenSwipeSeekEnabled) {
        val seekSeconds = fullscreenSwipeSeekSeconds ?: return null
        val stepWidthPx = (containerWidthPx / 8f).coerceAtLeast(1f)
        val stepCount = (totalDragDistanceX / stepWidthPx).toInt()
        val steppedDelta = stepCount * seekSeconds * 1000L
        if (steppedDelta != 0L) return steppedDelta
    }
    return (totalDragDistanceX * 200f * gestureSensitivity).toLong()
}

internal fun shouldCommitGestureSeek(
    currentPositionMs: Long,
    targetPositionMs: Long,
    minDeltaMs: Long = 300L
): Boolean {
    return abs(targetPositionMs - currentPositionMs) >= minDeltaMs
}

internal fun resolveOrientationSwitchHintText(isFullscreen: Boolean): String {
    return if (isFullscreen) "已切换到横屏" else "已切换到竖屏"
}

internal fun shouldTriggerFullscreenBySwipe(
    isFullscreen: Boolean,
    reverseGesture: Boolean,
    totalDragDistanceY: Float,
    thresholdPx: Float
): Boolean {
    if (thresholdPx <= 0f) return false
    val isSwipeUp = totalDragDistanceY < -thresholdPx
    val isSwipeDown = totalDragDistanceY > thresholdPx
    return if (!isFullscreen) {
        if (reverseGesture) isSwipeDown else isSwipeUp
    } else {
        if (reverseGesture) isSwipeUp else isSwipeDown
    }
}

internal fun shouldAllowPlaybackStateAutoFullscreen(
    smallestScreenWidthDp: Int
): Boolean {
    return smallestScreenWidthDp < 600
}

internal fun resolveGestureIndicatorLabel(mode: VideoGestureMode): String {
    return when (mode) {
        VideoGestureMode.Brightness -> "亮度"
        VideoGestureMode.Volume -> "音量"
        else -> ""
    }
}

internal fun resolveGestureDisplayIcon(
    mode: VideoGestureMode,
    percent: Float,
    fallbackIcon: ImageVector?
): ImageVector {
    val normalizedPercent = percent.coerceIn(0f, 1f)
    return when (mode) {
        VideoGestureMode.Brightness -> when {
            normalizedPercent < 0.34f -> CupertinoIcons.Outlined.SunMax
            else -> CupertinoIcons.Default.SunMax
        }
        VideoGestureMode.Volume -> when {
            normalizedPercent < 0.01f -> CupertinoIcons.Default.SpeakerSlash
            normalizedPercent < 0.5f -> CupertinoIcons.Default.Speaker
            else -> CupertinoIcons.Default.SpeakerWave2
        }
        else -> fallbackIcon ?: CupertinoIcons.Filled.SunMax
    }
}

internal data class GestureLevelOverlayVisualPolicy(
    val accentColor: Color,
    val containerAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float
)

internal data class VideoGestureMotionSpec(
    val digitBlurResetDurationMillis: Int,
    val digitAlphaResetDurationMillis: Int,
    val digitEnterFadeDurationMillis: Int,
    val digitExitFadeDurationMillis: Int,
    val digitScaleDurationMillis: Int,
    val levelOverlayEnterFadeDurationMillis: Int,
    val levelOverlayEnterTransformDurationMillis: Int,
    val levelOverlayExitDurationMillis: Int,
    val levelProgressDurationMillis: Int,
    val levelIconScaleDurationMillis: Int,
    val levelValueScaleDurationMillis: Int,
    val levelIconEnterFadeDurationMillis: Int,
    val levelIconExitFadeDurationMillis: Int,
    val levelIconContentScaleDurationMillis: Int,
    val orientationHintEnterFadeDurationMillis: Int,
    val orientationHintEnterTransformDurationMillis: Int,
    val orientationHintExitDurationMillis: Int,
    val longPressHintDurationMillis: Int,
    val longPressArrowCycleDurationMillis: Int,
    val longPressArrowPhaseStepDurationMillis: Int
)

internal fun resolveVideoGestureMotionSpec(): VideoGestureMotionSpec {
    return VideoGestureMotionSpec(
        digitBlurResetDurationMillis = 220,
        digitAlphaResetDurationMillis = 220,
        digitEnterFadeDurationMillis = 130,
        digitExitFadeDurationMillis = 120,
        digitScaleDurationMillis = 200,
        levelOverlayEnterFadeDurationMillis = 160,
        levelOverlayEnterTransformDurationMillis = 220,
        levelOverlayExitDurationMillis = 200,
        levelProgressDurationMillis = 130,
        levelIconScaleDurationMillis = 180,
        levelValueScaleDurationMillis = 140,
        levelIconEnterFadeDurationMillis = 120,
        levelIconExitFadeDurationMillis = 110,
        levelIconContentScaleDurationMillis = 180,
        orientationHintEnterFadeDurationMillis = 150,
        orientationHintEnterTransformDurationMillis = 230,
        orientationHintExitDurationMillis = 200,
        longPressHintDurationMillis = 200,
        longPressArrowCycleDurationMillis = 900,
        longPressArrowPhaseStepDurationMillis = 300
    )
}

internal fun resolveGestureRenderProgress(percent: Float): Float {
    return percent.coerceIn(0f, 1f)
}

internal fun resolveGestureLevelOverlayVisualPolicy(
    mode: VideoGestureMode,
    percent: Float
): GestureLevelOverlayVisualPolicy {
    val progress = resolveGestureRenderProgress(percent)
    return when (mode) {
        VideoGestureMode.Brightness -> GestureLevelOverlayVisualPolicy(
            accentColor = Color(0xFFFFD54F),
            containerAlpha = 0.20f + progress * 0.08f,
            borderAlpha = 0.52f + progress * 0.22f,
            glowAlpha = 0.30f + progress * 0.40f
        )

        VideoGestureMode.Volume -> GestureLevelOverlayVisualPolicy(
            accentColor = Color(0xFF80DEEA),
            containerAlpha = 0.19f + progress * 0.08f,
            borderAlpha = 0.50f + progress * 0.20f,
            glowAlpha = 0.28f + progress * 0.38f
        )

        else -> GestureLevelOverlayVisualPolicy(
            accentColor = Color.White,
            containerAlpha = 0.22f,
            borderAlpha = 0.50f,
            glowAlpha = 0.32f
        )
    }
}

internal fun resolveGesturePercentDigits(percent: Int): List<Char?> {
    val normalized = percent.coerceIn(0, 100)
    val hundreds = (normalized / 100)
    val tens = (normalized / 10) % 10
    val ones = normalized % 10
    return listOf(
        hundreds.takeIf { it > 0 }?.let { ('0'.code + it).toChar() },
        if (hundreds > 0 || tens > 0) ('0'.code + tens).toChar() else null,
        ('0'.code + ones).toChar()
    )
}

internal fun resolveGesturePercentDigitChangeMask(
    previousPercent: Int,
    currentPercent: Int
): List<Boolean> {
    val previousDigits = resolveGesturePercentDigits(previousPercent)
    val currentDigits = resolveGesturePercentDigits(currentPercent)
    return currentDigits.indices.map { index ->
        previousDigits.getOrNull(index) != currentDigits.getOrNull(index)
    }
}

internal fun shouldUseTextureSurfaceForFlip(
    isFlippedHorizontal: Boolean,
    isFlippedVertical: Boolean
): Boolean {
    return isFlippedHorizontal || isFlippedVertical
}

internal fun shouldEnableLivePlayerSharedElement(
    transitionEnabled: Boolean,
    allowLivePlayerSharedElement: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        allowLivePlayerSharedElement &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun resolveSubtitleLanguageLabel(
    languageCode: String?,
    fallbackLabel: String
): String {
    val normalized = languageCode?.lowercase().orEmpty()
    return when {
        normalized.contains("zh") -> "中文"
        normalized.contains("en") -> "英文"
        languageCode.isNullOrBlank() -> fallbackLabel
        else -> languageCode
    }
}

internal fun shouldForceCoverDuringReturnAnimation(
    forceCoverOnly: Boolean
): Boolean {
    return forceCoverOnly
}

internal fun shouldShowCoverImage(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasStartedSmoothReveal: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation ||
        shouldKeepCoverForManualStart ||
        !isFirstFrameRendered ||
        !hasStartedSmoothReveal
}

internal fun shouldStartSmoothCoverReveal(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return isFirstFrameRendered &&
        !forceCoverDuringReturnAnimation &&
        !shouldKeepCoverForManualStart
}

internal fun shouldKeepCoverForManualStart(
    playWhenReady: Boolean,
    currentPositionMs: Long
): Boolean {
    return !playWhenReady && currentPositionMs <= 0L
}

internal fun shouldShowManualStartPlayButton(
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return shouldKeepCoverForManualStart
}

internal fun shouldEnableManualStartCoverOverlay(
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return shouldKeepCoverForManualStart
}

internal fun shouldFillPlayerViewportForManualStartCover(
    shouldKeepCoverForManualStart: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return shouldKeepCoverForManualStart && !forceCoverDuringReturnAnimation
}

internal enum class ManualStartPlayButtonAnchor {
    Center,
    CenterEnd,
    BottomEnd
}

internal data class ManualStartPlayButtonLayoutSpec(
    val anchor: ManualStartPlayButtonAnchor,
    val endPaddingDp: Int,
    val iconWidthDp: Int,
    val iconHeightDp: Int,
    val showCoverScrim: Boolean,
    val showTopDecorations: Boolean
)

internal fun resolveManualStartPlayButtonLayoutSpec(): ManualStartPlayButtonLayoutSpec {
    return ManualStartPlayButtonLayoutSpec(
        anchor = ManualStartPlayButtonAnchor.BottomEnd,
        endPaddingDp = 24,
        iconWidthDp = 72,
        iconHeightDp = 60,
        showCoverScrim = false,
        showTopDecorations = false
    )
}

internal fun shouldDisableCoverFadeAnimation(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation
}

internal data class VideoPlayerCoverMotionSpec(
    val shouldAnimateFade: Boolean,
    val enterFadeDurationMillis: Int,
    val exitFadeDurationMillis: Int
)

internal data class VideoPlayerRevealMotionSpec(
    val coverRevealHoldDelayMillis: Int,
    val surfaceRevealDurationMillis: Int,
    val surfaceRevealInitialScale: Float
)

internal data class VideoPlayerSurfaceRevealSpec(
    val alpha: Float,
    val scale: Float
)

internal fun resolveVideoPlayerCoverMotionSpec(
    forceCoverDuringReturnAnimation: Boolean
): VideoPlayerCoverMotionSpec {
    return VideoPlayerCoverMotionSpec(
        shouldAnimateFade = !forceCoverDuringReturnAnimation,
        enterFadeDurationMillis = VIDEO_PLAYER_COVER_FADE_ENTER_DURATION_MILLIS,
        exitFadeDurationMillis = VIDEO_PLAYER_COVER_FADE_EXIT_DURATION_MILLIS
    )
}

internal fun resolveVideoPlayerRevealMotionSpec(): VideoPlayerRevealMotionSpec {
    return VideoPlayerRevealMotionSpec(
        coverRevealHoldDelayMillis = VIDEO_PLAYER_COVER_REVEAL_HOLD_DELAY_MILLIS,
        surfaceRevealDurationMillis = VIDEO_PLAYER_SURFACE_REVEAL_DURATION_MILLIS,
        surfaceRevealInitialScale = VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE
    )
}

internal fun resolveVideoPlayerSurfaceRevealSpec(
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasStartedSmoothReveal: Boolean,
    surfaceRevealInitialScale: Float = VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE
): VideoPlayerSurfaceRevealSpec {
    if (forceCoverDuringReturnAnimation) {
        return VideoPlayerSurfaceRevealSpec(alpha = 0f, scale = 1f)
    }
    if (shouldKeepCoverForManualStart) {
        return VideoPlayerSurfaceRevealSpec(alpha = 0f, scale = 1f)
    }
    if (!hasStartedSmoothReveal) {
        return VideoPlayerSurfaceRevealSpec(
            alpha = 0f,
            scale = surfaceRevealInitialScale
        )
    }
    return VideoPlayerSurfaceRevealSpec(alpha = 1f, scale = 1f)
}

internal fun shouldHidePlayerSurfaceDuringForcedReturn(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation
}

internal fun shouldKeepInlinePlayerContentOnReset(
    isPortraitFullscreen: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return !isPortraitFullscreen && !forceCoverDuringReturnAnimation
}

internal fun shouldShowInlinePlayerView(
    isPortraitFullscreen: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return !isPortraitFullscreen && !forceCoverDuringReturnAnimation
}

internal fun shouldEnableCoverImageCrossfade(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return !forceCoverDuringReturnAnimation
}

internal fun resolvePreferredVideoCoverUrl(
    entryCoverUrl: String,
    detailCoverUrl: String
): String {
    val normalizedEntryCoverUrl = entryCoverUrl.trim()
    if (normalizedEntryCoverUrl.isNotEmpty()) return normalizedEntryCoverUrl

    val normalizedDetailCoverUrl = detailCoverUrl.trim()
    if (normalizedDetailCoverUrl.isNotEmpty()) return normalizedDetailCoverUrl

    return ""
}

internal fun shouldEnableForcedReturnCoverSharedBounds(
    forceCoverDuringReturnAnimation: Boolean,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean,
    sourceRoute: String?
): Boolean {
    val sourceRouteBase = sourceRoute?.substringBefore("?")
    val allowBySourceRoute = sourceRouteBase == null ||
        com.android.purebilibili.navigation.isVideoCardReturnTargetRoute(sourceRouteBase)
    return forceCoverDuringReturnAnimation &&
        transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope &&
        allowBySourceRoute
}

internal fun shouldPromoteFirstFrameByPlaybackFallback(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    playbackState: Int,
    playWhenReady: Boolean,
    currentPositionMs: Long,
    videoWidth: Int,
    videoHeight: Int
): Boolean {
    if (isFirstFrameRendered || forceCoverDuringReturnAnimation) return false
    val hasVideoTrack = videoWidth > 0 && videoHeight > 0
    return hasVideoTrack &&
        playWhenReady &&
        playbackState == Player.STATE_READY &&
        currentPositionMs > 300L
}

internal fun shouldAutoHidePlayerChromeOnPlaybackStart(
    showControls: Boolean,
    hasAutoHiddenForCurrentVideo: Boolean,
    isPlaying: Boolean,
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return showControls &&
        !hasAutoHiddenForCurrentVideo &&
        isPlaying &&
        isFirstFrameRendered &&
        !forceCoverDuringReturnAnimation
}

internal fun shouldRebindPlayerSurfaceOnForeground(
    hasPlayerView: Boolean,
    isInPipMode: Boolean,
    videoWidth: Int,
    videoHeight: Int
): Boolean {
    return hasPlayerView && !isInPipMode
}

internal fun shouldStartForegroundSurfaceRecovery(
    hasPlayerView: Boolean,
    shouldBindInlinePlayerView: Boolean,
    isInPipMode: Boolean
): Boolean {
    return hasPlayerView && shouldBindInlinePlayerView && !isInPipMode
}

internal fun shouldKickPlaybackAfterSurfaceRecovery(
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int
): Boolean {
    return playWhenReady &&
        !isPlaying &&
        (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)
}

internal fun shouldLogForegroundSurfaceRecoveryTimeout(
    hasRenderedFirstFrameSinceRecovery: Boolean,
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    if (hasRenderedFirstFrameSinceRecovery || !playWhenReady) return false
    return playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING
}

internal fun shouldLogPlaybackStall(
    bufferingDurationMs: Long,
    playWhenReady: Boolean,
    currentPositionMs: Long
): Boolean {
    return bufferingDurationMs >= PLAYBACK_STALL_LOG_THRESHOLD_MS &&
        playWhenReady &&
        currentPositionMs > 0L
}

internal fun shouldBindInlinePlayerViewToPlayer(
    isPortraitFullscreen: Boolean,
    hostLifecycleStarted: Boolean,
    isInPipMode: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return !isPortraitFullscreen &&
        !forceCoverDuringReturnAnimation &&
        (hostLifecycleStarted || isInPipMode)
}

internal fun shouldLoadDanmakuForForegroundHost(
    hostLifecycleStarted: Boolean,
    shouldLoadImmediately: Boolean
): Boolean {
    return hostLifecycleStarted && shouldLoadImmediately
}

internal fun rebindPlayerSurfaceIfNeeded(
    playerView: PlayerView,
    player: Player
) {
    when (val videoSurface = playerView.videoSurfaceView) {
        is TextureView -> {
            player.clearVideoTextureView(videoSurface)
        }
        is SurfaceView -> {
            player.clearVideoSurfaceView(videoSurface)
        }
    }
    if (playerView.player === player) {
        playerView.player = null
    }
    playerView.player = player
    when (val videoSurface = playerView.videoSurfaceView) {
        is TextureView -> {
            player.setVideoTextureView(videoSurface)
        }
        is SurfaceView -> {
            player.setVideoSurfaceView(videoSurface)
        }
    }
}
