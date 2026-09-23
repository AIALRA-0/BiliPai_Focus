package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.DynamicTopic
import com.android.purebilibili.data.model.response.DrawMajor
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.EmojiInfo
import com.android.purebilibili.data.model.response.RichTextNode
import com.android.purebilibili.data.model.response.StatItem
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import com.android.purebilibili.data.model.response.OpusSummary
import com.android.purebilibili.feature.article.ArticleContentBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DynamicDetailFallbackPolicyTest {

    @Test
    fun mergeRicherOpusDetailContent_retainsDesktopDrawPreviewImages() {
        val seed = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_DRAW",
                        draw = DrawMajor(items = listOf(DrawItem(src = "https://img.example/a.jpg")))
                    )
                )
            )
        )
        val detail = seed.copy(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(summary = OpusSummary(text = "body"))
                    )
                )
            )
        )
        val candidates = listOf(detail, seed)
        val resolved = requireNotNull(resolvePreferredDynamicDetailItem(candidates))
        val merged = mergeRicherOpusDetailContent(resolved, candidates)

        assertEquals(detail, resolved)
        assertEquals("https://img.example/a.jpg", merged.modules.module_dynamic?.major?.opus?.pics?.single()?.url)
    }

    @Test
    fun shouldFallback_returnsTrue_whenAuthorAndContentMissing() {
        val item = DynamicItem(modules = DynamicModules())
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsFalse_whenDescTextExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "text")
                )
            )
        )
        assertFalse(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsTrue_whenOnlyAuthorExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 1, name = "author")
            )
        )
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun standardDetail_isFetchedForPlainTextDynamicAndSuppliesLongerBody() {
        val desktopItem = DynamicItem(
            type = "DYNAMIC_TYPE_WORD",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "摘要"))
            )
        )
        val standardItem = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "这是动态的完整正文"))
            )
        )

        assertTrue(shouldFetchStandardDetailForPlainTextDynamic(desktopItem))
        assertEquals(
            "这是动态的完整正文",
            mergeDynamicDetailWithLongerDesc(desktopItem, standardItem)
                .modules.module_dynamic?.desc?.text
        )
    }

    @Test
    fun standardDetail_isNotFetchedForDynamicWithMajorContent() {
        val item = DynamicItem(
            type = "DYNAMIC_TYPE_AV",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "视频动态文案"),
                    major = DynamicMajor(type = "MAJOR_TYPE_ARCHIVE")
                )
            )
        )

        assertFalse(shouldFetchStandardDetailForPlainTextDynamic(item))
    }

    @Test
    fun shouldFetchOpusDetail_returnsTrueForPreviewOnlyOpusMajor() {
        val item = DynamicItem(
            id_str = "1201902028962398230",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "预览摘要"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(summary = OpusSummary(text = "预览摘要"))
                    )
                )
            )
        )

        assertTrue(shouldFetchOpusDetailForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsTrueForFoldedWebLinkPlaceholder() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "网页链接")
                )
            )
        )

        assertTrue(isFoldedDynamicLinkPlaceholder("网页链接"))
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFetchDynamicDetailByRid_whenCurrentItemIsUnrenderable() {
        assertTrue(
            shouldFetchDynamicDetailByRid(
                current = DynamicItem(modules = DynamicModules()),
                rid = " 998877 "
            )
        )
        assertFalse(
            shouldFetchDynamicDetailByRid(
                current = DynamicItem(
                    modules = DynamicModules(
                        module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "正文"))
                    )
                ),
                rid = "998877"
            )
        )
        assertFalse(shouldFetchDynamicDetailByRid(current = null, rid = " "))
    }

    @Test
    fun resolvePreferredDynamicDetailItem_prefersRenderableCandidate() {
        val empty = DynamicItem(id_str = "empty")
        val renderable = DynamicItem(
            id_str = "ok",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "完整正文"))
            )
        )

        assertEquals(
            "ok",
            resolvePreferredDynamicDetailItem(listOf(empty, renderable))?.id_str
        )
        assertEquals(empty, resolvePreferredDynamicDetailItem(listOf(empty)))
        assertEquals(null, resolvePreferredDynamicDetailItem(emptyList()))
    }

    @Test
    fun shouldFetchOpusDetail_returnsTrueForSpaceDrawPreview() {
        val item = DynamicItem(
            id_str = "1236527093179744277",
            type = "DYNAMIC_TYPE_DRAW",
            basic = DynamicBasic(comment_id_str = "405532534", comment_type = 11),
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "开门见山介绍combo"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_DRAW",
                        draw = com.android.purebilibili.data.model.response.DrawMajor(
                            id = 405532534L,
                            items = listOf(
                                com.android.purebilibili.data.model.response.DrawItem(
                                    src = "https://i0.hdslb.com/1.jpg"
                                )
                            )
                        )
                    )
                )
            )
        )

        assertTrue(shouldFetchOpusDetailForDynamicDetail(item))
    }

    @Test
    fun shouldFetchOpusDetail_returnsFalseForOrdinaryTextDynamic() {
        val item = DynamicItem(
            id_str = "987654321",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "普通动态正文")
                )
            )
        )

        assertFalse(shouldFetchOpusDetailForDynamicDetail(item))
    }

    @Test
    fun shouldRequestOpusDetail_usesImageSeedWhenWebDetailDropsMedia() {
        val webItem = DynamicItem(
            id_str = "1236527093179744277",
            type = "2",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "详情接口只剩文字")
                )
            )
        )
        val seedItem = DynamicItem(
            id_str = webItem.id_str,
            type = "DYNAMIC_TYPE_DRAW",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_DRAW",
                        draw = DrawMajor(
                            items = listOf(DrawItem(src = "https://img.example/seed.jpg"))
                        )
                    )
                )
            )
        )

        assertTrue(
            shouldRequestOpusDetailForDynamicDetail(
                webItem = webItem,
                seedItem = seedItem,
            )
        )
    }

    @Test
    fun mergeRicherOpusDetailContent_replacesNineGridPreviewWithFullParagraphs() {
        val preview = DynamicItem(
            id_str = "opus-preview",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "预览摘要"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            title = "新翼神龙卡组考卷",
                            summary = OpusSummary(text = "预览摘要"),
                            pics = listOf(
                                OpusPic(url = "https://i0.hdslb.com/1.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/2.jpg")
                            )
                        )
                    )
                )
            )
        )
        val full = DynamicItem(
            id_str = "opus-full",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "完整正文第一段"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            title = "新翼神龙卡组考卷，已快速公式答题",
                            summary = OpusSummary(text = "完整正文第一段"),
                            pics = listOf(
                                OpusPic(url = "https://i0.hdslb.com/1.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/2.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/3.jpg")
                            ),
                            contentBlocks = listOf(
                                OpusContentBlock.Text("完整正文第一段"),
                                OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/1.jpg")),
                                OpusContentBlock.Text("图后还有公式和答题说明")
                            )
                        )
                    )
                )
            )
        )

        val merged = mergeRicherOpusDetailContent(preview, listOf(preview, full))

        assertEquals(3, merged.modules.module_dynamic?.major?.opus?.contentBlocks?.size)
        assertEquals(
            "图后还有公式和答题说明",
            (merged.modules.module_dynamic?.major?.opus?.contentBlocks?.last()
                as? OpusContentBlock.Text)?.text
        )
        assertEquals(
            "opus-preview",
            merged.id_str
        )
    }

    @Test
    fun mergeRicherOpusDetailContent_prefersParagraphsOverLongPreviewSummary() {
        val longPreview = "开门见山简单介绍两个阴的没边的神秘combo".repeat(8)
        val preview = DynamicItem(
            id_str = "1236527093179744277",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = longPreview),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            title = "新翼神龙卡组考卷，已快速公式答题",
                            summary = OpusSummary(text = longPreview),
                            pics = listOf(
                                OpusPic(url = "https://i0.hdslb.com/1.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/2.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/3.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/4.jpg"),
                                OpusPic(url = "https://i0.hdslb.com/5.jpg")
                            )
                        )
                    )
                )
            )
        )
        val full = DynamicItem(
            id_str = "1236527093179744277",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            title = "新翼神龙卡组考卷，已快速公式答题",
                            contentBlocks = listOf(
                                OpusContentBlock.Text(longPreview),
                                OpusContentBlock.Text("新翼神龙卡组考卷，已快速公式答题"),
                                OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/1.jpg")),
                                OpusContentBlock.Text("图后还有公式和答题说明")
                            )
                        )
                    )
                )
            )
        )

        val merged = mergeRicherOpusDetailContent(preview, listOf(preview, full))

        assertEquals(4, merged.modules.module_dynamic?.major?.opus?.contentBlocks?.size)
        assertEquals(
            "图后还有公式和答题说明",
            (merged.modules.module_dynamic?.major?.opus?.contentBlocks?.last()
                as? OpusContentBlock.Text)?.text
        )
        assertTrue(resolveDynamicOpusContentScore(full) > resolveDynamicOpusContentScore(preview))
    }

    @Test
    fun resolveOpusArticleFallbackCvId_prefersFallbackThenColumnCommentId() {
        assertEquals(
            34646640L,
            resolveOpusArticleFallbackCvId(
                fallbackId = 34646640L,
                commentType = 11,
                commentIdStr = "99"
            )
        )
        assertEquals(
            34646640L,
            resolveOpusArticleFallbackCvId(
                fallbackId = null,
                commentType = 12,
                commentIdStr = "34646640"
            )
        )
        assertEquals(
            null,
            resolveOpusArticleFallbackCvId(
                fallbackId = null,
                commentType = 11,
                commentIdStr = "34646640"
            )
        )
    }

    @Test
    fun mergeArticleDetailIntoOpus_replacesPreviewGridWithArticleParagraphs() {
        val preview = DynamicItem(
            id_str = "opus-preview",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            title = "预览标题",
                            pics = listOf(OpusPic(url = "https://i0.hdslb.com/cover.jpg"))
                        )
                    )
                )
            )
        )
        val merged = mergeArticleDetailIntoOpus(
            base = preview,
            title = "新翼神龙卡组考卷，已快速公式答题",
            blocks = listOf(
                ArticleContentBlock.Paragraph("公式说明"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/card.jpg",
                    width = 800,
                    height = 600
                ),
                ArticleContentBlock.Paragraph("答题解析")
            )
        )

        assertEquals(3, merged.modules.module_dynamic?.major?.opus?.contentBlocks?.size)
        assertEquals(
            "答题解析",
            (merged.modules.module_dynamic?.major?.opus?.contentBlocks?.last() as? OpusContentBlock.Text)?.text
        )
        assertEquals(
            "https://i0.hdslb.com/card.jpg",
            (merged.modules.module_dynamic?.major?.opus?.contentBlocks?.get(1) as? OpusContentBlock.Image)?.pic?.url
        )
    }

    @Test
    fun mergeInteractionMetadata_retainsDocumentedCommentTargetFromFeedSeed() {
        val detail = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "detail"))
            )
        )
        val seed = DynamicItem(
            id_str = "dynamic-id",
            basic = DynamicBasic(comment_id_str = "326122895", comment_type = 11),
            modules = DynamicModules(
                module_stat = DynamicStatModule(comment = StatItem(count = 17))
            )
        )

        val merged = mergeDynamicDetailInteractionMetadata(detail, seed)

        assertEquals("326122895", merged.basic?.comment_id_str)
        assertEquals(11, merged.basic?.comment_type)
        assertEquals(17, merged.modules.module_stat?.comment?.count)
    }

    @Test
    fun mergeInteractionMetadata_retainsStandaloneTopicFromFeedSeed() {
        val detail = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "完整正文")),
            ),
        )
        val seed = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    topic = DynamicTopic(id = 1314000L, name = "新机来了！"),
                ),
            ),
        )

        val merged = mergeDynamicDetailInteractionMetadata(detail, seed)

        assertEquals(1314000L, merged.modules.module_dynamic?.topic?.id)
        assertEquals("新机来了！", merged.modules.module_dynamic?.topic?.name)
    }

    @Test
    fun mergeInteractionMetadata_retainsSeedDrawWhenDetailOnlyHasText() {
        val detail = DynamicItem(
            id_str = "dynamic-id",
            type = "2",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "完整正文")
                )
            )
        )
        val seed = DynamicItem(
            id_str = detail.id_str,
            type = "DYNAMIC_TYPE_DRAW",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "预览正文"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_DRAW",
                        draw = DrawMajor(
                            items = listOf(DrawItem(src = "https://img.example/seed.jpg"))
                        )
                    )
                )
            )
        )

        val merged = mergeDynamicDetailInteractionMetadata(detail, seed)

        assertEquals("完整正文", merged.modules.module_dynamic?.desc?.text)
        assertEquals(
            "https://img.example/seed.jpg",
            merged.modules.module_dynamic?.major?.draw?.items?.single()?.src,
        )
    }

    @Test
    fun mergeInteractionMetadata_retainsFeedEmojiNodesForFullDetailBody() {
        val detail = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "完整正文[UPOWER_3546635395139954_舔舔]"),
                ),
            ),
        )
        val seed = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(
                        text = "预览正文[UPOWER_3546635395139954_舔舔]",
                        rich_text_nodes = listOf(
                            RichTextNode(
                                type = "EMOJI",
                                text = "[UPOWER_3546635395139954_舔舔]",
                                emoji = EmojiInfo(
                                    icon_url = "https://i0.hdslb.com/bfs/emote/upower.png",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val merged = mergeDynamicDetailInteractionMetadata(detail, seed)

        assertEquals(
            "完整正文[UPOWER_3546635395139954_舔舔]",
            merged.modules.module_dynamic?.desc?.text,
        )
        assertEquals(
            seed.modules.module_dynamic?.desc?.rich_text_nodes,
            merged.modules.module_dynamic?.desc?.rich_text_nodes,
        )
    }

    @Test
    fun mergeInteractionMetadata_readsEmojiNodesFromOpusSummary() {
        val detailTextNode = RichTextNode(
            type = "RICH_TEXT_NODE_TYPE_TEXT",
            text = "完整正文[UPOWER_3546635395139954_舔舔]",
        )
        val detail = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(
                        text = detailTextNode.text,
                        rich_text_nodes = listOf(detailTextNode),
                    ),
                ),
            ),
        )
        val summaryEmojiNode = RichTextNode(
            type = "RICH_TEXT_NODE_TYPE_EMOJI",
            text = "[UPOWER_3546635395139954_舔舔]",
            emoji = EmojiInfo(
                icon_url = "https://i0.hdslb.com/bfs/garb/upower.png",
                text = "[UPOWER_3546635395139954_舔舔]",
            ),
        )
        val seed = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            summary = com.android.purebilibili.data.model.response.OpusSummary(
                                text = "预览正文[UPOWER_3546635395139954_舔舔]",
                                rich_text_nodes = listOf(summaryEmojiNode),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val merged = mergeDynamicDetailInteractionMetadata(detail, seed)
        val mergedNodes = merged.modules.module_dynamic?.desc?.rich_text_nodes.orEmpty()

        assertEquals(listOf(detailTextNode, summaryEmojiNode), mergedNodes)
    }

    @Test
    fun mergeDetailRichTextNodes_doesNotDuplicateExistingEmojiMetadata() {
        val emojiNode = RichTextNode(
            type = "EMOJI",
            text = "[UPOWER_3546635395139954_糖笑]",
            emoji = EmojiInfo(icon_url = "https://i0.hdslb.com/bfs/garb/smile.png"),
        )

        assertEquals(
            listOf(emojiNode),
            mergeDynamicDetailRichTextNodes(
                detailNodes = listOf(emojiNode),
                seedEmojiNodes = listOf(emojiNode),
            ),
        )
    }

    @Test
    fun mergeRicherOpusDetailContent_retainsCandidateEmojiNodesInMergedSummaryAndDesc() {
        val emojiNode = RichTextNode(
            type = "EMOJI",
            text = "[豹富]",
            emoji = EmojiInfo(
                icon_url = "https://i0.hdslb.com/bfs/emote/baofu.png",
                text = "[豹富]"
            )
        )
        val seed = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "预览正文[豹富]", rich_text_nodes = listOf(emojiNode)),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            summary = OpusSummary(text = "预览摘要[豹富]", rich_text_nodes = listOf(emojiNode))
                        )
                    )
                )
            )
        )
        val detail = DynamicItem(
            id_str = "dynamic-id",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            contentBlocks = listOf(
                                OpusContentBlock.Text("详细正文")
                            )
                        )
                    )
                )
            )
        )
        val candidates = listOf(detail, seed)
        val merged = mergeRicherOpusDetailContent(detail, candidates)

        val mergedDescNodes = merged.modules.module_dynamic?.desc?.rich_text_nodes.orEmpty()
        val mergedSummaryNodes = merged.modules.module_dynamic?.major?.opus?.summary?.rich_text_nodes.orEmpty()
        assertTrue(mergedDescNodes.any { it.text == "[豹富]" && it.emoji?.icon_url == "https://i0.hdslb.com/bfs/emote/baofu.png" })
        assertTrue(mergedSummaryNodes.any { it.text == "[豹富]" && it.emoji?.icon_url == "https://i0.hdslb.com/bfs/emote/baofu.png" })
    }
}
