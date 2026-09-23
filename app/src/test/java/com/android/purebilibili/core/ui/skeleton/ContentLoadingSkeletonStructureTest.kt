package com.android.purebilibili.core.ui.skeleton

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentLoadingSkeletonStructureTest {

    @Test
    fun contentSkeletons_coverVideoGridMediaRowUserRowAndComments() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/skeleton/ContentLoadingSkeletons.kt"
        )
        assertTrue(source.contains("fun ContentVideoGridSkeleton("))
        assertTrue(source.contains("fun ContentVideoGridSkeletonFixedColumns("))
        assertTrue(source.contains("fun MediaListRowSkeleton("))
        assertTrue(source.contains("fun UserListRowSkeleton("))
        assertTrue(source.contains("fun CommentListSkeleton("))
        assertTrue(source.contains("fun CommentListColumnSkeleton("))
        assertTrue(source.contains("fun ContentMediaListSkeleton("))
        assertTrue(source.contains("fun ArticleDetailSkeleton("))
        assertTrue(source.contains("fun PosterDetailSkeleton("))
        assertTrue(source.contains("fun ContentCategoryGridSkeleton("))
        assertTrue(source.contains("fun TrendingListSkeleton("))
        assertTrue(source.contains("rememberContentSkeletonPulse("))
        // Home-style soft pulse; no left-right shimmer sweep flicker.
        assertTrue(source.contains("RepeatMode.Reverse"))
        assertFalse(source.contains("shimmerEffect()"))
    }

    @Test
    fun detailCatalogAndSpaceInitialLoads_matchTheirRealContentShape() {
        val space = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt"
        )
        val spaceSkeleton = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/space/SpaceLoadingSkeleton.kt"
        )
        val article = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/article/ArticleDetailScreen.kt"
        )
        val bangumiDetail = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiDetailScreen.kt"
        )
        val liveArea = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt"
        )
        val trending = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchTrendingScreen.kt"
        )

        assertTrue(space.contains("SpaceLoadingSkeleton("))
        assertFalse(space.contains("SpaceUiState.Loading -> {\n                        com.android.purebilibili.core.ui.skeleton.ContentMediaListSkeleton("))
        assertTrue(spaceSkeleton.contains("resolveSpaceContentGridColumnCount("))
        assertTrue(spaceSkeleton.contains("resolveSpaceAdaptiveLayoutSpec("))
        assertTrue(spaceSkeleton.contains("adaptiveLayoutSpec.contentMaxWidthDp.dp"))
        assertTrue(spaceSkeleton.contains("adaptiveLayoutSpec.useExpandedHeader"))
        assertTrue(spaceSkeleton.contains("resolveSpaceBannerMetrics("))
        assertTrue(spaceSkeleton.contains("GridItemSpan(maxLineSpan)"))
        assertTrue(article.contains("ArticleDetailSkeleton("))
        assertTrue(bangumiDetail.contains("PosterDetailSkeleton("))
        assertTrue(liveArea.contains("ContentCategoryGridSkeleton("))
        assertTrue(trending.contains("TrendingListSkeleton("))
    }

    @Test
    fun searchScreen_usesSkeletonInsteadOfThemeLoadingForResults() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt"
        )
        assertTrue(source.contains("SearchResultBodyMode.LOADING"))
        assertTrue(
            source.contains("ContentVideoGridSkeletonFixedColumns(") ||
                source.contains("ContentVideoGridSkeleton(")
        )
        assertTrue(source.contains("ContentMediaListSkeleton("))
        assertFalse(source.contains("LoadingAnimation("))
        assertFalse(source.contains("text = \"搜索中...\""))
    }

    @Test
    fun listStyleScreens_preferSkeletonOnInitialLoad() {
        val category = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt"
        )
        val partition = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt"
        )
        val watchLater = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt"
        )
        val liveSearch = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveSearchScreen.kt"
        )

        assertTrue(category.contains("ContentVideoGridSkeletonFixedColumns("))
        assertTrue(partition.contains("ContentMediaListSkeleton("))
        assertTrue(watchLater.contains("PersonalMediaCardSkeleton("))
        assertTrue(liveSearch.contains("ContentVideoGridSkeletonFixedColumns("))
        assertTrue(liveSearch.contains("ContentMediaListSkeleton("))
    }

    @Test
    fun bangumiAndCommentSurfaces_useSkeletonsForEmptyInitialLoading() {
        val bangumi = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiHubContent.kt"
        )
        val videoComments = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoCommentSheetHost.kt"
        )
        val dynamicComments = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        )

        assertTrue(bangumi.contains("BangumiPosterGridSkeleton()"))
        assertTrue(bangumi.contains("BangumiTimelineSkeleton()"))
        assertTrue(bangumi.contains("BangumiFollowManagerSkeleton()"))
        assertTrue(videoComments.contains("state.isRepliesLoading && state.replies.isEmpty()"))
        assertTrue(videoComments.contains("CommentListSkeleton("))
        assertTrue(dynamicComments.contains("isLoading && comments.isEmpty()"))
        assertTrue(dynamicComments.contains("CommentListSkeleton("))
        assertTrue(dynamicComments.contains("CommentListColumnSkeleton("))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
        )
        return candidates.first { it.exists() }.readText()
    }
}
