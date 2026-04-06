package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsMiuixSimplificationStructureTest {

    @Test
    fun `appearance settings restore ui preset selection while keeping miuix scaffold`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")

        assertTrue(source.contains("resolveUiPresetSegmentOptions("))
        assertTrue(source.contains("viewModel.setUiPreset("))
        assertTrue(source.contains("MiuixScaffold("))
        assertTrue(source.contains("MiuixSmallTopAppBar("))
    }

    @Test
    fun `animation settings no longer expose transparency preview controls`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt")

        assertFalse(source.contains("previewLiquidGlassProgress"))
        assertFalse(source.contains("通透到磨砂"))
        assertTrue(source.contains("液态玻璃"))
        assertTrue(source.contains("MiuixScaffold("))
        assertTrue(source.contains("MiuixSmallTopAppBar("))
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
