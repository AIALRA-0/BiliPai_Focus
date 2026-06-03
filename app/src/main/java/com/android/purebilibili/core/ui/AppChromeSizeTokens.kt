package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

/**
 * 高频胶囊控件的尺寸基准。
 *
 * 这里不承载颜色和动画，只约束搜索栏、分段控件、筛选 chip、小操作按钮等
 * 常用 chrome 的尺寸、留白和圆角，避免各页面继续散落相近但不一致的硬编码。
 */
data class CompactCapsuleChromeSpec(
    val primaryHeightDp: Int,
    val secondaryButtonSizeDp: Int,
    val chipHeightDp: Int,
    val compactChipHeightDp: Int,
    val primaryCornerRadiusDp: Int,
    val secondaryButtonCornerRadiusDp: Int,
    val chipCornerRadiusDp: Int,
    val compactChipCornerRadiusDp: Int,
    val iconSizeDp: Int,
    val smallIconSizeDp: Int,
    val inputHorizontalPaddingDp: Int,
    val chipHorizontalPaddingDp: Int,
    val compactChipHorizontalPaddingDp: Int,
    val standardGapDp: Int
)

fun resolveCompactCapsuleChromeSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): CompactCapsuleChromeSpec {
    val inputHorizontalPadding = when {
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> 14
        uiPreset == UiPreset.MD3 -> 16
        else -> 12
    }
    return CompactCapsuleChromeSpec(
        primaryHeightDp = 44,
        secondaryButtonSizeDp = 40,
        chipHeightDp = 36,
        compactChipHeightDp = 32,
        primaryCornerRadiusDp = 22,
        secondaryButtonCornerRadiusDp = 20,
        chipCornerRadiusDp = 18,
        compactChipCornerRadiusDp = 16,
        iconSizeDp = 20,
        smallIconSizeDp = 16,
        inputHorizontalPaddingDp = inputHorizontalPadding,
        chipHorizontalPaddingDp = 12,
        compactChipHorizontalPaddingDp = 10,
        standardGapDp = 8
    )
}
