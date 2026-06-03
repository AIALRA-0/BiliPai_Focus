package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Upper
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteFolderAggregationPolicyTest {

    @Test
    fun `mergeFavoriteFoldersForDisplay keeps owned folders first and appends subscribed folders`() {
        val owned = listOf(
            FavFolder(id = 1, title = "默认收藏夹", media_count = 10, source = FavFolderSource.OWNED),
            FavFolder(id = 2, title = "动画", media_count = 4, source = FavFolderSource.OWNED)
        )
        val subscribed = listOf(
            FavFolder(id = 3, title = "游戏合集", media_count = 6, source = FavFolderSource.SUBSCRIBED)
        )

        val result = mergeFavoriteFoldersForDisplay(owned, subscribed)

        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
        assertEquals(
            listOf(FavFolderSource.OWNED, FavFolderSource.OWNED, FavFolderSource.SUBSCRIBED),
            result.map { it.source }
        )
    }

    @Test
    fun `mergeFavoriteFoldersForDisplay de duplicates by id and preserves owned version`() {
        val owned = listOf(
            FavFolder(id = 1, title = "默认收藏夹", media_count = 10, source = FavFolderSource.OWNED)
        )
        val subscribed = listOf(
            FavFolder(id = 1, title = "默认收藏夹", media_count = 10, source = FavFolderSource.SUBSCRIBED),
            FavFolder(id = 4, title = "技术合集", media_count = 8, source = FavFolderSource.SUBSCRIBED)
        )

        val result = mergeFavoriteFoldersForDisplay(owned, subscribed)

        assertEquals(listOf(1L, 4L), result.map { it.id })
        assertEquals(FavFolderSource.OWNED, result.first().source)
    }

    @Test
    fun `resolveFavoriteFolderTabLabel marks subscribed folders`() {
        val label = resolveFavoriteFolderTabLabel(
            FavFolder(id = 3, title = "游戏合集", media_count = 6, source = FavFolderSource.SUBSCRIBED)
        )

        assertTrue(label.contains("订阅"))
        assertTrue(label.startsWith("游戏合集"))
    }

    @Test
    fun `resolveFavoriteFolderMediaId keeps owned folder media id`() {
        val mediaId = resolveFavoriteFolderMediaId(
            FavFolder(
                id = 1725337634L,
                fid = 17253376L,
                mid = 3461565701425334L,
                title = "默认收藏夹",
                source = FavFolderSource.OWNED
            )
        )

        assertEquals(1725337634L, mediaId)
    }

    @Test
    fun `resolveFavoriteFolderMediaId expands subscribed fid into media id`() {
        val mediaId = resolveFavoriteFolderMediaId(
            FavFolder(
                id = 1650276L,
                fid = 1650276L,
                mid = 3461565701425334L,
                title = "好评如潮",
                source = FavFolderSource.SUBSCRIBED
            )
        )

        assertEquals(165027634L, mediaId)
    }

    @Test
    fun `resolveSubscribedFavoriteCollectionRoute returns season route for subscribed collection`() {
        val route = resolveSubscribedFavoriteCollectionRoute(
            FavFolder(
                id = 1324105L,
                mid = 39366561L,
                title = "一天体重测试系列",
                type = 21,
                upper = Upper(mid = 39366561L, name = "测试UP"),
                source = FavFolderSource.SUBSCRIBED
            )
        )

        assertEquals(
            FavoriteCollectionRoute(
                type = "favorite_season",
                id = 1324105L,
                mid = 39366561L,
                title = "一天体重测试系列",
                ownerName = "测试UP"
            ),
            route
        )
    }

    @Test
    fun `resolveFavoriteCollectionRoute carries collection owner name`() {
        val route = resolveFavoriteCollectionRoute(
            VideoItem(
                title = "小约翰可汗高分视频",
                owner = Owner(mid = 96070394L, name = "UP-Sings"),
                isCollectionResource = true,
                collectionId = 725909L,
                collectionMid = 96070394L
            )
        )

        assertEquals("UP-Sings", route?.ownerName)
    }

    @Test
    fun `resolveSubscribedFavoriteCollectionRoute ignores non collection folders`() {
        val route = resolveSubscribedFavoriteCollectionRoute(
            FavFolder(
                id = 1324105L,
                mid = 39366561L,
                title = "一天体重测试系列",
                type = 0,
                source = FavFolderSource.SUBSCRIBED
            )
        )

        assertEquals(null, route)
    }

    @Test
    fun `filterFavoriteFoldersByQuery matches subscribed titles`() {
        val result = filterFavoriteFoldersByQuery(
            folders = listOf(
                FavFolder(id = 1, title = "Wallpaper Engine 壁纸推荐", source = FavFolderSource.SUBSCRIBED),
                FavFolder(id = 2, title = "烂活电竞2023", source = FavFolderSource.SUBSCRIBED)
            ),
            query = "壁纸"
        )

        assertEquals(listOf("Wallpaper Engine 壁纸推荐"), result.map { it.title })
    }
}
