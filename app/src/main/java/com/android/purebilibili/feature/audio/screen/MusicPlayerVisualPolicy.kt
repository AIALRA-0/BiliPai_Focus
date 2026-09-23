package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.math.abs

internal data class MusicLyricFocusStyle(
    val blurRadiusDp: Int,
    val alphaPercent: Int
)

internal fun resolveMusicPagerIndicatorPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float
): Float = (currentPage + currentPageOffsetFraction).coerceIn(0f, 1f)

internal fun resolveMusicPlayModeIndex(mode: PlayMode): Int = when (mode) {
    PlayMode.SEQUENTIAL -> 0
    PlayMode.SHUFFLE -> 1
    PlayMode.REPEAT_ONE -> 2
    PlayMode.REPEAT_ALL -> 3
}

internal fun resolveMusicPlayMode(index: Int): PlayMode = when (index) {
    1 -> PlayMode.SHUFFLE
    2 -> PlayMode.REPEAT_ONE
    3 -> PlayMode.REPEAT_ALL
    else -> PlayMode.SEQUENTIAL
}

internal enum class MusicRepeatGlyph {
    OFF,
    ONE,
    ALL
}

internal data class MusicSecondaryTransportState(
    val shuffleEnabled: Boolean,
    val repeatGlyph: MusicRepeatGlyph
)

internal fun resolveMusicRepeatGlyph(mode: PlayMode): MusicRepeatGlyph = when (mode) {
    PlayMode.REPEAT_ONE -> MusicRepeatGlyph.ONE
    PlayMode.REPEAT_ALL, PlayMode.SHUFFLE -> MusicRepeatGlyph.ALL
    PlayMode.SEQUENTIAL -> MusicRepeatGlyph.OFF
}

internal fun resolveMusicSecondaryTransport(
    mode: PlayMode,
    shuffleEnabled: Boolean = mode == PlayMode.SHUFFLE
): MusicSecondaryTransportState = MusicSecondaryTransportState(
    shuffleEnabled = shuffleEnabled || mode == PlayMode.SHUFFLE,
    repeatGlyph = resolveMusicRepeatGlyph(mode)
)

internal fun resolvePlayModeAfterShuffleToggle(mode: PlayMode): PlayMode =
    if (mode == PlayMode.SHUFFLE) PlayMode.SEQUENTIAL else PlayMode.SHUFFLE

internal fun resolvePlayModeAfterRepeatToggle(mode: PlayMode): PlayMode = when (mode) {
    PlayMode.SEQUENTIAL, PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
    PlayMode.REPEAT_ONE -> PlayMode.REPEAT_ALL
    PlayMode.REPEAT_ALL -> PlayMode.SEQUENTIAL
}

internal fun resolveRepeatModeAfterToggle(mode: PlayMode): PlayMode =
    resolvePlayModeAfterRepeatToggle(
        if (mode == PlayMode.SHUFFLE) PlayMode.REPEAT_ALL else mode
    )

internal fun resolveMusicLyricsBlurEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    reduceMotion: Boolean
): Boolean = sdkInt >= 31 && effectsEnabled && !reduceMotion

internal fun resolveMusicLyricFocusStyle(
    lineIndex: Int,
    currentIndex: Int,
    blurEnabled: Boolean
): MusicLyricFocusStyle {
    val distance = abs(lineIndex - currentIndex)
    val alphaPercent = when (distance) {
        0 -> 100
        1 -> 62
        2 -> 40
        else -> 20
    }
    val blurRadiusDp = if (!blurEnabled) {
        0
    } else {
        when (distance) {
            0 -> 0
            1 -> 1
            2 -> 3
            else -> 7
        }
    }
    return MusicLyricFocusStyle(blurRadiusDp, alphaPercent)
}

internal fun resolveMusicLiquidGlassEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    isAppInBackground: Boolean,
    reduceMotion: Boolean
): Boolean {
    return effectsEnabled &&
        sdkInt >= 33 &&
        !isAppInBackground &&
        !reduceMotion
}

internal fun resolveMusicCoverFlowItemEntranceProgress(
    overallProgress: Float,
    distanceFromCenter: Float,
): Float {
    val delayFraction = (distanceFromCenter.coerceAtLeast(0f) * 0.16f).coerceAtMost(0.32f)
    return ((overallProgress.coerceIn(0f, 1f) - delayFraction) / (1f - delayFraction))
        .coerceIn(0f, 1f)
}

internal fun resolveMusicCoverFlowShadowEntranceProgress(overallProgress: Float): Float =
    ((overallProgress.coerceIn(0f, 1f) - 0.58f) / 0.42f).coerceIn(0f, 1f)

internal const val APPLE_MUSIC_COVER_SCALE = 1.0f
internal const val APPLE_MUSIC_COVER_SCALE_PLAYING = 1.0f
internal const val APPLE_MUSIC_COVER_SCALE_PAUSED = 0.88f
internal const val APPLE_MUSIC_COVER_MOTION_STIFFNESS = 180f
internal const val APPLE_MUSIC_COVER_CORNER_RADIUS_DP = 20
internal const val APPLE_MUSIC_CARD_CORNER_RADIUS_DP = 18
internal const val APPLE_MUSIC_COVER_SHADOW_ELEVATION_DP = 16

internal fun resolveAppleMusicCoverScale(
    isPlaying: Boolean = true,
    reduceMotion: Boolean = false
): Float = if (isPlaying) {
    APPLE_MUSIC_COVER_SCALE_PLAYING
} else {
    APPLE_MUSIC_COVER_SCALE_PAUSED
}

internal fun resolveAppleMusicCoverShadowElevation(
    playbackProgress: Float,
): Float {
    val progress = playbackProgress.coerceIn(0f, 1f)
    return APPLE_MUSIC_COVER_SHADOW_ELEVATION_DP * (0.72f + 0.28f * progress)
}

// Fade out beyond the second neighbor instead of leaving an opaque wall of covers.
internal fun resolveMusicCoverFlowItemAlpha(distanceFromCenter: Float): Float {
    val distance = distanceFromCenter.coerceAtLeast(0f)
    return if (distance <= 2f) 1f - distance * 0.18f
    else (3f - distance).coerceIn(0f, 1f) * 0.64f
}
