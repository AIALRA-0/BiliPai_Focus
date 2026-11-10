package com.android.purebilibili.core.network.socket

import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.DeflaterOutputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuProtocolLimitsTest {

    @Test
    fun `empty and oversized websocket frames are rejected before copying`() {
        assertFalse(shouldAcceptLiveDanmakuFrame(0))
        assertFalse(shouldAcceptLiveDanmakuFrame(MAX_LIVE_DANMAKU_FRAME_BYTES + 1))
    }

    @Test
    fun `bounded websocket frame is accepted`() {
        assertTrue(shouldAcceptLiveDanmakuFrame(MAX_LIVE_DANMAKU_FRAME_BYTES))
    }

    @Test
    fun `plain concatenated packets round trip in order`() = runBlocking {
        val first = message("first", sequence = 7)
        val second = message("second", sequence = 8)
        assertEquals(listOf(first, second), DanmakuProtocol.decode(
            DanmakuProtocol.encode(first) + DanmakuProtocol.encode(second)
        ))
    }

    @Test
    fun `zlib nested packets and outer siblings retain order`() = runBlocking {
        val first = message("first")
        val second = message("second")
        val third = message("third")
        val compressed = compressed(DanmakuProtocol.encode(first) + DanmakuProtocol.encode(second))
        assertEquals(listOf(first, second, third), DanmakuProtocol.decode(
            compressed + DanmakuProtocol.encode(third)
        ))
    }

    @Test
    fun `brotli envelope decodes its actual wire payload`() = runBlocking {
        // Encoded from the complete inner packet using node:zlib brotliCompressSync.
        val compressedHex = "0b16800000002d0010000000000005000000017b22636d64223a2244414e4d555f4d5347222c22696e666f223a5b5d7d03"
        val body = compressedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val outer = DanmakuProtocol.Packet(3, 5, body = body)
        assertEquals(
            listOf(message("""{"cmd":"DANMU_MSG","info":[]}""")),
            DanmakuProtocol.decode(DanmakuProtocol.encode(outer))
        )
    }

    @Test
    fun `damaged compressed packet does not discard following valid packet`() = runBlocking {
        val damaged = DanmakuProtocol.encode(DanmakuProtocol.Packet(2, 5, body = byteArrayOf(1, 2, 3)))
        val valid = message("still alive")
        assertEquals(listOf(valid), DanmakuProtocol.decode(damaged + DanmakuProtocol.encode(valid)))
    }

    @Test
    fun `unknown version and bounded invalid header skip only their packet`() = runBlocking {
        val unknown = DanmakuProtocol.encode(DanmakuProtocol.Packet(65535, 5, body = byteArrayOf(1)))
        val invalid = DanmakuProtocol.encode(message("bad header"))
        ByteBuffer.wrap(invalid).order(ByteOrder.BIG_ENDIAN).putShort(4, 8.toShort())
        val valid = message("valid")
        assertEquals(listOf(valid), DanmakuProtocol.decode(
            unknown + invalid + DanmakuProtocol.encode(valid)
        ))
    }

    @Test
    fun `unsigned extended header length is accepted`() = runBlocking {
        val headerSize = 0x8000
        val body = "extended".toByteArray()
        val frame = ByteBuffer.allocate(headerSize + body.size).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(capacity())
            putShort(headerSize.toShort())
            putShort(0)
            putInt(5)
            putInt(1)
            position(headerSize)
            put(body)
        }.array()
        assertEquals(listOf(message("extended")), DanmakuProtocol.decode(frame))
    }

    @Test
    fun `truncated or impossible length never loses already parsed packets`() = runBlocking {
        val first = message("first")
        val truncated = DanmakuProtocol.encode(message("truncated")).dropLast(2).toByteArray()
        assertEquals(listOf(first), DanmakuProtocol.decode(DanmakuProtocol.encode(first) + truncated))
        assertEquals(emptyList(), DanmakuProtocol.decode(ByteArray(16)))
    }

    @Test
    fun `decompression and packet count retain existing bounds`() = runBlocking {
        val inflated = ByteArray(MAX_LIVE_DANMAKU_DECOMPRESSED_BYTES + 1)
        val valid = message("after oversized compression")
        assertEquals(listOf(valid), DanmakuProtocol.decode(compressed(inflated) + DanmakuProtocol.encode(valid)))
        val encoded = DanmakuProtocol.encode(message(""))
        val frames = ByteArray(encoded.size * (MAX_LIVE_DANMAKU_PACKETS_PER_FRAME + 1))
        for (offset in frames.indices step encoded.size) encoded.copyInto(frames, offset)
        assertEquals(MAX_LIVE_DANMAKU_PACKETS_PER_FRAME, DanmakuProtocol.decode(frames).size)
    }

    private fun message(text: String, sequence: Int = 1) = DanmakuProtocol.Packet(
        version = 0, operation = 5, sequence = sequence, body = text.toByteArray()
    )

    private fun compressed(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output).use { it.write(data) }
        return DanmakuProtocol.encode(DanmakuProtocol.Packet(2, 5, body = output.toByteArray()))
    }
}
