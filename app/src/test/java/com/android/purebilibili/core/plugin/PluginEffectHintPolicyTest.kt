package com.android.purebilibili.core.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginEffectHintPolicyTest {

    @Test
    fun frequentMatchHintsFollowUserPreference() {
        assertFalse(shouldShowPluginEffectHint(PluginEffectHintKind.FEED_FILTER, false))
        assertFalse(shouldShowPluginEffectHint(PluginEffectHintKind.DANMAKU, false))
        assertTrue(shouldShowPluginEffectHint(PluginEffectHintKind.FEED_FILTER, true))
        assertTrue(shouldShowPluginEffectHint(PluginEffectHintKind.GENERIC, false))
        assertTrue(shouldShowPluginEffectHint(PluginEffectHintKind.EYE_CARE, false))
    }

    @Test
    fun cooldownRejectsHintsInsideWindow() {
        assertTrue(shouldAcceptPluginEffectHint(lastAcceptedAtMs = null, nowMs = 1_000L, cooldownMs = 5_000L))
        assertFalse(shouldAcceptPluginEffectHint(lastAcceptedAtMs = 1_000L, nowMs = 3_000L, cooldownMs = 5_000L))
        assertTrue(shouldAcceptPluginEffectHint(lastAcceptedAtMs = 1_000L, nowMs = 6_000L, cooldownMs = 5_000L))
        assertTrue(shouldAcceptPluginEffectHint(lastAcceptedAtMs = 1_000L, nowMs = 1_100L, cooldownMs = 0L))
    }

    @Test
    fun enabledHintUsesPluginName() {
        val hint = resolvePluginEnabledEffectHint("eye_protection", "夜间护眼")
        assertEquals("夜间护眼已启用", hint.title)
        assertEquals(PluginEffectHintKind.GENERIC, hint.kind)
    }

    @Test
    fun feedFilterHintUsesSinglePluginName() {
        val hint = resolveFeedFilterEffectHint(3, listOf("去广告增强", "去广告增强"))
        assertEquals("去广告增强已生效", hint?.title)
        assertEquals("已隐藏 3 条内容", hint?.subtitle)
        assertEquals(PLUGIN_EFFECT_HINT_FEED_GROUP_ID, hint?.pluginId)
    }

    @Test
    fun feedFilterHintFallsBackForMultiplePlugins() {
        val hint = resolveFeedFilterEffectHint(5, listOf("去广告增强", "BiliPai 信息流过滤"))
        assertEquals("插件已过滤内容", hint?.title)
    }

    @Test
    fun feedFilterHintIgnoresEmptyResults() {
        assertNull(resolveFeedFilterEffectHint(0, listOf("去广告增强")))
        assertNull(resolveFeedFilterEffectHint(2, emptyList()))
    }

    @Test
    fun activityLabelOnlyWhenEffectIsLive() {
        assertEquals(
            "生效中",
            resolvePluginListActivityLabel(enabled = true, unavailable = false, effectActive = true)
        )
        assertNull(resolvePluginListActivityLabel(enabled = true, unavailable = false, effectActive = false))
        assertNull(resolvePluginListActivityLabel(enabled = false, unavailable = false, effectActive = true))
        assertNull(resolvePluginListActivityLabel(enabled = true, unavailable = true, effectActive = true))
    }

    @Test
    fun eyeProtectionHintMentionsScheduleWhenTimed() {
        val timed = resolveEyeProtectionEffectHint(forceEnabled = false, endHour = 7)
        assertEquals("夜间护眼已开启", timed.title)
        assertTrue(timed.subtitle.orEmpty().contains("07:00"))

        val forced = resolveEyeProtectionEffectHint(forceEnabled = true, endHour = 7)
        assertTrue(forced.subtitle.orEmpty().contains("暖色滤镜"))
    }
}
