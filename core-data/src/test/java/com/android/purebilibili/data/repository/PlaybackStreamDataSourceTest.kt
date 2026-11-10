package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlaybackStreamDataSourceTest {
    @Test fun `TV credential keeps TV app key while retaining requested part and quality`() {
        val params = buildSignedAppPlayUrlParams("BV-demo", 22, 80, "test-token", "tv", timestampSec = 1_000)
        assertEquals(AppSignUtils.TV_APP_KEY, params["appkey"])
        assertEquals("android_tv_yst", params["mobi_app"])
        assertEquals("22", params["cid"])
        assertEquals("80", params["qn"])
        assertEquals("1000", params["ts"])
        assertFalse(params["sign"].isNullOrBlank())
    }
    @Test fun `Android credential uses Android signing fields and preserves audio language`() {
        val params = buildSignedAppPlayUrlParams("BV-demo", 22, 120, "test-token", "android", "ja", 1_000)
        assertEquals(AppSignUtils.ANDROID_APP_KEY, params["appkey"])
        assertEquals("android", params["mobi_app"])
        assertEquals("ja", params["cur_language"])
        assertEquals("ja", params["lang"])
        assertFalse(params["sign"].isNullOrBlank())
    }
}
