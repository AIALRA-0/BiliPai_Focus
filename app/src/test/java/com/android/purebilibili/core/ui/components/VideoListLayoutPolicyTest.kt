package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoListLayoutPolicyTest {
    @Test
    fun explicitColumnChoiceWorksOnPhonesAndTabletsWithNarrowWindowFallback() {
        listOf(320f, 360f, 600f, 840f).forEach { width ->
            assertEquals(1, resolveVideoListColumns(true, width))
            assertEquals(2, resolveVideoListColumns(false, width))
        }
        assertEquals(1, resolveVideoListColumns(false, 280f))
    }

    @Test
    fun wideWindowsDeriveThirdColumnFromMinReadableCardWidth() {
        // 每列至少 320dp：960dp 以下保持 2 列；≥960dp 推导第 3 列并封顶。
        assertEquals(2, resolveVideoListColumns(false, 950f))
        assertEquals(3, resolveVideoListColumns(false, 960f))
        assertEquals(3, resolveVideoListColumns(false, 1200f))
        assertEquals(3, resolveVideoListColumns(false, 1920f))
    }

    @Test
    fun ordinaryVideoListsExposeThemeAdaptiveLayoutControl() {
        listOf(
            "feature/list/CommonListScreen.kt",
            "feature/watchlater/WatchLaterScreen.kt",
            "feature/profile/ProfileScreen.kt",
            "feature/download/DownloadListScreen.kt",
        ).forEach { path ->
            val source = source(path)
            assertTrue(source.contains("VideoListLayoutToggle("), path)
            assertTrue(source.contains("videoListItemModifier(") || source.contains("videoListBoundsAnimation("), path)
        }
        val searchScreen = source("feature/search/SearchScreen.kt")
        val searchFilterBar = source("feature/search/SearchVideoFilterSheet.kt")
        assertTrue(searchScreen.contains("SearchVideoFilterBar("))
        assertTrue(searchScreen.contains("onLayoutToggle = listLayout.toggle"))
        assertTrue(searchFilterBar.contains("VideoListLayoutToggle("))
        assertTrue(searchFilterBar.contains("onClick = onLayoutToggle"))
        assertTrue(searchScreen.contains("videoListItemModifier(") || searchScreen.contains("videoListBoundsAnimation("))
        listOf(
            "feature/home/HomeScreen.kt",
            "feature/partition/PartitionScreen.kt",
            "feature/video/ui/components/RelatedVideoItem.kt",
        ).forEach { path ->
            assertFalse(source(path).contains("VideoListLayoutToggle("), path)
        }
        assertTrue(source("feature/space/SpaceScreen.kt").contains("videoListItemModifier("))
    }

    private fun source(path: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/$path"),
        File("src/main/java/com/android/purebilibili/$path"),
    ).first(File::exists).readText()
}
