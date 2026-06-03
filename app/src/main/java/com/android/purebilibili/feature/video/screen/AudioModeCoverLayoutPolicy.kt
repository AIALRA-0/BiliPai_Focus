package com.android.purebilibili.feature.video.screen

import kotlin.math.abs
import kotlin.math.roundToInt

internal const val AUDIO_MODE_COVER_WIDTH_FRACTION = 0.92f
internal const val AUDIO_MODE_COVER_MIN_VERTICAL_CLEARANCE_DP = 8
internal const val AUDIO_MODE_COVER_CORNER_RADIUS_DP = 26
internal const val AUDIO_MODE_COVER_SHADOW_ELEVATION_DP = 28
internal const val AUDIO_MODE_BACKGROUND_SCRIM_ALPHA_PERCENT = 46
internal const val AUDIO_MODE_COVER_ASPECT_WIDTH = 16
internal const val AUDIO_MODE_COVER_ASPECT_HEIGHT = 10
internal const val AUDIO_MODE_COVER_FLOW_MAX_ROTATION_DEGREES = 18
internal const val AUDIO_MODE_COVER_FLOW_MAX_TRANSLATION_DP = 34
internal const val AUDIO_MODE_COVER_FLOW_MAX_SCALE_LOSS_PERCENT = 10
internal const val AUDIO_MODE_COVER_FLOW_MAX_ALPHA_LOSS_PERCENT = 28

internal data class AudioModeCoverArtworkStyle(
    val cornerRadiusDp: Int,
    val shadowElevationDp: Int,
    val backgroundScrimAlphaPercent: Int,
    val aspectWidth: Int,
    val aspectHeight: Int,
    val maxRotationDegrees: Int,
    val maxTranslationDp: Int,
    val maxScaleLossPercent: Int,
    val maxAlphaLossPercent: Int
)

internal fun resolveAudioModeCoverArtworkStyle(): AudioModeCoverArtworkStyle {
    return AudioModeCoverArtworkStyle(
        cornerRadiusDp = AUDIO_MODE_COVER_CORNER_RADIUS_DP,
        shadowElevationDp = AUDIO_MODE_COVER_SHADOW_ELEVATION_DP,
        backgroundScrimAlphaPercent = AUDIO_MODE_BACKGROUND_SCRIM_ALPHA_PERCENT,
        aspectWidth = AUDIO_MODE_COVER_ASPECT_WIDTH,
        aspectHeight = AUDIO_MODE_COVER_ASPECT_HEIGHT,
        maxRotationDegrees = AUDIO_MODE_COVER_FLOW_MAX_ROTATION_DEGREES,
        maxTranslationDp = AUDIO_MODE_COVER_FLOW_MAX_TRANSLATION_DP,
        maxScaleLossPercent = AUDIO_MODE_COVER_FLOW_MAX_SCALE_LOSS_PERCENT,
        maxAlphaLossPercent = AUDIO_MODE_COVER_FLOW_MAX_ALPHA_LOSS_PERCENT
    )
}

internal fun resolveAudioModeCenteredCoverSizeDp(
    availableWidthDp: Int,
    availableHeightDp: Int,
    widthFraction: Float = AUDIO_MODE_COVER_WIDTH_FRACTION,
    minVerticalClearanceDp: Int = AUDIO_MODE_COVER_MIN_VERTICAL_CLEARANCE_DP
): Int {
    val widthBound = (availableWidthDp * widthFraction).roundToInt()
    val heightBound = (availableHeightDp - (minVerticalClearanceDp * 2)).coerceAtLeast(0)
    return widthBound.coerceAtMost(heightBound)
}

internal data class AudioModeArtworkSizeDp(
    val widthDp: Int,
    val heightDp: Int
)

internal data class AudioModeVerticalPageTransform(
    val rotationXDegrees: Float,
    val translationYDp: Float,
    val scale: Float,
    val alpha: Float,
    val pivotFractionY: Float
)

internal fun resolveAudioModeArtworkSizeDp(
    availableWidthDp: Int,
    availableHeightDp: Int,
    widthFraction: Float = AUDIO_MODE_COVER_WIDTH_FRACTION,
    minVerticalClearanceDp: Int = AUDIO_MODE_COVER_MIN_VERTICAL_CLEARANCE_DP,
    aspectWidth: Int = AUDIO_MODE_COVER_ASPECT_WIDTH,
    aspectHeight: Int = AUDIO_MODE_COVER_ASPECT_HEIGHT
): AudioModeArtworkSizeDp {
    if (availableWidthDp <= 0 || availableHeightDp <= 0 || aspectWidth <= 0 || aspectHeight <= 0) {
        return AudioModeArtworkSizeDp(widthDp = 0, heightDp = 0)
    }
    val widthBound = (availableWidthDp * widthFraction).roundToInt()
    val heightBound = (availableHeightDp - (minVerticalClearanceDp * 2)).coerceAtLeast(0)
    val widthFromHeight = (heightBound * aspectWidth.toFloat() / aspectHeight.toFloat()).roundToInt()
    val width = widthBound.coerceAtMost(widthFromHeight).coerceAtLeast(0)
    val height = (width * aspectHeight.toFloat() / aspectWidth.toFloat()).roundToInt()
    return AudioModeArtworkSizeDp(widthDp = width, heightDp = height)
}

internal fun resolveAudioModeVerticalPageTransform(
    pageOffset: Float,
    style: AudioModeCoverArtworkStyle
): AudioModeVerticalPageTransform {
    val clampedOffset = pageOffset.coerceIn(-1f, 1f)
    val distance = abs(clampedOffset)
    val maxAlphaLoss = style.maxAlphaLossPercent / 100f
    return AudioModeVerticalPageTransform(
        rotationXDegrees = clampedOffset * style.maxRotationDegrees,
        translationYDp = clampedOffset * -style.maxTranslationDp,
        scale = 1f,
        alpha = 1f - (distance * maxAlphaLoss).coerceIn(0f, maxAlphaLoss),
        pivotFractionY = if (clampedOffset < 0f) 1f else 0f
    )
}
