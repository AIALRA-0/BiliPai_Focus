package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileMemberAccountParsingTest {

    @Test
    fun decodeMemberAccountResponse_mapsEditableProfileFields() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "ttl": 1,
              "data": {
                "mid": 293793435,
                "uname": "测试用户",
                "userid": "bili_test",
                "sign": "这个人很神秘",
                "birthday": "2002-03-05",
                "sex": "男",
                "nick_free": false,
                "rank": "正式会员"
              }
            }
        """.trimIndent()

        val response = Json { ignoreUnknownKeys = true }.decodeFromString<MemberAccountResponse>(payload)

        assertEquals(0, response.code)
        assertEquals(293793435, response.data?.mid)
        assertEquals("测试用户", response.data?.uname)
        assertEquals("这个人很神秘", response.data?.sign)
        assertEquals("2002-03-05", response.data?.birthday)
        assertEquals("男", response.data?.sex)
    }
}
