package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.components.AppPlatformNavigationBarDisplayMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarMiuixPolicyTest {

    @Test
    fun `plain Miuix floating bar is used only without glass and blur`() {
        assertTrue(shouldUsePlainMiuixFloatingBar(glassEnabled = false, blurEnabled = false))
        assertFalse(shouldUsePlainMiuixFloatingBar(glassEnabled = true, blurEnabled = false))
        assertFalse(shouldUsePlainMiuixFloatingBar(glassEnabled = false, blurEnabled = true))

        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt")
        val plain = source.substringAfter("fun PlainMiuixFloatingBottomBar(")
            .substringBefore("val FloatingBottomBarIndicatorHeight")
        assertFalse(plain.contains("rememberChromeBackdropSource"))
        assertFalse(plain.contains("drawBackdrop"))
        assertFalse(plain.contains("lens("))
        assertTrue(plain.contains("DampedDragAnimation("))
        assertTrue(plain.contains("dragAnimation.modifier"))
        assertTrue(plain.contains("onSelectedLatest.value(targetIndex)"))
    }

    @Test
    fun `runtime low blur budget disables expensive liquid glass effects`() {
        assertTrue(
            shouldRenderBottomBarLiquidGlassEffects(
                glassEnabled = true,
                forceLowBlurBudget = false,
            )
        )
        assertFalse(
            shouldRenderBottomBarLiquidGlassEffects(
                glassEnabled = true,
                forceLowBlurBudget = true,
            )
        )
        assertFalse(
            shouldRenderBottomBarLiquidGlassEffects(
                glassEnabled = false,
                forceLowBlurBudget = false,
            )
        )

        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val renderer = source
            .substringAfter("private fun BiliPaiFloatingBottomBar(")
            .substringBefore("internal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(")
        assertTrue(renderer.contains("val effectiveGlassEnabled = shouldRenderBottomBarLiquidGlassEffects("))
        assertTrue(renderer.contains("FloatingBottomBarMode.LiquidGlass"))
        assertTrue(
            renderer.contains("effectiveGlassEnabled && miuixBackdrop != null -> FloatingBottomBarMode.LiquidGlass")
        )
        assertTrue(renderer.contains("FloatingBottomBar("))
    }

    @Test
    fun `floating android native bottom bar adopts miuix chrome defaults`() {
        val spec = resolveMd3BottomBarFloatingChromeSpec(isFloating = true)

        assertEquals(50f, spec.cornerRadiusDp)
        assertEquals(36f, spec.horizontalOutsidePaddingDp)
        assertEquals(12f, spec.innerHorizontalPaddingDp)
        assertEquals(12f, spec.itemSpacingDp)
        assertEquals(1f, spec.shadowElevationDp)
        assertFalse(spec.showDivider)
    }

    @Test
    fun `material label mode maps to matching miuix display mode`() {
        assertEquals(
            Md3BottomBarDisplayMode.IconAndText,
            resolveMd3BottomBarDisplayMode(labelMode = 0)
        )
        assertEquals(
            Md3BottomBarDisplayMode.IconOnly,
            resolveMd3BottomBarDisplayMode(labelMode = 1)
        )
        assertEquals(
            Md3BottomBarDisplayMode.TextOnly,
            resolveMd3BottomBarDisplayMode(labelMode = 2)
        )
        assertEquals(
            Md3BottomBarDisplayMode.IconAndText,
            resolveMd3BottomBarDisplayMode(labelMode = 99)
        )
    }

    @Test
    fun `platform navigation display mode maps text-only onto icon-with-selected-label`() {
        assertEquals(
            AppPlatformNavigationBarDisplayMode.ICON_AND_TEXT,
            Md3BottomBarDisplayMode.IconAndText.toAppPlatformNavigationDisplayMode()
        )
        assertEquals(
            AppPlatformNavigationBarDisplayMode.ICON_ONLY,
            Md3BottomBarDisplayMode.IconOnly.toAppPlatformNavigationDisplayMode()
        )
        assertEquals(
            AppPlatformNavigationBarDisplayMode.ICON_WITH_SELECTED_LABEL,
            Md3BottomBarDisplayMode.TextOnly.toAppPlatformNavigationDisplayMode()
        )
    }

    @Test
    fun `official miuix navigation item is used without skin chrome`() {
        assertTrue(
            shouldUseMiuixOfficialNavigationBarItem(
                skinIconPath = null,
                labelScrimAlpha = 0f
            )
        )
        assertFalse(
            shouldUseMiuixOfficialNavigationBarItem(
                skinIconPath = "/skin/home.png",
                labelScrimAlpha = 0f
            )
        )
        assertFalse(
            shouldUseMiuixOfficialNavigationBarItem(
                skinIconPath = null,
                labelScrimAlpha = 0.4f
            )
        )
    }

    @Test
    fun `docked miuix bottom item uses theme color when selected`() {
        val themeColor = Color(0xFFE85A91)
        val neutralColor = Color(0xFF9A9AA0)

        assertEquals(
            themeColor,
            resolveMiuixDockedBottomBarItemColor(
                selected = true,
                selectedColor = themeColor,
                unselectedColor = neutralColor
            )
        )
        assertEquals(
            neutralColor,
            resolveMiuixDockedBottomBarItemColor(
                selected = false,
                selectedColor = themeColor,
                unselectedColor = neutralColor
            )
        )
    }

    @Test
    fun `android native floating branch declares its own tuning entrypoint`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("resolveAndroidNativeBottomBarTuning("))
        assertTrue(source.contains("resolveAndroidNativeBottomBarContainerColor("))
        assertTrue(source.contains("BiliPaiFloatingBottomBar("))
        assertTrue(source.contains("iconStyle = sharedBarIconStyle"))
        assertTrue(source.contains("SharedFloatingBottomBarIconStyle.MATERIAL"))
    }

    @Test
    fun `android native floating branch uses BiliPai three layer backdrop structure`() {
        val floating = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"
        )

        assertTrue(floating.contains("val tabsBackdrop = rememberLayerBackdrop()"))
        assertTrue(floating.contains(".layerBackdrop(tabsBackdrop)"))
        assertTrue(floating.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(floating.contains("liquidGlassTuning.backdropBlurRadius.dp.toPx()"))
        assertTrue(floating.contains("padding = maxOf("))
        assertTrue(floating.contains("refractionHeight = shellRefractionHeightPx"))
        assertTrue(floating.contains("refractionAmount = shellRefractionAmountPx"))
        assertTrue(floating.contains("BottomBarReferencePressedScale"))
    }

    @Test
    fun `android native indicator backdrop matches BiliPai lens order`() {
        val floating = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"
        )

        assertTrue(
            Regex(
                """rememberCombinedBackdrop\(backdrop, tabsBackdrop\)[\s\S]*?drawBackdrop\([\s\S]*?effects = \{[\s\S]*?lens\(""",
                RegexOption.MULTILINE
            ).containsMatchIn(floating)
        )
        assertTrue(floating.contains("resolveLiquidGlassIndicatorChromaticAberration("))
        assertTrue(floating.contains("depthEffect = true"))
    }

    @Test
    fun `android native indicator follows BiliPai combined page plus tabs capture`() {
        val floating = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"
        )
        val host = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val renderer = host
            .substringAfter("private fun BiliPaiFloatingBottomBar(")
            .substringBefore("internal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(")

        assertTrue(floating.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(floating.contains(".layerBackdrop(tabsBackdrop)"))
        assertTrue(floating.contains("FloatingBottomBarIndicatorHeight: Dp = 56.dp"))
        assertTrue(renderer.contains("FloatingBottomBar("))
        assertTrue(renderer.contains("indicatorHeight = resolveBiliPaiBottomBarIndicatorHeight(dockHeight)"))
        assertTrue(
            floating.contains("BottomBarReferencePressedScale") ||
                host.contains("BOTTOM_BAR_INDICATOR_DRAG_SCALE_TARGET =")
        )
    }

    @Test
    fun `android native ordinary blur does not redraw raw backdrop over haze`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("if (backdrop != null && !useHazeBlur)"))
        assertTrue(source.contains("Modifier.unifiedBlur("))
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
