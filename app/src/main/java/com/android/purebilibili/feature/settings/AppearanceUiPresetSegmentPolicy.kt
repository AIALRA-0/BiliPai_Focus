package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.components.AppSegmentOption

internal fun resolveThemeSelectionOptions(
    material3Label: String,
    miuixLabel: String,
): List<AppSegmentOption<AppUiStyle>> {
    return listOf(
        AppSegmentOption(AppUiStyle.MATERIAL3, material3Label),
        AppSegmentOption(AppUiStyle.MIUIX, miuixLabel),
    )
}
