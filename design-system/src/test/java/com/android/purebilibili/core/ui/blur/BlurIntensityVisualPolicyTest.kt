package com.android.purebilibili.core.ui.blur

import kotlin.test.Test
import kotlin.test.assertEquals

class BlurIntensityVisualPolicyTest {

    @Test
    fun `glass dock uses strongest haze material and highest cover alpha`() {
        assertEquals(BlurHazeMaterial.THICK, resolveBlurHazeMaterial(BlurIntensity.APPLE_DOCK))
        assertEquals(0.6f, resolveBlurBackgroundAlpha(BlurIntensity.APPLE_DOCK))
    }

    @Test
    fun `medium tier stays between light and heavy`() {
        assertEquals(BlurHazeMaterial.THIN, resolveBlurHazeMaterial(BlurIntensity.THICK))
        assertEquals(0.4f, resolveBlurBackgroundAlpha(BlurIntensity.THICK))
    }

    @Test
    fun `light tier keeps background most visible`() {
        assertEquals(BlurHazeMaterial.ULTRA_THIN, resolveBlurHazeMaterial(BlurIntensity.THIN))
        assertEquals(0.15f, resolveBlurBackgroundAlpha(BlurIntensity.THIN))
    }

    @Test
    fun `budget clamp follows visual intensity order`() {
        val budget = BlurBudget(
            maxBlurLevel = 1,
            backgroundAlphaMultiplier = 1.0f,
            allowRealtime = true,
        )

        assertEquals(
            BlurIntensity.THIN,
            resolveBudgetedBlurIntensity(BlurIntensity.APPLE_DOCK, budget),
        )
        assertEquals(
            BlurIntensity.THICK,
            resolveBudgetedBlurIntensity(BlurIntensity.THICK, budget),
        )
    }
}
