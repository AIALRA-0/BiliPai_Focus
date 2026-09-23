package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.CodecInfo
import com.android.purebilibili.data.model.response.FormatInfo
import com.android.purebilibili.data.model.response.LiveDurl
import com.android.purebilibili.data.model.response.LivePlayUrlData
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.model.response.Playurl
import com.android.purebilibili.data.model.response.PlayurlInfo
import com.android.purebilibili.data.model.response.StreamInfo
import com.android.purebilibili.data.model.response.UrlInfo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePlaybackUrlPolicyTest {

    @Test
    fun `quality descriptions without stream urls are not playable`() {
        assertFalse(
            hasPlayableLiveUrl(
                LivePlayUrlData(
                    quality_description = listOf(LiveQuality(qn = 10000, desc = "原画"))
                )
            )
        )
    }

    @Test
    fun `xlive host and base url form a playable stream`() {
        assertTrue(
            hasPlayableLiveUrl(
                LivePlayUrlData(
                    playurl_info = PlayurlInfo(
                        playurl = Playurl(
                            stream = listOf(
                                StreamInfo(
                                    protocolName = "http_hls",
                                    format = listOf(
                                        FormatInfo(
                                            formatName = "fmp4",
                                            codec = listOf(
                                                CodecInfo(
                                                    baseUrl = "/live/index.m4s",
                                                    url_info = listOf(
                                                        UrlInfo(host = "https://example.com")
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `legacy blank urls are ignored`() {
        assertFalse(
            hasPlayableLiveUrl(
                LivePlayUrlData(durl = listOf(LiveDurl(url = "")))
            )
        )
        assertTrue(
            hasPlayableLiveUrl(
                LivePlayUrlData(durl = listOf(LiveDurl(url = "https://example.com/live.flv")))
            )
        )
    }
}
