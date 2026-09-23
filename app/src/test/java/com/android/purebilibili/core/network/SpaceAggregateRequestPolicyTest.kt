package com.android.purebilibili.core.network

import com.android.purebilibili.core.store.TokenManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceAggregateRequestPolicyTest {

    @Test
    fun `guest space aggregate request is app signed without access key`() {
        val params = buildSpaceAggregateParams(mid = 2L, accessToken = null)

        assertEquals("2", params["vmid"])
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertEquals(
            "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
            params["statistics"]
        )
        assertTrue(params["ts"].orEmpty().isNotBlank())
        assertTrue(params["sign"].orEmpty().isNotBlank())
        assertEquals(
            AppSignUtils.signForAndroidHdLogin(params - "sign")["sign"],
            params["sign"]
        )
        assertFalse(params.containsKey("access_key"))
        assertFalse(params.containsKey("actionKey"))
    }

    @Test
    fun `authenticated space aggregate request includes access key before signing`() {
        val params = buildSpaceAggregateParams(mid = 2L, accessToken = "token")

        assertEquals("token", params["access_key"])
        assertTrue(params["sign"].orEmpty().isNotBlank())
    }

    @Test
    fun `space aggregate pairs each access token with its issuing app credentials`() {
        val androidParams = buildSpaceAggregateParams(
            mid = 2L,
            accessToken = "android-token",
            accessTokenPlatform = TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID
        )
        assertEquals("android-token", androidParams["access_key"])
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, androidParams["appkey"])
        assertEquals(
            AppSignUtils.signForAndroidHdLogin(androidParams - "sign")["sign"],
            androidParams["sign"]
        )

        val tvParams = buildSpaceAggregateParams(
            mid = 2L,
            accessToken = "tv-token",
            accessTokenPlatform = TokenManager.ACCESS_TOKEN_PLATFORM_TV
        )
        assertEquals("tv-token", tvParams["access_key"])
        assertEquals(AppSignUtils.TV_APP_KEY, tvParams["appkey"])
        assertEquals(
            AppSignUtils.signForTvApi(tvParams - "sign")["sign"],
            tvParams["sign"]
        )
    }

    @Test
    fun `space liked archive request includes pagination params and app sign`() {
        val params = buildSpaceLikedArchiveParams(
            mid = 8047632L,
            page = 2,
            pageSize = 20,
            accessToken = null
        )

        assertEquals("8047632", params["vmid"])
        assertEquals("2", params["pn"])
        assertEquals("20", params["ps"])
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertTrue(params["sign"].orEmpty().isNotBlank())
        assertEquals(
            AppSignUtils.signForAndroidHdLogin(params - "sign")["sign"],
            params["sign"]
        )
    }
}
