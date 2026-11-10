package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MessageHistoryParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun emptyHistoryAcceptsUnsignedSentinelFromServer() {
        val payload = """{"code":0,"msg":"OK","message":"OK","ttl":1,"data":{"messages":null,"has_more":0,"min_seqno":18446744073709551615,"max_seqno":0}}"""
        val data = requireNotNull(json.decodeFromString<MessageHistoryResponse>(payload).data)

        assertNull(data.messages)
        assertEquals(0L, data.min_seqno)
        assertEquals(0, data.has_more)
    }

    @Test
    fun validLargeCursorRetainsFullPrecision() {
        val data = json.decodeFromString<MessageHistoryData>("""{"min_seqno":9223372036854775807}""")
        assertEquals(Long.MAX_VALUE, data.min_seqno)
        assertEquals(data, json.decodeFromString<MessageHistoryData>(json.encodeToString(MessageHistoryData.serializer(), data)))
    }

    @Test
    fun quotedCursorAndMissingCursorAreSupported() {
        assertEquals(123L, json.decodeFromString<MessageHistoryData>("""{"min_seqno":"123"}""").min_seqno)
        assertEquals(0L, json.decodeFromString<MessageHistoryData>("{}").min_seqno)
    }

    @Test
    fun unknownOverflowIsRejectedInsteadOfTruncatingToWrongPage() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageHistoryData>("""{"min_seqno":9223372036854775808}""")
        }
    }
}
