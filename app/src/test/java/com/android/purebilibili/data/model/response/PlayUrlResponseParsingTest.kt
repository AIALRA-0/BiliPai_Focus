package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlayUrlResponseParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `ugc playurl parsing keeps snake case dash fields playable`() {
        val payload = json.decodeFromString<PlayUrlResponse>(
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "quality": 80,
                "format": "hdflv2",
                "timelength": 123456,
                "accept_quality": [80, 64, 32],
                "accept_description": ["1080P", "720P", "480P"],
                "video_codecid": 7,
                "dash": {
                  "duration": 123,
                  "min_buffer_time": 1.5,
                  "video": [
                    {
                      "id": 80,
                      "base_url": "https://video.cdn/example-80.m4s",
                      "backup_url": ["https://backup.cdn/example-80.m4s"],
                      "bandwidth": 1200000,
                      "mime_type": "video/mp4",
                      "codecs": "avc1.640028",
                      "width": 1920,
                      "height": 1080,
                      "frame_rate": "16000/672",
                      "sar": "1:1",
                      "start_with_sap": 1,
                      "segment_base": {
                        "initialization": "0-100",
                        "index_range": "101-200"
                      },
                      "codecid": 7
                    }
                  ],
                  "audio": [
                    {
                      "id": 30280,
                      "base_url": "https://audio.cdn/example-30280.m4s",
                      "backup_url": [],
                      "bandwidth": 128000,
                      "mime_type": "audio/mp4",
                      "codecs": "mp4a.40.2"
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(0, payload.code)
        val data = assertNotNull(payload.data)
        assertEquals(80, data.quality)
        assertEquals(listOf(80, 64, 32), data.acceptQuality)
        val dash = assertNotNull(data.dash)
        assertEquals(1.5f, dash.minBufferTime)
        val video = assertNotNull(dash.video.firstOrNull())
        assertEquals("https://video.cdn/example-80.m4s", video.baseUrl)
        assertEquals(listOf("https://backup.cdn/example-80.m4s"), video.backupUrl)
        assertEquals("16000/672", video.frameRate)
        assertEquals(1, video.startWithSap)
        assertEquals("101-200", video.segmentBase?.indexRange)
        val audio = assertNotNull(dash.audio?.firstOrNull())
        assertEquals("https://audio.cdn/example-30280.m4s", audio.baseUrl)
    }
}
