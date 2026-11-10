package com.android.purebilibili.feature.home

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.motion.AppMotionEasing

internal fun <T> homeWaterfallFadeInSpec(delayMillis: Int): TweenSpec<T> = tween(
    durationMillis = 280,
    delayMillis = delayMillis,
    easing = AppMotionEasing.M1Decelerate,
)

internal fun <T> homeWaterfallExpandSpec(delayMillis: Int): TweenSpec<T> = tween(
    durationMillis = 420,
    delayMillis = delayMillis,
    easing = AppMotionEasing.M1Standard,
)

internal fun <T> homeWaterfallFadeOutSpec(): TweenSpec<T> = tween(durationMillis = 120)

/** 首页横幅卡被消散移除后，下一条在原位浮现的入场淡入节奏。 */
internal fun <T> homeHeroBannerEntranceSpec(): TweenSpec<T> = tween(durationMillis = 260)
