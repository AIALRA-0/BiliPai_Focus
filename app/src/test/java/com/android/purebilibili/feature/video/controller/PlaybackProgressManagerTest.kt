package com.android.purebilibili.feature.video.controller

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackProgressManagerTest {

    @Test
    fun `progress cache is isolated by cid for same bvid`() {
        val manager = PlaybackProgressManager()
        val bvid = "BV1TEST12345"

        manager.savePosition(bvid = bvid, cid = 1001L, positionMs = 600_000L)
        manager.savePosition(bvid = bvid, cid = 1002L, positionMs = 120_000L)

        assertEquals(600_000L, manager.getCachedPosition(bvid = bvid, cid = 1001L))
        assertEquals(120_000L, manager.getCachedPosition(bvid = bvid, cid = 1002L))
    }

    @Test
    fun `cid 0 falls back to bvid level progress key`() {
        val manager = PlaybackProgressManager()
        val bvid = "BV1TEST67890"

        manager.savePosition(bvid = bvid, cid = 0L, positionMs = 90_000L)

        assertEquals(90_000L, manager.getCachedPosition(bvid = bvid, cid = 0L))
    }

    @Test
    fun `cid lookup should not reuse bvid fallback from another page`() {
        val manager = PlaybackProgressManager()
        val bvid = "BV1MULTIPAGE"

        manager.savePosition(bvid = bvid, cid = 0L, positionMs = 600_000L)
        manager.savePosition(bvid = bvid, cid = 1001L, positionMs = 600_000L)

        assertEquals(0L, manager.getCachedPosition(bvid = bvid, cid = 1002L))
        assertEquals(600_000L, manager.getCachedPosition(bvid = bvid, cid = 0L))
    }
}
