package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

fun resolveBottomBarLikeHorizontalContentTransform(
    durationMillis: Int,
    forward: Boolean,
): ContentTransform {
    if (durationMillis <= 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val spec = navigationSlideSpring(durationMillis)
    return if (forward) {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = spec,
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = spec,
        )
    } else {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth },
            animationSpec = spec,
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = spec,
        )
    }
}
