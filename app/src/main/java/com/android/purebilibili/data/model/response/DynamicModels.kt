// 文件路径: data/model/response/DynamicModels.kt
package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 *  动态页面数据模型
 * API: x/polymer/web-dynamic/v1/feed/all
 */

// --- 顶层响应 ---
@Serializable
data class DynamicFeedResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DynamicFeedData? = null
)

@Serializable
data class DynamicFeedData(
    val items: List<DynamicItem> = emptyList(),
    val offset: String = "", // 分页偏移量
    val has_more: Boolean = false,
    val update_baseline: String = "",
    val update_num: Int = 0
)

@Serializable
data class DynamicDetailResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DynamicDetailData? = null
)

@Serializable
data class DynamicDetailData(
    val item: DynamicItem? = null
)

@Serializable
data class TopicDetailResponse(
    val code: Int = 0,
    val message: String = "",
    val data: TopicDetailData? = null
)

@Serializable
data class TopicDetailData(
    @SerialName("top_details")
    val topDetails: TopicTopDetails? = null
)

@Serializable
data class TopicTopDetails(
    @SerialName("topic_creator")
    val topicCreator: TopicCreator? = null,
    @SerialName("topic_item")
    val topicItem: TopicItem? = null
)

@Serializable
data class TopicCreator(
    val uid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class TopicItem(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    @SerialName("share_pic")
    val sharePic: String = "",
    val view: Long = 0,
    val discuss: Long = 0,
    val dynamics: Long = 0,
    val fav: Long = 0,
    val like: Long = 0,
    val share: Long = 0,
    @SerialName("share_url")
    val shareUrl: String = "",
    @SerialName("jump_url")
    val jumpUrl: String = ""
)

@Serializable
data class TopicFeedResponse(
    val code: Int = 0,
    val message: String = "",
    val data: TopicFeedData? = null
)

@Serializable
data class TopicFeedData(
    @SerialName("topic_card_list")
    val topicCardList: TopicCardList? = null
)

@Serializable
data class TopicCardList(
    @SerialName("has_more")
    val hasMore: Boolean = false,
    val offset: String = "",
    val items: List<TopicDynamicCardItem> = emptyList()
)

@Serializable
data class TopicDynamicCardItem(
    @SerialName("dynamic_card_item")
    val dynamicCardItem: DynamicItem? = null,
    @SerialName("topic_type")
    val topicType: String = ""
)

// --- 动态卡片 ---
@Serializable
data class DynamicItem(
    val id_str: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val type: String = "", // DYNAMIC_TYPE_AV, DYNAMIC_TYPE_DRAW, DYNAMIC_TYPE_WORD, DYNAMIC_TYPE_FORWARD；opus/detail 可能返回数字
    val visible: Boolean = true,
    @Serializable(with = DynamicModulesFlexibleSerializer::class)
    val modules: DynamicModules = DynamicModules(),
    val orig: DynamicItem? = null,  //  转发动态的原始内容
    val basic: DynamicBasic? = null  //  [新增] 评论区参数
)

object DynamicModulesFlexibleSerializer : KSerializer<DynamicModules> {
    override val descriptor: SerialDescriptor = DynamicModules.serializer().descriptor

    override fun deserialize(decoder: Decoder): DynamicModules {
        val jsonDecoder = decoder as? JsonDecoder ?: return DynamicModules.serializer().deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(DynamicModules.serializer(), element)
            is JsonArray -> {
                var merged = DynamicModules()
                var opusTitle: String? = null
                val opusContentBlocks = mutableListOf<OpusContentBlock>()
                element.forEach { node ->
                    val obj = node as? JsonObject ?: return@forEach
                    val parsed = jsonDecoder.json.decodeFromJsonElement(DynamicModules.serializer(), obj)
                    merged = merged.copy(
                        module_author = parsed.module_author ?: merged.module_author,
                        module_dynamic = parsed.module_dynamic ?: merged.module_dynamic,
                        module_more = parsed.module_more ?: merged.module_more,
                        module_stat = parsed.module_stat ?: merged.module_stat
                    )

                    val moduleType = obj["module_type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (moduleType == "MODULE_TYPE_TITLE" || obj["module_title"] != null) {
                        opusTitle = obj["module_title"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    }
                    if (moduleType == "MODULE_TYPE_CONTENT" || obj["module_content"] != null) {
                        val paragraphs = obj["module_content"]?.jsonObject?.get("paragraphs") as? JsonArray
                        paragraphs?.forEach { paragraphNode ->
                            val paragraph = paragraphNode as? JsonObject ?: return@forEach
                            opusContentBlocks += extractParagraphBlocks(paragraph)
                        }
                    }
                }
                merged.normalizeWithOpusModules(
                    title = opusTitle,
                    contentBlocks = opusContentBlocks
                )
            }
            else -> DynamicModules()
        }
    }

    override fun serialize(encoder: Encoder, value: DynamicModules) {
        DynamicModules.serializer().serialize(encoder, value)
    }

    private fun extractParagraphBlocks(paragraph: JsonObject): List<OpusContentBlock> {
        val blocks = mutableListOf<OpusContentBlock>()
        extractParagraphText(paragraph)?.let { blocks += OpusContentBlock.Text(it) }
        extractParagraphPics(paragraph).forEach { pic ->
            blocks += OpusContentBlock.Image(pic)
        }
        extractParagraphLinkCard(paragraph)?.let { blocks += OpusContentBlock.LinkCard(it) }
        return blocks
    }

    private fun extractParagraphText(paragraph: JsonObject): String? {
        val nodes = paragraph["text"]?.jsonObject?.get("nodes") as? JsonArray ?: return null
        val text = buildString {
            nodes.forEach { node ->
                val nodeObject = node as? JsonObject ?: return@forEach
                val words = nodeObject["word"]
                    ?.jsonObject
                    ?.get("words")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: nodeObject["rich"]
                        ?.jsonObject
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.contentOrNull
                    ?: nodeObject["rich"]
                        ?.jsonObject
                        ?.get("orig_text")
                        ?.jsonPrimitive
                        ?.contentOrNull
                    ?: return@forEach
                append(words)
            }
        }.trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun extractParagraphPics(paragraph: JsonObject): List<OpusPic> {
        val results = mutableListOf<OpusPic>()
        val picObject = paragraph["pic"]?.let { runCatching { it.jsonObject }.getOrNull() }
        val pics = picObject?.get("pics") as? JsonArray
        pics?.mapNotNullTo(results) { picNode ->
            val pic = picNode as? JsonObject ?: return@mapNotNullTo null
            parseOpusPic(pic)
        }
        if (results.isEmpty()) {
            parseOpusPic(picObject)?.let(results::add)
        }
        paragraph["line"]
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.get("pic")
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.let(::parseOpusPic)
            ?.let(results::add)
        return results
    }

    private fun parseOpusPic(pic: JsonObject?): OpusPic? {
        if (pic == null) return null
        val url = pic["url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (url.isEmpty()) return null
        return OpusPic(
            url = normalizeOpusImageUrl(url),
            width = pic["width"]?.jsonPrimitive?.intOrNull ?: 0,
            height = pic["height"]?.jsonPrimitive?.intOrNull ?: 0,
            size = pic["size"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        )
    }

    private fun extractParagraphLinkCard(paragraph: JsonObject): OpusLinkCard? {
        val card = paragraph["link_card"]
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.get("card")
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?: return null
        val type = card.stringValue("type")
        if (type.isBlank()) return null
        return when (type) {
            "LINK_CARD_TYPE_UGC" -> parseUgcLinkCard(card, type)
            "LINK_CARD_TYPE_COMMON" -> parseCommonLinkCard(card, type)
            "LINK_CARD_TYPE_LIVE" -> parseLiveLinkCard(card, type)
            "LINK_CARD_TYPE_OPUS" -> parseOpusLinkCard(card, type)
            "LINK_CARD_TYPE_MUSIC" -> parseMusicLinkCard(card, type)
            "LINK_CARD_TYPE_GOODS" -> parseGoodsLinkCard(card, type)
            "LINK_CARD_TYPE_VOTE" -> parseVoteLinkCard(card, type)
            "LINK_CARD_TYPE_ITEM_NULL" -> parseItemNullLinkCard(card, type)
            else -> parseGenericLinkCard(card, type)
        }.takeIf { it.title.isNotBlank() || it.cover.isNotBlank() || it.jumpUrl.isNotBlank() }
    }

    private fun parseUgcLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val ugc = card.objectValue("ugc")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { ugc.stringValue("id_str") },
            title = ugc.stringValue("title"),
            description = ugc.stringValue("desc_second"),
            label = ugc.stringValue("head_text"),
            cover = normalizeOptionalOpusImageUrl(ugc.stringValue("cover")),
            jumpUrl = ugc.stringValue("jump_url")
        )
    }

    private fun parseCommonLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val common = card.objectValue("common")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { common.stringValue("id_str") },
            title = common.stringValue("title"),
            description = listOf(
                common.stringValue("desc"),
                common.stringValue("desc1"),
                common.stringValue("desc2")
            ).filter { it.isNotBlank() }.joinToString("\n"),
            label = common.stringValue("head_text"),
            cover = normalizeOptionalOpusImageUrl(common.stringValue("cover")),
            jumpUrl = common.stringValue("jump_url")
        )
    }

    private fun parseLiveLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val live = card.objectValue("live")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { live.stringValue("id") },
            title = live.stringValue("title"),
            description = listOf(
                live.stringValue("desc_first"),
                live.stringValue("desc_second")
            ).filter { it.isNotBlank() }.joinToString("\n"),
            label = live.stringValue("badge_text"),
            cover = normalizeOptionalOpusImageUrl(live.stringValue("cover")),
            jumpUrl = live.stringValue("jump_url")
        )
    }

    private fun parseOpusLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val opus = card.objectValue("opus")
        val authorName = opus.objectValue("author").stringValue("name")
        val statView = opus.objectValue("stat").stringValue("view")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid"),
            title = opus.stringValue("title"),
            description = listOf(
                authorName,
                statView.takeIf { it.isNotBlank() }?.let { "${it}阅读" }.orEmpty()
            ).filter { it.isNotBlank() }.joinToString(" · "),
            cover = normalizeOptionalOpusImageUrl(opus.stringValue("cover")),
            jumpUrl = opus.stringValue("jump_url")
        )
    }

    private fun parseMusicLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val music = card.objectValue("music")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { music.stringValue("id") },
            title = music.stringValue("title"),
            description = music.stringValue("label"),
            cover = normalizeOptionalOpusImageUrl(music.stringValue("cover")),
            jumpUrl = music.stringValue("jump_url")
        )
    }

    private fun parseGoodsLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val goods = card.objectValue("goods")
        val firstItem = goods?.get("items")
            ?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.firstOrNull()
            ?.let { runCatching { it.jsonObject }.getOrNull() }
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { firstItem.stringValue("id") },
            title = firstItem.stringValue("name").ifBlank { goods.stringValue("head_text") },
            description = firstItem.stringValue("price").ifBlank { firstItem.stringValue("brief") },
            label = goods.stringValue("head_text"),
            badgeText = firstItem.stringValue("jump_desc"),
            cover = normalizeOptionalOpusImageUrl(firstItem.stringValue("cover")),
            jumpUrl = firstItem.stringValue("jump_url").ifBlank { goods.stringValue("jump_url") }
        )
    }

    private fun parseVoteLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val vote = card.objectValue("vote")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid").ifBlank { vote.stringValue("vote_id") },
            title = vote.stringValue("title").ifBlank { vote.stringValue("desc") }.ifBlank { "投票" },
            description = vote.stringValue("desc"),
            jumpUrl = vote.stringValue("jump_url")
        )
    }

    private fun parseItemNullLinkCard(card: JsonObject, type: String): OpusLinkCard {
        val itemNull = card.objectValue("item_null")
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid"),
            title = itemNull.stringValue("text").ifBlank { "内容已失效" },
            cover = normalizeOptionalOpusImageUrl(itemNull.stringValue("icon"))
        )
    }

    private fun parseGenericLinkCard(card: JsonObject, type: String): OpusLinkCard {
        return OpusLinkCard(
            type = type,
            oid = card.stringValue("oid"),
            title = card.stringValue("title"),
            description = card.stringValue("desc"),
            cover = normalizeOptionalOpusImageUrl(card.stringValue("cover")),
            jumpUrl = card.stringValue("jump_url")
        )
    }

    private fun JsonObject?.stringValue(key: String): String {
        if (this == null) return ""
        return get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    }

    private fun JsonObject?.objectValue(key: String): JsonObject? {
        if (this == null) return null
        return get(key)?.let { runCatching { it.jsonObject }.getOrNull() }
    }

    private fun normalizeOptionalOpusImageUrl(rawUrl: String): String {
        return rawUrl.takeIf { it.isNotBlank() }?.let(::normalizeOpusImageUrl).orEmpty()
    }

    private fun normalizeOpusImageUrl(rawUrl: String): String {
        return when {
            rawUrl.startsWith("//") -> "https:$rawUrl"
            rawUrl.startsWith("http://") -> rawUrl.replaceFirst("http://", "https://")
            else -> rawUrl
        }
    }

    private fun DynamicModules.normalizeWithOpusModules(
        title: String?,
        contentBlocks: List<OpusContentBlock>
    ): DynamicModules {
        val existing = module_dynamic
        val paragraphTexts = contentBlocks.mapNotNull { block ->
            (block as? OpusContentBlock.Text)?.text
        }
        val pics = contentBlocks.mapNotNull { block ->
            (block as? OpusContentBlock.Image)?.pic
        }
        val descText = paragraphTexts.joinToString(separator = "\n").trim()
        val cleanTitle = title?.trim().takeUnless { it.isNullOrBlank() }
        val hasDerivedContent = descText.isNotBlank() || pics.isNotEmpty() || cleanTitle != null
        if (!hasDerivedContent) return this

        val existingDesc = existing?.desc
        val existingDescText = existingDesc?.text.orEmpty()
        val mergedDescText = when {
            descText.isBlank() -> existingDescText
            existingDescText.isBlank() -> descText
            descText.length > existingDescText.length -> descText
            else -> existingDescText
        }
        val mergedDesc = if (mergedDescText.isNotBlank()) {
            DynamicDesc(
                text = mergedDescText,
                rich_text_nodes = if (mergedDescText == existingDescText) {
                    existingDesc?.rich_text_nodes.orEmpty()
                } else {
                    emptyList()
                }
            )
        } else {
            null
        }

        val existingMajor = existing?.major
        val existingOpus = existingMajor?.opus
        val mergedOpus = OpusMajor(
            jump_url = existingOpus?.jump_url.orEmpty(),
            title = cleanTitle ?: existingOpus?.title,
            summary = when {
                mergedDescText.isNotBlank() -> OpusSummary(text = mergedDescText)
                existingOpus?.summary != null -> existingOpus.summary
                else -> null
            },
            pics = if (pics.isNotEmpty()) pics else existingOpus?.pics.orEmpty(),
            contentBlocks = if (contentBlocks.isNotEmpty()) contentBlocks else existingOpus?.contentBlocks.orEmpty()
        )
        val mergedMajor = DynamicMajor(
            type = "MAJOR_TYPE_OPUS",
            archive = existingMajor?.archive,
            article = existingMajor?.article,
            draw = existingMajor?.draw,
            live_rcmd = existingMajor?.live_rcmd,
            opus = mergedOpus,
            ugc_season = existingMajor?.ugc_season
        )
        return copy(
            module_dynamic = DynamicContentModule(
                desc = mergedDesc ?: existingDesc,
                major = if (existingMajor == null || existingMajor.type.isBlank() || existingMajor.type == "MAJOR_TYPE_OPUS") {
                    mergedMajor
                } else {
                    existingMajor
                }
            )
        )
    }

}

