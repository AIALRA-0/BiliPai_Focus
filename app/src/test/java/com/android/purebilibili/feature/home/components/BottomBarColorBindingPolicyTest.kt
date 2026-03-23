package com.android.purebilibili.feature.home.components

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
}
