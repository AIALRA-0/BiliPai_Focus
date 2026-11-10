package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.store.TokenManager

/** 登录设备查询使用凭证原属客户端签名，不改变账号的登录方式。 */
internal fun loginDeviceRequestParams(
    accessKey: String,
    platform: String,
    csrf: String,
    buvid: String,
    timestampSeconds: Long,
): Map<String, String> {
    require(accessKey.isNotBlank()) { "请使用扫码或短信重新登录后查看登录设备" }
    val params = mutableMapOf(
        "access_key" to accessKey,
        "csrf" to csrf,
        "device_name" to "android",
        "device_platform" to "android",
        "platform" to "android",
        "ts" to timestampSeconds.toString(),
    )
    if (buvid.isNotBlank()) {
        params["buvid"] = buvid
        params["local_id"] = buvid
    }
    return when (platform) {
        TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD -> AppSignUtils.signForAndroidHdLogin(params + mapOf(
            "mobi_app" to "android_hd",
            "statistics" to "{\"appId\":5,\"platform\":3,\"version\":\"2.0.1\",\"abtest\":\"\"}",
        ))
        TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID -> AppSignUtils.signForAndroidEncodedApi(params + ("mobi_app" to "android"))
        TokenManager.ACCESS_TOKEN_PLATFORM_TV -> AppSignUtils.signForTvApi(params + ("mobi_app" to "android_tv_yst"))
        else -> error("当前登录方式暂不支持查询设备，请使用扫码或短信重新登录")
    }
}
