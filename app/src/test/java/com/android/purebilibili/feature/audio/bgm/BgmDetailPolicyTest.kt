package com.android.purebilibili.feature.audio.bgm

import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.navigation.ScreenRoutes
import com.android.purebilibili.navigation3.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.*

class BgmDetailPolicyTest {
    @Test
    fun detailDecodesMusicMetadataHeatAndCommentSubject() {
        val detail = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<BgmDetailData>("""
            {"music_title":"Ocean", "origin_artist_list":"Pat", "album":"Dance", "music_publish":"2014-01-01",
             "artists_list":[{"mid":123,"name":"Pat","identity":"演唱者"}],
             "wish_listen":true,"wish_count":6,"mv_bvid":"BV123","mv_cid":321,
             "hot_song_heat":{"last_heat":805000,"song_heat":[{"date":300,"heat":12},{"date":100,"heat":8}]},
             "music_comment":{"oid":9876,"nums":13,"page_type":47}}
        """.trimIndent())
        assertEquals("Dance", detail.album)
        assertEquals("Pat", detail.originArtistList)
        assertTrue(detail.wishListen)
        assertEquals(805000L, detail.hotSongHeat?.lastHeat)
        assertEquals(listOf(100L, 300L), resolveBgmHeatPoints(detail.hotSongHeat!!.songHeat).map { it.date })
        assertEquals(9876L, detail.musicComment?.oid)
        assertEquals(47, resolveBgmCommentType(detail.musicComment!!))
    }

    @Test
    fun trendUsesMostRecentThirtyUniqueValidDates() {
        val points = (1..40).reversed().map { BgmSongHeat(it.toLong(), it * 10L) } +
            listOf(BgmSongHeat(40, 400), BgmSongHeat(0, 0), BgmSongHeat(41, -1))
        val result = resolveBgmHeatPoints(points)
        assertEquals(30, result.size)
        assertEquals(11L, result.first().date)
        assertEquals(40L, result.last().date)
        assertEquals(47, resolveBgmCommentType(BgmCommentInfo(oid = 1, pageType = 0)))
        assertEquals(99, resolveBgmCommentType(BgmCommentInfo(oid = 1, pageType = 99)))
    }

    @Test
    fun flatAndSingleDayTrendsKeepNonzeroAxisRange() {
        for (points in listOf(emptyList(), listOf(BgmSongHeat(1, 0)), listOf(BgmSongHeat(1, 100), BgmSongHeat(2, 100)))) {
            val range = resolveBgmHeatRange(points)
            assertTrue(range.minimum >= 0)
            assertTrue(range.maximum > range.minimum)
        }
    }

    @Test
    fun musicRoutesPreserveAuPlaybackAndRouteMaToCopyrightDetail() {
        assertEquals(BiliPaiNavKey.MusicDetail(123), legacyRouteToBiliPaiNavKey(ScreenRoutes.createMusicRoute("au123")))
        assertEquals(BiliPaiNavKey.BgmDetail("MA123"), legacyRouteToBiliPaiNavKey(ScreenRoutes.createMusicRoute("MA123")))
        assertNull(ScreenRoutes.createMusicRoute("invalid"))
        assertNull(ScreenRoutes.createMusicRoute("au0"))
        val key = BiliPaiNavKey.BgmDetail("MA123", aid = 12, cid = 34, showVideos = true)
        assertEquals(key, legacyRouteToBiliPaiNavKey(key.toLegacyRoute()))
        assertEquals(BiliPaiNavEntryContentRole.BGM_DETAIL, resolveBiliPaiNavEntryContentRole(key))
    }
}
