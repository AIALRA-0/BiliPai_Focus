package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import com.android.purebilibili.data.model.response.OpusSummary
import com.android.purebilibili.data.model.response.RichTextNode
import com.android.purebilibili.feature.article.ArticleContentBlock
import com.android.purebilibili.feature.article.scoreOpusContentBlocks

internal fun shouldFetchStandardDetailForPlainTextDynamic(item: DynamicItem): Boolean {
    val content = item.modules.module_dynamic ?: return false
    return item.type == "DYNAMIC_TYPE_WORD" && content.desc?.text?.isNotBlank() == true
}

internal fun mergeDynamicDetailWithLongerDesc(
    desktopItem: DynamicItem,
    standardItem: DynamicItem,
): DynamicItem {
    val desktopContent = desktopItem.modules.module_dynamic ?: return desktopItem
    val standardDesc = standardItem.modules.module_dynamic?.desc ?: return desktopItem
    val desktopDesc = desktopContent.desc
    if (standardDesc.text.length <= desktopDesc?.text?.length ?: 0) return desktopItem

    return desktopItem.copy(
        modules = desktopItem.modules.copy(
            module_dynamic = desktopContent.copy(desc = standardDesc)
        )
    )
}

internal fun isFoldedDynamicLinkPlaceholder(text: String): Boolean {
    return text.trim() == "网页链接"
}

internal fun shouldFallbackForDynamicDetail(item: DynamicItem): Boolean {
    val modules = item.modules
    val descText = modules.module_dynamic?.desc?.text.orEmpty()
    val hasDescText = descText.isNotBlank() && !isFoldedDynamicLinkPlaceholder(descText)
    val hasMajorContent = modules.module_dynamic?.major != null
    val hasOrig = item.orig != null

    // 可渲染内容都没有时，说明解析结构可能不兼容，应该走 fallback
    val hasRenderableContent = hasDescText || hasMajorContent || hasOrig
    return !hasRenderableContent
}

internal fun shouldFetchDynamicDetailByRid(
    current: DynamicItem?,
    rid: String
): Boolean {
    val cleanedRid = rid.trim()
    if (cleanedRid.isEmpty()) return false
    return current == null || shouldFallbackForDynamicDetail(current)
}

internal fun resolvePreferredDynamicDetailItem(
    candidates: List<DynamicItem>
): DynamicItem? {
    val renderable = candidates.filter { !shouldFallbackForDynamicDetail(it) }
    if (renderable.isEmpty()) return candidates.firstOrNull()
    return renderable.maxByOrNull(::resolveDynamicOpusContentScore) ?: renderable.first()
}

internal fun resolveDynamicOpusContentScore(item: DynamicItem): Int {
    val opus = item.modules.module_dynamic?.major?.opus ?: return 0
    val blockScore = scoreOpusContentBlocks(opus.contentBlocks)
    if (blockScore > 0) {
        return 100_000 + blockScore
    }
    return opus.pics.size + (opus.summary?.text?.length ?: 0)
}

