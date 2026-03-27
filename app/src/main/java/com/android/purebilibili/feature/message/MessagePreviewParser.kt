package com.android.purebilibili.feature.message

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class MessageVideoCardPreview(
    val title: String,
    val bvid: String,
    val cover: String,
    val duration: Long
)

object MessagePreviewParser {

    fun parseSessionPreview(content: String?, msgType: Int): String {
        if (content.isNullOrEmpty()) return ""

        return when (msgType) {
            1 -> parseTextContent(content)
            2 -> "[图片]"
            5 -> "[消息已撤回]"
            6 -> "[表情]"
            7 -> parseSharePreview(content)
            10 -> "[通知]"
            11 -> parseVideoCard(content)?.let { "视频：${it.title}" } ?: "[视频]"
            12 -> "[专栏推送]"
            else -> "[消息]"
        }
    }

    fun parseVideoCard(content: String): MessageVideoCardPreview? {
        val json = parseJsonObject(content) ?: return null
        val bvid = json.string("bvid")
        val cover = json.string("cover")
        val title = json.string("title").ifBlank {
            if (json.long("times") == 0L) "内容已失效" else "视频"
        }
        val duration = json.long("times")

        if (title.isBlank() && bvid.isBlank() && cover.isBlank()) {
            return null
        }

        return MessageVideoCardPreview(
            title = title,
            bvid = bvid,
            cover = cover,
            duration = duration
        )
    }

    private fun parseSharePreview(content: String): String {
        val json = parseJsonObject(content) ?: return "[分享]"
        return when (json.int("source")) {
            5 -> json.string("title").takeIf { it.isNotBlank() }?.let { "分享视频：$it" } ?: "[分享视频]"
            6 -> "[分享专栏]"
            else -> "[分享]"
        }
    }

    private fun parseTextContent(content: String): String {
        if (!content.trim().startsWith("{")) {
            return content
        }

        return parseJsonObject(content)?.string("content").orEmpty().ifBlank { content }
    }

    private fun parseJsonObject(content: String): JsonObject? {
        return runCatching {
            Json.parseToJsonElement(content) as? JsonObject
        }.getOrNull()
    }

    private fun JsonObject.string(key: String): String {
        return (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    }

    private fun JsonObject.long(key: String): Long {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
    }

    private fun JsonObject.int(key: String): Int {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
    }
}
