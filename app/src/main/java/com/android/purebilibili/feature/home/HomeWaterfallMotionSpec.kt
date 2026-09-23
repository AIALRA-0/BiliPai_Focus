package com.android.purebilibili.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

internal fun <T> homeWaterfallFadeInSpec(delayMillis: Int): TweenSpec<T> = tween(
    durationMillis = 280,
    delayMillis = delayMillis,
    easing = LinearOutSlowInEasing,
)

internal fun <T> homeWaterfallExpandSpec(delayMillis: Int): TweenSpec<T> = tween(
    durationMillis = 420,
    delayMillis = delayMillis,
    easing = FastOutSlowInEasing,
)

internal fun <T> homeWaterfallFadeOutSpec(): TweenSpec<T> = tween(durationMillis = 120)
