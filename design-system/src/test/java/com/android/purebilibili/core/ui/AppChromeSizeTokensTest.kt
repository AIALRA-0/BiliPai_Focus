package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppChromeSizeTokensTest {

    @Test
    fun `icon actions keep a 48dp outer touch target`() {
        assertEquals(48.dp, AppChromeSizeTokens.MinimumTouchTarget)
    }

    @Test
    fun `material3 compact capsule tokens align search bar and touch targets with material3 chrome`() {
        val spec = resolveCompactCapsuleChromeSpec(AppUiStyle.MATERIAL3)

        assertEquals(56, spec.primaryHeightDp)
        assertEquals(48, spec.secondaryButtonSizeDp)
        assertEquals(32, spec.chipHeightDp)
        assertEquals(28, spec.compactChipHeightDp)
        // 56 * 0.3 = 16; never full pill (28)
        assertEquals(16, spec.primaryCornerRadiusDp)
        // 48 * 0.3 = 14
        assertEquals(14, spec.secondaryButtonCornerRadiusDp)
        assertEquals(24, spec.iconSizeDp)
        assertEquals(12, spec.standardGapDp)
    }

    @Test
    fun `miuix compact capsule tokens keep denser search chrome within material touch bounds`() {
        val spec = resolveCompactCapsuleChromeSpec(AppUiStyle.MIUIX)

        assertEquals(48, spec.primaryHeightDp)
        assertEquals(48, spec.secondaryButtonSizeDp)
        // 48 * 0.3 = 14; never full pill (22)
        assertEquals(14, spec.primaryCornerRadiusDp)
        assertEquals(14, spec.inputHorizontalPaddingDp)
        assertEquals(8, spec.standardGapDp)
    }
}
