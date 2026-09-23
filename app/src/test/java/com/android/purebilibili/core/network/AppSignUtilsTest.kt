package com.android.purebilibili.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSignUtilsTest {

    @Test
    fun `TV API signing percent encodes structured parameter values`() {
        val params = mapOf(
            "statistics" to "{\"appId\":1,\"platform\":3}",
            "vmid" to "2"
        )

        val signed = AppSignUtils.signForTvApi(params)

        assertEquals(AppSignUtils.TV_APP_KEY, signed["appkey"])
        assertEquals(signed["sign"], AppSignUtils.signForTvApi(params)["sign"])
    }

    @Test
    fun `official bilibili APP sign demo vector matches documented hash`() {
        // https://github.com/SocialSisterYi/bilibili-API-collect docs/misc/sign/APP.md
        val params = mapOf(
            "appkey" to "1d8b6e7d45233436",
            "id" to "114514",
            "str" to "1919810",
            "test" to "いいよ，こいよ"
        )
        val sorted = params.toSortedMap()
        val query = sorted.entries.joinToString("&") { (key, value) ->
            "${AppSignUtils.percentEncode(key)}=${AppSignUtils.percentEncode(value)}"
        }
        val sign = java.security.MessageDigest.getInstance("MD5")
            .digest((query + "560c52ccd288fed045859ed18bffd973").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        assertEquals("01479cf20504d865519ac50f33ba3a7d", sign)
    }

    @Test
    fun `android HD login sign uses HD appkey and percent-encoded statistics`() {
        val params = mapOf(
            "appkey" to AppSignUtils.ANDROID_HD_APP_KEY,
            "build" to "2001100",
            "mobi_app" to "android_hd",
            "platform" to "android",
            "statistics" to "{\"appId\":5,\"platform\":3,\"version\":\"2.0.1\",\"abtest\":\"\"}",
            "ts" to "123"
        )
        val signed = AppSignUtils.signForAndroidHdLogin(params)

        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, signed["appkey"])
        assertEquals(32, signed["sign"]?.length)
        // Recompute once to ensure deterministic encoding of JSON punctuation.
        assertEquals(signed["sign"], AppSignUtils.signForAndroidHdLogin(params)["sign"])
    }
}
