package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoDetailReturnCoverImageTest {
    @Test
    fun changingFromPlayerToSixteenByTenCoverRecomputesCropWithoutStretching() {
        val source = Size(320f, 180f)
        for (direction in LayoutDirection.entries) {
            for (height in listOf(180f, 190f, 200f)) {
                val target = Size(320f, height)
                val result = resolveVideoDetailCoverDrawGeometry(source, target, ContentScale.Crop, direction)
                assertEquals(result.size.width / source.width, result.size.height / source.height, .0001f)
                assertTrue(result.size.width >= target.width && result.size.height >= target.height)
                assertEquals(0f, result.offset.y)
                assertTrue(result.offset.x <= 0f)
            }
        }
        val endpoint = resolveVideoDetailCoverDrawGeometry(
            source, Size(320f, 200f), ContentScale.Crop, LayoutDirection.Ltr,
        )
        assertEquals(-18f, endpoint.offset.x)
        assertEquals(200f, endpoint.size.height, .0001f)
    }

    @Test
    fun portraitCoverCropsVerticallyAndUnavailableIntrinsicSizeFillsTheSlot() {
        val target = Size(320f, 180f)
        val portrait = resolveVideoDetailCoverDrawGeometry(
            Size(180f, 320f), target, ContentScale.Crop, LayoutDirection.Ltr,
        )
        assertEquals(320f, portrait.size.width, .0001f)
        assertEquals(0f, portrait.offset.x)
        assertTrue(portrait.offset.y < 0f)
        for (intrinsic in listOf(Size.Unspecified, Size(0f, Float.POSITIVE_INFINITY))) {
            val fallback = resolveVideoDetailCoverDrawGeometry(intrinsic, target, ContentScale.Crop, LayoutDirection.Ltr)
            assertEquals(target, fallback.size)
            assertEquals(Offset.Zero, fallback.offset)
        }
    }
}
