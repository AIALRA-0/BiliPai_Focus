package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.FullscreenAspectRatio
import com.android.purebilibili.core.store.FullscreenMode
import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSettingsSelectionPolicyTest {

    @Test
    fun `playback interaction and fullscreen blocks should be split into scene composables`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt"
        )

        assertTrue(source.contains("private fun PlaybackInteractionSettingsSection("))
        assertTrue(source.contains("private fun PlaybackFullscreenGestureSettingsSection("))
        val contentBlock = source
            .substringAfter("fun PlaybackSettingsContent(")
            .substringBefore("private fun PlaybackInteractionSettingsSection(")
        assertTrue(contentBlock.contains("IOSSectionTitle(\"互动与评论\")"))
        assertTrue(contentBlock.contains("IOSSectionTitle(\"全屏与手势\")"))
        assertTrue(contentBlock.contains("PlaybackInteractionSettingsSection("))
        assertTrue(contentBlock.contains("PlaybackFullscreenGestureSettingsSection("))
    }

    @Test
    fun `comment controls should stay in interaction section instead of fullscreen gesture section`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt"
        )

        val interactionBlock = source
            .substringAfter("private fun PlaybackInteractionSettingsSection(")
            .substringBefore("PlaybackFullscreenGestureSettingsSection(")
        val fullscreenBlock = source
            .substringAfter("private fun PlaybackFullscreenGestureSettingsSection(")

        listOf("评论回复预览", "评论发送检测", "评论区个性装扮", "图片长按保存").forEach { title ->
            assertTrue(interactionBlock.contains(title))
            assertFalse(fullscreenBlock.contains(title))
        }
    }

    @Test
    fun `fullscreen gesture section should own its setting state collection`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt"
        )

        val contentBlock = source
            .substringAfter("fun PlaybackSettingsContent(")
            .substringBefore("private fun PlaybackInteractionSettingsSection(")

        assertTrue(source.contains("private fun PlaybackFullscreenGestureSettingsSection(\n    context: Context"))
        assertTrue(contentBlock.contains("PlaybackFullscreenGestureSettingsSection(context = context)"))
        assertFalse(contentBlock.contains("getFullscreenMode(context)"))
        assertFalse(contentBlock.contains("getAppGestureScreenshotEnabled(context)"))
        assertFalse(contentBlock.contains("getPortraitPlayerCollapseMode(context)"))
    }

    @Test
    fun `interaction section should own playback and comment setting state collection`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt"
        )

        val contentBlock = source
            .substringAfter("fun PlaybackSettingsContent(")
            .substringBefore("private fun PlaybackInteractionSettingsSection(")

        assertTrue(source.contains("private fun PlaybackInteractionSettingsSection(\n    context: Context"))
        assertTrue(contentBlock.contains("PlaybackInteractionSettingsSection("))
        assertTrue(contentBlock.contains("context = context"))
        assertTrue(contentBlock.contains("state = state"))
        assertTrue(contentBlock.contains("viewModel = viewModel"))
        assertFalse(contentBlock.contains("getAutoPlay(context)"))
        assertFalse(contentBlock.contains("getSubtitleAutoPreference(context)"))
        assertFalse(contentBlock.contains("getCommentFraudDetectionEnabled(context)"))
    }

    @Test
    fun `resolveSelectionIndex should return matched option index`() {
        val options = listOf(
            PlaybackSegmentOption("avc1", "AVC"),
            PlaybackSegmentOption("hev1", "HEVC"),
            PlaybackSegmentOption("av01", "AV1")
        )

        assertEquals(1, resolveSelectionIndex(options, "hev1"))
    }

    @Test
    fun `resolveSelectionIndex should fallback to first option when value missing`() {
        val options = listOf(
            PlaybackSegmentOption(116, "1080P60"),
            PlaybackSegmentOption(80, "1080P"),
            PlaybackSegmentOption(64, "720P")
        )

        assertEquals(0, resolveSelectionIndex(options, 32))
        assertEquals("720P", resolveSelectionLabel(options, 64, fallbackLabel = "默认"))
        assertEquals("默认", resolveSelectionLabel(options, 32, fallbackLabel = "默认"))
    }

    @Test
    fun `md3 segmented labels should shrink for crowded language options`() {
        assertEquals(
            14f,
            resolveMd3SegmentedLabelFontSizeSp(
                optionCount = 4,
                longestLabelLength = "English".length
            ),
            0.001f
        )
        assertEquals(
            16f,
            resolveMd3SegmentedLabelFontSizeSp(
                optionCount = 3,
                longestLabelLength = "HEVC".length
            ),
            0.001f
        )
    }

    @Test
    fun `ios liquid segmented control default label size matches tall indicator`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/IOSSlidingSegmentedControl.kt")

        assertTrue(source.contains("labelFontSize: TextUnit = 14.sp"))
        assertFalse(source.contains("labelFontSize: TextUnit = 12.sp"))
        assertEquals(
            13f,
            resolveMd3SegmentedLabelFontSizeSp(
                optionCount = 5,
                longestLabelLength = "跟随系统".length
            ),
            0.001f
        )
    }

    @Test
    fun `android native liquid glass opt in makes shared ios segmented control use liquid indicator`() {
        assertEquals(
            IosSlidingSegmentedControlChrome.MD3_SEGMENTED,
            resolveIosSlidingSegmentedControlChrome(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = false
            )
        )
        assertEquals(
            IosSlidingSegmentedControlChrome.LIQUID_INDICATOR,
            resolveIosSlidingSegmentedControlChrome(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )
        assertEquals(
            IosSlidingSegmentedControlChrome.LIQUID_INDICATOR,
            resolveIosSlidingSegmentedControlChrome(
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `resolveEffectiveMobileQuality should clamp to 480p when data saver active`() {
        assertEquals(32, resolveEffectiveMobileQuality(rawMobileQuality = 80, isDataSaverActive = true))
        assertEquals(16, resolveEffectiveMobileQuality(rawMobileQuality = 16, isDataSaverActive = true))
        assertEquals(80, resolveEffectiveMobileQuality(rawMobileQuality = 80, isDataSaverActive = false))
    }

    @Test
    fun `resolveDefaultPlaybackQualityOptions should only expose fixed quality tiers`() {
        val options = resolveDefaultPlaybackQualityOptions()

        assertEquals(listOf(125, 116, 80, 64, 32, 16), options.map { it.value })
        assertEquals(listOf("4K HDR", "1080P60", "1080P", "720P", "480P", "360P"), options.map { it.label })
    }

    @Test
    fun `resolveDefaultQualitySubtitle should warn non vip users about automatic 1080p fallback`() {
        assertEquals(
            "非大会员将自动以 1080P 起播",
            resolveDefaultQualitySubtitle(
                rawQuality = 116,
                fallbackSubtitle = "仅 WiFi 环境生效",
                isLoggedIn = true,
                isVip = false
            )
        )
    }

    @Test
    fun `resolveDefaultQualitySubtitle should warn guests about automatic 720p fallback`() {
        assertEquals(
            "未登录时将自动以 720P 起播",
            resolveDefaultQualitySubtitle(
                rawQuality = 116,
                fallbackSubtitle = "仅 WiFi 环境生效",
                isLoggedIn = false,
                isVip = false
            )
        )
    }

    @Test
    fun `resolveDefaultQualitySubtitle should keep fallback subtitle for playable tiers`() {
        assertEquals(
            "仅 WiFi 环境生效",
            resolveDefaultQualitySubtitle(
                rawQuality = 80,
                fallbackSubtitle = "仅 WiFi 环境生效",
                isLoggedIn = true,
                isVip = false
            )
        )
    }

    @Test
    fun `resolveSegmentedSwipeTargetIndex should switch to adjacent option when drag exceeds threshold`() {
        assertEquals(
            3,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = 42f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
        assertEquals(
            1,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = -45f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
        assertEquals(
            2,
            resolveSegmentedSwipeTargetIndex(
                currentIndex = 2,
                totalDragPx = 10f,
                optionCount = 5,
                thresholdPx = 30f
            )
        )
    }

    @Test
    fun `resolveFullscreenModeSegmentOptions should expose only primary modes`() {
        val modes = resolveFullscreenModeSegmentOptions().map { it.value }
        assertEquals(
            listOf(
                FullscreenMode.AUTO,
                FullscreenMode.NONE,
                FullscreenMode.VERTICAL,
                FullscreenMode.HORIZONTAL
            ),
            modes
        )
    }

    @Test
    fun `resolveFullscreenAspectRatioSegmentOptions should expose fixed fullscreen ratios`() {
        val ratios = resolveFullscreenAspectRatioSegmentOptions().map { it.value }
        assertEquals(
            listOf(
                FullscreenAspectRatio.FIT,
                FullscreenAspectRatio.FILL,
                FullscreenAspectRatio.RATIO_16_9,
                FullscreenAspectRatio.RATIO_4_3,
                FullscreenAspectRatio.STRETCH
            ),
            ratios
        )
    }

    @Test
    fun `resolvePortraitPlayerCollapseModeSegmentOptions should expose orientation strategy modes`() {
        val modes = resolvePortraitPlayerCollapseModeSegmentOptions().map { it.value }
        assertEquals(
            listOf(
                PortraitPlayerCollapseMode.OFF,
                PortraitPlayerCollapseMode.INTRO_ONLY,
                PortraitPlayerCollapseMode.COMMENT_ONLY,
                PortraitPlayerCollapseMode.BOTH,
                PortraitPlayerCollapseMode.PAUSED_ONLY
            ),
            modes
        )
        assertEquals(
            listOf("关闭", "竖屏", "横屏", "全部", "暂停时"),
            resolvePortraitPlayerCollapseModeSegmentOptions().map { it.label }
        )
    }

    @Test
    fun `fullscreen swipe seek setting should use adaptive switch style`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()
        val block = source
            .substringAfter("text = \"横屏滑动调进度范围\"")
            .substringBefore("val seekStepOptions = listOf(")

        assertTrue(block.contains("AppAdaptiveSwitch("))
        assertFalse(Regex("""(?m)^\s*Switch\(""").containsMatchIn(block))
    }

    @Test
    fun `playback settings exposes interactive command danmaku hiding switch`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()

        assertTrue(source.contains("隐藏视频内互动提示"))
        assertTrue(source.contains("getDanmakuHideInteractiveCommands"))
        assertTrue(source.contains("setDanmakuHideInteractiveCommands"))
    }

    @Test
    fun `playback settings exposes double tap seek switch and seconds`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()

        assertTrue(source.contains("双击跳转"))
        assertTrue(source.contains("getDoubleTapSeekEnabled"))
        assertTrue(source.contains("setDoubleTapSeekEnabled"))
        assertTrue(source.contains("getSeekForwardSeconds"))
        assertTrue(source.contains("setSeekForwardSeconds"))
        assertTrue(source.contains("getSeekBackwardSeconds"))
        assertTrue(source.contains("setSeekBackwardSeconds"))
    }

    @Test
    fun `playback settings exposes quality downgrade dialog switches`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()

        assertTrue(source.contains("画质降档诊断弹窗"))
        assertTrue(source.contains("降档弹窗仅提示一次"))
        assertTrue(source.contains("getQualitySwitchFailureDialogEnabled"))
        assertTrue(source.contains("setQualitySwitchFailureDialogEnabled"))
        assertTrue(source.contains("getQualitySwitchFailureDialogOnceEnabled"))
        assertTrue(source.contains("setQualitySwitchFailureDialogOnceEnabled"))
    }

    @Test
    fun `playback settings exposes video note switches with collapse gated by enabled`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()

        assertTrue(source.contains("显示视频笔记"))
        assertTrue(source.contains("默认折叠视频笔记"))
        assertTrue(source.contains("getVideoNoteEnabled"))
        assertTrue(source.contains("setVideoNoteEnabled"))
        assertTrue(source.contains("getVideoNoteDefaultCollapsed"))
        assertTrue(source.contains("setVideoNoteDefaultCollapsed"))

        val noteSwitchBlock = source
            .substringAfter("title = \"显示视频笔记\"")
            .substringBefore("title = \"默认折叠视频笔记\"")
        assertTrue(noteSwitchBlock.contains("if (videoNoteEnabled)"))
    }

    @Test
    fun `playback settings clarifies auto highest does not treat video cap as failure`() {
        val source = File("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
            .readText()

        assertTrue(source.contains("视频实际最高可播"))
        assertTrue(source.contains("视频本身无更高档不打断播放"))
        assertTrue(source.contains("默认画质会作为关闭后的偏好保留"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