//  [新增] 动态基础信息 - 包含评论区参数
@Serializable
data class DynamicBasic(
    val comment_id_str: String = "",   // 评论区 oid
    val comment_type: Int = 0,         // 评论区 type (1=视频, 11=图片, 17=动态)
    val rid_str: String = ""           // 资源 id
)

// --- 动态模块集合 ---
@Serializable
data class DynamicModules(
    val module_author: DynamicAuthorModule? = null,
    val module_dynamic: DynamicContentModule? = null,
    val module_more: DynamicMoreModule? = null,
    val module_stat: DynamicStatModule? = null
)

@Serializable
data class DynamicMoreModule(
    val three_point_items: List<DynamicThreePointItem> = emptyList()
)

@Serializable
data class DynamicThreePointItem(
    val label: String = "",
    val modal: DynamicThreePointModal? = null,
    val params: DynamicThreePointParams? = null,
    val type: String = ""
)

@Serializable
data class DynamicThreePointModal(
    val cancel: String = "",
    val confirm: String = "",
    val content: String = "",
    val title: String = ""
)

@Serializable
data class DynamicThreePointParams(
    val dyn_id_str: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val dyn_type: Int = 0,
    val rid_str: String = ""
)

// --- 作者模块 ---
@Serializable
data class DynamicAuthorModule(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
    val pub_time: String = "", // "昨天 18:00"
    val pub_ts: Long = 0, // 时间戳
    val following: Boolean? = null,
    val official_verify: DynamicOfficialVerify? = null,
    val vip: DynamicVipInfo? = null,
    val decorate: DecorateInfo? = null
)

