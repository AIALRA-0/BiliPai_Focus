package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HomeUpSpaceNavigationStructureTest {

    @Test
    fun homeFeedPassesUpClickFromScreenToOrdinaryVideoCards() {
        val screenSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")

        assertTrue(screenSource.contains("val onHomeFeedUpClick = remember(onSpaceClick)"))
        assertTrue(screenSource.contains("onUpClick = onHomeFeedUpClick"))
        assertTrue(pageSource.contains("onUpClick: (Long) -> Unit = {}"))
        assertTrue(pageSource.contains("ElegantVideoCard("))
        assertTrue(pageSource.contains("StoryVideoCard("))
        assertTrue(pageSource.contains("onUpClick = onUpClick"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
