package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPlayerChromeProfileTest {
    @Test
    fun materialUnderlineUsesRoomyOverlayControls() {
        val profile = testProfile(AppTopTabPresentation.MATERIAL_UNDERLINE)
        assertFalse(profile.usesCompactOverlayControls)
    }

    @Test
    fun capsulePresentationsUseCompactOverlayControls() {
        assertTrue(testProfile(AppTopTabPresentation.MOVING_CAPSULE).usesCompactOverlayControls)
        assertTrue(testProfile(AppTopTabPresentation.TONAL_CAPSULE).usesCompactOverlayControls)
    }

    private fun testProfile(presentation: AppTopTabPresentation) = AppPlayerChromeProfile(
        tabPresentation = presentation,
        iconFamily = AppSemanticIconFamily.MATERIAL,
        compactChromeSpec = CompactCapsuleChromeSpec(
            primaryHeightDp = 48,
            secondaryButtonSizeDp = 48,
            chipHeightDp = 32,
            compactChipHeightDp = 28,
            primaryCornerRadiusDp = 24,
            secondaryButtonCornerRadiusDp = 16,
            chipCornerRadiusDp = 16,
            compactChipCornerRadiusDp = 14,
            iconSizeDp = 20,
            smallIconSizeDp = 16,
            inputHorizontalPaddingDp = 14,
            chipHorizontalPaddingDp = 12,
            compactChipHorizontalPaddingDp = 10,
            standardGapDp = 12,
        ),
        effects = AppEffectCapability(
            supportsIndependentLiquidGlass = false,
            usesTonalContainerTreatment = false,
        ),
    )
}
