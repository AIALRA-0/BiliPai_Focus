package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.PlayUrlResponse

/** Credential platform controls signing; each frontend owns retry/cooldown policy. */
fun buildSignedAppPlayUrlParams(
    bvid: String, cid: Long, quality: Int, accessToken: String, tokenPlatform: String,
    audioLang: String? = null, timestampSec: Long = AppSignUtils.getTimestamp(),
): Map<String, String> {
    val androidToken = tokenPlatform == TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID
    val hdToken = tokenPlatform == TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD
    val params = mutableMapOf(
        "bvid" to bvid, "cid" to cid.toString(), "qn" to quality.toString(),
        "fnval" to "20432", "fnver" to "0", "fourk" to "1", "access_key" to accessToken,
        "appkey" to when {
            hdToken -> AppSignUtils.ANDROID_HD_APP_KEY
            androidToken -> AppSignUtils.ANDROID_APP_KEY
            else -> AppSignUtils.TV_APP_KEY
        },
        "ts" to timestampSec.toString(), "platform" to "android",
        "mobi_app" to when {
            hdToken -> "android_hd"
            androidToken -> "android"
            else -> "android_tv_yst"
        }, "device" to "android",
    )
    if (!audioLang.isNullOrEmpty()) {
        params["cur_language"] = audioLang
        params["lang"] = audioLang
    }
    return when {
        hdToken -> AppSignUtils.signForAndroidHdLogin(params)
        androidToken -> AppSignUtils.signForAndroidApi(params)
        else -> AppSignUtils.signForTvLogin(params)
    }
}

object PlaybackStreamDataSource {
    suspend fun appPlayUrl(
        bvid: String, cid: Long, quality: Int, accessToken: String, tokenPlatform: String,
        audioLang: String? = null,
    ): PlayUrlResponse = NetworkModule.playbackApi().getPlayUrlApp(
        buildSignedAppPlayUrlParams(bvid, cid, quality, accessToken, tokenPlatform, audioLang),
    )
}
