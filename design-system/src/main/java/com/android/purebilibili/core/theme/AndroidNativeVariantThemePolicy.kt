package com.android.purebilibili.core.theme

import androidx.compose.material3.MotionScheme
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
    uiStyle: AppUiStyle
): AndroidNativeChromeTokens = when (uiStyle) {
    AppUiStyle.MIUIX -> AndroidNativeChromeTokens(
        containerCornerRadiusDp = 20,
        pillCornerRadiusDp = 22,
        selectedContainerAlpha = 0.18f,
        tonalSurfaceElevationDp = 0,
        denseHorizontalSpacingDp = 16,
        // Miuix controls use denser visual geometry. A 48dp visual minimum makes
        // buttons and preference rows look inflated; accessibility hit slop is
        // handled by the component instead of enlarging its visible container.
        rowMinTouchTargetDp = 44,
        expressiveMotionDurationMillis = 180,
        motionScale = 1f,
        motionStandardMillis = 180,
        motionEmphasizedMillis = 240
    )
    AppUiStyle.MATERIAL3 -> AndroidNativeChromeTokens(
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
}

fun resolveCornerRadiusScale(
    uiStyle: AppUiStyle
): Float = when (uiStyle) {
    AppUiStyle.MIUIX -> MIUIX_CORNER_RADIUS_SCALE
    AppUiStyle.MATERIAL3 -> MD3_CORNER_RADIUS_SCALE
}

fun shouldUseMiuixSmoothRounding(
    uiStyle: AppUiStyle
): Boolean = uiStyle == AppUiStyle.MIUIX

fun resolveMaterialTypography(
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean = true,
): Typography = when (uiStyle) {
    // Miuix 主题始终使用完整的标准字阶（正文 17sp），液态玻璃仅作用于局部 Dock 与指示器，不压缩全局排版
    AppUiStyle.MIUIX -> BiliMiuixTypography
    AppUiStyle.MATERIAL3 -> Md3Typography
}

fun resolveMaterialMotionScheme(
    uiStyle: AppUiStyle
): MotionScheme = when (uiStyle) {
    AppUiStyle.MATERIAL3 -> MotionScheme.expressive()
    AppUiStyle.MIUIX -> MotionScheme.standard()
}

fun resolveMaterialShapes(
    uiStyle: AppUiStyle
): Shapes = when (uiStyle) {
    AppUiStyle.MIUIX -> MiuixAlignedShapes
    AppUiStyle.MATERIAL3 -> Md3Shapes
}
