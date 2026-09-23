package com.android.purebilibili.core.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.calculateContrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserVipBadgePolicyTest {

    @Test
    fun `label prefers server text over vip type`() {
        assertEquals(
            "十年大会员",
            resolveUserVipBadgeLabel(label = "十年大会员", vipType = 2),
        )
    }

    @Test
    fun `label falls back by vip type when empty`() {
        assertEquals("年度大会员", resolveUserVipBadgeLabel(label = "", vipType = 2))
        assertEquals("大会员", resolveUserVipBadgeLabel(label = null, vipType = 1))
        assertEquals("大会员", resolveUserVipBadgeLabel(label = "  ", vipType = 0))
    }

    @Test
    fun `dark translucent pink container keeps readable label`() {
        // iOS/dark scheme style: primary-tinted secondaryContainer + pink on-container.
        val result = resolveVipBadgeColors(
            containerColor = Color(0xFFFA7298).copy(alpha = 0.22f),
            preferredContentColor = Color(0xFFFA7298).copy(alpha = 0.9f),
            surfaceColor = Color(0xFF121212),
            onSurface = Color(0xFFE6E1E5),
            onBackground = Color(0xFFE6E1E5),
            inverseOnSurface = Color(0xFF313033),
        )

        assertEquals(1f, result.containerColor.alpha)
        assertEquals(1f, result.contentColor.alpha)
        assertTrue(
            calculateContrastRatio(result.contentColor, result.containerColor) >=
                ACCESSIBLE_TEXT_MIN_CONTRAST,
        )
    }

    @Test
    fun `light cream container keeps dark readable label`() {
        // Space-header style soft cream pill + dark on-container.
        val result = resolveVipBadgeColors(
            containerColor = Color(0xFFF5E6D3),
            preferredContentColor = Color(0xFF4A3728),
            surfaceColor = Color.White,
            onSurface = Color(0xFF1B1C1F),
            onBackground = Color(0xFF1B1C1F),
            inverseOnSurface = Color(0xFFF4EFF4),
        )

        assertEquals(Color(0xFF4A3728), result.contentColor)
        assertTrue(
            calculateContrastRatio(result.contentColor, result.containerColor) >=
                ACCESSIBLE_TEXT_MIN_CONTRAST,
        )
    }
}
