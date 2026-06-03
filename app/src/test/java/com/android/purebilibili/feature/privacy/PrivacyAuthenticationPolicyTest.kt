package com.android.purebilibili.feature.privacy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacyAuthenticationPolicyTest {

    @Test
    fun `privacy mode disabled never requires authentication`() {
        assertFalse(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = false,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(routeBase = "history")
            )
        )
    }

    @Test
    fun `privacy mode enabled blocks protected routes until session is unlocked`() {
        assertTrue(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(routeBase = "favorite")
            )
        )
        assertTrue(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(routeBase = "chat")
            )
        )
    }

    @Test
    fun `unlocked privacy session allows protected routes without repeated prompts`() {
        assertFalse(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = true,
                target = PrivacyNavigationTarget(routeBase = "download_list")
            )
        )
    }

    @Test
    fun `favorite season series detail is protected but non favorite detail is not`() {
        assertTrue(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(
                    routeBase = "season_series_detail",
                    seasonSeriesType = "favorite"
                )
            )
        )
        assertTrue(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(
                    routeBase = "season_series_detail",
                    seasonSeriesType = "favorite_season"
                )
            )
        )
        assertFalse(
            shouldRequirePrivacyAuthentication(
                privacyAuthenticationEnabled = true,
                privacySessionUnlocked = false,
                target = PrivacyNavigationTarget(
                    routeBase = "season_series_detail",
                    seasonSeriesType = "series"
                )
            )
        )
    }
}
