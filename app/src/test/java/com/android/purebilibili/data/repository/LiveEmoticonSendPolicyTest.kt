package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.LiveEmoticonRootResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveEmoticonSendPolicyTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun packages(): List<LiveEmoticonPackage> = json.decodeFromString(
        LiveEmoticonRootResponse.serializer(),
        """
        {
          "code": 0,
          "data": {
            "data": [
              {
                "pkg_id": 1,
                "pkg_name": "基础",
                "pkg_type": 3,
                "emoticons": [
                  {"emoji": "[鼓掌]", "url": "https://example.invalid/basic.png", "emoticon_unique": "emoji_1"}
                ]
              },
              {
                "pkg_id": 2,
                "pkg_name": "独立",
                "pkg_type": 1,
                "emoticons": [
                  {"emoji": "点赞", "url": "https://example.invalid/large.png", "emoticon_unique": "official_331"}
                ]
              }
            ]
          }
        }
        """.trimIndent()
    ).data!!.data!!.map { it.toLiveEmoticonPackage() }

    @Test
    fun `basic emoji is appended to existing draft and sends as ordinary reply text`() {
        val item = packages().first().items.single()
        val draft = appendLiveTextEmoticon("精彩", item, 40).getOrThrow()
        assertEquals("精彩[鼓掌]", draft)

        val request = buildLiveEmoticonSendRequest(
            roomId = 26863308,
            item = item,
            replyMid = 42,
            replayDmid = "dm-123"
        ).getOrThrow()
        assertEquals("[鼓掌]", request.message)
        assertNull(request.dmType)
        assertNull(request.emoticonOptions)
        assertEquals(42L, request.replyMid)
        assertEquals("dm-123", request.replayDmid)
        assertEquals(0, request.replyAttr)
        assertEquals("", request.replyUname)
    }

    @Test
    fun `large emoji sends unique id without reply but keeps readable echo`() {
        val item = packages().last().items.single()
        assertTrue(appendLiveTextEmoticon("精彩", item, 40).isFailure)

        val request = buildLiveEmoticonSendRequest(
            roomId = 26863308,
            item = item,
            replyMid = 42,
            replayDmid = "dm-123"
        ).getOrThrow()
        assertEquals("official_331", request.message)
        assertEquals(1, request.dmType)
        assertEquals("[object Object]", request.emoticonOptions)
        assertEquals(0L, request.replyMid)
        assertEquals("", request.replayDmid)
        assertEquals("点赞", item.displayText)
        assertEquals("https://example.invalid/large.png", item.url)
        assertFalse(item.displayText == request.message)
    }

    @Test
    fun `missing unique id is refused instead of sending readable emoji as id`() {
        val large = packages().last().items.single().copy(emoticonUnique = " ")
        val result = buildLiveEmoticonSendRequest(26863308, large)
        assertTrue(result.isFailure)
        assertEquals("该独立表情缺少发送标识，无法发送", result.exceptionOrNull()?.message)

        val basic = packages().first().items.single().copy(emoticonUnique = "")
        assertEquals("[鼓掌]", buildLiveEmoticonSendRequest(26863308, basic).getOrThrow().message)
    }

    @Test
    fun `text insertion at limit preserves whole emoji and rejects overflow`() {
        val basic = packages().first().items.single()
        assertEquals("精彩[鼓掌]", appendLiveTextEmoticon("精彩", basic, 6).getOrThrow())
        val overflow = appendLiveTextEmoticon("精彩", basic, 5)
        assertTrue(overflow.isFailure)
        assertEquals("插入表情后超过弹幕字数限制", overflow.exceptionOrNull()?.message)
        assertTrue(appendLiveTextEmoticon("精彩", basic.copy(emoji = ""), 40).isFailure)
    }

    @Test
    fun `image only large emoji remains sendable and never shows opaque id as label`() {
        val item = packages().last().items.single().copy(emoji = "", description = "加油")
        assertEquals("official_331", buildLiveEmoticonSendRequest(26863308, item).getOrThrow().message)
        assertEquals("加油", item.displayText)
        assertEquals("表情", item.copy(description = "").displayText)
    }

    @Test
    fun `root API errors expose message or msg without losing server explanation`() {
        for ((payload, expected) in listOf(
            """{"code":-101,"message":"账号未登录"}""" to "账号未登录",
            """{"code":-400,"msg":"参数错误"}""" to "参数错误",
            """{"code":-400,"message":"","msg":"表情不可用"}""" to "表情不可用",
            """{"code":-400,"message":"服务端原因","msg":"备用原因"}""" to "服务端原因"
        )) {
            val response = json.decodeFromString(LiveEmoticonRootResponse.serializer(), payload)
            assertEquals(expected, response.errorMessage)
        }
    }
}
