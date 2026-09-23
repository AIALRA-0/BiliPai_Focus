package com.android.purebilibili.feature.live

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePlayerControlsStructureTest {

    private val source by lazy {
        loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/components/LivePlayerControls.kt"
        )
    }

    @Test
    fun `player icon button requires accessibility and state inputs`() {
        assertTrue(source.contains("fun LivePlayerIconButton("))
        assertTrue(source.contains("label: String,"))
        assertTrue(source.contains("selected: Boolean,"))
        assertTrue(source.contains("enabled: Boolean,"))
        assertTrue(source.contains("require(label.isNotBlank())"))
        assertTrue(source.contains("contentDescription = label"))
        assertTrue(source.contains("this.selected = selected"))
        assertTrue(source.contains("this.stateDescription = stateDescription"))
        assertFalse(source.contains("fun PlayerIconBtn("))
    }

    @Test
    fun `player icon button separates touch and visual token sizes`() {
        assertTrue(source.contains("resolveLiveVisualSpec(playerChromeProfile.tabPresentation)"))
        assertTrue(source.contains("visualSpec.playerButtonTouchTargetDp.dp"))
        assertTrue(source.contains("visualSpec.playerButtonVisualSizeDp.dp"))
        assertTrue(source.contains(".size(touchTargetSize)"))
        assertTrue(source.contains(".size(visualSize)"))
        assertTrue(source.contains("AppSpacingTokens."))
        assertTrue(source.contains("AppShapes.container(ContainerLevel.Pill)"))
        assertTrue(source.contains("shape = CircleShape"))
        assertFalse(source.contains("modifier = Modifier.size(34.dp)"))
        assertFalse(source.contains("RoundedCornerShape("))
    }

    @Test
    fun `brightness and volume feedback reuse the ordinary video edge host`() {
        assertTrue(source.contains("GestureLevelOverlayHost("))
        assertFalse(source.contains("GestureLevelOverlayContent("))
    }

    @Test
    fun `all icon button call sites provide localized labels and required state`() {
        val expectedLabels = listOf(
            "返回",
            "进入画中画",
            "仅听声音",
            "后台播放",
            "定时关闭",
            "播放信息",
            "刷新直播",
            "发送弹幕",
            "屏蔽设置",
            "弹幕设置",
            "锁定",
            "截图",
            "解锁",
        )

        expectedLabels.forEach { label ->
            assertTrue(source.contains("label = \"$label\""), "Missing label: $label")
        }
        assertEquals(15, Regex("""LivePlayerIconButton\(""").findAll(source).count() - 1)
        assertEquals(15, Regex("""\blabel\s*=""").findAll(source).count())
        assertEquals(15, Regex("""\benabled\s*=\s*true""").findAll(source).count())
        assertTrue(source.contains("label = if (isPlaying) \"暂停\" else \"播放\""))
        assertTrue(source.contains("label = if (isFullscreen) \"退出全屏\" else \"进入全屏\""))
        assertTrue(source.contains("selected = isAudioOnly"))
        assertTrue(source.contains("selected = isBackgroundPlaybackEnabled"))
        assertTrue(source.contains("stateDescription = if (isAudioOnly) \"已开启\" else \"已关闭\""))
        assertTrue(source.contains("stateDescription = if (isBackgroundPlaybackEnabled) \"已开启\" else \"已关闭\""))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
