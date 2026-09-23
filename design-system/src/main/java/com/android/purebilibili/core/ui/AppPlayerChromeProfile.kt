package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class AppEffectCapability(
    val supportsIndependentLiquidGlass: Boolean,
    val usesTonalContainerTreatment: Boolean,
)

data class AppPlayerChromeProfile(
    val tabPresentation: AppTopTabPresentation,
    val iconFamily: AppSemanticIconFamily,
    val compactChromeSpec: CompactCapsuleChromeSpec,
    val effects: AppEffectCapability,
) {
    val usesCompactOverlayControls: Boolean
        get() = tabPresentation != AppTopTabPresentation.MATERIAL_UNDERLINE
}

@Composable
fun rememberAppPlayerChromeProfile(): AppPlayerChromeProfile {
    val topChromePolicy = rememberAppTopChromePolicy()
    val semanticVisualPolicy = rememberAppSemanticVisualPolicy()
    val contentCardSurfaceSpec = rememberContentCardSurfaceSpec()
    return remember(topChromePolicy, semanticVisualPolicy, contentCardSurfaceSpec) {
        AppPlayerChromeProfile(
            tabPresentation = topChromePolicy.tabPresentation,
            iconFamily = topChromePolicy.iconFamily,
            compactChromeSpec = topChromePolicy.compactChromeSpec,
            effects = AppEffectCapability(
                supportsIndependentLiquidGlass = semanticVisualPolicy.supportsIndependentLiquidGlass,
                usesTonalContainerTreatment = contentCardSurfaceSpec.usesTonalContainerTreatment,
            ),
        )
    }
}
