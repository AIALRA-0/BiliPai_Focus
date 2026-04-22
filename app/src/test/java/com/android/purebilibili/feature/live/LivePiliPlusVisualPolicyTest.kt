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
    fun `chip colors use Material secondary container for theme aware selection`() {
        val colors = resolveLivePiliPlusChipColors(
            selectedContainer = Color(0xFFE8DEF8),
            selectedContent = Color(0xFF1D192B),
            unselectedContent = Color(0xFF49454F)
        )

        assertEquals(Color(0xFFE8DEF8), colors.selectedContainerColor)
        assertEquals(Color(0xFF1D192B), colors.selectedContentColor)
        assertEquals(Color.Transparent, colors.unselectedContainerColor)
        assertEquals(Color(0xFF49454F), colors.unselectedContentColor)
    }

    @Test
    fun `overlay chat bubble mirrors PiliPlus portrait player density`() {
        val dark = resolveLivePiliPlusChatBubbleTokens(isOverlay = true, isDark = true)
        val light = resolveLivePiliPlusChatBubbleTokens(isOverlay = true, isDark = false)

        assertEquals(14, dark.cornerRadiusDp)
        assertEquals(10, dark.horizontalPaddingDp)
        assertEquals(4, dark.verticalPaddingDp)
        assertEquals(14, dark.fontSizeSp)
        assertEquals(0.56f, dark.backgroundAlpha)
        assertEquals(0.90f, dark.nameAlpha)
        assertEquals(0.56f, light.backgroundAlpha)
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
