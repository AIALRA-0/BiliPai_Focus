package com.android.purebilibili.feature.category

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryScreenStructureTest {

    @Test
    fun categoryVideoClickPassesCidInsteadOfArchiveId() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt")

        assertTrue(source.contains("onVideoClick(bvid, video.cid, video.pic, video.isVertical)"))
        assertFalse(source.contains("onVideoClick(bvid, video.id, video.pic, video.isVertical)"))
    }

    @Test
    fun categoryCardsFollowHomeMetadataVisibilitySettings() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt")

        assertTrue(source.contains("homeDurationStyle = homeSettings.homeDurationStyle"))
        assertTrue(source.contains("compactStatsOnCover = homeSettings.compactVideoStatsOnCover ||"))
        assertTrue(source.contains("showOnlineCount = showOnlineCount"))
    }
}

private fun loadSource(path: String): String {
    val normalizedPath = path.removePrefix("app/")
    val sourceFile = listOf(
        File(path),
        File(normalizedPath),
        File("app", normalizedPath)
    ).firstOrNull { it.exists() }
    require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
    return sourceFile.readText()
}
