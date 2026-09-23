package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable

data class AppNavigationCapabilities(
    val usePlatformSideRail: Boolean,
)

fun resolveAppNavigationCapabilities(
    renderer: PresetPrimitiveRenderer,
): AppNavigationCapabilities = AppNavigationCapabilities(
    usePlatformSideRail = renderer == PresetPrimitiveRenderer.MIUIX_BRIDGED,
)

@Composable
fun rememberAppNavigationCapabilities(): AppNavigationCapabilities =
    resolveAppNavigationCapabilities(rememberPresetPrimitiveRenderer())
