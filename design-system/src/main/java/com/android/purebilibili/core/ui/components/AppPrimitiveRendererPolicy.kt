package com.android.purebilibili.core.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.isMiuixNonGlassEnabled

internal fun shouldUseMiuixOutlinedTextField(
    uiStyle: AppUiStyle,
    hasPrefix: Boolean,
    hasSuffix: Boolean,
    liquidGlassEnabled: Boolean = false,
): Boolean = uiStyle == AppUiStyle.MIUIX && (!liquidGlassEnabled || (!hasPrefix && !hasSuffix))

/**
 * Native Miuix action primitives are used when UI style is MIUIX.
 * Liquid glass affects dock and indicator chrome while preserving standard Miuix action primitives.
 */
internal fun shouldUseMiuixActionPrimitive(
    uiStyle: AppUiStyle,
): Boolean = uiStyle == AppUiStyle.MIUIX

@Deprecated("Action primitives now consistently follow Miuix theme regardless of liquid glass.")
internal fun shouldUseMiuixNonGlassActionPrimitive(
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean = false,
): Boolean = uiStyle == AppUiStyle.MIUIX

internal enum class AppMiuixActionTone {
    PRIMARY,
    SECONDARY,
}

internal fun resolveMiuixChipActionTone(selected: Boolean): AppMiuixActionTone =
    if (selected) AppMiuixActionTone.PRIMARY else AppMiuixActionTone.SECONDARY

internal data class AppMiuixCompactChipMetrics(
    val minHeightDp: Int,
    val minWidthDp: Int,
    val cornerRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val iconGapDp: Int,
)

/** Compact filter chips stay denser than official Button 40/16; 10dp follows the non-glass compact-filter role. */
internal fun resolveMiuixNonGlassChipMetrics(): AppMiuixCompactChipMetrics =
    AppMiuixCompactChipMetrics(
        minHeightDp = 36,
        minWidthDp = 52,
        cornerRadiusDp = 10,
        horizontalPaddingDp = 12,
        iconGapDp = 8,
    )

internal fun resolveMiuixFabMinSizeDp(small: Boolean): Int = if (small) 48 else 60

internal fun resolveMiuixFabContainerColor(
    requested: Color,
    defaultMaterialContainer: Color,
    miuixDefaultContainer: Color,
): Color = if (requested == defaultMaterialContainer) miuixDefaultContainer else requested

internal fun resolveMiuixFabContentColor(
    requested: Color,
    defaultMaterialContent: Color,
    miuixDefaultContent: Color,
): Color = if (requested == defaultMaterialContent) miuixDefaultContent else requested
