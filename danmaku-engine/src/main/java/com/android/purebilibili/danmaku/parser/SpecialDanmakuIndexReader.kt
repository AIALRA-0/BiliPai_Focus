package com.android.purebilibili.danmaku.parser

import java.io.EOFException
import java.io.IOException

/** An original field-1 envelope, suitable for parsing as a one-element reply. */
data class SpecialDanmakuEntry(
    val offset: Long,
    val byteLength: Int,
    val startTimeMs: Long,
    val mode: Int
)

/**
 * Incrementally indexes a DmSegMobileReply without fetching its content strings.
 *
 * Only one bounded range is retained. Length-delimited and fixed-width fields are
 * skipped by advancing the offset, not by downloading their bytes. A range can
 * include up to [READ_BUFFER_SIZE] bytes of lookahead, including skipped content.
 * Entries preserve file order; callers may sort the small metadata independently.
 * This reader is single-consumer: calls to [next] must not overlap.
 */
class SpecialDanmakuIndexReader(
    val byteLength: Long,
    private val readRange: suspend (offset: Long, byteCount: Int) -> ByteArray
) {
    init {
        require(byteLength >= 0) { "Negative protobuf byte length" }
    }

    private var position = 0L
    private var bufferOffset = 0L
    private var buffer = ByteArray(0)

    suspend fun next(): SpecialDanmakuEntry? {
        while (position < byteLength) {
            val entryOffset = position
            val tag = readTag(byteLength)
            if (tag ushr 3 != 1L || tag and 7L != 2L) {
                skipField((tag and 7L).toInt(), byteLength)
                continue
            }

            val elementEnd = readDelimitedEnd(byteLength)
            val envelopeLength = elementEnd - entryOffset
            if (envelopeLength > Int.MAX_VALUE) {
                throw IOException("Danmaku envelope exceeds Int byte length at $entryOffset")
            }
            var startTimeMs = 0L
            var mode = 1
            while (position < elementEnd) {
                val elementTag = readTag(elementEnd)
                val wireType = (elementTag and 7L).toInt()
                when {
                    elementTag ushr 3 == 2L && wireType == 0 -> {
                        // DanmakuElem progress and mode are protobuf int32 fields.
                        startTimeMs = readVarint(elementEnd).toInt().toLong()
                    }
                    elementTag ushr 3 == 3L && wireType == 0 -> {
                        mode = readVarint(elementEnd).toInt()
                    }
                    else -> skipField(wireType, elementEnd)
                }
            }
            return SpecialDanmakuEntry(entryOffset, envelopeLength.toInt(), startTimeMs, mode)
        }
        return null
    }

    private suspend fun readTag(limit: Long): Long {
        val tag = readVarint(limit)
        if (tag <= 0L || tag > 0xffffffffL || tag ushr 3 == 0L) {
            throw IOException("Invalid protobuf tag at $position")
        }
        return tag
    }

    private suspend fun readDelimitedEnd(limit: Long): Long {
        val length = readVarint(limit)
        if (length < 0L) throw IOException("Protobuf length exceeds Long at $position")
        ensureAvailable(length, limit)
        return position + length
    }

    private suspend fun skipField(wireType: Int, limit: Long) {
        when (wireType) {
            0 -> readVarint(limit)
            1 -> skipBytes(8L, limit)
            2 -> position = readDelimitedEnd(limit)
            5 -> skipBytes(4L, limit)
            else -> throw IOException("Unsupported protobuf wire type $wireType at $position")
        }
    }

    private fun skipBytes(count: Long, limit: Long) {
        ensureAvailable(count, limit)
        position += count
    }

    private fun ensureAvailable(count: Long, limit: Long) {
        // Subtraction avoids overflowing position + an untrusted length.
        if (count > limit - position) {
            throw EOFException("Truncated protobuf field at $position (needs $count bytes)")
        }
    }

    private suspend fun readVarint(limit: Long): Long {
        var value = 0L
        for (index in 0 until 10) {
            val next = readByte(limit)
            if (index == 9 && next > 1) {
                throw IOException("Protobuf varint exceeds 64 bits at $position")
            }
            value = value or ((next and 0x7f).toLong() shl (index * 7))
            if (next and 0x80 == 0) return value
        }
        throw IOException("Unterminated protobuf varint at $position")
    }

    private suspend fun readByte(limit: Long): Int {
        if (position >= limit) throw EOFException("Truncated protobuf varint at $position")
        val bufferIndex = position - bufferOffset
        if (bufferIndex < 0L || bufferIndex >= buffer.size.toLong()) {
            val count = minOf(READ_BUFFER_SIZE.toLong(), byteLength - position).toInt()
            val bytes = readRange(position, count)
            if (bytes.size < count) {
                throw EOFException("Range at $position returned ${bytes.size} of $count bytes")
            }
            if (bytes.size > count) {
                throw IOException("Range at $position exceeded requested $count bytes")
            }
            bufferOffset = position
            buffer = bytes
        }
        val result = buffer[(position - bufferOffset).toInt()].toInt() and 0xff
        position++
        return result
    }

    private companion object {
        const val READ_BUFFER_SIZE = 1024
    }
}
