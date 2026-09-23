package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle

data class AppearanceUiPresetDescription(
    val title: String,
    val summary: String
)

internal fun resolveAppearanceUiPresetDescription(
    selection: AppUiStyle,
    materialTitle: String,
    materialSummary: String,
    miuixTitle: String,
    miuixSummary: String
): AppearanceUiPresetDescription {
    return when (selection) {
        AppUiStyle.MATERIAL3 -> AppearanceUiPresetDescription(
            title = materialTitle,
            summary = materialSummary
        )

        AppUiStyle.MIUIX -> AppearanceUiPresetDescription(
            title = miuixTitle,
            summary = miuixSummary
        )
    }
}
