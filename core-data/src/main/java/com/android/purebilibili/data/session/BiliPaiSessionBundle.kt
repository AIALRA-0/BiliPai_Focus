package com.android.purebilibili.data.session

import kotlinx.serialization.Serializable

@Serializable
data class BiliPaiSessionBundle(
    val mid: Long,
    val sessData: String,
    val csrf: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenPlatform: String = "tv",
    val buvid3: String = "",
    val isVip: Boolean = false,
)
