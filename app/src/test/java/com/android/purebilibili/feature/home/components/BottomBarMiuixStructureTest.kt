package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BottomBarMiuixStructureTest {

    @Test
    fun `android native floating branch renders through kernelsu aligned renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val exportOffsetUses = "translationX = exportPanelOffsetPx".toRegex().findAll(source).count()
        val indicatorOffsetUses = "translationX = indicatorPanelOffsetPx".toRegex().findAll(source).count()
        val visibleOffsetUses = "translationX = visiblePanelOffsetPx".toRegex().findAll(source).count()

        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(source.contains("AndroidNativeBottomBarTuning("))
        assertTrue(source.contains("resolveSharedBottomBarCapsuleShape("))
        assertTrue(source.contains("drawBackdrop("))
        assertTrue(source.contains("vibrancy()"))
        assertTrue(source.contains("lens("))
        assertTrue(source.contains("ColorFilter.tint("))
        assertTrue(source.contains("rememberCombinedBackdrop(backdrop, tintedContentBackdrop)"))
        assertTrue(source.contains("val motionProgress by remember"))
        assertTrue(source.contains("Highlight.Default.copy(alpha = motionProgress)"))
        assertTrue(source.contains("val exportPanelOffsetPx by remember"))
        assertTrue(source.contains("val indicatorPanelOffsetPx by remember"))
        assertTrue(source.contains("val visiblePanelOffsetPx by remember"))
        assertTrue(exportOffsetUses == 2)
        assertTrue(indicatorOffsetUses == 1)
        assertTrue(visibleOffsetUses == 2)
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
