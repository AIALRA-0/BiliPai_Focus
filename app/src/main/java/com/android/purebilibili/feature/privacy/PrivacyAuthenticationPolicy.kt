package com.android.purebilibili.feature.privacy

data class PrivacyNavigationTarget(
    val routeBase: String,
    val seasonSeriesType: String? = null
)

enum class PrivacyAuthenticationReason(
    val title: String,
    val subtitle: String
) {
    OPEN_PRIVACY_CONTENT(
        title = "解锁隐私内容",
        subtitle = "使用指纹、人脸或锁屏密码继续"
    )
}

data class PrivacyAuthenticationRequest(
    val reason: PrivacyAuthenticationReason
)

sealed interface PrivacyAuthenticationResult {
    data object Success : PrivacyAuthenticationResult
    data class Failure(val message: String) : PrivacyAuthenticationResult
}

internal fun shouldRequirePrivacyAuthentication(
    privacyAuthenticationEnabled: Boolean,
    privacySessionUnlocked: Boolean,
    target: PrivacyNavigationTarget
): Boolean {
    return privacyAuthenticationEnabled &&
        !privacySessionUnlocked &&
        isPrivacyProtectedNavigationTarget(target)
}

internal fun isPrivacyProtectedNavigationTarget(target: PrivacyNavigationTarget): Boolean {
    return when (target.routeBase) {
        "search",
        "search_trending",
        "history",
        "favorite",
        "watch_later",
        "download_list",
        "offline_video",
        "inbox",
        "message/reply_me",
        "message/at_me",
        "message/like_me",
        "message/system_notice",
        "chat" -> true
        "season_series_detail" -> target.seasonSeriesType == "favorite" ||
            target.seasonSeriesType == "favorite_season"
        else -> false
    }
}
