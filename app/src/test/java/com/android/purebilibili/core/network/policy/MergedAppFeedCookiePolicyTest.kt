package com.android.purebilibili.core.network.policy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MergedAppFeedCookiePolicyTest {

    @Test
    fun mergedAppFeedHalf_stripsCookies() {
        assertTrue(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index",
                mobiApp = "android_hd"
            )
        )
    }

    @Test
    fun appOnlyMode_keepsCookies() {
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index",
                mobiApp = "android"
            )
        )
    }

    @Test
    fun missingMobiApp_keepsCookies() {
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index",
                mobiApp = null
            )
        )
    }

    @Test
    fun webHomeFeed_keepsCookies() {
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "api.bilibili.com",
                encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd",
                mobiApp = "android_hd"
            )
        )
    }

    @Test
    fun otherAppEndpoints_keepCookies() {
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/space",
                mobiApp = "android_hd"
            )
        )
    }

    @Test
    fun partialMatches_keepCookies() {
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index",
                mobiApp = "android_hdx"
            )
        )
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index/",
                mobiApp = "android_hd"
            )
        )
        assertFalse(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com.cn",
                encodedPath = "/x/v2/feed/index",
                mobiApp = "android_hd"
            )
        )
    }

    @Test
    fun interceptorMarkerMatchesPolicyConstant() {
        assertTrue(
            shouldStripMergedAppFeedCookies(
                host = "app.bilibili.com",
                encodedPath = "/x/v2/feed/index",
                mobiApp = MERGED_APP_FEED_MOBI_APP
            )
        )
    }
}