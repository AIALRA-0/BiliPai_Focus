package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.SettingsManager
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedSettingsSelectionPolicyTest {

    @Test
    fun `resolveFeedApiSegmentOptions should preserve enum order and labels`() {
        val options = resolveFeedApiSegmentOptions()

        assertEquals(SettingsManager.FeedApiType.entries.size, options.size)
        assertEquals(SettingsManager.FeedApiType.WEB, options[0].value)
        assertEquals("网页端 (Web)", options[0].label)
        assertEquals(SettingsManager.FeedApiType.MOBILE, options[1].value)
        assertEquals("移动端 (App)", options[1].label)
    }

    @Test
    fun `home refresh count summary explains request cap instead of guaranteed visible count`() {
        assertEquals(
            "单次最多请求 20 条推荐内容，实际显示可能更少",
            resolveHomeRefreshCountSummary(20)
        )
    }

    @Test
    fun `home refresh slider uses compact supported range`() {
        assertEquals(10f, resolveHomeRefreshSliderRange().start, 0.001f)
        assertEquals(30f, resolveHomeRefreshSliderRange().endInclusive, 0.001f)
        assertEquals(19, resolveHomeRefreshSliderSteps())
    }

    @Test
    fun `settings cards stay opaque over wallpaper text`() {
        val source = locate("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
            .readText()

        assertTrue(source.contains("darkTintBase.compositeOver(baseCardContainer)"))
        assertFalse(source.contains("copy(alpha = 0.96f)"))
    }

    private fun locate(path: String): File {
        return listOf(File(path), File("app/$path"))
            .firstOrNull { it.exists() }
            ?: error("Cannot locate $path from cwd")
    }
}
