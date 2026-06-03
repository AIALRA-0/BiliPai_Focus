package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.DynamicMoreModule
import com.android.purebilibili.data.model.response.DynamicThreePointItem
import com.android.purebilibili.data.model.response.DynamicThreePointModal
import com.android.purebilibili.data.model.response.DynamicThreePointParams
import com.android.purebilibili.data.model.response.SpaceAggregateArchiveItem
import com.android.purebilibili.data.model.response.SpaceAggregateData
import com.android.purebilibili.data.model.response.SpaceAggregateFavoriteItem
import com.android.purebilibili.data.model.response.SpaceAggregateFavoriteSection
import com.android.purebilibili.data.model.response.SpaceDynamicAuthor
import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicDesc
import com.android.purebilibili.data.model.response.SpaceDynamicDraw
import com.android.purebilibili.data.model.response.SpaceDynamicDrawItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import com.android.purebilibili.data.model.response.SpaceDynamicOpus
import com.android.purebilibili.data.model.response.SpaceDynamicRichText
import com.android.purebilibili.data.model.response.SpaceVideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileSpacePolicyTest {

    @Test
    fun `profile space tabs match self space order`() {
        assertEquals(
            listOf("主页", "动态", "投稿", "收藏", "追番"),
            defaultProfileSpaceTabs().map { it.title }
        )
        assertEquals(ProfileSpaceMainTab.HOME, defaultProfileSpaceTabs().first().tab)
        assertEquals(ProfileSpaceMainTab.BANGUMI, defaultProfileSpaceTabs().last().tab)
    }

    @Test
    fun `home sections keep reference order and hide empty sections`() {
        val sections = resolveProfileSpaceHomeSections(
            favoriteFolders = listOf(FavFolder(id = 1, title = "默认收藏夹", media_count = 8)),
            bangumiItems = listOf(FollowBangumiItem(seasonId = 2, title = "追番")),
            coinVideos = listOf(SpaceAggregateArchiveItem(aid = 3, title = "投币")),
            likeVideos = listOf(SpaceAggregateArchiveItem(aid = 4, title = "点赞")),
            contributionVideos = listOf(SpaceVideoItem(aid = 5, bvid = "BV1xx", title = "投稿"))
        )

        assertEquals(
            listOf(
                ProfileSpaceHomeSection.FAVORITES,
                ProfileSpaceHomeSection.BANGUMI,
                ProfileSpaceHomeSection.COIN_VIDEOS,
                ProfileSpaceHomeSection.LIKE_VIDEOS,
                ProfileSpaceHomeSection.CONTRIBUTIONS,
                ProfileSpaceHomeSection.SERVICES
            ),
            sections
        )
    }

    @Test
    fun `home sections always keep services even when content is empty`() {
        assertEquals(
            listOf(ProfileSpaceHomeSection.SERVICES),
            resolveProfileSpaceHomeSections(
                favoriteFolders = emptyList(),
                bangumiItems = emptyList(),
                coinVideos = emptyList(),
                likeVideos = emptyList(),
                contributionVideos = emptyList()
            )
        )
    }

    @Test
    fun `editable account state trims sign and marks only sign as editable`() {
        val state = ProfileEditableAccountState(
            name = "测试用户",
            birthday = "2000-01-01",
            sex = "保密",
            sign = "  这个人很神秘  "
        )

        assertEquals("这个人很神秘", state.normalizedSign)
        assertTrue(state.canSubmitSign)
        assertFalse(state.canEditName)
        assertFalse(state.canEditBirthday)
        assertFalse(state.canEditSex)
    }

    @Test
    fun `sign validation allows empty sign and blocks over length`() {
        assertEquals(null, validateProfileSign(""))
        assertEquals(null, validateProfileSign("a".repeat(70)))
        assertEquals("签名最多支持 70 个字符", validateProfileSign("a".repeat(71)))
    }

    @Test
    fun `space favorite folders keep cover from aggregate response`() {
        val state = resolveProfileSpaceStateFromAggregate(
            aggregate = SpaceAggregateData(
                favourite2 = SpaceAggregateFavoriteSection(
                    count = 1,
                    item = listOf(
                        SpaceAggregateFavoriteItem(
                            mediaId = 123,
                            title = "默认收藏夹",
                            cover = "https://i0.hdslb.com/bfs/archive/folder.jpg",
                            media_count = 9
                        )
                    )
                )
            ),
            favoriteFoldersFallback = emptyList(),
            bangumiItems = emptyList(),
            dynamicItems = emptyList()
        )

        assertEquals("https://i0.hdslb.com/bfs/archive/folder.jpg", state.favoriteFolders.first().cover)
    }

    @Test
    fun `dynamic cover falls back to draw image when archive is absent`() {
        val item = SpaceDynamicItem(
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        draw = SpaceDynamicDraw(
                            items = listOf(SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/draw.jpg"))
                        )
                    )
                )
            )
        )

        assertEquals("https://i0.hdslb.com/bfs/draw.jpg", resolveProfileDynamicCover(item))
    }

    @Test
    fun `dynamic image urls include all draw pictures for preview`() {
        val item = SpaceDynamicItem(
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        draw = SpaceDynamicDraw(
                            items = listOf(
                                SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/draw-a.jpg"),
                                SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/draw-b.jpg")
                            )
                        )
                    )
                )
            )
        )

        assertEquals(
            listOf(
                "https://i0.hdslb.com/bfs/draw-a.jpg",
                "https://i0.hdslb.com/bfs/draw-b.jpg"
            ),
            resolveProfileDynamicImageUrls(item)
        )
    }

    @Test
    fun `dynamic image urls include opus pictures for preview`() {
        val item = SpaceDynamicItem(
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        opus = SpaceDynamicOpus(
                            pics = listOf(
                                SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/opus-a.jpg"),
                                SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/opus-b.jpg")
                            )
                        )
                    )
                )
            )
        )

        assertEquals(
            listOf(
                "https://i0.hdslb.com/bfs/opus-a.jpg",
                "https://i0.hdslb.com/bfs/opus-b.jpg"
            ),
            resolveProfileDynamicImageUrls(item)
        )
    }

    @Test
    fun `dynamic text prefers rich text nodes and keeps mentions`() {
        val item = SpaceDynamicItem(
            modules = SpaceDynamicModules(
                module_author = SpaceDynamicAuthor(name = "测试用户", pub_time = "2026年05月29日"),
                module_dynamic = SpaceDynamicContent(
                    desc = SpaceDynamicDesc(
                        rich_text_nodes = listOf(
                            SpaceDynamicRichText(type = "RICH_TEXT_NODE_TYPE_AT", orig_text = "@影视飓风 "),
                            SpaceDynamicRichText(type = "RICH_TEXT_NODE_TYPE_TEXT", text = "互动抽奖")
                        ),
                        text = "兜底文本"
                    )
                )
            )
        )

        assertEquals("@影视飓风 互动抽奖", resolveProfileDynamicText(item))
        assertEquals("测试用户", resolveProfileDynamicAuthorName(item))
        assertEquals("2026年05月29日", resolveProfileDynamicPublishText(item))
    }

    @Test
    fun `forward dynamic keeps original content cover and action count text`() {
        val item = SpaceDynamicItem(
            type = "DYNAMIC_TYPE_FORWARD",
            orig = SpaceDynamicItem(
                modules = SpaceDynamicModules(
                    module_dynamic = SpaceDynamicContent(
                        major = SpaceDynamicMajor(
                            draw = SpaceDynamicDraw(
                                items = listOf(SpaceDynamicDrawItem(src = "https://i0.hdslb.com/bfs/orig.jpg"))
                            )
                        )
                    )
                )
            )
        )

        assertEquals("https://i0.hdslb.com/bfs/orig.jpg", resolveProfileDynamicCover(item.orig!!))
        assertEquals("点赞 1.2万", resolveProfileDynamicActionText("点赞", 12000))
    }

    @Test
    fun `profile dynamic delete action uses server menu params`() {
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
                            ),
                            modal = DynamicThreePointModal(
                                title = "要删除动态吗？",
                                content = "动态删除后将无法恢复，请谨慎操作",
                                confirm = "确认删除",
                                cancel = "再想想"
                            )
                        )
                    )
                )
            )
        )

        val action = resolveProfileDynamicDeleteAction(item)

        assertEquals("1063487284684259332", action?.dynamicId)
        assertEquals(1, action?.dynType)
        assertEquals("1063487284684259332", action?.rid)
        assertEquals("确认删除", action?.confirmText)
        assertEquals("再想想", action?.cancelText)
    }
}
