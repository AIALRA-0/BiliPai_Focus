package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkedBottomDockStructureTest {

    @Test
    fun dockKeepsChildrenMountedAndDefersMergeReads() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/LinkedBottomDock.kt"
        )
        assertTrue(source.contains("mergeProgressProvider"))
        assertTrue(source.contains("searchProgressProvider"))
        assertTrue(source.contains("nowPlayingContent?.invoke("))
        assertTrue(source.contains("navigationContent()"))
        assertFalse(source.contains("if (merge.value < 0.999f)"))
        assertFalse(source.contains("if (merge.value > 0.001f)"))
        assertFalse(source.contains("merge.value.coerceIn(0f, 1f),"))
        assertFalse(source.contains("search.value.coerceIn(0f, 1f),"))
        assertFalse(source.contains("fieldAlpha = search.value"))
    }

    @Test
    fun searchVisualReadsAlphaAndScaleInDraw() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        )
        val visual = source.substringAfter("internal fun BiliPaiBottomBarSearchVisualContent(")
        assertTrue(visual.contains("iconScale: () -> Float"))
        assertTrue(visual.contains("fieldAlpha: () -> Float"))
        assertTrue(visual.contains("val scale = iconScale()"))
        assertTrue(visual.contains("graphicsLayer { alpha = fieldAlpha() }"))
        assertFalse(visual.contains(".alpha(fieldAlpha"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
