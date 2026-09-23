package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Upper
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.FavoriteRequestException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

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
                ownerName = "测试UP",
                sharedElementTransition = true
            ),
            route
        )
    }

    @Test
    fun `favorite collection shared element id uses type and id`() {
        assertEquals(
            "favorite_season:1324105",
            resolveFavoriteCollectionSharedElementId(" favorite_season ", 1324105L)
        )
        assertEquals(null, resolveFavoriteCollectionSharedElementId("", 1324105L))
        assertEquals(null, resolveFavoriteCollectionSharedElementId("favorite_season", 0L))
    }

    @Test
    fun `subscribed favorite preview uses normalized list cover`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/latest.jpg",
            resolveSubscribedFavoritePreviewCover(
                FavFolder(
                    cover = " https://i0.hdslb.com/bfs/archive/latest.jpg ",
                    source = FavFolderSource.SUBSCRIBED
                )
            )
        )
        assertEquals(
            null,
            resolveSubscribedFavoritePreviewCover(
                FavFolder(cover = " ", source = FavFolderSource.SUBSCRIBED)
            )
        )
        assertEquals(
            null,
            resolveSubscribedFavoritePreviewCover(
                FavFolder(cover = "https://example.com/owned.jpg", source = FavFolderSource.OWNED)
            )
        )
    }

    @Test
    fun `favorite folder preview prefers folder cover then loaded item cover`() {
        val loadedItems = listOf(
            VideoItem(pic = ""),
            VideoItem(pic = "https://example.com/first-video.jpg")
        )

        assertEquals(
            "https://example.com/folder.jpg",
            resolveFavoriteFolderPreviewCover(
                folder = FavFolder(cover = " https://example.com/folder.jpg "),
                loadedItems = loadedItems
            )
        )
        assertEquals(
            "https://example.com/first-video.jpg",
            resolveFavoriteFolderPreviewCover(
                folder = FavFolder(cover = " "),
                loadedItems = loadedItems
            )
        )
        assertEquals(
            null,
            resolveFavoriteFolderPreviewCover(
                folder = FavFolder(cover = " "),
                loadedItems = listOf(VideoItem(pic = " "))
            )
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

    @Test
    fun `resolveFavoriteFolderLoadState preserves cached items when request fails`() {
        val cachedItem = VideoItem(id = 1L, title = "已加载视频")

        val state = resolveFavoriteFolderLoadState(
            previousState = ListUiState(items = listOf(cachedItem), isLoading = true),
            title = "默认收藏夹",
            canRemoveItems = true,
            result = Result.failure(IllegalStateException("请求过于频繁"))
        )

        assertEquals(listOf(cachedItem), state.items)
        assertEquals("请求过于频繁", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `resolveFavoriteFolderItems rejects missing resources for non empty folder`() {
        val result = resolveFavoriteFolderItems(
            expectedItemCount = 3,
            resources = null
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `resolveFavoriteFolderItems accepts a genuinely empty folder`() {
        val result = resolveFavoriteFolderItems(
            expectedItemCount = 0,
            resources = null
        )

        assertEquals(emptyList(), result.getOrThrow())
    }

    @Test
    fun `requestFavoriteFolderWithRetry retries one network failure`() = runTest {
        var attempts = 0

        val result = requestFavoriteFolderWithRetry(
            retryDelayMillis = 0L,
            delayAction = {}
        ) {
            attempts++
            if (attempts == 1) {
                Result.failure(IOException("连接重置"))
            } else {
                Result.success("已恢复")
            }
        }

        assertEquals("已恢复", result.getOrThrow())
        assertEquals(2, attempts)
    }

    @Test
    fun `requestFavoriteFolderWithRetry does not retry 412 or 429`() = runTest {
        val riskCodes = listOf(412, 429)

        riskCodes.forEach { code ->
            var attempts = 0
            val result = requestFavoriteFolderWithRetry(
                retryDelayMillis = 0L,
                delayAction = {}
            ) {
                attempts++
                Result.failure<String>(
                    FavoriteRequestException(
                        httpCode = code,
                        message = "HTTP $code"
                    )
                )
            }

            assertTrue(result.isFailure)
            assertEquals(1, attempts)
            assertEquals(
                "请求被风控，请稍后重试",
                resolveFavoriteErrorMessage(result.exceptionOrNull()!!)
            )
        }
    }

    @Test
    fun `requestFavoriteFolderWithRetry retries one 5xx failure`() = runTest {
        var attempts = 0

        val result = requestFavoriteFolderWithRetry(
            retryDelayMillis = 0L,
            delayAction = {}
        ) {
            attempts++
            if (attempts == 1) {
                Result.failure(
                    FavoriteRequestException(
                        httpCode = 503,
                        message = "HTTP 503"
                    )
                )
            } else {
                Result.success("已恢复")
            }
        }

        assertEquals("已恢复", result.getOrThrow())
        assertEquals(2, attempts)
    }

    @Test
    fun `resolveFavoriteFolderLoadState shows friendly risk control error and keeps cache`() {
        val cachedItem = VideoItem(id = 1L, title = "缓存视频")

        val state = resolveFavoriteFolderLoadState(
            previousState = ListUiState(items = listOf(cachedItem), isLoading = true),
            title = "默认收藏夹",
            canRemoveItems = true,
            result = Result.failure(
                FavoriteRequestException(
                    apiCode = -412,
                    message = "请求被拦截"
                )
            )
        )

        assertEquals(listOf(cachedItem), state.items)
        assertEquals("请求被风控，请稍后重试", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `shouldApplyFavoriteFolderResult rejects stale order response`() {
        assertFalse(
            shouldApplyFavoriteFolderResult(
                requestGeneration = 3L,
                currentGeneration = 3L,
                requestedMediaId = 10L,
                currentMediaId = 10L,
                requestedOrder = "mtime",
                currentOrder = "view"
            )
        )
    }
}
