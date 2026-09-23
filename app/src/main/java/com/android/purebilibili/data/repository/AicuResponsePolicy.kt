package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.*
import kotlinx.serialization.json.*
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AicuRequestException(message: String, val retryAfterSeconds: Long = 0) : IOException(message)

internal fun aicuRetryAfterSeconds(raw: String?, now: Instant = Instant.now()): Long =
    raw?.trim()?.toLongOrNull()?.coerceIn(0, 86400)
        ?: runCatching {
            (ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() - now.epochSecond)
                .coerceIn(0, 86400)
        }.getOrDefault(0)

internal fun aicuHttpFailure(status: Int, retryAfter: String?): AicuRequestException = when (status) {
    403, 468 -> AicuRequestException("Aicu 要求安全验证，当前原生查询暂不可用，请稍后重试。")
    429 -> AicuRequestException("请求过于频繁，请稍后重试。", aicuRetryAfterSeconds(retryAfter).coerceAtLeast(1))
    503 -> AicuRequestException("Aicu 服务暂不可用，请稍后重试。")
    else -> AicuRequestException("Aicu 请求失败（HTTP $status）。")
}

internal fun JsonObject.aicuText(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.aicuLong(key: String): Long? = aicuText(key).toLongOrNull()
internal fun JsonObject.aicuObject(key: String): JsonObject = get(key) as? JsonObject ?: JsonObject(emptyMap())

internal fun decodeAicuEnvelope(body: String): JsonObject {
    val envelope = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        ?: throw AicuRequestException("Aicu 返回了无法识别的数据，可能需要安全验证。")
    when (val code = envelope.aicuLong("code")) {
        0L -> return envelope.aicuObject("data")
        -419L -> throw AicuRequestException("排队凭据无效或已过期，请重新查询。")
        403L, 468L -> throw aicuHttpFailure(403, null)
        429L -> throw aicuHttpFailure(429, null)
        else -> throw AicuRequestException(
            envelope.aicuText("message").take(250).ifBlank { "Aicu 返回错误（${code ?: "未知"}）。" }
        )
    }
}

internal fun decodeAicuPage(data: JsonObject, query: AicuQuery): AicuPage {
    val key = when (query.category) {
        AicuCategory.COMMENT -> "replies"
        AicuCategory.VIDEO_DANMAKU -> "videodmlist"
        AicuCategory.LIVE_DANMAKU -> "list"
    }
    val items = data[key] as? JsonArray
        ?: throw AicuRequestException("Aicu 记录格式发生变化，请稍后重试。")
    val records = items.flatMapIndexed { index, element ->
        val item = element as? JsonObject ?: return@flatMapIndexed emptyList()
        when (query.category) {
            AicuCategory.COMMENT -> listOf(AicuRecord(
                id = item.aicuText("rpid"), text = item.aicuText("message"),
                timestampSeconds = item.aicuLong("time"),
                objectId = item.aicuObject("dyn").aicuText("oid"),
                objectType = item.aicuObject("dyn").aicuLong("type")?.toInt() ?: 0,
                rootId = item.aicuObject("parent").aicuText("rootid"),
                rank = item.aicuLong("rank")?.toInt() ?: 0,
            ))
            AicuCategory.VIDEO_DANMAKU -> listOf(AicuRecord(
                id = item.aicuText("id"), text = item.aicuText("content"),
                timestampSeconds = item.aicuLong("ctime"), objectId = item.aicuText("oid"),
                progressMs = item.aicuLong("progress"), objectType = 1,
            ))
            AicuCategory.LIVE_DANMAKU -> {
                val room = item.aicuObject("roominfo")
                (item["danmu"] as? JsonArray).orEmpty().mapNotNull { entry ->
                    val message = entry as? JsonObject ?: return@mapNotNull null
                    AicuRecord(
                        id = message.aicuText("id"), text = message.aicuText("text"),
                        timestampSeconds = message.aicuLong("ts"),
                        roomId = room.aicuText("roomid"), roomName = room.aicuText("roomname"),
                        upName = room.aicuText("upname"), authorName = message.aicuText("uname"),
                        groupKey = "${query.page}:$index",
                    )
                }
            }
        }
    }
    val cursor = data.aicuObject("cursor")
    val total = cursor.aicuLong("all_count")?.takeIf { it >= 0 }
    val explicitEnd = (cursor["is_end"] as? JsonPrimitive)?.booleanOrNull
    return AicuPage(query, records, total, explicitEnd ?: (
        items.isEmpty() || (total != null && query.page.toLong() * AicuQuery.PAGE_SIZE >= total)
    ))
}

internal data class AicuQueueEvent(val type: String, val data: String)

/** SSE events end at a blank line; comment heartbeats are ignored. */
internal class AicuQueueEventDecoder {
    private var type = "message"
    private val data = StringBuilder()
    fun accept(line: String): AicuQueueEvent? {
        if (line.isEmpty()) {
            val event = AicuQueueEvent(type, data.toString().trimEnd('\n'))
            type = "message"
            data.setLength(0)
            return event
        }
        if (line.startsWith(':')) return null
        val value = line.substringAfter(':', "").removePrefix(" ")
        when (line.substringBefore(':')) {
            "event" -> type = value
            "data" -> {
                if (data.length + value.length > 65536) throw AicuRequestException("排队响应过大，请重试。")
                data.append(value).append('\n')
            }
        }
        return null
    }
}
