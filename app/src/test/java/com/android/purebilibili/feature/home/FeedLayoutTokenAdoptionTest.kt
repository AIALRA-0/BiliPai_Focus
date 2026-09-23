package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FeedLayoutTokenAdoptionTest {
    @Test
    fun shellHostedLists_readTheResolvedBottomBarPadding() {
        val files = listOf(
            "src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt",
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt",
            "src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
            "src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt",
        )

        files.forEach { path ->
            val source = locate(path).readText()
            assertTrue(
                source.contains("LocalBottomBarContentPadding.current"),
                "$path 应读取 App Shell 计算后的底栏占位",
            )
        }
    }

    @Test
    fun singleColumnFeeds_wireTheirLargeScreenWidthPolicies() {
        val expected = mapOf(
            "src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt" to
                ".responsiveContentWidth(maxWidth = contentWidth)",
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt" to
                ".responsiveContentWidth(maxWidth = resolveDynamicTimelineMaxWidth())",
            "src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt" to
                ".responsiveContentWidth(resolveFollowingListMaxWidth())",
            "src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt" to
                ".responsiveContentWidth(resolveCommonListSingleColumnMaxWidth())",
        )

        expected.forEach { (path, call) ->
            assertTrue(locate(path).readText().contains(call), "$path 未接入大屏限宽策略")
        }

        val dynamicSource = locate(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt",
        ).readText()
        assertTrue(
            dynamicSource.indexOf(".responsiveContentWidth(maxWidth = resolveDynamicTimelineMaxWidth())") <
                dynamicSource.indexOf(".fillMaxSize()", startIndex = dynamicSource.indexOf("FeedVerticalStaggeredGrid(")),
            "动态宽屏约束必须位于 fillMaxSize 外层，否则最大宽度不会生效",
        )
    }

    @Test
    fun featureGeometry_isResolvedByNamedLayoutPolicies() {
        val expected = mapOf(
            "src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt" to
                "resolveHomeFeedMaxContentWidth()",
            "src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt" to
                "resolveVideoListColumns(",
            "src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt" to
                "resolveVideoListColumns(",
            "src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt" to
                "resolveFollowingBatchGroupDialogMaxHeight()",
        )

        expected.forEach { (path, call) ->
            assertTrue(locate(path).readText().contains(call), "$path 未从功能布局 Policy 读取几何尺寸")
        }

        val commonListSource = locate(
            "src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
        ).readText()
        assertTrue(commonListSource.contains("resolveFavoriteSubscribedFolderPreviewWidth()"))
    }

    private fun locate(path: String): File = listOf(File(path), File("app/$path"))
        .firstOrNull(File::exists) ?: error("Cannot locate $path")
}
