package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsItemSearchPolicyTest {
    @Test
    fun textCopySearchOpensTextSettingsAndKeepsItsStableId() {
        val result = resolveSettingsSearchResults("点按文字复制").first()
        assertEquals("animation.global_text_tap_copy_enabled", result.settingId)
        assertEquals(SettingsSearchTarget.APPEARANCE, result.target)
        assertEquals("appearance_text_size", result.focusId)
        assertEquals("设置 / 外观与动画 / 文字与大小", result.path)
        assertEquals(BiliPaiNavKey.AppearanceSettings, resolveSettingsSearchNavigation(result))
    }

    @Test
    fun directoryIdsAreUniqueAndHaveConcreteDestinations() {
        assertEquals(playbackSettingDirectory.size, playbackSettingDirectory.map { it.settingId }.toSet().size)
        playbackSettingDirectory.forEach { item ->
            val result = resolveSettingsSearchResults(item.title, Int.MAX_VALUE).first { it.settingId == item.settingId }
            assertEquals(item.title, result.title)
            assertEquals(item.sectionKey, result.focusId)
            assertEquals(BiliPaiNavKey.SettingsCategory(item.page), resolveSettingsSearchNavigation(result))
        }
    }

    @Test
    fun autoplayKeepsMultipleOptionsOnTheSamePage() {
        val results = resolveSettingsSearchResults("自动播放", Int.MAX_VALUE)
        assertTrue(results.any { it.settingId == "playback.click_to_play_enabled" })
        assertTrue(results.any { it.settingId == "playback.auto_play_enabled" })
        assertEquals(results.size, results.map { it.settingId }.toSet().size)
    }

    @Test
    fun exactTitleWinsAndNaturalSentenceKeepsItsItemId() {
        assertEquals("playback.video_note_enabled", resolveSettingsSearchResults("显示视频笔记").first().settingId)
        assertEquals("playback.video_note_default_collapsed", resolveSettingsSearchResults("怎么关闭默认折叠视频笔记").first().settingId)
    }

    @Test
    fun renamedTechnicalAndChineseTermsRemainSearchable() {
        assertEquals("playback.image_preview3d_page_enabled", resolveSettingsSearchResults("图片立体翻页3D").first().settingId)
        assertTrue(resolveSettingsSearchResults("SHA-256").isNotEmpty())
        assertTrue(resolveSettingsSearchResults("sha256").isNotEmpty())
        assertTrue(resolveSettingsSearchResults("CDN").isNotEmpty())
        assertTrue(resolveSettingsSearchResults("zimu").isNotEmpty())
    }

    @Test
    fun hardwareDecoderOpensItsIndependentPage() {
        val result = resolveSettingsSearchResults("启用硬件解码").first()
        assertEquals(SettingsRootCategory.VIDEO_DECODER, result.page)
        assertEquals("设置 / 播放设置 / 视频解码", result.path)
    }

    @Test
    fun focusClearingCannotDiscardANewerSelection() {
        SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_SPEED, "first")
        val first = SettingsSearchFocusController.request.value!!
        SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_FULLSCREEN, "second")
        SettingsSearchFocusController.clear(first.token)
        assertEquals("second", SettingsSearchFocusController.request.value?.settingId)
        SettingsSearchFocusController.clear()
    }
    @Test
    fun partResumeSearchOpensTheSameSectionAsItsVisibleSwitch() {
        listOf("定位分P", "上次看到", "显示继续播放分P提示").forEach { query ->
            val result = resolveSettingsSearchResults(query, Int.MAX_VALUE)
                .first { it.settingId == "playback.show_continue_part_notice" }
            assertEquals(SettingsRootCategory.PLAYBACK_QUALITY, result.page)
            assertEquals(SettingsSearchFocusIds.PLAYBACK_INTERACTION, result.focusId)
            assertTrue(result.path.endsWith("字幕与连播"))
        }
    }

}
