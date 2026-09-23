package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuComposerPolicyTest {

    @Test
    fun inlineComposer_usedForLandscapeFullscreenMode() {
        assertTrue(shouldUseInlineDanmakuComposer(isFullscreenMode = true))
        assertFalse(shouldUseInlineDanmakuComposer(isFullscreenMode = false))
    }

    @Test
    fun danmakuInput_shownFrom720dpInFullscreen() {
        assertFalse(shouldShowDanmakuInputInControlBar(isFullscreen = true, widthDp = 479))
        assertFalse(shouldShowDanmakuInputInControlBar(isFullscreen = true, widthDp = 480))
        assertFalse(shouldShowDanmakuInputInControlBar(isFullscreen = true, widthDp = 616))
        assertFalse(shouldShowDanmakuInputInControlBar(isFullscreen = true, widthDp = 719))
        assertTrue(shouldShowDanmakuInputInControlBar(isFullscreen = true, widthDp = 720))
        assertFalse(shouldShowDanmakuInputInControlBar(isFullscreen = false, widthDp = 800))
    }

    @Test
    fun compactDanmakuSend_shownBelow480dpInFullscreen() {
        assertTrue(shouldShowCompactDanmakuSendAction(isFullscreen = true, widthDp = 479))
        assertFalse(shouldShowCompactDanmakuSendAction(isFullscreen = true, widthDp = 480))
        assertFalse(shouldShowCompactDanmakuSendAction(isFullscreen = true, widthDp = 616))
    }

    @Test
    fun danmakuInputCapsule_hiddenWhenSlotIsNarrowerThanAHorizontalPill() {
        assertFalse(shouldDrawDanmakuInputCapsule(availableWidthDp = 40))
        assertFalse(shouldDrawDanmakuInputCapsule(availableWidthDp = 95))
        assertTrue(shouldDrawDanmakuInputCapsule(availableWidthDp = 96))
    }

    @Test
    fun compactDanmakuSend_shownOnTabletInlinePlayer() {
        assertTrue(shouldShowCompactDanmakuSendAction(isFullscreen = false, widthDp = 800))
        assertTrue(shouldShowCompactDanmakuSendAction(isFullscreen = false, widthDp = 600))
        assertFalse(shouldShowCompactDanmakuSendAction(isFullscreen = false, widthDp = 411))
    }

    @Test
    fun moreActionsFallback_whenInlineInputHidden() {
        assertTrue(
            shouldShowDanmakuSendInMoreActions(
                isFullscreen = true,
                showInlineDanmakuInput = false
            )
        )
        assertFalse(
            shouldShowDanmakuSendInMoreActions(
                isFullscreen = true,
                showInlineDanmakuInput = true
            )
        )
    }

    @Test
    fun placeholder_reflectsLoginState() {
        assertTrue(resolveDanmakuInputPlaceholder(isLoggedIn = true).contains("友善"))
        assertTrue(resolveDanmakuInputPlaceholder(isLoggedIn = false).contains("登录"))
    }

    @Test
    fun sendOptions_includeExpectedDefaults() {
        assertEquals(9, danmakuSendColorOptions().size)
        assertEquals(3, danmakuSendModeOptions().size)
        assertEquals(3, danmakuSendFontSizeOptions().size)
    }
}