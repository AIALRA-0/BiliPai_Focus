package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

/**
 * Haze 1-style `hazeEffect(state, style)` for Haze 2 blur module.
 * Applies [style] via [blurEffect] and respects recoverable background gating.
 */
@Composable
fun Modifier.hazeEffectCompat(
    state: HazeState,
    style: HazeBlurStyle,
    blurEnabled: Boolean = recoverableBlurEnabled(state),
): Modifier = hazeEffect(state = state) {
    blurEffect {
        this.style = style
        this.blurEnabled = blurEnabled
    }
}
