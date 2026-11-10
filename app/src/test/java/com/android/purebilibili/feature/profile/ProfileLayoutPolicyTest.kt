package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileLayoutPolicyTest {

    @Test
    fun heroHeight_usesScreenFractionAndClamps() {
        val tokens = resolveProfileLayoutTokens()
        val heroHeight = resolveProfileHeroHeightDp(screenHeightDp = 800)

        assertEquals(tokens.heroMinHeightDp.toFloat(), heroHeight, 0.001f)
        assertTrue(heroHeight in tokens.heroMinHeightDp.toFloat()..tokens.heroMaxHeightDp.toFloat())
    }

    @Test
    fun heroHeight_clampsOnVerySmallScreens() {
        assertEquals(336f, resolveProfileHeroHeightDp(screenHeightDp = 500))
    }

    @Test
    fun heroHeight_clampsOnVeryTallScreens() {
        assertEquals(360f, resolveProfileHeroHeightDp(screenHeightDp = 1200))
    }

    @Test
    fun tabletWallpaper_reservesLargerVisibleBannerBeforeSplitContent() {
        assertEquals(
            224,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = true,
                hasWallpaper = true
            )
        )
        assertEquals(
            0,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = true,
                hasWallpaper = false
            )
        )
        assertEquals(
            0,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = false,
                hasWallpaper = true
            )
        )
    }

    @Test
    fun identityReserve_keepsMeasuredLargeTextBelowAvatarAndAboveStats() {
        for (density in listOf(0.85f, 1f, 2.625f, 3.5f)) {
            for (fontScale in listOf(0.85f, 1f, 1.3f, 2f)) {
                val top = 28f * density
                val nameHeight = kotlin.math.ceil(24f * density * fontScale).toInt()
                val uidHeight = kotlin.math.ceil(16f * density * fontScale).toInt()
                val nameTop = top + PROFILE_EXPANDED_NAME_TOP_DP * density
                val avatarBottom = top + PROFILE_EXPANDED_AVATAR_TOP_DP * density +
                    resolveProfileAvatarSizePx(density, 1f, 0f)
                val uidBottom = nameTop + nameHeight * PROFILE_EXPANDED_NAME_SCALE +
                    6f * density + uidHeight
                val statsTop = resolveProfileIdentityStatsTopPx(top, density, nameHeight, uidHeight)

                assertTrue(nameTop - avatarBottom >= 16f * density)
                // 24dp clearance also keeps the fully expanded UID's fade alpha at one.
                assertTrue(statsTop - uidBottom >= 24f * density - 0.001f)
                assertTrue(statsTop >= top + 215f * density)
            }
        }
    }

    @Test
    fun avatarSize_usesComposeDensityButPreservesPhysicalCutoutEndpoint() {
        for (density in listOf(0.85f, 1f, 2.625f, 3.5f)) {
            assertEquals(96f * density, resolveProfileAvatarSizePx(density, 1f, 48f), 0.001f)
            assertEquals(24f, resolveProfileAvatarSizePx(density, 0f, 48f), 0.001f)
            assertEquals(24f * density, resolveProfileAvatarSizePx(density, 0f, 0f), 0.001f)
            val midpoint = resolveProfileAvatarSizePx(density, 0.5f, 48f)
            assertEquals((24f + 96f * density) / 2f, midpoint, 0.001f)
        }
    }

    @Test
    fun identityReserve_retainsMinimumAndGrowsDownwardWithText() {
        assertEquals(239f, resolveProfileIdentityStatsTopPx(24f, 1f, 0, 0))
        val normal = resolveProfileIdentityStatsTopPx(24f, 1f, 24, 16)
        val large = resolveProfileIdentityStatsTopPx(24f, 1f, 48, 32)
        assertTrue(large > normal)
        assertEquals(24f * PROFILE_EXPANDED_NAME_SCALE + 16f, large - normal, 0.001f)
    }
}
