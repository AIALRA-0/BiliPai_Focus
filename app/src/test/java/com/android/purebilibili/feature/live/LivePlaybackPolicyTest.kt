package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.CodecInfo
import com.android.purebilibili.data.model.response.FormatInfo
import com.android.purebilibili.data.model.response.LivePlayUrlData
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.model.response.Playurl
import com.android.purebilibili.data.model.response.PlayurlInfo
import com.android.purebilibili.data.model.response.StreamInfo
import com.android.purebilibili.data.model.response.UrlInfo
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class LivePlaybackPolicyTest {

    @Test
    fun `resolve playback should prefer hls and keep server ordered hosts`() {
        val resolved = resolveLivePlayback(
            data = playbackData(
                streams = listOf(
                    stream(
                        protocol = "http_stream",
                        format = "flv",
                        codec = codec(
                            codecName = "avc",
                            baseUrl = "/live.flv",
                            urls = listOf(
                                "https://flv-primary.example.com/live.flv?token=1",
                                "https://flv-backup.example.com/live.flv?token=2"
                            )
                        )
                    ),
                    stream(
                        protocol = "http_hls",
                        format = "ts",
                        codec = codec(
                            codecName = "avc",
                            baseUrl = "/live.m3u8",
                            urls = listOf(
                                "https://hls-primary.example.com/live.m3u8?token=1",
                                "https://hls-backup.example.com/live.m3u8?token=2"
                            )
                        )
                    )
                )
            ),
            requestedQn = 10000
        )

        assertNotNull(resolved)
        assertEquals("https://hls-primary.example.com/live.m3u8?token=1", resolved.primaryUrl)
        assertEquals(
            listOf(
                "https://hls-primary.example.com/live.m3u8?token=1",
                "https://hls-backup.example.com/live.m3u8?token=2"
            ),
            resolved.candidates.first().urls
        )
    }

    @Test
    fun `resolve playback should use server current and accept qn before legacy descriptions`() {
        val resolved = resolveLivePlayback(
            data = playbackData(
                streams = listOf(
                    stream(
                        protocol = "http_hls",
                        format = "ts",
                        codec = codec(
                            codecName = "avc",
                            currentQn = 400,
                            acceptQn = listOf(10000, 400, 250),
                            baseUrl = "/live.m3u8",
                            urls = listOf("https://hls-primary.example.com/live.m3u8?token=1")
                        )
                    )
                ),
                gQnDesc = listOf(
                    LiveQuality(qn = 10000, desc = "Original"),
                    LiveQuality(qn = 400, desc = "BluRay"),
                    LiveQuality(qn = 250, desc = "Ultra HD")
                ),
                legacyQuality = listOf(
                    LiveQuality(qn = 150, desc = "HD"),
                    LiveQuality(qn = 80, desc = "Smooth")
                )
            ),
            requestedQn = 10000
        )

        assertNotNull(resolved)
        assertEquals(400, resolved.currentQuality)
        assertEquals(listOf(10000, 400, 250), resolved.qualityList.map { it.qn })
    }

    @Test
    fun `advance playback should move to next host before next candidate`() {
        val resolved = resolveLivePlayback(
            data = playbackData(
                streams = listOf(
                    stream(
                        protocol = "http_hls",
                        format = "ts",
                        codec = codec(
                            codecName = "avc",
                            baseUrl = "/hls.m3u8",
                            urls = listOf(
                                "https://hls-primary.example.com/hls.m3u8?token=1",
                                "https://hls-backup.example.com/hls.m3u8?token=2"
                            )
                        )
                    ),
                    stream(
                        protocol = "http_stream",
                        format = "flv",
                        codec = codec(
                            codecName = "avc",
                            baseUrl = "/stream.flv",
                            urls = listOf("https://flv-primary.example.com/stream.flv?token=1")
                        )
                    )
                )
            ),
            requestedQn = 10000
        )
        assertNotNull(resolved)

        val next = advanceLivePlayback(
            resolved = resolved,
            candidateIndex = 0,
            urlIndex = 0
        )

        assertIs<LiveAdvanceResult.NextSource>(next)
        assertEquals(0, next.candidateIndex)
        assertEquals(1, next.urlIndex)
        assertEquals("https://hls-backup.example.com/hls.m3u8?token=2", next.playUrl)
    }

    @Test
    fun `advance playback should request reload after all candidates are exhausted`() {
        val resolved = resolveLivePlayback(
            data = playbackData(
                streams = listOf(
                    stream(
                        protocol = "http_hls",
                        format = "ts",
                        codec = codec(
                            codecName = "avc",
                            baseUrl = "/hls.m3u8",
                            urls = listOf("https://hls-primary.example.com/hls.m3u8?token=1")
                        )
                    )
                )
            ),
            requestedQn = 10000
        )
        assertNotNull(resolved)

        val action = advanceLivePlayback(
            resolved = resolved,
            candidateIndex = 0,
            urlIndex = 0
        )

        assertIs<LiveAdvanceResult.ReloadCurrentQuality>(action)
        assertEquals(10000, action.qualityQn)
    }

    private fun playbackData(
        streams: List<StreamInfo>,
        gQnDesc: List<LiveQuality> = emptyList(),
        legacyQuality: List<LiveQuality> = emptyList()
    ): LivePlayUrlData {
        return LivePlayUrlData(
            quality_description = legacyQuality,
            current_quality = 0,
            playurl_info = PlayurlInfo(
                playurl = Playurl(
                    stream = streams,
                    gQnDesc = gQnDesc
                )
            )
        )
    }

    private fun stream(
        protocol: String,
        format: String,
        codec: CodecInfo
    ): StreamInfo {
        return StreamInfo(
            protocolName = protocol,
            format = listOf(
                FormatInfo(
                    formatName = format,
                    codec = listOf(codec)
                )
            )
        )
    }

    private fun codec(
        codecName: String,
        baseUrl: String,
        urls: List<String>,
        currentQn: Int = 0,
        acceptQn: List<Int> = emptyList()
    ): CodecInfo {
        return CodecInfo(
            codecName = codecName,
            currentQn = currentQn,
            acceptQn = acceptQn,
            baseUrl = baseUrl,
            url_info = urls.map { raw ->
                val uri = URI(raw)
                UrlInfo(
                    host = "${uri.scheme}://${uri.authority}",
                    extra = uri.rawQuery?.let { "?$it" }.orEmpty()
                )
            }
        )
    }
}
