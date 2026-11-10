package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.store.TokenManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class HdQrLoginPolicyTest {
    @Test
    fun `qr generation and polling keep the same HD signing identity`() {
        val generation = buildHdQrLoginParams(null, timestampSeconds = 1000)
        val polling = buildHdQrLoginParams("auth&code", timestampSeconds = 1001)
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, generation["appkey"])
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, polling["appkey"])
        assertEquals("android", generation["platform"])
        assertEquals("android_hd", generation["mobi_app"])
        assertEquals("auth&code", polling["auth_code"])
        assertEquals("0", polling["local_id"])
        assertEquals("1001", polling["ts"])
        assertEquals(AppSignUtils.signForAndroidHdLogin(polling - "sign"), polling)
        assertFalse(polling.containsKey("mobi_app"))
    }

    @Test
    fun `HD credentials cannot silently use legacy Android or TV signing`() {
        val hd = buildSignedAppPlayUrlParams(
            "BV-demo", 22, 120, "test-token", TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD,
            timestampSec = 1000,
        )
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, hd["appkey"])
        assertEquals("android_hd", hd["mobi_app"])
        assertNotEquals(AppSignUtils.TV_APP_KEY, hd["appkey"])
        assertNotEquals(AppSignUtils.ANDROID_APP_KEY, hd["appkey"])
        assertEquals(AppSignUtils.signForAndroidHdLogin(hd - "sign"), hd)
    }
}
