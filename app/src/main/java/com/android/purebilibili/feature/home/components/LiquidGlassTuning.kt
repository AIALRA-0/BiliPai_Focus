package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.store.normalizeLiquidGlassProgress
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength
import com.android.purebilibili.core.store.resolveDefaultLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLiquidGlassStrengthFromProgress
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassMode

data class LiquidGlassTuning(
    val mode: LiquidGlassMode,
    val progress: Float,
    val strength: Float,
    val backdropBlurRadius: Float,
    val surfaceAlpha: Float,
    val whiteOverlayAlpha: Float,
    val refractIntensity: Float,
    val refractionAmount: Float,
    val refractionHeight: Float,
    val indicatorTintAlpha: Float,
    val indicatorLensBoost: Float,
    val indicatorEdgeWarpBoost: Float,
    val indicatorChromaticBoost: Float,
    val chromaticAberrationEnabled: Boolean,
    val chromaticAberrationAmount: Float,
    val scrollCoupledRefraction: Boolean,
    val scrollCoupledRefractionAmount: Float,
    val useNeutralIndicatorTint: Boolean,
    val neutralIndicatorTintAmount: Float,
    val depthEffectEnabled: Boolean,
    val depthEffectAmount: Float
)

internal fun resolveLiquidGlassTuning(progress: Float): LiquidGlassTuning {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    val mode = when {
        normalizedProgress < 0.34f -> LiquidGlassMode.CLEAR
        normalizedProgress < 0.68f -> LiquidGlassMode.BALANCED
        else -> LiquidGlassMode.FROSTED
    }
    val frostWeight = normalizedProgress
    val chromaticAmount = (1f - normalizedProgress).coerceIn(0f, 1f) * 0.18f
    val scrollCouplingAmount = (1f - normalizedProgress * 1.15f).coerceIn(0f, 1f)
    val neutralTintAmount = (1f - normalizedProgress * 2.2f).coerceIn(0f, 1f)
    val depthEffectAmount = (1f - normalizedProgress * 1.2f).coerceIn(0f, 1f)
    return LiquidGlassTuning(
        mode = mode,
        progress = normalizedProgress,
        strength = resolveLiquidGlassStrengthFromProgress(normalizedProgress),
        backdropBlurRadius = lerp(3f, 30f, normalizedProgress),
        surfaceAlpha = lerp(0.12f, 0.42f, normalizedProgress),
        whiteOverlayAlpha = lerp(0.012f, 0.11f, normalizedProgress),
        refractIntensity = lerp(0.5f, 0.14f, normalizedProgress),
        refractionAmount = lerp(26f, 8f, normalizedProgress),
        refractionHeight = lerp(22f, 8f, normalizedProgress),
        indicatorTintAlpha = lerp(0.20f, 0.34f, normalizedProgress),
        indicatorLensBoost = lerp(1.72f, 1.04f, frostWeight),
        indicatorEdgeWarpBoost = lerp(1.78f, 1.08f, frostWeight),
        indicatorChromaticBoost = lerp(1.36f, 0.82f, frostWeight),
        chromaticAberrationEnabled = chromaticAmount > 0.01f,
        chromaticAberrationAmount = chromaticAmount,
        scrollCoupledRefraction = scrollCouplingAmount > 0.01f,
        scrollCoupledRefractionAmount = scrollCouplingAmount,
        useNeutralIndicatorTint = neutralTintAmount > 0.5f,
        neutralIndicatorTintAmount = neutralTintAmount,
        depthEffectEnabled = depthEffectAmount > 0.08f,
        depthEffectAmount = depthEffectAmount
    )
}

internal fun resolveLiquidGlassTuning(
    mode: LiquidGlassMode,
    strength: Float
): LiquidGlassTuning {
    return resolveLiquidGlassTuning(
        progress = resolveLegacyLiquidGlassProgress(
            mode = mode,
            strength = normalizeLiquidGlassStrength(strength)
        )
    )
}

internal fun resolveLiquidGlassTuning(style: LiquidGlassStyle): LiquidGlassTuning {
    return when (style) {
        LiquidGlassStyle.SUKISU -> sukisuLiquidGlassTuning()
        else -> resolveLiquidGlassTuning(progress = resolveLegacyLiquidGlassProgress(style))
    }
}

private fun sukisuLiquidGlassTuning(): LiquidGlassTuning {
    return LiquidGlassTuning(
        mode = LiquidGlassMode.BALANCED,
        progress = 0.5f,
        strength = resolveDefaultLiquidGlassStrength(LiquidGlassMode.BALANCED),
        backdropBlurRadius = 8f,
        surfaceAlpha = 0.40f,
        whiteOverlayAlpha = 0.04f,
        refractIntensity = 0.28f,
        refractionAmount = 24f,
        refractionHeight = 24f,
        indicatorTintAlpha = 0.28f,
        indicatorLensBoost = 1.18f,
        indicatorEdgeWarpBoost = 1.16f,
        indicatorChromaticBoost = 0.90f,
        chromaticAberrationEnabled = false,
        chromaticAberrationAmount = 0f,
        scrollCoupledRefraction = false,
        scrollCoupledRefractionAmount = 0f,
        useNeutralIndicatorTint = false,
        neutralIndicatorTintAmount = 0f,
        depthEffectEnabled = true,
        depthEffectAmount = 1f
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
