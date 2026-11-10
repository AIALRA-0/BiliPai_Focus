package com.android.purebilibili.feature.share

import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.feature.video.ui.components.resolveReplyCommentShareUrl

internal sealed interface ShareImageBlock {
    data class Text(val text: String, val heading: Boolean = false, val emotes: Map<String, String> = emptyMap()) : ShareImageBlock
    data class Image(val url: String) : ShareImageBlock
    data class Author(val name: String, val avatar: String, val metadata: String = "") : ShareImageBlock
}

internal data class ShareImageDocument(val key: String, val title: String, val url: String, val blocks: List<ShareImageBlock>)

internal fun replyShareDocument(item: ReplyItem, context: ReplyShareContext? = null, emotes: Map<String, String> = emptyMap()): ShareImageDocument {
    val spec = com.android.purebilibili.feature.video.ui.components.buildReplyCommentImageSpec(item)
    return ShareImageDocument("reply:${item.oid}:${item.rpid}", "评论", resolveReplyCommentShareUrl(item, context), buildList {
        add(ShareImageBlock.Author(item.member.uname, item.member.avatar, spec.metadataText))
        add(ShareImageBlock.Text(item.content.message, emotes = emotes + item.content.emote.orEmpty().mapValues { it.value.url }))
        item.content.pictures.orEmpty().forEach { add(ShareImageBlock.Image(it.imgSrc)) }
        item.content.richText.note?.let { note ->
            if (note.summary.isNotBlank() && note.summary != item.content.message) add(ShareImageBlock.Text(note.summary))
            note.images.forEach { add(ShareImageBlock.Image(it)) }
        }
        item.content.vote?.let { add(ShareImageBlock.Text("投票：${it.title}")) }
        context?.let {
            if (it.title.isNotBlank()) add(ShareImageBlock.Text(it.title, heading = true))
            if (it.cover.isNotBlank()) add(ShareImageBlock.Image(it.cover))
        }
    })
}

private fun List<RichTextNode>.shareEmotes(): Map<String, String> = mapNotNull { node ->
    val emoji = node.emoji ?: return@mapNotNull null
    val key = node.text.ifBlank { node.orig_text.ifBlank { emoji.text } }
    val url = emoji.icon_url.ifBlank { emoji.webp_url.ifBlank { emoji.gif_url } }
    if (key.isBlank() || url.isBlank()) null else key to url
}.toMap()

internal fun dynamicShareDocument(item: DynamicItem): ShareImageDocument = ShareImageDocument(
    "dynamic:${item.id_str}", "动态", "https://t.bilibili.com/${item.id_str}", buildList {
        val visited = mutableSetOf<String>()
        fun appendDynamic(current: DynamicItem) {
            if (!visited.add(current.id_str)) return
            current.modules.module_author?.let { add(ShareImageBlock.Author(it.name, it.face, it.pub_time)) }
            val content = current.modules.module_dynamic
            val major = content?.major
            val opus = major?.opus
            if (!opus?.title.isNullOrBlank()) add(ShareImageBlock.Text(opus?.title.orEmpty(), heading = true))
            if (!opus?.contentBlocks.isNullOrEmpty()) {
                opus?.contentBlocks.orEmpty().forEach { block ->
                    when (block) {
                        is OpusContentBlock.Image -> add(ShareImageBlock.Image(block.pic.url))
                        is OpusContentBlock.Text -> add(ShareImageBlock.Text(block.text, emotes = block.richTextNodes.shareEmotes()))
                        is OpusContentBlock.LinkCard -> {
                            add(ShareImageBlock.Text(block.card.title, heading = true))
                            if (block.card.cover.isNotBlank()) add(ShareImageBlock.Image(block.card.cover))
                            if (block.card.description.isNotBlank()) add(ShareImageBlock.Text(block.card.description))
                        }
                        is OpusContentBlock.Divider -> add(ShareImageBlock.Text("────────────────"))
                        else -> add(ShareImageBlock.Text(block.plainText, heading = block is OpusContentBlock.Heading))
                    }
                }
            } else {
                val desc = content?.desc
                val body = desc?.text.orEmpty().ifBlank { opus?.summary?.text.orEmpty() }
                if (body.isNotBlank()) add(ShareImageBlock.Text(body, emotes = (desc?.rich_text_nodes.orEmpty() + opus?.summary?.rich_text_nodes.orEmpty()).shareEmotes()))
                val pics = major?.draw?.items.orEmpty().map { it.src } + opus?.pics.orEmpty().map { it.url }
                pics.distinct().forEach { add(ShareImageBlock.Image(it)) }
            }
            fun card(title: String, cover: String, description: String = "") {
                if (title.isNotBlank()) add(ShareImageBlock.Text(title, heading = true))
                if (cover.isNotBlank()) add(ShareImageBlock.Image(cover))
                if (description.isNotBlank()) add(ShareImageBlock.Text(description))
            }
            (major?.archive ?: major?.pgc)?.let { card(it.title, it.cover, it.desc) }
            major?.article?.let { card(it.title, it.covers.firstOrNull().orEmpty(), it.desc) }
            major?.ugc_season?.let { card(it.title, it.cover, it.intro.ifBlank { it.desc }) }
            major?.medialist?.let { card(it.title, it.cover, it.sub_title) }
            major?.music?.let { card(it.title, it.cover, it.label) }
            major?.common?.let { card(it.title, it.cover, it.desc) }
            major?.courses?.let { card(it.title, it.cover, it.desc) }
            major?.live?.let { card(it.title, it.cover, it.desc_first) }
            major?.live_rcmd?.let { live ->
                runCatching { kotlinx.serialization.json.Json.parseToJsonElement(live.content) as? kotlinx.serialization.json.JsonObject }.getOrNull()?.let { root ->
                    val info = root["live_play_info"] as? kotlinx.serialization.json.JsonObject ?: root
                    fun value(key: String) = (info[key] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    card(value("title"), value("cover"), value("uname"))
                }
            }
            major?.none?.let { add(ShareImageBlock.Text(it.tips)) }
            current.orig?.let { add(ShareImageBlock.Text("转发内容", heading = true)); appendDynamic(it) }
        }
        appendDynamic(item)
    },
)
