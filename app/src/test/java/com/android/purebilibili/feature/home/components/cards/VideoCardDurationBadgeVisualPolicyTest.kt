package com.android.purebilibili.feature.home.components.cards

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoCardDurationBadgeVisualPolicyTest {

    @Test
    fun `duration badge style uses background instead of text shadow`() {
        val style = resolveVideoCardDurationBadgeVisualStyle()

        assertEquals(0.54f, style.backgroundAlpha, 0.0001f)
    }

    @Test
    fun `home video card text does not use shadow during detail return`() {
        val sourceRoot = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/cards"),
            File("src/main/java/com/android/purebilibili/feature/home/components/cards")
        ).first { it.exists() }
        val sources = listOf(
            "VideoCard.kt",
            "StoryVideoCard.kt",
            "GlassVideoCard.kt",
            "CinematicVideoCard.kt"
        ).joinToString(separator = "\n") { fileName ->
            sourceRoot.resolve(fileName).readText()
        }

        assertFalse(sources.contains("shadow = Shadow("))
        assertFalse(sources.contains("shadow = androidx.compose.ui.graphics.Shadow("))
    }

    @Test
    fun `compact duration badge keeps enough width for mm ss text`() {
        val width = resolveVideoCardDurationBadgeMinWidthDp("02:57")

        assertEquals(40f, width, 0.0001f)
    }

    @Test
    fun `extended duration badge widens for hh mm ss text`() {
        val width = resolveVideoCardDurationBadgeMinWidthDp("1:25:10")

        assertEquals(52f, width, 0.0001f)
    }
}
