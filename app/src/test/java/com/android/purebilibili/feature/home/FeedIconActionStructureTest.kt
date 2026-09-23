package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FeedIconActionStructureTest {
    private data class ExpectedIconAction(
        val path: String,
        val semanticMarkers: List<String>,
        val minimumTouchTargetCount: Int,
    )

    @Test
    fun compactCardActions_useNamed48DpTouchTargetsAndLabels() {
        val files = listOf(
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/home/components/cards/CinematicVideoCard.kt",
                semanticMarkers = listOf("contentDescription = \"更多操作\""),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/home/components/cards/HomeStyleSingleColumnVideoCard.kt",
                semanticMarkers = listOf("contentDescription = \"更多操作\""),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt",
                semanticMarkers = listOf(
                    "contentDescription = \"取消收藏\"",
                    "contentDescription = \"更多操作\"",
                ),
                minimumTouchTargetCount = 2,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt",
                semanticMarkers = listOf("contentDescription = \"删除\""),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt",
                semanticMarkers = listOf(
                    "contentDescription = \"个人中心\"",
                    "contentDescription = \"设置\"",
                ),
                minimumTouchTargetCount = 2,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt",
                semanticMarkers = listOf("contentDescription = \"切换布局模式\""),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt",
                semanticMarkers = listOf("\"隐藏已隐藏用户\"", "\"显示隐藏用户\""),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt",
                semanticMarkers = listOf("contentDescription = \"查看\${author.name}的个人主页\""),
                minimumTouchTargetCount = 2,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/dynamic/components/ActionButton.kt",
                semanticMarkers = listOf("contentDescription = label"),
                minimumTouchTargetCount = 1,
            ),
            ExpectedIconAction(
                "src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt",
                semanticMarkers = listOf("contentDescription = \"搜索\""),
                minimumTouchTargetCount = 1,
            ),
        )

        files.forEach { expected ->
            val source = locate(expected.path).readText()
            val touchTargetCount = Regex(Regex.escape("AppChromeSizeTokens.MinimumTouchTarget"))
                .findAll(source)
                .count()
            assertTrue(
                touchTargetCount >= expected.minimumTouchTargetCount,
                "${expected.path} 的紧凑图标操作没有逐个使用 48dp 外层触控 token",
            )
            expected.semanticMarkers.forEach { marker ->
                assertTrue(
                    source.contains(marker),
                    "${expected.path} 的图标操作缺少可读名称标记 $marker",
                )
            }
        }

        val cinematicSource = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/CinematicVideoCard.kt",
        ).readText()
        val touchTargetIndex = cinematicSource.indexOf(".size(AppChromeSizeTokens.MinimumTouchTarget)")
        val visualContainerIndex = cinematicSource.indexOf(
            ".size(AppSpacingTokens.ExtraLarge)",
            startIndex = touchTargetIndex,
        )
        val visualBackgroundIndex = cinematicSource.indexOf(
            ".background(MediaContrastPalette.Scrim.copy(alpha = 0.3f), CircleShape)",
            startIndex = touchTargetIndex,
        )
        assertTrue(touchTargetIndex >= 0)
        assertTrue(visualContainerIndex > touchTargetIndex)
        assertTrue(visualBackgroundIndex > visualContainerIndex)
    }

    private fun locate(path: String): File = listOf(File(path), File("app/$path"))
        .firstOrNull(File::exists) ?: error("Cannot locate $path")
}
