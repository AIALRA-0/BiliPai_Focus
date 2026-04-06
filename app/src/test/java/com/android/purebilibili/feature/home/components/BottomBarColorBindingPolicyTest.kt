package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarColorBindingPolicyTest {

    @Test
    fun `resolves custom color by enum name`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = mapOf("DYNAMIC" to 6)
        )

        assertEquals(6, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `resolves custom color by lowercase route key`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = mapOf("dynamic" to 4)
        )

        assertEquals(4, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `resolves custom color by legacy chinese alias`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.WATCHLATER,
            itemColorIndices = mapOf("稍后再看" to 5)
        )

        assertEquals(5, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `falls back to default when no key matches`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = emptyMap()
        )

        assertEquals(0, binding.colorIndex)
        assertFalse(binding.hasCustomAccent)
    }

    @Test
    fun `light mode bottom bar always uses dark readable foreground`() {
        assertEquals(
            Color.Black,
            resolveBottomBarReadableContentColor(
                isLightMode = true,
                liquidGlassProgress = 0.08f,
                contentLuminance = 0.95f
            )
        )
    }

    @Test
    fun `bright frosted backdrop flips bottom bar content to dark foreground`() {
        val color = resolveBottomBarReadableContentColor(
            isLightMode = false,
            liquidGlassProgress = 0.92f,
            contentLuminance = 0.84f
        )

        assertTrue(color.red < 0.2f)
        assertTrue(color.alpha >= 0.8f)
    }

    @Test
    fun `clear dark glass keeps bright foreground for contrast`() {
        val color = resolveBottomBarReadableContentColor(
            isLightMode = false,
            liquidGlassProgress = 0.18f,
            contentLuminance = 0.22f
        )

        assertTrue(color.red > 0.9f)
        assertTrue(color.alpha >= 0.95f)
    }
}
