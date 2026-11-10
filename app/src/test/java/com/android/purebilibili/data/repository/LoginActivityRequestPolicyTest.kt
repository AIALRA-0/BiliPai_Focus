package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.AccountLoginDevicesResponse
import com.android.purebilibili.data.model.response.AccountLoginRecordsResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginActivityRequestPolicyTest {
    @Test
    fun hdDeviceQuerySignsEncodedStructuredParameters() {
        val params = loginDeviceRequestParams(
            "sample-access", TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD,
            "sample-csrf", "buvid +/", 1_700_000_000L,
        )
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertEquals("android_hd", params["mobi_app"])
        assertEquals("1700000000", params["ts"])
        assertEquals("d2365902b9a42836ca36cbe90dd59fa7", params["sign"])
        assertEquals("buvid +/", params["local_id"])
    }

    @Test
    fun queryUsesAppKeyOfTheStoredTokenPlatform() {
        val android = loginDeviceRequestParams("token", TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID, "csrf", "", 1L)
        val tv = loginDeviceRequestParams("token", TokenManager.ACCESS_TOKEN_PLATFORM_TV, "csrf", "", 1L)
        assertEquals(AppSignUtils.ANDROID_APP_KEY, android["appkey"])
        assertEquals(AppSignUtils.TV_APP_KEY, tv["appkey"])
        assertFalse(android.containsKey("buvid"))
        assertFalse(tv.containsKey("local_id"))
        assertFailsWith<IllegalArgumentException> { loginDeviceRequestParams("", TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD, "", "", 1L) }
        assertFailsWith<IllegalStateException> { loginDeviceRequestParams("token", "unsupported", "", "", 1L) }
    }

    @Test
    fun absentPayloadIsDifferentFromAnEmptyDeviceList() {
        val missing = Json.decodeFromString<AccountLoginDevicesResponse>("""{"code":0,"data":{}}""")
        val empty = Json.decodeFromString<AccountLoginDevicesResponse>("""{"code":0,"data":{"devices":[]}}""")
        assertNull(missing.data?.devices)
        assertEquals(emptyList(), empty.data?.devices)
        val unknown = Json.decodeFromString<AccountLoginDevicesResponse>("""{"code":0,"data":{"devices":[{}]}}""")
        assertNull(unknown.data?.devices?.single()?.isCurrentDevice)
        assertNull(Json.decodeFromString<AccountLoginRecordsResponse>("""{"code":0,"data":{}}""").data?.list)
        assertEquals(-1, Json.decodeFromString<AccountLoginRecordsResponse>("{}").code)
    }

    @Test
    fun readsServerCurrentDeviceAndRecordFieldNames() {
        val devices = Json.decodeFromString<AccountLoginDevicesResponse>("""{"code":0,"data":{"devices":[{"device_name":"Phone","is_current_device":true,"latest_login_at":"2026-10-26 12:00","source":"Android"}]}}""")
        assertEquals(true, devices.data?.devices?.single()?.isCurrentDevice)
        assertEquals("2026-10-26 12:00", devices.data?.devices?.single()?.latestLoginAt)
        val records = Json.decodeFromString<AccountLoginRecordsResponse>("""{"code":0,"data":{"list":[{"ip":"2001:db8::1","time_at":"2026-10-26","geo":"示例地区"}]}}""")
        assertEquals("2001:db8::1", records.data?.list?.single()?.ip)
        assertEquals("2026-10-26", records.data?.list?.single()?.timeAt)
        assertTrue(records.data?.list?.single()?.geo?.isNotBlank() == true)
    }
}
