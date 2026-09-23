package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardImageDecodeStrategyStructureTest {

    @Test
    fun `video cards constrain decode only with layout derived cover request specs`() {
        val sourceRoot = File("src/main/java/com/android/purebilibili/feature/home/components/cards")
        val cardSources = listOf(
            "VideoCard.kt",
            "StoryVideoCard.kt",
            "GlassVideoCard.kt",
            "CinematicVideoCard.kt"
        ).associateWith { fileName -> sourceRoot.resolve(fileName).readText() }

        cardSources.forEach { (fileName, source) ->
            val imageRequests = Regex(
                "ImageRequest\\.Builder\\([^)]*\\)[\\s\\S]*?\\.build\\(\\)"
            ).findAll(source)
                .map { it.value }
                .filter { request ->
                    request.contains(".data(coverUrl)") || request.contains(".data(requestCoverUrl)")
                }
                .toList()
            assertTrue(imageRequests.isNotEmpty(), "$fileName 应包含封面图片请求")
            assertFalse(
                imageRequests.any { request ->
                    request.contains(".size(") ||
                        (request.contains("size(") && !request.contains("coverRequestSpec?.let"))
                },
                "$fileName 只能通过布局派生的 coverRequestSpec 限制封面解码尺寸"
            )
        }
    }

    @Test
    fun `fixed home avatars and horizontal cover have bounded decode sizes`() {
        val sourceRoot = File("src/main/java/com/android/purebilibili/feature/home")
        val resolvedRoot = if (sourceRoot.exists()) sourceRoot else File("app/${sourceRoot.path}")

        assertTrue(resolvedRoot.resolve("components/HomeTopControls.kt").readText().contains(".size(128, 128)"))
        assertTrue(resolvedRoot.resolve("components/TopBar.kt").readText().contains(".size(128, 128)"))
        assertTrue(resolvedRoot.resolve("components/cards/StoryVideoCard.kt").readText().contains(".size(96, 96)"))
        assertTrue(resolvedRoot.resolve("components/cards/GlassVideoCard.kt").readText().contains(".size(72, 72)"))
        assertTrue(
            resolvedRoot.resolve("components/cards/HomeStyleSingleColumnVideoCard.kt")
                .readText()
                .contains(".size(coverRequestSpec.widthPx, coverRequestSpec.heightPx)"),
        )
    }
}
