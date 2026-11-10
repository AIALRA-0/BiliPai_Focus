package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DonationPreviewGeometryTest {
    @Test
    fun containerUsesExactEndpointsAndOneContinuousGeometryInBothOrientations() {
        for ((compact, preview) in listOf(
            IntSize(420, 760) to IntSize(500, 1000),
            IntSize(740, 320) to IntSize(900, 320),
            IntSize(280, 360) to IntSize(280, 360),
        )) {
            assertEquals(compact, donationPreviewContainerSize(compact, preview, 0f))
            assertEquals(preview, donationPreviewContainerSize(compact, preview, 1f))
            val sizes = (0..100).map { donationPreviewContainerSize(compact, preview, it / 100f) }
            assertTrue(sizes.zipWithNext().all { (a, b) -> b.width >= a.width && b.height >= a.height })
            assertTrue(sizes.all { it.width <= preview.width && it.height <= preview.height })
        }
    }

    @Test
    fun springOvershootNeverExceedsSafeWindow() {
        val compact = IntSize(420, 760)
        val preview = IntSize(500, 1000)
        assertEquals(preview, donationPreviewContainerSize(compact, preview, 1.03f))
        assertEquals(compact, donationPreviewContainerSize(compact, preview, -0.03f))
    }
}
