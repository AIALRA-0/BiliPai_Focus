package com.android.purebilibili.core.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppFontFileStorePolicyTest {

    @Test
    fun `font import keeps supported local file extensions`() {
        assertEquals("custom_app_font.ttf", buildStoredAppFontFileName("霞鹜文楷.ttf"))
        assertEquals("custom_app_font.otf", buildStoredAppFontFileName("MiSans.otf"))
        assertEquals("custom_app_font.ttc", buildStoredAppFontFileName("PingFang.ttc"))
    }

    @Test
    fun `font import falls back to ttf extension for unknown picker names`() {
        assertEquals("custom_app_font.ttf", buildStoredAppFontFileName("download.bin"))
        assertEquals("custom_app_font.ttf", buildStoredAppFontFileName(null))
    }

    @Test
    fun `font display name falls back to local font label`() {
        assertEquals("本地字体", sanitizeAppFontDisplayName(" "))
        assertEquals("MiSans.ttf", sanitizeAppFontDisplayName(" MiSans.ttf "))
    }

    @Test
    fun `imported font defaults to full coverage and keeps cjk flag`() {
        val covered = ImportedAppFontFile(fileName = "custom_app_font.ttf", displayName = "MiSans.ttf")
        assertEquals(true, covered.coversCjk)

        val partial = covered.copy(coversCjk = false)
        assertEquals(false, partial.coversCjk)
    }

    @Test
    fun `coverage notice only warns when cjk glyphs are missing`() {
        assertEquals(null, resolveAppFontCoverageNotice(coversCjk = true))
        assertEquals("该字体缺少中文字形，中文将回退系统字体", resolveAppFontCoverageNotice(coversCjk = false))
    }

    @Test
    fun `font import probes glyph coverage with injectable probe`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/theme/AppFontFileStore.kt"
        )
        assertTrue(source.contains("glyphProbe: AppFontGlyphProbe = DefaultAppFontGlyphProbe"))
        // 探测失败不能阻断导入,按覆盖完整处理。
        assertTrue(source.contains(".getOrDefault(true)"))
        val screen = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt"
        )
        assertTrue(screen.contains("resolveAppFontCoverageNotice(imported.coversCjk)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).first { it.exists() }.readText()
    }
}
