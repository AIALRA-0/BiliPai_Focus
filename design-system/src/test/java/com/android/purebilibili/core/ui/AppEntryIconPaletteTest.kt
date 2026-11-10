package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AppEntryIconPaletteTest {
    @Test
    fun paletteKeepsDistinctEntryColorsSoSiblingGroupsStayDistinguishable() {
        assertEquals(16, AppEntryIconPalette.size)
        assertEquals(AppEntryIconPalette.size, AppEntryIconPalette.distinct().size)
    }

    @Test
    fun cursorAssignsPaletteColorsInDeclarationOrderAndWrapsAround() {
        val cursor = AppEntryIconColorCursor()
        assertEquals(AppEntryIconPalette[0], cursor.nextColor())
        assertEquals(AppEntryIconPalette[1], cursor.nextColor())
        repeat(AppEntryIconPalette.size - 2) { cursor.nextColor() }
        assertEquals(AppEntryIconPalette[0], cursor.nextColor())
    }
}
