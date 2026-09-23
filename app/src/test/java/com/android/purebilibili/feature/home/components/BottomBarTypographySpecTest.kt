package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarTypographySpecTest {
    @Test
    fun skinDockTypography_keepsItsCompactOverlayDensity() {
        assertEquals(12.sp, resolveBottomBarSkinDockLabelFontSize())
        assertEquals(18.sp, resolveBottomBarSkinDockLabelLineHeight())
    }

    @Test
    fun floatingDockTextOnlyLabelIsLargerThanIconAndTextCaption() {
        assertEquals(11.sp, resolveFloatingDockIconAndTextLabelFontSize())
        assertEquals(15.sp, resolveFloatingDockTextOnlyLabelFontSize())
        assertEquals(
            11.sp,
            resolveFloatingDockLabelFontSize(showIcon = true, showText = true),
        )
        assertEquals(
            15.sp,
            resolveFloatingDockLabelFontSize(showIcon = false, showText = true),
        )
        assertEquals(
            0.sp,
            resolveFloatingDockLabelFontSize(showIcon = true, showText = false),
        )
    }

    @Test
    fun liquidGlassTextOnlyDocksShareTheLargerLabelSize() {
        val bottomBar = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        )
        val topDock = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabFloatingDock.kt"
        )
        val topBar = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt"
        )
        val floatingVisual = bottomBar
            .substringAfter("private fun ColumnScope.FloatingBottomBarTabVisual(")
            .substringBefore("internal fun resolveMaterialBottomBarIcon(")
        assertTrue(floatingVisual.contains("resolveFloatingDockLabelFontSize("))
        assertTrue(topBar.contains("resolveFloatingDockLabelFontSize("))
        assertTrue(topDock.contains("fontSize = labelFontSize"))
        assertFalse(topDock.contains("labelSmall.fontSize"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).first { it.exists() }.readText()
    }
}
