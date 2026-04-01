package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarIndicatorPolicyTest {

    @Test
    fun `five or more items stays close to top floating indicator family`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 5)
        val topTuning = resolveTopTabVisualTuning()

        assertEquals(topTuning.floatingIndicatorWidthMultiplier + 0.02f, policy.widthMultiplier)
        assertEquals(topTuning.floatingIndicatorMinWidthDp + 2f, policy.minWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthDp + 2f, policy.maxWidthDp)
        assertEquals(true, policy.clampToBounds)
        assertEquals(topTuning.floatingIndicatorMaxWidthToItemRatio + 0.02f, policy.maxWidthToItemRatio)
    }

    @Test
    fun `four items is only slightly wider than five item geometry`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 4)
        val topTuning = resolveTopTabVisualTuning()

        assertEquals(topTuning.floatingIndicatorWidthMultiplier + 0.04f, policy.widthMultiplier)
        assertEquals(topTuning.floatingIndicatorMinWidthDp + 4f, policy.minWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthDp + 4f, policy.maxWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthToItemRatio + 0.04f, policy.maxWidthToItemRatio)
        assertEquals(true, policy.clampToBounds)
    }

    @Test
    fun `icon and text mode with five items uses flatter indicator height on phone`() {
        assertEquals(
            50f,
            resolveBottomIndicatorHeightDp(
                labelMode = 0,
                isTablet = false,
                itemCount = 5
            )
        )
    }

    @Test
    fun `static indicator keeps theme color and disables refraction`() {
        val policy = resolveBottomBarIndicatorVisualPolicy(
            position = 2f,
            isDragging = false,
            velocity = 0f,
            useNeutralIndicatorTint = true
        )

        assertFalse(policy.isInMotion)
        assertFalse(policy.shouldRefract)
        assertFalse(policy.useNeutralTint)
    }

    @Test
    fun `moving indicator can use neutral tint and refraction`() {
        val policy = resolveBottomBarIndicatorVisualPolicy(
            position = 2.15f,
            isDragging = false,
            velocity = 52f,
            useNeutralIndicatorTint = true
        )

        assertTrue(policy.isInMotion)
        assertTrue(policy.shouldRefract)
        assertTrue(policy.useNeutralTint)
    }

    @Test
    fun `refraction layer enables tinted export only for floating moving glass`() {
        val active = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )
        val idle = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )
        val docked = resolveBottomBarRefractionLayerPolicy(
            isFloating = false,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertTrue(active.captureTintedContentLayer)
        assertFalse(active.useCombinedBackdrop)
        assertFalse(idle.captureTintedContentLayer)
        assertFalse(idle.useCombinedBackdrop)
        assertFalse(docked.captureTintedContentLayer)
        assertFalse(docked.useCombinedBackdrop)
    }

    @Test
    fun `moving refraction profile adds panel offset and suppresses visible emphasis`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 1.32f,
            velocity = 860f,
            isDragging = true
        )

        assertTrue(profile.progress > 0f)
        assertEquals(0f, profile.exportPanelOffsetFraction)
        assertEquals(0f, profile.indicatorPanelOffsetFraction)
        assertEquals(0f, profile.visiblePanelOffsetFraction)
        assertTrue(profile.forceChromaticAberration)
        assertTrue(profile.visibleSelectionEmphasis < 1f)
        assertEquals(1f, profile.exportSelectionEmphasis)
        assertEquals(1f, profile.exportCaptureWidthScale)
        assertTrue(profile.indicatorLensAmountScale < 1f)
        assertTrue(profile.indicatorLensHeightScale < 1f)
    }

    @Test
    fun `idle refraction profile disables offset and keeps full visible emphasis`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 2f,
            velocity = 0f,
            isDragging = false
        )

        assertEquals(0f, profile.progress)
        assertEquals(0f, profile.exportPanelOffsetFraction)
        assertEquals(0f, profile.indicatorPanelOffsetFraction)
        assertEquals(0f, profile.visiblePanelOffsetFraction)
        assertFalse(profile.forceChromaticAberration)
        assertEquals(1f, profile.visibleSelectionEmphasis)
        assertEquals(1f, profile.exportSelectionEmphasis)
        assertEquals(1f, profile.exportCaptureWidthScale)
        assertEquals(1f, profile.indicatorLensAmountScale)
        assertEquals(1f, profile.indicatorLensHeightScale)
    }

    @Test
    fun `bottom bar outer shell preserves liquid glass when glass effect is enabled`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveBottomBarChromeMaterialMode(
                showGlassEffect = true,
                hasBlur = true
            )
        )
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveBottomBarChromeMaterialMode(
                showGlassEffect = false,
                hasBlur = true
            )
        )
        assertEquals(
            TopTabMaterialMode.PLAIN,
            resolveBottomBarChromeMaterialMode(
                showGlassEffect = false,
                hasBlur = false
            )
        )
    }

    @Test
    fun `frosted idle indicator keeps stronger alpha floor for visibility`() {
        val alpha = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 1f,
            configuredAlpha = 0.14f
        )

        assertTrue(alpha >= 0.32f)
    }

    @Test
    fun `clear idle indicator stays lighter than frosted idle indicator`() {
        val clear = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 0f,
            configuredAlpha = 0.14f
        )
        val frosted = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 1f,
            configuredAlpha = 0.14f
        )

        assertTrue(clear < frosted)
    }
}