internal fun mergeRicherOpusDetailContent(
    base: DynamicItem,
    candidates: List<DynamicItem>
): DynamicItem {
    val richest = candidates.maxByOrNull(::resolveDynamicOpusContentScore) ?: return base
    val richestOpus = if (
        resolveDynamicOpusContentScore(richest) > resolveDynamicOpusContentScore(base)
    ) {
        richest.modules.module_dynamic?.major?.opus
    } else {
        base.modules.module_dynamic?.major?.opus
            ?: richest.modules.module_dynamic?.major?.opus
    } ?: return base
    val baseContent = base.modules.module_dynamic ?: return richest
    val baseMajor = baseContent.major
    val baseOpus = baseMajor?.opus
    // Detail and feed responses split rich content across payloads: the opus
    // response may contain the article blocks while the feed seed contains the
    // actual preview pictures. Keep the union so navigating into detail cannot
    // accidentally drop images just because the richer candidate scored higher.
    val candidateItems = candidates.asSequence()
        .flatMap { item -> sequence {
            yield(item)
            item.orig?.let { yield(it) }
        } }
        .toList()
    val richestPics = candidateItems.asSequence()
        .mapNotNull { it.modules.module_dynamic?.major?.opus }
        .flatMap { it.pics.asSequence() }
        .filter { it.url.isNotBlank() }
        .distinctBy { it.url }
        .toList()
    // The desktop/feed API documents legacy image dynamics under
    // `major.draw.items`, while the detail/opus API may return the same
    // dynamic as an opus payload. Preserve those preview images when the
    // preferred candidate switches representation.
    val drawPics = candidateItems.asSequence()
        .mapNotNull { it.modules.module_dynamic?.major?.draw }
        .flatMap { it.items.asSequence() }
        .filter { it.src.isNotBlank() }
        .map { OpusPic(url = it.src, width = it.width, height = it.height) }
        .toList()
    val mergedPics = (richestPics + drawPics)
        .distinctBy { it.url }
    val candidateEmojiNodes = candidateItems.asSequence()
        .flatMap { item ->
            val content = item.modules.module_dynamic
            val descNodes = content?.desc?.rich_text_nodes.orEmpty()
            val summaryNodes = content?.major?.opus?.summary?.rich_text_nodes.orEmpty()
            val blockNodes = content?.major?.opus?.contentBlocks.orEmpty().flatMap { block ->
                when (block) {
                    is OpusContentBlock.Text -> block.richTextNodes
                    else -> emptyList()
                }
            }
            (descNodes + summaryNodes + blockNodes).asSequence()
        }
        .filter(::containsDynamicRichTextMetadata)
        .distinctBy(::dynamicEmojiMetadataKey)
        .toList()
    val mergedSummary = if (candidateEmojiNodes.isNotEmpty()) {
        val s = if ((richestOpus.summary?.text?.length ?: 0) >= (baseOpus?.summary?.text?.length ?: 0)) {
            richestOpus.summary
        } else {
            baseOpus?.summary
        }
        if (s != null) {
            s.copy(
                rich_text_nodes = mergeDynamicDetailRichTextNodes(
                    detailNodes = s.rich_text_nodes,
                    seedEmojiNodes = candidateEmojiNodes,
                )
            )
        } else {
            OpusSummary(rich_text_nodes = candidateEmojiNodes)
        }
    } else {
        if ((richestOpus.summary?.text?.length ?: 0) >= (baseOpus?.summary?.text?.length ?: 0)) {
            richestOpus.summary
        } else {
            baseOpus?.summary
        }
    }
    val mergedDesc = if (candidateEmojiNodes.isNotEmpty()) {
        val existingDescNodes = (baseContent.desc?.rich_text_nodes).orEmpty()
        baseContent.desc?.copy(
            rich_text_nodes = mergeDynamicDetailRichTextNodes(
                detailNodes = existingDescNodes,
                seedEmojiNodes = candidateEmojiNodes,
            )
        ) ?: DynamicDesc(rich_text_nodes = candidateEmojiNodes)
    } else {
        baseContent.desc
    }
    val mergedOpus = OpusMajor(
        jump_url = baseOpus?.jump_url?.takeIf { it.isNotBlank() } ?: richestOpus.jump_url,
        pics = mergedPics.ifEmpty { baseOpus?.pics.orEmpty() },
        summary = mergedSummary,
        title = richestOpus.title?.takeIf { it.isNotBlank() } ?: baseOpus?.title,
        contentBlocks = if (richestOpus.contentBlocks.size >= (baseOpus?.contentBlocks?.size ?: 0)) {
            richestOpus.contentBlocks
        } else {
            baseOpus?.contentBlocks.orEmpty()
        }
    )
    val mergedMajor = (baseMajor ?: DynamicMajor(
        type = "MAJOR_TYPE_OPUS"
    )).copy(
        type = baseMajor?.type?.takeIf { it.isNotBlank() } ?: "MAJOR_TYPE_OPUS",
        opus = mergedOpus
    )
    return base.copy(
        modules = base.modules.copy(
            module_dynamic = baseContent.copy(desc = mergedDesc, major = mergedMajor)
        )
    )
}

