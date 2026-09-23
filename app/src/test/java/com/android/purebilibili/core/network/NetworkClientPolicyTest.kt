package com.android.purebilibili.core.network

import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.net.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkClientPolicyTest {

    @Test
    fun sharedClient_allowsHttpTwoForMultiplexing() {
        assertTrue(
            NetworkModule.okHttpClient.protocols.contains(Protocol.HTTP_2),
            "Expected shared client to support HTTP/2, actual=${NetworkModule.okHttpClient.protocols}"
        )
    }

    @Test
    fun sharedClient_expandsHttpCacheBudget() {
        val cache = NetworkModule.okHttpClient.cache
        val expectedBudget = 32L * 1024 * 1024

        requireNotNull(cache) { "Expected shared client to expose an HTTP cache" }
        assertEquals(expectedBudget, cache.maxSize())
    }

    @Test
    fun playbackClient_bypassesSystemProxyButKeepsProtocols() {
        val sharedClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()

        val playbackClient = NetworkModule.buildPlaybackOkHttpClient(sharedClient)

        assertEquals(Proxy.NO_PROXY, playbackClient.proxy)
        assertEquals(sharedClient.protocols, playbackClient.protocols)
    }

    @Test
    fun guestClient_allowsHttpTwoForFallbackRequests() {
        assertEquals(
            listOf(Protocol.HTTP_2, Protocol.HTTP_1_1),
            NetworkModule.guestOkHttpClient.protocols
        )
    }

    @Test
    fun sharedProtocolPolicy_keepsHttpTwoWithHttpOneFallback() {
        assertEquals(
            listOf(Protocol.HTTP_2, Protocol.HTTP_1_1),
            NetworkModule.resolveSharedNetworkProtocols()
        )
    }

    @Test
    fun androidHdLoginEndpoints_useAndroidHdAppKeyHeader() {
        assertEquals(
            "android_hd",
            NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/passport-login/sms/send")
        )
        assertEquals(
            "android_hd",
            NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/passport-login/login/sms")
        )
        assertEquals(
            "android_hd",
            NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/passport-login/oauth2/login")
        )
        assertEquals(
            "android_hd",
            NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/safecenter/common/sms/send")
        )
        assertEquals(
            "android_hd",
            NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/safecenter/login/tel/verify")
        )
        assertNull(NetworkModule.resolveAndroidHdLoginAppKeyHeader("/x/web-interface/nav"))
    }

    @Test
    fun forcedCookieHeaderReplacesJarCookieAfterBridge() {
        val request = okhttp3.Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .header("Cookie", "buvid3=from-jar")
            .header(FORCE_COOKIE_HEADER, "SESSDATA=imported; bili_jct=csrf")
            .build()

        val applied = applyForcedCookieHeader(request)
        assertEquals("SESSDATA=imported; bili_jct=csrf", applied.header("Cookie"))
        assertNull(applied.header(FORCE_COOKIE_HEADER))
        assertEquals(request, applyForcedCookieHeader(request.newBuilder().removeHeader(FORCE_COOKIE_HEADER).build()))
    }
}
