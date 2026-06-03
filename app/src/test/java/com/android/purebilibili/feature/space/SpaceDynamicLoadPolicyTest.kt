package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicDraw
import com.android.purebilibili.data.model.response.SpaceDynamicDrawItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicDesc
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import com.android.purebilibili.data.model.response.SpaceDynamicArchive
import com.android.purebilibili.data.model.response.SpaceDynamicArticle
import com.android.purebilibili.data.model.response.SpaceDynamicOpus
import com.android.purebilibili.data.model.response.SpaceDynamicOpusSummary
import com.android.purebilibili.data.model.response.DynamicMajorBadge
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicMoreModule
import com.android.purebilibili.data.model.response.DynamicThreePointItem
import com.android.purebilibili.data.model.response.DynamicThreePointParams
import com.android.purebilibili.data.model.response.SpaceDynamicCount
import com.android.purebilibili.data.model.response.SpaceDynamicStat
import com.android.purebilibili.feature.dynamic.DynamicCommentTarget
import com.android.purebilibili.feature.dynamic.resolveDynamicCommentTargets
import com.android.purebilibili.feature.dynamic.components.resolveDynamicArchiveBadgeLabel
import com.android.purebilibili.feature.dynamic.components.DynamicCardMediaAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardMediaAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpaceDynamicLoadPolicyTest {

    @Test
    fun filterSpaceDynamicItemsByQuery_matchesDynamicTextAndArchiveTitle() {
        val items = listOf(
            SpaceDynamicItem(
                id_str = "1",
                modules = SpaceDynamicModules(
                    module_dynamic = SpaceDynamicContent(
                        desc = SpaceDynamicDesc(text = "今天打游戏")
                    )
                )
            ),
            SpaceDynamicItem(
                id_str = "2",
                modules = SpaceDynamicModules(
                    module_dynamic = SpaceDynamicContent(
                        major = SpaceDynamicMajor(
                            archive = SpaceDynamicArchive(
                                bvid = "BV1",
                                title = "火影周年庆攻略",
                                desc = "视频简介"
                            )
                        )
                    )
                )
            )
        )

        assertEquals(listOf("1"), filterSpaceDynamicItemsByQuery(items, "游戏").map { it.id_str })
        assertEquals(listOf("2"), filterSpaceDynamicItemsByQuery(items, "火影").map { it.id_str })
    }

    @Test
    fun filterSpaceDynamicItemsByQuery_matchesOpusTitleAndSummary() {
        val items = listOf(
            SpaceDynamicItem(
                id_str = "3",
                modules = SpaceDynamicModules(
                    module_dynamic = SpaceDynamicContent(
                        major = SpaceDynamicMajor(
                            opus = SpaceDynamicOpus(
                                title = "旅行相册",
                                summary = SpaceDynamicOpusSummary(text = "记录春天的海边")
                            )
                        )
                    )
                )
            )
        )

        assertEquals(listOf("3"), filterSpaceDynamicItemsByQuery(items, "旅行").map { it.id_str })
        assertEquals(listOf("3"), filterSpaceDynamicItemsByQuery(items, "海边").map { it.id_str })
    }

    @Test
    fun resolveSpaceDynamicCardItem_preservesDeleteMenuParams() {
        val item = SpaceDynamicItem(
            id_str = "1063487284684259332",
            modules = SpaceDynamicModules(
                module_more = DynamicMoreModule(
                    three_point_items = listOf(
                        DynamicThreePointItem(
                            label = "删除",
                            type = "THREE_POINT_DELETE",
                            params = DynamicThreePointParams(
                                dyn_id_str = "1063487284684259332",
                                dyn_type = 1,
                                rid_str = "1063487284684259332"
                            )
                        )
                    )
                )
            )
        )

        val dynamic = resolveSpaceDynamicCardItem(item)
        val params = dynamic.modules.module_more?.three_point_items?.single()?.params

        assertEquals("1063487284684259332", params?.dyn_id_str)
        assertEquals(1, params?.dyn_type)
        assertEquals("1063487284684259332", params?.rid_str)
    }

    @Test
    fun shouldRequestInitialSpaceDynamicLoad_onlyTriggersBeforeFirstCompletedLoad() {
        assertEquals(
            true,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = false,
                isLoading = false
            )
        )
        assertEquals(
            false,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = false,
                isLoading = true
            )
        )
        assertEquals(
            false,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = true,
                isLoading = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_treatsUntouchedEmptyStateAsLoading() {
        assertEquals(
            SpaceDynamicPresentationState.LOADING,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = false,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsEmptyAfterSuccessfulEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.EMPTY,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsErrorAfterFailedEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.ERROR,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = true
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_prefersContentWhenItemsExist() {
        assertEquals(
            SpaceDynamicPresentationState.CONTENT,
            resolveSpaceDynamicPresentationState(
                itemCount = 3,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicCardItems_maps_space_draw_to_shared_media_capable_card() {
        val mapped = resolveSpaceDynamicCardItems(
            listOf(
                SpaceDynamicItem(
                    id_str = "123",
                    modules = SpaceDynamicModules(
                        module_dynamic = SpaceDynamicContent(
                            major = SpaceDynamicMajor(
                                draw = SpaceDynamicDraw(
                                    id = 9L,
                                    items = listOf(
                                        SpaceDynamicDrawItem(src = "a", width = 10, height = 10),
                                        SpaceDynamicDrawItem(src = "b", width = 20, height = 20)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).first()

        val mediaAction = assertIs<DynamicCardMediaAction.PreviewImages>(
            resolveDynamicCardMediaAction(mapped, clickedIndex = 1)
        )

        assertEquals(listOf("a", "b"), mediaAction.images)
        assertEquals(1, mediaAction.initialIndex)
    }

    @Test
    fun resolveSpaceDynamicCardItems_maps_space_article_to_shared_article_card() {
        val mapped = resolveSpaceDynamicCardItems(
            listOf(
                SpaceDynamicItem(
                    id_str = "1200069469486972932",
                    type = "DYNAMIC_TYPE_ARTICLE",
                    basic = DynamicBasic(
                        comment_id_str = "1200069469486972932",
                        comment_type = 17,
                        rid_str = "1200069469486972932"
                    ),
                    modules = SpaceDynamicModules(
                        module_dynamic = SpaceDynamicContent(
                            major = SpaceDynamicMajor(
                                type = "MAJOR_TYPE_ARTICLE",
                                article = SpaceDynamicArticle(
                                    id = 1200069469486972932L,
                                    title = "长图文标题",
                                    desc = "完整长图文摘要",
                                    covers = listOf("https://i0.hdslb.com/bfs/article/cover.jpg"),
                                    jump_url = "https://www.bilibili.com/opus/1200069469486972932"
                                )
                            )
                        ),
                        module_stat = SpaceDynamicStat(
                            comment = SpaceDynamicCount(count = 5),
                            forward = SpaceDynamicCount(count = 2),
                            like = SpaceDynamicCount(count = 32)
                        )
                    )
                )
            )
        ).first()

        val article = mapped.modules.module_dynamic?.major?.article
        val desc = mapped.modules.module_dynamic?.desc
        val stat = mapped.modules.module_stat
        val mediaAction = assertIs<DynamicCardMediaAction.PreviewImages>(
            resolveDynamicCardMediaAction(mapped, clickedIndex = 0)
        )

        assertEquals(
            listOf(DynamicCommentTarget(oid = 1200069469486972932L, type = 17)),
            resolveDynamicCommentTargets(mapped)
        )
        assertEquals("长图文标题\n完整长图文摘要", desc?.text)
        assertEquals(5, stat?.comment?.count)
        assertEquals(2, stat?.forward?.count)
        assertEquals(32, stat?.like?.count)
        assertEquals(1200069469486972932L, article?.id)
        assertEquals("长图文标题", article?.title)
        assertEquals("完整长图文摘要", article?.desc)
        assertEquals(listOf("https://i0.hdslb.com/bfs/article/cover.jpg"), mediaAction.images)
    }

    @Test
    fun resolveSpaceDynamicCardItems_preserves_space_archive_charge_badge() {
        val mapped = resolveSpaceDynamicCardItems(
            listOf(
                SpaceDynamicItem(
                    id_str = "1200000000000000000",
                    type = "DYNAMIC_TYPE_AV",
                    modules = SpaceDynamicModules(
                        module_dynamic = SpaceDynamicContent(
                            major = SpaceDynamicMajor(
                                type = "MAJOR_TYPE_ARCHIVE",
                                archive = SpaceDynamicArchive(
                                    aid = "123",
                                    bvid = "BV1xx411c7mD",
                                    title = "空间充电动态",
                                    badge = DynamicMajorBadge(text = "充电专属"),
                                    isChargingArc = true,
                                    elecArcType = 1
                                )
                            )
                        )
                    )
                )
            )
        ).first()

        val archive = mapped.modules.module_dynamic?.major?.archive

        assertEquals("充电专属", archive?.badge?.text)
        assertEquals(true, archive?.isChargingArc)
        assertEquals(1, archive?.elecArcType)
        assertEquals("充电专属", archive?.let(::resolveDynamicArchiveBadgeLabel))
    }
}
