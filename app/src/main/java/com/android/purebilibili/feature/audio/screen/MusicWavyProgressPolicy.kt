package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.core.theme.AppUiStyle

internal const val MUSIC_WAVY_AMPLITUDE_DP = 6
internal const val MUSIC_WAVY_WAVELENGTH_DP = 22
internal const val MUSIC_WAVY_STROKE_DP = 3
internal const val MUSIC_WAVY_THUMB_DP = 12

internal fun shouldAnimateMusicWavyProgress(
    isPlaying: Boolean,
    isDragging: Boolean,
    reduceMotion: Boolean
): Boolean = isPlaying && !isDragging && !reduceMotion

internal fun shouldUseNativeThemeMusicProgress(
    glassEnabled: Boolean,
    uiStyle: AppUiStyle
): Boolean = !glassEnabled || uiStyle == AppUiStyle.MIUIX

internal fun shouldUseMusicWavyProgress(
    glassEnabled: Boolean,
    uiStyle: AppUiStyle,
    isPlaying: Boolean,
    isDragging: Boolean,
    reduceMotion: Boolean
): Boolean = !shouldUseNativeThemeMusicProgress(glassEnabled, uiStyle) &&
    shouldAnimateMusicWavyProgress(
        isPlaying = isPlaying,
        isDragging = isDragging,
        reduceMotion = reduceMotion
    )

internal fun resolveMusicProgressFraction(
    value: Float,
    rangeStart: Float,
    rangeEnd: Float
): Float {
    val span = rangeEnd - rangeStart
    if (span <= 0f) return 0f
    return ((value - rangeStart) / span).coerceIn(0f, 1f)
}

internal fun resolveMusicProgressValue(
    fraction: Float,
    rangeStart: Float,
    rangeEnd: Float
): Float {
    val clamped = fraction.coerceIn(0f, 1f)
    return rangeStart + (rangeEnd - rangeStart) * clamped
}
