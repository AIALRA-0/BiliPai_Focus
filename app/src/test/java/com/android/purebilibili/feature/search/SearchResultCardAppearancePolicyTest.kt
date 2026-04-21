package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultCardAppearancePolicyTest {

    @Test
    fun searchCardBlur_enabledWhenEitherHomeBlurToggleIsOn() {
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = true,
                bottomBarBlurEnabled = false
            )
        )
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = true
            )
        )
        assertFalse(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = false
            )
        )
    }

    @Test
    fun videoSearchAppearance_reusesHomeGlassAndBadgeInputs() {
        val appearance = resolveSearchVideoCardAppearance(
            liquidGlassEnabled = false,
            blurEnabled = true,
            showHomeCoverGlassBadges = false,
            showHomeInfoGlassBadges = true
        )

        assertFalse(appearance.glassEnabled)
        assertTrue(appearance.blurEnabled)
        assertFalse(appearance.showCoverGlassBadges)
        assertTrue(appearance.showInfoGlassBadges)
    }

    @Test
    fun genericSearchResultCard_switchesBetweenGlassAndPlainStyles() {
        val glass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true,
            uiPreset = UiPreset.IOS
        )
        val plain = resolveSearchResultCardAppearance(
            liquidGlassEnabled = false,
            uiPreset = UiPreset.IOS
        )

        assertEquals(SearchResultCardSurfaceStyle.GLASS, glass.surfaceStyle)
        assertEquals(0.92f, glass.containerAlpha)
        assertEquals(0.12f, glass.borderAlpha)
        assertEquals(0, glass.tonalElevationDp)

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, plain.surfaceStyle)
        assertEquals(1f, plain.containerAlpha)
        assertEquals(0f, plain.borderAlpha)
        assertEquals(1, plain.shadowElevationDp)
    }

    @Test
    fun md3SearchResultCard_usesMoreMaterialSurfaceTuningEvenWhenGlassIsEnabled() {
        val md3Glass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true,
            uiPreset = UiPreset.MD3
        )

        assertEquals(SearchResultCardSurfaceStyle.GLASS, md3Glass.surfaceStyle)
        assertEquals(0.96f, md3Glass.containerAlpha)
        assertEquals(0f, md3Glass.borderAlpha)
        assertEquals(1, md3Glass.tonalElevationDp)
        assertEquals(0, md3Glass.shadowElevationDp)
    }

    @Test
    fun md3PlainSearchResultCard_staysFlatAndMaterialWhenGlassIsDisabled() {
        val md3Plain = resolveSearchResultCardAppearance(
            liquidGlassEnabled = false,
            uiPreset = UiPreset.MD3
        )

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, md3Plain.surfaceStyle)
        assertEquals(1f, md3Plain.containerAlpha)
        assertEquals(0f, md3Plain.borderAlpha)
        assertEquals(1, md3Plain.tonalElevationDp)
        assertEquals(0, md3Plain.shadowElevationDp)
    }
}