@Serializable
data class DynamicOfficialVerify(
    val type: Int = -1, // 0: 个人认证, 1: 机构认证, -1: 无
    val desc: String = ""
)

@Serializable
data class DynamicVipInfo(
    val type: Int = 0, // 0: 无, 1: 月度, 2: 年度
    val status: Int = 0,
    val nickname_color: String = "" // "#FB7299"
)

@Serializable
data class DecorateInfo(
    val card_url: String = "", // 装扮卡片 URL
    val name: String = ""
)

// --- 内容模块 ---
@Serializable
data class DynamicContentModule(
    val desc: DynamicDesc? = null,
    val major: DynamicMajor? = null
)

@Serializable
data class DynamicDesc(
    val text: String = "", // 动态文字内容
    val rich_text_nodes: List<RichTextNode> = emptyList()
)

@Serializable
data class RichTextNode(
    val type: String = "", // TEXT, EMOJI, AT, TOPIC
    val text: String = "",
    val emoji: EmojiInfo? = null,
    val jump_url: String? = null
)

@Serializable
data class EmojiInfo(
    val icon_url: String = "",
    val size: Int = 1,
    val text: String = ""
)

// --- 主要内容 (视频/图片/直播/图文) ---
@Serializable
data class DynamicMajor(
    val type: String = "", // MAJOR_TYPE_ARCHIVE, MAJOR_TYPE_DRAW, MAJOR_TYPE_LIVE_RCMD, MAJOR_TYPE_OPUS, MAJOR_TYPE_NONE
    val archive: ArchiveMajor? = null, // 视频
    val pgc: ArchiveMajor? = null, // 番剧/影视
    val article: ArticleMajor? = null, // 专栏
    val draw: DrawMajor? = null, // 图片
    val live_rcmd: LiveRcmdMajor? = null, //  直播
    val opus: OpusMajor? = null, //  [新增] 图文动态 (新版格式)
    val ugc_season: UgcSeasonMajor? = null // [新增] 合集
)

