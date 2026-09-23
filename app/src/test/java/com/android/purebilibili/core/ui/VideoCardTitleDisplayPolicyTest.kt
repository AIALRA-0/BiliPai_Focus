package com.android.purebilibili.core.ui

import androidx.compose.ui.text.style.TextOverflow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoCardTitleDisplayPolicyTest {

    @Test
    fun truncatedTitlesStayAtTwoLines() {
        assertEquals(2, resolveVideoCardTitleMaxLines(showFullCardContent = false))
        assertEquals(TextOverflow.Ellipsis, resolveVideoCardTitleOverflow(showFullCardContent = false))
    }

    @Test
    fun fullCardContentExpandsTitles() {
        assertEquals(Int.MAX_VALUE, resolveVideoCardTitleMaxLines(showFullCardContent = true))
        assertEquals(TextOverflow.Visible, resolveVideoCardTitleOverflow(showFullCardContent = true))
    }

    @Test
    fun truncatedMaxLinesCanStayStricterThanDefault() {
        assertEquals(1, resolveVideoCardTitleMaxLines(showFullCardContent = false, truncatedMaxLines = 1))
        assertEquals(Int.MAX_VALUE, resolveVideoCardTitleMaxLines(showFullCardContent = true, truncatedMaxLines = 1))
    }

    @Test
    fun appNavigationProvidesTheSettingGlobally() {
        val source = loadSource("navigation/AppNavigation.kt")
        assertTrue(source.contains("LocalFullVideoCardContentVisible provides"))
        assertTrue(source.contains("homeSettings.showFullVideoCardContent"))
    }

    private fun loadSource(relativePath: String): String {
        val path = "src/main/java/com/android/purebilibili/$relativePath"
        return listOf(File(path), File("app/$path")).first(File::exists).readText()
    }
}
