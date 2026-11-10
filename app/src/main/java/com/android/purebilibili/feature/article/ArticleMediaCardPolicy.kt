package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.OpusLinkCard
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun parseArticleOpsMediaCard(insert: JsonObject): ArticleContentBlock.MediaCard? {
    return listOf("article-card", "video-card", "live-card", "vote-card", "goods-card", "mall-card")
        .firstNotNullOfOrNull { kind ->
            val card = insert[kind]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return@firstNotNullOfOrNull null
            articleMediaCard(
                kind = kind,
                id = card.value("id"),
                cover = card.value("url"),
                title = card.value("title").ifBlank { card.value("alt") },
                targetUrl = card.value("jump_url"),
                width = card.value("width").toDoubleOrNull()?.toInt() ?: 0,
                height = card.value("height").toDoubleOrNull()?.toInt() ?: 0,
            )
        }
}

internal fun parseArticleHtmlMediaCard(imageTag: String): ArticleContentBlock.MediaCard? {
    fun attribute(name: String): String = Regex("""(?is)\b$name\s*=\s*["']([^"']*)["']""")
        .find(imageTag)?.groupValues?.getOrNull(1).orEmpty()
    val kind = attribute("class").split(Regex("\\s+")).firstOrNull { it.endsWith("-card") } ?: return null
    return articleMediaCard(kind,
        attribute("aid").ifBlank { attribute("data-id") }.ifBlank { attribute("data-aid") }.ifBlank { attribute("data-article-id") }.ifBlank { attribute("bvid") },
        attribute("data-src").ifBlank { attribute("src") }, attribute("alt"), attribute("data-url"),
        attribute("width").toDoubleOrNull()?.toInt() ?: 0, attribute("height").toDoubleOrNull()?.toInt() ?: 0)
}

private fun articleMediaCard(kind: String, id: String, cover: String, title: String, targetUrl: String, width: Int = 0, height: Int = 0): ArticleContentBlock.MediaCard? {
    val cleanedId = id.trim()
    val numericId = when (kind) {
        "article-card" -> cleanedId.removePrefix("cv").removePrefix("CV")
        "video-card" -> cleanedId.removePrefix("av").removePrefix("AV")
        else -> cleanedId
    }
    val positiveId = numericId.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.toLongOrNull()?.takeIf { it > 0L }
    val type = when (kind) {
        "article-card" -> "LINK_CARD_TYPE_OPUS"
        "video-card" -> "LINK_CARD_TYPE_UGC"
        "live-card" -> "LINK_CARD_TYPE_LIVE"
        "vote-card" -> "LINK_CARD_TYPE_VOTE"
        "goods-card", "mall-card" -> "LINK_CARD_TYPE_GOODS"
        else -> return null
    }
    val target = normalizeArticleLink(targetUrl) ?: when (kind) {
        "article-card" -> positiveId?.let { "https://www.bilibili.com/read/cv$it" }
        "video-card" -> when {
            Regex("BV[a-zA-Z0-9]{10}").matches(cleanedId) -> "https://www.bilibili.com/video/$cleanedId"
            positiveId != null -> "https://www.bilibili.com/video/av$positiveId"
            else -> null
        }
        "live-card" -> positiveId?.let { "https://live.bilibili.com/$it" }
        else -> null
    }
    // 无目标的旧卡片仍交给图片解析，避免正文内容消失或产生空操作。
    if (target == null && !(kind == "vote-card" && positiveId != null)) return null
    val normalizedCover = normalizeArticleLink(cover).orEmpty()
    return ArticleContentBlock.MediaCard(card = OpusLinkCard(
        type = type, oid = positiveId?.toString() ?: cleanedId,
        title = title.takeUnless { it.isBlank() || it == "undefined" } ?: when (kind) {
            "article-card" -> "专栏 $cleanedId"
            "video-card" -> "视频 $cleanedId"
            "live-card" -> "直播 $cleanedId"
            "vote-card" -> "投票"
            else -> "商品"
        },
        cover = normalizedCover,
        jumpUrl = target.orEmpty(),
    ), preview = normalizedCover.takeIf(String::isNotBlank)?.let {
        ArticleContentBlock.Image(it, width = width.coerceAtLeast(0), height = height.coerceAtLeast(0))
    })
}

private fun JsonObject.value(key: String): String = runCatching { get(key)?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty()
