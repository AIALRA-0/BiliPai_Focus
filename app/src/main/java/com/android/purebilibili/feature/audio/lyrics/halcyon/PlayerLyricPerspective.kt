package com.android.purebilibili.feature.audio.lyrics.halcyon

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/**
 * Ported Halcyon `PlayerLyricPerspective`.
 */
internal fun Modifier.playerLyricPerspective(
    enabled: Boolean,
    angle: Int,
    lyricTextAlign: Int = PLAYER_LYRIC_ALIGN_START,
    cameraDistanceMultiplier: Float = 18f,
): Modifier = composed {
    if (!enabled || angle <= 0) return@composed this

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val originX = when (lyricTextAlign) {
        PLAYER_LYRIC_ALIGN_RIGHT -> 1f
        PLAYER_LYRIC_ALIGN_CENTER -> 0.5f
        else -> 0f
    }
    val resolvedAngle = when (lyricTextAlign) {
        PLAYER_LYRIC_ALIGN_RIGHT -> angle.toFloat()
        else -> -angle.toFloat()
    }
    val ultraWideLandscape = isUltraWideLandscapePlayerLayout(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    val isLargeLandscape = configuration.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE &&
        (configuration.smallestScreenWidthDp >= 600 || ultraWideLandscape)
    val effectiveAngle = if (isLargeLandscape) {
        resolvedAngle.coerceIn(-14f, 14f)
    } else {
        resolvedAngle
    }
    val scaleXValue = when {
        ultraWideLandscape -> 0.97f
        isLargeLandscape -> 0.94f
        else -> 1f
    }
    val translationRatio = if (ultraWideLandscape) 0.010f else 0.018f
    val translationXValue = when {
        !isLargeLandscape -> 0f
        lyricTextAlign == PLAYER_LYRIC_ALIGN_RIGHT -> {
            -configuration.screenWidthDp * density.density * translationRatio
        }
        lyricTextAlign == PLAYER_LYRIC_ALIGN_CENTER -> 0f
        else -> configuration.screenWidthDp * density.density * translationRatio
    }

    graphicsLayer {
        rotationY = effectiveAngle
        transformOrigin = TransformOrigin(originX, 0.5f)
        cameraDistance = density.density * cameraDistanceMultiplier
        scaleX = scaleXValue
        translationX = translationXValue
    }
}

internal const val PLAYER_LYRIC_ALIGN_START = 0
internal const val PLAYER_LYRIC_ALIGN_LEFT = PLAYER_LYRIC_ALIGN_START
internal const val PLAYER_LYRIC_ALIGN_CENTER = 1
internal const val PLAYER_LYRIC_ALIGN_RIGHT = 2

internal fun isUltraWideLandscapePlayerLayout(
    screenWidthDp: Int,
    screenHeightDp: Int,
): Boolean {
    if (screenHeightDp <= 0 || screenWidthDp <= 0) return false
    if (screenWidthDp <= screenHeightDp) return false
    return screenWidthDp.toFloat() / screenHeightDp.toFloat() >= 2.1f
}

/** Halcyon 歌词样式默认 Y 轴旋转角（设置页 25°）。 */
internal const val HALCYON_DEFAULT_PERSPECTIVE_ANGLE = 25
