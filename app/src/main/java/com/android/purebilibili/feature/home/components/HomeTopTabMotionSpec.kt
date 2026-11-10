package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.motion.iosMorphTween

internal fun <T> iosTopTabCapsuleMotionSpec(): TweenSpec<T> = iosMorphTween(260)

/** 原生 MD3 顶部分类翻页节奏；玻璃与 Miuix 路径保持各自默认，不共用本档。 */
internal fun <T> nativeMd3TopTabSelectionSpec(): TweenSpec<T> = tween(
    durationMillis = 240,
    easing = AppMotionEasing.M1Standard,
)
