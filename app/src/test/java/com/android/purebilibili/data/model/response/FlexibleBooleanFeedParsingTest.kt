package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlexibleBooleanFeedParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `dynamic feed accepts numeric boolean fields`() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "offset": "next",
                "has_more": 1,
                "update_baseline": "base",
                "update_num": 1,
                "items": [
                  {
                    "id_str": "dyn-1",
                    "type": "DYNAMIC_TYPE_AV",
                    "visible": 1,
                    "modules": {
                      "module_author": {
                        "mid": 1001,
                        "name": "UP",
                        "face": "",
                        "pub_ts": 1710000000,
                        "following": 1
                      },
                      "module_dynamic": {
                        "major": {
                          "type": "MAJOR_TYPE_ARCHIVE",
                          "archive": {
                            "aid": "1",
                            "bvid": "BV1xx411c7mD",
                            "title": "video",
                            "cover": "https://example.com/cover.jpg",
                            "duration_text": "01:00",
                            "stat": { "play": "1", "danmaku": "2" },
                            "is_charging_arc": 1,
                            "is_ugcpay": 0
                          }
                        }
                      },
                      "module_stat": {
                        "comment": { "count": 0, "forbidden": 0 },
                        "forward": { "count": 0, "forbidden": 0 },
                        "like": { "count": 1, "forbidden": 0 }
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicFeedResponse>(payload)
        val item = assertNotNull(response.data).items.single()

        assertTrue(response.data.has_more)
        assertTrue(item.visible)
        assertEquals(true, item.modules.module_author?.following)
        assertTrue(assertNotNull(item.modules.module_dynamic?.major?.archive).isChargingArc)
        assertFalse(assertNotNull(item.modules.module_dynamic?.major?.archive).isUgcpay)
        assertFalse(item.modules.module_stat?.like?.forbidden ?: true)
    }

    @Test
    fun `space dynamic feed accepts numeric boolean fields`() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "has_more": 1,
                "offset": "next",
                "items": [
                  {
                    "id_str": "space-dyn-1",
                    "type": "DYNAMIC_TYPE_AV",
                    "visible": 1,
                    "modules": {
                      "module_author": { "mid": 1001, "name": "UP", "face": "", "pub_ts": 1710000000 },
                      "module_dynamic": {
                        "major": {
                          "type": "MAJOR_TYPE_ARCHIVE",
                          "archive": {
                            "aid": "1",
                            "bvid": "BV1xx411c7mD",
                            "title": "video",
                            "cover": "https://example.com/cover.jpg",
                            "duration_text": "01:00",
                            "stat": { "play": "1", "danmaku": "2" },
                            "is_charging_arc": 1,
                            "is_ugcpay": 0
                          }
                        }
                      },
                      "module_stat": {
                        "comment": { "count": 0, "forbidden": 0, "hidden": 0, "status": 1 },
                        "forward": { "count": 0, "forbidden": 0, "hidden": 0, "status": 1 },
                        "like": { "count": 1, "forbidden": 0, "hidden": 0, "status": 1 }
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<SpaceDynamicResponse>(payload)
        val item = assertNotNull(response.data).items.single()

        assertTrue(response.data.has_more)
        assertTrue(item.visible)
        assertTrue(assertNotNull(item.modules.module_dynamic?.major?.archive).isChargingArc)
        assertFalse(assertNotNull(item.modules.module_dynamic?.major?.archive).isUgcpay)
        assertFalse(item.modules.module_stat?.like?.forbidden ?: true)
        assertFalse(item.modules.module_stat?.like?.hidden ?: true)
        assertTrue(item.modules.module_stat?.like?.status ?: false)
    }

    @Test
    fun `home adjacent responses accept numeric boolean fields`() {
        val nav = json.decodeFromString<NavResponse>(
            """{"code":0,"data":{"isLogin":1,"uname":"u","mid":1}}"""
        )
        val popular = json.decodeFromString<PopularResponse>(
            """{"code":0,"message":"0","data":{"list":[],"no_more":0}}"""
        )
        val favorite = json.decodeFromString<FavoriteResourceResponse>(
            """{"code":0,"message":"0","data":{"medias":[],"has_more":"1","ttl":1}}"""
        )

        assertTrue(assertNotNull(nav.data).isLogin)
        assertFalse(assertNotNull(popular.data).no_more)
        assertTrue(assertNotNull(favorite.data).has_more)
    }
}
