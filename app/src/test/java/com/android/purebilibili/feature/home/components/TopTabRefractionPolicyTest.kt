package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabRefractionPolicyTest {

    @Test
    fun `liquid glass top tabs reuse capsule or underline indicator shape`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")

        assertTrue(source.contains("shouldUseMovingIosCapsule"))
        assertTrue(source.contains("shouldUseLiquidGlassIndicator"))
        assertTrue(source.contains("shouldForceDragLiquidGlassIndicator"))
        assertTrue(source.contains("KernelSuBottomBarIndicatorLayer("))
        assertFalse(source.contains("BottomBarLiquidIndicatorSurface("))
        assertTrue(source.contains("resolveBottomBarRefractionMotionProfile("))
        assertTrue(source.contains("resolveBottomBarBackdropPresetIndicatorLens("))
        assertTrue(source.contains("topTabShouldStretchIndicator"))
        assertTrue(source.contains("val shouldPrimeTopTabLiquidGlassCapture ="))
        assertTrue(source.contains("(isLiquidGlassEnabled || backdrop != null)"))
        assertTrue(source.contains("val topTabContentBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("rememberCombinedBackdrop(backdrop, topTabContentBackdrop)"))
        assertTrue(source.contains("contentBackdrop = effectiveTopTabIndicatorContentBackdrop"))
        assertTrue(source.contains("indicatorHeight = 4.dp"))
    }

    @Test
    fun `indicator should not refract when stationary on integer page`() {
        assertFalse(
            shouldTopTabIndicatorUseRefraction(
                position = 1.0f,
                interacting = false,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `indicator should refract while dragging horizontally`() {
        assertTrue(
            shouldTopTabIndicatorUseRefraction(
                position = 1.04f,
                interacting = true,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `indicator should not refract for pager scroll flag without horizontal movement`() {
        assertFalse(
            shouldTopTabIndicatorUseRefraction(
                position = 1.0f,
                interacting = true,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `indicator should refract during settle phase`() {
        assertTrue(
            shouldTopTabIndicatorUseRefraction(
                position = 1.18f,
                interacting = false,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `liquid top tab refraction forces chromatic aberration during motion`() {
        val profile = resolveTopTabRefractionMotionProfile(
            shouldRefract = true,
            velocityPxPerSecond = 1800f,
            liquidGlassEnabled = true
        )

        assertTrue(profile.forceChromaticAberration)
        assertEquals(1f, profile.lensAmountScale, 0.001f)
        assertEquals(1f, profile.lensHeightScale, 0.001f)
    }

    @Test
    fun `top tab refraction profile stays neutral when liquid glass is disabled`() {
        val profile = resolveTopTabRefractionMotionProfile(
            shouldRefract = true,
            velocityPxPerSecond = 1800f,
            liquidGlassEnabled = false
        )

        assertFalse(profile.forceChromaticAberration)
        assertEquals(1f, profile.lensAmountScale, 0.001f)
        assertEquals(1f, profile.lensHeightScale, 0.001f)
        assertEquals(0f, profile.indicatorPanelOffsetFraction, 0.001f)
        assertEquals(0f, profile.visiblePanelOffsetFraction, 0.001f)
        assertEquals(0f, profile.exportPanelOffsetFraction, 0.001f)
    }

    @Test
    fun `stationary top tab indicator follows bottom bar static tint policy`() {
        val policy = resolveTopTabIndicatorVisualPolicy(
            position = 2f,
            interacting = false,
            velocityPxPerSecond = 0f,
            useNeutralIndicatorTint = true
        )

        assertFalse(policy.isInMotion)
        assertFalse(policy.shouldRefract)
        assertFalse(policy.useNeutralTint)
    }

    @Test
    fun `moving top tab indicator follows bottom bar refraction tint policy`() {
        val policy = resolveTopTabIndicatorVisualPolicy(
            position = 2.18f,
            interacting = false,
            velocityPxPerSecond = 0f,
            useNeutralIndicatorTint = true
        )

        assertTrue(policy.isInMotion)
        assertTrue(policy.shouldRefract)
        assertTrue(policy.useNeutralTint)
    }

    @Test
    fun `moving top tab item emphasis follows bottom bar text rendering policy`() {
        val top = resolveTopTabItemMotionVisual(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            isInMotion = true,
            selectionEmphasis = 0.28f
        )
        val bottom = resolveBottomBarItemMotionVisual(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f,
            selectionEmphasis = 0.28f
        )

        assertEquals(bottom.themeWeight, top.themeWeight, 0.001f)
        assertEquals(bottom.useSelectedIcon, top.useSelectedIcon)
    }

    @Test
    fun `moving top tab refraction profile follows bottom bar chromatic policy`() {
        val top = resolveTopTabRefractionMotionProfile(
            position = 1.32f,
            shouldRefract = true,
            velocityPxPerSecond = 860f,
            liquidGlassEnabled = true
        )
        val bottom = resolveBottomBarRefractionMotionProfile(
            position = 1.32f,
            velocity = 860f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.IOS_FLOATING)
        )

        assertEquals(1f, top.lensAmountScale, 0.001f)
        assertEquals(1f, top.lensHeightScale, 0.001f)
        assertEquals(bottom.visibleSelectionEmphasis, top.visibleSelectionEmphasis, 0.001f)
        assertEquals(bottom.exportSelectionEmphasis, top.exportSelectionEmphasis, 0.001f)
        assertEquals(bottom.indicatorPanelOffsetFraction, top.indicatorPanelOffsetFraction, 0.001f)
        assertEquals(bottom.visiblePanelOffsetFraction, top.visiblePanelOffsetFraction, 0.001f)
        assertEquals(bottom.exportPanelOffsetFraction, top.exportPanelOffsetFraction, 0.001f)
    }

    @Test
    fun `top tab indicator uses bottom bar stretch ratio while sliding`() {
        val transform = resolveTopTabIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.IOS_FLOATING)
        )
        val bottom = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.IOS_FLOATING)
        )

        assertEquals(bottom.scaleX, transform.scaleX, 0.001f)
        assertEquals(bottom.scaleY, transform.scaleY, 0.001f)
        assertEquals(88f / 56f, transform.scaleX, 0.001f)
        assertEquals(88f / 56f, transform.scaleY, 0.001f)
    }

    @Test
    fun `top tab indicator deformation ignores vertical page scrolling`() {
        assertFalse(
            shouldDeformTopTabIndicator(
                position = 2f,
                isInMotion = true
            )
        )
        assertFalse(
            shouldDeformTopTabIndicator(
                position = 2.004f,
                isInMotion = true
            )
        )
        assertTrue(
            shouldDeformTopTabIndicator(
                position = 2.02f,
                isInMotion = true
            )
        )
    }

    @Test
    fun `liquid top tab indicator keeps stable backdrop while idle`() {
        val idle = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )
        val moving = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertFalse(idle.useCombinedBackdrop)
        assertTrue(idle.useIndicatorBackdrop)
        assertTrue(moving.useCombinedBackdrop)
        assertTrue(moving.useIndicatorBackdrop)
    }

    @Test
    fun `liquid top tab indicator keeps content backdrop when page backdrop is unavailable`() {
        val idle = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = false,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )

        assertTrue(idle.useIndicatorBackdrop)
        assertFalse(idle.useCombinedBackdrop)
    }

    @Test
    fun `liquid top tab keeps content sampling layer when page backdrop is unavailable`() {
        val moving = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = false,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertTrue(moving.useIndicatorBackdrop)
        assertFalse(moving.useCombinedBackdrop)
    }

    @Test
    fun `top tab render material keeps glass ahead of blur`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveTopTabRenderMaterialMode(
                liquidGlassEnabled = true,
                hasHazeState = true
            )
        )
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveTopTabRenderMaterialMode(
                liquidGlassEnabled = false,
                hasHazeState = true
            )
        )
        assertEquals(
            TopTabMaterialMode.PLAIN,
            resolveTopTabRenderMaterialMode(
                liquidGlassEnabled = false,
                hasHazeState = false
            )
        )
    }

    @Test
    fun `home top tab row uses lightweight pager aware tabs with shared liquid renderer`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt"
        )

        assertTrue(source.contains("LightweightHomeTopTabs("))
        assertTrue(source.contains("resolveTopTabIndicatorRenderPosition("))
        assertTrue(source.contains("pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction"))
        assertTrue(source.contains("resolveTopTabClickAction(index, selectedIndex)"))
        assertTrue(source.contains("KernelSuBottomBarIndicatorLayer("))
        assertFalse(source.contains("BottomBarLiquidIndicatorSurface("))
        assertFalse(source.contains("LiquidIndicator("))
        assertFalse(source.contains("SimpleLiquidIndicator("))
        assertFalse(source.contains("BottomBarStyleIndicatorSurface("))
        assertFalse(source.contains("drawBackdrop("))
        assertFalse(source.contains(".layerBackdrop(tabsBackdrop)"))
        assertFalse(source.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(source.contains("if (shouldPrimeTopTabLiquidGlassCapture)"))
        assertTrue(source.contains("layerBackdrop(topTabContentBackdrop)"))
    }

    @Test
    fun `md3 liquid top tab uses moving capsule instead of bottom underline`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt"
        )

        assertTrue(source.contains("val shouldUseMd3LiquidCapsule = effectiveRenderer == HomeTopTabRenderer.MD3"))
        assertTrue(source.contains("val shouldUseMd3DockBackedCapsule = effectiveRenderer == HomeTopTabRenderer.MD3"))
        assertTrue(source.contains("indicatorLayerScaleProgress = topTabIndicatorLayerScaleProgress"))
        assertTrue(source.contains("indicatorHeight = dockIndicatorHeight"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
