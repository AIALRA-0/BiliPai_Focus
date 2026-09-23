package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.ui.wallpaper.ProfileWallpaperTransform
import com.android.purebilibili.core.ui.wallpaper.applyGestureToProfileWallpaperTransform
import com.android.purebilibili.core.ui.wallpaper.sanitizeProfileWallpaperTransform
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileWallpaperTransformPolicyTest {

    @Test
    fun sanitizeProfileWallpaperTransform_clampsScaleAndOffsets() {
        val transform = sanitizeProfileWallpaperTransform(
            ProfileWallpaperTransform(scale = 9f, offsetX = -4f, offsetY = 3f)
        )

        assertEquals(3f, transform.scale)
        assertEquals(-1f, transform.offsetX)
        assertEquals(1f, transform.offsetY)
    }

    @Test
    fun applyGestureToProfileWallpaperTransform_mapsPanIntoNormalizedBias() {
        val transform = applyGestureToProfileWallpaperTransform(
            current = ProfileWallpaperTransform(),
            panX = 60f,
            panY = -90f,
            zoomChange = 1.4f,
            containerWidthPx = 300f,
            containerHeightPx = 600f
        )

        assertEquals(1.4f, transform.scale)
        assertEquals(0.4f, transform.offsetX)
        assertEquals(-0.3f, transform.offsetY)
    }

    @Test
    fun sanitizeProfileWallpaperTransform_recoversFromNonFiniteInput() {
        val transform = sanitizeProfileWallpaperTransform(
            ProfileWallpaperTransform(
                scale = Float.NaN,
                offsetX = Float.POSITIVE_INFINITY,
                offsetY = Float.NEGATIVE_INFINITY
            )
        )

        assertEquals(ProfileWallpaperTransform(), transform)
    }
}
