package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiPreset

internal fun resolveUiPresetSegmentOptions(): List<PlaybackSegmentOption<UiPreset>> {
    return resolveUiPresetSegmentOptions(
        iosLabel = UiPreset.IOS.label,
        androidNativeLabel = UiPreset.MD3.label
    )
}

internal fun resolveUiPresetSegmentOptions(
    iosLabel: String,
    androidNativeLabel: String
): List<PlaybackSegmentOption<UiPreset>> {
    return listOf(
        PlaybackSegmentOption(UiPreset.IOS, iosLabel),
        PlaybackSegmentOption(UiPreset.MD3, androidNativeLabel)
    )
}
