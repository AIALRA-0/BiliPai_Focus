package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PagerIndicatorContinuityStructureTest {

    @Test
    fun `pager-backed liquid tab controls expose continuous indicator position`() {
        val videoSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val bangumiSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt"
        )
        val liveSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(videoSource.contains("currentPageOffsetFraction"))
        assertTrue(bangumiSource.contains("currentPageOffsetFraction"))
        assertTrue(liveSource.contains("currentPageOffsetFraction"))
    }

    @Test
    fun `live primary interaction panel keeps one programmatic pager writer`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )
        val panelSource = source.substringAfter("private fun LivePrimaryInteractionPanel(")

        assertEquals(1, Regex("pagerState\\.animateScrollToPage").findAll(panelSource).count())
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .first { it.exists() }
            .readText()
    }
}
