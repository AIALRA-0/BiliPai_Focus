package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.ArticleMajor
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicMajor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicUserRequestPolicyTest {

    @Test
    fun `user dynamic result applies only when uid and token still match`() {
        assertTrue(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 456L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 11L,
                requestToken = 10L
            )
        )
    }

    @Test
    fun `selected user reload policy retries same user when scoped state is empty or failed`() {
        assertFalse(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = null,
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = emptyList(),
                userError = null,
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = "加载失败",
            )
        )
    }

    @Test
    fun `selected user should show local timeline items before remote feed arrives`() {
        val localOnly = listOf(
            buildDynamicItem(id = "local_1", mid = 10001L),
            buildDynamicItem(id = "other", mid = 10002L),
            buildDynamicItem(id = "local_2", mid = 10001L)
        )

        val result = resolveSelectedUserVisibleItems(
            timelineItems = localOnly,
            remoteUserItems = emptyList(),
            selectedUid = 10001L
        )

        assertEquals(listOf("local_1", "local_2"), result.map { it.id_str })
    }

    @Test
    fun `selected user always loads authoritative space feed while showing local matches`() {
        assertTrue(
            shouldAutoLoadSelectedUserDynamics(
                previousUid = null,
                nextUid = 10001L,
                currentItems = emptyList(),
                userError = null,
            )
        )
        assertTrue(
            shouldAutoLoadSelectedUserDynamics(
                previousUid = null,
                nextUid = 10001L,
                currentItems = emptyList(),
                userError = null,
            )
        )
    }

    @Test
    fun `selected user content filter keeps author and content axes independent`() {
        val video = DynamicItem(
            id_str = "video",
            type = "DYNAMIC_TYPE_AV",
        )
        val article = DynamicItem(
            id_str = "article",
            type = "DYNAMIC_TYPE_DRAW",
        )
        val legacyArticle = DynamicItem(
            id_str = "legacy-article",
            type = "DYNAMIC_TYPE_NONE",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(article = ArticleMajor(id = 123L)),
                ),
            ),
        )

        assertEquals(
            listOf("video"),
            filterSelectedUserDynamicItems(
                items = listOf(video, article),
                filter = DynamicUserContentFilter.VIDEO,
            ).map { it.id_str },
        )
        assertEquals(
            listOf("article", "legacy-article"),
            filterSelectedUserDynamicItems(
                items = listOf(video, article, legacyArticle),
                filter = DynamicUserContentFilter.ARTICLE,
            ).map { it.id_str },
        )
    }

    @Test
    fun `empty concrete user filter does not trigger unbounded pagination`() {
        assertEquals(
            listOf("全部", "视频", "图文专栏"),
            DynamicUserContentFilter.entries.map(DynamicUserContentFilter::label),
        )
        assertTrue(
            shouldAutoLoadMoreForUserContentFilter(
                isSelectedUserFeed = true,
                filter = DynamicUserContentFilter.ALL,
                visibleItemCount = 0,
            )
        )
        assertFalse(
            shouldAutoLoadMoreForUserContentFilter(
                isSelectedUserFeed = true,
                filter = DynamicUserContentFilter.ARTICLE,
                visibleItemCount = 0,
            )
        )
        assertTrue(
            shouldAutoLoadMoreForUserContentFilter(
                isSelectedUserFeed = true,
                filter = DynamicUserContentFilter.ARTICLE,
                visibleItemCount = 1,
            )
        )
    }
}

private fun buildDynamicItem(id: String, mid: Long) = DynamicItem(
    id_str = id,
    modules = DynamicModules(
        module_author = DynamicAuthorModule(
            mid = mid,
            name = "user_$mid"
        )
    )
)
