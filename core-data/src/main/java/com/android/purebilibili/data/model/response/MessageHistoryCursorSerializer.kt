package com.android.purebilibili.data.model.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Empty message histories can return UINT64_MAX as their minimum sequence number. */
object MessageHistoryCursorSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("MessageHistoryCursor", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        if (decoder !is JsonDecoder) return decoder.decodeLong().coerceAtLeast(0L)
        val raw = (decoder.decodeJsonElement() as? JsonPrimitive)?.contentOrNull
        if (raw == "18446744073709551615") return 0L
        return raw?.toLongOrNull()?.coerceAtLeast(0L)
            ?: throw SerializationException("Invalid message history cursor")
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}
