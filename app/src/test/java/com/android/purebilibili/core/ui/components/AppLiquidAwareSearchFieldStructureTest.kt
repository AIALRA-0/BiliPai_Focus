package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppLiquidAwareSearchFieldStructureTest {
    @Test
    fun `liquid search matches shared dock geometry and keeps native fallback`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidAwareSearchField.kt"
        ).readText()

        assertTrue(source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(source.contains("shape = CircleShape"))
        assertTrue(source.contains("useNeutralLiquidContainer = true"))
        assertTrue(source.contains("drawShellLens = true"))
        assertTrue(source.contains("shellLensIntensity = resolveFloatingDockGeometryScale("))
        assertTrue(source.contains("BottomBarMatchedSegmentedControlHeightDp.dp"))
        assertTrue(source.contains("containerColor = if (liquidChromeActive)"))
        assertTrue(source.contains("heightOverride = if (liquidChromeActive)"))
        assertTrue(source.contains("leadingIconHorizontalOffset: Dp = 0.dp"))
        assertTrue(source.contains("leadingIconHorizontalOffset = leadingIconHorizontalOffset"))
    }

    @Test
    fun `comment search uses dock indicators and one shared popup surface`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSearchSheet.kt"
        ).readText()

        assertTrue(source.contains("AppLiquidAwareSearchField("))
        assertTrue(source.contains("leadingIconHorizontalOffset = 8.dp"))
        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("items = listOf(\"全部评论\", \"只看UP主\")"))
        assertTrue(source.contains("items = CommentSearchSortMode.entries.map"))
        assertTrue(source.contains("val stackControls = maxWidth < 420.dp"))
        assertTrue(source.contains("height = 48.dp"))
        assertTrue(!source.substringBefore("private fun CommentSearchResultRow(")
            .contains("biliPaiFloatingDockShell("))
    }

    @Test
    fun `popup renderer receives the page backdrop before dialog windows open`() {
        val navigation = File(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"
        ).readText()
        val renderer = File(
            "app/src/main/java/com/android/purebilibili/core/ui/components/" +
                "BiliPaiPopupSurfaceRenderer.kt"
        ).readText()

        assertTrue(navigation.contains("LocalAppPopupSurfaceRenderer provides"))
        assertTrue(navigation.contains("LocalFloatingChromeBackdrop provides"))
        assertTrue(navigation.contains("bottomBarBackdrop"))
        assertTrue(renderer.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(renderer.contains("backdrop = LocalFloatingChromeBackdrop.current"))
        assertTrue(renderer.contains("!isLowBlurBudgetForced()"))
    }
}
