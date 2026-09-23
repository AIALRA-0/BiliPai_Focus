package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class AppDrawerContainerTreatment {
    TRANSLUCENT,
    OPAQUE,
}

data class AppDrawerVisualPolicy(
    val containerTreatment: AppDrawerContainerTreatment,
    val profileChevronSizeDp: Int,
)

fun resolveAppDrawerVisualPolicy(
    renderer: PresetPrimitiveRenderer,
    blurEnabled: Boolean,
): AppDrawerVisualPolicy = when (renderer) {
    PresetPrimitiveRenderer.MATERIAL3,
    PresetPrimitiveRenderer.MIUIX_BRIDGED -> AppDrawerVisualPolicy(
        containerTreatment = if (blurEnabled) {
            AppDrawerContainerTreatment.TRANSLUCENT
        } else {
            AppDrawerContainerTreatment.OPAQUE
        },
        profileChevronSizeDp = 20,
    )
}

@Composable
fun rememberAppDrawerVisualPolicy(blurEnabled: Boolean): AppDrawerVisualPolicy {
    val renderer = rememberPresetPrimitiveRenderer()
    return remember(renderer, blurEnabled) {
        resolveAppDrawerVisualPolicy(
            renderer = renderer,
            blurEnabled = blurEnabled,
        )
    }
}
