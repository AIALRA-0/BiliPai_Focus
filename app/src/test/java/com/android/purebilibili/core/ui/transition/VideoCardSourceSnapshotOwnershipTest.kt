package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardSourceSnapshotOwnershipTest {
    @Test
    fun rejectedNeighbourClickCannotSupplyPixelsToTheFirstTransition() {
        val first = VideoCardSourceChromeSnapshot(title = "第一个视频", ownerName = "UP A")
        val neighbour = VideoCardSourceChromeSnapshot(title = "第二个视频", ownerName = "UP B")

        assertTrue(ownsVideoCardSourceSnapshot(first, first))
        assertFalse(ownsVideoCardSourceSnapshot(first, neighbour))
        assertTrue(ownsVideoCardSourceSnapshot(neighbour, neighbour))
    }

    @Test
    fun repeatedClickWithIdenticalTextStillHasIndependentPixelOwnership() {
        val first = VideoCardSourceChromeSnapshot(title = "同一个视频", ownerName = "UP")
        val repeatedClick = first.copy()

        assertFalse(ownsVideoCardSourceSnapshot(first, repeatedClick))
    }

    @Test
    fun missingOrReleasedSnapshotCannotBorrowGlobalPixels() {
        val snapshot = VideoCardSourceChromeSnapshot(title = "视频", ownerName = "UP")

        assertFalse(ownsVideoCardSourceSnapshot(null, snapshot))
        assertFalse(ownsVideoCardSourceSnapshot(snapshot, null))
        assertFalse(ownsVideoCardSourceSnapshot(null, null))
    }
}
