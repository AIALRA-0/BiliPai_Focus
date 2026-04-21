package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

data class AppearanceUiPresetDescription(
    val title: String,
    val summary: String
)

internal fun resolveAppearanceUiPresetDescription(
    preset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    iosTitle: String,
    iosSummary: String,
    materialTitle: String,
    materialSummary: String,
    miuixTitle: String,
    miuixSummary: String
): AppearanceUiPresetDescription {
    return when (preset) {
        UiPreset.IOS -> AppearanceUiPresetDescription(
            title = iosTitle,
            summary = iosSummary
        )

        UiPreset.MD3 -> when (androidNativeVariant) {
            AndroidNativeVariant.MATERIAL3 -> AppearanceUiPresetDescription(
                title = materialTitle,
                summary = materialSummary
            )

            AndroidNativeVariant.MIUIX -> AppearanceUiPresetDescription(
                title = miuixTitle,
                summary = miuixSummary
            )
        }
    }
}
