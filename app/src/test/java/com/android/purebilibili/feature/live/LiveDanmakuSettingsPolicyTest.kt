package com.android.purebilibili.feature.live

import com.android.purebilibili.core.store.DanmakuSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDanmakuSettingsPolicyTest {
    @Test
    fun `fullscreen selects font independently from window orientation`() {
        val normal = DanmakuSettings(fontScale = 0.8f, opacity = 0.4f, lineHeight = 1.4f)
        val fullscreen = DanmakuSettings(fontScale = 1.7f, opacity = 0.9f, lineHeight = 2.7f)
        for (landscape in listOf(false, true)) {
            for (isFullscreen in listOf(false, true)) {
                val expected = (if (landscape) fullscreen else normal).copy(
                    fontScale = if (isFullscreen) fullscreen.fontScale else normal.fontScale,
                )
                assertEquals(expected, resolveLiveDanmakuSettings(landscape, isFullscreen, normal, fullscreen))
            }
        }
    }
    @Test
    fun `font previews affect only the selected fullscreen state`() {
        val saved = DanmakuSettings(fontScale = 1.1f, allowColorful = false)
        val preview = mapOf(
            LiveDanmakuStyleOption.NormalFontScale to 0.7f,
            LiveDanmakuStyleOption.FullscreenFontScale to 2.3f,
            LiveDanmakuStyleOption.Opacity to 0.2f,
        )
        assertEquals(saved.copy(fontScale = 0.7f, opacity = 0.2f), applyLiveDanmakuStylePreview(saved, false, preview))
        assertEquals(saved.copy(fontScale = 2.3f, opacity = 0.2f), applyLiveDanmakuStylePreview(saved, true, preview))
        assertEquals(saved, applyLiveDanmakuStylePreview(saved, false, emptyMap()))
    }

    @Test
    fun `advanced preview preserves filters and switches and accepts default values`() {
        val saved = DanmakuSettings(
            fontWeight = 8, strokeWidth = 4f, lineHeight = 2.8f,
            scrollDurationSeconds = 30f, staticDurationSeconds = 20f,
            massiveMode = true, allowScroll = false, blockRules = listOf("屏蔽词"),
        )
        val defaults = DanmakuSettings()
        val preview = mapOf(
            LiveDanmakuStyleOption.FontWeight to defaults.fontWeight.toFloat(),
            LiveDanmakuStyleOption.StrokeWidth to defaults.strokeWidth,
            LiveDanmakuStyleOption.LineHeight to defaults.lineHeight,
            LiveDanmakuStyleOption.ScrollDuration to defaults.scrollDurationSeconds,
            LiveDanmakuStyleOption.StaticDuration to defaults.staticDurationSeconds,
        )
        assertEquals(saved.copy(
            fontWeight = defaults.fontWeight, strokeWidth = defaults.strokeWidth, lineHeight = defaults.lineHeight,
            scrollDurationSeconds = defaults.scrollDurationSeconds, staticDurationSeconds = defaults.staticDurationSeconds,
        ), applyLiveDanmakuStylePreview(saved, false, preview))
    }

    @Test
    fun `stored values acknowledge fonts by viewing state and styles by window direction`() {
        val normal = DanmakuSettings(fontScale = 0.8f, strokeWidth = 1f)
        val fullscreen = DanmakuSettings(fontScale = 1.7f, strokeWidth = 4f)
        for (landscape in listOf(false, true)) {
            assertEquals(0.8f, savedLiveDanmakuStyleValue(LiveDanmakuStyleOption.NormalFontScale, normal, fullscreen, landscape, 360))
            assertEquals(1.7f, savedLiveDanmakuStyleValue(LiveDanmakuStyleOption.FullscreenFontScale, normal, fullscreen, landscape, 360))
            assertEquals(if (landscape) 4f else 1f, savedLiveDanmakuStyleValue(LiveDanmakuStyleOption.StrokeWidth, normal, fullscreen, landscape, 360))
            assertEquals(360f, savedLiveDanmakuStyleValue(LiveDanmakuStyleOption.SuperChatWidth, normal, fullscreen, landscape, 360))
        }
    }

}
