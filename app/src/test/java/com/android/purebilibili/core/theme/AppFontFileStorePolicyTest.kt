package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
