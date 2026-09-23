package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class DynamicTopBarThemePolicyTest {

    @Test
    fun `selected dynamic tab uses current theme color`() {
        val themeColor = Color(0xFFFF6F6F)

        assertEquals(themeColor, resolveDynamicTabSelectedColor(themeColor))
    }

    @Test
    fun `publish action consumes image and native color skin modes`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        ).readText()

        assertTrue(source.contains("publishSkinDecoration: DynamicPublishSkinDecoration? = null"))
        assertTrue(source.contains("publishIconPaths.pathFor(publishPressed)"))
        assertTrue(source.contains("Brush.verticalGradient("))
        assertTrue(source.contains("publishSkinDecoration?.iconTint"))
    }

    @Test
    fun `miuix non glass dynamic tabs can omit the inner track`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        ).readText()

        assertTrue(source.contains("if (liquidGlassEnabled)"))
        assertTrue(source.contains(".clip(RectangleShape)"))
        assertTrue(source.contains(".clip(dockShape)"))
        assertTrue(source.contains(".background(dockColor)"))
        assertTrue(source.contains("drawMiuixNonGlassTrack = liquidGlassEnabled"))
        assertTrue(source.contains("LocalAppUiStyle.current != AppUiStyle.MIUIX"))
    }

    @Test
    fun `layout mode menu uses the miuix window action menu`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        ).readText()

        assertTrue(source.contains("AppWindowActionMenu("))
        assertTrue(source.contains("selected = displayMode == mode"))
        assertTrue(source.contains("resolveDynamicDisplayModeLabel(mode)"))
        assertTrue(!source.contains("AppDropdownMenu("))
        assertTrue(!source.contains("AppDropdownMenuItem("))
    }

}
