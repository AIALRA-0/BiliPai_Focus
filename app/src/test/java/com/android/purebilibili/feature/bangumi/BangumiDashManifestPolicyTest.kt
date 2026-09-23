package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.SegmentBase
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BangumiDashManifestPolicyTest {

    @Test
    fun `keeps pugv audio when audio segment base is omitted`() {
        val manifest = buildBangumiDashManifest(
            dash = Dash(duration = 584, minBufferTime = 1.5f),
            video = DashVideo(
                id = 80,
                baseUrl = "https://cdn.example/video.m4s",
                mimeType = "video/mp4",
                codecs = "avc1.640028",
                segmentBase = SegmentBase(
                    initialization = "0-999",
                    indexRange = "1000-1999"
                )
            ),
            videoUrl = "https://cdn.example/video.m4s",
            audio = DashAudio(
                id = 30280,
                baseUrl = "https://cdn.example/audio.m4s",
                mimeType = "audio/mp4",
                codecs = "mp4a.40.2"
            ),
            audioUrl = "https://cdn.example/audio.m4s",
            durationMs = 584_000L
        )

        val resolvedManifest = assertNotNull(manifest)
        assertTrue(resolvedManifest.contains("contentType=\"audio\""))
        assertTrue(resolvedManifest.contains("https://cdn.example/audio.m4s"))
        assertTrue(resolvedManifest.contains("indexRange=\"1000-1999\""))
    }

    @Test
    fun `builds a manifest from a video url even when segment base is absent`() {
        val manifest = buildBangumiDashManifest(
            dash = Dash(duration = 30),
            video = DashVideo(
                id = 64,
                baseUrl = "https://cdn.example/video.m4s",
                mimeType = "video/mp4",
                codecs = "avc1.64001f"
            ),
            videoUrl = "https://cdn.example/video.m4s",
            audio = null,
            audioUrl = null,
            durationMs = 30_000L
        )

        assertNotNull(manifest)
    }
}
