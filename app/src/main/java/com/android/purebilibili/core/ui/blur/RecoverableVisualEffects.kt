package com.android.purebilibili.core.ui.blur

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.lifecycle.BackgroundManager
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.util.Collections
import java.util.WeakHashMap

/**
 * Haze 2 removed [HazeState.blurEnabled]. Recoverable background/foreground gating is tracked
 * per-state and applied inside each [dev.chrisbanes.haze.hazeEffect] via [recoverableBlurEnabled].
 */
private val recoverableBlurGates: MutableMap<HazeState, MutableState<Boolean>> =
    Collections.synchronizedMap(WeakHashMap())

@Composable
fun recoverableBlurEnabled(state: HazeState): Boolean {
    val gate = recoverableBlurGates[state] ?: return true
    return gate.value
}

fun Modifier.hazeSourceCompat(state: HazeState): Modifier {
    if (!shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)) return this
    return hazeSource(state)
}

internal fun shouldEnableRecoverableHeavyVisualEffects(
    userEnabled: Boolean,
    isAppInBackground: Boolean
): Boolean {
    return userEnabled && !isAppInBackground
}

internal fun shouldRecreateRecoverableHazeState(
    sdkInt: Int
): Boolean {
    return sdkInt >= 36
}

internal fun shouldAllowHomeChromeLiquidGlass(
    sdkInt: Int
): Boolean {
    return sdkInt >= Build.VERSION_CODES.TIRAMISU
}

internal fun resolveHomeChromeLiquidGlassEnabled(
    userEnabled: Boolean,
    sdkInt: Int
): Boolean {
    return userEnabled && shouldAllowHomeChromeLiquidGlass(sdkInt)
}

internal fun shouldAllowRuntimeShaderBackedHazeEffect(
    sdkInt: Int
): Boolean {
    return sdkInt >= Build.VERSION_CODES.TIRAMISU
}

internal fun shouldAllowRenderEffectBackedHazeEffect(
    sdkInt: Int
): Boolean {
    return sdkInt >= Build.VERSION_CODES.S
}

internal fun shouldAllowDirectHazeLiquidGlassFallback(
    sdkInt: Int
): Boolean {
    return shouldAllowHomeChromeLiquidGlass(sdkInt) && sdkInt < 36
}

@Composable
fun rememberRecoverableHazeState(
    userEnabled: Boolean = true,
    initialBlurEnabled: Boolean = true,
    sdkInt: Int = Build.VERSION.SDK_INT
): HazeState {
    var isAppInBackground by remember { mutableStateOf(BackgroundManager.isInBackground) }
    var recreationKey by remember { mutableIntStateOf(0) }
    val shouldRecreateState = shouldRecreateRecoverableHazeState(sdkInt)
    val hazeState = remember(recreationKey) {
        HazeState()
    }
    val blurGate = remember(hazeState, initialBlurEnabled) {
        mutableStateOf(initialBlurEnabled).also { recoverableBlurGates[hazeState] = it }
    }

    DisposableEffect(hazeState) {
        recoverableBlurGates[hazeState] = blurGate
        onDispose {
            recoverableBlurGates.remove(hazeState)
        }
    }

    DisposableEffect(shouldRecreateState) {
        val listener = object : BackgroundManager.BackgroundStateListener {
            override fun onEnterBackground() {
                isAppInBackground = true
                if (shouldRecreateState) {
                    recreationKey += 1
                }
            }

            override fun onEnterForeground() {
                isAppInBackground = false
                if (shouldRecreateState) {
                    recreationKey += 1
                }
            }
        }
        BackgroundManager.addListener(listener)
        onDispose {
            BackgroundManager.removeListener(listener)
        }
    }

    SideEffect {
        blurGate.value = shouldEnableRecoverableHeavyVisualEffects(
            userEnabled = userEnabled,
            isAppInBackground = isAppInBackground
        )
    }

    return hazeState
}
