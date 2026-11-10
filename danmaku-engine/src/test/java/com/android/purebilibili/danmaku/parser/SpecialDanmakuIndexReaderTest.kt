package com.android.purebilibili.danmaku.parser

import java.io.EOFException
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecialDanmakuIndexReaderTest {
    @Test
    fun originalEnvelopesParseIndependentlyAndPreserveUnsortedOrder() = runSuspend {
        val prefix = field(10, byteArrayOf(1, 2, 3))
        val first = field(1, field(7, "first".encodeToByteArray()) + scalar(3, 9) + scalar(2, 9000))
        val second = field(1, scalar(2, 100) + field(7, "second".encodeToByteArray()))
        val third = field(1, field(7, "defaults".encodeToByteArray()))
        val source = prefix + first + scalar(2, 1) + second + third
        val reader = reader(source)

        val entries = listOf(assertNotNull(reader.next()), assertNotNull(reader.next()), assertNotNull(reader.next()))
        assertEquals(listOf(9000L, 100L, 0L), entries.map { it.startTimeMs })
        assertEquals(listOf(9, 1, 1), entries.map { it.mode })
        assertEquals(listOf(prefix.size.toLong(), (prefix.size + first.size + scalar(2, 1).size).toLong(),
            (source.size - third.size).toLong()), entries.map { it.offset })
        assertEquals(listOf(first.size, second.size, third.size), entries.map { it.byteLength })
        entries.forEachIndexed { index, entry ->
            val envelope = source.copyOfRange(entry.offset.toInt(), entry.offset.toInt() + entry.byteLength)
            val decoded = DanmakuProto.parse(envelope).single()
            assertEquals(entry.startTimeMs, decoded.progress.toLong())
            assertEquals(entry.mode, decoded.mode)
            assertEquals(listOf("first", "second", "defaults")[index], decoded.content)
        }
        assertNull(reader.next())
        assertNull(reader.next())
    }

    @Test
    fun largeContentBeforeMetadataIsSkippedWithBoundedActualReads() = runSuspend {
        val contentLength = 64L * 1024 * 1024
        val contentHeader = varint((7 shl 3 or 2).toLong()) + varint(contentLength)
        val tail = scalar(3, 9) + scalar(2, 123456)
        val elementLength = contentHeader.size + contentLength + tail.size
        val header = varint(10) + varint(elementLength) + contentHeader
        val tailOffset = header.size + contentLength
        val source = SparseSource(tailOffset + tail.size, listOf(0L to header, tailOffset to tail))
        val reader = SpecialDanmakuIndexReader(source.length, source::read)

        val entry = assertNotNull(reader.next())
        assertEquals(SpecialDanmakuEntry(0, source.length.toInt(), 123456, 9), entry)
        assertNull(reader.next())
        assertTrue(source.downloadedBytes <= 2048, "Downloaded ${source.downloadedBytes} bytes")
        assertEquals(2, source.requests.size)
        assertTrue(source.requests.all { it.second in 1..1024 })
        assertEquals(tailOffset, source.requests.last().first)
    }

    @Test
    fun unknownTopLevelPayloadCanExceedIntWithoutAllocatingIt() = runSuspend {
        val skippedLength = Int.MAX_VALUE.toLong() + 5000
        val prefix = varint((12 shl 3 or 2).toLong()) + varint(skippedLength)
        val envelope = field(1, scalar(2, 17) + scalar(3, 7))
        val entryOffset = prefix.size + skippedLength
        val source = SparseSource(entryOffset + envelope.size, listOf(0L to prefix, entryOffset to envelope))
        val reader = SpecialDanmakuIndexReader(source.length, source::read)

        assertEquals(SpecialDanmakuEntry(entryOffset, envelope.size, 17, 7), reader.next())
        assertNull(reader.next())
        assertTrue(source.downloadedBytes <= 2048)
    }

    @Test
    fun varintsCrossBufferBoundariesAndUnknownWireTypesAreSkipped() = runSuspend {
        // The envelope starts at 1022, so its two-byte length crosses the cache boundary.
        val padding = field(10, ByteArray(1019))
        assertEquals(1022, padding.size)
        val unknownFields = scalar(15, -1L) + varint((16 shl 3 or 1).toLong()) + ByteArray(8) +
            varint((17 shl 3 or 5).toLong()) + ByteArray(4) + field(18, ByteArray(150))
        val envelope = field(1, unknownFields + scalar(2, 16384) + scalar(3, 9))
        val source = padding + envelope
        val requests = mutableListOf<Pair<Long, Int>>()
        val reader = SpecialDanmakuIndexReader(source.size.toLong()) { offset, count ->
            requests += offset to count
            source.copyOfRange(offset.toInt(), offset.toInt() + count)
        }

        assertEquals(SpecialDanmakuEntry(padding.size.toLong(), envelope.size, 16384, 9), reader.next())
        assertNull(reader.next())
        assertTrue(requests.size >= 2)
        requests.forEach { (offset, count) ->
            assertTrue(count in 1..1024)
            assertTrue(offset + count <= source.size)
        }
    }

    @Test
    fun duplicateMetadataUsesLastValueAndEmptyElementUsesDefaults() = runSuspend {
        val source = field(1, scalar(2, 10) + scalar(3, 7) + scalar(2, 20) + scalar(3, 9)) +
            field(1, byteArrayOf())
        val reader = reader(source)
        assertEquals(20L, assertNotNull(reader.next()).startTimeMs)
        val empty = assertNotNull(reader.next())
        assertEquals(0L, empty.startTimeMs)
        assertEquals(1, empty.mode)
        assertEquals(2, empty.byteLength)
        assertNull(reader.next())
    }

    @Test
    fun truncatedMessagesAndRangesFailExplicitly() {
        val malformed = listOf(
            byteArrayOf(0x80.toByte()), // unfinished tag
            byteArrayOf(10, 0x80.toByte()), // unfinished length
            byteArrayOf(10, 3, 16, 1), // declared envelope exceeds file
            byteArrayOf(10, 1, 16, 0), // value must not bleed into following top-level bytes
            byteArrayOf(10, 2, 58, 1), // nested content length exceeds element
            byteArrayOf(9, 0, 0), // incomplete fixed64
            byteArrayOf(13, 0, 0) // incomplete fixed32
        )
        malformed.forEach { source ->
            assertFailsWith<EOFException> { runSuspend { reader(source).next() } }
        }
        assertFailsWith<EOFException> {
            runSuspend { SpecialDanmakuIndexReader(20) { _, count -> ByteArray(count - 1) }.next() }
        }
    }

    @Test
    fun invalidTagsWireTypesVarintsAndLengthsFailWithoutLargeReads() {
        val malformed = listOf(
            byteArrayOf(0),
            byteArrayOf(1), // field number zero
            byteArrayOf(11), // group start
            byteArrayOf(12), // group end
            byteArrayOf(14),
            byteArrayOf(15),
            field(1, byteArrayOf(11)), // nested groups also unsupported
            varint(0x100000000L), // tag wider than uint32
            ByteArray(10) { 0x80.toByte() },
            ByteArray(9) { 0x80.toByte() } + byteArrayOf(2), // 65-bit varint
            byteArrayOf(10) + varint(-1L), // unsigned length cannot fit Long
            byteArrayOf(10) + varint(Long.MAX_VALUE) // bounded by known file size
        )
        malformed.forEach { source ->
            assertFailsWith<IOException> { runSuspend { reader(source).next() } }
        }
        val header = byteArrayOf(10) + varint(Int.MAX_VALUE.toLong())
        val source = SparseSource(Int.MAX_VALUE.toLong() + header.size, listOf(0L to header))
        assertFailsWith<IOException> {
            runSuspend { SpecialDanmakuIndexReader(source.length, source::read).next() }
        }
        assertEquals(1024L, source.downloadedBytes)
        assertFailsWith<IOException> {
            runSuspend { SpecialDanmakuIndexReader(1) { _, count -> ByteArray(count + 1) }.next() }
        }
        assertFailsWith<IllegalArgumentException> { SpecialDanmakuIndexReader(-1) { _, _ -> byteArrayOf() } }
    }

    @Test
    fun emptyFileDoesNotRead() = runSuspend {
        val reader = SpecialDanmakuIndexReader(0) { _, _ -> error("Must not read an empty file") }
        assertNull(reader.next())
    }

    private fun reader(source: ByteArray) = SpecialDanmakuIndexReader(source.size.toLong()) { offset, count ->
        source.copyOfRange(offset.toInt(), offset.toInt() + count)
    }

    /** Serves actual requested bytes from a sparse virtual file, counting every transferred byte. */
    private class SparseSource(val length: Long, private val segments: List<Pair<Long, ByteArray>>) {
        var downloadedBytes = 0L
        val requests = mutableListOf<Pair<Long, Int>>()

        suspend fun read(offset: Long, count: Int): ByteArray {
            assertTrue(offset >= 0 && count > 0 && count <= 1024 && count.toLong() <= length - offset)
            downloadedBytes += count
            requests += offset to count
            val bytes = ByteArray(count) { 0x55 }
            for ((segmentOffset, segment) in segments) {
                val start = maxOf(offset, segmentOffset)
                val end = minOf(offset + count, segmentOffset + segment.size)
                if (start < end) {
                    segment.copyInto(bytes, (start - offset).toInt(), (start - segmentOffset).toInt(),
                        (end - segmentOffset).toInt())
                }
            }
            return bytes
        }
    }

    private fun scalar(field: Int, value: Long): ByteArray = varint((field shl 3).toLong()) + varint(value)

    private fun field(field: Int, payload: ByteArray): ByteArray =
        varint((field shl 3 or 2).toLong()) + varint(payload.size.toLong()) + payload

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val bytes = ArrayList<Byte>(10)
        do {
            var next = (remaining and 0x7f).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) next = next or 0x80
            bytes += next.toByte()
        } while (remaining != 0L)
        return bytes.toByteArray()
    }

    // The range sources above complete synchronously; no kotlinx-coroutines-test dependency is needed.
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) {
                result = value
            }
        })
        return checkNotNull(result) { "Test coroutine unexpectedly suspended" }.getOrThrow()
    }
}
