package com.android.purebilibili.core.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptivePullToRefreshIntegrationTest {

    @Test
    fun `adaptive pull to refresh box applies indicator top inset for miuix and default indicator`() {
        val source = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/AdaptivePullToRefreshBox.kt"
        )
        assertTrue(source.contains("indicatorTopInset"))
        assertTrue(source.contains("mergedContentPadding"))
        assertTrue(source.contains("padding(top = indicatorTopInset)"))
        assertTrue(source.contains("AdaptivePullToRefreshDefaultIndicator("))
        assertTrue(source.contains("PullToRefreshDefaults.LoadingIndicator("))
        assertTrue(source.contains("PresetPrimitiveRenderer.MATERIAL3"))
    }

    @Test
    fun `home material default pull refresh uses official loading indicator`() {
        val home = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        assertTrue(home.contains("AppPullRefreshLoadingIndicator("))
        assertTrue(home.contains("AppPullRefreshIndicatorStyle.MATERIAL_DEFAULT"))
    }

    @Test
    fun `overlay screens pass non zero indicator top inset`() {
        val home = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val dynamic = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
        val partition = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")

        assertTrue(home.contains("indicatorTopInset = homeRefreshIndicatorTopInset"))
        assertTrue(dynamic.contains("indicatorTopInset = dynamicRefreshIndicatorTopInset"))
        assertTrue(partition.contains("indicatorTopInset = partitionRefreshIndicatorTopInset"))
    }

    @Test
    fun `scaffolded list screens align indicator with their content top inset`() {
        val screens = listOf(
            "app/src/main/java/com/android/purebilibili/feature/message/InboxScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/LikeMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/AtMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/ReplyMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/SystemNoticeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchTrendingScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt"
        )
        screens.forEach { path ->
            val source = loadSource(path)
            // Full-height scaffold bodies keep the viewport edge-to-edge and apply the same
            // scaffold inset to their scroll content, so the refresh indicator uses that inset too.
            // A page that applies scaffold padding to an outer parent instead correctly uses zero.
            val alignedWithPaddedList = when {
                source.contains("indicatorTopInset = paddingValues.calculateTopPadding()") ->
                    source.contains("top = paddingValues.calculateTopPadding()")
                source.contains("indicatorTopInset = padding.calculateTopPadding()") ->
                    source.contains("top = padding.calculateTopPadding()")
                else -> false
            }
            assertTrue(
                "$path must align the refresh indicator with its list content or use zero when " +
                    "an outer parent already applies scaffold padding",
                alignedWithPaddedList || source.contains("indicatorTopInset = 0.dp") ||
                    source.contains("indicatorTopInset = AppSpacingTokens.None")
            )
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
            File("../$path")
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
