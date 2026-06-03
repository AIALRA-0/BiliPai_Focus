package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class LivePiliPlusVisualPolicyTest {

    @Test
    fun `home metrics mirror PiliPlus live constants`() {
        val metrics = resolveLivePiliPlusHomeMetrics()

        assertEquals(12, metrics.safeSpaceDp)
        assertEquals(8, metrics.cardSpaceDp)
        assertEquals(10, metrics.cardRadiusDp)
        assertEquals(16f / 10f, metrics.coverAspectRatio)
        assertEquals(45, metrics.followAvatarSizeDp)
        assertEquals(70, metrics.followItemExtentDp)
    }

    @Test
    fun `mobile grid keeps two columns and expanded grid follows PiliPlus max extent`() {
        assertEquals(2, resolveLivePiliPlusGridColumns(widthDp = 390, isExpandedScreen = false))
        assertEquals(3, resolveLivePiliPlusGridColumns(widthDp = 720, isExpandedScreen = true))
        assertEquals(4, resolveLivePiliPlusGridColumns(widthDp = 1100, isExpandedScreen = true))
    }

    @Test
    fun `chip colors use theme accent for selected home category`() {
        val colors = resolveLivePiliPlusChipColors(
            selectedContainer = Color(0xFF8FD5FF),
            selectedContent = Color(0xFF001F2A),
            unselectedContent = Color(0xFF49454F)
        )

        assertEquals(Color(0xFF8FD5FF), colors.selectedContainerColor)
        assertEquals(Color(0xFF001F2A), colors.selectedContentColor)
        assertEquals(Color.Transparent, colors.unselectedContainerColor)
        assertEquals(Color(0xFF49454F), colors.unselectedContentColor)
    }

    @Test
    fun `overlay chat bubble removes black shadow background`() {
        val dark = resolveLivePiliPlusChatBubbleTokens(isOverlay = true, isDark = true)
        val light = resolveLivePiliPlusChatBubbleTokens(isOverlay = true, isDark = false)

        assertEquals(14, dark.cornerRadiusDp)
        assertEquals(10, dark.horizontalPaddingDp)
        assertEquals(4, dark.verticalPaddingDp)
        assertEquals(14, dark.fontSizeSp)
        assertEquals(0f, dark.backgroundAlpha)
        assertEquals(0.90f, dark.nameAlpha)
        assertEquals(0f, light.backgroundAlpha)
        assertEquals(0.90f, light.nameAlpha)
    }

    @Test
    fun `blank live danmaku is hidden unless it carries an image emoticon`() {
        assertEquals(false, shouldRenderLiveDanmaku(text = "", emoticonUrl = null))
        assertEquals(false, shouldRenderLiveDanmaku(text = "   ", emoticonUrl = ""))
        assertEquals(true, shouldRenderLiveDanmaku(text = "", emoticonUrl = "https://example.com/e.png"))
        assertEquals(true, shouldRenderLiveDanmaku(text = "赛事", emoticonUrl = null))
    }

    @Test
    fun `chat only uses image emoticon branch for non blank urls`() {
        assertEquals(false, shouldRenderLiveDanmakuImageEmoticon(null))
        assertEquals(false, shouldRenderLiveDanmakuImageEmoticon(""))
        assertEquals(false, shouldRenderLiveDanmakuImageEmoticon("   "))
        assertEquals(true, shouldRenderLiveDanmakuImageEmoticon("https://example.com/e.png"))
    }

    @Test
    fun `interaction segmented control keeps liquid glass touch target dimensions`() {
        val spec = resolveLiveInteractionSegmentedControlSpec()

        assertEquals(14, spec.horizontalPaddingDp)
        assertEquals(8, spec.verticalPaddingDp)
        assertEquals(44, spec.heightDp)
        assertEquals(30, spec.indicatorHeightDp)
        assertEquals(14, spec.labelFontSizeSp)
    }

    @Test
    fun `live room backdrop and input alpha mirror PiliPlus transparent stack`() {
        val tokens = resolveLivePiliPlusRoomColorTokens(
            inputOverlayColor = Color(0xFFDDE1E6),
            inputContentColor = Color(0xFFE6E1E5)
        )

        assertEquals(Color.Black, tokens.baseBackgroundColor)
        assertEquals(0.60f, tokens.backdropImageAlpha)
        assertEquals(0.10f, tokens.inputContainerAlpha)
        assertEquals(Color(0xFFDDE1E6), tokens.inputOverlayColor)
        assertEquals(Color(0xFFE6E1E5), tokens.inputContentColor)
    }
}
