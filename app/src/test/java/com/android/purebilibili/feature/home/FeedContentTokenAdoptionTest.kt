package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FeedContentTokenAdoptionTest {
    private val cardFiles = listOf(
        "CinematicVideoCard.kt",
        "GlassVideoCard.kt",
        "HomeStyleSingleColumnVideoCard.kt",
        "HorizontalVideoCardStats.kt",
        "StoryVideoCard.kt",
        "VideoCard.kt",
    )

    @Test
    fun feedCardCompositions_readSharedSemanticTypography() {
        val sources = cardFiles.associateWith { name ->
            locate("src/main/java/com/android/purebilibili/feature/home/components/cards/$name")
                .readText()
        }

        sources.forEach { (name, source) ->
            assertTrue(source.contains("feedContentTypography("), "$name 未读取共享信息流排版")
            val requiredRoles = if (name == "HorizontalVideoCardStats.kt") {
                listOf("statistic")
            } else if (name == "HomeStyleSingleColumnVideoCard.kt") {
                listOf("title", "author", "coverBadge")
            } else {
                listOf("title", "author", "statistic", "coverBadge")
            }
            requiredRoles.forEach { role ->
                assertTrue(
                    source.contains("contentTypography.$role"),
                    "$name 没有使用信息流排版角色 $role",
                )
            }
        }

        val sharedRowSource = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt",
        ).readText()
        assertTrue(!sharedRowSource.contains("topSpacingDp"), "卡片间距不能通过裸整数绕过 token 扫描")
        assertTrue(sharedRowSource.contains("topSpacing: Dp"), "共享元数据行应接收已解析的 Dp")
    }

    @Test
    fun large_card_compositions_preserve_title_hierarchy_through_shared_roles() {
        val tokens = locateDesignSystem(
            "src/main/java/com/android/purebilibili/core/ui/FeedContentTokens.kt",
        ).readText()
        val cinematic = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/CinematicVideoCard.kt",
        ).readText()
        val story = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/StoryVideoCard.kt",
        ).readText()

        assertTrue(tokens.contains("enum class FeedTitleHierarchy"))
        assertTrue(tokens.contains("MaterialTheme.typography.bodyMedium"))
        assertTrue(tokens.contains("lineHeight = bodyMedium.fontSize * 1.38f"))
        assertTrue(tokens.contains("letterSpacing = 0.3.sp"))
        assertTrue(!tokens.contains("MaterialTheme.typography.titleLarge"))
        assertTrue(cinematic.contains("feedContentTypography(FeedTitleHierarchy.Prominent)"))
        assertTrue(story.contains("FeedTitleHierarchy.Standard"))
    }

    private fun locate(path: String): File = listOf(File(path), File("app/$path"))
        .firstOrNull(File::exists) ?: error("Cannot locate $path")

    private fun locateDesignSystem(path: String): File = listOf(
        File("../design-system/$path"),
        File("design-system/$path"),
    ).firstOrNull(File::exists) ?: error("Cannot locate design-system/$path")
}
