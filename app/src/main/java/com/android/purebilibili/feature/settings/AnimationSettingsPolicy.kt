package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.normalizeLiquidGlassProgress
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress

internal data class LiquidGlassPreviewUiState(
    val modeLabel: String,
    val subtitle: String,
    val normalizedProgress: Float,
    val strengthLabel: String
)

internal fun resolveLiquidGlassPreviewUiState(
    progress: Float
): LiquidGlassPreviewUiState {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    val (modeLabel, subtitle) = when {
        normalizedProgress < 0.34f -> "通透" to "更清晰、更通透，折射更明显"
        normalizedProgress < 0.68f -> "柔化" to "开始柔化背景，但仍保留液态折射"
        else -> "磨砂" to "更柔和、更雾化，适合弱化背景干扰"
    }
    return LiquidGlassPreviewUiState(
        modeLabel = modeLabel,
        subtitle = subtitle,
        normalizedProgress = normalizedProgress,
        strengthLabel = "${(normalizedProgress * 100).toInt()}%"
    )
}

internal fun resolveLiquidGlassPreviewUiState(
    mode: LiquidGlassMode,
    strength: Float
): LiquidGlassPreviewUiState {
    return resolveLiquidGlassPreviewUiState(
        progress = resolveLegacyLiquidGlassProgress(
            mode = mode,
            strength = normalizeLiquidGlassStrength(strength)
        )
    )
}
