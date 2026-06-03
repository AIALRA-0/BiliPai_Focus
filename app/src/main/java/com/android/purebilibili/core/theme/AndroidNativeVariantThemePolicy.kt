package com.android.purebilibili.core.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

internal const val MD3_CORNER_RADIUS_SCALE = 0.9f
internal const val MIUIX_CORNER_RADIUS_SCALE = 1.15f

data class AndroidNativeChromeTokens(
    val containerCornerRadiusDp: Int,
    val pillCornerRadiusDp: Int,
    val selectedContainerAlpha: Float,
    val tonalSurfaceElevationDp: Int,
    val denseHorizontalSpacingDp: Int,
    val rowMinTouchTargetDp: Int,
    val expressiveMotionDurationMillis: Int,
    val motionScale: Float,
    val motionStandardMillis: Int,
    val motionEmphasizedMillis: Int
)

fun resolveAndroidNativeChromeTokens(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AndroidNativeChromeTokens {
    return when {
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 20,
            pillCornerRadiusDp = 22,
            selectedContainerAlpha = 0.18f,
            tonalSurfaceElevationDp = 0,
            denseHorizontalSpacingDp = 16,
            rowMinTouchTargetDp = 48,
            expressiveMotionDurationMillis = 180,
            motionScale = 1f,
            motionStandardMillis = 180,
            motionEmphasizedMillis = 240
        )
        uiPreset == UiPreset.MD3 -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 24,
            pillCornerRadiusDp = 28,
            selectedContainerAlpha = 0.14f,
            tonalSurfaceElevationDp = 3,
            denseHorizontalSpacingDp = 18,
            rowMinTouchTargetDp = 48,
            expressiveMotionDurationMillis = 200,
            motionScale = 1f,
            motionStandardMillis = 200,
            motionEmphasizedMillis = 300
        )
        else -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 20,
            pillCornerRadiusDp = 10,
            selectedContainerAlpha = 0.12f,
            tonalSurfaceElevationDp = 1,
            denseHorizontalSpacingDp = 16,
            rowMinTouchTargetDp = 44,
            expressiveMotionDurationMillis = 180,
            motionScale = 1f,
            // Nominal iOS values; iOS motion specs use spring, not tween.
            motionStandardMillis = 280,
            motionEmphasizedMillis = 360
        )
    }
}

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
