package com.android.purebilibili.core.network.grpc

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.WireFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// protobuf wire 编解码为共享网络能力，与 BiliGrpcClient 一并供两端消费。
object ProtoWire {
    const val WIRE_VARINT = WireFormat.WIRETYPE_VARINT
    const val WIRE_FIXED64 = WireFormat.WIRETYPE_FIXED64
    const val WIRE_LENGTH_DELIMITED = WireFormat.WIRETYPE_LENGTH_DELIMITED
    const val WIRE_FIXED32 = WireFormat.WIRETYPE_FIXED32

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

    fun fixed64(fieldNumber: Int, value: Long): ByteArray = encode { writeFixed64(fieldNumber, value) }

    fun fixed32(fieldNumber: Int, value: Int): ByteArray = encode { writeFixed32(fieldNumber, value) }

    fun int64(fieldNumber: Int, value: Long): ByteArray = encode { writeInt64(fieldNumber, value) }

    fun int32(fieldNumber: Int, value: Int): ByteArray = encode { writeInt32(fieldNumber, value) }

    fun bool(fieldNumber: Int, value: Boolean): ByteArray = encode { writeBool(fieldNumber, value) }

    fun packedInt64(fieldNumber: Int, values: List<Long>): ByteArray {
        if (values.isEmpty()) return ByteArray(0)
        val payload = encode { values.forEach { writeInt64NoTag(it) } }
        return bytes(fieldNumber, payload)
    }

    fun string(fieldNumber: Int, value: String): ByteArray =
        if (value.isEmpty()) ByteArray(0) else encode { writeString(fieldNumber, value) }

    fun bytes(fieldNumber: Int, value: ByteArray): ByteArray =
        if (value.isEmpty()) ByteArray(0) else encode { writeByteArray(fieldNumber, value) }

    fun double(fieldNumber: Int, value: Double): ByteArray = encode { writeDouble(fieldNumber, value) }

    /** 字段映射保持不变，长度、标签、变长整数和边界检查交给 Protobuf 运行库。 */
    fun parseFields(data: ByteArray): List<Field> {
        val input = CodedInputStream.newInstance(data)
        return buildList {
            while (true) {
                val tag = input.readTag()
                if (tag == 0) break
                val number = WireFormat.getTagFieldNumber(tag)
                val wireType = WireFormat.getTagWireType(tag)
                val field = when (wireType) {
                    WIRE_VARINT -> Field(number, wireType, varint = input.readInt64())
                    WIRE_FIXED64 -> Field(number, wireType, fixed64 = input.readFixed64())
                    WIRE_LENGTH_DELIMITED -> Field(number, wireType, bytes = input.readByteArray())
                    WIRE_FIXED32 -> Field(number, wireType, fixed32 = input.readFixed32())
                    // 未识别的旧式 group 交由运行库整体跳过，避免误读嵌套字段。
                    else -> {
                        require(input.skipField(tag)) { "Unexpected protobuf end-group" }
                        null
                    }
                }
                if (field != null) add(field)
            }
        }
    }

    fun stringValue(field: Field): String = decodeStringValue(field.bytes)

    fun doubleValue(field: Field): Double = java.lang.Double.longBitsToDouble(field.fixed64)

    private inline fun encode(write: CodedOutputStream.() -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        val output = CodedOutputStream.newInstance(bytes)
        write(output)
        output.flush()
        return bytes.toByteArray()
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

}
