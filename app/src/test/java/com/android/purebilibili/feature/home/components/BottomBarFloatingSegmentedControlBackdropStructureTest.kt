package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BottomBarFloatingSegmentedControlBackdropStructureTest {

    @Test
    fun `appearance overrides reach the shared home dock renderer`() {
        val entry = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt")
        val wrapper = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt")
        val renderer = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt")
        for (parameter in listOf("tapPressRefractionEnabled", "indicatorIdleSurfaceColorOverride")) {
            assertTrue(entry.contains("$parameter = $parameter"))
            assertTrue(wrapper.contains("$parameter = $parameter"))
        }
        assertTrue(wrapper.contains("contentHorizontalPadding = horizontalPadding"))
        assertTrue(wrapper.contains("contentVerticalPadding = verticalPadding"))
        assertTrue(renderer.contains("resolveFloatingDockRefractionProgress("))
        assertTrue(renderer.contains("color = indicatorIdleSurfaceColorOverride ?:"))
        assertTrue(renderer.contains(".background(indicatorIdleSurfaceColorOverride ?:"))
        assertTrue(renderer.contains("horizontalPaddingLatest.value.toPx()"))
    }

    @Test
    fun `external backdrop stays singular and local sampling is fallback only`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        )

        assertTrue(source.contains("val localBackdrop = if (liquidGlassEnabled && miuixBackdrop == null)"))
        assertTrue(source.contains("miuixBackdrop ?: localBackdrop"))
        assertTrue(source.contains("effectiveBackdrop != null && miuixBackdrop == null"))
        assertTrue(!source.contains("rememberCombinedBackdrop(localBackdrop, miuixBackdrop)"))
        assertTrue(source.contains(".matchParentSize()"))
        assertTrue(source.contains(".bottomBarMatchedCaptureOverflow("))
        assertTrue(source.contains("horizontalInset = captureInsets.horizontalDp.dp"))
        assertTrue(source.contains("verticalInset = captureInsets.verticalDp.dp"))
        assertTrue(source.contains(".layerBackdrop(localBackdrop)"))
        assertTrue(source.contains(".background(AppSurfaceTokens.background())"))
        assertTrue(source.contains("backdrop = effectiveBackdrop"))
    }

    @Test
    fun `reused liquid dock applies caller sizing to its direct layout root`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        )

        assertTrue(source.contains("val viewportHeight = effectiveHeight + scaleOverflow * 2"))
        assertTrue(source.contains("modifier = rootModifier.height(viewportHeight)"))
        assertTrue(source.contains("val effectiveHeight = height.coerceAtLeast(0.dp)"))
        assertTrue(!source.contains("height.coerceAtLeast(48.dp)"))
        assertTrue(source.contains(".height(viewportHeight)"))
    }

    @Test
    fun `segmented content participates in the shared dispersion capture`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt")
        assertTrue(source.contains("if (isLiquidGlassMode) rememberChromeBackdropSource()"))
        assertTrue(source.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(source.contains("LocalFloatingBottomBarIndicatorStretchX provides indicatorStretchXProvider"))
        assertTrue(source.contains("resolveLiquidGlassIndicatorChromaticAberration("))
        assertTrue(source.contains(".height(capturedContentHeight)"))
        assertTrue(!source.contains("separateForeground"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
