package com.android.purebilibili.feature.video.handoff

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackHandoffStructureTest {
    @Test
    fun `handoff payload excludes playback credentials and urls`() {
        val source = locateSource()
        assertTrue(source.contains("KEY_BVID"))
        assertTrue(source.contains("KEY_CID"))
        assertTrue(source.contains("KEY_SEASON_ID"))
        assertTrue(source.contains("KEY_EP_ID"))
        assertFalse(source.contains("KEY_COOKIE"))
        assertFalse(source.contains("KEY_TOKEN"))
        assertFalse(source.contains("KEY_PLAYBACK_URL"))
    }

    @Test
    fun `handoff has app and https fallback paths`() {
        val source = locateSource()
        assertTrue(source.contains("setFallbackUri"))
        assertTrue(source.contains("https://www.bilibili.com/video/"))
        assertTrue(source.contains("https://www.bilibili.com/bangumi/play/ep"))
    }

    @Test
    fun `video handoff decoder accepts valid identifiers and normalizes position`() {
        val payload = PlaybackHandoffCodec.decodeV1(
            version = 1,
            kind = "video",
            bvid = "BV1xx411c7mD",
            cid = 123L,
            seasonId = 0L,
            epId = 0L,
            positionMs = -1L,
            startAudio = true
        ) as PlaybackHandoffPayload.V1.Video

        assertEquals(0L, payload.resumePositionMs)
        assertTrue(payload.startAudio)
    }

    @Test
    fun `handoff decoder rejects unknown versions and invalid ids`() {
        assertNull(
            PlaybackHandoffCodec.decodeV1(
                version = 2,
                kind = "video",
                bvid = "BV1xx411c7mD",
                cid = 123L,
                seasonId = 0L,
                epId = 0L,
                positionMs = 1L,
                startAudio = false
            )
        )
        assertNull(
            PlaybackHandoffCodec.decodeV1(
                version = 1,
                kind = "bangumi",
                bvid = null,
                cid = 0L,
                seasonId = 123L,
                epId = 0L,
                positionMs = 1L,
                startAudio = false
            )
        )
    }

    private fun locateSource(): String {
        val candidates = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/handoff/PlaybackHandoff.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/handoff/PlaybackHandoff.kt")
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Cannot locate PlaybackHandoff.kt from ${File(".").absolutePath}")
    }
}
