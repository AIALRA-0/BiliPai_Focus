package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle

/**
 * Canonical decision for which preset's renderer a shared adaptive primitive
 * should dispatch to. Each primitive may collapse [MATERIAL3] and [MIUIX_BRIDGED]
 * onto the same code path (e.g. AdaptiveTopAppBar) or split them (e.g. SuperDialog
 * vs AlertDialog). Use this enum at the entry point of every preset-aware primitive
 * so the dispatch is testable in plain Kotlin and consistent across primitives.
 */
enum class PresetPrimitiveRenderer {
    /** Material 3 native components. MATERIAL3 style. */
    MATERIAL3,
    /** Miuix native components. MIUIX style. */
    MIUIX_BRIDGED
}

fun resolvePresetPrimitiveRenderer(
    uiStyle: AppUiStyle
): PresetPrimitiveRenderer = when (uiStyle) {
    AppUiStyle.MIUIX -> PresetPrimitiveRenderer.MIUIX_BRIDGED
    AppUiStyle.MATERIAL3 -> PresetPrimitiveRenderer.MATERIAL3
}

@Composable
@ReadOnlyComposable
fun rememberPresetPrimitiveRenderer(): PresetPrimitiveRenderer =
    resolvePresetPrimitiveRenderer(LocalAppUiStyle.current)
