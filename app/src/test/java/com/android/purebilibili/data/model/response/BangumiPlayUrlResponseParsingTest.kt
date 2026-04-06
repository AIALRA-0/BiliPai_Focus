package com.android.purebilibili.data.model.response

import com.android.purebilibili.data.repository.decodeBangumiPlayUrlPayload
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BangumiPlayUrlResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodeBangumiPlayUrlPayload parses v2 video_info envelope`() {
        val payload = decodeBangumiPlayUrlPayload(
            """
            {
              "code": 0,
              "message": "0",
              "result": {
                "video_info": {
                  "quality": 80,
                  "format": "hdflv2",
                  "timelength": 123456,
                  "accept_quality": [80, 64, 32],
                  "accept_description": ["1080P", "720P", "480P"],
                  "dash": {
                    "duration": 123,
                    "video": [
                      {
                        "id": 80,
                        "base_url": "https://video.cdn/example-80.m4s",
                        "backup_url": []
                      }
                    ],
                    "audio": []
                  }
                }
              }
            }
            """.trimIndent(),
            json
        )

        assertEquals(0, payload.code)
        assertEquals("0", payload.message)
        val videoInfo = assertNotNull(payload.videoInfo)
        assertEquals(80, videoInfo.quality)
        assertEquals(listOf(80, 64, 32), videoInfo.acceptQuality)
        assertEquals(80, videoInfo.dash?.video?.firstOrNull()?.id)
    }

    @Test
    fun `decodeBangumiPlayUrlPayload still supports legacy direct result envelope`() {
        val payload = decodeBangumiPlayUrlPayload(
            """
            {
              "code": 0,
              "message": "0",
              "result": {
                "quality": 64,
                "format": "flv720",
                "timelength": 45678,
                "durl": [
                  {
                    "order": 1,
                    "url": "https://video.cdn/example-64.mp4"
                  }
                ]
              }
            }
            """.trimIndent(),
            json
        )

        assertEquals(0, payload.code)
        val videoInfo = assertNotNull(payload.videoInfo)
        assertEquals(64, videoInfo.quality)
        assertEquals("https://video.cdn/example-64.mp4", videoInfo.durl?.firstOrNull()?.url)
    }
}