//  [新增] 图文动态 (MAJOR_TYPE_OPUS) - B站新版图文格式
@Serializable
data class OpusMajor(
    val jump_url: String = "",
    val pics: List<OpusPic> = emptyList(), // 图片列表
    val summary: OpusSummary? = null, // 文字摘要
    val title: String? = null, // 标题 (可选)
    @Transient
    val contentBlocks: List<OpusContentBlock> = emptyList()
)

sealed interface OpusContentBlock {
    data class Text(val text: String) : OpusContentBlock
    data class Image(val pic: OpusPic) : OpusContentBlock
    data class LinkCard(val card: OpusLinkCard) : OpusContentBlock
}

@Serializable
data class OpusLinkCard(
    val type: String = "",
    val oid: String = "",
    val title: String = "",
    val description: String = "",
    val label: String = "",
    val cover: String = "",
    val jumpUrl: String = "",
    val badgeText: String = ""
)

@Serializable
data class OpusPic(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Double = 0.0
)

@Serializable
data class OpusSummary(
    val text: String = "",
    val rich_text_nodes: List<RichTextNode> = emptyList()
)

//  直播推荐
@Serializable
data class LiveRcmdMajor(
    val content: String = "" // JSON string，需要解析
)

//  [新增] 合集/剧集 (MAJOR_TYPE_UGC_SEASON)
@Serializable
data class UgcSeasonMajor(
    @Serializable(with = FlexibleLongSerializer::class)
    val aid: Long = 0,
    val title: String = "",
    val cover: String = "",
    val desc: String = "",
    val duration_text: String = "",
    val jump_url: String = "",
    val intro: String = "",
    val id: Long = 0, // season_id
    val sign_state: Int = 0,
    val type: Int = 0, // 1=合集
    val stat: UgcSeasonStat = UgcSeasonStat(),
    val archive: ArchiveMajor? = null // 播放第一集或最新一集
)

