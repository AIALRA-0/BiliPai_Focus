package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DrawGridLayoutPolicyTest {

    @Test
    fun resolveSingleImageAspectRatio_matchesPiliPlusLongImageClamp() {
        assertEquals(9f / 22f, resolveSingleImageAspectRatio(width = 900, height = 3000))
    }

    @Test
    fun resolveSingleImageWidthFraction_expandsWideImagesToFullWidth() {
        assertEquals(1f, resolveSingleImageWidthFraction(width = 1600, height = 800))
    }

    @Test
    fun resolveSingleImageWidthFraction_keepsSquareImagesAtTwoThirdsWidth() {
        assertEquals(2f / 3f, resolveSingleImageWidthFraction(width = 1200, height = 1200))
    }

    @Test
    fun resolveSingleImageWidthFraction_keepsTallImagesNarrower() {
        assertEquals(0.5f, resolveSingleImageWidthFraction(width = 800, height = 2200))
    }

    @Test
    fun resolveDrawGridScaleMode_usesFitForSingleImage() {
        assertEquals(DrawGridScaleMode.FIT, resolveDrawGridScaleMode(totalImages = 1))
    }

    @Test
    fun resolveDrawGridScaleMode_keepsCropForMultiImageGrid() {
        assertEquals(DrawGridScaleMode.CROP, resolveDrawGridScaleMode(totalImages = 4))
    }

    @Test
    fun resolveDrawGridSpacingDp_matchesPiliPlusGridSpacing() {
        assertEquals(5, resolveDrawGridSpacingDp())
        assertEquals(10, resolveDrawGridCornerRadiusDp())
    }
}
