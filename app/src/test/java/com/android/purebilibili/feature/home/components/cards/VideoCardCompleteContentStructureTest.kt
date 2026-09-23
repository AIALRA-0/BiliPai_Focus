package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardCompleteContentStructureTest {

    private val completeContentCardSources = listOf(
        "feature/home/components/cards/VideoCard.kt",
        "feature/home/components/cards/StoryVideoCard.kt",
        "feature/home/components/cards/GlassVideoCard.kt",
        "feature/home/components/cards/CinematicVideoCard.kt",
        "feature/home/components/cards/HomeStyleSingleColumnVideoCard.kt",
        "feature/dynamic/components/VideoCards.kt",
        "feature/list/FavoritePersonalCard.kt",
        "feature/video/ui/components/RelatedVideoItem.kt",
        "feature/video/ui/components/UpPreviewSheet.kt",
        "feature/watchlater/WatchLaterScreen.kt",
    )

    @Test
    fun `video card titles follow the global full-content setting`() {
        completeContentCardSources.forEach { relativePath ->
            val source = loadSource(relativePath)
            assertTrue(
                source.contains("videoCardTitleMaxLines("),
                "$relativePath must truncate titles when 完整卡片展示 is off",
            )
            assertTrue(
                source.contains("videoCardTitleOverflow("),
                "$relativePath must omit full titles when 完整卡片展示 is off",
            )
        }
    }

    @Test
    fun `grid search and space cards follow the global title setting`() {
        val searchCard = loadSource("feature/search/SearchScreen.kt")
            .substringAfter("fun SearchResultCard(")
            .substringBefore("internal fun UpSearchResultCard(")
        val spaceCards = loadSource("feature/space/SpaceScreen.kt")
            .substringAfter("private fun SpaceHomeVideoCard(")
            .substringBefore("private fun SpaceNoticeCard(")

        assertTrue(searchCard.contains("videoCardTitleMaxLines("))
        assertTrue(searchCard.contains("videoCardTitleOverflow("))
        assertTrue(searchCard.contains("FlowRow("))
        assertTrue(spaceCards.contains("videoCardTitleMaxLines("))
        assertTrue(spaceCards.contains("videoCardTitleOverflow("))
    }

    @Test
    fun `grid card metadata stays complete while titles may truncate`() {
        val source = loadSource("feature/home/components/cards/VideoCard.kt")
        assertTrue(source.contains("metaMaxLines = Int.MAX_VALUE"))
        assertFalse(
            source.substringAfter("if (scrollLitePolicy.showSecondaryStatsRow)")
                .substringBefore("VideoCardOwnerMetadata(")
                .contains("overflow = TextOverflow.Ellipsis")
        )
    }

    private fun loadSource(relativePath: String): String {
        val path = "src/main/java/com/android/purebilibili/$relativePath"
        return listOf(File(path), File("app/$path")).first(File::exists).readText()
    }
}