/** Retains feed-defined interaction and rich-text metadata when detail/opus responses omit it. */
internal fun mergeDynamicDetailInteractionMetadata(
    detailItem: DynamicItem,
    seedItem: DynamicItem?
): DynamicItem {
    if (seedItem == null) return detailItem
    val detailBasic = detailItem.basic?.takeIf {
        it.comment_type > 0 && it.comment_id_str.toLongOrNull()?.let { oid -> oid > 0L } == true
    }
    val seedBasic = seedItem.basic?.takeIf {
        it.comment_type > 0 && it.comment_id_str.toLongOrNull()?.let { oid -> oid > 0L } == true
    }
    val seedContent = seedItem.modules.module_dynamic
    val contentWithSeedMedia = mergeDynamicDetailContentWithSeedMedia(
        detailContent = detailItem.modules.module_dynamic,
        seedContent = seedContent,
    )
    val detailContent = when {
        contentWithSeedMedia == null -> seedContent
        contentWithSeedMedia.topic == null && seedContent?.topic != null -> {
            contentWithSeedMedia.copy(topic = seedContent.topic)
        }
        else -> contentWithSeedMedia
    }
    val seedEmojiNodes = collectDynamicDetailSeedEmojiNodes(seedItem)
    val mergedContent = if (detailContent != null && seedEmojiNodes.isNotEmpty()) {
        val mergedSummary = detailContent.major?.opus?.summary?.let { summary ->
            summary.copy(
                rich_text_nodes = mergeDynamicDetailRichTextNodes(
                    detailNodes = summary.rich_text_nodes,
                    seedEmojiNodes = seedEmojiNodes,
                )
            )
        }
        val mergedMajor = if (mergedSummary != null) {
            detailContent.major?.copy(
                opus = detailContent.major.opus?.copy(summary = mergedSummary)
            )
        } else {
            detailContent.major
        }
        detailContent.copy(
            desc = detailContent.desc?.copy(
                rich_text_nodes = mergeDynamicDetailRichTextNodes(
                    detailNodes = detailContent.desc.rich_text_nodes,
                    seedEmojiNodes = seedEmojiNodes,
                )
            )
                ?: DynamicDesc(rich_text_nodes = seedEmojiNodes),
            major = mergedMajor ?: detailContent.major,
        )
    } else {
        detailContent
    }
    return detailItem.copy(
        basic = detailBasic ?: seedBasic ?: detailItem.basic,
        modules = detailItem.modules.copy(
            module_dynamic = mergedContent,
            module_stat = detailItem.modules.module_stat ?: seedItem.modules.module_stat,
        )
    )
}

internal fun mergeDynamicDetailContentWithSeedMedia(
    detailContent: DynamicContentModule?,
    seedContent: DynamicContentModule?,
): DynamicContentModule? {
    val seedMajor = seedContent?.major ?: return detailContent
    val seedPics = buildList {
        addAll(seedMajor.opus?.pics.orEmpty())
        seedMajor.draw?.items.orEmpty().mapTo(this) { item ->
            OpusPic(url = item.src, width = item.width, height = item.height)
        }
    }.filter { it.url.isNotBlank() }
        .distinctBy { it.url }
    if (seedPics.isEmpty()) return detailContent
    if (detailContent == null) return seedContent

    val detailMajor = detailContent.major
        ?: return detailContent.copy(major = seedMajor)
    val detailHasMedia = detailMajor.draw?.items?.any { it.src.isNotBlank() } == true ||
        detailMajor.opus?.pics?.any { it.url.isNotBlank() } == true ||
        detailMajor.opus?.contentBlocks?.any { block ->
            block is OpusContentBlock.Image && block.pic.url.isNotBlank()
        } == true
    if (detailHasMedia) return detailContent

    val mergedMajor = when {
        detailMajor.opus != null || detailMajor.type == "MAJOR_TYPE_OPUS" -> detailMajor.copy(
            opus = (detailMajor.opus ?: OpusMajor()).copy(pics = seedPics),
        )
        detailMajor.draw != null || detailMajor.type == "MAJOR_TYPE_DRAW" -> {
            seedMajor.draw?.let { seedDraw -> detailMajor.copy(draw = seedDraw) } ?: seedMajor
        }
        detailMajor.type.isBlank() || detailMajor.type == "MAJOR_TYPE_NONE" -> seedMajor
        else -> detailMajor
    }
    return detailContent.copy(major = mergedMajor)
}

/**
 * Dynamic feed payloads may expose emoji metadata in either `desc.rich_text_nodes` or
 * `major.opus.summary.rich_text_nodes`. The detail/opus body can retain only the shortcode,
 * so both documented preview sources must be carried into the full-body renderer.
 */
internal fun collectDynamicDetailSeedEmojiNodes(item: DynamicItem): List<RichTextNode> {
    val content = item.modules.module_dynamic ?: return emptyList()
    return (content.desc?.rich_text_nodes.orEmpty() +
        content.major?.opus?.summary?.rich_text_nodes.orEmpty())
        .filter(::containsDynamicRichTextMetadata)
        .distinctBy(::dynamicEmojiMetadataKey)
}

