package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils

internal fun buildAndroidSmsSendParams(
    phone: String,
    countryCode: Int, // PiliPlus countryId, e.g. China = 86
    token: String?,
    challenge: String?,
    validate: String?,
    seccode: String?,
    buvid: String,
    loginSessionId: String,
    timestampSeconds: Long
): Map<String, String> = buildMap {
    putAll(androidLoginBaseParams(timestampSeconds))
    put("buvid", buvid)
    put("local_id", buvid)
    put("login_session_id", loginSessionId)
    put("cid", countryCode.toString())
    put("tel", phone)
    putIfNotBlank("recaptcha_token", token)
    putIfNotBlank("gee_challenge", challenge)
    putIfNotBlank("gee_validate", validate)
    putIfNotBlank("gee_seccode", seccode)
}

internal fun buildAndroidSmsLoginParams(
    phone: String,
    countryCode: Int,
    code: Int,
    captchaKey: String,
    buvid: String,
    deviceId: String,
    encryptedDeviceToken: String,
    timestampSeconds: Long
): Map<String, String> = buildMap {
    putAll(androidLoginBaseParams(timestampSeconds))
    putAll(androidLoginDeviceParams(buvid, deviceId, encryptedDeviceToken))
    put("cid", countryCode.toString())
    put("tel", phone)
    put("code", code.toString())
    put("captcha_key", captchaKey)
    put("from_pv", "main.my-information.my-login.0.click")
    // Pre-encode like BiliPai; AppSign percent-encodes again when hashing.
    put("from_url", AppSignUtils.percentEncode("bilibili://user_center/mine"))
}

internal fun buildAndroidPasswordLoginParams(
    username: String,
    encryptedPassword: String,
    token: String?,
    challenge: String?,
    validate: String?,
    seccode: String?,
    buvid: String,
    deviceId: String,
    encryptedDeviceToken: String,
    timestampSeconds: Long
): Map<String, String> = buildMap {
    putAll(androidLoginBaseParams(timestampSeconds))
    putAll(androidLoginDeviceParams(buvid, deviceId, encryptedDeviceToken))
    put("username", username)
    put("password", encryptedPassword)
    put("permission", "ALL")
    putIfNotBlank("recaptcha_token", token)
    putIfNotBlank("gee_challenge", challenge)
    putIfNotBlank("gee_validate", validate)
    putIfNotBlank("gee_seccode", seccode)
    put("from_pv", "main.homepage.avatar-nologin.all.click")
    put("from_url", AppSignUtils.percentEncode("bilibili://pegasus/promo"))
}

private fun androidLoginDeviceParams(
    buvid: String,
    deviceId: String,
    encryptedDeviceToken: String
): Map<String, String> = mapOf(
    "bili_local_id" to deviceId,
    "buvid" to buvid,
    "device" to "phone",
    "device_id" to deviceId,
    // Match the desktop PiliPlus Passport profile. These fields are assessed
    // together with the signed buvid and the Android-HD request headers.
    "device_name" to "vivo",
    "device_platform" to "Android14vivo",
    // BiliPai pre-encodes dt before AppSign / form body.
    "dt" to AppSignUtils.percentEncode(encryptedDeviceToken),
    "local_id" to buvid
)

private fun MutableMap<String, String>.putIfNotBlank(key: String, value: String?) {
    value?.takeIf(String::isNotBlank)?.let { put(key, it) }
}

private fun androidLoginBaseParams(timestampSeconds: Long): Map<String, String> = mapOf(
    "appkey" to AppSignUtils.ANDROID_HD_APP_KEY,
    "build" to "2001100",
    "c_locale" to "zh_CN",
    "channel" to "master",
    "disable_rcmd" to "0",
    "mobi_app" to "android_hd",
    "platform" to "android",
    "s_locale" to "zh_CN",
    "statistics" to "{\"appId\":5,\"platform\":3,\"version\":\"2.0.1\",\"abtest\":\"\"}",
    "ts" to timestampSeconds.toString()
)
