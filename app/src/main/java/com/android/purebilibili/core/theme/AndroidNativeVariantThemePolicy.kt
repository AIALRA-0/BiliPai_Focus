package com.android.purebilibili.core.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

internal const val MD3_CORNER_RADIUS_SCALE = 0.9f
internal const val MIUIX_CORNER_RADIUS_SCALE = 1.15f

fun resolveMaterialTypography(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Typography {
    return when {
        uiPreset == UiPreset.IOS -> BiliTypography
        androidNativeVariant == AndroidNativeVariant.MIUIX -> BiliMiuixTypography
        else -> BiliTypography
    }
}

fun resolveMaterialShapes(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Shapes {
    return when {
        uiPreset == UiPreset.IOS -> iOSShapes
        androidNativeVariant == AndroidNativeVariant.MIUIX -> MiuixAlignedShapes
        else -> Md3Shapes
    }
}

fun resolveCornerRadiusScale(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Float {
    return when {
        uiPreset == UiPreset.IOS -> 1f
        androidNativeVariant == AndroidNativeVariant.MIUIX -> MIUIX_CORNER_RADIUS_SCALE
        else -> MD3_CORNER_RADIUS_SCALE
    }
}

fun shouldUseMiuixSmoothRounding(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX
