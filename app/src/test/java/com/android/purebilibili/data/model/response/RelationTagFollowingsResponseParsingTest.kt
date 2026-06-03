package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RelationTagFollowingsResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `relation tag followings response maps full user list`() {
        val response = json.decodeFromString(
            RelationTagFollowingsResponse.serializer(),
            """
            {
              "code": 0,
              "message": "0",
              "ttl": 1,
              "data": [
                {
                  "mid": 420831218,
                  "uname": "支付宝Alipay",
                  "face": "https://i2.hdslb.com/bfs/face/a.jpg",
                  "sign": "关注点赞转发投币四连走起！",
                  "mtime": 1704067200,
                  "official_verify": {
                    "type": 1,
                    "desc": "支付宝官方账号"
                  },
                  "vip": {
                    "vipType": 1
                  },
                  "live": {
                    "live_status": 0
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val user = response.data.single()
        assertEquals(0, response.code)
        assertEquals(420831218L, user.mid)
        assertEquals("支付宝Alipay", user.uname)
        assertEquals("https://i2.hdslb.com/bfs/face/a.jpg", user.face)
        assertEquals("关注点赞转发投币四连走起！", user.sign)
        assertEquals(1704067200L, user.mtime)
        assertEquals(1, user.officialVerify.type)
        assertEquals("支付宝官方账号", user.officialVerify.desc)
    }
}
