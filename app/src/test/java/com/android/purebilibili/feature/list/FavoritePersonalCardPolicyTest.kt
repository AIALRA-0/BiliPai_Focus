package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritePersonalCardPolicyTest {

    @Test
    fun favoriteDateLabel_addsFavoriteContext() {
        assertEquals("", resolveFavoriteDateLabel(0))
        assertEquals("刚刚收藏", resolveFavoriteDateLabel(1_700_000_000, nowMs = 1_700_000_000_000L))
    }

    @Test
    fun favoriteOwnerMetadataUsesGlobalUpBadgeVisibility() {
        val source = loadFavoritePersonalCardSource()

        assertTrue(source.contains("UpBadgeName("))
        assertFalse(source.contains("showUpBadge = true"))
    }
}

private fun loadFavoritePersonalCardSource(): String {
    val path = "src/main/java/com/android/purebilibili/feature/list/FavoritePersonalCard.kt"
    return listOf(File(path), File("app/$path")).first(File::exists).readText()
}
