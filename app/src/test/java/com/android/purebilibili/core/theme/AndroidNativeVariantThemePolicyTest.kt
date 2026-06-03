package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidNativeVariantThemePolicyTest {

    @Test
    fun miuixVariant_usesMiuixAlignedTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(BiliMiuixTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliMiuixTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun material3Variant_keepsExistingTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(BiliTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun miuixVariant_enablesSmoothRoundingAndLargerCornerScale() {
        assertTrue(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            MIUIX_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun material3Variant_keepsCompactCornerScaleWithoutSmoothRounding() {
        assertFalse(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            MD3_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
    }

    @Test
    fun androidNativeVariants_exposeMaterialAndMiuixChromeTokens() {
        val material = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val miuix = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(24, material.containerCornerRadiusDp)
        assertEquals(20, miuix.containerCornerRadiusDp)
        assertTrue(material.pillCornerRadiusDp > miuix.pillCornerRadiusDp)
        assertTrue(material.selectedContainerAlpha < miuix.selectedContainerAlpha)
        assertEquals(1f, material.motionScale)
        assertEquals(1f, miuix.motionScale)
    }
}
