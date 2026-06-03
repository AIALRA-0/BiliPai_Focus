package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicArchive
import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceArticleItem
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import com.android.purebilibili.data.model.response.SpaceDynamicOpus
import com.android.purebilibili.data.model.response.SpaceDynamicArticle
import com.android.purebilibili.data.model.response.SpaceDynamicDrawItem
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpaceDynamicNavigationPolicyTest {

    @Test
    fun resolveSpaceDynamicClickAction_prefersVideoWhenArchiveBvidExists() {
        val dynamic = SpaceDynamicItem(
            id_str = "12345",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        archive = SpaceDynamicArchive(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as SpaceDynamicClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveSpaceDynamicClickAction_usesDesktopDynamicDetailWhenNoVideo() {
        val dynamic = SpaceDynamicItem(id_str = "987654321")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenDynamicDetail)
        assertEquals("987654321", (action as SpaceDynamicClickAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveSpaceDynamicClickAction_returnsNoneWhenNoBvidAndNoId() {
        val dynamic = SpaceDynamicItem(id_str = "   ")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.None)
    }

    @Test
    fun resolveSpaceDynamicClickAction_matches_shared_dynamic_primary_action() {
        val dynamic = SpaceDynamicItem(
            id_str = "2468",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        opus = SpaceDynamicOpus(
                            title = "图文标题",
                            pics = listOf(SpaceDynamicDrawItem(src = "a"))
                        )
                    )
                )
            )
        )

        val sharedAction = assertIs<DynamicCardPrimaryAction.OpenDynamicDetail>(
            resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))
        )
        val spaceAction = assertIs<SpaceDynamicClickAction.OpenDynamicDetail>(
            resolveSpaceDynamicClickAction(dynamic)
        )

        assertEquals(sharedAction.dynamicId, spaceAction.dynamicId)
    }

    @Test
    fun resolveSpaceDynamicClickAction_opensArticleWhenArticleMajorHasId() {
        val dynamic = SpaceDynamicItem(
            id_str = "1200069469486972932",
            type = "DYNAMIC_TYPE_ARTICLE",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        type = "MAJOR_TYPE_ARTICLE",
                        article = SpaceDynamicArticle(
                            id = 1200069469486972932L,
                            title = "长图文标题"
                        )
                    )
                )
            )
        )

        val sharedAction = assertIs<DynamicCardPrimaryAction.OpenArticle>(
            resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))
        )
        val spaceAction = assertIs<SpaceDynamicClickAction.OpenArticle>(
            resolveSpaceDynamicClickAction(dynamic)
        )

        assertEquals(sharedAction.articleId, spaceAction.articleId)
        assertEquals("长图文标题", spaceAction.title)
    }

    @Test
    fun resolveSpaceDynamicClickAction_opensDynamicDetailWhenArticleJumpUrlIsOpus() {
        val dynamic = SpaceDynamicItem(
            id_str = "1200069469486972932",
            type = "DYNAMIC_TYPE_ARTICLE",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        type = "MAJOR_TYPE_ARTICLE",
                        article = SpaceDynamicArticle(
                            id = 1200069469486972932L,
                            title = "长图文标题",
                            jump_url = "https://www.bilibili.com/opus/1200069469486972932"
                        )
                    )
                )
            )
        )

        val sharedAction = assertIs<DynamicCardPrimaryAction.OpenDynamicDetail>(
            resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))
        )
        val spaceAction = assertIs<SpaceDynamicClickAction.OpenDynamicDetail>(
            resolveSpaceDynamicClickAction(dynamic)
        )

        assertEquals(sharedAction.dynamicId, spaceAction.dynamicId)
        assertEquals("1200069469486972932", spaceAction.dynamicId)
    }

    @Test
    fun resolveSpaceArticleClickAction_opensDynamicDetailForOpusJumpUrl() {
        val action = resolveSpaceArticleClickAction(
            SpaceArticleItem(
                id = 1201902028962398230L,
                title = "长图文",
                jump_url = "//www.bilibili.com/opus/1201902028962398230"
            )
        )

        assertTrue(action is SpaceDynamicClickAction.OpenDynamicDetail)
        assertEquals("1201902028962398230", action.dynamicId)
    }

    @Test
    fun resolveSpaceArticleClickAction_opensDynamicDetailForOpusIdWithoutJumpUrl() {
        val action = resolveSpaceArticleClickAction(
            SpaceArticleItem(
                id = 1201902028962398230L,
                title = "长图文"
            )
        )

        assertTrue(action is SpaceDynamicClickAction.OpenDynamicDetail)
        assertEquals("1201902028962398230", action.dynamicId)
    }

    @Test
    fun resolveSpaceArticleClickAction_keepsLegacyArticleForCvId() {
        val action = resolveSpaceArticleClickAction(
            SpaceArticleItem(
                id = 123456L,
                title = "旧专栏"
            )
        )

        assertTrue(action is SpaceDynamicClickAction.OpenArticle)
        assertEquals(123456L, action.articleId)
        assertEquals("旧专栏", action.title)
    }
}
