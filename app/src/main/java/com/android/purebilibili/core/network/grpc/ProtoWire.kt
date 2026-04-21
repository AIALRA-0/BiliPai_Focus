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

    fun stringValue(field: Field): String = field.bytes.toString(Charsets.UTF_8)

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
