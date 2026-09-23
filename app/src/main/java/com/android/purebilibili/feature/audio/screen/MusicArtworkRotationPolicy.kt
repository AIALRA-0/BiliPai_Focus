package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive

internal const val MUSIC_ARTWORK_ROTATION_DURATION_MS = 24_000

internal fun shouldRotateMusicArtwork(
    isPlaying: Boolean,
    reduceMotion: Boolean
): Boolean = isPlaying && !reduceMotion

internal fun resolveMusicArtworkRotationDurationMs(playbackSpeed: Float): Int {
    val speed = playbackSpeed.coerceIn(0.25f, 4f)
    return (MUSIC_ARTWORK_ROTATION_DURATION_MS / speed).toInt().coerceAtLeast(250)
}

@Composable
internal fun rememberMusicArtworkRotationDegrees(
    active: Boolean,
    contentKey: String,
    playbackSpeed: Float = 1f
): () -> Float {
    val rotation = remember(contentKey) { Animatable(0f) }
    val durationMs = resolveMusicArtworkRotationDurationMs(playbackSpeed)
    LaunchedEffect(active, contentKey, durationMs) {
        if (!active) return@LaunchedEffect
        while (isActive) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = LinearEasing
                )
            )
        }
    }
    return remember(rotation) { { rotation.value } }
}
