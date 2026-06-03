package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class AppChromeSizeTokensTest {

    @Test
    fun `ios compact capsule tokens keep global chrome dense and aligned`() {
        val spec = resolveCompactCapsuleChromeSpec(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(44, spec.primaryHeightDp)
        assertEquals(40, spec.secondaryButtonSizeDp)
        assertEquals(36, spec.chipHeightDp)
        assertEquals(32, spec.compactChipHeightDp)
        assertEquals(22, spec.primaryCornerRadiusDp)
        assertEquals(20, spec.secondaryButtonCornerRadiusDp)
        assertEquals(20, spec.iconSizeDp)
        assertEquals(8, spec.standardGapDp)
    }

    @Test
    fun `md3 compact capsule tokens keep normal touch target without oversized pills`() {
        val spec = resolveCompactCapsuleChromeSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(44, spec.primaryHeightDp)
        assertEquals(40, spec.secondaryButtonSizeDp)
        assertEquals(36, spec.chipHeightDp)
        assertEquals(32, spec.compactChipHeightDp)
        assertEquals(22, spec.primaryCornerRadiusDp)
        assertEquals(20, spec.secondaryButtonCornerRadiusDp)
    }

    @Test
    fun `miuix compact capsule tokens keep slightly denser horizontal padding`() {
        val spec = resolveCompactCapsuleChromeSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(44, spec.primaryHeightDp)
        assertEquals(40, spec.secondaryButtonSizeDp)
        assertEquals(14, spec.inputHorizontalPaddingDp)
        assertEquals(8, spec.standardGapDp)
    }
}
