package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageSendPayloadFactoryTest {

    @Test
    fun buildTextContent_escapesQuotesAndNewLines() {
        val payload = MessageSendPayloadFactory.buildTextContent(
            "hello \"bili\"\nworld"
        )

        assertEquals(
            "{\"content\":\"hello \\\"bili\\\"\\nworld\"}",
            payload
        )
    }

    @Test
    fun buildWithdrawContent_usesRawMessageKeyString() {
        val payload = MessageSendPayloadFactory.buildWithdrawContent(987654321L)

        assertEquals("987654321", payload)
    }

    @Test
    fun buildImageContent_serializesWhisperImagePayload() {
        val payload = MessageSendPayloadFactory.buildImageContent(
            imageUrl = "https://i0.hdslb.com/bfs/im/test.png",
            width = 1280,
            height = 720,
            imageType = "png",
            size = 456.5f
        )

        assertEquals(
            "{\"url\":\"https://i0.hdslb.com/bfs/im/test.png\",\"height\":720,\"width\":1280,\"imageType\":\"png\",\"original\":1,\"size\":456.5}",
            payload
        )
    }
}
