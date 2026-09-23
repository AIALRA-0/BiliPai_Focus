package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LiquidDockViewportStructureTest {

    @Test
    fun `liquid dock viewport keeps the horizontal shell rounded`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/components/LiquidDockViewport.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/components/LiquidDockViewport.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("val shape = if (liquidGlassEnabled)"))
        assertTrue(source.contains("CircleShape"))
        assertTrue(source.contains("return this.clip(shape)"))
    }

    @Test
    fun `scrolling is owned by the shared dock instead of the page row`() {
        val controlSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )
        val rendererSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        )

        assertTrue(controlSource.contains("scrollState: ScrollState? = null"))
        assertTrue(controlSource.contains("scrollState = scrollState"))
        assertTrue(rendererSource.contains("BottomBarMatchedLiquidDock("))
        assertTrue(rendererSource.contains(".horizontalScroll(scrollState)"))
        assertTrue(rendererSource.contains(".width(contentWidth)"))
        assertTrue(rendererSource.contains("drawShell = scrollState == null"))
        assertTrue(rendererSource.contains("allowLabelOverflow && scrollState == null"))
    }

    private fun loadSource(path: String): String {
        return listOf(File(path), File(path.removePrefix("app/")))
            .first { it.exists() }
            .readText()
    }
}
