package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceSearchSerializationPolicyTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `search up item accepts numeric fields encoded as strings`() {
        val response = json.decodeFromString<SearchUpResponse>(
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "page": "1",
                "numPages": "2",
                "numResults": "20",
                "result": [
                  {
                    "mid": "123456",
                    "uname": "UP",
                    "usign": "hello",
                    "upic": "//example.com/avatar.jpg",
                    "fans": "1024",
                    "videos": "88",
                    "level": "6",
                    "is_senior_member": "1"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = response.data?.result?.single()
        assertEquals(123456L, item?.mid)
        assertEquals(1024, item?.fans)
        assertEquals(88, item?.videos)
        assertEquals(6, item?.level)
        assertEquals(1, item?.is_senior_member)
    }

    @Test
    fun `space video response accepts numeric fields encoded as strings`() {
        val response = json.decodeFromString<SpaceVideoResponse>(
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "page": {
                  "pn": "1",
                  "ps": "30",
                  "count": "1"
                },
                "list": {
                  "vlist": [
                    {
                      "aid": "100",
                      "bvid": "BV1xx411c7mD",
                      "title": "demo",
                      "pic": "https://example.com/cover.jpg",
                      "description": "",
                      "play": "1200",
                      "comment": "77",
                      "length": "01:23",
                      "created": "1710000000",
                      "author": "UP",
                      "typeid": "17",
                      "typename": "动画"
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val page = response.data?.page
        val item = response.data?.list?.vlist?.single()
        assertEquals(1, page?.pn)
        assertEquals(30, page?.ps)
        assertEquals(1, page?.count)
        assertEquals(100L, item?.aid)
        assertEquals(1200, item?.play)
        assertEquals(77, item?.comment)
        assertEquals(1710000000L, item?.created)
        assertEquals(17, item?.typeid)
    }
}
