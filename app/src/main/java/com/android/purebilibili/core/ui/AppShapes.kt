package com.android.purebilibili.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import com.android.purebilibili.core.theme.resolveCornerRadiusScale

/** Semantic container categories shared across the three UI presets. */
enum class ContainerLevel {
    /** Tiny tags / badges. iOS base = 4dp. */
    Tag,
    /** Small chips / micro-buttons. iOS base = 6dp. */
    Chip,
    /** Input fields, search bars, small chip-like containers. iOS base = 10dp. */
    Field,
    /** Standard surface cards. iOS base = 12dp. */
    Card,
    /** Alert / confirm dialog containers. iOS base = 14dp. */
    Dialog,
    /** Bottom sheet / modal sheet (top-rounded). iOS base = 20dp. */
    Sheet,
    /** Floating elements — FABs, floating bars. iOS base = 28dp. */
    Floating,
    /** Pill / segmented selectors — radius comes from chrome tokens directly. */
    Pill
}

/**
 * Preset-aware shape tokens. Use [AppShapes.container] in composables instead of
 * writing literal `RoundedCornerShape(N.dp)`. iOS base values scale by
 * [com.android.purebilibili.core.theme.LocalCornerRadiusScale]; the Pill level
 * pulls directly from chrome tokens so each preset retains its native pill curvature.
 */
object AppShapes {

    private fun baseDp(level: ContainerLevel): Float = when (level) {
        ContainerLevel.Tag -> 4f
        ContainerLevel.Chip -> 6f
        ContainerLevel.Field -> 10f
        ContainerLevel.Card -> 12f
        ContainerLevel.Dialog -> 14f
        ContainerLevel.Sheet -> 20f
        ContainerLevel.Floating -> 28f
        ContainerLevel.Pill -> 0f
    }

    fun resolveContainerCornerDp(
        level: ContainerLevel,
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): Dp {
        if (level == ContainerLevel.Pill) {
            return resolveAndroidNativeChromeTokens(uiPreset, androidNativeVariant)
                .pillCornerRadiusDp.dp
        }
        val scale = resolveCornerRadiusScale(uiPreset, androidNativeVariant)
        return (baseDp(level) * scale).dp
    }

    fun resolveContainerShape(
        level: ContainerLevel,
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): Shape {
        val dp = resolveContainerCornerDp(level, uiPreset, androidNativeVariant)
        return if (level == ContainerLevel.Sheet) {
            RoundedCornerShape(topStart = dp, topEnd = dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        } else {
            RoundedCornerShape(dp)
        }
    }

    @Composable
    fun container(level: ContainerLevel): Shape = resolveContainerShape(
        level = level,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    fun containerCornerDp(level: ContainerLevel): Dp = resolveContainerCornerDp(
        level = level,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )
}
