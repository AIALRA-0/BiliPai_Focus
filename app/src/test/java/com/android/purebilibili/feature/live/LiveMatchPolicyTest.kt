package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMatchPolicyTest {
    @Test
    fun `replays accept video identifiers and reject unrelated or malformed links`() {
        assertEquals("BV1n5na61Eev", liveMatchPlaybackBvid("https://www.bilibili.com/video/BV1n5na61Eev/?p=2"))
        assertEquals("BV1n5na61Eev", liveMatchPlaybackBvid("BV1n5na61Eev"))
        assertNull(liveMatchPlaybackBvid("https://live.bilibili.com/7734200"))
        assertNull(liveMatchPlaybackBvid("https://www.bilibili.com/video/BV1n5na61Eevx"))
        assertNull(liveMatchPlaybackBvid(""))
    }

    @Test
    fun `team images accept relative and protocol relative paths`() {
        assertEquals("https://i0.hdslb.com/bfs/team.png", liveMatchImageUrl("/bfs/team.png"))
        assertEquals("https://i1.hdslb.com/bfs/team.png", liveMatchImageUrl("//i1.hdslb.com/bfs/team.png"))
        assertEquals("https://i0.hdslb.com/bfs/team.png", liveMatchImageUrl("http://i0.hdslb.com/bfs/team.png"))
        assertEquals("", liveMatchImageUrl(""))
    }
}
