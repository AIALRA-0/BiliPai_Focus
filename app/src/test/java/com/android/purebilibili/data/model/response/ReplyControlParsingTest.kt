package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReplyControlParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `reply item maps member official verify and support share`() {
        val item = json.decodeFromString(
            ReplyItem.serializer(),
            """
            {
              "rpid": 1001,
              "oid": 777,
              "mid": 420831218,
              "member": {
                "mid": "420831218",
                "uname": "支付宝Alipay",
                "official_verify": {
                  "type": 1,
                  "desc": "支付宝官方账号"
                }
              },
              "content": {
                "message": "评论内容"
              },
              "reply_control": {
                "location": "IP属地：上海",
                "support_share": false
              }
            }
            """.trimIndent()
        )

        assertEquals(1, item.member.officialVerify.type)
        assertEquals("支付宝官方账号", item.member.officialVerify.desc)
        assertFalse(item.replyControl?.supportShare ?: true)
    }
}
