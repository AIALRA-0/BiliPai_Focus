package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicModules
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicPagerFeedStateTest {

    @Test
    fun `更新分类页不会覆盖全部页缓存`() {
        val allPage = DynamicTimelinePageState(
            items = listOf(dynamicItem("all-1")).toImmutableList(),
            hasMore = true
        )
        val initial = DynamicUiState(
            timelinePages = persistentMapOf("all" to allPage)
        )

        val updated = updateDynamicTimelinePage(
            currentState = initial,
            requestType = "video"
        ) {
            DynamicTimelinePageState(
                items = listOf(dynamicItem("video-1")).toImmutableList(),
                isLoading = true,
                hasMore = false
            )
        }

        assertEquals(listOf("all-1"), updated.timelinePage("all").items.map { it.id_str })
        assertFalse(updated.timelinePage("all").isLoading)
        assertTrue(updated.timelinePage("all").hasMore)
        assertEquals(listOf("video-1"), updated.timelinePage("video").items.map { it.id_str })
        assertTrue(updated.timelinePage("video").isLoading)
        assertFalse(updated.timelinePage("video").hasMore)
    }

    @Test
    fun `未加载分类页返回独立空状态`() {
        val state = DynamicUiState(
            timelinePages = persistentMapOf(
                "all" to DynamicTimelinePageState(
                    items = listOf(dynamicItem("all-1")).toImmutableList()
                )
            )
        )

        val articlePage = state.timelinePage("article")

        assertTrue(articlePage.items.isEmpty())
        assertFalse(articlePage.isLoading)
        assertTrue(articlePage.hasMore)
    }

    @Test
    fun `每个 Pager 页面读取对应 Tab 缓存`() {
        val state = DynamicUiState(
            timelinePages = persistentMapOf(
                "all" to DynamicTimelinePageState(
                    items = listOf(dynamicItem("all-1")).toImmutableList()
                ),
                "video" to DynamicTimelinePageState(
                    items = listOf(
                        DynamicItem(id_str = "video-1", type = "DYNAMIC_TYPE_AV")
                    ).toImmutableList()
                )
            )
        )

        val allPresentation = resolveDynamicPagePresentation(state, logicalTab = 0, selectedUserId = null)
        val videoPresentation = resolveDynamicPagePresentation(state, logicalTab = 1, selectedUserId = null)

        assertEquals(listOf("all-1"), allPresentation.items.map { it.id_str })
        assertEquals(listOf("video-1"), videoPresentation.items.map { it.id_str })
    }

    @Test
    fun `presentation hides folded cards until they are unfolded`() {
        val state = DynamicUiState(
            timelinePages = persistentMapOf(
                "all" to DynamicTimelinePageState(
                    items = listOf(
                        DynamicItem(id_str = "visible-7h", visible = true),
                        DynamicItem(id_str = "hidden-a", visible = false),
                        DynamicItem(id_str = "visible-12h", visible = true),
                    ).toImmutableList()
                )
            )
        )

        val presentation = resolveDynamicPagePresentation(
            state,
            logicalTab = 0,
            selectedUserId = null,
        )

        assertEquals(listOf("visible-7h", "visible-12h"), presentation.items.map { it.id_str })
    }

    @Test
    fun `author selection filters each category while pgc and mixed feeds remain independent`() {
        fun authorItem(id: String, type: String) = DynamicItem(
            id_str = id,
            type = type,
            modules = DynamicModules(module_author = DynamicAuthorModule(mid = 22L))
        )
        val localVideo = authorItem("local-video", "DYNAMIC_TYPE_AV")
        val localArticle = authorItem("local-article", "DYNAMIC_TYPE_DRAW")
        val remoteVideo = authorItem("remote-video", "DYNAMIC_TYPE_AV")
        val remoteArticle = authorItem("remote-article", "DYNAMIC_TYPE_ARTICLE")
        val otherVideo = DynamicItem(id_str = "other-video", type = "DYNAMIC_TYPE_AV")
        val pgcItem = DynamicItem(id_str = "pgc", type = "DYNAMIC_TYPE_PGC")
        val state = DynamicUiState(
            timelinePages = persistentMapOf(
                "all" to DynamicTimelinePageState(
                    items = listOf(otherVideo, localVideo, localArticle).toImmutableList(),
                    hasMore = false
                ),
                "video" to DynamicTimelinePageState(
                    items = listOf(otherVideo, localVideo).toImmutableList(),
                    hasMore = false
                ),
                "article" to DynamicTimelinePageState(
                    items = listOf(localArticle).toImmutableList(),
                    hasMore = false
                ),
                "pgc" to DynamicTimelinePageState(
                    items = listOf(pgcItem).toImmutableList(),
                    hasMore = false
                )
            ),
            userItems = listOf(remoteVideo, remoteArticle).toImmutableList(),
            userError = "author request failed",
            hasUserMore = true
        )

        val expectedAuthorIds = mapOf(
            0 to listOf("local-video", "local-article", "remote-video", "remote-article"),
            1 to listOf("local-video", "remote-video"),
            3 to listOf("local-article", "remote-article")
        )
        for ((tab, expectedIds) in expectedAuthorIds) {
            val author = resolveDynamicPagePresentation(state, logicalTab = tab, selectedUserId = 22L)
            assertTrue(author.isSelectedUserFeed)
            assertEquals(expectedIds, author.items.map { it.id_str })
            assertEquals("author request failed", author.error)
            assertTrue(author.hasMore)
        }
        val expectedMixedIds = mapOf(
            0 to listOf("other-video", "local-video", "local-article"),
            1 to listOf("other-video", "local-video"),
            3 to listOf("local-article")
        )
        for ((tab, expectedIds) in expectedMixedIds) {
            val mixed = resolveDynamicPagePresentation(state, logicalTab = tab, selectedUserId = null)
            assertFalse(mixed.isSelectedUserFeed)
            assertEquals(expectedIds, mixed.items.map { it.id_str })
            assertEquals(null, mixed.error)
            assertFalse(mixed.hasMore)
        }
        val pgc = resolveDynamicPagePresentation(state, logicalTab = 2, selectedUserId = 22L)
        assertFalse(pgc.isSelectedUserFeed)
        assertEquals(listOf("pgc"), pgc.items.map { it.id_str })
        assertEquals(null, pgc.error)
        assertFalse(pgc.hasMore)
    }

    private fun dynamicItem(id: String) = DynamicItem(id_str = id)
}
