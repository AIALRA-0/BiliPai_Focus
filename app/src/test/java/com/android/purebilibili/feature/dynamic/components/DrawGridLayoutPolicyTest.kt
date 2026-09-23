package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawGridLayoutPolicyTest {

    @Test
    fun longImageBadge_showsForTallImage() {
        assertTrue(shouldShowDrawGridLongImageBadge(width = 500, height = 1500))
    }

    @Test
    fun longImageBadge_hidesForNormalImage() {
        assertFalse(shouldShowDrawGridLongImageBadge(width = 500, height = 500))
        assertFalse(shouldShowDrawGridLongImageBadge(width = 500, height = 900))
    }

    @Test
    fun longImageBadge_hidesForInvalidOrTinyWidth() {
        assertFalse(shouldShowDrawGridLongImageBadge(width = 0, height = 1500))
        assertFalse(shouldShowDrawGridLongImageBadge(width = 50, height = 1500))
    }

    @Test
    fun resolveSingleImageAspectRatio_matchesBiliPaiLongImageClamp() {
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
    fun resolveDrawGridSpacingDp_matchesBiliPaiGridSpacing() {
        assertEquals(5, resolveDrawGridSpacingDp())
        assertEquals(10, resolveDrawGridCornerRadiusDp())
    }

    @Test
    fun resolveDrawGridDisplayCount_capsFeedPreviewAtNineImages() {
        assertEquals(
            DYNAMIC_FEED_PREVIEW_MAX_IMAGES,
            resolveDrawGridDisplayCount(
                totalImages = 14,
                maxDisplayImages = DYNAMIC_FEED_PREVIEW_MAX_IMAGES
            )
        )
    }

    @Test
    fun resolveDrawGridDisplayCount_keepsAllImagesInDetail() {
        assertEquals(14, resolveDrawGridDisplayCount(totalImages = 14, maxDisplayImages = null))
    }

    @Test
    fun resolveDrawGridColumnCount_usesThreeColumnsForNineImageCollage() {
        assertEquals(1, resolveDrawGridColumnCount(displayCount = 1))
        assertEquals(2, resolveDrawGridColumnCount(displayCount = 4))
        assertEquals(3, resolveDrawGridColumnCount(displayCount = 5))
        assertEquals(3, resolveDrawGridColumnCount(displayCount = 9))
    }

    @Test
    fun shouldDrawGridShowMoreBadge_onlyWhenPreviewIsCapped() {
        assertEquals(true, shouldDrawGridShowMoreBadge(index = 8, displayCount = 9, totalCount = 14))
        assertEquals(false, shouldDrawGridShowMoreBadge(index = 13, displayCount = 14, totalCount = 14))
    }
}
