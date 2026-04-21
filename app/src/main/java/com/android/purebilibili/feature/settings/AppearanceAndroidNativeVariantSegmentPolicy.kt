package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AndroidNativeVariant

internal fun resolveAndroidNativeVariantSegmentOptions(
    material3Label: String,
    miuixLabel: String
): List<PlaybackSegmentOption<AndroidNativeVariant>> {
    return listOf(
        PlaybackSegmentOption(AndroidNativeVariant.MATERIAL3, material3Label),
        PlaybackSegmentOption(AndroidNativeVariant.MIUIX, miuixLabel)
    )
}
