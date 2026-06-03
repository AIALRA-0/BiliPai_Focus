package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PlaybackHeartbeatPolicyTest {

    @Test
    fun `heartbeat fields use official web form shape`() {
        val fields = buildPlaybackHeartbeatFields(
            bvid = "BV1test",
            aid = 123L,
            cid = 456L,
            epid = 0L,
            sid = 0L,
            mid = 789L,
            playedTimeSec = 30L,
            realPlayedTimeSec = 25L,
            startTsSec = 1_700_000_000L,
            csrf = "csrf-token"
        )

        assertEquals("123", fields["aid"])
        assertEquals("BV1test", fields["bvid"])
        assertEquals("456", fields["cid"])
        assertEquals("789", fields["mid"])
        assertEquals("30", fields["played_time"])
        assertEquals("25", fields["real_played_time"])
        assertEquals("25", fields["realtime"])
        assertEquals("1700000000", fields["start_ts"])
        assertEquals("3", fields["type"])
        assertEquals("2", fields["dt"])
        assertEquals("0", fields["outer"])
        assertEquals("0", fields["play_type"])
        assertEquals("csrf-token", fields["csrf"])
        assertFalse(fields.containsKey("csrf_token"))
    }

    @Test
    fun `initial heartbeat resolves initial play type and omits unknown ids`() {
        val fields = buildPlaybackHeartbeatFields(
            bvid = "BV1test",
            aid = 0L,
            cid = 456L,
            epid = 0L,
            sid = 0L,
            mid = null,
            playedTimeSec = -1L,
            realPlayedTimeSec = -1L,
            startTsSec = -1L,
            csrf = ""
        )

        assertFalse(fields.containsKey("aid"))
        assertFalse(fields.containsKey("mid"))
        assertFalse(fields.containsKey("csrf"))
        assertEquals("0", fields["played_time"])
        assertEquals("0", fields["real_played_time"])
        assertEquals("0", fields["realtime"])
        assertEquals("0", fields["start_ts"])
        assertEquals("1", fields["play_type"])
    }

    @Test
    fun `bangumi heartbeat fields use pgc identifiers and video type`() {
        val fields = buildPlaybackHeartbeatFields(
            bvid = "BV1pgc",
            aid = 123L,
            cid = 456L,
            epid = 789L,
            sid = 987L,
            mid = 654L,
            playedTimeSec = 60L,
            realPlayedTimeSec = 60L,
            startTsSec = 1_700_000_000L,
            csrf = "csrf-token",
            videoType = 4,
            subType = 1
        )

        assertEquals("789", fields["epid"])
        assertEquals("987", fields["sid"])
        assertEquals("4", fields["type"])
        assertEquals("1", fields["sub_type"])
    }
}
