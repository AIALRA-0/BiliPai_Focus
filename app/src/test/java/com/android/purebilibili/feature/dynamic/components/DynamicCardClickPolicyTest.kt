package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.ArticleMajor
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import com.android.purebilibili.data.model.response.UgcSeasonMajor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCardClickPolicyTest {

    @Test
    fun resolveDynamicCardPrimaryAction_prefersVideoWhenArchiveBvidExists() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_opensDynamicDetailWhenNoVideo() {
        val item = DynamicItem(id_str = "  987654321  ")

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenDynamicDetail)
        assertEquals("987654321", (action as DynamicCardPrimaryAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_opensDynamicDetailForArticleOpusJumpUrl() {
        val item = DynamicItem(
            id_str = "1200069469486972932",
            type = "DYNAMIC_TYPE_ARTICLE",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_ARTICLE",
                        article = ArticleMajor(
                            id = 1200069469486972932L,
                            title = "长图文标题",
                            jump_url = "https://www.bilibili.com/opus/1200069469486972932"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenDynamicDetail)
        assertEquals("1200069469486972932", (action as DynamicCardPrimaryAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_returnsNoneWhenNoVideoAndNoId() {
        val item = DynamicItem(id_str = "  ")

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.None)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesArchiveJumpUrlWhenBvidMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(
                            bvid = "",
                            jump_url = "//www.bilibili.com/video/BV1d4421Z7nW/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1d4421Z7nW", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesUgcSeasonJumpUrlWhenArchiveMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            jump_url = "//www.bilibili.com/video/BV1oeWNebEv2/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("BV1oeWNebEv2", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardPrimaryAction_usesUgcSeasonAidWhenJumpUrlMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            aid = 1129813966L
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        assertTrue(action is DynamicCardPrimaryAction.OpenVideo)
        assertEquals("av1129813966", (action as DynamicCardPrimaryAction.OpenVideo).bvid)
    }

    @Test
    fun dispatchDynamicCardPrimaryClick_prefersOverrideOverDefaultAction() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )
        val events = mutableListOf<String>()

        dispatchDynamicCardPrimaryClick(
            item = item,
            action = DynamicCardPrimaryAction.OpenVideo("BV1xx411c7mD"),
            onPrimaryClickOverride = { events += "comment:${it.id_str}" },
            onVideoClick = { events += "video:$it" },
            onBangumiClick = { seasonId, epId -> events += "bangumi:$seasonId:$epId" },
            onArticleClick = { articleId, _ -> events += "article:$articleId" },
            onDynamicDetailClick = { events += "dynamic:$it" },
            onUserClick = { events += "user:$it" },
            onLiveClick = { roomId, _, _ -> events += "live:$roomId" }
        )

        assertEquals(listOf("comment:123"), events)
    }

    @Test
    fun shouldEnableDynamicCardPrimaryClick_allowsOverrideWhenDefaultActionIsUnavailable() {
        assertTrue(
            shouldEnableDynamicCardPrimaryClick(
                action = DynamicCardPrimaryAction.None,
                hasArticleClick = false,
                hasDynamicDetailClick = false,
                hasPrimaryClickOverride = true
            )
        )
    }

    @Test
    fun resolveDynamicWatchLaterAid_usesArchiveAidWhenVideoDynamicHasAid() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(aid = "1129813966", bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        assertEquals(1129813966L, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicWatchLaterAid_usesOriginalItemForForwardedVideoDynamic() {
        val item = DynamicItem(
            id_str = "forward",
            orig = DynamicItem(
                id_str = "origin",
                modules = DynamicModules(
                    module_dynamic = DynamicContentModule(
                        major = DynamicMajor(
                            archive = ArchiveMajor(aid = "99887766", bvid = "BV1xx411c7mD")
                        )
                    )
                )
            )
        )

        assertEquals(99887766L, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicWatchLaterAid_returnsNullForNonVideoDynamic() {
        val item = DynamicItem(id_str = "123")

        assertEquals(null, resolveDynamicWatchLaterAid(item))
    }

    @Test
    fun resolveDynamicCardMediaAction_previewsArticleCovers() {
        val item = DynamicItem(
            id_str = "1199344045210468386",
            type = "DYNAMIC_TYPE_ARTICLE",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_ARTICLE",
                        article = ArticleMajor(
                            covers = listOf(
                                "https://i0.hdslb.com/bfs/article/a.jpg",
                                "https://i0.hdslb.com/bfs/article/b.jpg"
                            )
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 1)

        assertTrue(action is DynamicCardMediaAction.PreviewImages)
        assertEquals(
            listOf(
                "https://i0.hdslb.com/bfs/article/a.jpg",
                "https://i0.hdslb.com/bfs/article/b.jpg"
            ),
            (action as DynamicCardMediaAction.PreviewImages).images
        )
        assertEquals(1, action.initialIndex)
    }

    @Test
    fun resolveDynamicCardMediaAction_opensDynamicDetailForOpusPreviewOnListCard() {
        val item = DynamicItem(
            id_str = "1201902028962398230",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            pics = listOf(OpusPic(url = "https://i0.hdslb.com/opus.jpg"))
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 0, isDetail = false)

        assertTrue(action is DynamicCardMediaAction.OpenDynamicDetail)
        assertEquals("1201902028962398230", action.dynamicId)
    }

    @Test
    fun resolveDynamicCardMediaAction_previewsOpusImagesOnDetailPage() {
        val item = DynamicItem(
            id_str = "1201902028962398230",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(
                            pics = listOf(OpusPic(url = "https://i0.hdslb.com/opus.jpg"))
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 0, isDetail = true)

        assertTrue(action is DynamicCardMediaAction.PreviewImages)
        assertEquals(listOf("https://i0.hdslb.com/opus.jpg"), action.images)
    }

    @Test
    fun resolveArticleCoverDrawItems_filtersBlankCoversForRendering() {
        val drawItems = resolveArticleCoverDrawItems(
            ArticleMajor(
                covers = listOf(
                    " ",
                    " https://i0.hdslb.com/bfs/article/a.jpg ",
                    ""
                )
            )
        )

        assertEquals(1, drawItems.size)
        assertEquals("https://i0.hdslb.com/bfs/article/a.jpg", drawItems.first().src)
    }

    @Test
    fun resolveDynamicOpusPresentationBlocks_usesFullBlocksOnlyOnDetailPage() {
        val opus = OpusMajor(
            pics = listOf(OpusPic(url = "https://i0.hdslb.com/preview.jpg")),
            contentBlocks = listOf(
                OpusContentBlock.Text("完整正文"),
                OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/full.jpg"))
            )
        )

        assertEquals(
            opus.contentBlocks,
            resolveDynamicOpusPresentationBlocks(opus = opus, isDetail = true)
        )
        assertEquals(
            emptyList(),
            resolveDynamicOpusPresentationBlocks(opus = opus, isDetail = false)
        )
    }

    @Test
    fun resolveDynamicOpusPreviewImageLimit_removesNineImageLimitOnDetailPage() {
        assertEquals(null, resolveDynamicOpusPreviewImageLimit(isDetail = true))
        assertEquals(9, resolveDynamicOpusPreviewImageLimit(isDetail = false))
    }

    @Test
    fun resolveDynamicOpusLinkCardAction_routesBilibiliTargetsInApp() {
        assertEquals(
            DynamicOpusLinkCardAction.OpenVideo("BV1xx411c7mD"),
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_UGC",
                    title = "视频",
                    jumpUrl = "https://www.bilibili.com/video/BV1xx411c7mD"
                )
            )
        )
        assertEquals(
            DynamicOpusLinkCardAction.OpenDynamicDetail("1201902028962398230"),
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_OPUS",
                    title = "图文",
                    jumpUrl = "https://www.bilibili.com/opus/1201902028962398230"
                )
            )
        )
        assertEquals(
            DynamicOpusLinkCardAction.OpenArticle(123456L, "专栏"),
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_COMMON",
                    title = "专栏",
                    jumpUrl = "https://www.bilibili.com/read/cv123456"
                )
            )
        )
        assertEquals(
            DynamicOpusLinkCardAction.OpenLive(6L),
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_LIVE",
                    title = "直播",
                    jumpUrl = "https://live.bilibili.com/6"
                )
            )
        )
    }

    @Test
    fun resolveDynamicOpusLinkCardAction_routesExternalUrlAndIgnoresMissingUrl() {
        assertEquals(
            DynamicOpusLinkCardAction.OpenExternalUrl("https://uland.taobao.com/item"),
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_GOODS",
                    title = "商品",
                    jumpUrl = " https://uland.taobao.com/item "
                )
            )
        )
        assertEquals(
            DynamicOpusLinkCardAction.None,
            resolveDynamicOpusLinkCardAction(
                OpusLinkCard(
                    type = "LINK_CARD_TYPE_ITEM_NULL",
                    title = "内容已失效"
                )
            )
        )
    }
}
