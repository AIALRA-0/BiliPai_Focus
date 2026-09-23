package com.android.purebilibili.feature.home

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedAlignedRowsPolicyTest {

    @Test
    fun truncatedCards_advanceInCompleteRows() {
        assertEquals(
            listOf(0..3, 4..7, 8..9),
            resolveHomeFeedAlignedRows(itemCount = 10, columns = 4),
        )
    }

    @Test
    fun oldContentDivider_startsAFreshAlignedRow() {
        assertEquals(
            listOf(0..3, 4..5, 6..9),
            resolveHomeFeedAlignedRows(itemCount = 10, columns = 4, dividerIndex = 6),
        )
    }

    @Test
    fun emptyFeed_hasNoRows() {
        assertEquals(
            emptyList<IntRange>(),
            resolveHomeFeedAlignedRows(itemCount = 0, columns = 4),
        )
    }

    @Test
    fun fullCardSetting_exclusivelyOwnsTheWaterfallBranch() {
        val pageSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt"
        )
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"
        )

        assertTrue(pageSource.contains("if (showFullVideoCardContent)"))
        assertTrue(pageSource.contains("resolveHomeFeedAlignedRows("))
        assertTrue(pageSource.contains("contentType = \"home_video_row\""))
        assertTrue(
            screenSource.contains(
                "showFullVideoCardContent = homeSettings.showFullVideoCardContent"
            )
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
