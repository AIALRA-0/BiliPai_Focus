package com.android.purebilibili.feature.video.ui.gesture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GestureLevelOverlayStructureTest {

    private val source by lazy {
        loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/gesture/GestureLevelOverlay.kt"
        )
    }

    @Test
    fun `miuix feedback uses native animated horizontal slider`() {
        assertTrue(source.contains("import top.yukonga.miuix.kmp.basic.Slider"))
        assertTrue(source.contains("private fun MiuixGestureLevelSlider("))
        assertTrue(source.contains("\n        Slider("))
        assertFalse(source.contains("VerticalSlider("))
        assertTrue(source.contains("Modifier.padding(top = spec.topInsetDp.dp)"))
        assertFalse(source.contains("private fun MiuixGestureLevelRail("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
