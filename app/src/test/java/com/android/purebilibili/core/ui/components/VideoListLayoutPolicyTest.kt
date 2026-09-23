package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoListLayoutPolicyTest {
    @Test
    fun explicitColumnChoiceWorksOnPhonesAndTabletsWithNarrowWindowFallback() {
        listOf(320f, 360f, 600f, 840f, 1200f).forEach { width ->
            assertEquals(1, resolveVideoListColumns(true, width))
            assertEquals(2, resolveVideoListColumns(false, width))
        }
        assertEquals(1, resolveVideoListColumns(false, 280f))
    }

    @Test
    fun ordinaryVideoListsExposeThemeAdaptiveLayoutControl() {
        listOf(
            "feature/list/CommonListScreen.kt",
            "feature/watchlater/WatchLaterScreen.kt",
            "feature/search/SearchScreen.kt",
            "feature/profile/ProfileScreen.kt",
            "feature/download/DownloadListScreen.kt",
        ).forEach { path ->
            val source = source(path)
            assertTrue(source.contains("VideoListLayoutToggle("), path)
            assertTrue(source.contains("videoListItemModifier(") || source.contains("videoListBoundsAnimation("), path)
        }
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
