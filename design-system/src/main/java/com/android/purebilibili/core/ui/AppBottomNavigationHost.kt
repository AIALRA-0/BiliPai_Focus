package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class AppBottomNavigationVisualPolicy(
    val liquidGlassEnabled: Boolean,
)

internal fun resolveAppBottomNavigationVisualPolicy(
    androidNativeLiquidGlassEnabled: Boolean,
): AppBottomNavigationVisualPolicy {
    return AppBottomNavigationVisualPolicy(
        liquidGlassEnabled = androidNativeLiquidGlassEnabled,
    )
}

/** Selects the active bottom-navigation implementation without leaking a style enum to callers. */
@Composable
fun AppBottomNavigationHost(
    androidNativeLiquidGlassEnabled: Boolean,
    materialContent: @Composable (AppBottomNavigationVisualPolicy) -> Unit,
    platformContent: @Composable (AppBottomNavigationVisualPolicy) -> Unit,
) {
    val renderer = rememberPresetPrimitiveRenderer()
    val policy = remember(androidNativeLiquidGlassEnabled) {
        resolveAppBottomNavigationVisualPolicy(
            androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled,
        )
    }
    when (renderer) {
        PresetPrimitiveRenderer.MATERIAL3 -> materialContent(policy)
        PresetPrimitiveRenderer.MIUIX_BRIDGED -> platformContent(policy)
    }
}
