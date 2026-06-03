package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteManagementPolicyTest {

    @Test
    fun `favorite sort order maps to bilibili api values`() {
        assertEquals("mtime", FavoriteResourceOrder.FAVORITE_TIME.apiValue)
        assertEquals("view", FavoriteResourceOrder.PLAY_COUNT.apiValue)
        assertEquals("pubtime", FavoriteResourceOrder.PUBLISH_TIME.apiValue)
    }

    @Test
    fun `favorite sort order exposes concise labels`() {
        assertEquals("收藏时间", FavoriteResourceOrder.FAVORITE_TIME.label)
        assertEquals("播放量", FavoriteResourceOrder.PLAY_COUNT.label)
        assertEquals("投稿时间", FavoriteResourceOrder.PUBLISH_TIME.label)
    }

    @Test
    fun `favorite clean invalid only applies to owned folders`() {
        assertTrue(
            canCleanInvalidFavoriteResources(
                FavFolder(id = 1L, title = "自建", source = FavFolderSource.OWNED)
            )
        )
        assertFalse(
            canCleanInvalidFavoriteResources(
                FavFolder(id = 2L, title = "订阅", source = FavFolderSource.SUBSCRIBED)
            )
        )
        assertFalse(canCleanInvalidFavoriteResources(null))
    }

    @Test
    fun `favorite clean confirmation mentions current folder title`() {
        assertEquals(
            "确认清理「默认收藏夹」里的失效内容吗？本操作会修改远端收藏夹。",
            resolveFavoriteCleanInvalidConfirmText("默认收藏夹")
        )
    }
}
