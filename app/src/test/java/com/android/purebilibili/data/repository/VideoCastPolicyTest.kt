package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.Dolby
import com.android.purebilibili.data.model.response.Durl
import com.android.purebilibili.data.model.response.Flac
import com.android.purebilibili.data.model.response.PlayUrlData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoCastPolicyTest {

    @Test
    fun `buildTvCastPlayUrlParams includes projection fields and token`() {
        val params = buildTvCastPlayUrlParams(
            aid = 1234L,
            cid = 5678L,
            qn = 120,
            accessToken = "token-abc"
        )

        assertEquals("1234", params["object_id"])
        assertEquals("5678", params["cid"])
        assertEquals("1", params["is_proj"])
        assertEquals("1", params["playurl_type"])
        assertEquals("120", params["qn"])
        assertEquals("token-abc", params["access_key"])
        assertEquals("token-abc", params["mobile_access_key"])
        assertNotNull(params["ts"])
        assertEquals("appkey", params["actionKey"])
    }

    @Test
    fun `buildTvCastPlayUrlParams falls back to default quality and omits blank token`() {
        val params = buildTvCastPlayUrlParams(
            aid = 99L,
            cid = 88L,
            qn = 0,
            accessToken = "  "
        )

        assertEquals("80", params["qn"])
        assertFalse(params.containsKey("access_key"))
        assertFalse(params.containsKey("mobile_access_key"))
    }

    @Test
    fun `extractTvCastPlayableUrl prefers durl url then backup url`() {
        val durlPrimary = PlayUrlData(
            durl = listOf(Durl(url = "https://cdn.example.com/main.mp4"))
        )
        val durlBackup = PlayUrlData(
            durl = listOf(Durl(url = "", backupUrl = listOf("https://cdn.example.com/backup.mp4")))
        )

        assertEquals("https://cdn.example.com/main.mp4", extractTvCastPlayableUrl(durlPrimary))
        assertEquals("https://cdn.example.com/backup.mp4", extractTvCastPlayableUrl(durlBackup))
    }

    @Test
    fun `extractTvCastPlayableUrl returns null for DASH-only payload instead of leaking video-only URL`() {
        val dashOnly = PlayUrlData(
            durl = emptyList(),
            dash = Dash(
                video = listOf(DashVideo(baseUrl = "https://cdn.example.com/video.m4s")),
                audio = listOf(DashAudio(baseUrl = "https://cdn.example.com/audio.m4s"))
            )
        )

        assertNull(extractTvCastPlayableUrl(dashOnly))
    }

    @Test
    fun `extractTvCastPlayableUrl returns null when payload has no playable stream`() {
        val emptyPayload = PlayUrlData(durl = emptyList(), dash = Dash(video = emptyList()))

        assertNull(extractTvCastPlayableUrl(emptyPayload))
    }

    // --- Cast DASH video selection (legacy Chromecast compatibility) ---

    @Test
    fun `selectCastDashVideo prefers AVC over HEVC at target quality for Chromecast 2 compatibility`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-avc.m4s", codecs = "avc1.640028", width = 1920, height = 1080),
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-hevc.m4s", codecs = "hev1.2.4.L153", width = 1920, height = 1080)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals("avc1.640028", selected.codecs)
        assertEquals("https://cdn.example.com/video-avc.m4s", selected.baseUrl)
    }

    @Test
    fun `selectCastDashVideo falls back to HEVC when no AVC available at target quality`() {
        val videos = listOf(
            DashVideo(id = 112, baseUrl = "https://cdn.example.com/video-hevc.m4s", codecs = "hev1.2.4.L153", width = 1920, height = 1080),
            DashVideo(id = 112, baseUrl = "https://cdn.example.com/video-av1.m4s", codecs = "av01.0.13M.10", width = 1920, height = 1080)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 112))

        assertEquals("hev1.2.4.L153", selected.codecs)
    }

    @Test
    fun `selectCastDashVideo falls back to highest lower quality when target is missing`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-80.m4s", codecs = "avc1.640028", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "https://cdn.example.com/video-64.m4s", codecs = "avc1.64001E", width = 1280, height = 720),
            DashVideo(id = 32, baseUrl = "https://cdn.example.com/video-32.m4s", codecs = "avc1.64000D", width = 640, height = 360)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 116))

        assertEquals(80, selected.id)
    }

    @Test
    fun `selectCastDashVideo falls back to lowest higher quality when all qualities exceed target`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-80.m4s", codecs = "avc1.640028", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "https://cdn.example.com/video-64.m4s", codecs = "avc1.64001E", width = 1280, height = 720)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 32))

        assertEquals(64, selected.id)
    }

    @Test
    fun `selectCastDashVideo returns null when no valid video tracks exist`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "", codecs = "avc1.640028", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "   ", codecs = "avc1.64001E", width = 1280, height = 720)
        )

        assertNull(selectCastDashVideo(videos, targetQuality = 80))
    }

    @Test
    fun `selectCastDashVideo prefers lower-quality AVC over exact-quality HEVC for Chromecast compatibility`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-hevc.m4s", codecs = "hev1.2.4.L153", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "https://cdn.example.com/video-avc.m4s", codecs = "avc1.64001E", width = 1280, height = 720)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals("avc1.64001E", selected.codecs)
        assertEquals(64, selected.id)
        assertEquals("https://cdn.example.com/video-avc.m4s", selected.baseUrl)
    }

    @Test
    fun `selectCastDashVideo prefers lower-quality AVC over exact-quality AV1 for Chromecast compatibility`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-av1.m4s", codecs = "av01.0.13M.10", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "https://cdn.example.com/video-avc.m4s", codecs = "avc1.64001E", width = 1280, height = 720)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals("avc1.64001E", selected.codecs)
        assertEquals(64, selected.id)
    }

    @Test
    fun `selectCastDashVideo prefers lower-quality HEVC over exact-quality AV1 when no AVC available`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-av1.m4s", codecs = "av01.0.13M.10", width = 1920, height = 1080),
            DashVideo(id = 64, baseUrl = "https://cdn.example.com/video-hevc.m4s", codecs = "hev1.2.4.L153", width = 1280, height = 720)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals("hev1.2.4.L153", selected.codecs)
        assertEquals(64, selected.id)
    }

    @Test
    fun `selectCastDashVideo prefers lower bandwidth within same codec family and quality`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-high.m4s", codecs = "avc1.640028", width = 1920, height = 1080, bandwidth = 5000000),
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-low.m4s", codecs = "avc1.640028", width = 1920, height = 1080, bandwidth = 2000000)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals(2000000, selected.bandwidth)
        assertEquals("https://cdn.example.com/video-low.m4s", selected.baseUrl)
    }

    // --- Cast DASH audio selection (AAC/mp4a only, ignore dolby/flac) ---

    @Test
    fun `selectCastDashAudio prefers AAC mp4a from normal dash audio tracks`() {
        val audios = listOf(
            DashAudio(id = 30280, baseUrl = "https://cdn.example.com/audio-aac.m4s", codecs = "mp4a.40.2", bandwidth = 192000),
            DashAudio(id = 30300, baseUrl = "https://cdn.example.com/audio-ec3.m4s", codecs = "ec-3", bandwidth = 256000)
        )

        val selected = assertNotNull(selectCastDashAudio(audios, dolby = null, flac = null))

        assertEquals("mp4a.40.2", selected.codecs)
    }

    @Test
    fun `selectCastDashAudio ignores dolby and flac special tracks when normal audio is empty`() {
        val dolbyAudio = DashAudio(id = 30250, baseUrl = "https://cdn.example.com/dolby.m4s", codecs = "ec-3")
        val flacAudio = DashAudio(id = 30251, baseUrl = "https://cdn.example.com/flac.m4s", codecs = "flac")

        val selected = selectCastDashAudio(
            audios = emptyList(),
            dolby = Dolby(type = 1, audio = listOf(dolbyAudio)),
            flac = Flac(display = true, audio = flacAudio)
        )

        assertNull(selected)
    }

    @Test
    fun `selectCastDashVideo recognizes hvc1 as HEVC codec`() {
        val videos = listOf(
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-hvc1.m4s", codecs = "hvc1.2.4.L153", width = 1920, height = 1080),
            DashVideo(id = 80, baseUrl = "https://cdn.example.com/video-av1.m4s", codecs = "av01.0.13M.10", width = 1920, height = 1080)
        )

        val selected = assertNotNull(selectCastDashVideo(videos, targetQuality = 80))

        assertEquals("hvc1.2.4.L153", selected.codecs)
        assertEquals("https://cdn.example.com/video-hvc1.m4s", selected.baseUrl)
    }

    @Test
    fun `selectCastDashAudio excludes tracks with blank url`() {
        val audios = listOf(
            DashAudio(id = 30280, baseUrl = "", codecs = "mp4a.40.2", bandwidth = 192000),
            DashAudio(id = 30300, baseUrl = "https://cdn.example.com/audio-aac.m4s", codecs = "mp4a.40.2", bandwidth = 128000)
        )

        val selected = assertNotNull(selectCastDashAudio(audios, dolby = null, flac = null))

        assertEquals(30300, selected.id)
        assertEquals("https://cdn.example.com/audio-aac.m4s", selected.baseUrl)
    }

    @Test
    fun `selectCastDashAudio returns null when all AAC tracks have blank url`() {
        val audios = listOf(
            DashAudio(id = 30280, baseUrl = "", codecs = "mp4a.40.2", bandwidth = 192000),
            DashAudio(id = 30300, baseUrl = "   ", codecs = "mp4a.40.2", bandwidth = 128000)
        )

        val selected = selectCastDashAudio(audios, dolby = null, flac = null)

        assertNull(selected)
    }

    @Test
    fun `selectCastDashAudio returns null when only non-AAC codecs in normal audio`() {
        val audios = listOf(
            DashAudio(id = 30250, baseUrl = "https://cdn.example.com/audio.m4s", codecs = "ec-3")
        )

        val selected = selectCastDashAudio(audios, dolby = null, flac = null)

        assertNull(selected)
    }

    // --- Cast DASH manifest availability ---

    @Test
    fun `isCastDashManifestAvailable returns true when both playable video and AAC audio exist`() {
        val dash = Dash(
            video = listOf(DashVideo(id = 80, baseUrl = "https://cdn.example.com/video.m4s", codecs = "avc1.640028")),
            audio = listOf(DashAudio(baseUrl = "https://cdn.example.com/audio.m4s", codecs = "mp4a.40.2"))
        )

        assertTrue(isCastDashManifestAvailable(dash))
    }

    @Test
    fun `isCastDashManifestAvailable returns false when video is missing`() {
        val dash = Dash(
            video = emptyList(),
            audio = listOf(DashAudio(baseUrl = "https://cdn.example.com/audio.m4s", codecs = "mp4a.40.2"))
        )

        assertFalse(isCastDashManifestAvailable(dash))
    }

    @Test
    fun `isCastDashManifestAvailable returns false when audio is missing`() {
        val dash = Dash(
            video = listOf(DashVideo(id = 80, baseUrl = "https://cdn.example.com/video.m4s", codecs = "avc1.640028")),
            audio = emptyList()
        )

        assertFalse(isCastDashManifestAvailable(dash))
    }

    @Test
    fun `isCastDashManifestAvailable returns false when only dolby audio without AAC`() {
        val dash = Dash(
            video = listOf(DashVideo(id = 80, baseUrl = "https://cdn.example.com/video.m4s", codecs = "avc1.640028")),
            audio = emptyList(),
            dolby = Dolby(type = 1, audio = listOf(DashAudio(baseUrl = "https://cdn.example.com/dolby.m4s", codecs = "ec-3")))
        )

        assertFalse(isCastDashManifestAvailable(dash))
    }
}
