package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomSelectionContentColorTest {
    @Test
    fun `classic blue stays chromatic when container requires a contrast adjustment`() {
        for (surface in listOf(Color(0xFF0C0D10), Color.White)) {
            val container = opaqueCompositeOver(iOSSystemBlue.copy(alpha = 0.14f), surface)
            val content = resolveCustomSelectionContentColor(iOSSystemBlue, container)
            assertTrue(calculateContrastRatio(content, container) >= ACCESSIBLE_TEXT_MIN_CONTRAST)
            assertNotEquals(Color.White, content)
            assertNotEquals(Color.Black, content)
            assertTrue(content.blue > content.red)
        }
    }

    @Test
    fun `readable picked accent is preserved exactly`() {
        val accent = Color(0xFF80BBFF)
        assertEquals(accent, resolveCustomSelectionContentColor(accent, Color.Black))
    }
}
