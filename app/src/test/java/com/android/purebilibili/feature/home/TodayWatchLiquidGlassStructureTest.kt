package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class TodayWatchLiquidGlassStructureTest {

    @Test
    fun `today watch mode control reuses compact bottom-bar liquid glass`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt"
        )
        val modeControl = source
            .substringAfter("private fun TodayWatchModeSegmentedControl(")
            .substringBefore("@OptIn(ExperimentalLayoutApi::class)")

        assertTrue(modeControl.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(modeControl.contains("AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp"))
        assertTrue(modeControl.contains("AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp"))
        assertTrue(modeControl.contains("tapPressRefractionEnabled = true"))
        assertTrue(modeControl.contains("liquidGlassEffectsEnabled = true"))
        assertTrue(modeControl.contains("miuixBackdrop = miuixBackdrop"))
    }

    @Test
    fun `today watch card samples queue content from a sibling backdrop`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt"
        )
        val card = source
            .substringAfter("private fun TodayWatchPlanCard(")
            .substringBefore("@Composable\nprivate fun WaterfallReveal(")

        assertTrue(card.contains("val todayWatchBackdrop = rememberLayerBackdrop()"))
        assertTrue(card.contains("miuixBackdrop = todayWatchBackdrop"))
        assertTrue(card.contains(".layerBackdrop(todayWatchBackdrop)"))
    }

    @Test
    fun `plugin settings mode control matches compact bottom-bar liquid glass`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/plugin/TodayWatchPlugin.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp"))
        assertTrue(source.contains("tapPressRefractionEnabled = true"))
        assertTrue(source.contains("miuixBackdrop = miuixBackdrop"))
        assertTrue(source.contains("val settingsBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains(".layerBackdrop(settingsBackdrop)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
