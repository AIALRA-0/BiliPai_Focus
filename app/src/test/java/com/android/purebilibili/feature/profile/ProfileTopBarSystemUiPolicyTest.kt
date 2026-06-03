package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileTopBarSystemUiPolicyTest {

    @Test
    fun mobileProfile_keepsTopBarPinnedWhileScrolling() {
        assertTrue(
            shouldPinProfileTopBarOnScroll(
                useSplitLayout = false
            )
        )
    }

    @Test
    fun splitLayoutProfile_keepsTopBarPinnedWhileScrolling() {
        assertTrue(
            shouldPinProfileTopBarOnScroll(
                useSplitLayout = true
            )
        )
    }

    @Test
    fun immersiveMobileProfile_usesLightTopScrimAtRest() {
        val alpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0f
        )

        assertTrue(alpha > 0f)
        assertTrue(alpha <= 0.12f)
    }

    @Test
    fun immersiveMobileProfile_increasesTopScrimWithoutObviousDarkBand() {
        val restingAlpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0f
        )
        val midScrollAlpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0.45f
        )

        assertTrue(midScrollAlpha > restingAlpha)
        assertTrue(midScrollAlpha <= 0.2f)
    }

    @Test
    fun immersiveMobileProfile_capsTopScrimWhenFullyCollapsed() {
        val alpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 1f
        )

        assertTrue(alpha > 0f)
        assertTrue(alpha <= 0.24f)
    }

    @Test
    fun immersiveMobileProfile_usesLightStatusBarIcons() {
        assertFalse(
            resolveProfileLightStatusBars(
                isImmersive = true,
                useSplitLayout = false,
                isDarkTheme = false
            )
        )
    }

    @Test
    fun nonImmersiveProfile_followsThemeForStatusBarIcons() {
        assertTrue(
            resolveProfileLightStatusBars(
                isImmersive = false,
                useSplitLayout = false,
                isDarkTheme = false
            )
        )
        assertFalse(
            resolveProfileLightStatusBars(
                isImmersive = false,
                useSplitLayout = false,
                isDarkTheme = true
            )
        )
    }
}