internal fun mergeDynamicDetailRichTextNodes(
    detailNodes: List<RichTextNode>,
    seedEmojiNodes: List<RichTextNode>,
): List<RichTextNode> {
    if (seedEmojiNodes.isEmpty()) return detailNodes
    val existingEmojiKeys = detailNodes
        .filter(::containsDynamicRichTextMetadata)
        .mapTo(mutableSetOf(), ::dynamicEmojiMetadataKey)
    return detailNodes + seedEmojiNodes
        .distinctBy(::dynamicEmojiMetadataKey)
        .filter { node -> dynamicEmojiMetadataKey(node) !in existingEmojiKeys }
}

private fun containsDynamicRichTextMetadata(node: RichTextNode): Boolean {
    val type = node.type.removePrefix("RICH_TEXT_NODE_TYPE_")
    return when {
        type.equals("AT", ignoreCase = true) -> node.rid?.toLongOrNull()?.let { it > 0L } == true
        type.equals("EMOJI", ignoreCase = true) -> node.emoji?.let { emoji ->
            emoji.icon_url.isNotBlank() || emoji.webp_url.isNotBlank() || emoji.gif_url.isNotBlank()
        } == true
        else -> false
    }
}

private fun dynamicEmojiMetadataKey(node: RichTextNode): String = sequenceOf(
    node.text,
    node.orig_text,
    node.emoji?.text.orEmpty(),
).map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()

internal fun shouldFetchOpusDetailForDynamicDetail(item: DynamicItem): Boolean {
    val major = item.modules.module_dynamic?.major
    if (major?.type == "MAJOR_TYPE_OPUS" || major?.opus != null) return true
    // Space/web 详情把长图文当成 DRAW 九宫格预览；正文在 opus/detail。
    if (major?.type == "MAJOR_TYPE_DRAW" || major?.draw != null) return true
    if (item.type.equals("DYNAMIC_TYPE_DRAW", ignoreCase = true)) return true
    return item.basic?.comment_type == 11
}

internal fun shouldRequestOpusDetailForDynamicDetail(
    webItem: DynamicItem?,
    seedItem: DynamicItem?,
): Boolean {
    return webItem == null ||
        shouldFallbackForDynamicDetail(webItem) ||
        shouldFetchOpusDetailForDynamicDetail(webItem) ||
        seedItem?.let(::shouldFetchOpusDetailForDynamicDetail) == true
}

internal fun resolveOpusArticleFallbackCvId(
    fallbackId: Long?,
    commentType: Int,
    commentIdStr: String
): Long? {
    val fallback = fallbackId?.takeIf { it > 0L }
    if (fallback != null) return fallback
    if (commentType != 12) return null
    return commentIdStr.toLongOrNull()?.takeIf { it > 0L }
}

internal fun articleContentBlocksToOpusBlocks(
    blocks: List<ArticleContentBlock>
): List<OpusContentBlock> {
    return blocks.map { block ->
        when (block) {
            is ArticleContentBlock.Heading -> OpusContentBlock.Heading(block.text)
            is ArticleContentBlock.Paragraph -> OpusContentBlock.Text(block.text)
            is ArticleContentBlock.Quote -> OpusContentBlock.Quote(block.text)
            is ArticleContentBlock.ListBlock -> OpusContentBlock.ListBlock(
                items = block.items,
                ordered = block.ordered,
            )
            is ArticleContentBlock.Code -> OpusContentBlock.Code(
                text = block.content,
                language = block.language,
            )
            is ArticleContentBlock.Image -> OpusContentBlock.Image(
                OpusPic(url = block.url, width = block.width, height = block.height)
            )
        }
    }
}

internal fun mergeArticleDetailIntoOpus(
    base: DynamicItem,
    title: String,
    blocks: List<ArticleContentBlock>
): DynamicItem {
    val opusBlocks = articleContentBlocksToOpusBlocks(blocks)
    if (opusBlocks.isEmpty()) return base
    return mergeRicherOpusDetailContent(
        base = base,
        candidates = listOf(
            base,
            DynamicItem(
                id_str = base.id_str,
                modules = DynamicModules(
                    module_dynamic = DynamicContentModule(
                        major = DynamicMajor(
                            type = "MAJOR_TYPE_OPUS",
                            opus = OpusMajor(
                                title = title.takeIf { it.isNotBlank() },
                                contentBlocks = opusBlocks,
                                pics = opusBlocks.mapNotNull { block ->
                                    (block as? OpusContentBlock.Image)?.pic
                                }
                            )
                        )
                    )
                )
            )
        )
    )
}
