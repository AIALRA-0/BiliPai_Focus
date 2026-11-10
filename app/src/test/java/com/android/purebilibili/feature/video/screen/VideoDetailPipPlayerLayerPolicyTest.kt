package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 系统画中画（PiP）只承接播放面：非全屏详情布局（手机内联、平板分栏、折叠屏分栏）
 * 不应整页缩进小窗，竖屏流的装饰层也不应叠加到小窗。详见 issue #973。
 *
 * 这里只做源码级结构校验：PiP 必须把主内容切到与横屏全屏共用的播放层，
 * 而 PortraitVideoPager 在小窗里只保留播放面、隐藏标题 / 互动 / 进度装饰。
 */
class VideoDetailPipPlayerLayerPolicyTest {

    private fun mainContentSource(): String {
        val candidates = listOf(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt",
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt",
        )
        return candidates.first { File(it).exists() }.let { File(it).readText() }
    }

    private fun pagerSource(): String {
        val candidates = listOf(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitVideoPager.kt",
            "src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitVideoPager.kt",
        )
        return candidates.first { File(it).exists() }.let { File(it).readText() }
    }

    private fun mainContentBlock(source: String): String =
        source.substring(
            source.indexOf("fun BoxScope.VideoDetailRouteSheetMainContent()"),
            source.indexOf("VideoDetailRouteSheetOverlayContent"),
        )

    @Test
    fun `pip forces the player-only fullscreen layer for non portrait-fullscreen states`() {
        val block = mainContentBlock(mainContentSource())

        assertTrue(
            block.contains("val pipPlayerOnlyLayer = isPipMode && !isPortraitFullscreen"),
            "PiP 在非竖屏全屏时应切到全屏播放层，不得沿用内联 / 平板布局",
        )
        assertTrue(
            block.contains("if (isFullscreenMode || pipPlayerOnlyLayer)"),
            "PiP 必须与横屏全屏共用同一播放面渲染分支",
        )
    }

    @Test
    fun `pip keeps tablet split and phone inline layouts out of the fullscreen branch`() {
        val block = mainContentBlock(mainContentSource())

        // 主内容里的内联 / 平板分支只在非全屏 + 非 PiP 时进入。
        assertFalse(
            block.contains("} else if (isPipMode)"),
            "PiP 不得新增专用分支去渲染整页详情，必须直接走全屏播放层",
        )
    }

    @Test
    fun `pip uses a black backdrop so window edges never leak the page background`() {
        val source = mainContentSource()
        assertTrue(
            source.contains("isFullscreenMode = isFullscreenMode || isPipMode"),
            "PiP 整窗即播放面，底板应与全屏一致为黑色",
        )
    }

    @Test
    fun `portrait pager keeps the shared player surface but hides chrome in pip`() {
        val source = mainContentSource()
        assertTrue(
            source.contains("isInPipMode = isPipMode,") &&
                source.contains("VideoDetailPortraitOverlayAdapter("),
            "竖屏全屏进入 PiP 时需把状态传给 PortraitVideoPager，pager 保持挂载、只隐藏装饰层",
        )
    }

    @Test
    fun `portrait pager skips overlay chrome subtitle and next-watch while leaving the video surface mounted`() {
        val source = pagerSource()

        assertTrue(
            source.contains("isInPipMode: Boolean = false,") &&
                source.contains("isInPipMode = isInPipMode,") &&
                source.contains("/** 系统小窗：只保留播放面与弹幕"),
            "PiP 状态必须贯穿 PortraitVideoPager -> VideoPageItem",
        )
        assertEquals(
            2,
            source.split("if (!isInPipMode) {").size - 1,
            "PiP 应分别隐藏装饰层和字幕，保留共享播放面",
        )

        // 下一集提示与播放器一致在小窗里关闭。
        assertTrue(source.contains("eligible = isPortraitPlaybackAllowed && !isInPipMode && !isLoading"))

        // 弹幕沿用播放器「小窗和画中画中不显示弹幕」设置。
        assertTrue(
            source.contains("val pipDanmakuVisible = !(isInPipMode && pipNoDanmakuEnabled)") &&
                source.contains("danmakuEnabled = danmakuEnabled && pipDanmakuVisible,"),
            "竖屏流 PiP 弹幕需遵循同一套 pipNoDanmaku 设置",
        )
    }
}
