package com.android.purebilibili.feature.video.danmaku

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebMaskParserTest {
    @Test
    fun `index parser reads big endian chunk times and bounded offsets`() {
        val bytes = ByteArray(80)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        buffer.put("MASK".encodeToByteArray())
        buffer.putInt(1)
        buffer.putInt(0)
        buffer.putInt(2)
        buffer.putLong(0L)
        buffer.putLong(48L)
        buffer.putLong(10_000L)
        buffer.putLong(64L)

        val index = WebMaskParser.parseIndex(bytes)

        assertEquals(2, index.size)
        assertEquals(WebMaskChunkIndex(0L, 48, 64), index[0])
        assertEquals(WebMaskChunkIndex(10_000L, 64, 80), index[1])
    }

    @Test
    fun `index parser rejects malformed header and descending offsets`() {
        assertTrue(WebMaskParser.parseIndex(ByteArray(16)).isEmpty())

        val bytes = ByteArray(64)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        buffer.put("MASK".encodeToByteArray())
        buffer.putInt(1)
        buffer.putInt(0)
        buffer.putInt(2)
        buffer.putLong(0L)
        buffer.putLong(56L)
        buffer.putLong(10_000L)
        buffer.putLong(48L)
        assertTrue(WebMaskParser.parseIndex(bytes).isEmpty())
    }
}
