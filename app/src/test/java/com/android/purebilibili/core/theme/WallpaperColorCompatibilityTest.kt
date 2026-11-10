package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WallpaperColorCompatibilityTest {
    @Test
    fun `red wallpaper and blue system palette use compatibility only for wallpaper source`() {
        val red = 0xFFF04444.toInt()
        val blue = 0xFF6476A5.toInt()
        assertTrue(shouldUseWallpaperColorFallback(red, blue, "home_wallpaper"))
        assertFalse(shouldUseWallpaperColorFallback(red, blue, "preset"))
        assertFalse(shouldUseWallpaperColorFallback(red, blue, null))
        assertFalse(shouldUseWallpaperColorFallback(red, blue, "lock_wallpaper"))
    }

    @Test
    fun `related hues and neutral wallpapers preserve system palette`() {
        assertFalse(shouldUseWallpaperColorFallback(0xFFFF4444.toInt(), 0xFF995555.toInt(), "home_wallpaper"))
        assertFalse(shouldUseWallpaperColorFallback(0xFF888888.toInt(), 0xFF6476A5.toInt(), "home_wallpaper"))
        assertFalse(shouldUseWallpaperColorFallback(0xFFFF4444.toInt(), 0xFF888888.toInt(), "home_wallpaper"))
    }

    @Test
    fun `manual seed expires on wallpaper change and cannot bind to unknown wallpaper`() {
        assertTrue(isManualWallpaperSeedCurrent(4, 4))
        assertFalse(isManualWallpaperSeedCurrent(5, 4))
        assertFalse(isManualWallpaperSeedCurrent(-1, -1))
        assertFalse(isManualWallpaperSeedCurrent(4, null))
    }
}
