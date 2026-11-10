package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppChromeSizeTokensTest {

    @Test
    fun `material3 compact capsule tokens keep search and action geometry independent of touch targets`() {
        val spec = resolveCompactCapsuleChromeSpec(AppUiStyle.MATERIAL3)

        assertEquals(48, spec.primaryHeightDp)
        assertEquals(40, spec.secondaryButtonSizeDp)
        assertEquals(32, spec.chipHeightDp)
        assertEquals(28, spec.compactChipHeightDp)
        // Compact corners stay proportional to the visual sizes.
        assertEquals(14, spec.primaryCornerRadiusDp)
        // 40 * 0.3 = 12
        assertEquals(12, spec.secondaryButtonCornerRadiusDp)
        assertEquals(24, spec.iconSizeDp)
        assertEquals(12, spec.standardGapDp)
    }

    @Test
    fun `miuix compact capsules share the action size while keeping native proportions`() {
        val spec = resolveCompactCapsuleChromeSpec(AppUiStyle.MIUIX)
        assertEquals(44, spec.primaryHeightDp)
        assertEquals(40, spec.secondaryButtonSizeDp)
        assertEquals(13, spec.primaryCornerRadiusDp)
        assertEquals(12, spec.secondaryButtonCornerRadiusDp)
    }
}
