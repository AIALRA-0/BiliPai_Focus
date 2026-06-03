package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.theme.AndroidNativeVariant

data class VideoSettingsPanelActionPolicy(
    val rowItemSpacingDp: Int,
    val pillHeightDp: Int,
    val pillMinWidthDp: Int,
    val pillHorizontalPaddingDp: Int,
    val pillIconSizeDp: Int
)

fun resolveVideoSettingsPanelActionPolicy(
    widthDp: Int,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): VideoSettingsPanelActionPolicy {
    val basePolicy = when {
        widthDp >= 840 -> VideoSettingsPanelActionPolicy(
            rowItemSpacingDp = 12,
            pillHeightDp = 50,
            pillMinWidthDp = 136,
            pillHorizontalPaddingDp = 16,
            pillIconSizeDp = 19
        )
        widthDp >= 600 -> VideoSettingsPanelActionPolicy(
            rowItemSpacingDp = 12,
            pillHeightDp = 48,
            pillMinWidthDp = 126,
            pillHorizontalPaddingDp = 16,
            pillIconSizeDp = 18
        )
        else -> VideoSettingsPanelActionPolicy(
            rowItemSpacingDp = 10,
            pillHeightDp = 46,
            pillMinWidthDp = 116,
            pillHorizontalPaddingDp = 14,
            pillIconSizeDp = 18
        )
    }
    return basePolicy
}
