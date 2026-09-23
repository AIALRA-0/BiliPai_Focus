package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import androidx.compose.ui.unit.dp

object AppChromeSizeTokens {
    val MinimumTouchTarget = 48.dp
    const val CompactControlHeightDp = 44
    const val CompactControlCornerRadiusDp = 20
    // Native Miuix controls use compact visual geometry while retaining their
    // surrounding accessibility hit target.
    const val MiuixNativeCompactControlHeightDp = 36
    const val MiuixNativeCompactControlMinWidthDp = 52
    const val MiuixNativeCompactCornerRadiusDp = 10
    const val MiuixNativeTabHeightDp = 42
    const val MiuixNativeTabCornerRadiusDp = 12
    // Compact liquid docks keep the home bottom-bar rest fill (48/64).
    const val BottomBarMatchedSegmentedControlHeightDp = 40
    const val BottomBarMatchedSegmentedIndicatorHeightDp = 30
}

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
    uiStyle: AppUiStyle
): CompactCapsuleChromeSpec {
    val chromeTokens = resolveAndroidNativeChromeTokens(uiStyle)
    return when (uiStyle) {
        // primaryCornerRadius must stay well below height/2 or search/filter bars
        // become full sausages (same failure mode as 48dp TabRow + 22–28dp Pill).
        AppUiStyle.MIUIX -> {
            val primaryHeight = 48
            CompactCapsuleChromeSpec(
                primaryHeightDp = primaryHeight,
                secondaryButtonSizeDp = chromeTokens.rowMinTouchTargetDp,
                chipHeightDp = 32,
                compactChipHeightDp = 28,
                primaryCornerRadiusDp = minOf(
                    chromeTokens.containerCornerRadiusDp, // 20
                    (primaryHeight * 0.3f).toInt(), // 14
                ),
                secondaryButtonCornerRadiusDp = minOf(
                    chromeTokens.containerCornerRadiusDp,
                    (chromeTokens.rowMinTouchTargetDp * 0.3f).toInt(),
                ),
                chipCornerRadiusDp = minOf(16, (32 * 0.3f).toInt()), // 9 → keep 9
                compactChipCornerRadiusDp = minOf(14, (28 * 0.3f).toInt()),
                iconSizeDp = 20,
                smallIconSizeDp = 16,
                inputHorizontalPaddingDp = 14,
                chipHorizontalPaddingDp = 12,
                compactChipHorizontalPaddingDp = 10,
                standardGapDp = 8
            )
        }
        AppUiStyle.MATERIAL3 -> {
            val primaryHeight = 56
            CompactCapsuleChromeSpec(
                primaryHeightDp = primaryHeight,
                secondaryButtonSizeDp = chromeTokens.rowMinTouchTargetDp,
                chipHeightDp = 32,
                compactChipHeightDp = 28,
                primaryCornerRadiusDp = minOf(
                    chromeTokens.containerCornerRadiusDp, // 24
                    (primaryHeight * 0.3f).toInt(), // 16
                ),
                secondaryButtonCornerRadiusDp = minOf(
                    chromeTokens.containerCornerRadiusDp,
                    (chromeTokens.rowMinTouchTargetDp * 0.3f).toInt(),
                ),
                chipCornerRadiusDp = 8,
                compactChipCornerRadiusDp = 8,
                iconSizeDp = 24,
                smallIconSizeDp = 18,
                inputHorizontalPaddingDp = 16,
                chipHorizontalPaddingDp = 16,
                compactChipHorizontalPaddingDp = 12,
                standardGapDp = 12
            )
        }
    }
}
