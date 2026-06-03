package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MentionSearchResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `mention search maps string uid and user metadata`() {
        val response = json.decodeFromString(
            MentionSearchResponse.serializer(),
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "groups": [
                  {
                    "group_name": "我的关注",
                    "group_type": 2,
                    "items": [
                      {
                        "face": "https://i0.hdslb.com/bfs/face/a.jpg",
                        "fans": 3613,
                        "name": "社会易姐QwQ",
                        "official_verify_type": -1,
                        "uid": "293793435"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val group = response.data?.groups.orEmpty().single()
        val user = group.items.single()
        assertEquals("我的关注", group.groupName)
        assertEquals(2, group.groupType)
        assertEquals(293793435L, user.uid)
        assertEquals("社会易姐QwQ", user.name)
        assertEquals("https://i0.hdslb.com/bfs/face/a.jpg", user.face)
        assertEquals(3613, user.fans)
        assertEquals(-1, user.officialVerifyType)
    }
}
