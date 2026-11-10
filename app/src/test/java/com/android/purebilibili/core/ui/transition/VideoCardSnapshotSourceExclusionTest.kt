package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardSnapshotSourceExclusionTest {
    @Test
    fun sourceHoleUsesSnapshotCanvasCoordinatesOnSplitScreens() {
        assertEquals(
            Rect(20f, 300f, 440f, 800f),
            resolveVideoCardSnapshotSourceExclusionBounds(
                Rect(820f, 380f, 1240f, 880f), Offset(800f, 80f), Size(600f, 1600f),
            ),
        )
    }

    @Test
    fun exclusionClipsToCanvasAndNeverHidesUnrelatedPane() {
        assertEquals(Rect(0f, 0f, 200f, 300f),
            resolveVideoCardSnapshotSourceExclusionBounds(
                Rect(-20f, -80f, 200f, 300f), Offset.Zero, Size(600f, 1600f)))
        assertEquals(null, resolveVideoCardSnapshotSourceExclusionBounds(
            Rect(0f, 0f, 420f, 500f), Offset(800f, 80f), Size(600f, 1600f)))
        assertEquals(null, resolveVideoCardSnapshotSourceExclusionBounds(
            null, Offset.Zero, Size(600f, 1600f)))
    }

    @Test
    fun originalSlotStaysEmptyThroughSettleEvenAtZeroDepth() {
        for (exposure in listOf(VideoCardTransitionExposure.BackPreview,
            VideoCardTransitionExposure.Returning, VideoCardTransitionExposure.Restoring,
            VideoCardTransitionExposure.SettledHidden)) {
            assertTrue(shouldHideStationarySourceCard(
                isSharedMorphSourceCard = true,
                exposure = exposure,
                isCoveredLatch = true,
            ))
            assertFalse(shouldHideStationarySourceCard(
                isSharedMorphSourceCard = false,
                exposure = exposure,
                isCoveredLatch = true,
            ))
        }
        assertFalse(shouldHideStationarySourceCard(
            isSharedMorphSourceCard = true,
            exposure = VideoCardTransitionExposure.Idle,
            isCoveredLatch = true,
        ))
        // 未置锁的 Opening：overlay 还没覆盖，卡槽保持（避免点击瞬间空洞）。
        assertFalse(shouldHideStationarySourceCard(
            isSharedMorphSourceCard = true,
            exposure = VideoCardTransitionExposure.Opening,
            isCoveredLatch = false,
        ))
    }
}
