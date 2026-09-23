package com.android.purebilibili.core.ui.blur

enum class BlurHazeMaterial {
    ULTRA_THIN,
    THIN,
    THICK,
}

fun resolveBlurHazeMaterial(intensity: BlurIntensity): BlurHazeMaterial {
    return when (intensity) {
        BlurIntensity.THIN -> BlurHazeMaterial.THIN
        BlurIntensity.APPLE_DOCK -> BlurHazeMaterial.THICK
        BlurIntensity.THICK -> BlurHazeMaterial.ULTRA_THIN
    }
}

fun resolveBlurBackgroundAlpha(intensity: BlurIntensity): Float {
    return when (intensity) {
        BlurIntensity.THIN -> 0.4f
        BlurIntensity.APPLE_DOCK -> 0.6f
        BlurIntensity.THICK -> 0.15f
    }
}

val blurIntensityLevelOrder = listOf(
    BlurIntensity.THICK,
    BlurIntensity.THIN,
    BlurIntensity.APPLE_DOCK,
)

fun resolveBudgetedBlurIntensity(
    preferred: BlurIntensity,
    budget: BlurBudget,
): BlurIntensity {
    val preferredLevel = blurIntensityLevelOrder.indexOf(preferred).coerceAtLeast(0)
    val cappedLevel = minOf(preferredLevel, budget.maxBlurLevel)
    return blurIntensityLevelOrder[cappedLevel]
}
