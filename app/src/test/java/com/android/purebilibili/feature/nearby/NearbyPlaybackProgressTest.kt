package com.android.purebilibili.feature.nearby

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NearbyPlaybackProgressTest {
    private val video = NearbyVideo("BV1xx411c7mD", 123, 10_000, 2f, "视频", "")

    @Test fun preparationCanBufferOrAdvanceAtTheTransferredSpeed() {
        assertTrue(nearbyPlaybackPositionMatches(video, 10_000, 5_000))
        assertTrue(nearbyPlaybackPositionMatches(video, 20_000, 5_000))
        assertFalse(nearbyPlaybackPositionMatches(video, 50_000, 5_000))
        assertFalse(nearbyPlaybackPositionMatches(video, 1_000, 5_000))
    }

    @Test fun zeroProgressAndClockBoundsDoNotPermitNegativePositions() {
        assertTrue(nearbyPlaybackPositionMatches(video.copy(positionMs = 0), 0, 0))
        assertFalse(nearbyPlaybackPositionMatches(video.copy(positionMs = 0), -1, 0))
        assertTrue(nearbyPlaybackPositionMatches(video, 10_000, -1))
        assertFalse(nearbyPlaybackPositionMatches(video, 20_000, -1))
    }
}
