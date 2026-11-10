package com.android.purebilibili.core.ui.blur

enum class BlurHazeMaterial {
    ULTRA_THIN,
    THIN,
    THICK,
}

// 档位与官方材质按「淡化背景」程度对齐：轻度→ultraThin、中度→thin、重度→thick。
// 染色浓度是三档的唯一差异（官方半径统一 24dp），暗色下轻度与中度同样可区分。
fun resolveBlurHazeMaterial(intensity: BlurIntensity): BlurHazeMaterial {
    return when (intensity) {
        BlurIntensity.THIN -> BlurHazeMaterial.ULTRA_THIN
        BlurIntensity.APPLE_DOCK -> BlurHazeMaterial.THICK
        BlurIntensity.THICK -> BlurHazeMaterial.THIN
    }
}

fun resolveBlurBackgroundAlpha(intensity: BlurIntensity): Float {
    return when (intensity) {
        BlurIntensity.THIN -> 0.15f
        BlurIntensity.APPLE_DOCK -> 0.6f
        BlurIntensity.THICK -> 0.4f
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
