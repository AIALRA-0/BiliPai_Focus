package com.android.purebilibili.core.ui

internal enum class InteractiveOverlaySurfaceType {
    BOTTOM_SHEET,
    DIALOG,
    DRAWER
}

internal data class InteractiveOverlayProgressVisual(
    val scrimAlpha: Float,
    val surfaceAlphaMultiplier: Float,
    val blurEnabled: Boolean,
    val forceLowBlurBudget: Boolean
)

internal fun resolveInteractiveOverlayProgressVisual(
    presentationProgress: Float,
    surfaceType: InteractiveOverlaySurfaceType,
    blurActive: Boolean,
    maxScrimAlpha: Float
): InteractiveOverlayProgressVisual {
    val progress = presentationProgress.coerceIn(0f, 1f)
    val minimumSurfaceAlpha = when (surfaceType) {
        InteractiveOverlaySurfaceType.BOTTOM_SHEET -> 0.88f
        InteractiveOverlaySurfaceType.DIALOG -> 0.92f
        InteractiveOverlaySurfaceType.DRAWER -> 0.86f
    }
    return InteractiveOverlayProgressVisual(
        scrimAlpha = maxScrimAlpha.coerceAtLeast(0f) * progress,
        surfaceAlphaMultiplier = minimumSurfaceAlpha + (1f - minimumSurfaceAlpha) * progress,
        blurEnabled = blurActive && progress > 0.001f,
        // 半开或关闭过程中降低实时模糊预算，避免拖拽时用满强度 blur。
        forceLowBlurBudget = progress < 0.999f
    )
}
