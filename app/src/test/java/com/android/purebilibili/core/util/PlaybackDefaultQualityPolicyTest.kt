package com.android.purebilibili.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDefaultQualityPolicyTest {

    @Test
    fun `auto highest should refresh vip status before resolving default quality when cache is stale`() {
        assertEquals(
            true,
            shouldRefreshVipStatusBeforeResolvingDefaultQuality(
                storedQuality = 64,
                autoHighestEnabled = true,
                isLoggedIn = true,
                cachedIsVip = false
            )
        )
    }

    @Test
    fun `premium stored quality should refresh vip status before resolving default quality when cache is stale`() {
        assertEquals(
            true,
            shouldRefreshVipStatusBeforeResolvingDefaultQuality(
                storedQuality = 116,
                autoHighestEnabled = false,
                isLoggedIn = true,
                cachedIsVip = false
            )
        )
    }

    @Test
    fun `standard quality should not refresh vip status before resolving default quality`() {
        assertEquals(
            false,
            shouldRefreshVipStatusBeforeResolvingDefaultQuality(
                storedQuality = 80,
                autoHighestEnabled = false,
                isLoggedIn = true,
                cachedIsVip = false
            )
        )
    }

    @Test
    fun `auto highest preference should bypass network specific default quality normalization`() {
        listOf(
            Triple(false, false, "guest"),
            Triple(true, false, "logged-in"),
            Triple(true, true, "vip")
        ).forEach { (isLoggedIn, isVip, label) ->
            assertEquals(
                label,
                127,
                resolvePlaybackDefaultQualityId(
                    storedQuality = 64,
                    autoHighestEnabled = true,
                    isLoggedIn = isLoggedIn,
                    isVip = isVip
                )
            )
        }
    }

    @Test
    fun `disabled auto highest preserves preferred quality before stream discovery`() {
        assertEquals(
            116,
            resolvePlaybackDefaultQualityId(
                storedQuality = 116,
                autoHighestEnabled = false,
                isLoggedIn = true,
                isVip = false
            )
        )
    }

    @Test
    fun `stale non vip cache preserves 1080p60 preference`() {
        assertEquals(
            116,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = true,
                isVip = false
            )
        )
    }

    @Test
    fun `guest preserves preference for server based fallback`() {
        assertEquals(
            116,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = false,
                isVip = false
            )
        )
    }

    @Test
    fun `vip should keep 1080p60 default`() {
        assertEquals(
            116,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = true,
                isVip = true
            )
        )
    }

    @Test
    fun `premium preferences are resolved against discovered streams`() {
        listOf(112, 120, 125, 126, 127).forEach { quality ->
            assertEquals(
                "quality=$quality",
                quality,
                resolvePlayableDefaultQualityId(
                    storedQuality = quality,
                    isLoggedIn = true,
                    isVip = false
                )
            )
        }
    }
}
