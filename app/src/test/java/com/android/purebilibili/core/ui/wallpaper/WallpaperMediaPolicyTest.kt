package com.android.purebilibili.core.ui.wallpaper

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WallpaperMediaPolicyTest {
    @Test
    fun recognizesImportedVideoAndCaseInsensitivePaths() {
        assertTrue(isVideoWallpaper("file:///wallpaper/clip.MP4"))
        assertTrue(isVideoWallpaper("file:///wallpaper/clip.video"))
        assertTrue(isVideoWallpaper("https://example.com/clip.webm?token=abc#preview"))
    }

    @Test
    fun keepsGifAndStillImagesOnImageDecoderPath() {
        assertFalse(isVideoWallpaper("file:///wallpaper/animated.gif"))
        assertFalse(isVideoWallpaper("file:///wallpaper/original.img"))
        assertFalse(isVideoWallpaper("file:///wallpaper/photo.jpg"))
    }
}
