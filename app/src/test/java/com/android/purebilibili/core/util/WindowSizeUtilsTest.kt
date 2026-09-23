package com.android.purebilibili.core.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowSizeUtilsTest {

    @Test
    fun `material adaptive V2 size class maps all app breakpoints`() {
        val compact = androidx.window.core.layout.WindowSizeClass(599, 479)
        val medium = androidx.window.core.layout.WindowSizeClass(600, 480)
        val expanded = androidx.window.core.layout.WindowSizeClass(840, 900)
        val large = androidx.window.core.layout.WindowSizeClass(1200, 900)
        val extraLarge = androidx.window.core.layout.WindowSizeClass(1600, 900)

        assertEquals(WindowWidthSizeClass.Compact, resolveWindowWidthSizeClass(compact))
        assertEquals(WindowHeightSizeClass.Compact, resolveWindowHeightSizeClass(compact))
        assertEquals(WindowWidthSizeClass.Medium, resolveWindowWidthSizeClass(medium))
        assertEquals(WindowHeightSizeClass.Medium, resolveWindowHeightSizeClass(medium))
        assertEquals(WindowWidthSizeClass.Expanded, resolveWindowWidthSizeClass(expanded))
        assertEquals(WindowWidthSizeClass.Large, resolveWindowWidthSizeClass(large))
        assertEquals(WindowWidthSizeClass.ExtraLarge, resolveWindowWidthSizeClass(extraLarge))
        assertEquals(WindowHeightSizeClass.Expanded, resolveWindowHeightSizeClass(extraLarge))
    }

    @Test
    fun `current configuration width overrides a stale adaptive class after activity return`() {
        val staleLargeClass = androidx.window.core.layout.WindowSizeClass(1200, 900)
        val resolvedWidthClass = resolveWindowWidthSizeClass(staleLargeClass, 411.dp)
        val currentWindow = WindowSizeClass(
            widthSizeClass = resolvedWidthClass,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 411.dp,
            heightDp = 800.dp,
            deviceWidthSizeClass = WindowWidthSizeClass.Large,
        )

        assertEquals(WindowWidthSizeClass.Compact, resolvedWidthClass)
        assertFalse(currentWindow.isTablet)
        assertFalse(currentWindow.shouldUseSideNavigation)
        assertFalse(currentWindow.shouldUseExpandedNavigationRail)
        assertTrue(currentWindow.isTabletDevice)
    }

    @Test
    fun `current configuration width preserves app breakpoints when adaptive class is stale`() {
        assertEquals(
            WindowWidthSizeClass.Expanded,
            resolveWindowWidthSizeClass(
                adaptiveSizeClass = androidx.window.core.layout.WindowSizeClass(411, 800),
                currentWindowWidthDp = 840.dp,
            ),
        )
        assertEquals(
            WindowWidthSizeClass.Large,
            resolveWindowWidthSizeClass(
                adaptiveSizeClass = androidx.window.core.layout.WindowSizeClass(411, 800),
                currentWindowWidthDp = 1200.dp,
            ),
        )
    }

    @Test
    fun `responsive text scaling keeps unspecified units`() {
        val scaled = TextUnit.Unspecified.scaledIfSpecified(1.2f)

        assertTrue(scaled.isUnspecified)
    }

    @Test
    fun `responsive text scaling scales specified units`() {
        val scaled = 14.sp.scaledIfSpecified(1.2f)

        assertEquals(16.8f, scaled.value, 0.0001f)
    }

    @Test
    fun `stable device width ignores UI density multiplier`() {
        val scaledWindowWidth = (600 / 1.08f).dp

        assertEquals(
            WindowWidthSizeClass.Compact,
            resolveWindowWidthSizeClass(scaledWindowWidth)
        )
        assertEquals(
            WindowWidthSizeClass.Medium,
            resolveStableDeviceWidthSizeClass(600)
        )
    }

    @Test
    fun `window size class separates current window from device shape`() {
        val windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 555.dp,
            heightDp = 800.dp,
            deviceWidthSizeClass = WindowWidthSizeClass.Medium
        )

        assertFalse(windowSizeClass.isTablet)
        assertFalse(windowSizeClass.isCompactDevice)
        assertTrue(windowSizeClass.isTabletDevice)
    }

    @Test
    fun `window size class cache tracks adaptive classes after rotation`() {
        val source = locateSource().readText()
        val calculation = source.substringAfter("fun calculateWindowSizeClass(")
            .substringBefore("fun <T> rememberResponsiveValue(")

        assertTrue(
            calculation.contains(
                "remember(widthDp, heightDp, widthSizeClass, heightSizeClass, deviceWidthSizeClass)"
            ),
            "Adaptive size classes can update after LocalConfiguration; both must invalidate the cache",
        )
        assertTrue(
            calculation.contains("resolveWindowWidthSizeClass(adaptiveWindowSizeClass, widthDp)"),
            "When the adaptive class is stale across an Activity return, the current window width must win",
        )
    }

    @Test
    fun `narrow current window on tablet device is recognized as foldable cover screen`() {
        val cover = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Medium,
            heightSizeClass = WindowHeightSizeClass.Compact,
            widthDp = 672.dp,
            heightDp = 459.dp,
            deviceWidthSizeClass = WindowWidthSizeClass.Large,
        )

        assertTrue(cover.isFoldableCoverScreen)
    }

    @Test
    fun `tablet-sized current window is not recognized as foldable cover screen`() {
        val inner = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium,
            widthDp = 940.dp,
            heightDp = 665.dp,
            deviceWidthSizeClass = WindowWidthSizeClass.Large,
        )

        assertFalse(inner.isFoldableCoverScreen)
    }

    @Test
    fun `responsive content claims parent width before centering constrained child`() {
        val source = locateSource().readText()
        val functionBody = source
            .substringAfter("fun Modifier.responsiveContentWidth(")
            .substringBefore("fun Modifier.centeredContent(")

        val fillIndex = functionBody.indexOf(".fillMaxWidth()")
        val wrapIndex = functionBody.indexOf(".wrapContentWidth(alignment)")
        val limitIndex = functionBody.indexOf(".widthIn(max = maxWidth)")
        val innerFillIndex = functionBody.indexOf(".fillMaxWidth()", limitIndex)

        assertTrue(fillIndex >= 0)
        assertTrue(fillIndex < wrapIndex, "外层必须先占满父容器，居中才有剩余空间")
        assertTrue(wrapIndex < limitIndex, "限宽必须作用于被居中的内层内容")
        assertTrue(
            innerFillIndex > limitIndex,
            "限宽之后必须再 fillMaxWidth，否则 wrapContentWidth 会把 minWidth 放成 0，" +
                "窄屏上原本铺满父宽的内容会退化成按内容裁剪",
        )
        assertFalse(functionBody.contains("LocalWindowSizeClass.current"))
    }

    private fun locateSource(): File = listOf(
        File("src/main/java/com/android/purebilibili/core/util/WindowSizeUtils.kt"),
        File("app/src/main/java/com/android/purebilibili/core/util/WindowSizeUtils.kt"),
    ).firstOrNull(File::exists) ?: error("Cannot locate WindowSizeUtils.kt")
}
