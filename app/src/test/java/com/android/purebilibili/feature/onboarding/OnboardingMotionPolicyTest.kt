package com.android.purebilibili.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingMotionPolicyTest {

    @Test
    fun pageCountKeepsSettingsGuideAsFinalPage() {
        assertEquals(5, resolveOnboardingPageCount())
        assertEquals(4, resolveOnboardingLastPageIndex())
    }

    @Test
    fun normalMotionUsesLayeredPageAndHeroAnimation() {
        val spec = resolveOnboardingMotionSpec(reduceMotion = false)

        assertTrue(spec.pager.minScale < 1f)
        assertTrue(spec.pager.minAlpha < 1f)
        assertTrue(spec.floating.translationYPx > 0f)
        assertTrue(spec.halo.maxScale > spec.halo.minScale)
        assertTrue(spec.card.selectedScale > spec.card.unselectedScale)
    }

    @Test
    fun reduceMotionDisablesLoopingMotion() {
        val spec = resolveOnboardingMotionSpec(reduceMotion = true)

        assertEquals(1f, spec.pager.minScale)
        assertEquals(1f, spec.pager.minAlpha)
        assertEquals(0f, spec.floating.translationYPx)
        assertEquals(0, spec.floating.durationMillis)
        assertEquals(1f, spec.halo.minScale)
        assertEquals(1f, spec.halo.maxScale)
        assertEquals(1f, spec.card.selectedScale)
    }
}
