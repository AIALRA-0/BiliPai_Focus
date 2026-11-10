package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteFolderCoverPrefetchPolicyTest {

    @Test
    fun `fetch targets cover every blank folder instead of the first six`() {
        // 回归：此前补封面被 take(6) 截断，第 7 个及以后的收藏夹永远没有封面。
        val folders = (1..9).map { index ->
            FavFolder(id = index.toLong(), title = "夹$index", cover = "", media_count = index)
        }

        val targets = resolveFavoriteFolderCoverFetchTargets(
            folders = folders,
            alreadyResolved = emptySet(),
        )

        assertEquals((1L..9L).toList(), targets.map { it.mediaId })
    }

    @Test
    fun `fetch targets skip folders that already have a cover or no content`() {
        val folders = listOf(
            FavFolder(id = 1, title = "已有封面", cover = "https://i0.hdslb.com/a.jpg", media_count = 3),
            FavFolder(id = 2, title = "空收藏夹", cover = "", media_count = 0),
            FavFolder(id = 3, title = "需要封面", cover = " ", media_count = 2),
        )

        val targets = resolveFavoriteFolderCoverFetchTargets(
            folders = folders,
            alreadyResolved = emptySet(),
        )

        assertEquals(listOf(3L), targets.map { it.mediaId })
        assertEquals(2, targets.single().mediaCount)
    }

    @Test
    fun `fetch targets skip already resolved folders and dedupe by media id`() {
        val folders = listOf(
            FavFolder(id = 1, title = "已解析", cover = "", media_count = 1),
            FavFolder(id = 2, title = "待补", cover = "", media_count = 1),
            FavFolder(id = 2, title = "重复项", cover = "", media_count = 1),
            FavFolder(id = 3, title = "也待补", cover = "", media_count = 1),
        )

        val targets = resolveFavoriteFolderCoverFetchTargets(
            folders = folders,
            alreadyResolved = setOf(1L),
        )

        assertEquals(listOf(2L, 3L), targets.map { it.mediaId })
    }

    @Test
    fun `fetch targets ignore folders without a valid media id`() {
        val folders = listOf(
            FavFolder(id = 0, fid = 0, title = "无 id", cover = "", media_count = 2),
            FavFolder(id = 5, title = "有效", cover = "", media_count = 2),
        )

        val targets = resolveFavoriteFolderCoverFetchTargets(
            folders = folders,
            alreadyResolved = emptySet(),
        )

        assertEquals(listOf(5L), targets.map { it.mediaId })
    }

    @Test
    fun `prefetch end extends past the visible frontier by the prefetch distance`() {
        assertEquals(9, resolveFavoriteFolderCoverPrefetchEnd(folderCount = 20, lastVisibleIndex = 5))
        assertEquals(4, resolveFavoriteFolderCoverPrefetchEnd(folderCount = 20, lastVisibleIndex = 4, prefetchDistance = 0))
    }

    @Test
    fun `prefetch end clamps to the last folder and rejects empty lists`() {
        assertEquals(4, resolveFavoriteFolderCoverPrefetchEnd(folderCount = 5, lastVisibleIndex = 4))
        assertEquals(-1, resolveFavoriteFolderCoverPrefetchEnd(folderCount = 0, lastVisibleIndex = 3))
        assertEquals(-1, resolveFavoriteFolderCoverPrefetchEnd(folderCount = 5, lastVisibleIndex = -1))
    }

    @Test
    fun `prefetch end stays inside bounds when the frontier overshoots`() {
        val end = resolveFavoriteFolderCoverPrefetchEnd(folderCount = 3, lastVisibleIndex = 100)
        assertTrue(end in 0..2)
        assertEquals(2, end)
    }
}
