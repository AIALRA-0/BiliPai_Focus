package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCommentAppearancePolicyTest {

    @Test
    fun `light comment appearance should derive every slot from active theme`() {
        val appearance = resolveVideoCommentAppearance(
            surfaceColor = Color(0xFFFFFFFF),
            surfaceVariantColor = Color(0xFFF1F2F4),
            surfaceContainerHighColor = Color(0xFFE8EAF0),
            outlineVariantColor = Color(0xFFD9DCE3),
            onSurfaceColor = Color(0xFF1B1C1F),
            onSurfaceVariantColor = Color(0xFF6A6F76),
            primaryColor = Color(0xFFFB7299),
            onPrimaryColor = Color(0xFFFFFFFF)
        )

        assertEquals(Color(0xFFFFFFFF), appearance.panelColor)
        assertEquals(Color(0xFFF1F2F4).copy(alpha = 0.40f), appearance.composerHintBackgroundColor)
        assertEquals(Color(0xFFF1F2F4).copy(alpha = 0.50f), appearance.segmentedBackgroundColor)
        assertEquals(Color(0xFFD9DCE3), appearance.dividerColor)
        assertEquals(Color(0xFF1B1C1F), appearance.primaryTextColor)
        assertEquals(Color(0xFF6A6F76), appearance.secondaryTextColor)
        assertEquals(Color(0xFFFB7299), appearance.accentColor)
        assertEquals(Color(0xFFFFFFFF), appearance.toggleCheckedContentColor)
    }

    @Test
    fun `dark comment appearance should keep dark surfaces while reusing theme accent`() {
        val appearance = resolveVideoCommentAppearance(
            surfaceColor = Color(0xFF141414),
            surfaceVariantColor = Color(0xFF242424),
            surfaceContainerHighColor = Color(0xFF1E1E1E),
            outlineVariantColor = Color(0xFF333333),
            onSurfaceColor = Color(0xFFF5F5F5),
            onSurfaceVariantColor = Color(0xFFD0D0D0),
            primaryColor = Color(0xFF34C759),
            onPrimaryColor = Color(0xFF081108)
        )

        assertEquals(Color(0xFF141414), appearance.panelColor)
        assertEquals(Color(0xFF242424), appearance.placeholderColor)
        assertEquals(Color(0xFF333333), appearance.dividerColor)
        assertEquals(Color(0xFFF5F5F5), appearance.primaryTextColor)
        assertEquals(Color(0xFFD0D0D0), appearance.secondaryTextColor)
        assertEquals(Color(0xFF34C759), appearance.accentColor)
        assertEquals(Color(0xFF081108), appearance.toggleCheckedContentColor)
        assertEquals(Color(0xFF242424).copy(alpha = 0.50f), appearance.toggleUncheckedBackgroundColor)
    }
}