@Serializable
data class UgcSeasonStat(
    val play: String = "0",
    val danmaku: String = "0"
)

@Serializable
data class ArchiveMajor(
    val aid: String = "",
    val bvid: String = "",
    val title: String = "",
    val cover: String = "",
    val desc: String = "",
    val duration_text: String = "", // "10:24"
    val stat: ArchiveStat = ArchiveStat(),
    val jump_url: String = "",
    val badge: DynamicMajorBadge? = null,
    @SerialName("is_charging_arc")
    val isChargingArc: Boolean = false,
    @SerialName("elec_arc_type")
    val elecArcType: Int = 0,
    @SerialName("is_ugcpay")
    val isUgcpay: Boolean = false,
    @SerialName("ugc_pay")
    val ugcPay: Int = 0,
    @SerialName("ugc_pay_preview")
    val ugcPayPreview: Int = 0,
    @Serializable(with = FlexibleLongSerializer::class)
    val epid: Long = 0,
    @Serializable(with = FlexibleLongSerializer::class)
    val season_id: Long = 0
)

@Serializable
data class DynamicMajorBadge(
    val text: String = "",
    val color: String = "",
    @SerialName("bg_color")
    val bgColor: String = ""
)

@Serializable
data class ArchiveStat(
    val play: String = "0", // "123.4万"
    val danmaku: String = "0"
)

