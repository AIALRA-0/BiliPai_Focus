package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.Durl
import com.android.purebilibili.core.network.BANGUMI_PLAY_URL_PATH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BangumiPlaybackUrlPolicyTest {

    @Test
    fun `bangumi playurl path matches PiliPlus v2 endpoint`() {
        assertEquals("pgc/player/web/v2/playurl", BANGUMI_PLAY_URL_PATH)
    }

    @Test
    fun `bangumi playurl params keep PiliPlus parity fields`() {
        val params = com.android.purebilibili.data.repository.buildBangumiPlayUrlParams(
            epId = 1001L,
            cid = 2002L,
            qn = 80,
            bvid = "BV1TEST12345",
            seasonId = 3003L,
            tryLook = true
        )

        assertEquals("1001", params["ep_id"])
        assertEquals("2002", params["cid"])
        assertEquals("3003", params["season_id"])
        assertEquals("80", params["qn"])
        assertEquals("4048", params["fnval"])
        assertEquals("0", params["fnver"])
        assertEquals("1", params["fourk"])
        assertEquals("1", params["voice_balance"])
        assertEquals("pre-load", params["gaia_source"])
        assertEquals("true", params["isGaiaAvoided"])
        assertEquals("1315873", params["web_location"])
        assertEquals("1", params["try_look"])
        assertEquals("BV1TEST12345", params["bvid"])
    }

    @Test
    fun `should keep all playable durl segments in order`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "https://cdn-1/video-1.m4s"),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s"),
                Durl(order = 3, url = "https://cdn-1/video-3.m4s")
            )
        )

        assertEquals(
            listOf(
                "https://cdn-1/video-1.m4s",
                "https://cdn-1/video-2.m4s",
                "https://cdn-1/video-3.m4s"
            ),
            urls
        )
    }

    @Test
    fun `should fallback to backup url when primary url missing`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "", backupUrl = listOf("https://backup/video-1.m4s")),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s")
            )
        )

        assertEquals(
            listOf("https://backup/video-1.m4s", "https://cdn-1/video-2.m4s"),
            urls
        )
    }

    @Test
    fun `should ignore empty segments`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "", backupUrl = emptyList()),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s")
            )
        )

        assertEquals(listOf("https://cdn-1/video-2.m4s"), urls)
    }
}
