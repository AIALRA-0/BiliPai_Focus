package com.android.purebilibili.core.network.grpc

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal object ProtoWire {
    const val WIRE_VARINT = 0
    const val WIRE_FIXED64 = 1
    const val WIRE_LENGTH_DELIMITED = 2
    const val WIRE_FIXED32 = 5

    data class Field(
        val number: Int,
        val wireType: Int,
        val varint: Long = 0L,
        val bytes: ByteArray = ByteArray(0),
        val fixed64: Long = 0L,
        val fixed32: Int = 0
    )

    fun frame(message: ByteArray, gzipMinLength: Int = 64): ByteArray {
        val compressed = message.size > gzipMinLength
        val payload = if (compressed) gzip(message) else message
        return ByteArrayOutputStream(payload.size + 5).use { out ->
            out.write(if (compressed) 1 else 0)
            out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.size).array())
            out.write(payload)
            out.toByteArray()
        }
    }

    fun unframe(frame: ByteArray): ByteArray {
        require(frame.size >= 5) { "Invalid gRPC frame length: ${frame.size}" }
        val compressed = frame[0].toInt() == 1
        val length = ByteBuffer.wrap(frame, 1, 4).order(ByteOrder.BIG_ENDIAN).int
        require(length >= 0 && frame.size >= length + 5) { "Invalid gRPC payload length: $length" }
        val payload = frame.copyOfRange(5, 5 + length)
        return if (compressed) gunzip(payload) else payload
    }

    fun message(vararg fields: ByteArray): ByteArray {
        return ByteArrayOutputStream(fields.sumOf { it.size }).use { out ->
            fields.forEach(out::write)
            out.toByteArray()
        }
    }

    fun int64(fieldNumber: Int, value: Long): ByteArray = field(fieldNumber, WIRE_VARINT, varint(value))

    fun int32(fieldNumber: Int, value: Int): ByteArray = int64(fieldNumber, value.toLong())

    fun bool(fieldNumber: Int, value: Boolean): ByteArray = int64(fieldNumber, if (value) 1L else 0L)

    fun string(fieldNumber: Int, value: String): ByteArray {
        if (value.isEmpty()) return ByteArray(0)
        val bytes = value.toByteArray(Charsets.UTF_8)
        return field(fieldNumber, WIRE_LENGTH_DELIMITED, varint(bytes.size.toLong()), bytes)
    }

    fun bytes(fieldNumber: Int, value: ByteArray): ByteArray {
        if (value.isEmpty()) return ByteArray(0)
        return field(fieldNumber, WIRE_LENGTH_DELIMITED, varint(value.size.toLong()), value)
    }

    fun double(fieldNumber: Int, value: Double): ByteArray {
        val fixed = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putDouble(value)
            .array()
        return field(fieldNumber, WIRE_FIXED64, fixed)
    }

    fun parseFields(data: ByteArray): List<Field> {
        val reader = Reader(data)
        val fields = mutableListOf<Field>()
        while (!reader.exhausted()) {
            val tag = reader.readVarint().toInt()
            if (tag == 0) break
            val number = tag ushr 3
            val wireType = tag and 0x07
            fields += when (wireType) {
                WIRE_VARINT -> Field(number, wireType, varint = reader.readVarint())
                WIRE_FIXED64 -> Field(number, wireType, fixed64 = reader.readFixed64())
                WIRE_LENGTH_DELIMITED -> {
                    val length = reader.readVarint().toInt()
                    Field(number, wireType, bytes = reader.readBytes(length))
                }
                WIRE_FIXED32 -> Field(number, wireType, fixed32 = reader.readFixed32())
                else -> error("Unsupported protobuf wire type: $wireType")
            }
        }
        return fields
    }

    fun stringValue(field: Field): String = decodeStringValue(field.bytes)

    fun doubleValue(field: Field): Double {
        return ByteBuffer.wrap(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(field.fixed64).array())
            .order(ByteOrder.LITTLE_ENDIAN)
            .double
    }

    private fun field(fieldNumber: Int, wireType: Int, vararg payload: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { out ->
            out.write(varint(((fieldNumber shl 3) or wireType).toLong()))
            payload.forEach(out::write)
            out.toByteArray()
        }
    }

    private fun varint(value: Long): ByteArray {
        var current = value
        val out = ByteArrayOutputStream()
        while (true) {
            if ((current and 0x7FL.inv()) == 0L) {
                out.write(current.toInt())
                return out.toByteArray()
            }
            out.write(((current and 0x7F) or 0x80).toInt())
            current = current ushr 7
        }
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        return GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
    }

    private fun decodeStringValue(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8)
        if (REPLACEMENT_CHAR !in utf8) return utf8

        // B 站部分 gRPC 字符串会用 CESU-8 代理对承载非 BMP 字符，标准 UTF-8 会把它解成 U+FFFD。
        val tolerant = decodeUtf8AllowingCesu8Surrogates(bytes)
        return if (tolerant.countReplacementChars() < utf8.countReplacementChars()) {
            tolerant
        } else {
            utf8
        }
    }

    private fun decodeUtf8AllowingCesu8Surrogates(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size)
        var index = 0
        while (index < bytes.size) {
            index = appendUtf8OrCesu8CodePoint(bytes, index, out)
        }
        return out.toString()
    }

    private fun appendUtf8OrCesu8CodePoint(bytes: ByteArray, index: Int, out: StringBuilder): Int {
        val first = bytes[index].unsigned()
        return when {
            first < 0x80 -> {
                out.append(first.toChar())
                index + 1
            }

            first in 0xC2..0xDF && hasContinuation(bytes, index, 1) -> {
                val codePoint = ((first and 0x1F) shl 6) or (bytes[index + 1].unsigned() and 0x3F)
                out.append(codePoint.toChar())
                index + 2
            }

            first in 0xE0..0xEF && hasContinuation(bytes, index, 2) && isValidThreeBytePrefix(bytes, index) ->
                appendThreeByteOrCesu8CodePoint(bytes, index, out)

            first in 0xF0..0xF4 && hasContinuation(bytes, index, 3) && isValidFourBytePrefix(bytes, index) -> {
                out.appendCodePoint(decodeFourByteCodePoint(bytes, index))
                index + 4
            }

            else -> {
                out.append(REPLACEMENT_CHAR)
                index + 1
            }
        }
    }

    private fun appendThreeByteOrCesu8CodePoint(bytes: ByteArray, index: Int, out: StringBuilder): Int {
        val codePoint = decodeThreeByteCodePoint(bytes, index)
        if (codePoint in HIGH_SURROGATE_START..HIGH_SURROGATE_END) {
            val lowSurrogate = decodeCesu8LowSurrogate(bytes, index + 3)
            return if (lowSurrogate != null) {
                out.appendCodePoint(Character.toCodePoint(codePoint.toChar(), lowSurrogate.toChar()))
                index + 6
            } else {
                out.append(REPLACEMENT_CHAR)
                index + 3
            }
        }
        if (codePoint in LOW_SURROGATE_START..LOW_SURROGATE_END) {
            out.append(REPLACEMENT_CHAR)
        } else {
            out.append(codePoint.toChar())
        }
        return index + 3
    }

    private fun decodeCesu8LowSurrogate(bytes: ByteArray, index: Int): Int? {
        if (index + 2 >= bytes.size || !hasContinuation(bytes, index, 2)) return null
        if (bytes[index].unsigned() != 0xED) return null
        val codePoint = decodeThreeByteCodePoint(bytes, index)
        return codePoint.takeIf { it in LOW_SURROGATE_START..LOW_SURROGATE_END }
    }

    private fun hasContinuation(bytes: ByteArray, index: Int, count: Int): Boolean {
        if (index + count >= bytes.size) return false
        for (offset in 1..count) {
            if ((bytes[index + offset].unsigned() and 0xC0) != 0x80) return false
        }
        return true
    }

    private fun isValidThreeBytePrefix(bytes: ByteArray, index: Int): Boolean {
        val first = bytes[index].unsigned()
        val second = bytes[index + 1].unsigned()
        return when (first) {
            0xE0 -> second >= 0xA0
            else -> true
        }
    }

    private fun isValidFourBytePrefix(bytes: ByteArray, index: Int): Boolean {
        val first = bytes[index].unsigned()
        val second = bytes[index + 1].unsigned()
        return when (first) {
            0xF0 -> second >= 0x90
            0xF4 -> second <= 0x8F
            else -> true
        }
    }

    private fun decodeThreeByteCodePoint(bytes: ByteArray, index: Int): Int {
        return ((bytes[index].unsigned() and 0x0F) shl 12) or
            ((bytes[index + 1].unsigned() and 0x3F) shl 6) or
            (bytes[index + 2].unsigned() and 0x3F)
    }

    private fun decodeFourByteCodePoint(bytes: ByteArray, index: Int): Int {
        return ((bytes[index].unsigned() and 0x07) shl 18) or
            ((bytes[index + 1].unsigned() and 0x3F) shl 12) or
            ((bytes[index + 2].unsigned() and 0x3F) shl 6) or
            (bytes[index + 3].unsigned() and 0x3F)
    }

    private fun Byte.unsigned(): Int = toInt() and 0xFF

    private fun String.countReplacementChars(): Int = count { it == REPLACEMENT_CHAR }

    private const val REPLACEMENT_CHAR = '\uFFFD'
    private const val HIGH_SURROGATE_START = 0xD800
    private const val HIGH_SURROGATE_END = 0xDBFF
    private const val LOW_SURROGATE_START = 0xDC00
    private const val LOW_SURROGATE_END = 0xDFFF

    private class Reader(private val data: ByteArray) {
        private var position = 0

        fun exhausted(): Boolean = position >= data.size

        fun readVarint(): Long {
            var shift = 0
            var result = 0L
            while (shift < 64) {
                val byte = readByte().toLong() and 0xFF
                result = result or ((byte and 0x7F) shl shift)
                if ((byte and 0x80) == 0L) return result
                shift += 7
            }
            error("Malformed protobuf varint")
        }

        fun readFixed64(): Long {
            require(position + 8 <= data.size) { "Unexpected EOF reading fixed64" }
            val value = ByteBuffer.wrap(data, position, 8).order(ByteOrder.LITTLE_ENDIAN).long
            position += 8
            return value
        }

        fun readFixed32(): Int {
            require(position + 4 <= data.size) { "Unexpected EOF reading fixed32" }
            val value = ByteBuffer.wrap(data, position, 4).order(ByteOrder.LITTLE_ENDIAN).int
            position += 4
            return value
        }

        fun readBytes(length: Int): ByteArray {
            require(length >= 0 && position + length <= data.size) { "Unexpected EOF reading bytes: $length" }
            return data.copyOfRange(position, position + length).also {
                position += length
            }
        }

        private fun readByte(): Byte {
            require(position < data.size) { "Unexpected EOF reading byte" }
            return data[position++]
        }
    }
}
