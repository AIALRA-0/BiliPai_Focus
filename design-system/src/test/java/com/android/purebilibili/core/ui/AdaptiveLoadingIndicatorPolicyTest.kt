package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLoadingIndicatorPolicyTest {

    @Test
    fun `miuix page uses infinite orbit indicator`() {
        assertEquals(
            AdaptiveLoadingVisual.MIUIX_INFINITE,
            resolveAdaptiveLoadingVisual(
                uiStyle = AppUiStyle.MIUIX,
                density = AdaptiveLoadingDensity.PAGE,
            ),
        )
    }

    @Test
    fun `miuix compact uses circular progress`() {
        assertEquals(
            AdaptiveLoadingVisual.MIUIX_CIRCULAR,
            resolveAdaptiveLoadingVisual(
                uiStyle = AppUiStyle.MIUIX,
                density = AdaptiveLoadingDensity.COMPACT,
            ),
        )
    }

    @Test
    fun `material3 page uses official loading indicator`() {
        assertEquals(
            AdaptiveLoadingVisual.MATERIAL3_LOADING_INDICATOR,
            resolveAdaptiveLoadingVisual(
                uiStyle = AppUiStyle.MATERIAL3,
                density = AdaptiveLoadingDensity.PAGE,
            ),
        )
    }

    @Test
    fun `material3 compact uses circular progress`() {
        assertEquals(
            AdaptiveLoadingVisual.MATERIAL3_CIRCULAR,
            resolveAdaptiveLoadingVisual(
                uiStyle = AppUiStyle.MATERIAL3,
                density = AdaptiveLoadingDensity.COMPACT,
            ),
        )
    }

    @Test
    fun `size heuristic maps compact threshold`() {
        assertEquals(AdaptiveLoadingDensity.PAGE, resolveAdaptiveLoadingDensity(null))
        assertEquals(AdaptiveLoadingDensity.PAGE, resolveAdaptiveLoadingDensity(80f))
        assertEquals(AdaptiveLoadingDensity.PAGE, resolveAdaptiveLoadingDensity(33f))
        assertEquals(AdaptiveLoadingDensity.COMPACT, resolveAdaptiveLoadingDensity(32f))
        assertEquals(AdaptiveLoadingDensity.COMPACT, resolveAdaptiveLoadingDensity(24f))
    }

}
