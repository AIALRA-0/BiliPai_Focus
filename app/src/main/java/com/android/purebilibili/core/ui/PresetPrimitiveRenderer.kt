package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset

/**
 * Canonical decision for which preset's renderer a shared `iOS*` primitive
 * should dispatch to. Each primitive may collapse [MATERIAL3] and [MIUIX_BRIDGED]
 * onto the same code path (e.g. AdaptiveTopAppBar) or split them (e.g. SuperDialog
 * vs AlertDialog). Use this enum at the entry point of every preset-aware primitive
 * so the dispatch is testable in plain Kotlin and consistent across primitives.
 */
enum class PresetPrimitiveRenderer {
    /** Cupertino-styled custom rendering. iOS preset only. */
    IOS,
    /** Material 3 native components. MD3 preset + Material 3 variant. */
    MATERIAL3,
    /** Miuix native components, with the Material bridge in place. MD3 preset + Miuix variant. */
    MIUIX_BRIDGED
}

fun resolvePresetPrimitiveRenderer(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): PresetPrimitiveRenderer = when {
    uiPreset == UiPreset.IOS -> PresetPrimitiveRenderer.IOS
    uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX ->
        PresetPrimitiveRenderer.MIUIX_BRIDGED
    else -> PresetPrimitiveRenderer.MATERIAL3
}

@Composable
@ReadOnlyComposable
fun rememberPresetPrimitiveRenderer(): PresetPrimitiveRenderer = resolvePresetPrimitiveRenderer(
    uiPreset = LocalUiPreset.current,
    androidNativeVariant = LocalAndroidNativeVariant.current
)
