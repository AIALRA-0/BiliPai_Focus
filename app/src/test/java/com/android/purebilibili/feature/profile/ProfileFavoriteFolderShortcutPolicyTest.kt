package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.FavFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFavoriteFolderShortcutPolicyTest {

    @Test
    fun `resolveProfileFavoriteFolderShortcuts filters invalid empty duplicate and caps results`() {
        val folders = listOf(
            FavFolder(id = 1L, mid = 42L, title = " 默认收藏夹 ", media_count = 8),
            FavFolder(id = 0L, title = "无效", media_count = 2),
            FavFolder(id = 2L, title = "", media_count = 2),
            FavFolder(id = 3L, title = "空夹", media_count = 0),
            FavFolder(id = 1L, title = "重复id", media_count = 99),
            FavFolder(id = 4L, mid = 0L, title = "公开收藏", media_count = 3),
            FavFolder(id = 5L, mid = 42L, title = "稍后整理", media_count = 1)
        )

        val result = resolveProfileFavoriteFolderShortcuts(
            folders = folders,
            ownerMid = 42L,
            maxCount = 2
        )

        assertEquals(listOf(1L, 4L), result.map { it.mediaId })
        assertEquals(listOf("默认收藏夹", "公开收藏"), result.map { it.title })
        assertEquals(listOf(42L, 42L), result.map { it.ownerMid })
        assertEquals(listOf(8, 3), result.map { it.mediaCount })
    }
}
