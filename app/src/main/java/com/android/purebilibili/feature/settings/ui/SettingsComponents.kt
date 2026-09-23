package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.ui.components.AppSegmentOption

@Composable
fun BlurIntensitySelector(
    selectedIntensity: BlurIntensity,
    onIntensityChange: (BlurIntensity) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSingleChoicePreference(
        title = "模糊强度",
        subtitle = when (selectedIntensity) {
            BlurIntensity.THIN -> "轻微淡化背景，效果自然且更省性能（推荐）"
            BlurIntensity.APPLE_DOCK -> "大幅淡化背景，前景内容最突出"
            BlurIntensity.THICK -> "适度淡化背景，兼顾层次与可读性"
        },
        options = listOf(
            AppSegmentOption(BlurIntensity.THIN, "轻度"),
            AppSegmentOption(BlurIntensity.THICK, "中度"),
            AppSegmentOption(BlurIntensity.APPLE_DOCK, "重度"),
        ),
        selectedValue = selectedIntensity,
        modifier = modifier,
        icon = rememberSettingsSemanticIcon(SettingsIconRole.BLUR_INTENSITY),
        iconTint = iOSBlue,
        onSelectionChange = onIntensityChange,
    )
}
