package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuSettingsPanelStructureTest {

    @Test
    fun portraitDisplayAreaMode_isOnlyRenderedForPortraitScope() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuSettingsPanel.kt"
        ).readText()

        assertTrue(source.contains("竖屏弹幕显示区域"))
        assertTrue(source.contains("PortraitDanmakuDisplayAreaMode.entries"))
        assertEquals(
            listOf("视频画面", "屏幕顶部"),
            com.android.purebilibili.core.store.PortraitDanmakuDisplayAreaMode.entries
                .map { it.label }
        )
        assertTrue(
            source.contains("settingsScope == DanmakuSettingsScope.PORTRAIT"),
            "竖屏显示区域设置必须只作用于竖屏弹幕设置面板。"
        )
    }
}
