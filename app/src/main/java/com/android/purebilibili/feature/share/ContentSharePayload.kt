package com.android.purebilibili.feature.share

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.DynamicItem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 分享身份与展示数据一起冻结，避免换集或切换内容后发送到旧目标。 */
internal data class ContentSharePayload(
    val key: String,
    val title: String,
    val url: String,
    val cover: String = "",
    val author: String = "",
    val authorId: Long = 0,
    val source: Int = 5,
    val resourceId: Long = 0,
    val dynamicType: Int? = null,
    val bvid: String = "",
    val messageCard: String? = null,
    val messageType: Int = 7,
    val positionMs: Long = 0,
    val replyOid: Long = 0,
    val replyType: Int = 1,
) {
    suspend fun cardContent(): String {
        messageCard?.let { return it }
        val video = if (resourceId <= 0 && bvid.isNotBlank()) {
            val result = NetworkModule.api.getVideoInfo(bvid)
            check(result.code == 0) { result.message.ifBlank { "获取视频信息失败" } }
            result.data ?: kotlin.error("视频编号不可用")
        } else null
        val id = resourceId.takeIf { it > 0 } ?: video?.aid?.takeIf { it > 0 }
            ?: kotlin.error("分享内容编号不可用")
        return buildJsonObject {
            put("id", id.toString())
            put("title", title)
            put("headline", title)
            put("source", source)
            put("thumb", cover.ifBlank { video?.pic.orEmpty() })
            put("url", url)
            put("author", author.ifBlank { video?.owner?.name.orEmpty() })
            put("author_id", (authorId.takeIf { it > 0 } ?: video?.owner?.mid ?: 0L).toString())
            if (source == 16) put("source_desc", "番剧影视")
        }.toString()
    }
}

internal fun buildPositionShareUrl(url: String, positionMs: Long): String {
    if (positionMs <= 0) return url
    val seconds = (positionMs / 1000).toString() + (positionMs % 1000).takeIf { it > 0 }?.let {
        "." + it.toString().padStart(3, '0').trimEnd('0')
    }.orEmpty()
    val base = url.substringBefore('#')
    val fragment = url.substringAfter('#', "").takeIf { it.isNotEmpty() }?.let { "#$it" }.orEmpty()
    return "$base${if ('?' in base) '&' else '?'}t=$seconds$fragment"
}

internal fun buildLiveSharePayload(roomId: Long, title: String, cover: String = "", author: String = "", authorId: Long = 0): ContentSharePayload {
    val url = "https://live.bilibili.com/$roomId"
    return ContentSharePayload(
        key = "live:$roomId", title = title.ifBlank { "直播间" }, url = url,
        cover = cover, author = author, authorId = authorId, messageType = 14,
        messageCard = buildJsonObject {
            put("cover", cover); put("sourceID", roomId.toString()); put("title", title)
            put("url", url); put("authorID", authorId.toString()); put("source", "直播")
            put("desc", title); put("author", author)
        }.toString(),
    )
}

internal fun buildDynamicContentSharePayload(item: DynamicItem): ContentSharePayload = ContentSharePayload(
    key = "dynamic:${item.id_str}",
    title = item.modules.module_dynamic?.major?.opus?.title.orEmpty().ifBlank {
        item.modules.module_dynamic?.desc?.text.orEmpty().ifBlank { "动态" }
    },
    url = "https://t.bilibili.com/${item.id_str}",
    messageCard = com.android.purebilibili.feature.dynamic.components.buildDynamicShareCardContent(item),
)
