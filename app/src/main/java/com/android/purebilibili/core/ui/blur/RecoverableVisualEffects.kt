package com.android.purebilibili.core.ui.blur

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.purebilibili.core.lifecycle.BackgroundManager
import dev.chrisbanes.haze.HazeState

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
    val hazeState = remember(initialBlurEnabled, recreationKey) {
        HazeState(initialBlurEnabled = initialBlurEnabled)
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
        hazeState.blurEnabled = shouldEnableRecoverableHeavyVisualEffects(
            userEnabled = userEnabled,
            isAppInBackground = isAppInBackground
        )
    }

    return hazeState
}
