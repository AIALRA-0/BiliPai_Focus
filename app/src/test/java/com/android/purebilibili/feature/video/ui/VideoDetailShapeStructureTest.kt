package com.android.purebilibili.feature.video.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailShapeStructureTest {

    @Test
    fun `detail shapes use theme semantics instead of hardcoded corner radii`() {
        val source = loadMainSource("feature/video/ui/VideoDetailShapes.kt")

        listOf("shapes.small", "shapes.medium", "shapes.large", "shapes.extraLarge")
            .forEach { token -> assertTrue(source.contains(token), "Missing MD3 token: $token") }
        assertTrue(source.contains("AppUiStyle.MATERIAL3"))
        assertTrue(source.contains("AppUiStyle.MIUIX"))
        assertTrue(source.contains("AppShapes.container"))
        assertFalse(source.contains("RoundedCornerShape"))
        assertFalse(source.contains(".dp"))
    }

    @Test
    fun `detail surfaces share the semantic shape palette`() {
        val related = loadMainSource("feature/video/ui/components/RelatedVideoItem.kt")
        val skeleton = loadMainSource("feature/video/ui/components/SkeletonComponents.kt")
        val collection = loadMainSource("feature/video/ui/components/CollectionRow.kt")
        val info = loadMainSource("feature/video/ui/section/VideoInfoSection.kt")
        val summary = loadMainSource("feature/video/ui/section/AiSummarySection.kt")
        val note = loadMainSource("feature/video/ui/section/VideoNoteSection.kt")

        assertTrue(related.contains("VideoDetailShapes.contentCard()"))
        assertTrue(related.contains("VideoDetailShapes.media()"))
        assertTrue(skeleton.contains("VideoDetailShapes.contentCard()"))
        assertTrue(skeleton.contains("VideoDetailShapes.media()"))
        assertTrue(collection.contains("VideoDetailShapes.compactIcon()"))
        assertTrue(info.contains("VideoDetailShapes.action()"))
        assertTrue(summary.contains("VideoDetailShapes.leadingIcon()"))
        assertTrue(note.contains("VideoDetailShapes.leadingIcon()"))
        assertTrue(note.contains("VideoDetailShapes.field()"))
    }

    private fun loadMainSource(relativePath: String): String {
        return listOf(
            File("src/main/java/com/android/purebilibili/$relativePath"),
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }.readText()
    }
}
