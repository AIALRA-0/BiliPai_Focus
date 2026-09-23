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
    fun immersiveMobileProfile_doesNotDrawTopScrimAtRest() {
        val alpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0f
        )

        assertEquals(0f, alpha)
    }

    @Test
    fun immersiveMobileProfile_keepsTopScrimRemovedWhileScrolling() {
        val restingAlpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0f
        )
        val midScrollAlpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 0.45f
        )

        assertEquals(0f, restingAlpha)
        assertEquals(0f, midScrollAlpha)
    }

    @Test
    fun immersiveMobileProfile_keepsTopScrimRemovedWhenFullyCollapsed() {
        val alpha = resolveProfileTopBarScrimAlpha(
            isImmersive = true,
            collapsedFraction = 1f
        )

        assertEquals(0f, alpha)
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

    @Test
    fun mobileProfile_pinnedTopChromeScrim_fadesInSmoothlyDuringScroll() {
        assertEquals(0f, resolveProfilePinnedTopChromeScrim(0, 0))
        assertEquals(0.5f, resolveProfilePinnedTopChromeScrim(0, 60))
        assertEquals(1f, resolveProfilePinnedTopChromeScrim(0, 120))
        assertEquals(1f, resolveProfilePinnedTopChromeScrim(0, 200))
        assertEquals(1f, resolveProfilePinnedTopChromeScrim(1, 0))
    }
}
