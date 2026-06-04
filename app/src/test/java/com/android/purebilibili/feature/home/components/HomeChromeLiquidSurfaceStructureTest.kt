package com.android.purebilibili.feature.home.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChromeLiquidSurfaceStructureTest {

    @Test
    fun `top header uses shared renderer while bottom bar uses ksu renderer only`() {
        val workspaceRoot = generateSequence(
            Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        ) { current ->
            current.parent
        }.first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt"
                )
            )
        }
        val componentsDir = workspaceRoot.resolve(
            "app/src/main/java/com/android/purebilibili/feature/home/components"
        )

        val topHeader = componentsDir.resolve("iOSHomeHeader.kt")
        val topTabChrome = componentsDir.resolve("HomeTopTabChrome.kt")
        val topBar = componentsDir.resolve("TopBar.kt")
        val bottomBar = componentsDir.resolve("BottomBar.kt")

        assertFalse(
            "home chrome should not keep the old shared renderer file after migrating the only real dependency",
            Files.exists(componentsDir.resolve("HomeChromeLiquidSurface.kt"))
        )
        val topHeaderSource = topHeader.readText()
        val topBarSource = topBar.readText()
        assertTrue(
            "top header should own the chrome surface renderer after removing the extra file",
            topHeaderSource.contains("private data class HomeTopChromeSurfaceStyle(") &&
                topHeaderSource.contains("private fun resolveHomeTopChromeBackdropSpec(")
        )
        assertFalse(
            "top header should not delegate to the removed app chrome renderer",
            topHeaderSource.contains(".appChromeLiquidSurface(") ||
                topHeaderSource.contains("AppChromeLiquidSurfaceStyle")
        )
        val topHeaderMatchedSurfaceCalls = Regex("""\.homeTopBottomBarMatchedSurface\(""")
            .findAll(topHeaderSource)
            .count()
        val topHeaderDisabledShellLensCalls = Regex("""drawShellLens\s*=\s*false""")
            .findAll(topHeaderSource)
            .count()
        assertTrue(
            "top header should use the same matched dock surface helper as the bottom bar",
            topHeaderMatchedSurfaceCalls > 0
        )
        assertTrue(
            "edge controls should still be able to disable the full-shell lens while search and top tab dock may use the bottom-bar shell lens",
            topHeaderDisabledShellLensCalls >= topHeaderMatchedSurfaceCalls - 1
        )
        assertTrue(
            "top tab row should only treat chrome as external when the outer surface is actually drawn",
            topHeaderSource.contains("hasOuterChromeSurface = drawTopTabDockChrome")
        )
        assertTrue(
            "home header should draw a bottom-bar matched dock around top tabs inside the unified top panel",
            topHeaderSource.contains("val topTabDockChromeRenderMode = if (") &&
                topHeaderSource.contains("unifiedLocalTabChromeRenderMode == HomeTopChromeRenderMode.PLAIN") &&
                topHeaderSource.contains("val useTopTabBottomBarMatchedDock =") &&
                topHeaderSource.contains("effectiveTabMaterialMode == TopTabMaterialMode.LIQUID_GLASS") &&
                topHeaderSource.contains("topTabDockChromeRenderMode == HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP") &&
                topHeaderSource.contains("val drawTopTabDockChrome = drawTopTabOuterChromeSurface || useTopTabBottomBarMatchedDock") &&
                topHeaderSource.contains("drawChromeSurface = drawTopTabDockChrome") &&
                topHeaderSource.contains("useBottomBarMatchedSurface = useTopTabBottomBarMatchedDock") &&
                topHeaderSource.contains("tabChromeRenderMode = if (useTopTabBottomBarMatchedDock)") &&
                topHeaderSource.contains("val bottomBarLiquidGlassPreset = homeSettings?.bottomBarLiquidGlassPreset") &&
                topHeaderSource.contains("liquidGlassPreset = bottomBarLiquidGlassPreset") &&
                topHeaderSource.contains("topTabDockChromeRenderMode") &&
                topHeaderSource.contains("tabShape = if (useUnifiedTopPanel)") &&
                topHeaderSource.contains("resolveSharedBottomBarCapsuleShape()") &&
                topTabChrome.readText().contains("useBottomBarMatchedSurface: Boolean = false") &&
                topTabChrome.readText().contains("liquidGlassPreset: BottomBarLiquidGlassPreset") &&
                topTabChrome.readText().contains(".homeTopBottomBarMatchedSurface(")
        )
        assertTrue(
            "home top avatar, search content and unread badge should live in extracted top-control components",
            componentsDir.resolve("HomeTopControls.kt").readText().contains("HomeTopAvatarContent(") &&
                componentsDir.resolve("HomeTopControls.kt").readText().contains("HomeTopSearchPillContent(") &&
                componentsDir.resolve("HomeTopControls.kt").readText().contains("HomeTopUnreadBadge(") &&
                topHeaderSource.contains("HomeTopAvatarContent(") &&
                topHeaderSource.contains("HomeTopSearchPillContent(") &&
                topHeaderSource.contains("HomeTopUnreadBadge(")
        )
        val searchLayerIndex = topHeaderSource.indexOf(".height(currentSearchHeight)")
        val tabsThenSearchIndex = topHeaderSource.indexOf("if (topLayoutOrder == HomeTopLayoutOrder.TABS_THEN_SEARCH)")
        val searchThenTabsIndex = topHeaderSource.indexOf("if (topLayoutOrder == HomeTopLayoutOrder.SEARCH_THEN_TABS)")
        assertTrue(
            "search-first mode should render top tabs after the search layer",
            searchLayerIndex in 0 until searchThenTabsIndex &&
                topHeaderSource.indexOf("topTabsContent()", startIndex = searchThenTabsIndex) > searchThenTabsIndex
        )
        assertTrue(
            "tabs-first mode should keep its explicit branch before the search layer",
            tabsThenSearchIndex in 0 until searchLayerIndex
        )
        assertTrue(
            "top tab row should use the lightweight native tab implementation",
            topBarSource.contains("LightweightHomeTopTabs(") &&
                topBarSource.contains("resolveTopTabClickAction(index, selectedIndex)")
        )
        assertFalse(
            "top tab row should not keep the old liquid dock renderer",
            topBarSource.contains("private fun TopTabDockSurface(") ||
                topBarSource.contains("private fun Md3CategoryTabRow(")
        )
        assertTrue(
            "top tab chrome should guard child indicator clip behind chrome surface gate",
            topTabChrome.readText().contains("if (drawChromeSurface) Modifier.clip(tabShape) else Modifier")
        )
        assertTrue(
            "top tab chrome should center the fixed-height tab row inside the taller shell",
            topTabChrome.readText().contains("contentAlignment = Alignment.Center")
        )
        assertFalse(
            "top tab dock should not switch sampling off during feed scroll",
            topBarSource.contains("shouldSampleTopTabDockBackdrop(")
        )
        assertTrue(
            "top tab row should continue to follow pager drag offset",
            topBarSource.contains("resolveTopTabIndicatorRenderPosition(") &&
                topBarSource.contains("pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction")
        )
        assertTrue(
            "MD3 top tab indicator should be a single moving layer tied to pager offset",
            topBarSource.contains("resolveMd3TopTabIndicatorTranslationPx(") &&
                topBarSource.contains("translationX = md3IndicatorTranslationXPx")
        )
        val lightweightTopTabItemSource = topBarSource
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)")
        assertFalse(
            "MD3 top tab item should not draw a second per-item underline",
            lightweightTopTabItemSource.contains(".align(Alignment.BottomCenter)")
        )
        assertTrue(
            "matched top dock helper should still use the KSU floating dock renderer for header controls",
            topBarSource.contains(".kernelSuFloatingDockSurface(") &&
                topBarSource.contains("liquidGlassPreset: BottomBarLiquidGlassPreset") &&
                topBarSource.contains("liquidGlassPreset = liquidGlassPreset")
        )
        assertTrue(
            "top tab indicator should reuse the bottom bar KSU indicator layer when chrome exists",
            topBarSource.contains("val shouldRenderTopTabLiquidGlassIndicator = shouldUseLiquidGlassIndicator") &&
                topBarSource.contains("!hasOuterChromeSurface") &&
                topBarSource.contains("if (shouldRenderTopTabLiquidGlassIndicator)") &&
                topBarSource.contains("val shouldUseMd3DockBackedCapsule =") &&
                topBarSource.contains("KernelSuBottomBarIndicatorLayer(") &&
                topBarSource.contains("val shouldPrimeTopTabLiquidGlassCapture =") &&
                topBarSource.contains("(isLiquidGlassEnabled || backdrop != null)") &&
                topBarSource.contains("val topTabContentBackdrop = rememberLayerBackdrop()") &&
                topBarSource.contains("rememberCombinedBackdrop(backdrop, topTabContentBackdrop)") &&
                topBarSource.contains("layerBackdrop(topTabContentBackdrop)") &&
                topBarSource.contains("shouldRenderBottomBarIndicatorBackdrop(") &&
                topBarSource.contains("allowIdleGlassEffect = false") &&
                topBarSource.contains("contentBackdrop = effectiveTopTabIndicatorContentBackdrop") &&
                topBarSource.contains("else if (!shouldUseMd3DockBackedCapsule)")
        )
        assertFalse(
            "top tab row should not keep the old bottom-bar local backdrop capture names",
            topBarSource.contains("backdrop = tabsBackdrop") ||
                topBarSource.contains(".layerBackdrop(tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)")
        )
        assertFalse(
            "top tab indicator should not keep its old custom indicator renderer",
            topBarSource.contains("BottomBarStyleIndicatorSurface(") ||
                topBarSource.contains("LiquidIndicator(") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)")
        )
        assertTrue(
            "KSU dock surface should use backdrop vibrancy, blur, and lens like the floating bottom bar",
            bottomBar.readText().contains("internal fun Modifier.kernelSuFloatingDockSurface(") &&
                bottomBar.readText().contains("vibrancy()") &&
                bottomBar.readText().contains("drawShellLens: Boolean = true") &&
                bottomBar.readText().contains("glassEnabled && drawShellLens") &&
                bottomBar.readText().contains("shellRefractionHeightDp") &&
                bottomBar.readText().contains("shellRefractionAmountDp") &&
                bottomBar.readText().contains("runtimeShaderEffect(") &&
                bottomBar.readText().contains("LIQUID_GLASS_SHADER_KEY")
        )
        assertFalse(
            "bottom bar should not keep the old appChromeLiquidSurface renderer",
            bottomBar.readText().contains(".appChromeLiquidSurface(")
        )
        assertFalse(
            "bottom bar should not keep the old floating dock surface style",
            bottomBar.readText().contains("resolveFloatingDockLiquidSurfaceStyle(")
        )
        assertFalse(
            "bottom bar should not keep the old LiquidIndicator renderer",
            bottomBar.readText().contains("LiquidIndicator(")
        )
        assertFalse(
            "bottom bar should not keep the old BottomBarContent renderer",
            bottomBar.readText().contains("BottomBarContent(")
        )
    }

    @Test
    fun `top tab indicator reuses bottom bar idle backdrop policy`() {
        assertFalse(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 0f,
                isTransitionRunning = false,
                isBottomBarInteractionActive = false,
                allowIdleGlassEffect = false,
                allowTransitionIndicatorPulse = false
            )
        )
    }
}
