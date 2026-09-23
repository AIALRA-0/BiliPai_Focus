package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonListHeaderStructureTest {

    @Test
    fun `layerBackdrop source paints a wallpaper-aware fill so header blur can sample it`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"
        )
        val contentModifier = source
            .substringAfter("val contentModifier = Modifier")
            .substringBefore("Box(modifier = contentModifier)")

        val backdropIndex = contentModifier.indexOf("commonListChromeSource?.modifier")
        val fillIndex = contentModifier.indexOf(
            "globalWallpaperAwareBackground(AppSurfaceTokens.groupedListContainer())"
        )
        assertTrue(backdropIndex >= 0)
        assertTrue(fillIndex >= 0)
        assertTrue(backdropIndex < fillIndex)
    }

    @Test
    fun `header motion layer wraps background and blur layers`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"
        )
        val headerModifier = source
            .substringAfter("// 2. 顶层：悬浮顶栏")
            .substringAfter("modifier = ")
            .substringBefore(".onGloballyPositioned")
        val motionLayerIndex = headerModifier.indexOf(".graphicsLayer")
        val backgroundLayerIndex = headerModifier.indexOf(".then(topBarBackgroundModifier)")

        assertTrue(motionLayerIndex >= 0)
        assertTrue(backgroundLayerIndex >= 0)
        assertTrue(motionLayerIndex < backgroundLayerIndex)
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
