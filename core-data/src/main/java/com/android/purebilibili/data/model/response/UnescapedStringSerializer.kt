package com.android.purebilibili.data.model.response

import com.android.purebilibili.core.util.HtmlEntityUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 反转义 B 站评论正文中的 HTML 实体（`&#39;` `&quot;` `&amp;` 等）。
 *
 * 仅作用于反序列化：REST/JSON 返回的 message 字段会携带 HTML 实体，
 * 这里在解析时统一清洗，避免 UI 直接展示 `&#39` 这类字面量（#583）。
 * 序列化原样输出，不影响请求构造。
 */
object UnescapedStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor(
        "com.android.purebilibili.data.model.response.UnescapedString",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return HtmlEntityUtils.unescape(decoder.decodeString())
    }
}
