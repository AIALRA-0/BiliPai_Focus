package com.android.purebilibili.core.ui.skeleton

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SkeletonSettingsStore

@Composable
fun rememberSkeletonBreathingEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    return remember(context) { SkeletonSettingsStore.breathingEnabled(context) }
        .collectAsStateWithLifecycle(initialValue = true).value
}

/** A visible, smooth luminance breath over a 2.8 second cycle; never changes layout size. */
@Composable
fun rememberGentleSkeletonPulse(): State<Float> {
    if (com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion()) {
        return rememberUpdatedState(0.5f)
    }
    return rememberInfiniteTransition(label = "gentleSkeleton").animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gentleSkeletonPulse",
    )
}