@Serializable
data class DrawMajor(
    val id: Long = 0,
    val items: List<DrawItem> = emptyList()
)

@Serializable
data class DrawItem(
    val src: String = "", // 图片 URL
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class ArticleMajor(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0,
    val title: String = "",
    val desc: String = "",
    val covers: List<String> = emptyList(),
    val jump_url: String = "",
    val label: String = ""
)

// --- 统计模块 ---
@Serializable
data class DynamicStatModule(
    val comment: StatItem = StatItem(),
    val forward: StatItem = StatItem(),
    val like: StatItem = StatItem()
)

@Serializable
data class StatItem(
    val count: Int = 0,
    val forbidden: Boolean = false
)

// --- 动态类型枚举 ---
enum class DynamicType(val apiValue: String) {
    VIDEO("DYNAMIC_TYPE_AV"),
    PGC("DYNAMIC_TYPE_PGC"),
    DRAW("DYNAMIC_TYPE_DRAW"),
    WORD("DYNAMIC_TYPE_WORD"),
    FORWARD("DYNAMIC_TYPE_FORWARD"),
    LIVE("DYNAMIC_TYPE_LIVE_RCMD"),
    OPUS("DYNAMIC_TYPE_DRAW"),  //  [新增] 图文动态 (使用 DRAW 类型，但 major 为 opus)
    UGC_SEASON("DYNAMIC_TYPE_UGC_SEASON"), // [新增] 合集/剧集
    UNKNOWN("UNKNOWN");
    
    companion object {
        fun fromApiValue(value: String): DynamicType {
            return entries.find { it.apiValue == value } ?: UNKNOWN
        }
    }
}
