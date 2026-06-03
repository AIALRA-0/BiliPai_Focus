package com.android.purebilibili.core.network.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeFeedAnonymizerPolicyTest {

    @Test
    fun disabledPlugin_keepsHomeFeedCookies() {
        assertFalse(
            shouldClearHomeFeedCookies(
                pluginEnabled = false,
                host = "api.bilibili.com",
                encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd"
            )
        )
    }

    @Test
    fun enabledPlugin_clearsWebHomeFeedCookies() {
        assertTrue(
            shouldClearHomeFeedCookies(
                pluginEnabled = true,
                host = "api.bilibili.com",
                encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd"
            )
        )
    }

    @Test
    fun enabledPlugin_clearsEntireCookieHeaderForWebHomeFeed() {
        val cookieNames = listOf(
            "buvid3",
            "buvid4",
            "b_nut",
            "SESSDATA",
            "bili_jct",
            "DedeUserID",
            "DedeUserID__ckMd5",
            "sid"
        )

        assertEquals(
            emptyList(),
            filterHomeFeedCookieHeaderNames(
                pluginEnabled = true,
                host = "api.bilibili.com",
                encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd",
                cookieNames = cookieNames
            )
        )
    }

    @Test
    fun nonMatchingRequest_keepsEntireCookieHeader() {
        val cookieNames = listOf("buvid3", "SESSDATA", "bili_jct")

        assertEquals(
            cookieNames,
            filterHomeFeedCookieHeaderNames(
                pluginEnabled = true,
                host = "api.bilibili.com",
                encodedPath = "/x/web-interface/nav",
                cookieNames = cookieNames
            )
        )
    }

    @Test
    fun enabledPlugin_keepsCookiesForPlaybackDynamicUserAndMobileFeed() {
        val requests = listOf(
            "api.bilibili.com" to "/x/player/wbi/playurl",
            "api.bilibili.com" to "/x/polymer/web-dynamic/v1/feed/all",
            "api.bilibili.com" to "/x/web-interface/nav",
            "app.bilibili.com" to "/x/v2/feed/index"
        )

        requests.forEach { (host, encodedPath) ->
            assertFalse(
                shouldClearHomeFeedCookies(
                    pluginEnabled = true,
                    host = host,
                    encodedPath = encodedPath
                )
            )
        }
    }

    @Test
    fun enabledPlugin_keepsCookiesForNonApiHostAndSimilarPath() {
        val requests = listOf(
            "www.bilibili.com" to "/x/web-interface/wbi/index/top/feed/rcmd",
            "api.bilibili.com" to "/x/web-interface/wbi/index/top/feed/rcmd_extra",
            "api.bilibili.com" to "/x/web-interface/wbi/index/top/feed"
        )

        requests.forEach { (host, encodedPath) ->
            assertFalse(
                shouldClearHomeFeedCookies(
                    pluginEnabled = true,
                    host = host,
                    encodedPath = encodedPath
                )
            )
        }
    }

    @Test
    fun matchingWebHomeFeedRequest_recordsAnonymizerStats() {
        HomeFeedAnonymizerRuntime.resetStats()

        val shouldClear = resolveHomeFeedCookieAnonymizerDecision(
            pluginEnabled = true,
            host = "api.bilibili.com",
            encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd",
            nowMs = 1_700_000_000_000L
        )
        val snapshot = HomeFeedAnonymizerRuntime.statsSnapshot

        assertTrue(shouldClear)
        assertEquals(1L, snapshot.totalHits)
        assertEquals(1_700_000_000_000L, snapshot.lastHitAtMs)
        assertEquals("api.bilibili.com", snapshot.lastHitHost)
        assertEquals("/x/web-interface/wbi/index/top/feed/rcmd", snapshot.lastHitEncodedPath)
    }

    @Test
    fun nonMatchingRequest_doesNotRecordAnonymizerStats() {
        HomeFeedAnonymizerRuntime.resetStats()

        val shouldClear = resolveHomeFeedCookieAnonymizerDecision(
            pluginEnabled = true,
            host = "api.bilibili.com",
            encodedPath = "/x/web-interface/nav",
            nowMs = 1_700_000_000_000L
        )

        assertFalse(shouldClear)
        assertEquals(0L, HomeFeedAnonymizerRuntime.statsSnapshot.totalHits)
        assertNull(HomeFeedAnonymizerRuntime.statsSnapshot.lastHitAtMs)
    }

    @Test
    fun resetStats_clearsAnonymizerStats() {
        HomeFeedAnonymizerRuntime.resetStats()
        resolveHomeFeedCookieAnonymizerDecision(
            pluginEnabled = true,
            host = "api.bilibili.com",
            encodedPath = "/x/web-interface/wbi/index/top/feed/rcmd",
            nowMs = 1_700_000_000_000L
        )

        HomeFeedAnonymizerRuntime.resetStats()

        val snapshot = HomeFeedAnonymizerRuntime.statsSnapshot
        assertEquals(0L, snapshot.totalHits)
        assertNull(snapshot.lastHitAtMs)
        assertNull(snapshot.lastHitHost)
        assertNull(snapshot.lastHitEncodedPath)
    }
}
